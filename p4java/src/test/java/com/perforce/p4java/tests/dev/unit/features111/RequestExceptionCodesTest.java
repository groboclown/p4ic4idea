/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static com.perforce.p4java.tests.ServerMessageMatcher.isText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.server.IServerMessage;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.features121.GetStreamOptionsTest;

/**
 * Miscellaneous tests of request exception error code features
 * introduced as a result of job043157 and general experience.
 */
@TestId("Features102_RequestExceptionCodesTest")
public class RequestExceptionCodesTest extends P4JavaRshTestCase {

	public RequestExceptionCodesTest() {
	}


    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", RequestExceptionCodesTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            Properties properties = new Properties();
            setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
            assertNotNull(server);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }
	/**
	 * Just test constructors and fields against known returns.
	 */
	@Test
	public void testRequestExceptionCodeBasics() {
		final String msg01 = "You don't have permission for this operation.";
		final String errStr01 = "805705769";
		final int errCode01 = new Integer(errStr01);
		final int severity01 = RpcMessage.getSeverity(errStr01);
		final int generic01 = RpcMessage.getGeneric(errStr01);
		final int unique01 = (errCode01 & 0xFFFF);
		final int subSystem01 = ((errCode01 >> 10) & 0x3F);
		final int subCode01 = ((errCode01 >> 0) & 0x3FF);
		
		try {
			IServerMessage msg = dummyServerMessage(msg01, errStr01);
			RequestException reqExc = new RequestException(msg);
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
			Map<String, Object>[] maps = server.execMapCmd("files",
											new String[] {filePath}, null);
			assertNotNull("null map results", maps);
			assertEquals("wrong maps length", 1, maps.length);
			Map<String, Object> map = maps[0];
			assertNotNull("null map[0]", map);
			IFileSpec fileSpec = dummyFileSpec(FileSpecOpStatus.ERROR, map);
			msg01 = RpcMessage.interpolateArgs((String) map.get("fmt0"), map);
			assertThat("message mismatch",
					fileSpec.getStatusMessage(),
					isText(msg01));
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
			// p4ic4idea: this is now an info status.
			assertEquals("wrong op status", FileSpecOpStatus.INFO, fSpec.getOpStatus());
			assertThat("mismatched error message",
					fSpec.getStatusMessage(),
					isText(msg01));
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
