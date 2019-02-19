/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

public interface AccountService {
    Account getAccount(long id, int height);

    Account getAccount(long id);

    Account addOrGetAccount(long id);

    Account addOrGetAccount(long id, boolean isGenesis);

    boolean setOrVerify(long accountId, byte[] key);
}
