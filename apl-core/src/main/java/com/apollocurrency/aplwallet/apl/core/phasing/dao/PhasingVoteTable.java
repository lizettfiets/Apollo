/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LinkKey;
import com.apollocurrency.aplwallet.apl.core.db.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.phasing.mapper.PhasingVoteMapper;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

@Singleton
public class PhasingVoteTable extends EntityDbTable<PhasingVote> {
    static final LinkKeyFactory<PhasingVote> KEY_FACTORY = new LinkKeyFactory<PhasingVote>("transaction_id", "voter_id") {
        @Override
        public DbKey newKey(PhasingVote vote) {
            return new LinkKey(vote.getPhasedTransactionId(), vote.getVoterId());
        }
    };
    private static final PhasingVoteMapper MAPPER = new PhasingVoteMapper();
    private static final String TABLE_NAME = "phasing_vote";


    public PhasingVoteTable() {
        super(TABLE_NAME, KEY_FACTORY, false);
    }

    @Override
    public PhasingVote load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        PhasingVote phasingVote = MAPPER.map(rs, null);
        phasingVote.setDbKey(dbKey);
        return phasingVote;
    }

    @Override
    public void save(Connection con, PhasingVote vote) throws SQLException {
        Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_vote (vote_id, transaction_id, "
                + "voter_id, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, vote.getVoteId());
            pstmt.setLong(++i, vote.getPhasedTransactionId());
            pstmt.setLong(++i, vote.getVoterId());
            pstmt.setInt(++i, vote.getHeight());
            pstmt.executeUpdate();
        }
    }

    public PhasingVote get(long phasedTransactionId, long voterId) {
        return get(KEY_FACTORY.newKey(phasedTransactionId, voterId));
    }

}
