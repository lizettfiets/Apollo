/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

import com.apollocurrency.aplwallet.apl.core.app.Block;

import java.util.Collection;

public interface BlockGenerator {
    Generator startGeneration(Generator generator);

    Generator stopGeneration(Generator generator);

    int stopAll();

    Generator getGenerator(long id);

    Collection<Generator> getAllGenerators();

    void suspendAll();

    void resumeAll();

    void performGenerationIteration();

    void setGenerationDelay(int delay);

    boolean canGenerateBetterBlock(long prevBlockId, Block anotherBlock);

}
