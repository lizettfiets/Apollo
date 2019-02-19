/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.BlockGenerationAlgoProvider;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultBlockValidator extends AbstractBlockValidator {
    private static final Logger log = LoggerFactory.getLogger(DefaultBlockValidator.class);
    private BlockGenerationAlgoProvider blockGenerationAlgoProvider;
    @Inject
    public DefaultBlockValidator(BlockDao blockDao, BlockchainConfig blockchainConfig, Blockchain blockchain, AccountService accountService) {
        super(blockDao, blockchainConfig, blockchain, accountService);
    }

    @Override
    public void validatePreviousHash(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (!Arrays.equals(Crypto.sha256().digest(((BlockImpl) previousBlock).bytes()),
                block.getPreviousBlockHash())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Previous block hash doesn't match", block);
        }
    }

    @Override
    public void verifySignature(Block block) throws BlockchainProcessor.BlockNotAcceptedException {
        if (!verifyBlockSignature(block)) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Block signature verification failed", block);
        }
    }

    public boolean verifyBlockSignature(Block block) {
        return checkSignature(block) && getAccountService().setOrVerify(block.getGeneratorId(), block.getGeneratorPublicKey());
    }

    public boolean checkSignature(Block block) {

        if (!block.hasValidSignature()) {
            byte[] bytes = block.getBytes();
            byte[] data = Arrays.copyOf(bytes, bytes.length - 64);
            boolean hasValidSignature = block.getBlockSignature() != null && Crypto.verify(block.getBlockSignature(), data,
                    block.getGeneratorPublicKey());
            block.setHasValidSignature(hasValidSignature);
        }
        return block.hasValidSignature();
    }

    @Override
    public void validateAdaptiveBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        int actualBlockTime = block.getTimestamp() - previousBlock.getTimestamp();
        BlockchainConfig blockchainConfig = getBlockchainConfig();
        if (actualBlockTime < blockchainConfig.getCurrentConfig().getAdaptiveBlockTime() && block.getTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid adaptive block. " + actualBlockTime, null);
        }
    }

    @Override
    public void validateInstantBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        BlockchainConfig blockchainConfig = getBlockchainConfig();
        if (block.getTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect instant block", block);
        }
    }

    @Override
    public void validateRegularBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getTransactions().size() <= getBlockchainConfig().getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock() || block.getTimeout() != 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect regular block", block);
        }
    }

    @Override
    public boolean verifyGenerationSignature(Block block) throws BlockchainProcessor.BlockOutOfOrderException {
        {
            try {
                Block previousBlock = getBlockchain().getBlock(block.getPreviousBlockId());
                if (previousBlock == null) {
                    throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing", block);
                }

                Account account = getAccountService().getAccount(block.getGeneratorId());
                long effectiveBalance = account == null ? 0 : account.getEffectiveBalanceAPL();
                if (effectiveBalance <= 0) {
                    return false;
                }

                byte[] actualGenerationSignature = blockGenerationAlgoProvider.calculateGenerationSignature(block.getGeneratorPublicKey(), previousBlock);
                if (!Arrays.equals(block.getGenerationSignature(), actualGenerationSignature)) {
                    return false;
                }

                BigInteger hit = blockGenerationAlgoProvider.calculateHit(actualGenerationSignature);

                return blockGenerationAlgoProvider.verifyHit(hit, BigInteger.valueOf(effectiveBalance), previousBlock, block.getTimestamp() - block.getTimeout());

            }
            catch (RuntimeException e) {
                log.info("Error verifying block generation signature", e);
                return false;
            }
        }
    }
}
