package com.perforce.p4java.tests.dev.unit;

import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.MockCommandCallback;
import org.junit.Assert;

import java.util.Properties;

public class P4JavaRshTestCase extends P4JavaTestCase {
	public static void setupServer(String p4dUrl, String user, String password, boolean login, Properties props) throws Exception {
		setupServer(p4dUrl, user, password, login, props, true);
	}

	public static void setupServer(String p4dUrl, String user, String password, boolean login, Properties props,
			boolean requireClient) throws Exception {
		server = ServerFactory.getOptionsServer(p4dUrl, props);
		Assert.assertNotNull(server);

		server.registerCallback(new ICommandCallback() {
			@Override
			public void issuingServerCommand(int key, String commandString) {
				serverCommand = commandString;
			}

			@Override
			public void completedServerCommand(int key, long millisecsTaken) {

			}

			@Override
			public void receivedServerInfoLine(int key, IServerMessage infoLine) {
				serverMessage = infoLine;
			}

			@Override
			public void receivedServerErrorLine(int key, IServerMessage errorLine) {
				serverMessage = errorLine;
			}

			@Override
			public void receivedServerMessage(int key, IServerMessage message) {
				serverMessage = message;
			}
		});
		server.connect();

		setUtf8CharsetIfServerSupportUnicode(server);
		server.setUserName((user == null || user.isEmpty()) ? getUserName() : user);
		if (login) {
			server.login((password == null || password.isEmpty()) ? getPassword() : password,
					new LoginOptions());
		}
		if (requireClient) {
			server.setCurrentClient(getDefaultClient(server));
		}
	}
}
