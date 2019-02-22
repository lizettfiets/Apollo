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

import com.apollocurrency.aplwallet.apl.core.account.Account;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final long previousBlockId;
    private volatile byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final long totalAmountATM;
    private final long totalFeeATM;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private final int timeout;
    private volatile List<Transaction> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private long baseTarget; //blockchainConfig.getCurrentConfig().getInitialBaseTarget();
    private volatile long nextBlockId;
    private int height = -1;
    private volatile long id;
    private volatile String stringId = null;
    private volatile long generatorId;
    private volatile byte[] bytes = null;
    // check signature only ones and then assume that signature valid
    private volatile boolean hasValidSignature = false;

    // for genesis block
    public BlockImpl(byte[] generatorPublicKey, byte[] generationSignature, long baseTarget) {
        this(-1, 0, 0, 0, 0, 0, new byte[32], generatorPublicKey, generationSignature, new byte[64],
                new byte[32],  0, Collections.emptyList());
        this.height = 0;
        this.baseTarget = baseTarget;
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, int timeout,
              List<Transaction> transactions) {
        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        this.totalAmountATM = totalAmountATM;
        this.totalFeeATM = totalFeeATM;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;
        this.previousBlockHash = previousBlockHash;
        this.timeout = timeout;
        if (transactions != null) {
            this.blockTransactions = Collections.unmodifiableList(transactions);
        }
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength,
              byte[] payloadHash, long generatorId, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long nextBlockId, int height, long id, int timeout,
              List<Transaction> blockTransactions) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash,
                null, generationSignature, blockSignature, previousBlockHash, timeout, null);
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.baseTarget = baseTarget;
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
        this.generatorId = generatorId;
        this.blockTransactions = blockTransactions;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        if (generatorPublicKey == null) {
            generatorPublicKey = Account.getPublicKey(generatorId);
        }
        return generatorPublicKey;
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public long getTotalAmountATM() {
        return totalAmountATM;
    }

    @Override
    public long getTotalFeeATM() {
        return totalFeeATM;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<Transaction> getTransactions() {

        return this.blockTransactions;
    }

    @Override
    public void setTransactions(List<Transaction> transactions) {
        this.blockTransactions = transactions;
    }

    @Override
    public long getBaseTarget() {
        return baseTarget;
    }

    @Override
    public void setBaseTarget(long baseTarget) {
        this.baseTarget = baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    @Override
    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
    }

    @Override
    public long getNextBlockId() {
        return nextBlockId;
    }

    public void setNextBlockId(long nextBlockId) {
        this.nextBlockId = nextBlockId;
    }

    @Override
    public int getHeight() {
        if (height == -1) {
            throw new IllegalStateException("Block height not yet set");
        }
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
        this.stringId = Long.toUnsignedString(id);
    }

    @Override
    public String getStringId() {
        if (id == 0) {
            throw new RuntimeException("Block id not set yet!");
        }
        if (stringId == null) {
            stringId = Long.toUnsignedString(id);
        }
        return stringId;
    }

    @Override
    public long getGeneratorId() {
        if (generatorId == 0) {
            generatorId = Account.getId(getGeneratorPublicKey());
        }
        return generatorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId() == ((BlockImpl)o).getId();
    }

    @Override
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(bytes(), bytes.length);
    }

    @Override
    public boolean hasValidSignature() {
        return hasValidSignature;
    }

    @Override
    public void setHasValidSignature(boolean hasValidSignature) {
        this.hasValidSignature = hasValidSignature;
    }

    static boolean requireTimeout(int version) {
        return Block.ADAPTIVE_BLOCK_VERSION == version || Block.INSTANT_BLOCK_VERSION == version;
    }
    byte[] bytes() {
        if (bytes == null || blockSignature == null) {
            ByteBuffer buffer =
                    ByteBuffer.allocate(4 + 4 + 8 + 4 + 8 + 8 + 4 + 32 + 32 + 32 + 32 +
                            (requireTimeout(version) ? 4 : 0) + (blockSignature != null ? 64 :
                    0));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(version);
            buffer.putInt(timestamp);
            buffer.putLong(previousBlockId);
            buffer.putInt(getTransactions().size());
            buffer.putLong(totalAmountATM);
            buffer.putLong(totalFeeATM);
            buffer.putInt(payloadLength);
            buffer.put(payloadHash);
            buffer.put(getGeneratorPublicKey());
            buffer.put(generationSignature);
            buffer.put(previousBlockHash);
            if (requireTimeout(version)) {
                buffer.putInt(timeout);
            }
            if (blockSignature != null) {
                buffer.put(blockSignature);
            }
            bytes = buffer.array();
        }
        return bytes;
    }

    public void setBlockSignature(byte[] blockSignature) {
        this.blockSignature = blockSignature;
    }

    void loadTransactions() {
        for (Transaction transaction : getTransactions()) {
            ((TransactionImpl)transaction).bytes();
            transaction.getAppendages();
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("BlockImpl{");
        sb.append("version=").append(version);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", previousBlockId=").append(previousBlockId);
        sb.append(", totalAmountATM=").append(totalAmountATM);
        sb.append(", totalFeeATM=").append(totalFeeATM);
        sb.append(", timeout=").append(timeout);
        sb.append(", blockTransactions=[").append(blockTransactions != null ? blockTransactions.size() : -1);
        sb.append("], baseTarget=").append(baseTarget);
        sb.append(", nextBlockId=").append(nextBlockId);
        sb.append(", height=").append(height);
        sb.append(", stringId='").append(stringId).append('\'');
        sb.append(", generatorId=").append(generatorId);
        sb.append(", hasValidSignature=").append(hasValidSignature);
        sb.append('}');
        return sb.toString();
    }
}
