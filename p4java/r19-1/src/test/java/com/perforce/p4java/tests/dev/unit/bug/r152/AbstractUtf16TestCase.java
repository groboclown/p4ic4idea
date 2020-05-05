/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test utf16-le encoded files: UTF-16LE BOM: "ff fe"
 * sync'd to client as UTF-16BE BOM: "fe ff"
 */

public abstract class AbstractUtf16TestCase extends P4JavaRshTestCase {
	protected IOptionsServer server = null;
	protected IClient client = null;
	protected List<IFileSpec> files = null;
	protected String serverMessage = null;

	protected void setUp(String serverUrl, String charsetName, String clientName) {
		// initialization code (before each test).
		try {
			server = ServerFactory.getOptionsServer(serverUrl, null);
			assertNotNull(server);

			// Register callback
			ICommandCallback callback = new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode, int severityCode, String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					serverMessage = String.valueOf(millisecsTaken);
				}
			};

			server.registerCallback(callback);
			// Add trust WITH 'force' and 'autoAccept' options
			//server.addTrust(new TrustOptions().setForce(true).setAutoAccept(true));
			// Connect to server
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName(charsetName);
				}
			}
			server.setUserName("p4jtestuser");
			server.login("p4jtestuser");
			client = server.getClient(clientName);
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
