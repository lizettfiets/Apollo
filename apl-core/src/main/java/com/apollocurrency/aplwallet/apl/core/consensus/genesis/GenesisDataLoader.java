/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.genesis;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Pair;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GenesisDataLoader {
    private BlockchainConfig blockchainConfig;

    @Inject
    public GenesisDataLoader(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
    }

    @Produces
    public GenesisDataHolder loadGenesisData() {
        byte[] genesisPublicKey;
        long generatorId;
        long epochBeginning;
        try (InputStream is = ClassLoader.getSystemResourceAsStream("conf/data/genesisParameters.json")) {
            JSONObject genesisParameters = (JSONObject)JSONValue.parseWithException(new InputStreamReader(is));
            genesisPublicKey = Convert.parseHexString((String)genesisParameters.get("genesisPublicKey"));
            generatorId = Account.getId(genesisPublicKey);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            epochBeginning = dateFormat.parse((String) genesisParameters.get("epochBeginning")).getTime();
        } catch (ParseException|IOException|java.text.ParseException e) {
            throw new RuntimeException("Failed to load genesis parameters", e);
        }
        Pair<byte[], JSONObject> accountsBytesAndJsonPair = loadGenesisAccounts(epochBeginning);
        return new GenesisDataHolder(genesisPublicKey, generatorId, epochBeginning, accountsBytesAndJsonPair.getSecond(), accountsBytesAndJsonPair.getFirst());
    }

    // retrieve pair of account parameters for better performance
     Pair<byte[],JSONObject> loadGenesisAccounts(long epochBeginning) {
        MessageDigest digest = Crypto.sha256();
        String path = "conf/"+blockchainConfig.getChain().getGenesisLocation();
        JSONObject genesisAccountsJSON;
        try (InputStreamReader is = new InputStreamReader(new DigestInputStream(
                ClassLoader.getSystemResourceAsStream(path), digest))) {
            genesisAccountsJSON = (JSONObject) JSONValue.parseWithException(is);
        } catch (ParseException | IOException e) {
            throw new RuntimeException("Failed to process genesis recipients accounts", e);
        }
        // we should leave here '0' to create correct genesis block for already launched mainnet
        digest.update((byte)(0));
        // also required for correct genesis block
        digest.update(Convert.toBytes(epochBeginning));
        return new Pair<>(digest.digest(), genesisAccountsJSON);
    }
}
