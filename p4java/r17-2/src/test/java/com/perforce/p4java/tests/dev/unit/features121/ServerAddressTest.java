/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.impl.mapbased.server.ServerAddressBuilder;
import com.perforce.p4java.server.IServerAddress;
import com.perforce.p4java.server.IServerAddress.Protocol;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test server address syntax
 */
@Jobs({ "job051534" })
@TestId("Dev121_ServerAddressTest")
public class ServerAddressTest extends P4JavaTestCase {

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
	 * Test server address syntax
	 */
	@Test
	public void testServerAddressSyntax() {

		ServerAddressBuilder addressBuilder = null;
		IServerAddress serverAddress = null;
		
		// address format looks good
		try {
			addressBuilder = new ServerAddressBuilder(
					"p4javassl://eng-p4java-vm.perforce.com:30121?socketPoolSize=10&testKey1=testVal1");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JAVASSL, serverAddress.getProtocol());
			assertTrue(serverAddress.isSecure());
			assertEquals(30121, serverAddress.getPort());
			assertNotNull(serverAddress.getHost());
			assertEquals("eng-p4java-vm.perforce.com", serverAddress.getHost());
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
			addressBuilder = new ServerAddressBuilder("p4java://eng-p4java-vm.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JAVA, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());
			
			// p4jrpc
			addressBuilder = new ServerAddressBuilder("p4jrpc://eng-p4java-vm.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPC, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());
			
			// p4jrpcssl
			addressBuilder = new ServerAddressBuilder("p4jrpcssl://eng-p4java-vm.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPCSSL, serverAddress.getProtocol());
			assertTrue(serverAddress.isSecure());
			
			// p4jrpcnts
			addressBuilder = new ServerAddressBuilder("p4jrpcnts://eng-p4java-vm.perforce.com:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPCNTS, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());

			// p4jrpcntsssl
			addressBuilder = new ServerAddressBuilder("p4jrpcntsssl://eng-p4java-vm.perforce.com:30121");
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
					"blahblah://eng-p4java-vm.perforce.com:30121");
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
					"p4jrpc://abcserver");
		} catch (URISyntaxException e) {
			assertTrue(e.getMessage().contains("missing or malformed Perforce server port specifier"));
		}
	}
}
