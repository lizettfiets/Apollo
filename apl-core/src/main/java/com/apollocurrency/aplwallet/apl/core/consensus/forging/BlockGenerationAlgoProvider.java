/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;

public interface BlockGenerationAlgoProvider {

    BigInteger calculateHit(byte[] publicKey, Block prevBlock);

    long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block);

    boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, long timestamp);

    /**
     * Calculate potential block timestamp. It's time when generator could forge a block.
     */
    int getBlockTimestamp(long hitTime, int generationTimestamp);

    Pair<Integer, Integer> getBlockTimeoutAndVersion(int blockTimestamp, int generationTimestamp, Block lastBlock);
}
