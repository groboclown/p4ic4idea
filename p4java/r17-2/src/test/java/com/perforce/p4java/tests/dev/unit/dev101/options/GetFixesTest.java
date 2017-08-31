/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.option.server.GetFixesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * A testbed for Options and FixListOptions development. Not intended
 * for use as-is in the real test suites. Won't always pass, due to
 * timing issues...
 * 
 * TODO: split this out into multiple tests.
 */

@Jobs({"job039408"})
@TestId("Dev101_GetFixesTest")
public class GetFixesTest extends P4JavaTestCase {
	
	public static final String DEPOT_PATH = "//depot/...";
	public static final String SERVER_URL = "p4java://eng-p4java-vm.perforce.com:20101";
	public static final Integer MAX_CHANGELISTS = 10;

	public GetFixesTest() {
	}

	@Test
	public void testOldVsNew() {
		IOptionsServer server = null;
		
		try {
			server = ServerFactory.getOptionsServer(SERVER_URL, null);
			assertNotNull(server);
			server.setUserName(P4JTEST_NOLOGINNAME_DEFAULT);
			server.connect();
			
			List<IFix> fixListOld = server.getFixList(null,
								IChangelist.UNKNOWN, null, false, 0);
			List<IFix> fixListNew = server.getFixes(null,
								new GetFixesOptions(IChangelist.UNKNOWN, null, false, 0));
			assertNotNull(fixListOld);
			assertNotNull(fixListNew);
			compareLists(fixListNew, fixListOld);
			
			fixListOld = server.getFixList(null,
								IChangelist.UNKNOWN, null, false, MAX_CHANGELISTS);
			fixListNew = server.getFixes(null,
								new GetFixesOptions(IChangelist.UNKNOWN, null, false, MAX_CHANGELISTS));
			assertNotNull(fixListOld);
			assertNotNull(fixListNew);
			compareLists(fixListNew, fixListOld);
			
			fixListOld = server.getFixList(null,
								IChangelist.UNKNOWN, null, false, MAX_CHANGELISTS);
			fixListNew = server.getFixes(null,
								new GetFixesOptions("-m" + MAX_CHANGELISTS.toString()));
			assertNotNull(fixListOld);
			assertNotNull(fixListNew);
			compareLists(fixListNew, fixListOld);

			fixListOld = server.getFixList(FileSpecBuilder.makeFileSpecList(DEPOT_PATH),
								IChangelist.UNKNOWN, null, false, 0);
			fixListNew = server.getFixes(FileSpecBuilder.makeFileSpecList(DEPOT_PATH),
								new GetFixesOptions(IChangelist.UNKNOWN, null, false, 0));
			assertNotNull(fixListOld);
			assertNotNull(fixListNew);
			compareLists(fixListNew, fixListOld);
			
			fixListOld = server.getFixList(FileSpecBuilder.makeFileSpecList(DEPOT_PATH),
								IChangelist.UNKNOWN, null, true, 0);
			fixListNew = server.getFixes(FileSpecBuilder.makeFileSpecList(DEPOT_PATH),
								new GetFixesOptions(IChangelist.UNKNOWN, null, true, 0));
			assertNotNull(fixListOld);
			assertNotNull(fixListNew);
			compareLists(fixListNew, fixListOld);
			
			fixListOld = server.getFixList(FileSpecBuilder.makeFileSpecList(DEPOT_PATH),
								IChangelist.UNKNOWN, null, true, 0);
			fixListNew = server.getFixes(FileSpecBuilder.makeFileSpecList(DEPOT_PATH),
								new GetFixesOptions("-i"));
			assertNotNull(fixListOld);
			assertNotNull(fixListNew);
			compareLists(fixListNew, fixListOld);
			
			GetFixesOptions opts = new GetFixesOptions();
			opts.setIncludeIntegrations(true);
			fixListOld = server.getFixList(FileSpecBuilder.makeFileSpecList(DEPOT_PATH),
								IChangelist.UNKNOWN, null, true, 0);
			fixListNew = server.getFixes(FileSpecBuilder.makeFileSpecList(DEPOT_PATH),
								opts);
			assertNotNull(fixListOld);
			assertNotNull(fixListNew);
			compareLists(fixListNew, fixListOld);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	public void compareLists(List<IFix> newList, List<IFix> oldList) {
		assertEquals(oldList.size(), newList.size());
		
		for (int i = 0; i < oldList.size(); i++) {
			IFix oldFix = oldList.get(i);
			IFix newFix = newList.get(i);
			assertNotNull(oldFix);
			assertNotNull(newFix);
			assertEquals(oldList.get(i).getStatus(), newList.get(i).getStatus());
			assertEquals(oldList.get(i).getAction(), newList.get(i).getAction());
			assertEquals(oldList.get(i).getChangelistId(), newList.get(i).getChangelistId());
			assertEquals(oldList.get(i).getDate(), newList.get(i).getDate());
			assertEquals(oldList.get(i).getJobId(), newList.get(i).getJobId());
			assertEquals(oldList.get(i).getClientName(), newList.get(i).getClientName());
			assertEquals(oldList.get(i).getUserName(), newList.get(i).getUserName());
		}
	}
}
