/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.tests.dev.annotations.TestId;
import org.junit.Test;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("ConfigExceptionTest")
// p4ic4idea: ConfigException is now abstract, so the test doesn't do anything...
public class ConfigExceptionTest {
    @Test
    public void testNoTests() {}
}
