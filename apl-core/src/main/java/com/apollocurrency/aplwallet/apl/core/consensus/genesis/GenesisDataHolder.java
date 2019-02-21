/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.genesis;

import org.json.simple.JSONObject;

import java.util.Arrays;

public class GenesisDataHolder {
    private final byte[] genesisPublicKey;
    private final long generatorId;
    private final long epochBeginning;
    private final JSONObject accountsJSON;
    private final byte[] accountBytes;

    public GenesisDataHolder(byte[] genesisPublicKey, long creatorId, long epochBeginning, JSONObject accountsJSON, byte[] accountBytes) {
        this.genesisPublicKey = genesisPublicKey;
        this.generatorId = creatorId;
        this.epochBeginning = epochBeginning;
        this.accountsJSON = accountsJSON;
        this.accountBytes = accountBytes;
    }

    public byte[] getGenesisPublicKey() {
        return Arrays.copyOf(genesisPublicKey, genesisPublicKey.length);
    }

    public long getGeneratorId() {
        return generatorId;
    }

    public long getEpochBeginning() {
        return epochBeginning;
    }

    public JSONObject getAccountsJSON() {
        return accountsJSON;
    }

    public byte[] getAccountBytes() {
        return Arrays.copyOf(accountBytes, accountBytes.length);
    }
}
