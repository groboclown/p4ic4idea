package com.perforce.p4java.tests.dev.unit;

import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.LocalServerRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.perforce.p4java.tests.dev.unit.P4JavaTestCase.props;

public class P4JavaLocalTest extends P4JavaLocalServerTestCase {

	@ClassRule
	public static LocalServerRule p4d = new LocalServerRule("r18.1", P4JavaLocalTest.class.getSimpleName(), "localhost:18100");

	@Test
	public void testServerInfo() throws Exception {
		IOptionsServer server = ServerFactory.getOptionsServer(p4d.getP4JavaUri(), props);
		Assert.assertNotNull(server);

		server.connect();
		IServerInfo info = server.getServerInfo();
		String root = info.getServerRoot();

		Path path = Paths.get(root);

		Assert.assertEquals(p4d.getPathToRoot(), path.toString());
	}
}
