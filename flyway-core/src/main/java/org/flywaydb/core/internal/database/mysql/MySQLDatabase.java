/*
 * Copyright 2010-2018 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.database.mysql;

import org.flywaydb.core.api.configuration.FlywayConfiguration;
import org.flywaydb.core.api.errorhandler.ErrorHandler;
import org.flywaydb.core.internal.database.Database;
import org.flywaydb.core.internal.database.SqlScript;
import org.flywaydb.core.internal.exception.FlywayDbUpgradeRequiredException;
import org.flywaydb.core.internal.exception.FlywaySqlException;
import org.flywaydb.core.internal.util.scanner.Resource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * MySQL database.
 */
public class MySQLDatabase extends Database<MySQLConnection> {
    /**
     * Creates a new instance.
     *
     * @param configuration The Flyway configuration.
     * @param connection    The connection to use.
     */
    public MySQLDatabase(FlywayConfiguration configuration, Connection connection



    ) {
        super(configuration, connection



        );
    }

    @Override
    protected MySQLConnection getConnection(Connection connection



    ) {
        return new MySQLConnection(configuration, this, connection



        );
    }

    @Override
    protected final void ensureSupported() {
        String version = majorVersion + "." + minorVersion;
        boolean isMariaDB;
        try {
            isMariaDB = jdbcMetaData.getDatabaseProductVersion().contains("MariaDB");
        } catch (SQLException e) {
            throw new FlywaySqlException("Unable to determine database product version", e);
        }
        String productName = isMariaDB ? "MariaDB" : "MySQL";

        if (majorVersion < 5) {
            throw new FlywayDbUpgradeRequiredException(productName, version, "5.0");
        }
        if (majorVersion == 5) {

            if (minorVersion < 5) {
                throw new org.flywaydb.core.internal.exception.FlywayEnterpriseUpgradeRequiredException(
                    isMariaDB ? "MariaDB" : "Oracle", productName, version);
            }

            if (minorVersion > 7) {
                recommendFlywayUpgrade(productName, version);
            }
        } else {
            if (isMariaDB) {
                if (majorVersion > 10 || (majorVersion == 10 && minorVersion > 2)) {
                    recommendFlywayUpgrade(productName, version);
                }
            } else {
                recommendFlywayUpgrade(productName, version);
            }
        }
    }

    @Override
    protected SqlScript doCreateSqlScript(Resource sqlScriptResource, String sqlScriptSource, boolean mixed



    ) {
        return new MySQLSqlScript(sqlScriptResource, sqlScriptSource, mixed



        );
    }

    @Override
    public String getDbName() {
        return "mysql";
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getMainConnection().getJdbcTemplate().queryForString("SELECT SUBSTRING_INDEX(USER(),'@',1)");
    }

    @Override
    public boolean supportsDdlTransactions() {
        return false;
    }

    @Override
    public String getBooleanTrue() {
        return "1";
    }

    @Override
    public String getBooleanFalse() {
        return "0";
    }

    @Override
    public String doQuote(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public boolean catalogIsSchema() {
        return true;
    }

    @Override
    public boolean useSingleConnection() {
        return true;
    }
}