package com.perforce.p4java.tests.dev.unit.features173;

import com.perforce.p4java.impl.mapbased.rpc.sys.helper.Utf8ByteHelper;
import org.junit.Assert;
import org.junit.Test;

public class TestUtf8ByteHelper {
	@Test
	public void testUtf8ByteHelpers() {
		Assert.assertEquals(Utf8ByteHelper.SINGLE, Utf8ByteHelper.parse((byte)0x25));

		Assert.assertEquals(Utf8ByteHelper.MULTI, Utf8ByteHelper.parse((byte)0x85));
		Assert.assertEquals(Utf8ByteHelper.MULTI, Utf8ByteHelper.parse((byte)0xB5));

		Assert.assertEquals(Utf8ByteHelper.MULTI, Utf8ByteHelper.parse((byte)0x80));
		Assert.assertEquals(Utf8ByteHelper.MULTI, Utf8ByteHelper.parse((byte)0xB0));

		Assert.assertEquals(Utf8ByteHelper.MULTI, Utf8ByteHelper.parse((byte)0x8F));
		Assert.assertEquals(Utf8ByteHelper.MULTI, Utf8ByteHelper.parse((byte)0xBF));

		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xD5));
		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xE5));
		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xF5));

		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xD0));
		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xE0));
		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xF0));

		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xDF));
		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xEF));
		Assert.assertEquals(Utf8ByteHelper.START, Utf8ByteHelper.parse((byte)0xF7));

		Assert.assertEquals(Utf8ByteHelper.UNKNOWN, Utf8ByteHelper.parse((byte)0xF8));
		Assert.assertEquals(Utf8ByteHelper.UNKNOWN, Utf8ByteHelper.parse((byte)0xFF));
	}

	@Test
	public void testUTF8StartLength() {
		Assert.assertEquals(0, Utf8ByteHelper.length((byte)0x25));
		Assert.assertEquals(0, Utf8ByteHelper.length((byte)0x85));

		Assert.assertEquals(1, Utf8ByteHelper.length((byte)0xD5));
		Assert.assertEquals(2, Utf8ByteHelper.length((byte)0xE5));
		Assert.assertEquals(3, Utf8ByteHelper.length((byte)0xF5));
	}
}