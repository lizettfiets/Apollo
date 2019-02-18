/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;

import java.util.Map;
import java.util.SortedSet;

public interface UnconfirmedTransactionService {
    SortedSet<UnconfirmedTransaction> selectUnconfirmedTransactions(Map<TransactionType, Map<String, Integer>> duplicates, Block previousBlock, int blockTimestamp);

    SortedSet<UnconfirmedTransaction> getUnconfirmedTransactions(Block previousBlock, int blockTimestamp);
}
