/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.BlockGenerationAlgoProvider;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.Generator;
import com.apollocurrency.aplwallet.apl.core.transaction.UnconfirmedTransactionService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class RegBlockExtrapolationFacade extends DefaultConsensusFacade {

    static final BigDecimal FLOAT_MULTIPLIER = BigDecimal.valueOf(1000L);
    static final BigInteger FLOAT_DIVIDER = BigInteger.valueOf(1000L);

    public RegBlockExtrapolationFacade(BlockchainConfig blockchainConfig, BlockAlgoProvider blockAlgoProvider, BlockGenerationAlgoProvider generationAlgoProvider, AccountService accountService, UnconfirmedTransactionService unconfirmedTransactionService) {
        super(blockchainConfig, blockAlgoProvider, generationAlgoProvider, accountService, unconfirmedTransactionService);
    }

    @Override
    public void setPreviousBlock(Block block, List<Block> prevBlocks) {
        super.setPreviousBlock(block, prevBlocks);
        if (!requireAdditionalDiff(block, prevBlocks.get(prevBlocks.size() - 1))) {
            return;
        }

        List<Block> fakeBlocks = new ArrayList<>(prevBlocks);

        long timeout = block.getTimeout();
        Generator forger = new Generator(new byte[32]);


        Block prevFakeBlock = block;
        Block newFakeBlock = block;
        long blockTime = 0;
        while (timeout > 0) {
            prevFakeBlock = newFakeBlock;
            updateGeneratorData(forger, prevFakeBlock);
            blockTime = forger.getDeadline() + 1;
            fakeBlocks.add(prevFakeBlock);
            // use constructor instead of generateBlock method to improve performance
            // maybe better just to calculate only base target and dont create block itself
            newFakeBlock = new BlockImpl(Block.REGULAR_BLOCK_VERSION, (int) forger.getHitTime() + 1, prevFakeBlock.getId(),
                    0L, 0L, 0, new byte[0], block.getGeneratorPublicKey(), new byte[32], new byte[32], 0, null, forger.getKeySeed());
            long id = getBlockAlgoProvider().calculateId(block);
            newFakeBlock.setId(id);
            super.setPreviousBlock(newFakeBlock, fakeBlocks.subList(Math.max(0, fakeBlocks.size() - 3), fakeBlocks.size()));
            timeout -= blockTime;
        }
        block.setBaseTarget(prevFakeBlock.getBaseTarget());
        BigInteger partialDiff = calculatePartialDiff(newFakeBlock, prevFakeBlock, timeout, blockTime);
        block.setCumulativeDifficulty(prevFakeBlock.getCumulativeDifficulty().add(partialDiff));
    }

    public BigInteger calculatePartialDiff(Block block, Block prevBlock, long timeout, long blockTime) {
        if (timeout >= 0) {
            return BigInteger.ZERO;
        }
        long timeoutRemaining = timeout + blockTime;
        BigInteger partDiff = getBlockAlgoProvider().calculateDifficulty(block, prevBlock);
        BigInteger bigDecimal = BigDecimal.valueOf(((double) timeoutRemaining / blockTime)).multiply(FLOAT_MULTIPLIER).toBigInteger();
        partDiff = partDiff.multiply(bigDecimal).divide(FLOAT_DIVIDER);
        return partDiff;
    }

    private boolean requireAdditionalDiff(Block block, Block prevBlock) {
        int adaptiveBlockTime = getBlockchainConfig().getCurrentConfig().getAdaptiveBlockTime();
        long actualBlockTime = block.getTimestamp() - prevBlock.getTimestamp() - block.getTimeout();
        return Block.LEGACY_BLOCK_VERSION != block.getVersion()
                && Block.REGULAR_BLOCK_VERSION != block.getVersion()
                && actualBlockTime < adaptiveBlockTime;
    }
}
