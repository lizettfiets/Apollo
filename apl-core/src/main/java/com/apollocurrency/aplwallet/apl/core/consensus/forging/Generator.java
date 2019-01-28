
package com.apollocurrency.aplwallet.apl.core.consensus.forging;


import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.math.BigInteger;

public class Generator implements Comparable<Generator> {
    private final byte[] keySeed;
    private final byte[] publicKey;
    private final Long accountId;
    private volatile long hitTime;
    private volatile BigInteger hit;
    private volatile BigInteger effectiveBalance;
    private volatile long deadline;

    public Generator(byte[] keySeed) {
        this.keySeed = keySeed;
        this.publicKey = Crypto.getPublicKey(keySeed);
        this.accountId = Convert.getId((publicKey));
    }

    public Long getAccountId() {
        return accountId;
    }

    public long getHitTime() {
        return hitTime;
    }

    public BigInteger getHit() {
        return hit;
    }

    public BigInteger getEffectiveBalance() {
        return effectiveBalance;
    }

    public long getDeadline() {
        return deadline;
    }

    @Override
    public int compareTo(Generator g) {
        int i = this.hit.multiply(g.getEffectiveBalance()).compareTo(g.getHit().multiply(this.getEffectiveBalance()));
        if (i != 0) {
            return i;
        }
        return Long.compare(accountId, g.getAccountId());
    }
}
