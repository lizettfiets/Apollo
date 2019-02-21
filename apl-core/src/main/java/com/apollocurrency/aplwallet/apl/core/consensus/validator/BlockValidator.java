/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.validator;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;

public interface BlockValidator {
    void validate(Block block, Block previousLastBlock, int curTime) throws BlockchainProcessor.BlockNotAcceptedException;
}
