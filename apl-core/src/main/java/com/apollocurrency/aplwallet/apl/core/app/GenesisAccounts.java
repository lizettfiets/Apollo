/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

/**
 *
 * @author al
 */
@Singleton
public class GenesisAccounts {
    private static List<Map.Entry<String, Long>> initialGenesisAccountsBalances;

    public static void init () {
        initialGenesisAccountsBalances = loadGenesisAccounts();
    }
    public static List<Map.Entry<String, Long>> loadGenesisAccounts() {
        String path = "conf/"+ CDI.current().select(BlockchainConfig.class).get().getChain().getGenesisLocation();
        try (InputStreamReader is = new InputStreamReader(
                GenesisAccounts.class.getClassLoader().getResourceAsStream(path))) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(is);
            JsonNode balancesArray = root.get("balances");
            Map<String, Long> map = objectMapper.readValue(balancesArray.toString(), new TypeReference<Map<String, Long>>(){});

            return map.entrySet()
                    .stream()
                    .sorted((o1, o2) -> Long.compare(o2.getValue(), o1.getValue()))
                    .skip(1) //skip first account to collect only genesis accounts
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load genesis accounts", e);
        }
    }
 
    public static List<Map.Entry<String, Long>> getGenesisBalances(int firstIndex, int lastIndex) {
        firstIndex = Math.max(firstIndex, 0);
        lastIndex = Math.max(lastIndex, 0);
        if (lastIndex < firstIndex) {
            throw new IllegalArgumentException("firstIndex should be less or equal lastIndex ");
        }
        if (firstIndex >= initialGenesisAccountsBalances.size() || lastIndex > initialGenesisAccountsBalances.size()) {
            throw new IllegalArgumentException("firstIndex and lastIndex should be less than " + initialGenesisAccountsBalances.size());
        }
        if (lastIndex - firstIndex > 99) {
            lastIndex = firstIndex + 99;
        }
        return initialGenesisAccountsBalances.subList(firstIndex, lastIndex + 1);
    }

    public static int getGenesisBalancesNumber() {
        return initialGenesisAccountsBalances.size();
    }
      
}
