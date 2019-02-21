/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.genesis;

import com.apollocurrency.aplwallet.apl.core.consensus.acceptor.BlockApplier;
import com.apollocurrency.aplwallet.apl.core.app.Block;

import javax.inject.Named;

@Named
public class GenesisBlockApplier implements BlockApplier {
    @Override
    public void apply(Block block) {

    }
}
