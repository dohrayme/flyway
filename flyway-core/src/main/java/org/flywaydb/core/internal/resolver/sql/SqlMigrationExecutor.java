/*
 * Copyright 2010-2018 Boxfuse GmbH
 *
 * INTERNAL RELEASE. ALL RIGHTS RESERVED.
 *
 * Must
 * be
 * exactly
 * 13 lines
 * to match
 * community
 * edition
 * license
 * length.
 */
package org.flywaydb.core.internal.resolver.sql;

import org.flywaydb.core.api.configuration.FlywayConfiguration;
import org.flywaydb.core.api.resolver.MigrationExecutor;
import org.flywaydb.core.internal.database.Database;
import org.flywaydb.core.internal.sqlscript.SqlScript;
import org.flywaydb.core.internal.util.PlaceholderReplacer;
import org.flywaydb.core.internal.util.scanner.LoadableResource;

import java.sql.Connection;

/**
 * Database migration based on a sql file.
 */
public class SqlMigrationExecutor implements MigrationExecutor {
    /**
     * Database-specific support.
     */
    private final Database database;

    /**
     * The placeholder replacer to apply to sql migration scripts.
     */
    private final PlaceholderReplacer placeholderReplacer;

    /**
     * The Resource pointing to the sql script.
     * The complete sql script is not held as a member field here because this would use the total size of all
     * sql migrations files in heap space during db migration, see issue 184.
     */
    private final LoadableResource sqlScriptResource;

    /**
     * The Flyway configuration.
     */
    private final FlywayConfiguration configuration;

    /**
     * The SQL script that will be executed.
     */
    private SqlScript sqlScript;

    /**
     * Creates a new sql script migration based on this sql script.
     *
     * @param database            The database-specific support.
     * @param sqlScriptResource   The resource containing the sql script.
     * @param placeholderReplacer The placeholder replacer to apply to sql migration scripts.
     * @param configuration       The Flyway configuration.
     */
    public SqlMigrationExecutor(Database database, LoadableResource sqlScriptResource, PlaceholderReplacer placeholderReplacer, FlywayConfiguration configuration) {
        this.database = database;
        this.sqlScriptResource = sqlScriptResource;
        this.placeholderReplacer = placeholderReplacer;
        this.configuration = configuration;
    }

    @Override
    public void execute(Connection connection) {
        getSqlScript().execute(database.getMigrationConnection().getJdbcTemplate());
    }

    private synchronized SqlScript getSqlScript() {
        if (sqlScript == null) {
            sqlScript = new SqlScript(database, sqlScriptResource, placeholderReplacer, configuration.getEncoding(), configuration.isMixed()
                    //[pro]
                    , configuration.getErrorHandlers()
                    //[/pro]
            );
        }
        return sqlScript;
    }

    @Override
    public boolean executeInTransaction() {
        return getSqlScript().executeInTransaction();
    }
}
