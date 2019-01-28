/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;

import java.util.List;

public interface BlockService {
    List<Transaction> getTransactions(Block block);
}
