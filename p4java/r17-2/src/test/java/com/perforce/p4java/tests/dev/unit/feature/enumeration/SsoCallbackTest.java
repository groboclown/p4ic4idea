/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import com.perforce.p4java.server.callback.ISSOCallback;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("SsoCallbackTest")
public class SsoCallbackTest extends BaseEnumTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#getEnum()
     */
    protected ISSOCallback.Status[] getEnum() {
	return ISSOCallback.Status.values();
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#valueOf(java.lang.String)
     */
    protected ISSOCallback.Status valueOf(String name) {
	return ISSOCallback.Status.valueOf(name);
    }
}
