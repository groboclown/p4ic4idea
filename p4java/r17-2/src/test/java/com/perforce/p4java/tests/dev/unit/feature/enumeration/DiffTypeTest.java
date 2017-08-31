/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import com.perforce.p4java.core.file.DiffType;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
public class DiffTypeTest extends BaseEnumTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#getEnum()
     */
    protected DiffType[] getEnum() {
	return DiffType.values();
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#valueOf(java.lang.String)
     */
    protected DiffType valueOf(String name) {
	return DiffType.valueOf(name);
    }

}
