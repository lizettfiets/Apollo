/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.consensus.acceptor.BlockAcceptor;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.BlockGenerationAlgoProvider;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.Generator;
import com.apollocurrency.aplwallet.apl.core.consensus.genesis.GenesisDataHolder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.UnconfirmedTransactionService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;


public class DefaultConsensusFacade implements ConsensusFacade {

    private static final Logger log = LoggerFactory.getLogger(DefaultConsensusFacade.class);

    private BlockchainConfig blockchainConfig;
    private BlockAlgoProvider blockAlgoProvider;
    private BlockGenerationAlgoProvider generationAlgoProvider;
    private AccountService accountService;
    private UnconfirmedTransactionService unconfirmedTransactionService;
    private BlockAcceptor blockAcceptor;
    private GenesisDataHolder genesisDataHolder;

    public DefaultConsensusFacade(BlockchainConfig blockchainConfig,
                                  BlockAlgoProvider blockAlgoProvider,
                                  BlockGenerationAlgoProvider generationAlgoProvider,
                                  AccountService accountService,
                                  UnconfirmedTransactionService unconfirmedTransactionService,
                                  BlockAcceptor genesisBlockAcceptor,
                                  GenesisDataHolder genesisDataHolder) {
        this.blockchainConfig = blockchainConfig;
        this.blockAlgoProvider = blockAlgoProvider;
        this.generationAlgoProvider = generationAlgoProvider;
        this.accountService = accountService;
        this.unconfirmedTransactionService = unconfirmedTransactionService;
        this.blockAcceptor = genesisBlockAcceptor;
        this.genesisDataHolder = genesisDataHolder;
    }

    @Override
    public void setPreviousBlock(Block block, List<Block> prevBlocks) {
        Block prevBlock = prevBlocks.get(prevBlocks.size() - 1);
        block.setHeight(prevBlock.getHeight() + 1);
        // calculate base target
        HeightConfig config = blockchainConfig.getCurrentConfig();
        List<Block> blocks = new ArrayList<>(prevBlocks);
        blocks.add(block);
        long blockTimeAverage = blockAlgoProvider.getBlockTimeAverage(blocks);
        long baseTarget = blockAlgoProvider.calculateBaseTarget(prevBlock, blockTimeAverage, config);
        block.setBaseTarget(baseTarget);
        // calculate diff
        BigInteger difficulty = blockAlgoProvider.calculateDifficulty(block, prevBlock);
        BigInteger cumulativeDifficulty = prevBlock.getCumulativeDifficulty().add(difficulty);
        block.setCumulativeDifficulty(cumulativeDifficulty);
        short index = 0;
        for (Transaction transaction : block.getTransactions()) {
            transaction.setBlock(block);
            transaction.setIndex(index++);
        }
    }

    @Override
    public List<Transaction> acceptBlock(Block block, List<Transaction> validPhasedTransactions, List<Transaction> invalidPhasedTransactions, Map<TransactionType, Map<String, Integer>> duplicates) throws BlockchainProcessor.TransactionNotAcceptedException {
        return blockAcceptor.accept(block, validPhasedTransactions, invalidPhasedTransactions, duplicates);
    }

    @Override
    public int compareGeneratorAndBlockTime(Generator generator, Block block, int curTime) {
        return Integer.compare(generationAlgoProvider.getBlockTimestamp(generator.getHitTime(), curTime), block.getTimestamp() - block.getTimeout());
    }

    /**
     * Update generator data from the last block.
     */
    @Override
    public void updateGeneratorData(Generator generator, Block lastBlock) {
        int height = lastBlock.getHeight();
        Account account = accountService.getAccount(generator.getAccountId(), height);
        if (account == null) {
            generator.setEffectiveBalance(BigInteger.ZERO);
        } else {
            generator.setEffectiveBalance(BigInteger.valueOf(Math.max(account.getEffectiveBalanceAPL(height), 0)));
        }
        if (generator.getEffectiveBalance().signum() == 0) {
            generator.setHitTime(0);
            generator.setHit(BigInteger.ZERO);
            return;
        }
        byte[] generationSignatureHash = generationAlgoProvider.calculateGenerationSignature(generator.getPublicKey(), lastBlock);
        generator.setHit(generationAlgoProvider.calculateHit(generationSignatureHash));
        generator.setHitTime(generationAlgoProvider.getHitTime(generator.getEffectiveBalance(), generator.getHit(), lastBlock));
        generator.setDeadline(Math.max(generator.getHitTime() - lastBlock.getTimestamp(), 0));
    }

    @Override
    public Block generateBlock(Generator forger, Block lastBlock, int generationTimestamp) {
        Block res = null;
        if (forger.getHitTime() <= generationTimestamp) {
            int potentialBlockTimestamp = generationAlgoProvider.getBlockTimestamp(forger.getHitTime(), generationTimestamp);
            int numberOfTxsAtGenerationTimestamp = unconfirmedTransactionService.getUnconfirmedTransactions(lastBlock, generationTimestamp).size();
            int numberOfTxsAtBlockTimestamp = unconfirmedTransactionService.getUnconfirmedTransactions(lastBlock, potentialBlockTimestamp).size();

            Pair<Integer, Integer> timeoutAndVersion = generationAlgoProvider.getBlockTimeoutAndVersion(potentialBlockTimestamp,
                    generationTimestamp,
                    lastBlock.getTimestamp(), numberOfTxsAtBlockTimestamp, numberOfTxsAtGenerationTimestamp);

            if (timeoutAndVersion != null) {
                int timeout = timeoutAndVersion.getFirst();
                int version = timeoutAndVersion.getSecond();
                log.trace("Timeout: {}, version: {}, Forger account: {}", timeout, version, forger.getAccountId());

                if (!generationAlgoProvider.verifyHit(forger.getHit(), forger.getEffectiveBalance(), lastBlock, potentialBlockTimestamp)) {
                    log.debug(forger.toString() + " failed to forge at " + (potentialBlockTimestamp + timeout) + " height " + lastBlock.getHeight() +
                            " " +
                            "last " +
                            "timestamp " + lastBlock.getTimestamp());
                } else {
                    res = createBlock(forger.getKeySeed(), lastBlock, potentialBlockTimestamp + timeout, timeout, version);
                }
            }
        }
        return res;
    }

    @Override
    public Block prepareBlock(Block block) {
        long id = blockAlgoProvider.calculateId(block);
        block.setId(id);
        return block;
    }

    @Override
    public Block generateGenesisBlock() {
        BlockImpl genesisBlock = new BlockImpl(genesisDataHolder.getGenesisPublicKey(), genesisDataHolder.getAccountBytes(),
                blockchainConfig.getCurrentConfig().getInitialBaseTarget());
        long id = blockAlgoProvider.calculateId(genesisBlock);
        genesisBlock.setId(id);
        return genesisBlock;
    }

    private Block createBlock(byte[] keySeed, Block previousBlock, int blockTimestamp, int timeout, int version) {
        SortedSet<UnconfirmedTransaction> sortedTransactions = unconfirmedTransactionService.getUnconfirmedTransactions(previousBlock,
                blockTimestamp);
        List<Transaction> blockTransactions = new ArrayList<>();
        MessageDigest digest = Crypto.sha256();
        long totalAmountATM = 0;
        long totalFeeATM = 0;
        int payloadLength = 0;
        for (UnconfirmedTransaction unconfirmedTransaction : sortedTransactions) {
            TransactionImpl transaction = unconfirmedTransaction.getTransaction();
            blockTransactions.add(transaction);
            digest.update(transaction.bytes());
            totalAmountATM += transaction.getAmountATM();
            totalFeeATM += transaction.getFeeATM();
            payloadLength += transaction.getFullSize();
        }
        byte[] payloadHash = digest.digest();
        final byte[] publicKey = Crypto.getPublicKey(keySeed);
        byte[] generationSignature = generationAlgoProvider.calculateGenerationSignature(publicKey, previousBlock);
        byte[] previousBlockHash = Crypto.sha256().digest(((BlockImpl) previousBlock).getBytes());

        BlockImpl block = new BlockImpl(version, blockTimestamp, previousBlock.getId(), totalAmountATM, totalFeeATM, payloadLength,
                payloadHash, publicKey, generationSignature,null, previousBlockHash, timeout, blockTransactions);
        byte[] blockSignature = Crypto.sign(block.getBytes(), keySeed);
        block.setBlockSignature(blockSignature);
        long id = blockAlgoProvider.calculateId(block);
        block.setId(id);
        return block;
    }


    public BlockchainConfig getBlockchainConfig() {
        return blockchainConfig;
    }

    public BlockAlgoProvider getBlockAlgoProvider() {
        return blockAlgoProvider;
    }

    public BlockGenerationAlgoProvider getGenerationAlgoProvider() {
        return generationAlgoProvider;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public UnconfirmedTransactionService getUnconfirmedTransactionService() {
        return unconfirmedTransactionService;
    }
}
