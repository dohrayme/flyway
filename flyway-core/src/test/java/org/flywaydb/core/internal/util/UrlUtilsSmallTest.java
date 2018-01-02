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
package org.flywaydb.core.internal.util;

import org.flywaydb.core.internal.util.UrlUtils;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;

public class UrlUtilsSmallTest {
    @Test
    public void toFilePath() throws MalformedURLException {
        File file = new File("/test dir/a+b");
        assertEquals(file.getAbsolutePath(), UrlUtils.toFilePath(file.toURI().toURL()));
    }
}
