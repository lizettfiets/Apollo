/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TransactionValidatorImpl implements TransactionValidator {
    private static final int TRANSACTION_VERSION = 1;
    private static final Logger log = LoggerFactory.getLogger(TransactionValidatorImpl.class);
    private ReferencedTransactionVerifier referencedTransactionVerifier;
    private Blockchain blockchain;

    public TransactionValidatorImpl(ReferencedTransactionVerifier referencedTransactionVerifier, Blockchain blockchain) {
        this.referencedTransactionVerifier = referencedTransactionVerifier;
        this.blockchain = blockchain;
    }

    @Override
    public void validatePhasedTransactions(int height, List<Transaction> validPhasedTransactions, List<Transaction> invalidPhasedTransactions,
                                            Map<TransactionType, Map<String, Integer>> duplicates) {
        try (DbIterator<Transaction> phasedTransactions = PhasingPoll.getFinishingTransactions(height + 1)) {
            for (Transaction phasedTransaction : phasedTransactions) {
                if (PhasingPoll.getResult(phasedTransaction.getId()) != null) {
                    continue;
                }
                try {
                    phasedTransaction.validate();
                    if (!phasedTransaction.attachmentIsDuplicate(duplicates, false)) {
                        validPhasedTransactions.add(phasedTransaction);
                    } else {
                        log.debug("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " is duplicate, will not apply");
                        invalidPhasedTransactions.add(phasedTransaction);
                    }
                } catch (AplException.ValidationException e) {
                    log.debug("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " no longer passes validation: "
                            + e.getMessage() + ", will not apply");
                    invalidPhasedTransactions.add(phasedTransaction);
                }
            }
        }
    }
    @Override
    public void validateTransactions(Block block, Block previousLastBlock, int curTime, Map<TransactionType, Map<String, Integer>> duplicates,
                                      boolean fullValidation) throws BlockchainProcessor.BlockNotAcceptedException {
        long payloadLength = 0;
        long calculatedTotalAmount = 0;
        long calculatedTotalFee = 0;
        MessageDigest digest = Crypto.sha256();
        boolean hasPrunedTransactions = false;
        // assume, that block already have transactions
        for (Transaction transaction : block.getTransactions()) {
            if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
                throw new BlockchainProcessor.BlockOutOfOrderException("Invalid transaction timestamp: " + transaction.getTimestamp()
                        + ", current time is " + curTime, block);
            }
            if (!transaction.verifySignature()) {
                throw new BlockchainProcessor.TransactionNotAcceptedException("Transaction signature verification failed at height " + previousLastBlock.getHeight(), transaction);
            }
            if (fullValidation) {
                if (transaction.getTimestamp() > block.getTimestamp() + Constants.MAX_TIMEDRIFT
                        || transaction.getExpiration() < block.getTimestamp()) {
                    throw new BlockchainProcessor.TransactionNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
                            + ", current time is " + curTime + ", block timestamp is " + block.getTimestamp(), transaction);
                }
                if (blockchain.hasTransaction(transaction.getId(), previousLastBlock.getHeight())) {
                    throw new BlockchainProcessor.TransactionNotAcceptedException("Transaction is already in the blockchain", transaction);
                }
                if (transaction.referencedTransactionFullHash() != null && !referencedTransactionVerifier.hasAllReferencedTransactions(transaction,
                        transaction.getTimestamp(),
                        0)) {
                    throw new BlockchainProcessor.TransactionNotAcceptedException("Missing or invalid referenced transaction "
                            + transaction.getReferencedTransactionFullHash(), transaction);
                }
                if (transaction.getVersion() != TRANSACTION_VERSION) {
                    throw new BlockchainProcessor.TransactionNotAcceptedException("Invalid transaction version " + transaction.getVersion()
                            + " at height " + previousLastBlock.getHeight(), transaction);
                }
                if (transaction.getId() == 0L) {
                    throw new BlockchainProcessor.TransactionNotAcceptedException("Invalid transaction id 0", transaction);
                }
                try {
                    transaction.validate();
                } catch (AplException.ValidationException e) {
                    throw new BlockchainProcessor.TransactionNotAcceptedException(e.getMessage(), transaction);
                }
            }
            if (transaction.attachmentIsDuplicate(duplicates, true)) {
                throw new BlockchainProcessor.TransactionNotAcceptedException("Transaction is a duplicate", transaction);
            }
            if (!hasPrunedTransactions) {
                for (Appendix appendage : transaction.getAppendages()) {
                    if ((appendage instanceof Prunable) && !((Prunable)appendage).hasPrunableData()) {
                        hasPrunedTransactions = true;
                        break;
                    }
                }
            }
            calculatedTotalAmount += transaction.getAmountATM();
            calculatedTotalFee += transaction.getFeeATM();
            payloadLength += transaction.getFullSize();
            digest.update(transaction.getBytes());
        }
        if (calculatedTotalAmount != block.getTotalAmountATM() || calculatedTotalFee != block.getTotalFeeATM()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Total amount or fee don't match transaction totals", block);
        }
        if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Payload hash doesn't match", block);
        }
        if (hasPrunedTransactions ? payloadLength > block.getPayloadLength() : payloadLength != block.getPayloadLength()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Transaction payload length " + payloadLength + " does not match block payload length "
                    + block.getPayloadLength(), block);
        }
    }
}
