/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.validator;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;

import java.util.List;
import java.util.Map;

public interface TransactionValidator {
    void validatePhasedTransactions(int height, List<Transaction> validPhasedTransactions, List<Transaction> invalidPhasedTransactions,
                                    Map<TransactionType, Map<String, Integer>> duplicates);

    void validateTransactions(Block block, Block previousLastBlock, int curTime, Map<TransactionType, Map<String, Integer>> duplicates,
                              boolean fullValidation) throws BlockchainProcessor.BlockNotAcceptedException;
}
