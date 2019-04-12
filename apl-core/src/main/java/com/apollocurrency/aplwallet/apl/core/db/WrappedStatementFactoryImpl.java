package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class WrappedStatementFactoryImpl implements WrappedStatementFactory {

    private long stmtThreshold;

    public WrappedStatementFactoryImpl(long stmtThreshold) {
        this.stmtThreshold = stmtThreshold;
    }

    @Override
    public Statement createStatement(Statement stmt) {
        return new DbStatementWrapper(stmt, stmtThreshold);
    }

    @Override
    public PreparedStatement createPreparedStatement(PreparedStatement stmt, String sql, long stmtThreshold) {
        return new DbPreparedStatementWrapper(stmt, sql, stmtThreshold);
    }

    public long getStmtThreshold() {
        return stmtThreshold;
    }
}
