/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;


import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;

import java.math.BigInteger;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("newBlockAlgoProvider")
public class BlockAlgoProviderImpl implements BlockAlgoProvider {

    private static final BigInteger two64 = new BigInteger("18446744073709551616");

    @Override
    public long calculateId(Block block) {
        if (block.getId() == 0) {
            if (block.getBlockSignature() == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
//            assuming that calculation of id will work only for generated blocks
            byte[] hash = Crypto.sha256().digest(block.getBytes());
            BigInteger id = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            return id.longValue();
        } else
            return block.getId();
    }


    @Override
    public long getBlockTimeAverage(List<Block> blocks) {
        long totalTime = 0;
        for (int i = blocks.size() - 1; i > 0 ; i--) {
            Block block = blocks.get(i);
            Block prevBlock = blocks.get(i - 1);
            long actualBlockTime = block.getTimestamp() - prevBlock.getTimestamp() - block.getTimeout();
            totalTime += actualBlockTime;
        }
        return totalTime / (blocks.size() - 1);
    }

    @Override
    public long calculateBaseTarget(Block previousBlock, long blocktimeAverage, HeightConfig config) {
        long prevBaseTarget = previousBlock.getBaseTarget();
        int blockchainHeight = previousBlock.getHeight();
        long calculatedBaseTarget;
        if (blockchainHeight > 2 && blockchainHeight % 2 == 0) {
            int blockTime = config.getBlockTime();
            if (blocktimeAverage > blockTime) {
                int maxBlocktimeLimit = config.getMaxBlockTimeLimit();
                calculatedBaseTarget = ((prevBaseTarget * Math.min(blocktimeAverage, maxBlocktimeLimit)) / blockTime);

            } else {
                int minBlocktimeLimit = config.getMinBlockTimeLimit();
                calculatedBaseTarget = (prevBaseTarget -
                        prevBaseTarget * Constants.BASE_TARGET_GAMMA * (blockTime - Math.max(blocktimeAverage, minBlocktimeLimit)) / (100 * blockTime));
            }
            long maxBaseTarget = config.getMaxBaseTarget();
            if (calculatedBaseTarget < 0 || calculatedBaseTarget > maxBaseTarget) {
                calculatedBaseTarget = maxBaseTarget;
            }
            long minBaseTarget = config.getMinBaseTarget();
            if (calculatedBaseTarget < minBaseTarget) {
                calculatedBaseTarget = minBaseTarget;
            }
        } else {
            calculatedBaseTarget = prevBaseTarget;
        }
        return calculatedBaseTarget;
    }

    @Override
    public BigInteger calculateDifficulty(Block block, Block previousBlock) {
        return two64.divide(BigInteger.valueOf(block.getBaseTarget()));
    }
}
