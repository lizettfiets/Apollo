/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

public interface AccountService {
    Account getAccount(long accountId, int height);
}
