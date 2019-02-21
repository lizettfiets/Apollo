/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.acceptor;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockDao;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("defaultBlockApplier")
public class DefaultBlockApplierImpl implements BlockApplier {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultBlockApplierImpl.class);

    private BlockDao blockDao;
    private AccountService accountService;

    @Inject
    public DefaultBlockApplierImpl(BlockDao blockDao, AccountService accountService) {
        this.blockDao = blockDao;
        this.accountService = accountService;
    }

    public void apply(Block block) {
        Account generatorAccount = accountService.addOrGetAccount(block.getGeneratorId());
        generatorAccount.apply(block.getGeneratorPublicKey());
        long totalBackFees = 0;
        if (block.getHeight() > 3) {
            long[] backFees = new long[3];
            for (Transaction transaction : block.getTransactions()) {
                long[] fees = transaction.getBackFees();
                for (int i = 0; i < fees.length; i++) {
                    backFees[i] += fees[i];
                }
            }
            for (int i = 0; i < backFees.length; i++) {
                if (backFees[i] == 0) {
                    break;
                }
                totalBackFees += backFees[i];
                Account previousGeneratorAccount = accountService.getAccount(blockDao.findBlockAtHeight(block.getHeight() - i - 1).getGeneratorId());
                LOG.debug("Back fees {} coins to forger at height {}", ((double)backFees[i])/ Constants.ONE_APL,
                        block.getHeight() - i - 1);
                previousGeneratorAccount.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.BLOCK_GENERATED, block.getId(), backFees[i]);
                previousGeneratorAccount.addToForgedBalanceATM(backFees[i]);
            }
        }
        if (totalBackFees != 0) {
            LOG.debug("Fee reduced by {} coins at height {}", ((double)totalBackFees)/Constants.ONE_APL, block.getHeight());
        }
        generatorAccount.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.BLOCK_GENERATED, block.getId(), block.getTotalFeeATM() - totalBackFees);
        generatorAccount.addToForgedBalanceATM(block.getTotalFeeATM() - totalBackFees);
    }
}
