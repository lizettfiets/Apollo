/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.consensus.BlockAlgoProviderImpl;

import java.util.List;

public class LegacyBlockAlgoProvider extends BlockAlgoProviderImpl {
    @Override
    public long getBlockTimeAverage(List<Block> blocks) {
        Block block = blocks.get(blocks.size() - 1);
        Block prevBlock = blocks.get(blocks.size() - 2);
        int blockchainHeight = prevBlock.getHeight();
        if (blockchainHeight > 2 && blockchainHeight % 2 == 0) {  //duplicate check from basetarget calculation
            if (block.getVersion() != Block.LEGACY_BLOCK_VERSION) {
                return super.getBlockTimeAverage(blocks);
            } else {
                Block lastBlockForTimeAverage = blocks.get(blocks.size() - 4);
                return (block.getTimestamp() - lastBlockForTimeAverage.getTimestamp()) / 3;
            }
        }
        return -1; // assume that baseTarget will not be calculated when blockchainHeight check fails
    }
}
