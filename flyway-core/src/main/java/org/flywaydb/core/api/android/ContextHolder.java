/*
 * Copyright 2010-2017 Boxfuse GmbH
 *
 * INTERNAL RELEASE. ALL RIGHTS RESERVED.
 */
package org.flywaydb.core.api.android;

import android.content.Context;

/**
 * Holds an Android context. The context must be set for Flyway to be able to scan assets and classes for migrations.
 *
 * <p>
 *     You can set this within an activity using ContextHolder.setContext(this);
 * </p>
 */
public class ContextHolder {
    private ContextHolder() {}

    /**
     * The Android context to use.
     */
    private static Context context;

    /**
     * @return The Android context to use to be able to scan assets and classes for migrations.
     */
    public static Context getContext() {
        return context;
    }

    /**
     * @param context The Android context to use to be able to scan assets and classes for migrations.
     */
    public static void setContext(Context context) {
        ContextHolder.context = context;
    }
}
