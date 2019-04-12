/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import org.slf4j.Logger;

public class DbStatementWrapper  implements Statement { // extends FilteredStatement {
    private static final Logger log = getLogger(DbStatementWrapper.class);
    private static Blockchain blockchain;

    private long stmtThreshold;
    private final Statement stmt;

    public DbStatementWrapper(Statement stmt, long stmtThreshold) {
//        super(stmt);
        this.stmt = stmt;
        this.stmtThreshold = stmtThreshold;
    }

    private Blockchain lookupBlockchain() {
        if (blockchain == null) {
            blockchain = CDI.current().select(BlockchainImpl.class).get();
        }
        return blockchain;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        long start = System.currentTimeMillis();
//        boolean b = super.execute(sql);
        boolean b = stmt.execute(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, lookupBlockchain().getHeight(), sql));
        return b;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        long start = System.currentTimeMillis();
//        ResultSet r = super.executeQuery(sql);
        ResultSet r = stmt.executeQuery(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, lookupBlockchain().getHeight(), sql));
        return r;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        long start = System.currentTimeMillis();
//        int c = super.executeUpdate(sql);
        int c = stmt.executeUpdate(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, lookupBlockchain().getHeight(), sql));
        return c;
    }

    private static void logThreshold(String msg) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(msg).append('\n');
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean firstLine = true;
        for (int i=3; i<stackTrace.length; i++) {
            String line = stackTrace[i].toString();
            if (!line.startsWith("apl."))
                break;
            if (firstLine)
                firstLine = false;
            else
                sb.append('\n');
            sb.append("  ").append(line);
        }
        log.debug(sb.toString());
    }

    @Override
    public void close() throws SQLException {
        stmt.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        stmt.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        stmt.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        stmt.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        stmt.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        stmt.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return stmt.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        stmt.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        stmt.setCursorName(name);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return stmt.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        stmt.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        stmt.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType()  throws SQLException {
        return stmt.getResultSetType();
    }

    @Override
    public void addBatch( String sql ) throws SQLException {
        stmt.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return stmt.executeBatch();
    }

    @Override
    public Connection getConnection()  throws SQLException {
        return stmt.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return stmt.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return stmt.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return stmt.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int columnIndexes[]) throws SQLException {
        return stmt.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String columnNames[]) throws SQLException {
        return stmt.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return stmt.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int columnIndexes[]) throws SQLException {
        return stmt.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String columnNames[]) throws SQLException {
        return stmt.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return stmt.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return stmt.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        stmt.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return stmt.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        stmt.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return stmt.isCloseOnCompletion();
    }

    @Override
    public long getLargeUpdateCount() throws SQLException {
        return stmt.getLargeUpdateCount();
    }

    @Override
    public void setLargeMaxRows(long max) throws SQLException {
        stmt.setLargeMaxRows(max);
    }

    @Override
    public long getLargeMaxRows() throws SQLException {
        return stmt.getLargeMaxRows();
    }

    @Override
    public long[] executeLargeBatch() throws SQLException {
        return stmt.executeLargeBatch();
    }

    @Override
    public long executeLargeUpdate(String sql) throws SQLException {
        return stmt.executeLargeUpdate(sql);
    }

    @Override
    public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return stmt.executeLargeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public long executeLargeUpdate(String sql, int columnIndexes[]) throws SQLException {
        return stmt.executeLargeUpdate(sql, columnIndexes);
    }

    @Override
    public long executeLargeUpdate(String sql, String columnNames[]) throws SQLException {
        return stmt.executeLargeUpdate(sql, columnNames);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return stmt.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return stmt.unwrap(iface);
    }



}
