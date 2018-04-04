/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 */
@Standalone
@Jobs({"job039640"})
@TestId("Bugs101_Job039640Test")
public class Job039640Test extends P4JavaTestCase {

	public Job039640Test() {
	}

	@Test
	public void testJob039640DeleteOptionsProcessing()
			throws OptionsException {
		DeleteFilesOptions opts = new DeleteFilesOptions();
		opts.setChangelistId(IChangelist.DEFAULT);
		List<String> optsList = opts.processOptions(null);
		assertNotNull(optsList);
		assertEquals(0, optsList.size());
	}
}
