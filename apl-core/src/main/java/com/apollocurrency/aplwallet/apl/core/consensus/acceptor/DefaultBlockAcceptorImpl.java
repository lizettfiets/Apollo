/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.acceptor;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class DefaultBlockAcceptorImpl implements BlockAcceptor {
    private static final Logger log = LoggerFactory.getLogger(DefaultBlockAcceptorImpl.class);
    private static final Comparator<Transaction> finishingTransactionsComparator = Comparator
            .comparingInt(Transaction::getHeight)
            .thenComparingInt(Transaction::getIndex)
            .thenComparingLong(Transaction::getId);

    private BlockApplier defaultBlockApplier;
    private BlockApplier genesisBlockApplier;
    private TransactionApplier transactionApplier;
    private Blockchain blockchain;

    @Inject
    public DefaultBlockAcceptorImpl(
                                    @Named("defaultBlockApplier")BlockApplier defaultBlockApplier,
                                    @Named("genesisBlockApplier") BlockApplier genesisBlockApplier,
                                    Blockchain blockchain,
                                    TransactionApplier transactionApplier) {
        this.defaultBlockApplier = defaultBlockApplier;
        this.genesisBlockApplier = genesisBlockApplier;
        this.blockchain = blockchain;
        this.transactionApplier = transactionApplier;
    }

    @Override
    public List<Transaction> accept(Block block, List<Transaction> validPhasedTransactions, List<Transaction> invalidPhasedTransactions,
                                    Map<TransactionType, Map<String, Integer>> duplicates) throws BlockchainProcessor.TransactionNotAcceptedException {
        if (block.getHeight() == 0) {
            genesisBlockApplier.apply(block);
            return Collections.emptyList(); // no transactions at genesis block
        }
        for (Transaction transaction : block.getTransactions()) { //assume that block has transactions
            if (!((TransactionImpl) transaction).applyUnconfirmed()) {
                throw new BlockchainProcessor.TransactionNotAcceptedException("Double spending", transaction);
            }
        }
        defaultBlockApplier.apply(block);
        validPhasedTransactions.forEach(transaction -> transaction.getPhasing().countVotes(transaction));
        invalidPhasedTransactions.forEach(transaction -> transaction.getPhasing().reject(transaction));
        for (Transaction transaction : block.getTransactions()) {
            try {
                transactionApplier.apply(transaction);
            }
            catch (RuntimeException e) {
                log.error(e.toString(), e);
                throw new BlockchainProcessor.TransactionNotAcceptedException(e, transaction);
            }
        }
        SortedSet<Transaction> possiblyApprovedTransactions = new TreeSet<>(finishingTransactionsComparator);
        block.getTransactions().forEach(tx -> {
            PhasingPoll.getLinkedPhasedTransactions(tx.getFullHash()).forEach(phasedTransaction -> {
                if (phasedTransaction.getPhasing().getFinishHeight() > block.getHeight()) {
                    possiblyApprovedTransactions.add(phasedTransaction);
                }
            });
            if (tx.getType() == Messaging.PHASING_VOTE_CASTING && !tx.attachmentIsPhased()) {
                MessagingPhasingVoteCasting voteCasting = (MessagingPhasingVoteCasting) tx.getAttachment();
                voteCasting.getTransactionFullHashes().forEach(hash -> {
                    PhasingPoll phasingPoll = PhasingPoll.getPoll(Convert.fullHashToId(hash));
                    if (phasingPoll.allowEarlyFinish() && phasingPoll.getFinishHeight() > block.getHeight()) {
                        possiblyApprovedTransactions.add(blockchain.getTransaction(phasingPoll.getId()));
                    }
                });
            }
        });
        validPhasedTransactions.forEach(phasedTransaction -> {
            if (phasedTransaction.getType() == Messaging.PHASING_VOTE_CASTING) {
                PhasingPoll.PhasingPollResult result = PhasingPoll.getResult(phasedTransaction.getId());
                if (result != null && result.isApproved()) {
                    MessagingPhasingVoteCasting phasingVoteCasting = (MessagingPhasingVoteCasting) phasedTransaction.getAttachment();
                    phasingVoteCasting.getTransactionFullHashes().forEach(hash -> {
                        PhasingPoll phasingPoll = PhasingPoll.getPoll(Convert.fullHashToId(hash));
                        if (phasingPoll.allowEarlyFinish() && phasingPoll.getFinishHeight() > block.getHeight()) {
                            possiblyApprovedTransactions.add(blockchain.getTransaction(phasingPoll.getId()));
                        }
                    });
                }
            }
        });
        possiblyApprovedTransactions.forEach(tx -> {
            if (PhasingPoll.getResult(tx.getId()) == null) {
                try {
                    tx.validate();
                    tx.getPhasing().tryCountVotes(tx, duplicates);
                }
                catch (AplException.ValidationException e) {
                    log.debug("At height " + block.getHeight() + " phased transaction " + tx.getStringId()
                            + " no longer passes validation: " + e.getMessage() + ", cannot finish early");
                }
            }
        });
        return block.getTransactions();
    }
}
