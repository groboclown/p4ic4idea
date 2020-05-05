package com.perforce.p4java.tests.dev.unit.bug.r173;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

@Jobs({ "job093734" })
public class Job093734Test extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", P4JavaRshTestCase.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		setupServer(p4d.getRSHURL(), userName, userName, false, properties);
	}

	@Test(expected = RequestException.class)
	public void testPrintFileNotFound() throws P4JavaException {
		List<IFileSpec> file = FileSpecBuilder.makeFileSpecList("//depot/no/such/file");
		GetFileContentsOptions opts = new GetFileContentsOptions();
		opts.setNoHeaderLine(true);
		server.getFileContents(file, opts);
	}

}
