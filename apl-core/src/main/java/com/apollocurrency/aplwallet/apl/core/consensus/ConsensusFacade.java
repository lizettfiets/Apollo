/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.Generator;

import java.util.List;

public interface ConsensusFacade {
    void setPreviousBlock(Block block, List<Block> prevBlocks);

    /**
     * Update generator data from the last block.
     */
    void updateGeneratorData(Generator generator, Block lastBlock);


    Block generateBlock(Generator generator, Block lastBlock, int currentTimeWithForgingDelay);

}

