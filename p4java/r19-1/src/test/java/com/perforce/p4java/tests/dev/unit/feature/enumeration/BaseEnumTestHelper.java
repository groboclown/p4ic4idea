/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
public abstract class BaseEnumTestHelper extends P4JavaRshTestCase {

    /**
     * Test enum
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEnum() {
	Enum<?>[] values = getEnum();
	Assert.assertNotNull(values);
	Assert.assertTrue(values.length > 0);
	for (Enum value : values) {
	    assertNotNull("Null status in enum array", value);
	    assertNotNull("Null name in enum", value.name());
	    assertNotNull("Null toString in enum", value.toString());
	    assertEquals("valueOf did not return specified enum for name()",
		    value, valueOf(value.name()));
	}
    }

    /**
     * Get enum
     * 
     * @return enum
     */
    protected abstract Enum<?>[] getEnum();

    /**
     * Get value of name enum
     * 
     * @param name
     * @return value
     */
    protected abstract Enum<?> valueOf(String name);

}
