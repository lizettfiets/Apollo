/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;

import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

public class BlockApplyData {
    private final boolean resetRestoreTime;
    private final Set<Long> prunableTransactionIds;
    private final SortedSet<Transaction> blockTransactions;

    public BlockApplyData(boolean resetRestoreTime, Set<Long> prunableTransactionIds, SortedSet<Transaction> blockTransactions) {
        this.resetRestoreTime = resetRestoreTime;
        this.prunableTransactionIds = prunableTransactionIds;
        this.blockTransactions = blockTransactions;
    }

    public boolean isResetRestoreTime() {
        return resetRestoreTime;
    }

    public Set<Long> getPrunableTransactionIds() {
        return prunableTransactionIds;
    }

    public SortedSet<Transaction> getBlockTransactions() {
        return blockTransactions;
    }

    @Override
    public String toString() {
        return "BlockApplyData{" +
                "resetRestoreTime=" + resetRestoreTime +
                ", prunableTransactionIds=" + prunableTransactionIds +
                ", blockTransactions=" + blockTransactions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockApplyData)) return false;
        BlockApplyData that = (BlockApplyData) o;
        return resetRestoreTime == that.resetRestoreTime &&
                Objects.equals(prunableTransactionIds, that.prunableTransactionIds) &&
                Objects.equals(blockTransactions, that.blockTransactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resetRestoreTime, prunableTransactionIds, blockTransactions);
    }
}
