package com.perforce.p4java.tests.dev.unit;

import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.ServerFactory;
import org.junit.Assert;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class P4JavaLocalServerTestCase extends P4JavaTestCase {

	public static void setupServer(String p4dUrl, String user, String password, boolean login, Properties props) throws Exception {
		server = getServer(p4dUrl, props);
		Assert.assertNotNull(server);

		server.registerCallback(createCommandCallback());
		server.connect();

		setUtf8CharsetIfServerSupportUnicode(server);
		server.setUserName((user == null || user.isEmpty()) ? getUserName() : user);
		if (login) {
			server.login((password == null || password.isEmpty()) ? getPassword() : password,
					new LoginOptions());
		}
		server.setCurrentClient(getDefaultClient(server));
	}

	public static void setupSSLServer(String p4dUrl, String userName, String password, boolean login, Properties props) throws Exception {
		server = ServerFactory.getOptionsServer(p4dUrl, props);

		server.removeTrust();
		// assume a new first time connection
		server.addTrust(new TrustOptions().setAutoAccept(true));
		assertNotNull(server);

		// Register callback
		server.registerCallback(createCommandCallback());
		// Connect to the server.
		server.connect();
		setUtf8CharsetIfServerSupportUnicode(server);

		// Set the server user
		server.setUserName(userName);

		// Login using the normal method
		if (login) {
			server.login(password, new LoginOptions());
		}

		server.setCurrentClient(getDefaultClient(server));
	}

}
