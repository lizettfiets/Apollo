/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.FilteringIterator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.enterprise.inject.spi.CDI;

public class UnconfirmedTransactionServiceImpl implements UnconfirmedTransactionService {
    private static final Comparator<UnconfirmedTransaction> transactionArrivalComparator = Comparator
            .comparingLong(UnconfirmedTransaction::getArrivalTimestamp)
            .thenComparingInt(UnconfirmedTransaction::getHeight)
            .thenComparingLong(UnconfirmedTransaction::getId);

    private static final int TRANSACTION_VERSION = 1;
    public static final int MAX_REFERENCED_TRANSACTIONS = 10;

    private BlockchainConfig blockchainConfig;
    private TransactionProcessor transactionProcessor;
    private Blockchain blockchain;

    public UnconfirmedTransactionServiceImpl(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
    }

    public SortedSet<UnconfirmedTransaction> getUnconfirmedTransactions(Block previousBlock, int blockTimestamp) {
        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        try (DbIterator<Transaction> phasedTransactions = PhasingPoll.getFinishingTransactions(lookupBlockhain().getHeight() + 1)) {
            for (Transaction phasedTransaction : phasedTransactions) {
                try {
                    phasedTransaction.validate();
                    phasedTransaction.attachmentIsDuplicate(duplicates, false); // pre-populate duplicates map
                } catch (AplException.ValidationException ignore) {
                }
            }
        }
//        validate and insert in unconfirmed_transaction db table all waiting transaction
        lookupTransactionProcessor().processWaitingTransactions();
        SortedSet<UnconfirmedTransaction> sortedTransactions = selectUnconfirmedTransactions(duplicates, previousBlock, blockTimestamp);
        return sortedTransactions;
    }

    public SortedSet<UnconfirmedTransaction> selectUnconfirmedTransactions(
            Map<TransactionType, Map<String, Integer>> duplicates, Block previousBlock, int blockTimestamp) {

        List<UnconfirmedTransaction> orderedUnconfirmedTransactions = new ArrayList<>();
        DbIterator<UnconfirmedTransaction> allUnconfirmedTransactions = lookupTransactionProcessor().getAllUnconfirmedTransactions();
        try (FilteringIterator<UnconfirmedTransaction> unconfirmedTransactions = new FilteringIterator<>(
                allUnconfirmedTransactions,
                transaction -> hasAllReferencedTransactions(
                        transaction.getTransaction(),
                        transaction.getTimestamp(), 0))) {
            for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                orderedUnconfirmedTransactions.add(unconfirmedTransaction);
            }
        }
        SortedSet<UnconfirmedTransaction> sortedTransactions = new TreeSet<>(transactionArrivalComparator);
        int payloadLength = 0;
        int maxPayloadLength = blockchainConfig.getCurrentConfig().getMaxPayloadLength();
        while (payloadLength <= maxPayloadLength && sortedTransactions.size() <= blockchainConfig.getCurrentConfig().getMaxNumberOfTransactions()) {
            int prevNumberOfNewTransactions = sortedTransactions.size();
            for (UnconfirmedTransaction unconfirmedTransaction : orderedUnconfirmedTransactions) {
                int transactionLength = unconfirmedTransaction.getTransaction().getFullSize();
                if (sortedTransactions.contains(unconfirmedTransaction) || payloadLength + transactionLength > maxPayloadLength) {
                    continue;
                }
                if (unconfirmedTransaction.getVersion() != TRANSACTION_VERSION) {
                    continue;
                }
                if (blockTimestamp > 0 && (unconfirmedTransaction.getTimestamp() > blockTimestamp + Constants.MAX_TIMEDRIFT
                        || unconfirmedTransaction.getExpiration() < blockTimestamp)) {
                    continue;
                }
                try {
                    unconfirmedTransaction.getTransaction().validate();
                } catch (AplException.ValidationException e) {
                    continue;
                }
                if (unconfirmedTransaction.getTransaction().attachmentIsDuplicate(duplicates, true)) {
                    continue;
                }
                sortedTransactions.add(unconfirmedTransaction);
                payloadLength += transactionLength;
            }
            if (sortedTransactions.size() == prevNumberOfNewTransactions) {
                break;
            }
        }
        return sortedTransactions;
    }
    boolean hasAllReferencedTransactions(Transaction transaction, int timestamp, int count) {
        if (transaction.referencedTransactionFullHash() == null) {
            return timestamp - transaction.getTimestamp() < Constants.MAX_REFERENCED_TRANSACTION_TIMESPAN && count < MAX_REFERENCED_TRANSACTIONS;
        }
        Transaction referencedTransaction = lookupBlockhain().findTransactionByFullHash(transaction.referencedTransactionFullHash());
        return referencedTransaction != null
                && referencedTransaction.getHeight() < transaction.getHeight()
                && hasAllReferencedTransactions(referencedTransaction, timestamp, count + 1);
    }
    private TransactionProcessor lookupTransactionProcessor() {
        if (transactionProcessor == null) transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
        return transactionProcessor;
    }

    private Blockchain lookupBlockhain() {
        if (blockchain == null) blockchain = CDI.current().select(BlockchainImpl.class).get();
        return blockchain;
    }

}
