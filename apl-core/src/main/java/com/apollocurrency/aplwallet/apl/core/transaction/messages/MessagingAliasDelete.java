/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MessagingAliasDelete extends AbstractAttachment {
    
    final String aliasName;

    public MessagingAliasDelete(final ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public MessagingAliasDelete(final JSONObject attachmentData) {
        super(attachmentData);
        this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
    }

    public MessagingAliasDelete(final String aliasName) {
        this.aliasName = aliasName;
    }

    @Override
    public TransactionType getTransactionType() {
        return Messaging.ALIAS_DELETE;
    }

    @Override
    int getMySize() {
        return 1 + Convert.toBytes(aliasName).length;
    }

    @Override
    void putMyBytes(final ByteBuffer buffer) {
        byte[] aliasBytes = Convert.toBytes(aliasName);
        buffer.put((byte) aliasBytes.length);
        buffer.put(aliasBytes);
    }

    @Override
    void putMyJSON(final JSONObject attachment) {
        attachment.put("alias", aliasName);
    }

    public String getAliasName() {
        return aliasName;
    }
    
}
