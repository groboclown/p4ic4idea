/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.charset;

import com.perforce.p4java.CharsetConverter;
import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("CharsetConverterTest")
public class CharsetConverterTest extends P4JavaRshTestCase {
    /**
     * Test byte buffer conversion using utf 8 character set
     * @throws Exception 
     */
    @Test
    public void testByteConversionUtf8() throws Exception {
	Charset set = CharsetDefs.UTF8;
	CharsetConverter converter = new CharsetConverter(set, set, false);
	ByteBuffer buffer;
	buffer = ByteBuffer.wrap("test".getBytes(set.name()));
	ByteBuffer converted = converter.convert(buffer);
	buffer.position(0);
	assertEquals(0, converted.position());
	assertEquals(buffer.limit(), converted.limit());
	assertEquals(buffer, converted);
    }

    /**
     * Test byte buffer conversion using utf 8 character set
     * @throws Exception 
     */
    @Test
    public void testCharConversionUtf8() throws Exception {
	Charset set = CharsetDefs.UTF8;
	String value = "test";
	CharsetConverter converter = new CharsetConverter(set, set, false);
	ByteBuffer bytes = ByteBuffer.wrap(value.getBytes(set.name()));
	CharBuffer buffer = CharBuffer.wrap(value);
	ByteBuffer converted = converter.convert(buffer);
	assertEquals(bytes.position(), converted.position());
	assertEquals(bytes.limit(), converted.limit());
	assertEquals(bytes, converted);
    }

}
