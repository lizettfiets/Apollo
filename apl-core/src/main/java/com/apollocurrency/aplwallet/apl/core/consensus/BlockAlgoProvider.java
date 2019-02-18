/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;


import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;

import java.math.BigInteger;
import java.util.List;

public interface BlockAlgoProvider {
    long calculateId(Block block);

    long calculateBaseTarget(Block previousBlock, long blocktimeAverage, HeightConfig config);

    BigInteger calculateDifficulty(Block block, Block previousBlock);

    long getBlockTimeAverage(List<Block> prevBlocks);
}
