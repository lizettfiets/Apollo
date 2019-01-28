/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

import io.firstbridge.fbchainmodel.model.Block;

import java.math.BigInteger;

public interface BlockGenerationAlgoProvider {

    BigInteger calculateHit(Long accountId, Block prevBlock);

    long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block);

    boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, long timestamp);

    /**
     * Calculate potential block timestamp. It's time when generator potential could forge a block.
     */
    long getBlockTimestamp(Generator generator, long generationLimit);

    long[] getBlockTimeoutAndVersion(long potentialBlockTimestamp, long currentTimeWithForgingDelay, Block lastBlock);
}
