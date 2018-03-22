package com.perforce.p4java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.UnsupportedCharsetException;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.FileDecoderException;
import com.perforce.p4java.exception.FileEncoderException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.server.PerforceCharsets;

public class CharsetConverterTest extends AbstractP4JavaUnitTest {

    private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";
    
	@Test
	public void testCharsetConverterCharsetCharsetValid() throws IOException, UnsupportedCharsetException, ConnectionException, RequestException, AccessException, NoSuchObjectException, ConfigException, ResourceException, URISyntaxException, FileDecoderException, FileEncoderException {
		File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/shift_jis.txt");
		
		CharsetConverter convert = new CharsetConverter(PerforceCharsets.getP4Charset("shiftjis"), CharsetDefs.UTF8);
		
		InputStream inStream = new FileInputStream(testResourceFile);
		
		byte[] bytes = new byte[2048];
		int read = inStream.read(bytes);
		inStream.close();
		
		byte[] trueIn = new byte[read];
		System.arraycopy(bytes, 0, trueIn, 0, read);
		
		ByteBuffer bufIn  = ByteBuffer.wrap(bytes, 0, read);
		ByteBuffer bufOut = convert.convert(bufIn);
		
		byte[] out = bufOut.array();
		
		CharsetConverter convert2 = new CharsetConverter(CharsetDefs.UTF8, PerforceCharsets.getP4Charset("shiftjis"));
		
		ByteBuffer bufIn2  = ByteBuffer.wrap(out, 0, bufOut.limit());
		ByteBuffer bufOut2 = convert2.convert(bufIn2);
		
		byte[] out2 = bufOut2.array();

		byte[] trueOut = new byte[bufOut2.limit()];
		System.arraycopy(out2, 0, trueOut, 0, bufOut2.limit());
		
		Assert.assertArrayEquals(trueIn, trueOut);
		
	}
	

	@Test(expected=FileDecoderException.class)
	public void testCharsetConverterCharsetCharsetInvalid() throws IOException, UnsupportedCharsetException, ConnectionException, RequestException, AccessException, NoSuchObjectException, ConfigException, ResourceException, URISyntaxException, FileDecoderException, FileEncoderException {
		File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/euc-jp.txt");
		
		CharsetConverter convert = new CharsetConverter(PerforceCharsets.getP4Charset("shiftjis"), CharsetDefs.UTF8);
		
		InputStream inStream = new FileInputStream(testResourceFile);

		byte[] bytes = new byte[2048];
		int read = inStream.read(bytes);
		inStream.close();
		
		byte[] trueIn = new byte[read];
		System.arraycopy(bytes, 0, trueIn, 0, read);
		
		ByteBuffer bufIn  = ByteBuffer.wrap(bytes, 0, read);
		convert.convert(bufIn);
	}


}
