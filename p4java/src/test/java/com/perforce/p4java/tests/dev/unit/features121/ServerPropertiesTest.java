/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.MockCommandCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.ClassRule;
import org.junit.Test;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test server construction with properties
 */
@Jobs({ "job051534" })
@TestId("Dev121_ServerPropertiesTest")
public class ServerPropertiesTest extends P4JavaRshTestCase { //TODO: still need to remove us server dependencies

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ServerPropertiesTest.class.getSimpleName());

	String serverMessage = null;
	long completedTime = 0;

	/**
	 * Test server properties
	 * @throws P4JavaException 
	 */
	@Test
	public void testServerProperties() {

		String[] serverUris = {
				"p4java://eng-p4java-vm.das.perforce.com:20121?socketPoolSize=10&testKey1=testVal1",
				"p4javassl://eng-p4java-vm.das.perforce.com:30121?socketPoolSize=10&testKey1=testVal1",
				"p4jrpc://eng-p4java-vm.das.perforce.com:20121?socketPoolSize=10&testKey1=testVal1",
				"p4jrpcssl://eng-p4java-vm.das.perforce.com:30121?socketPoolSize=10&testKey1=testVal1",
				"p4jrpcnts://eng-p4java-vm.das.perforce.com:20121?socketPoolSize=10&testKey1=testVal1",
				"p4jrpcntsssl://eng-p4java-vm.das.perforce.com:30121?socketPoolSize=10&testKey1=testVal1" };

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
				server.registerCallback(new MockCommandCallback());
				setupServer(p4d.getRSHURL(), userName, password, true, props);
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
