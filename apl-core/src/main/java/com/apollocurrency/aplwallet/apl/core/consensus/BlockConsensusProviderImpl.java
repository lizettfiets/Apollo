/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.BlockService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;

import java.util.Objects;
import javax.inject.Inject;

public class BlockConsensusProviderImpl implements BlockConsensusProvider {
    private BlockAlgoProvider blockAlgoProvider;

    @Inject
    public BlockConsensusProviderImpl(BlockAlgoProvider blockAlgoProvider, BlockService blockService) {
        this.blockAlgoProvider = blockAlgoProvider;
    }

    @Override
    public void setPrevious(Block block, Block prevBlock) {
        Objects.requireNonNull(block, "Block should not be null");
            if (prevBlock != null) {
                if (prevBlock.getId() != block.getPreviousBlockId()) {
                    // shouldn't happen as previous id is already verified, but just in case
                    throw new IllegalStateException("Previous block id doesn't match");
                }
                block.setHeight(prevBlock.getHeight() + 1);
                long baseTarget = blockAlgoProvider.calculateBaseTarget(block, prevBlock);
                block.setBaseTarget(baseTarget);
                block.setCumulativeDifficulty(blockAlgoProvider.calculateCumulativeDifficulty(block, prevBlock));
            } else {
                block.setHeight(0);
            }
            short index = 0;
            for (Transaction transaction : block.getTransactions()) {
                transaction.setBlock(block);
                transaction.setIndex(index++);
            }
        }

    }
