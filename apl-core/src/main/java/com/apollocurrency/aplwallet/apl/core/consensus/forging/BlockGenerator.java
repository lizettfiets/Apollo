/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

public interface BlockGenerator<T> {
    T startGeneration(T generator);

    T stopGeneration(T generator);

    boolean stopAll();

    boolean suspendAll();

    boolean resumeAll();

    void performGenerationIteration();

    void setGenerationDelay(int delay);

}
