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
package org.flywaydb.gradle.task;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.internal.configuration.ConfigurationUtils;
import org.flywaydb.core.internal.util.Location;
import org.flywaydb.core.internal.util.StringUtils;
import org.flywaydb.gradle.FlywayExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A base class for all flyway tasks.
 */
public abstract class AbstractFlywayTask extends DefaultTask {
    /**
     * The flyway {} block in the build script.
     */
    protected FlywayExtension extension;
    /**
     * The fully qualified classname of the jdbc driver to use to connect to the database
     */
    public String driver;

    /**
     * The jdbc url to use to connect to the database
     */
    public String url;

    /**
     * The user to use to connect to the database
     */
    public String user;

    /**
     * The password to use to connect to the database
     */
    public String password;

    /**
     * The name of Flyway's metadata table
     */
    public String table;

    /**
     * The case-sensitive list of schemas managed by Flyway
     */
    public String[] schemas;

    /**
     * The version to tag an existing schema with when executing baseline. (default: 1)
     */
    public String baselineVersion;

    /**
     * The description to tag an existing schema with when executing baseline. (default: << Flyway Baseline >>)
     */
    public String baselineDescription;

    /**
     * Locations to scan recursively for migrations. The location type is determined by its prefix.
     * (default: filesystem:src/main/resources/db/migration)
     * <p>
     * <tt>Unprefixed locations or locations starting with classpath:</tt>
     * point to a package on the classpath and may contain both sql and java-based migrations.
     * <p>
     * <tt>Locations starting with filesystem:</tt>
     * point to a directory on the filesystem and may only contain sql migrations.
     */
    public String[] locations;

    /**
     * The fully qualified class names of the custom MigrationResolvers to be used in addition (default)
     * or as a replacement (using skipDefaultResolvers) to the built-in ones for resolving Migrations to
     * apply.
     * <p>(default: none)</p>
     */
    public String[] resolvers;

    /**
     * If set to true, default built-in resolvers will be skipped, only custom migration resolvers will be used.
     * <p>(default: false)</p>
     */
    public Boolean skipDefaultResolvers;

    /**
     * The file name prefix for Sql migrations
     * <p>
     * <p>Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,
     * which using the defaults translates to V1_1__My_description.sql</p>
     */
    public String sqlMigrationPrefix;

    /**
     * The file name prefix for repeatable sql migrations (default: R).
     * <p>
     * <p>Repeatable sql migrations have the following file name structure: prefixSeparatorDESCRIPTIONsuffix ,
     * which using the defaults translates to R__My_description.sql</p>
     */
    public String repeatableSqlMigrationPrefix;

    /**
     * The file name prefix for Sql migrations
     * <p>
     * <p>Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,
     * which using the defaults translates to V1_1__My_description.sql</p>
     */
    public String sqlMigrationSeparator;

    /**
     * The file name suffix for Sql migrations
     * <p>
     * <p>Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,
     * which using the defaults translates to V1_1__My_description.sql</p>
     */
    public String sqlMigrationSuffix;

    /**
     * The encoding of Sql migrations
     */
    public String encoding;

    /**
     * Placeholders to replace in Sql migrations
     */
    public Map<Object, Object> placeholders;

    /**
     * Whether placeholders should be replaced.
     */
    public Boolean placeholderReplacement;

    /**
     * The prefix of every placeholder
     */
    public String placeholderPrefix;

    /**
     * The suffix of every placeholder
     */
    public String placeholderSuffix;

    /**
     * The target version up to which Flyway should consider migrations.
     * Migrations with a higher version number will be ignored.
     * The special value {@code current} designates the current version of the schema.
     */
    public String target;

    /**
     * An array of fully qualified FlywayCallback class implementations
     */
    public String[] callbacks;

    /**
     * If set to true, default built-in callbacks will be skipped, only custom migration callbacks will be used.
     * <p>(default: false)</p>
     */
    public Boolean skipDefaultCallbacks;

    /**
     * Allows migrations to be run "out of order"
     */
    public Boolean outOfOrder;

    /**
     * Whether to automatically call validate or not when running migrate. (default: true)
     */
    public Boolean validateOnMigrate;

    /**
     * Whether to automatically call clean or not when a validation error occurs
     */
    public Boolean cleanOnValidationError;

    /**
     * Ignore missing migrations when reading the metadata table. These are migrations that were performed by an
     * older deployment of the application that are no longer available in this version. For example: we have migrations
     * available on the classpath with versions 1.0 and 3.0. The metadata table indicates that a migration with version 2.0
     * (unknown to us) has also been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and Flyway continues normally. This is useful for situations where one must be able to deploy
     * a newer version of the application even though it doesn't contain migrations included with an older one anymore.
     * <p>
     * {@code true} to continue normally and log a warning, {@code false} to fail fast with an exception.
     * (default: {@code false})
     */
    public Boolean ignoreMissingMigrations;

    /**
     * Ignore future migrations when reading the metadata table. These are migrations that were performed by a
     * newer deployment of the application that are not yet available in this version. For example: we have migrations
     * available on the classpath up to version 3.0. The metadata table indicates that a migration to version 4.0
     * (unknown to us) has already been applied. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and Flyway continues normally. This is useful for situations where one must be able to redeploy
     * an older version of the application after the database has been migrated by a newer one. (default: {@code true})
     */
    public Boolean ignoreFutureMigrations;

    /**
     * Whether to disable clean. (default: {@code false})
     * <p>This is especially useful for production environments where running clean can be quite a career limiting move.</p>
     */
    public Boolean cleanDisabled;

    /**
     * <p>
     * Whether to automatically call baseline when migrate is executed against a non-empty schema with no metadata table.
     * This schema will then be baselined with the {@code baselineVersion} before executing the migrations.
     * Only migrations above {@code baselineVersion} will then be applied.
     * </p>
     * <p>
     * This is useful for initial Flyway production deployments on projects with an existing DB.
     * </p>
     * <p>
     * Be careful when enabling this as it removes the safety net that ensures
     * Flyway does not migrate the wrong database in case of a configuration mistake!
     * </p>
     * <p>
     * <p>{@code true} if baseline should be called on migrate for non-empty schemas, {@code false} if not. (default: {@code false})</p>
     */
    public Boolean baselineOnMigrate;

    /**
     * Whether to allow mixing transactional and non-transactional statements within the same migration.
     * <p>{@code true} if mixed migrations should be allowed. {@code false} if an error should be thrown instead. (default: {@code false})</p>
     */
    public Boolean mixed;

    /**
     * Whether to group all pending migrations together in the same transaction when applying them (only recommended for databases with support for DDL transactions).
     * <p>{@code true} if migrations should be grouped. {@code false} if they should be applied individually instead. (default: {@code false})</p>
     */
    public Boolean group;

    /**
     * The username that will be recorded in the metadata table as having applied the migration.
     * <p>
     * {@code null} for the current database user of the connection. (default: {@code null}).
     */
    public String installedBy;

    //[pro]
    /**
     * The fully qualified class name of the ErrorHandler for errors that occur during a migration. This can be used to customize Flyway's behavior by for example
     * throwing another runtime exception, outputting a warning or suppressing the error instead of throwing a FlywaySqlException.
     * <p>{@code null} if the default internal handler should be used instead. (default: {@code null})</p>
     */
    public String errorHandler;
    //[/pro]

    public AbstractFlywayTask() {
        super();
        setGroup("Flyway");
        extension = (FlywayExtension) getProject().getExtensions().getByName("flyway");
    }

    @TaskAction
    public Object runTask() {
        try {
            List<URL> extraURLs = new ArrayList<URL>();
            if (isJavaProject()) {
                JavaPluginConvention plugin = getProject().getConvention().getPlugin(JavaPluginConvention.class);

                for (SourceSet sourceSet : plugin.getSourceSets()) {
                    URL classesUrl = sourceSet.getOutput().getClassesDir().toURI().toURL();
                    getLogger().debug("Adding directory to Classpath: " + classesUrl);
                    extraURLs.add(classesUrl);

                    URL resourcesUrl = sourceSet.getOutput().getResourcesDir().toURI().toURL();
                    getLogger().debug("Adding directory to Classpath: " + resourcesUrl);
                    extraURLs.add(resourcesUrl);
                }

                addDependenciesWithScope(extraURLs, "compile");
                addDependenciesWithScope(extraURLs, "runtime");
                addDependenciesWithScope(extraURLs, "testCompile");
                addDependenciesWithScope(extraURLs, "testRuntime");
            }

            ClassLoader classLoader = new URLClassLoader(
                    extraURLs.toArray(new URL[extraURLs.size()]),
                    getProject().getBuildscript().getClassLoader());

            Flyway flyway = new Flyway(classLoader);
            flyway.configure(createFlywayConfig());
            return run(flyway);
        } catch (Exception e) {
            handleException(e);
            return null;
        }
    }

    private void addDependenciesWithScope(List<URL> urls, String scope) throws IOException {
        for (ResolvedArtifact artifact : getProject().getConfigurations().getByName(scope).getResolvedConfiguration().getResolvedArtifacts()) {
            URL artifactUrl = artifact.getFile().toURI().toURL();
            getLogger().debug("Adding Dependency to Classpath: " + artifactUrl);
            urls.add(artifactUrl);
        }
    }

    /**
     * Executes the task's custom behavior.
     */
    protected abstract Object run(Flyway flyway);

    /**
     * Creates the Flyway config to use.
     */
    private Map<String, String> createFlywayConfig() {
        Map<String, String> conf = new HashMap<String, String>();
        putIfSet(conf, ConfigurationUtils.DRIVER, driver, extension.driver);
        putIfSet(conf, ConfigurationUtils.URL, url, extension.url);
        putIfSet(conf, ConfigurationUtils.USER, user, extension.user);
        putIfSet(conf, ConfigurationUtils.PASSWORD, password, extension.password);
        putIfSet(conf, ConfigurationUtils.TABLE, table, extension.table);
        putIfSet(conf, ConfigurationUtils.BASELINE_VERSION, baselineVersion, extension.baselineVersion);
        putIfSet(conf, ConfigurationUtils.BASELINE_DESCRIPTION, baselineDescription, extension.baselineDescription);
        putIfSet(conf, ConfigurationUtils.SQL_MIGRATION_PREFIX, sqlMigrationPrefix, extension.sqlMigrationPrefix);
        putIfSet(conf, ConfigurationUtils.REPEATABLE_SQL_MIGRATION_PREFIX, repeatableSqlMigrationPrefix, extension.repeatableSqlMigrationPrefix);
        putIfSet(conf, ConfigurationUtils.SQL_MIGRATION_SEPARATOR, sqlMigrationSeparator, extension.sqlMigrationSeparator);
        putIfSet(conf, ConfigurationUtils.SQL_MIGRATION_SUFFIX, sqlMigrationSuffix, extension.sqlMigrationSuffix);
        putIfSet(conf, ConfigurationUtils.MIXED, mixed, extension.mixed);
        putIfSet(conf, ConfigurationUtils.GROUP, group, extension.group);
        putIfSet(conf, ConfigurationUtils.INSTALLED_BY, installedBy, extension.installedBy);
        putIfSet(conf, ConfigurationUtils.ENCODING, encoding, extension.encoding);
        putIfSet(conf, ConfigurationUtils.PLACEHOLDER_REPLACEMENT, placeholderReplacement, extension.placeholderReplacement);
        putIfSet(conf, ConfigurationUtils.PLACEHOLDER_PREFIX, placeholderPrefix, extension.placeholderPrefix);
        putIfSet(conf, ConfigurationUtils.PLACEHOLDER_SUFFIX, placeholderSuffix, extension.placeholderSuffix);
        putIfSet(conf, ConfigurationUtils.TARGET, target, extension.target);
        putIfSet(conf, ConfigurationUtils.OUT_OF_ORDER, outOfOrder, extension.outOfOrder);
        putIfSet(conf, ConfigurationUtils.VALIDATE_ON_MIGRATE, validateOnMigrate, extension.validateOnMigrate);
        putIfSet(conf, ConfigurationUtils.CLEAN_ON_VALIDATION_ERROR, cleanOnValidationError, extension.cleanOnValidationError);
        putIfSet(conf, ConfigurationUtils.IGNORE_MISSING_MIGRATIONS, ignoreMissingMigrations, extension.ignoreMissingMigrations);
        putIfSet(conf, ConfigurationUtils.IGNORE_FUTURE_MIGRATIONS, ignoreFutureMigrations, extension.ignoreFutureMigrations);
        putIfSet(conf, ConfigurationUtils.CLEAN_DISABLED, cleanDisabled, extension.cleanDisabled);
        putIfSet(conf, ConfigurationUtils.BASELINE_ON_MIGRATE, baselineOnMigrate, extension.baselineOnMigrate);
        putIfSet(conf, ConfigurationUtils.SKIP_DEFAULT_RESOLVERS, skipDefaultResolvers, extension.skipDefaultResolvers);
        putIfSet(conf, ConfigurationUtils.SKIP_DEFAULT_CALLBACKS, skipDefaultCallbacks, extension.skipDefaultCallbacks);
        putIfSet(conf, ConfigurationUtils.SCHEMAS, StringUtils.arrayToCommaDelimitedString(schemas), StringUtils.arrayToCommaDelimitedString(extension.schemas));

        conf.put(ConfigurationUtils.LOCATIONS, Location.FILESYSTEM_PREFIX + getProject().getProjectDir().getAbsolutePath() + "/src/main/resources/db/migration");
        putIfSet(conf, ConfigurationUtils.LOCATIONS, StringUtils.arrayToCommaDelimitedString(locations), StringUtils.arrayToCommaDelimitedString(extension.locations));

        putIfSet(conf, ConfigurationUtils.RESOLVERS, StringUtils.arrayToCommaDelimitedString(resolvers), StringUtils.arrayToCommaDelimitedString(extension.resolvers));
        putIfSet(conf, ConfigurationUtils.CALLBACKS, StringUtils.arrayToCommaDelimitedString(callbacks), StringUtils.arrayToCommaDelimitedString(extension.callbacks));

        //[pro]
        putIfSet(conf, ConfigurationUtils.ERROR_HANDLER, errorHandler, extension.errorHandler);
        //[/pro]

        if (placeholders != null) {
            for (Map.Entry<Object, Object> entry : placeholders.entrySet()) {
                conf.put(ConfigurationUtils.PLACEHOLDERS_PROPERTY_PREFIX + entry.getKey().toString(), entry.getValue().toString());
            }
        }
        if (extension.placeholders != null) {
            for (Map.Entry<Object, Object> entry : extension.placeholders.entrySet()) {
                conf.put(ConfigurationUtils.PLACEHOLDERS_PROPERTY_PREFIX + entry.getKey().toString(), entry.getValue().toString());
            }
        }

        addConfigFromProperties(conf, getProject().getProperties());
        addConfigFromProperties(conf, System.getProperties());
        addConfigFromProperties(conf, ConfigurationUtils.environmentVariablesToPropertyMap());

        return conf;
    }

    private static void addConfigFromProperties(Map<String, String> config, Properties properties) {
        for (String prop : properties.stringPropertyNames()) {
            if (prop.startsWith("flyway.")) {
                config.put(prop, properties.getProperty(prop));
            }
        }
    }

    private static void addConfigFromProperties(Map<String, String> config, Map<String, ?> properties) {
        for (String prop : properties.keySet()) {
            if (prop.startsWith("flyway.")) {
                config.put(prop, properties.get(prop).toString());
            }
        }
    }

    /**
     * @param throwable Throwable instance to be handled
     */
    private void handleException(Throwable throwable) {
        String message = "Error occurred while executing " + getName();
        throw new FlywayException(collectMessages(throwable, message), throwable);
    }

    /**
     * Collect error messages from the stack trace
     *
     * @param throwable Throwable instance from which the message should be build
     * @param message   the message to which the error message will be appended
     * @return a String containing the composed messages
     */
    private String collectMessages(Throwable throwable, String message) {
        if (throwable != null) {
            message += "\n" + throwable.getMessage();
            return collectMessages(throwable.getCause(), message);
        }
        return message;
    }

    /**
     * Puts this property in the config if it has been set either in the task or the extension.
     *
     * @param config         The config.
     * @param key            The peoperty name.
     * @param propValue      The value in the plugin.
     * @param extensionValue The value in the extension.
     */
    private void putIfSet(Map<String, String> config, String key, Object propValue, Object extensionValue) {
        if (propValue != null) {
            config.put(key, propValue.toString());
        } else if (extensionValue != null) {
            config.put(key, extensionValue.toString());
        }
    }

    private boolean isJavaProject() {
        return getProject().getPluginManager().hasPlugin("java");
    }
}
