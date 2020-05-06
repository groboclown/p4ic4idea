/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import org.junit.Test;

import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.tests.dev.annotations.TestId;

import junit.framework.Assert;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("FileActionTest")
public class FileActionTest extends BaseEnumTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#getEnum()
     */
    protected FileAction[] getEnum() {
	return FileAction.values();
    }

    /**
     * Test {@link FileAction#fromString(String)}
     */
    @Test
    public void testFromString() {
	for (FileAction action : getEnum()) {
	    Assert.assertEquals(action, FileAction
		    .fromString(action.toString()));
	}
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.enumeration.BaseEnumTestHelper#valueOf(java.lang.String)
     */
    protected FileAction valueOf(String name) {
	return FileAction.valueOf(name);
    }

}
