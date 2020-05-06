/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test P4Java supports 'large errors': p4 cmd --explain
 */
@TestId("Dev141_LargeErrorsTest")
public class LargeErrorsTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", LargeErrorsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, null);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test Perforce charsets cp1253, cp737, and iso8859-7.
	 */
	@Test
	public void testLargeErrors() {
		
		try {
			Map<String, Object>[] res = server.execMapCmd("integ", new String[] {"--explain"}, null);
			assertTrue( "Should have only 1 responce block", res.length == 1 );
			
			int msgs = 0;
			if( res.length > 0 ) {
			    for(String key : res[0].keySet()) {
			        try {
    			        if( key.startsWith("fmt") ) {
    			            msgs = Integer.parseInt(key.substring(3)) > msgs ?
					Integer.parseInt(key.substring(3)) : msgs;
    			        }
			        } catch ( NumberFormatException e ) {}
			    }
			}
			
			StringBuffer buf = new StringBuffer();
			assertTrue( "Integ has more than 8 options", msgs > 8 );
			for( int m = msgs; m >= 0; m-- ) {
			    assertTrue( res[0].containsKey("fmt" + m));
			    buf.append(RpcMessage.interpolateArgs((String)res[0].get("fmt" + m), res[0])).append("\n");
			}

			if (!buf.toString().contains(
				"--keep-have (-h): specifies to leave target files at the currently synced revision.\n" +
				"--output-flags (-O): specifies codes controlling the command output.\n" +
				"--parent (-P): specifies the stream to be used as the parent.\n" +
				"--stream (-S): specifies the stream to be used.\n" +
				"--integ-flags (-R): specifies how integrate should schedule resolves.\n" +
				"--delete-flags (-D): specifies how integration should handle deleted files.\n" +
				"--force (-f): overrides the normal safety checks.\n" +
				"--source-file (-s): treat fromFile as the source and both sides of the branch view as the target.\n" +
				"--quiet (-q): suppresses normal information output.\n" +
				"--virtual (-v): performs the action on the server without updating client data.\n" +
				"--max (-m): specifies the maximum number of objects.\n" +
				"--preview (-n): specifies that the command should display a preview of the results, but not execute them.\n" +
				"--change (-c): specifies the changelist to use for the command.\n" +
				"--reverse (-r): specifies to reverse the direction of the branch view.\n" +
				"--branch (-b): specifies the branch name to use.\n" +
				"Usage: integrate [ -c changelist# -D<flags> -f -h -m max -n -Obr -q -r -s from ] [ -b branch to... | from to ] [ -S stream [ -P parent ] files... ]"
			)) {
				Assert.fail("returned error message is not the correct error message");
			}


			// Whilst we're here, check we havn't broken alt-text
			HashMap<String, Object> map = new HashMap<String, Object>();
			String msg = "[a] [%bee%|b] c";
			assertEquals(RpcMessage.interpolateArgs(msg, map), "[a] b c");
			map.put("bee", "d");
            assertEquals(RpcMessage.interpolateArgs(msg, map), "[a] d c");
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
