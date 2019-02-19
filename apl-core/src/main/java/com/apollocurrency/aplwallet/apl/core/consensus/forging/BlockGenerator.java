/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

import com.apollocurrency.aplwallet.apl.core.app.Block;

public interface BlockGenerator {
    Generator startGeneration(Generator generator);

    Generator stopGeneration(Generator generator);

    int stopAll();

    boolean suspendAll();

    boolean resumeAll();

    void performGenerationIteration();

    void setGenerationDelay(int delay);

    boolean canGenerateBetterBlock(long prevBlockId, Block anotherBlock);

}
