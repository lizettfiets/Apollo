/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.acceptor;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;

public interface TransactionApplier {
    void apply(Transaction transaction);
}
