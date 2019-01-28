/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core;

import com.apollocurrency.aplwallet.apl.core.app.Account;
import com.apollocurrency.aplwallet.apl.core.app.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockDao;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockApplierImpl {
    private static final Logger LOG = LoggerFactory.getLogger(BlockApplierImpl.class);

    private BlockDao blockDao;

    public BlockApplierImpl(BlockDao blockDao) {
        this.blockDao = blockDao;
    }

    public void apply(Block block) {
        Account generatorAccount = Account.addOrGetAccount(block.getGeneratorId());
        generatorAccount.apply(block.getGeneratorPublicKey());
        long totalBackFees = 0;
        if (block.getHeight() > 3) {
            long[] backFees = new long[3];
            for (Transaction transaction : block.getTransactions()) {
                long[] fees = ((TransactionImpl)transaction).getBackFees();
                for (int i = 0; i < fees.length; i++) {
                    backFees[i] += fees[i];
                }
            }
            for (int i = 0; i < backFees.length; i++) {
                if (backFees[i] == 0) {
                    break;
                }
                totalBackFees += backFees[i];
                Account previousGeneratorAccount = Account.getAccount(blockDao.findBlockAtHeight(block.getHeight() - i - 1).getGeneratorId());
                LOG.debug("Back fees {} coins to forger at height {}", ((double)backFees[i])/ Constants.ONE_APL,
                        block.getHeight() - i - 1);
                previousGeneratorAccount.addToBalanceAndUnconfirmedBalanceATM(AccountLedger.LedgerEvent.BLOCK_GENERATED, block.getId(), backFees[i]);
                previousGeneratorAccount.addToForgedBalanceATM(backFees[i]);
            }
        }
        if (totalBackFees != 0) {
            LOG.debug("Fee reduced by {} coins at height {}", ((double)totalBackFees)/Constants.ONE_APL, block.getHeight());
        }
        generatorAccount.addToBalanceAndUnconfirmedBalanceATM(AccountLedger.LedgerEvent.BLOCK_GENERATED, block.getId(), block.getTotalFeeATM() - totalBackFees);
        generatorAccount.addToForgedBalanceATM(block.getTotalFeeATM() - totalBackFees);
    }
}
