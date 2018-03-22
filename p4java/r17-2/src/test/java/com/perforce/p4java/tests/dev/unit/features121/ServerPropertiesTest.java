/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test server construction with properties
 */
@Jobs({ "job051534" })
@TestId("Dev121_ServerPropertiesTest")
public class ServerPropertiesTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;
	long completedTime = 0;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
	}

	/**
	 * Test server properties
	 * @throws P4JavaException 
	 */
	@Test
	public void testServerProperties() {

		String[] serverUris = {
				"p4java://eng-p4java-vm.perforce.com:20121?socketPoolSize=10&testKey1=testVal1",
				"p4javassl://eng-p4java-vm.perforce.com:30121?socketPoolSize=10&testKey1=testVal1",
				"p4jrpc://eng-p4java-vm.perforce.com:20121?socketPoolSize=10&testKey1=testVal1",
				"p4jrpcssl://eng-p4java-vm.perforce.com:30121?socketPoolSize=10&testKey1=testVal1",
				"p4jrpcnts://eng-p4java-vm.perforce.com:20121?socketPoolSize=10&testKey1=testVal1",
				"p4jrpcntsssl://eng-p4java-vm.perforce.com:30121?socketPoolSize=10&testKey1=testVal1" };

		try {
			// List all the providers and the algorithms supporter
			for (Provider provider : Security.getProviders()) {
			    debugPrint("Provider: " + provider.getName());
				for (Provider.Service service : provider.getServices()) {
				    debugPrint("  Algorithm: " + service.getAlgorithm());
				}
			}

			// List supported protocol versions for this connection
			Socket socket = SSLSocketFactory.getDefault().createSocket();
			String[] supportedProtocols = ((SSLSocket) socket)
					.getSupportedProtocols();
			for (String supportedProtocol : supportedProtocols) {
			    debugPrint("Supported Protocol Version: "
						+ supportedProtocol);
			}
			// List enabled protocol versions for this connection
			String[] enabledProtocols = ((SSLSocket) socket)
					.getEnabledProtocols();
			for (String enabledProtocol : enabledProtocols) {
			    debugPrint("Enabled Protocol Version: "
						+ enabledProtocol);
			}

			Properties props = new Properties();

			// props.put("secureSocketTrustAll", "false");
			// props.put("secureSocketProtocol", "TLS");
			props.put("secureSocketProtocol", "SSL");
			// props.put("secureSocketSetEnabledProptocols", "false");
			// props.put("secureSocketEnabledProtocols", "SSLv3, TLSv1");
			props.put("secureSocketEnabledProtocols", "TLSv1, SSLv3");
			// props.put("secureSocketEnabledProtocols", "TLSv1");

			for (String serverUri : serverUris) {
				server = ServerFactory.getOptionsServer(serverUri, props);
				assertNotNull(server);

				// Register callback
				server.registerCallback(new ICommandCallback() {
					public void receivedServerMessage(int key, int genericCode,
							int severityCode, String message) {
						serverMessage = message;
					}

					public void receivedServerInfoLine(int key, String infoLine) {
						serverMessage = infoLine;
					}

					public void receivedServerErrorLine(int key,
							String errorLine) {
						serverMessage = errorLine;
					}

					public void issuingServerCommand(int key, String command) {
						serverMessage = command;
					}

					public void completedServerCommand(int key,
							long millisecsTaken) {
						completedTime = millisecsTaken;
					}
				});
				// Connect to the server.
				try {
				    server.connect();
				} catch (ConnectionException ce) {
				    if (!(ce.getCause() instanceof TrustException)) {
				        throw(ce);
				    }
                    server.addTrust(new TrustOptions().setAutoAccept(true).setForce(true));
    				server.connect();
				}
				if (server.isConnected()) {
					if (server.supportsUnicode()) {
						server.setCharsetName("utf8");
					}
				}

				// Set the server user
				server.setUserName(this.userName);

				// Login using the normal method
				server.login(this.password, new LoginOptions());

				client = server.getClient("p4TestUserWS20112");
				assertNotNull(client);
				server.setCurrentClient(client);

				IServerInfo serverInfo = server.getServerInfo();
				assertNotNull(serverInfo);
			}

		} catch (Exception e) {
		    StringWriter sw = new StringWriter();
		    e.printStackTrace(new PrintWriter(sw));
			fail("Unexpected exception: " + e.getLocalizedMessage() +
			        "\n" + sw.toString() );
		}
	}
}
