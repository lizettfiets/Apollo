/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

@Singleton
public class PrunableTransactionsStore {
    private final Set<Long> prunableTransactionIds = ConcurrentHashMap.newKeySet();

    public PrunableTransactionsStore() {}

    public void add(Long id) {
        prunableTransactionIds.add(id);
    }

    public boolean remove(Long id) {
        return prunableTransactionIds.remove(id);
    }

    public int size() {
        return prunableTransactionIds.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Set<Long> copy() {
        return new HashSet<>(prunableTransactionIds);
    }

}
