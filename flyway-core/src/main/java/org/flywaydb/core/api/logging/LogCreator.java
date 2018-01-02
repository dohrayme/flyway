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
package org.flywaydb.core.api.logging;

/**
 * Factory for implementation-specific loggers.
 */
public interface LogCreator {
    /**
     * Creates an implementation-specific logger for this class.
     *
     * @param clazz The class to create the logger for.
     * @return The logger.
     */
    Log createLogger(Class<?> clazz);
}
