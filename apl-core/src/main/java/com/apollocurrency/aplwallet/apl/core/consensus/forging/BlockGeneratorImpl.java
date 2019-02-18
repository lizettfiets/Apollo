/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;


import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Time;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.consensus.BlockAlgoProvider;
import com.apollocurrency.aplwallet.apl.core.consensus.ConsensusFacade;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class BlockGeneratorImpl implements BlockGenerator<Generator>, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(BlockGeneratorImpl.class);
    // All available generators
    private Map<Long, Generator> generators = new HashMap<>();
    // Filtered generators which have positive effective balance
    // This field cached for better performance
    private List<Generator> sortedGenerators;
    // Last block id, calculated during generation iteration
    // This field cached for better performance
    private long lastBlockId;
    private BlockAlgoProvider blockAlgoProvider;
    private Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private BlockGenerationAlgoProvider blockGenerationAlgoProvider;
    private BlockchainConfig blockchainConfig;
    private Time time;
    private boolean suspendGeneration;
    private PropertiesHolder propertiesHolder;
    private ConsensusFacade consensusFacade;
    private int generationDelay;

    @Inject
    public BlockGeneratorImpl(BlockAlgoProvider blockAlgoProvider, Blockchain blockchain, BlockchainProcessor blockchainProcessor,
                              BlockGenerationAlgoProvider blockGenerationAlgoProvider, BlockchainConfig blockchainConfig, Time time,
                              PropertiesHolder propertiesHolder, ConsensusFacade consensusFacade) {

        this.blockAlgoProvider = blockAlgoProvider;
        this.blockchain = blockchain;
        this.blockchainProcessor = blockchainProcessor;
        this.blockGenerationAlgoProvider = blockGenerationAlgoProvider;
        this.blockchainConfig = blockchainConfig;
        this.time = time;
        this.propertiesHolder = propertiesHolder;
        this.generationDelay = propertiesHolder.FORGING_DELAY();
        this.consensusFacade = consensusFacade;
    }

    @Override
    public void run() {
        performGenerationIteration();
    }

    public void performGenerationIteration() {
        if (suspendGeneration) {
            return;
        }
        try {
            try {
                blockchain.updateLock();
                try {
                    Block lastBlock = blockchain.getLastBlock();
                    if (lastBlock == null || lastBlock.getHeight() < blockchainConfig.getLastKnownBlockHeight()) {
                        return;
                    }
//                    Note, that generation delay can be negative, so generation will be more faster
                    final int generationLimit = time.getTime() - generationDelay;
                    if (lastBlock.getId() != lastBlockId || sortedGenerators == null) {
                        lastBlockId = lastBlock.getId();
                        if (lastBlock.getTimestamp() > time.getTime() - 600) {
                            Block previousBlock = blockchain.getBlock(lastBlock.getPreviousBlockId());
                            for (Generator generator : generators.values()) {
                                setLastBlock(generator, previousBlock);
                                int timestamp = blockGenerationAlgoProvider.getBlockTimestamp(generator.getHitTime(), generationLimit);
                                if (timestamp != generationLimit && generator.getHitTime() > 0 && timestamp < lastBlock.getTimestamp() - lastBlock.getTimeout()) {
                                    LOG.debug("Pop off: " + generator.toString() + " will pop off last block " + lastBlock.getStringId());
                                    blockchainProcessor.popOffAndProcessTransactions(previousBlock);
                                    lastBlock = previousBlock;
                                    lastBlockId = previousBlock.getId();
                                    break;
                                }
                            }
                        }
                        List<Generator> activeGenerators = new ArrayList<>();
                        for (Generator generator : generators.values()) {
                            setLastBlock(generator, lastBlock);
                            if (generator.getEffectiveBalance().signum() > 0) {
                                activeGenerators.add(generator);
                            }
                        }
                        Collections.sort(activeGenerators);
                        sortedGenerators = Collections.unmodifiableList(activeGenerators);
                    }
                    for (Generator generator : sortedGenerators) {
                        Block block = consensusFacade.generateBlock(generator, lastBlock, generationLimit);
                        if (block != null) {
                            List<Block> prevBlocks = new ArrayList<>();
                            prevBlocks.add(lastBlock);
                            if (lastBlock.getHeight() > 1) {
                                prevBlocks.add(blockchain.getBlockAtHeight(lastBlock.getHeight() - 1));
                            }
                            if (lastBlock.getHeight() > 2) {
                                prevBlocks.add(blockchain.getBlockAtHeight(lastBlock.getHeight() - 2));
                            }
                            prevBlocks.sort(Comparator.comparingInt(Block::getHeight));
                            consensusFacade.setPreviousBlock(block, prevBlocks);

                        }
                    }
                } finally {
                    blockchain.updateUnlock();
                }
            } catch (Exception e) {
                LOG.info("Error in block generation thread", e);
            }
        } catch (Throwable t) {
            LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }

    }

    public void setGenerationDelay(int generationDelay) {
        this.generationDelay = generationDelay;
    }

    /**
     * Update generator data from the last block.
     */
        private void setLastBlock(Generator generator, Block lastBlock) {
            int height = lastBlock.getHeight();
            Account account = Account.getAccount(generator.getAccountId(), height);
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
            generator.setHit(blockGenerationAlgoProvider.calculateHit(generator.getPublicKey(), lastBlock));
            generator.setHitTime(blockGenerationAlgoProvider.getHitTime(generator.getEffectiveBalance(), generator.getHit(), lastBlock));
            generator.setDeadline(Math.max(generator.getHitTime() - lastBlock.getTimestamp(), 0));
        }

     public boolean forge(Generator generator, int generationTimestamp, Block lastBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        int timestamp = blockGenerationAlgoProvider.getBlockTimestamp(generator.getHitTime(),
                generationTimestamp);
        Pair<Integer, Integer> timeoutAndVersion = blockGenerationAlgoProvider.getBlockTimeoutAndVersion(timestamp, generationTimestamp, lastBlock);
        if (timeoutAndVersion == null) {
            return false;
        }
        int timeout = timeoutAndVersion.getLeft();
        int version = timeoutAndVersion.getRight();
        if (!blockGenerationAlgoProvider.verifyHit(generator.getHit(), generator.getEffectiveBalance(), lastBlock, timestamp)) {
            LOG.debug(this.toString() + " failed to forge at " + (timestamp + timeout) + " height " + lastBlock.getHeight() + " " +
                    "last " +
                    "timestamp " + lastBlock.getTimestamp());
            return false;
        }
        int start = time.getTime();
        while (true) {
            try {
                blockchainProcessor.generateBlock(generator.getKeySeed(), timestamp + timeout, timeout, version);
                setGenerationDelay(generationDelay);
                return true;
            }
            catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (time.getTime() - start > 2) { // give up after trying for 2 s
                    throw e;
                }
            }
        }
    }

    @Override
    public Generator startGeneration(Generator generator) {
        Generator old = generators.putIfAbsent(generator.getAccountId(), generator);
        if (old != null) {
            LOG.debug(old + " is already forging");
            return old;
        }
        return generator;
    }

    @Override
    public Generator stopGeneration(Generator generator) {
        return generators.remove(generator.getAccountId());
    }

    @Override
    public boolean stopAll() {
        return false;
    }

    @Override
    public boolean suspendAll() {
        return false;
    }

    @Override
    public boolean resumeAll() {
        return false;
    }
}
