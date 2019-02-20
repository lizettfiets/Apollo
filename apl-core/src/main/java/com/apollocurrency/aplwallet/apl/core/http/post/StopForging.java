/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.consensus.forging.BlockGenerator;
import com.apollocurrency.aplwallet.apl.core.consensus.forging.Generator;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;


public class StopForging extends AbstractAPIRequestHandler {

    private static class StopForgingHolder {
        private static final StopForging INSTANCE = new StopForging();
    }

    public static StopForging getInstance() {
        return StopForgingHolder.INSTANCE;
    }

    private StopForging() {
        super(new APITag[] {APITag.FORGING}, "secretPhrase", "adminPassword");
    }

    private final BlockGenerator blockGenerator = CDI.current().select(BlockGenerator.class).get();
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long accountId = ParameterParser.getAccountId(req, vaultAccountName(), false);
        byte[] keySeed = ParameterParser.getKeySeed(req, accountId, false);
        JSONObject response = new JSONObject();
        if (keySeed != null) {
            Generator generator = blockGenerator.stopGeneration(new Generator(keySeed));
            response.put("foundAndStopped", generator != null);
        } else {
            API.verifyPassword(req);
            int count = blockGenerator.stopAll();
            response.put("stopped", count);
        }
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }


    @Override
    protected boolean is2FAProtected() {
        return true;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}
