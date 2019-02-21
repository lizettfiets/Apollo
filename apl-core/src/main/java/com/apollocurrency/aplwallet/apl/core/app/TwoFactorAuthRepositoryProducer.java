/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthRepositoryImpl;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TwoFactorAuthRepositoryProducer {
    private boolean store2FAInFS;
    private DatabaseManager databaseManager;

    @Inject
    public TwoFactorAuthRepositoryProducer(
            DatabaseManager databaseManager,
            @Property(name = "apl.store2FAInFileSystem", defaultValue = "false") Boolean store2FAInFS
    ) {
        this.store2FAInFS = store2FAInFS;
        this.databaseManager = databaseManager;
    }

    @Produces
    public TwoFactorAuthRepository create2FARepository() {
        if (store2FAInFS) {
            return new TwoFactorAuthFileSystemRepository(AplCoreRuntime.getInstance().get2FADir());
        } else {
            // TODO WARNING!!! when sharding will be implemented, we should pass databaseManger instead of datasource
            return new TwoFactorAuthRepositoryImpl(databaseManager.getDataSource());
        }
    }
}
