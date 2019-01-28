/*
 * Copyright © 2018-2019 Apollo Foundation
 */


package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import javax.inject.Inject;

public abstract class AbstractBlockValidator implements BlockValidator {
    private static final Logger LOG = getLogger(AbstractBlockValidator.class);
    private BlockDao blockDao;
    protected BlockchainConfig blockchainConfig;
    private Blockchain blockchain;
    @Inject
    public AbstractBlockValidator(BlockDao blockDao, BlockchainConfig blockchainConfig, Blockchain blockchain) {
        Objects.requireNonNull(blockDao, "BlockDao is null");
        Objects.requireNonNull(blockchainConfig, "Blockchain config is null");
        this.blockDao = blockDao;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
    }

    @Override
    public void validate(Block block, Block previousLastBlock, int curTime) throws BlockchainProcessor.BlockNotAcceptedException {
        if (previousLastBlock.getId() != block.getPreviousBlockId()) {
            throw new BlockchainProcessor.BlockOutOfOrderException("Previous block id doesn't match", block);
        }
        if (block.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
            LOG.warn("Received block " + block.getStringId() + " from the future, timestamp " + block.getTimestamp()
                    + " generator " + Long.toUnsignedString(block.getGeneratorId()) + " current time " + curTime + ", system clock may be off");
            throw new BlockchainProcessor.BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
                    + " current time is " + curTime, block);
        }
        if (block.getTimestamp() <= previousLastBlock.getTimestamp()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Block timestamp " + block.getTimestamp() + " is before previous block timestamp "
                    + previousLastBlock.getTimestamp(), block);
        }
        verifySignature(block);
        validatePreviousHash(block, previousLastBlock);
        if (block.getId() == 0L || blockDao.hasBlock(block.getId(), previousLastBlock.getHeight())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Duplicate block or invalid id", block);
        }
        if (!block.verifyGenerationSignature() && !Generator.allowsFakeForging(block.getGeneratorPublicKey())) {
            Account generatorAccount = Account.getAccount(block.getGeneratorId());
            long generatorBalance = generatorAccount == null ? 0 : generatorAccount.getEffectiveBalanceAPL();
            throw new BlockchainProcessor.BlockNotAcceptedException("Generation signature verification failed, effective balance " + generatorBalance, block);
        }

        int numberOfTransactions = block.getTransactions().size();
        if (numberOfTransactions > blockchainConfig.getCurrentConfig().getMaxNumberOfTransactions()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid block transaction count " + numberOfTransactions, block);
        }
        if (block.getPayloadLength() > blockchainConfig.getCurrentConfig().getMaxPayloadLength() || block.getPayloadLength() < 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid block payload length " + block.getPayloadLength(), block);
        }
        switch (block.getVersion()) {
            case Block.LEGACY_BLOCK_VERSION:
                if (blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled()) {
                    throw new BlockchainProcessor.BlockNotAcceptedException("Legacy blocks are not accepting during adaptive forging", block);
                }
                break;
            case Block.INSTANT_BLOCK_VERSION:
                validateInstantBlock(block, previousLastBlock);
                break;
            case Block.ADAPTIVE_BLOCK_VERSION:
                validateAdaptiveBlock(block, previousLastBlock);
                break;
            case Block.REGULAR_BLOCK_VERSION:
                validateRegularBlock(block, previousLastBlock);
                break;
        }
    }

    abstract void validatePreviousHash(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void verifySignature(Block block) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateAdaptiveBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateInstantBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateRegularBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    public boolean verifyGenerationSignature(Block block) throws BlockchainProcessor.BlockOutOfOrderException {
        try {
            Block previousBlock = blockchain.getBlock(block.getPreviousBlockId());
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing", block);
            }

            Account account = Account.getAccount(block.getGeneratorId());
            long effectiveBalance = account == null ? 0 : account.getEffectiveBalanceAPL();
            if (effectiveBalance <= 0) {
                return false;
            }

            MessageDigest digest = Crypto.sha256();
            digest.update(previousBlock.getGenerationSignature());
            byte[] actualGenerationSignature = digest.digest(block.getGeneratorPublicKey());
            if (!Arrays.equals(block.getGenerationSignature(), actualGenerationSignature)) {
                return false;
            }

            BigInteger hit = new BigInteger(1, new byte[]{actualGenerationSignature[7], actualGenerationSignature[6], actualGenerationSignature[5], actualGenerationSignature[4], actualGenerationSignature[3], actualGenerationSignature[2], actualGenerationSignature[1], actualGenerationSignature[0]});

            return Generator.verifyHit(hit, BigInteger.valueOf(effectiveBalance), previousBlock, block.getTimestamp() - block.getTimeout());

        } catch (RuntimeException e) {

            LOG.info("Error verifying block generation signature", e);
            return false;

        }

    }
}
