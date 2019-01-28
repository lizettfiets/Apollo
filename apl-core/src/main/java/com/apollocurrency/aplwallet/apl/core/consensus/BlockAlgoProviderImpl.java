/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;


import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockDao;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.math.BigInteger;
import java.util.Objects;
import javax.inject.Inject;

public class BlockAlgoProviderImpl implements BlockAlgoProvider {

    private static final BigInteger two64 = new BigInteger("18446744073709551616");
    private BlockchainConfig blockchainConfig;
    private BlockDao blockDao;

    @Inject
    public BlockAlgoProviderImpl(BlockchainConfig blockchainConfig, BlockDao blockDao) {
        Objects.requireNonNull(blockchainConfig, "Blockchain config should not be null");
        Objects.requireNonNull(blockchainConfig, "BlockDao should not be null");
        this.blockchainConfig = blockchainConfig;
        this.blockDao = blockDao;
    }

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

//    @Override
//    public void setPreviousBlock(Block block, Block previousBlock) {
//        block.setHeight(previousBlock.getHeight() + 1);
//        long baseTarget = calculateBaseTarget(block, previousBlock);
//        block.setBaseTarget(baseTarget);
//        BigInteger cumulativeDifficulty = calculateCumulativeDifficulty(block, previousBlock);
//        block.setCumulativeDifficulty(cumulativeDifficulty);
//    }

    @Override
    public long calculateBaseTarget(Block block, Block previousBlock) {
        long prevBaseTarget = previousBlock.getBaseTarget();
        int blockchainHeight = previousBlock.getHeight();
        long calculatedBaseTarget;
        if (blockchainHeight > 2 && blockchainHeight % 2 == 0) {
            long blocktimeAverage = getBlockTimeAverage(block, previousBlock);
            HeightConfig config = blockchainConfig.getCurrentConfig();
            int blockTime = config.getBlockTime();
            if (blocktimeAverage > blockTime) {
                int maxBlocktimeLimit = config.getMaxBlockTimeLimit();
                calculatedBaseTarget = ((prevBaseTarget * Math.min(blocktimeAverage, maxBlocktimeLimit)) / blockTime);

            } else {
                int minBlocktimeLimit = config.getMinBlockTimeLimit();
                calculatedBaseTarget = (prevBaseTarget - prevBaseTarget * Constants.BASE_TARGET_GAMMA
                        * (blockTime - Math.max(blocktimeAverage, minBlocktimeLimit)) / (100 * blockTime));
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
    public BigInteger calculateCumulativeDifficulty(Block block, Block previousBlock) {
        return previousBlock.getCumulativeDifficulty().add(two64.divide(BigInteger.valueOf(block.getBaseTarget())));
    }

    @Override
    public long getBlockTimeAverage(Block block, Block previousBlock) {
        int blockchainHeight = previousBlock.getHeight();
        Block lastBlockForTimeAverage = blockDao.findBlockAtHeight(blockchainHeight - 2);
        if (block.getVersion() != Block.LEGACY_BLOCK_VERSION) {
            Block intermediateBlockForTimeAverage = blockDao.findBlockAtHeight(blockchainHeight - 1);
            int thisBlockActualTime = block.getTimestamp() - previousBlock.getTimestamp() - block.getTimeout();
            int previousBlockTime = previousBlock.getTimestamp() - previousBlock.getTimeout() - intermediateBlockForTimeAverage.getTimestamp();
            int secondAvgBlockTime = intermediateBlockForTimeAverage.getTimestamp()
                    - intermediateBlockForTimeAverage.getTimeout() - lastBlockForTimeAverage.getTimestamp();
            return  (thisBlockActualTime + previousBlockTime + secondAvgBlockTime) / 3;
        } else {
            return (block.getTimestamp() - lastBlockForTimeAverage.getTimestamp()) / 3;
        }
    }

}
