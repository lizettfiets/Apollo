/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.consensus.ConsensusFacadeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ActiveGenerators {
    private static final Logger LOG = LoggerFactory.getLogger(ActiveGenerators.class);

    private Blockchain blockchain;
    private ConsensusFacadeHolder consensusFacadeHolder;
    private BlockchainProcessor blockchainProcessor;
    /**
     * Active block generators
     */
    private static final Set<Long> activeGeneratorIds = new HashSet<>();

    /**
     * Active block identifier
     */
    private  long activeBlockId;

    /**
     * Sorted list of generators for the next block
     */
    private final List<Generator> activeGenerators = new ArrayList<>();

    /**
     * Generator list has been initialized
     */
    private boolean generatorsInitialized = false;

    @Inject
    public ActiveGenerators(Blockchain blockchain, ConsensusFacadeHolder consensusFacadeHolder) {
        this.blockchain = blockchain;
        this.consensusFacadeHolder = consensusFacadeHolder;
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }

    /**
     * Return a list of generators for the next block.  The caller must hold the blockchain
     * read lock to ensure the integrity of the returned list.
     *
     * @return List of generator account identifiers
     */
    public synchronized List<Generator> getNextGenerators() {
        List<Generator> generatorList;
        if (!generatorsInitialized) {
            activeGeneratorIds.addAll(blockchain.getBlockGenerators(Math.max(1, blockchain.getHeight() - 10000)));
            activeGeneratorIds.forEach(activeGeneratorId -> activeGenerators.add(new Generator(null, Account.getPublicKey(activeGeneratorId), activeGeneratorId)));
            LOG.debug(activeGeneratorIds.size() + " block generators found");
            lookupBlockchainProcessor().addListener(block -> {
                long generatorId = block.getGeneratorId();
                    if (!activeGeneratorIds.contains(generatorId)) {
                        activeGeneratorIds.add(generatorId);
                        activeGenerators.add(new Generator(null, block.getGeneratorPublicKey(), generatorId));
                    }
            }, BlockchainProcessor.Event.BLOCK_PUSHED);
            generatorsInitialized = true;
        }
        long blockId = blockchain.getLastBlock().getId();
        if (blockId != activeBlockId) {
            activeBlockId = blockId;
            Block lastBlock = blockchain.getLastBlock();
            for (Generator generator : activeGenerators) {
                consensusFacadeHolder.getConsensusFacade().updateGeneratorData(generator, lastBlock);
            }
            Collections.sort(activeGenerators);
        }
        generatorList = new ArrayList<>(activeGenerators);
        return generatorList;

    }
}

