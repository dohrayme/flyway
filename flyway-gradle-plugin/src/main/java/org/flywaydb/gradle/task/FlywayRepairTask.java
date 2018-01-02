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
package org.flywaydb.gradle.task;

import org.flywaydb.core.Flyway;

/**
 * Repairs the Flyway schema history table. This will perform the following actions:
 * <ul>
 * <li>Remove any failed migrations on databases without DDL transactions (User objects left behind must still be cleaned up manually)</li>
 * <li>Realign the checksums, descriptions and types of the applied migrations with the ones of the available migrations</li>
 * </ul>
 */
public class FlywayRepairTask extends AbstractFlywayTask {
    public FlywayRepairTask() {
        super();
        setDescription("Repairs the Flyway schema history table.");
    }

    @Override
    protected Object run(Flyway flyway) {
        flyway.repair();
        return null;
    }
}
