/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import javax.inject.Singleton;

@Singleton
public class AccountServiceImpl implements AccountService {
    @Override
    public Account getAccount(long accountId, int height) {
        return Account.getAccount(accountId, height);
    }

    @Override
    public Account getAccount(long id) {
        return Account.getAccount(id);
    }

    @Override
    public Account addOrGetAccount(long id) {
        return Account.addOrGetAccount(id);
    }

    @Override
    public Account addOrGetAccount(long id, boolean isGenesis) {
        return Account.addOrGetAccount(id, isGenesis);
    }

    @Override
    public boolean setOrVerify(long accountId, byte[] key) {
        return Account.setOrVerify(accountId, key);
    }
}
