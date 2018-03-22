/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldPatternRule;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRangeRule;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the creation of different RpcPacketFieldRule objects.
 */
@Jobs({ "job037798" })
@TestId("Dev112_RpcPacketFieldRuleTest")
public class RpcPacketFieldRuleTest extends P4JavaTestCase {

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
	 * Test the creation of a RpcPacketFieldRangeRule instance
	 */
	@Test
	public void testRpcPacketFieldRangeRuleConstructor() {

		try {
			Map<String, Object> cmdMap = new HashMap<String, Object>();
			cmdMap.put(RpcPacketFieldRule.START_FIELD, "testStartField");
			cmdMap.put(RpcPacketFieldRule.STOP_FIELD, "testStopField");
			RpcPacketFieldRule fieldRule = RpcPacketFieldRule.getInstance(cmdMap);
		
			assertNotNull(fieldRule);
			assertTrue(fieldRule instanceof RpcPacketFieldRangeRule);

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
	
	/**
	 * Test the creation of a RpcPacketFieldPatternRule instance
	 */
	@Test
	public void testRpcPacketFieldPatternRuleConstructor() {

		try {
			Map<String, Object> cmdMap = new HashMap<String, Object>();
			cmdMap.put(RpcPacketFieldRule.FIELD_PATTERN, "TT*");
			RpcPacketFieldRule fieldRule = RpcPacketFieldRule.getInstance(cmdMap);
		
			assertNotNull(fieldRule);
			assertTrue(fieldRule instanceof RpcPacketFieldPatternRule);

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
