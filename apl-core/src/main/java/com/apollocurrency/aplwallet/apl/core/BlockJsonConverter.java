/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BlockJsonConverter {
    private BlockService service;

    @Inject
    public BlockJsonConverter(BlockService blockService) {
        this.service = blockService;
    }

    public JSONObject toJson(Block block) {
        JSONObject json = new JSONObject();
        json.put("version", block.getVersion());
        json.put("timestamp", block.getTimestamp());
        json.put("previousBlock", Long.toUnsignedString(block.getPreviousBlockId()));
        json.put("totalAmountATM", block.getTotalAmountATM());
        json.put("totalFeeATM", block.getTotalFeeATM());
        json.put("payloadLength", block.getPayloadLength());
        json.put("payloadHash", Convert.toHexString(block.getPayloadHash()));
        json.put("generatorPublicKey", Convert.toHexString(block.getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(block.getGenerationSignature()));
        json.put("previousBlockHash", Convert.toHexString(block.getPreviousBlockHash()));
        json.put("blockSignature", Convert.toHexString(block.getBlockSignature()));
        json.put("timeout", block.getTimeout());

        JSONArray transactionsData = new JSONArray();
        // retrieve transactions from db. Note that this is a temporal solution, in future releases transactions should be retrieved in proper
        // place.
        service.getTransactions(block).forEach(transaction -> transactionsData.add(transaction.getJSONObject()));
        json.put("transactions", transactionsData);
        return json;
    }
    public Block fromJson(JSONObject blockData) throws AplException.NotValidException {
//        try {
            int version = ((Long) blockData.get("version")).intValue();
            int timestamp = ((Long) blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            long totalAmountATM = blockData.containsKey("totalAmountATM") ? Convert.parseLong(blockData.get("totalAmountATM")) : Convert.parseLong(blockData.get("totalAmountNQT"));
            long totalFeeATM = blockData.containsKey("totalFeeATM") ? Convert.parseLong(blockData.get("totalFeeATM")) : Convert.parseLong(blockData.get("totalFeeNQT"));
            int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
            Object timeoutJsonValue = blockData.get("timeout");
            int timeout =  ((Long) timeoutJsonValue).intValue();
            List<Transaction> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(TransactionImpl.parseTransaction((JSONObject) transactionData));
            }
            BlockImpl block = new BlockImpl(version, timestamp, previousBlock, totalAmountATM, totalFeeATM, payloadLength, payloadHash, generatorPublicKey,
                    generationSignature, blockSignature, previousBlockHash, timeout, blockTransactions);
        return block;

    }


}
