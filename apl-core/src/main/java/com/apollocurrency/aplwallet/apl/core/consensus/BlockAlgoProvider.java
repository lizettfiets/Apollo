/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;


import com.apollocurrency.aplwallet.apl.core.app.Block;

import java.math.BigInteger;

public interface BlockAlgoProvider {
    long calculateId(Block block);

    long calculateBaseTarget(Block block, Block previousBlock);

    BigInteger calculateCumulativeDifficulty(Block block, Block previousBlock);

    long getBlockTimeAverage(Block block, Block previousBlock);
}
