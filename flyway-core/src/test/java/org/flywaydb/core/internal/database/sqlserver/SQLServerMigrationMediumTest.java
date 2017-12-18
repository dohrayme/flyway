/*
 * Copyright 2010-2017 Boxfuse GmbH
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
package org.flywaydb.core.internal.database.sqlserver;

import org.flywaydb.core.DbCategory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.internal.sqlscript.FlywaySqlScriptException;
import org.flywaydb.core.internal.database.Schema;
import org.flywaydb.core.internal.sqlscript.SqlScript;
import org.flywaydb.core.internal.util.jdbc.DriverDataSource;
import org.flywaydb.core.internal.util.scanner.classpath.ClassPathResource;
import org.flywaydb.core.migration.MigrationTestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Test to demonstrate the migration functionality using SQL Server.
 */
@SuppressWarnings({"JavaDoc"})
@Category(DbCategory.SQLServer.class)
public class SQLServerMigrationMediumTest extends MigrationTestCase {
    static String JDBC_PORT = "62070";
    static String JDBC_USER = "sa";
    static String JDBC_PASSWORD = "flywayPWD000";

    @Override
    protected DataSource createDataSource(Properties customProperties) {
        return new DriverDataSource(Thread.currentThread().getContextClassLoader(), null,
                "jdbc:sqlserver://localhost:" + JDBC_PORT + ";databaseName=flyway_db_ms", JDBC_USER, JDBC_PASSWORD);
    }

    @Test
    public void singleUser() {
        flyway.setLocations("migration/database/sqlserver/sql/singleUser");
        flyway.migrate();
    }

    @Test
    public void backup() {
        flyway.setLocations("migration/database/sqlserver/sql/backup");
        assertEquals(1, flyway.migrate());
    }

    @Test
    public void jTDS() {
        flyway = new Flyway();
        flyway.setDataSource("jdbc:jtds:sqlserver://localhost:" + JDBC_PORT + "/flyway_db_jtds", JDBC_USER, JDBC_PASSWORD);
        flyway.clean();
        flyway.setLocations(getMigrationDir() + "/sql");
        assertEquals(4, flyway.migrate());
    }

    @Override
    protected String getQuoteLocation() {
        return "migration/quote";
    }

    @Test
    public void failedMigration() {
        String tableName = "before_the_error";

        flyway.setLocations("migration/failed");
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("tableName", database.quote(tableName));
        flyway.setPlaceholders(placeholders);

        try {
            flyway.migrate();
            fail();
        } catch (FlywaySqlScriptException e) {
            // root cause of exception must be defined, and it should be FlywaySqlScriptException
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof SQLException);
            // and make sure the failed statement was properly recorded
            // Normal DB should fail at line 21. SqlServer fails at line 17 as statements are executed in batches.
            assertEquals(17, e.getLineNumber());
            assertTrue(e.getStatement().contains("THIS IS NOT VALID SQL"));
        }
    }

    /**
     * Tests clean and migrate for SQL Server Stored Procedures.
     */
    @Test
    public void procedure() throws Exception {
        flyway.setLocations("migration/database/sqlserver/sql/procedure");
        flyway.migrate();

        assertEquals("Hello", jdbcTemplate.queryForString("SELECT value FROM test_data"));

        flyway.clean();

        // Running migrate again on an unclean database, triggers duplicate object exceptions.
        flyway.migrate();
    }

    /**
     * Tests clean and migrate for SQL Server Functions.
     */
    @Test
    public void function() throws Exception {
        flyway.setLocations("migration/database/sqlserver/sql/function");
        flyway.migrate();

        // Test inlined function.
        jdbcTemplate.execute("INSERT INTO test_data (value) VALUES ('Hello')");

        List<String> reverse = jdbcTemplate.queryForStringList("SELECT * from reverseInlineFunc();");
        assertEquals(1, reverse.size());
        assertEquals("olleH", reverse.get(0));

        // Test table valued-function.
        final int count = 10;
        List<String> integers = jdbcTemplate.queryForStringList("SELECT * from dbo.positiveIntegers(?)", String.valueOf(count));
        assertEquals(count, integers.size());
        for (int i = 1; i <= 10; i++) {
            assertEquals(i, Integer.parseInt(integers.get(i - 1)));
        }

        flyway.clean();

        // Running migrate again on an unclean database, triggers duplicate object exceptions.
        flyway.migrate();
    }

    /**
     * Tests clean and migrate for SQL Server Triggers.
     */
    @Test
    public void trigger() throws Exception {
        flyway.setLocations("migration/database/sqlserver/sql/trigger");
        flyway.migrate();

        assertEquals(3, jdbcTemplate.queryForInt("SELECT priority FROM customers where name='MS Internet Explorer Team'"));

        flyway.clean();

        // Running migrate again on an unclean database, triggers duplicate object exceptions.
        flyway.migrate();
    }

    /**
     * Tests clean and migrate for SQL Server Views.
     */
    @Test
    public void view() throws Exception {
        flyway.setLocations("migration/database/sqlserver/sql/view");
        flyway.migrate();

        assertEquals(150, jdbcTemplate.queryForInt("SELECT value FROM v"));

        flyway.clean();

        // Running migrate again on an unclean database, triggers duplicate object exceptions.
        flyway.migrate();
    }

    /**
     * Tests clean and migrate for SQL Server Types.
     */
    @Test
    public void type() {
        flyway.setLocations("migration/database/sqlserver/sql/type");
        flyway.migrate();

        flyway.clean();

        // Running migrate again on an unclean database, triggers duplicate object exceptions.
        flyway.migrate();
    }

    /**
     * Tests clean and migrate for SQL Server assemblies.
     */
    @Test
    public void assembly() throws Exception {
        CallableStatement stmt = jdbcTemplate.getConnection().prepareCall("EXEC sp_configure 'clr enabled', 1; RECONFIGURE;");
        stmt.execute();
        stmt.close();
        stmt = jdbcTemplate.getConnection().prepareCall("EXEC sp_configure 'show advanced options', 1; RECONFIGURE;");
        stmt.execute();
        stmt.close();
        stmt = jdbcTemplate.getConnection().prepareCall("EXEC sp_configure 'clr strict security', 0; RECONFIGURE;");
        stmt.execute();
        stmt.close();

        try {

            flyway.setLocations("migration/database/sqlserver/sql/assembly");
            flyway.migrate();

            // CLR procedure.
            stmt = jdbcTemplate.getConnection().prepareCall("EXEC helloFromProc ?, ?");
            stmt.setString(1, "Alice");
            stmt.registerOutParameter(2, Types.VARCHAR);
            stmt.execute();
            assertEquals("Hello Alice", stmt.getString(2));

            // CLR function.
            assertEquals("Hello Bob", jdbcTemplate.queryForString("SELECT dbo.helloFromFunc('Bob');"));

            List<String> greetings = jdbcTemplate.queryForStringList("SELECT * FROM dbo.helloFromTableValuedFunction(3, 'Charlie')");
            assertEquals(3, greetings.size());

            for (String greeting : greetings) {
                assertEquals("Hello Charlie", greeting);
            }

            String[] names = new String[]{"Dave", "Erin", "Faythe"};

            for (String name : names) {
                jdbcTemplate.execute("INSERT INTO names (name) VALUES (?)", name);
            }

            // CLR trigger.
            greetings = jdbcTemplate.queryForStringList("SELECT * FROM triggered_greetings");

            assertEquals(names.length, greetings.size());

            for (String name : names) {
                assertTrue(greetings.remove("Hello " + name));
            }

            // User aggregate.
            greetings = jdbcTemplate.queryForStringList("SELECT dbo.helloAll(name) FROM names");

            assertEquals(1, greetings.size());
            assertEquals("Hello Dave, Erin, Faythe", greetings.get(0));

            flyway.clean();

            // Running migrate again on an unclean database, triggers duplicate object exceptions.
            flyway.migrate();
        } finally {
            try {
                jdbcTemplate.getConnection().prepareCall("EXEC sp_configure 'clr enabled', 0; RECONFIGURE;");
            } catch (Exception e) {
                // Swallow.
            }
        }
    }

    /**
     * Tests clean and migrate for SQL Server unicode strings.
     */
    @Test
    public void nvarchar() {
        flyway.setLocations("migration/database/sqlserver/sql/nvarchar");
        flyway.migrate();

        flyway.clean();
    }

    /**
     * Tests clean and migrate for SQL Server sequences.
     */
    @Test
    public void sequence() {
        flyway.setLocations("migration/database/sqlserver/sql/sequence");
        flyway.migrate();

        flyway.clean();
        flyway.migrate();
    }

    /**
     * Tests clean and migrate for default constraints with functions.
     */
    @Test
    public void defaultConstraints() {
        flyway.setLocations("migration/database/sqlserver/sql/default");
        flyway.migrate();

        flyway.clean();
    }

    /**
     * Tests migrate error for pk constraints.
     */
    @Test(expected = FlywayException.class)
    public void pkConstraints() {
        flyway.setLocations("migration/database/sqlserver/sql/pkConstraint");
        flyway.migrate();
    }

    /**
     * Tests clean and migrate for synonyms.
     */
    @Test
    public void synonym() {
        flyway.setLocations("migration/database/sqlserver/sql/synonym");
        flyway.migrate();

        flyway.clean();
        flyway.migrate();
    }

    @Test
    public void use() {
        flyway.setLocations("migration/database/sqlserver/sql/use");
        assertEquals(2, flyway.migrate());
    }

    @Test
    public void itShouldCleanCheckConstraint() {
        // given
        flyway.setLocations("migration/database/sqlserver/sql/checkConstraint");
        flyway.migrate();

        // when
        flyway.clean();

        // then
        int pendingMigrations = flyway.info().pending().length;
        assertEquals(3, pendingMigrations);
    }

    /**
     * Tests a large migration that has been reported to hang on SqlServer 2005.
     */
    @Ignore("Axel: Fails due to nested transaction being opened in script, causing outer transaction not to receive COMMIT statement")
    @Test
    public void large() throws Exception {
        flyway.setLocations("migration/database/sqlserver/sql/large",
                "org.flywaydb.core.internal.database.sqlserver.large");
        flyway.setTarget(MigrationVersion.fromVersion("3.1.0"));
        flyway.migrate();

        assertEquals("3.1.0", flyway.info().current().getVersion().toString());
        assertEquals(MigrationState.SUCCESS, flyway.info().current().getState());
        assertTrue(jdbcTemplate.queryForInt("SELECT COUNT(*) FROM dbo.CHANGELOG") > 0);
    }

    /**
     * Tests that dml errors that occur in the middle of a batch are correctly detected
     * see issue 718
     */
    @Test
    public void dmlErrorsCorrectlyDetected() {
        String tableName = "sample_table";

        flyway.setLocations("migration/database/sqlserver/sql/dmlErrorDetection");
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("tableName", database.quote(tableName));
        flyway.setPlaceholders(placeholders);

        try {
            flyway.migrate();
            fail("This migration should have failed and this point shouldn't have been reached");
        } catch (FlywaySqlScriptException e) {
            // root cause of exception must be defined, and it should be FlywaySqlScriptException
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof SQLException);
            // and make sure the failed statement was properly recorded
            assertEquals(23, e.getLineNumber());
            assertTrue(e.getStatement().contains("INSERT INTO"));
            assertTrue(e.getStatement().contains("VALUES(1)"));
        }
    }

    @Test
    public void msDBToolsIgnoredForEmpty() {
        Schema schema = database.getMainConnection().getOriginalSchema();

        new SqlScript(new ClassPathResource("migration/database/sqlserver/createMSDBTools.sql",
                Thread.currentThread().getContextClassLoader()).loadAsString("UTF-8"), database).
                execute(jdbcTemplate);

        try {
            assertTrue("MS DB tools must be ignored in empty check.", schema.empty());
        } finally {
            try {
                new SqlScript(new ClassPathResource("migration/database/sqlserver/dropMSDBTools.sql",
                        Thread.currentThread().getContextClassLoader()).loadAsString("UTF-8"), database).
                        execute(jdbcTemplate);
            } catch (Exception e) {
                // Swallow to prevent override of test raised exception.
            }
        }
    }

    @Test
    public void msDBToolsNotCleared() throws Exception {
        Schema schema = database.getMainConnection().getOriginalSchema();

        new SqlScript(new ClassPathResource("migration/database/sqlserver/createMSDBTools.sql",
                Thread.currentThread().getContextClassLoader()).loadAsString("UTF-8"), database).
                execute(jdbcTemplate);

        try {
            final String queryObjectCount = "SELECT COUNT(*) from sys.all_objects";

            int initialObjectsCount = jdbcTemplate.queryForInt(queryObjectCount);

            schema.clean();

            int finalObjectCount = jdbcTemplate.queryForInt(queryObjectCount);

            assertEquals("Cleaning the schema must not delete MS DB Tools objects.", initialObjectsCount, finalObjectCount);
        } finally {
            try {
                new SqlScript(new ClassPathResource("migration/database/sqlserver/dropMSDBTools.sql",
                        Thread.currentThread().getContextClassLoader()).loadAsString("UTF-8"), database).
                        execute(jdbcTemplate);
            } catch (Exception e) {
                // Swallow to prevent override of test raised exception.
            }
        }
    }

    @Override
    @Ignore("Not supported on SQL Server")
    public void setCurrentSchema() {
        //Skip
    }
}
