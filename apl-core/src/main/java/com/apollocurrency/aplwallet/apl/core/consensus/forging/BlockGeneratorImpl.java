/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;


import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.consensus.BlockAlgoProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;

public class BlockGeneratorImpl implements BlockGenerator, Runnable {
    // All available generators
    private Map<Long, Generator> generators = new HashMap<>();
    // Filtered generators which have positive effective balance
    private List<Generator> sortedGenerators;


    private AccountService accountService = CDI.current().select(AccountService.class).get();
    private UnconfirmedTransactionService unconfirmedTransactionService = CDI.current().select(UnconfirmedTransactionService.class).get();
    private BlockAlgoProvider blockAlgoProvider = CDI.current().select(BlockAlgoProvider.class).get();
    private BlockGenerationAlgoProvider blockGenerationAlgoProvider = CDI.current().select(BlockGenerationAlgoProvider.class).get();

    public BlockGeneratorImpl() {

    }

    @Override
    public void run() {
        performForgingIteration();
    }

    public void performForgingIteration() {
        try {
            try {
                Block lastBlock = nodeService.getLastBlock(node.getId());
                if (lastBlock == null) {
                    log.error("Last block is null");
                    return;
                }

                if (lastBlock.getId() != lastBlockId || sortedGenerators == null) {
                    List<Generator> generators = new ArrayList<>();
                    for (Generator generator : this.generators.values()) {
                        updateGeneratorData(generator, lastBlock);
                        if (generator.getEffectiveBalance().signum() > 0) {
                            generators.add(generator);
                        }
                    }
                    Collections.sort(generators);
                    sortedGenerators = Collections.unmodifiableList(generators);
                }

                final Long currentTimeWithForgingDelay = timeSource.getCurrentTime() - Constants.FORGING_DELAY;
                for (Generator generator : sortedGenerators) {
                    if (generator.getHitTime() > currentTimeWithForgingDelay || forge(generator, lastBlock, currentTimeWithForgingDelay)) {
                        return;
                    }
                }
            }
            catch (Exception e) {
                log.info("Error in block generation thread", e);
            }
        }
        catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Update generator data from the last block.
     */
    private void updateGeneratorData(Generator generator, Block lastBlock) {
        Account accountAtHeight = accountService.getAccount(generator.getAccountId());
        if (accountAtHeight == null) {
            generator.effectiveBalance = BigInteger.ZERO;
            return;
        } else {
            generator.effectiveBalance = BigInteger.valueOf(Math.max(accountService.getEffectiveBalanceAPL(generator.getAccountId()).longValue(),
                    0));
        }
        if (accountAtHeight.getEffectiveBalance().signum() == 0) {
            generator.hitTime = 0;
            generator.hit = BigInteger.ZERO;
            return;
        }
        generator.hit = blockGenerationAlgoProvider.calculateHit(generator.accountId, lastBlock);
        generator.hitTime = blockGenerationAlgoProvider.getHitTime(generator.effectiveBalance, generator.hit, lastBlock);
        generator.deadline = Math.max(generator.hitTime - lastBlock.getTimestamp(), 0);

        log.debug("Generator     - {} got lastBlock {} on node {}", generator, lastBlock, node.getId());
    }

    boolean forge(Generator generator, Block lastBlock, Long currentTimeWithForgingDelay) {
        long potentialBlockTimestamp = blockGenerationAlgoProvider.getBlockTimestamp(generator, currentTimeWithForgingDelay);
        long[] timeoutAndVersion = blockGenerationAlgoProvider.getBlockTimeoutAndVersion(potentialBlockTimestamp, currentTimeWithForgingDelay, lastBlock);
        if (timeoutAndVersion == null) {
            return false;
        }
        long timeout = timeoutAndVersion[0];
        log.debug("Timeout: {}, version: {}, Generator account: {}", timeout, timeoutAndVersion[1], generator.accountId);

        if (!blockGenerationAlgoProvider.verifyHit(generator.hit, generator.effectiveBalance, lastBlock, potentialBlockTimestamp)) {
            log.debug(this.toString() + " failed to forge at " + (potentialBlockTimestamp + timeout) + " height " + lastBlock.getHeight() + " " +
                    "last " +
                    "timestamp " + lastBlock.getTimestamp());
            return false;
        }
        generateBlock(lastBlock, generator.getAccountId(), potentialBlockTimestamp + timeout, timeout, timeoutAndVersion[1]);
        return true;
    }

    void generateBlock(Block previousBlock, Long accountId, long blockTimestamp, long timeout, long version) {
        List<UnconfirmedTransaction> unconfirmedTransactions = unconfirmedTransactionService.getUnconfirmedTransactions(node.getId(), blockTimestamp);
        List<Transaction> blockTransactions = unconfirmedTransactions.stream()
                .map(unTr -> new Transaction(unTr, node.getId()))
                .collect(Collectors.toList());

        unconfirmedTransactions.stream().forEach(unTr -> unconfirmedTransactionService.delete(node.getId(), unTr.getId()));

        Block block = new Block(node.getId(), version, blockTimestamp, previousBlock.getId(), accountId, timeout, blockTransactions);
        blockAlgoProvider.calculateId(block);
        blockAlgoProvider.setPreviousBlock(block, previousBlock);
        nodeService.saveBlock(block);

        log.debug("Forging: block was generated NodeId: '{}' blockId: '{}'", node.getId().toString(), block.getId().toString());

        //Send new block for all;
        nodeService.sendBlockForAll(node, block);
    }


    @Override
    public Generator startGeneration(Generator generator) {
        Generator old = generators.putIfAbsent(generator.getAccountId(), generator);
        if (old != null) {
            log.debug(old + " is already forging");
            return old;
        }
        return generator;
    }

    @Override
    public Generator stopGeneration(Generator generator) {
        return generators.remove(generator.getAccountId());
    }
}
