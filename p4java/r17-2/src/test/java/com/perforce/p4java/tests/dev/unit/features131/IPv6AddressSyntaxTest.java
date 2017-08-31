/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
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
 * Test IPv6 server address syntax.
 */
@Jobs({ "job061060", "job061048" })
@TestId("Dev131_IPv6AddressSyntaxTest")
public class IPv6AddressSyntaxTest extends P4JavaTestCase {

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
	 * Test IPv6 server addresses using Java URI
	 */
	@Test
	public void testIPv6AddressUri() {
		String[] addresses = new String[] {
				"p4java://[FE80::0202:B3FF:FE1E:8329]:1666",
				"p4java://[1080:0:0:0:8:800:200C:417A]",
				"p4java://[3ffe:2a00:100:7031::1]:1667/foo?key1=value1",
				"p4java://[1080::8:800:200C:417A]:1666/foo?key1=value1",
				"p4java://[::192.9.5.5]/foo",
				"p4java://192.168.1.2/foo",
				"p4java://[2010:836B:4179::836B:4179]/foo",
				"p4java://[fe80::24c8:1d1c:3f57:a675%13]:1777",
				"p4java://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80",
				"[FE80::0202:B3FF:FE1E:8329]:1666",
				"p4java://FE80::0202:B3FF:FE1E:8329:1666"};

		for (String address : addresses) {
			String error = null;
			System.out.println("--------------------------");
			try {
				System.out.println("Original URI: " + address);
				URI uri = URI.create(address);
				System.out.println("URI.toString(): " + uri.toString());
				System.out.println("uri.getScheme(): " + uri.getScheme());
				System.out.println("uri.getHost(): " + uri.getHost());
				System.out.println("uri.getPort(): " + uri.getPort());
				System.out.println("uri.getPath(): " + uri.getPath());
				System.out.println("uri.getQuery(): " + uri.getQuery());
			} catch (IllegalArgumentException e) {
				//e.printStackTrace();
				error = e.getMessage();
			}
			if (error != null) {
				System.out.println("IllegalArgumentException: " + error);
			}
			System.out.println("--------------------------");
		}
	}

	/**
	 * Test IPv6 server address syntax
	 */
	@Test
	public void testIPv6ServerSyntax() {

		ServerAddressBuilder addressBuilder = null;
		IServerAddress serverAddress = null;
		
		// address format looks good
		try {
			addressBuilder = new ServerAddressBuilder(
					"p4javassl://[fc01:5034:a05:1e:250:56ff:fe84:3c6]:1701?socketPoolSize=10&testKey1=testVal1");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JAVASSL, serverAddress.getProtocol());
			assertTrue(serverAddress.isSecure());
			assertEquals(1701, serverAddress.getPort());
			assertNotNull(serverAddress.getHost());
			assertEquals("[fc01:5034:a05:1e:250:56ff:fe84:3c6]", serverAddress.getHost());
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
			addressBuilder = new ServerAddressBuilder("p4java://[fc01:5034:a05:1e:250:56ff:fe84:3c6]:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JAVA, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());
			
			// p4jrpc
			addressBuilder = new ServerAddressBuilder("p4jrpc://[fc01:5034:a05:1e:250:56ff:fe84:3c6]:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPC, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());
			
			// p4jrpcssl
			addressBuilder = new ServerAddressBuilder("p4jrpcssl://[fc01:5034:a05:1e:250:56ff:fe84:3c6]:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPCSSL, serverAddress.getProtocol());
			assertTrue(serverAddress.isSecure());
			
			// p4jrpcnts
			addressBuilder = new ServerAddressBuilder("p4jrpcnts://[fc01:5034:a05:1e:250:56ff:fe84:3c6]:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPCNTS, serverAddress.getProtocol());
			assertFalse(serverAddress.isSecure());

			// p4jrpcntsssl
			addressBuilder = new ServerAddressBuilder("p4jrpcntsssl://[fc01:5034:a05:1e:250:56ff:fe84:3c6]:30121");
			serverAddress = addressBuilder.build();
			assertNotNull(serverAddress);
			assertNotNull(serverAddress.getProtocol());
			assertEquals(Protocol.P4JRPCNTSSSL, serverAddress.getProtocol());
			assertTrue(serverAddress.isSecure());

		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
		
		// non-numeric port
		try {
			addressBuilder = new ServerAddressBuilder(
					"p4java://fc01:5034:a05:1e:250:56ff:fe84:3c6");
		} catch (URISyntaxException e) {
			assertTrue(e.getMessage().contains("non-numeric Perforce server port specifier"));
		}

		// unknown protocol
		try {
			addressBuilder = new ServerAddressBuilder(
					"blahblah://[fc01:5034:a05:1e:250:56ff:fe84:3c6]:30121");
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
