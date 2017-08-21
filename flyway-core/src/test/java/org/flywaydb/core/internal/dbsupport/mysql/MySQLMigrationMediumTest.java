/*
 * Copyright 2010-2017 Boxfuse GmbH
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
package org.flywaydb.core.internal.dbsupport.mysql;

import org.flywaydb.core.DbCategory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.util.jdbc.DriverDataSource;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.HostPortWaitStrategy;

import javax.sql.DataSource;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * Test to demonstrate the migration functionality using Mysql.
 */
@SuppressWarnings({"JavaDoc"})
@Category(DbCategory.MySQL.class)
public class MySQLMigrationMediumTest extends MySQLMigrationTestCase {
    static final String DOCKER_IMAGE_NAME = "mysql:5.5.57";

    private static String jdbcUrl;
    private static String jdbcUser;
    private static String jdbcPassword;

    @ClassRule
    public static ExternalResource initMySQL() {
        return new ExternalResource() {
            private MySQLContainer mysql;

            @Override
            protected void before() throws Throwable {
                try {
                    DockerClientFactory.instance().client();
                    mysql = new MySQLContainer(DOCKER_IMAGE_NAME);
                    mysql.start();
                    new HostPortWaitStrategy().waitUntilReady(mysql);
                    jdbcUrl = mysql.getJdbcUrl();
                    jdbcUser = "root";
                    jdbcPassword = mysql.getPassword();
                } catch (Exception e) {
                    // Docker not found, fall back to local MySQL instance.
                    jdbcUrl = customProperties.getProperty("mysql.url", "jdbc:mysql://localhost/flyway_db");
                    jdbcUser = customProperties.getProperty("mysql.user", "flyway");
                    jdbcPassword = customProperties.getProperty("mysql.password", "flyway");
                }
            }

            @Override
            protected void after() {
                if (mysql != null) {
                    mysql.stop();
                }
            }
        };
    }

    @Override
    protected DataSource createDataSource(Properties customProperties) {
        return new DriverDataSource(Thread.currentThread().getContextClassLoader(), null,
                jdbcUrl, jdbcUser, jdbcPassword, null);
    }

    @Test
    public void migrateWithNonExistingSchemaSetInPropertyButNotInUrl() throws Exception {
        Flyway flyway = new Flyway();
        flyway.setDataSource(createDataSource(null));
        flyway.setSchemas("non-existing-schema");
        flyway.setLocations(BASEDIR);
        flyway.clean();
        assertEquals(4, flyway.migrate());
    }

    @Test
    public void migrateWithExistingSchemaSetInPropertyButNotInUrl() throws Exception {
        Flyway flyway = new Flyway();
        flyway.setDataSource(createDataSource(null));
        flyway.setSchemas("test");
        flyway.setLocations(getBasedir());
        flyway.clean();
        assertEquals(4, flyway.migrate());
    }
}