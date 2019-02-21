/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.Generator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;

import java.util.List;
import java.util.Map;

public interface ConsensusFacade {
    void setPreviousBlock(Block block, List<Block> prevBlocks);

    /**
     * Update generator data from the last block.
     */
    void updateGeneratorData(Generator generator, Block lastBlock);


    Block generateBlock(Generator generator, Block lastBlock, int currentTimeWithForgingDelay);

    Block generateGenesisBlock();

    List<Transaction> acceptBlock(Block block, List<Transaction> validPhasedTransactions, List<Transaction> invalidPhasedTransactions,
                                  Map<TransactionType, Map<String, Integer>> duplicates) throws BlockchainProcessor.TransactionNotAcceptedException;

    int compareGeneratorAndBlockTime(Generator generator, Block block, int curTime);
}

