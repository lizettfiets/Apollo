/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus;

import com.apollocurrency.aplwallet.apl.core.app.Block;

public interface Consensus {
    Block generateBlock();

    boolean verifyBlock(Block block);
}
