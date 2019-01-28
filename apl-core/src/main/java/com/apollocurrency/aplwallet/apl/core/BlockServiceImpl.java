/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDao;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class BlockServiceImpl implements BlockService {
    private TransactionDao transactionDao;

    @Inject
    public BlockServiceImpl(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    @Override
    public List<Transaction> getTransactions(Block block) {
        if (block.getTransactions() == null) {
            List<Transaction> transactions = Collections.unmodifiableList(transactionDao.findBlockTransactions(block.getId()));
            for (Transaction transaction : transactions) {
                transaction.setBlock(block);
            }
            block.setTransactions(transactions);
        }
        return block.getTransactions();
    }
}
