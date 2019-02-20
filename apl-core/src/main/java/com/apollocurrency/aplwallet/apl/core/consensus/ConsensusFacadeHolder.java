/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.BlockGenerationAlgoProvider;
import com.apollocurrency.aplwallet.apl.core.transaction.UnconfirmedTransactionService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ConsensusFacadeHolder {
    // cached values for better performance
    private ConsensusFacade defaultFacade;
    private ConsensusFacade diffAdjustingFacade;
    private int diffAlgo;

    private final BlockchainConfig blockchainConfig;
    private final BlockAlgoProvider newBlockAlgoProvider;
    private final BlockAlgoProvider oldBlockAlgoProvider;
    private final BlockGenerationAlgoProvider blockGenerationAlgoProvider;
    private final UnconfirmedTransactionService unconfirmedTransactionService;
    private final AccountService accountService;
    private final BlockAcceptor blockAcceptor;

    @Inject
    public ConsensusFacadeHolder(BlockchainConfig blockchainConfig,
                                 @Named("newBlockAlgoProvider") BlockAlgoProvider newBlockAlgoProvider,
                                 @Named("oldBlockAlgoProvider") BlockAlgoProvider oldBlockAlgoProvider,
                                 BlockGenerationAlgoProvider blockGenerationAlgoProvider, UnconfirmedTransactionService unconfirmedTransactionService, AccountService accountService, BlockAcceptor blockAcceptor) {
        this.blockchainConfig = blockchainConfig;
        this.newBlockAlgoProvider = newBlockAlgoProvider;
        this.oldBlockAlgoProvider = oldBlockAlgoProvider;
        this.blockGenerationAlgoProvider = blockGenerationAlgoProvider;
        this.unconfirmedTransactionService = unconfirmedTransactionService;
        this.accountService = accountService;
        this.blockAcceptor = blockAcceptor;
        this.diffAlgo = -1;
    }

    public ConsensusFacade getConsensusFacade() {
        int currentDiffAlgo = blockchainConfig.getCurrentConfig().getDiffAdjustingAlgo();
        if (currentDiffAlgo == 1) {
            if (diffAdjustingFacade == null || diffAlgo != 1) {
                diffAlgo = 1;
                diffAdjustingFacade = new RegBlockExtrapolationFacade(
                        blockchainConfig,
                        newBlockAlgoProvider,
                        blockGenerationAlgoProvider,
                        accountService,
                        unconfirmedTransactionService,
                        blockAcceptor);
            }
            return diffAdjustingFacade;
        } else if (currentDiffAlgo == 0) {
            if (defaultFacade == null || diffAlgo != 0) {
                diffAlgo = 0;
                defaultFacade = new DefaultConsensusFacade(
                        blockchainConfig,
                        oldBlockAlgoProvider,
                        blockGenerationAlgoProvider,
                        accountService,
                        unconfirmedTransactionService,
                        blockAcceptor);
            }
            return defaultFacade;
        } else {
            throw new RuntimeException("Unable to create consensus facade for diff algo = " + currentDiffAlgo);
        }
    }
}
