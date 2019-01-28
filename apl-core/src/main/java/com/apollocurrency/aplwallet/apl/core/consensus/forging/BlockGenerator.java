/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

public interface BlockGenerator {
    Generator startGeneration(Generator generator);

    Generator stopGeneration(Generator generator);

    void performForgingIteration();

}
