/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Miscellaneous tests of request exception error code features
 * introduced as a result of job043157 and general experience.
 */
@TestId("Features102_RequestExceptionCodesTest")
public class RequestExceptionCodesTest extends P4JavaTestCase {

	public RequestExceptionCodesTest() {
	}

	/**
	 * Just test constructors and fields against known returns.
	 */
	@Test
	public void testRequestExceptionCodeBasics() {
		IOptionsServer server = null;
		final String msg01 = "You don't have permission for this operation.";
		final String errStr01 = "805705769";
		final int errCode01 = new Integer(errStr01);
		final int severity01 = RpcMessage.getSeverity(errStr01);
		final int generic01 = RpcMessage.getGeneric(errStr01);
		final int unique01 = (errCode01 & 0xFFFF);
		final int subSystem01 = ((errCode01 >> 10) & 0x3F);
		final int subCode01 = ((errCode01 >> 0) & 0x3FF);
		
		try {
			RequestException reqExc = new RequestException(msg01, errStr01);
			assertEquals("message mismatch", msg01, reqExc.getMessage());
			assertEquals("raw code mismatch", errCode01, reqExc.getRawCode());
			assertEquals("severity mismatch", severity01, reqExc.getSeverityCode());
			assertEquals("generic mismatch", generic01, reqExc.getGenericCode());
			assertEquals("unique mismatch", unique01, reqExc.getUniqueCode());
			assertEquals("subsystem mismatch", subSystem01, reqExc.getSubSystem());
			assertEquals("subs code mismatch", subCode01, reqExc.getSubCode());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	/**
	 * Test the FileSpec constructors for the new code setup.
	 */
	@Test
	public void testFileSpecCodeBasics() {
		IOptionsServer server = null;
		String msg01 = null;
		final String errStr01 = "554768759";
		final int errCode01 = new Integer(errStr01);
		final int severity01 = RpcMessage.getSeverity(errStr01);
		final int generic01 = RpcMessage.getGeneric(errStr01);
		final int unique01 = (errCode01 & 0xFFFF);
		final int subSystem01 = ((errCode01 >> 10) & 0x3F);
		final int subCode01 = ((errCode01 >> 0) & 0x3FF);
		final String filePath = "//depot/xyzabc/...";

		try {
			server = getServer();
			Map<String, Object>[] maps = server.execMapCmd("files",
											new String[] {filePath}, null);
			assertNotNull("null map results", maps);
			assertEquals("wrong maps length", 1, maps.length);
			Map<String, Object> map = maps[0];
			assertNotNull("null map[0]", map);
			IFileSpec fileSpec = new FileSpec(FileSpecOpStatus.ERROR,
										RpcMessage.interpolateArgs((String) map.get("fmt0"), map),
										(String) map.get("code0"));
			msg01 = RpcMessage.interpolateArgs((String) map.get("fmt0"), map);
			assertEquals("message mismatch", msg01, fileSpec.getStatusMessage());
			assertEquals("raw code mismatch", errCode01, fileSpec.getRawCode());
			assertEquals("severity mismatch", severity01, fileSpec.getSeverityCode());
			assertEquals("generic mismatch", generic01, fileSpec.getGenericCode());
			assertEquals("unique mismatch", unique01, fileSpec.getUniqueCode());
			assertEquals("subsystem mismatch", subSystem01, fileSpec.getSubSystem());
			assertEquals("subs code mismatch", subCode01, fileSpec.getSubCode());
			
			List<IFileSpec> files = server.getDepotFiles(
										FileSpecBuilder.makeFileSpecList(filePath), null);
			assertNotNull("null files list returned from server", files);
			assertEquals("list size mismatch", 1, files.size());
			IFileSpec fSpec = files.get(0);
			assertNotNull("null file spec in returned files list", fSpec);
			assertEquals("wrong op status", FileSpecOpStatus.ERROR, fSpec.getOpStatus());
			assertEquals("mismatched error message", msg01, fSpec.getStatusMessage());
			assertEquals("mismatched raw code", errCode01, fSpec.getRawCode());
			assertEquals("mismatched generic code", generic01, fSpec.getGenericCode());
			assertEquals("mismatched severity code", severity01, fSpec.getSeverityCode());
			assertEquals("mismatched unique code", unique01, fSpec.getUniqueCode());
			assertEquals("mismatched subsystem code", subSystem01, fSpec.getSubSystem());
			assertEquals("mismatched sub code code", subCode01, fSpec.getSubCode());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
