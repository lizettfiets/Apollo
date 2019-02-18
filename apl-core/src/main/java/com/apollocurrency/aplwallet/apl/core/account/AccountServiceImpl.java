/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

public class AccountServiceImpl implements AccountService {
    @Override
    public Account getAccount(long accountId, int height) {
        return Account.getAccount(accountId, height);
    }
}
