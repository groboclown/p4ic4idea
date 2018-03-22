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
     * @see BaseEnumTestHelper#getEnum()
     */
    protected FileSpecOpStatus[] getEnum() {
	return FileSpecOpStatus.values();
    }

    /**
     * @see BaseEnumTestHelper#valueOf(String)
     */
    protected FileSpecOpStatus valueOf(String name) {
	return FileSpecOpStatus.valueOf(name);
    }

}
