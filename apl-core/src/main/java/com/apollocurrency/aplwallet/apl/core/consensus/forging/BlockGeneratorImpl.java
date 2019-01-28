/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;


import com.apollocurrency.aplwallet.apl.core.app.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Time;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.consensus.BlockAlgoProvider;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class BlockGeneratorImpl implements BlockGenerator, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(BlockGeneratorImpl.class);
    // All available generators
    private Map<Long, Generator> generators = new HashMap<>();
    // Filtered generators which have positive effective balance
    private List<Generator> sortedGenerators;
    // Last block id, calculated during generation iteration
    private long lastBlockId;
    private static final Listeners<Generator,Event> listeners = new Listeners<>();
    private BlockAlgoProvider blockAlgoProvider;
    private Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private BlockGenerationAlgoProvider blockGenerationAlgoProvider;
    private BlockchainConfig blockchainConfig;
    private Time time;
    private boolean suspendGeneration;
    private int generationDelay = Constants.FORGING_SPEEDUP;

    @Inject
    public BlockGeneratorImpl(BlockAlgoProvider blockAlgoProvider, Blockchain blockchain, BlockchainProcessor blockchainProcessor, BlockGenerationAlgoProvider blockGenerationAlgoProvider, BlockchainConfig blockchainConfig, Time time) {
        this.blockAlgoProvider = blockAlgoProvider;
        this.blockchain = blockchain;
        this.blockchainProcessor = blockchainProcessor;
        this.blockGenerationAlgoProvider = blockGenerationAlgoProvider;
        this.blockchainConfig = blockchainConfig;
        this.time = time;
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
//                    Note, that generation delay can be negative, so forging will be more faster
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
                                    List<Block> poppedOffBlock = blockchainProcessor.popOffTo(previousBlock);
                                    for (Block block : poppedOffBlock) {
                                        transactionProcessor.processLater(block.getTransactions());
                                    }
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
                        if (generator.getHitTime() > generationLimit || forge(generator, lastBlock, generationLimit)) {
                            return;
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
            listeners.notify(generator, Event.GENERATOR_UPDATE);
        }

    boolean forge(Generator generator, Block lastBlock, int generationTimestamp) throws BlockchainProcessor.BlockNotAcceptedException {
        int timestamp = blockGenerationAlgoProvider.getBlockTimestamp(generator.getHitTime(), generationTimestamp);
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
                blockchainProcessor.generateBlock(generator.getKeySeed(), timestamp  + timeout, timeout, version);
                setGenerationDelay(Constants.GENERATION_DELAY);
                return true;
            }
            catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (time.getTime() - start > 3) { // give up after trying for 3 s
                    throw e;
                }
            }
            catch (BlockchainProcessor.BlockNotAcceptedException e) {
                throw e;
            }
        }
    }

    int timestamp = getTimestamp(generationLimit);
    int[] timeoutAndVersion = getBlockTimeoutAndVersion(timestamp, generationLimit, lastBlock);
        if (timeoutAndVersion == null) {
        return false;
    }
    int timeout = timeoutAndVersion[0];
        if (!verifyHit(hit, effectiveBalance, lastBlock, timestamp)) {
        LOG.debug(this.toString() + " failed to forge at " + (timestamp + timeout) + " height " + lastBlock.getHeight() + " " +
                "last " +
                "timestamp " + lastBlock.getTimestamp());
        return false;
    }
    int start = timeService.getEpochTime();
        while (true) {
        try {
            blockchainProcessor.generateBlock(keySeed, timestamp  + timeout, timeout, timeoutAndVersion[1]);
            setDelay(Constants.GENERATION_DELAY);
            return true;
        }
        catch (BlockchainProcessor.TransactionNotAcceptedException e) {
            // the bad transaction has been expunged, try again
            if (timeService.getEpochTime() - start > 10) { // give up after trying for 10 s
                throw e;
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
    public boolean addListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }
}
