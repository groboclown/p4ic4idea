/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("FileSpecOpStatusTest")
public class FileSpecOpStatusTest extends BaseEnumTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#getEnum()
     */
    protected FileSpecOpStatus[] getEnum() {
	return FileSpecOpStatus.values();
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#valueOf(java.lang.String)
     */
    protected FileSpecOpStatus valueOf(String name) {
	return FileSpecOpStatus.valueOf(name);
    }

}
