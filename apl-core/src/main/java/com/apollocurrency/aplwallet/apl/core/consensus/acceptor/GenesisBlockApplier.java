/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.acceptor;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.consensus.genesis.GenesisDataHolder;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("genesisBlockApplier")
public class GenesisBlockApplier implements BlockApplier {
    private static final Logger log = LoggerFactory.getLogger(GenesisBlockApplier.class);
    public static final String LOADING_STRING_PUB_KEYS = "Loading public keys %d / %d...";
    public static final String LOADING_STRING_GENESIS_BALANCE = "Loading genesis amounts %d / %d...";

    private GenesisDataHolder genesisDataHolder;
    private BlockchainConfig blockchainConfig;
    private DatabaseManager databaseManager;

    @Inject
    public GenesisBlockApplier(GenesisDataHolder genesisDataHolder, BlockchainConfig blockchainConfig, DatabaseManager databaseManager) {
        this.genesisDataHolder = genesisDataHolder;
        this.blockchainConfig = blockchainConfig;
        this.databaseManager = databaseManager;
    }
    @Override
    public void apply(Block block) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        int count = 0;
        JSONArray publicKeys = (JSONArray) genesisDataHolder.getAccountsJSON().get("publicKeys");
        String loadingPublicKeysString = "Loading public keys";
        log.debug("Loading public keys [{}]...", publicKeys.size());
        AppStatus.getInstance().update(loadingPublicKeysString + "...");
        for (Object jsonPublicKey : publicKeys) {
            byte[] publicKey = Convert.parseHexString((String)jsonPublicKey);
            Account account = Account.addOrGetAccount(Account.getId(publicKey), true);
            account.apply(publicKey, true);
            if (count++ % 100 == 0) {
                dataSource.commit(false);
            }
            if (publicKeys.size() > 20000 && count % 10000 == 0) {
                String message = String.format(LOADING_STRING_PUB_KEYS, count, publicKeys.size());
                log.debug(message);
                AppStatus.getInstance().update(message);
            }
        }
        log.debug("Loaded " + publicKeys.size() + " public keys");
        count = 0;
        JSONObject balances = (JSONObject) genesisDataHolder.getAccountsJSON().get("balances");
        String loadingAmountsString = "Loading genesis amounts";
        log.debug(loadingAmountsString);
        AppStatus.getInstance().update(loadingAmountsString + "...");
        long total = 0;
        for (Map.Entry<String, Long> entry : ((Map<String, Long>)balances).entrySet()) {
            Account account = Account.addOrGetAccount(Long.parseUnsignedLong(entry.getKey()), true);
            account.addToBalanceAndUnconfirmedBalanceATM(null, 0, entry.getValue());
            total += entry.getValue();
            if (count++ % 100 == 0) {
                dataSource.commit(false);
            }
            if (balances.size() > 10000 && count % 10000 == 0) {
                String message = String.format(LOADING_STRING_GENESIS_BALANCE, count, balances.size());
                log.debug(message);
                AppStatus.getInstance().update(message);
            }
        }
        long maxBalanceATM = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        if (total > maxBalanceATM) {
            throw new RuntimeException("Total balance " + total + " exceeds maximum allowed " + maxBalanceATM);
        }
        log.debug(String.format("Total balance %f %s", (double)total / Constants.ONE_APL, blockchainConfig.getCoinSymbol()));
        Account creatorAccount = Account.addOrGetAccount(genesisDataHolder.getGeneratorId(), true);
        creatorAccount.apply(genesisDataHolder.getGenesisPublicKey(), true);
        creatorAccount.addToBalanceAndUnconfirmedBalanceATM(null, 0, -total);
    }
}
