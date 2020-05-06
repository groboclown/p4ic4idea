/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import com.perforce.p4java.impl.mapbased.server.ServerAddressBuilder;
import com.perforce.p4java.server.IServerAddress;
import com.perforce.p4java.server.IServerAddress.Protocol;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test server address syntax
 */
@Jobs({ "job051534" })
@TestId("Dev121_ServerAddressTest")
public class ServerAddressTest extends P4JavaRshTestCase {

	/**
	 * Test server address syntax
	 */
	@Test
	public void testServerAddressSyntax() {

		ServerAddressBuilder addressBuilder = null;
		IServerAddress serverAddress = null;
		
		// address format looks good
		try {
		    addressBuilder = new ServerAddressBuilder(
                    "p4javassl://fakeserver.perforce.com:30121?socketPoolSize=10&testKey1=testVal1");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JAVASSL, serverAddress.getProtocol());
			assertTrue(serverAddress.isSecure());
			assertEquals(30121, serverAddress.getPort());
			assertNotNull(serverAddress.getHost());
			assertEquals("fakeserver.perforce.com", serverAddress.getHost());
			assertNotNull(serverAddress.getProperties());
			assertEquals(2, serverAddress.getProperties().size());
			assertEquals("10", serverAddress.getProperties().getProperty("socketPoolSize"));
			assertEquals("testVal1", serverAddress.getProperties().getProperty("testKey1"));
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}

		// supported protocols
		try {
			// p4java
			addressBuilder = new ServerAddressBuilder("p4java://fakeserver.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JAVA, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());
			
			// p4jrpc
			addressBuilder = new ServerAddressBuilder("p4jrpc://fakeserver.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPC, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());
			
			// p4jrpcssl
			addressBuilder = new ServerAddressBuilder("p4jrpcssl://fakeserver.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPCSSL, serverAddress.getProtocol());
			assertTrue(serverAddress.isSecure());
			
			// p4jrpcnts
			addressBuilder = new ServerAddressBuilder("p4jrpcnts://fakeserver.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPCNTS, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());

			// p4jrpcntsssl
			addressBuilder = new ServerAddressBuilder("p4jrpcntsssl://fakeserver.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPCNTSSSL, serverAddress.getProtocol());
			assertTrue(serverAddress.isSecure());

		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
		
		// unknown protocol
		try {
			addressBuilder = new ServerAddressBuilder(
					"blahblah://fakeserver.perforce.com:30121");
		} catch (URISyntaxException e) {
			assertTrue(e.getMessage().contains("unknown protocol"));
		}
		
		// null host
		try {
			addressBuilder = new ServerAddressBuilder(
					"p4jrpc://:30121");
		} catch (URISyntaxException e) {
			assertTrue(e.getMessage().contains("missing or malformed Perforce server hostname"));
		}
		
		// no port
		try {
			addressBuilder = new ServerAddressBuilder(
					"p4jrpc://fakeserver.perforce.com");
		} catch (URISyntaxException e) {
			assertTrue(e.getMessage().contains("missing or malformed Perforce server port specifier"));
		}
	}
}
