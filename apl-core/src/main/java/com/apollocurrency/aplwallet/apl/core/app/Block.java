/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import java.math.BigInteger;
import java.util.List;

public interface Block {
    int LEGACY_BLOCK_VERSION   = 3;
    int REGULAR_BLOCK_VERSION  = 4;
    int INSTANT_BLOCK_VERSION  = 5;
    int ADAPTIVE_BLOCK_VERSION = 6;

    int getVersion();

    long getId();

    void setId(long id);

    String getStringId();

    int getHeight();

    void setHeight(int height);

    int getTimestamp();

    long getGeneratorId();

    byte[] getGeneratorPublicKey();

    long getPreviousBlockId();

    byte[] getPreviousBlockHash();

    long getNextBlockId();

    void setNextBlockId(long nextBlockId);

    long getTotalAmountATM();

    long getTotalFeeATM();

    int getPayloadLength();

    byte[] getPayloadHash();
    // should not retrieve data from db
    List<Transaction> getTransactions();

    void setTransactions(List<Transaction> transactions);

    byte[] getGenerationSignature();

    byte[] getBlockSignature();

    long getBaseTarget();

    void setBaseTarget(long baseTarget);

    BigInteger getCumulativeDifficulty();

    void setCumulativeDifficulty(BigInteger cumulativeDifficulty);

    byte[] getBytes();

    boolean hasValidSignature();

    void setHasValidSignature(boolean hasValidSignature);

    int getTimeout();
}
