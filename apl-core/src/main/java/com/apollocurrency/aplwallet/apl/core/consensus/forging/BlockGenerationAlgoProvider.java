/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.util.Pair;

import java.math.BigInteger;

public interface BlockGenerationAlgoProvider {

    BigInteger calculateHit(byte[] generationSignatureHash);

    long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block);

    boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, long timestamp);

    /**
     * Calculate potential block timestamp. It's time when generator could forge a block.
     */
    int getBlockTimestamp(long hitTime, int generationTimestamp);

    byte[] calculateGenerationSignature(byte[] publicKey, Block prevBlock);

    Pair<Integer, Integer> getBlockTimeoutAndVersion(int blockTimestamp, int generationTimestamp, int lastBlockTimestamp,
                                                     int numberOfTxsAtBlockTimestamp, int numberOfTxsAtGenerationTimestamp);
}
