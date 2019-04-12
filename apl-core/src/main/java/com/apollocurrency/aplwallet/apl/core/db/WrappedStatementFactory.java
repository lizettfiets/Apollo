/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * Create wrapped DbStatementWrapper and DbPreparedStatementWrapper for use with DbConnectionWrapper
 */
public interface WrappedStatementFactory {

    long DEFAULT_STATEMENT_LOGGING_THRESHOLD = 15_000L;

    /**
     * Create a DbStatementWrapper for the supplied Statement
     *
     * @param   stmt                Statement
     * @return                      Wrapped statement
     */
    Statement createStatement(Statement stmt);

    /**
     * Create a DbPreparedStatementWrapper for the supplied PreparedStatement
     *
     * @param   stmt                Prepared statement
     * @param   sql                 SQL statement
     * @return                      Wrapped prepared statement
     */
    PreparedStatement createPreparedStatement(PreparedStatement stmt, String sql, long stmtThreshold);

/*
    default Statement createStatement(Statement stmt, String sql) {
        if (sql != null && !sql.isEmpty()) {
            return createPreparedStatement((PreparedStatement) stmt, sql, DEFAULT_STATEMENT_LOGGING_THRESHOLD); // 15 secs
        } else {
            return createStatement(stmt);
        }
    }
*/

    long getStmtThreshold();
}
