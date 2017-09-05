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
package org.flywaydb.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.flywaydb.core.Flyway;
import org.h2.Driver;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for AbstractFlywayMojo.
 */
public class AbstractFlywayMojoSmallTest {
    private static final String FLYWAY_MY_PROPERTY = "flyway.myProperty";

    @Before
    public void cleanCustomSystemProperty() {
        System.clearProperty(FLYWAY_MY_PROPERTY);
    }

    @Test
    public void execute() throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
                new ClassRealm(new ClassWorld("a", originalClassLoader), "a", originalClassLoader));

        try {
            AbstractFlywayMojo mojo = new AbstractFlywayMojo() {
                @Override
                protected void doExecute(Flyway flyway) throws Exception {
                    assertEquals(2, flyway.getSchemas().length);
                    assertEquals("first", flyway.getSchemas()[0]);
                    assertEquals("second", flyway.getSchemas()[1]);
                }
            };

            mojo.driver = Driver.class.getName();
            mojo.url = "jdbc:h2:mem:dummy";
            mojo.user = "sa";
            mojo.settings = new Settings();
            mojo.mavenProject = new MavenProject();
            mojo.mavenProject.setFile(new File(".").getAbsoluteFile());
            mojo.mavenProject.getProperties().setProperty("flyway.schemas", "first,second");
            mojo.mavenProject.getBuild().setOutputDirectory(".");
            mojo.execute();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Test
    public void skipExecute() throws Exception {
        AbstractFlywayMojo mojo = new AbstractFlywayMojo() {
            @Override
            protected void doExecute(Flyway flyway) throws Exception {
                assertNull(flyway.getDataSource());
            }
        };

        mojo.skip = true;
        mojo.url = "jdbc:h2:mem:dummy";
        mojo.mavenProject = new MavenProject();
        mojo.execute();
    }

    @Test
    public void shouldHaveABooleanPropertyWithTrue() throws Exception {
        System.setProperty(FLYWAY_MY_PROPERTY, "true");
        AbstractFlywayMojo mojo = new AbstractFlywayMojo() {
            @Override
            protected void doExecute(Flyway flyway) throws Exception {
            }
        };

        boolean booleanProperty = mojo.getBooleanProperty(FLYWAY_MY_PROPERTY, false);
        assertEquals(true, booleanProperty);
    }

    @Test
    public void shouldHaveTheMavenPropertyWithFalse() throws Exception {
        AbstractFlywayMojo mojo = new AbstractFlywayMojo() {
            @Override
            protected void doExecute(Flyway flyway) throws Exception {
            }
        };

        boolean booleanProperty = mojo.getBooleanProperty(FLYWAY_MY_PROPERTY, false);
        assertEquals(false, booleanProperty);
    }
}
