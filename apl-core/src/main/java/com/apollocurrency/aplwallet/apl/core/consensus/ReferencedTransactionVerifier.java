/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.util.Constants;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReferencedTransactionVerifier {
    private static final int TRANSACTION_VERSION = 1;
    public static final int MAX_REFERENCED_TRANSACTIONS = 10;
    private Blockchain blockchain;

    @Inject
    public ReferencedTransactionVerifier(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public boolean hasAllReferencedTransactions(Transaction transaction, int timestamp, int count) {
        if (transaction.referencedTransactionFullHash() == null) {
            return timestamp - transaction.getTimestamp() < Constants.MAX_REFERENCED_TRANSACTION_TIMESPAN && count < MAX_REFERENCED_TRANSACTIONS;
        }
        Transaction referencedTransaction = blockchain.findTransactionByFullHash(transaction.referencedTransactionFullHash());
        return referencedTransaction != null
                && referencedTransaction.getHeight() < transaction.getHeight()
                && hasAllReferencedTransactions(referencedTransaction, timestamp, count + 1);
    }

}
