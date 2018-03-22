/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import com.perforce.p4java.server.callback.ILogCallback;
import com.perforce.p4java.server.callback.ILogCallback.LogTraceLevel;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("LogCallbackTest")
public class LogCallbackTest extends BaseEnumTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#getEnum()
     */
    protected LogTraceLevel[] getEnum() {
	return LogTraceLevel.values();
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#valueOf(String)
     */
    protected LogTraceLevel valueOf(String name) {
	return LogTraceLevel.valueOf(name);
    }
}
