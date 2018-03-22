package com.perforce.p4java.tests.dev.unit;

import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.MockCommandCallback;
import org.junit.Assert;

import java.util.Properties;

public class P4JavaRshTestCase extends P4JavaTestCase {

	public static void setupServer(String p4dUrl, String user, String password, boolean login, Properties props) throws Exception {
		server = ServerFactory.getOptionsServer(p4dUrl, props);
		Assert.assertNotNull(server);

		server.registerCallback(new MockCommandCallback());
		server.connect();

		setUtf8CharsetIfServerSupportUnicode(server);
		server.setUserName((user == null || user.isEmpty()) ? getUserName() : user);
		if (login) {
			server.login((password == null || password.isEmpty()) ? getPassword() : password,
					new LoginOptions());
		}
		server.setCurrentClient(getDefaultClient(server));
	}
}
