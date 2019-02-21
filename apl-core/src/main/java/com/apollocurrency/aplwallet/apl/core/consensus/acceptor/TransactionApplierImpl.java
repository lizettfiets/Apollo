/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.acceptor;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionApplierImpl implements TransactionApplier {
    private AccountService accountService;
    private BlockchainConfig config;
    @Inject
    public TransactionApplierImpl(AccountService accountService, BlockchainConfig config) {
        this.accountService = accountService;
        this.config = config;
    }

    public void apply(Transaction transaction) {
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        senderAccount.apply(transaction.getSenderPublicKey());
        Account recipientAccount = null;
        long recipientId = transaction.getRecipientId();
        if (recipientId != 0) {
            recipientAccount = Account.getAccount(recipientId);
            if (recipientAccount == null) {
                recipientAccount = Account.addOrGetAccount(recipientId);
            }
        }
        if (transaction.getReferencedTransactionFullHash() != null) {
            senderAccount.addToUnconfirmedBalanceATM(transaction.getType().getLedgerEvent(), transaction.getId(),
                    0, config.getUnconfirmedPoolDepositAtm());
        }
        if (transaction.attachmentIsPhased()) {
            senderAccount.addToBalanceATM(transaction.getType().getLedgerEvent(), transaction.getId(), 0, -transaction.getFeeATM());
        }
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            if (!appendage.isPhased(transaction)) {
                appendage.loadPrunable(transaction);
                appendage.apply(transaction, senderAccount, recipientAccount);
            }
        }
    }
}
