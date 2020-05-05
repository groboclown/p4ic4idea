package com.perforce.p4java.tests.dev.unit.feature.server;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Simple very minimal tests for some of the IServerInfo fields.
 * Not a lot we can really test here in any case...
 * 
 * @testid IServerInfoTest01
 */

@TestId("IServerInfoTest01")
public class IServerInfoTest extends P4JavaRshTestCase {

    private IClient client = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", IServerInfoTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void beforeEach() throws Exception{
        setupServer(p4d.getRSHURL(), userName, password, true, props);
        client = getClient(server);
     }

	@Test
	public void testGetClientInfo() throws Exception {
		
		try {
			IServerInfo serverInfo = server.getServerInfo();		
	
			assertNotNull("Unexpected Null returned by serverInfo.getClientName()",
								serverInfo.getClientName());
			assertEquals("Client name mismatch",
			             getPlatformClientName(this.getDefaultTestClientName()),
						 serverInfo.getClientName());
			assertEquals("Client root mismatch", client.getRoot(), serverInfo.getClientRoot());
			assertFalse("unicode enabled on non-unicode-enabled server",
							serverInfo.isUnicodeEnabled());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());		
		}
	}
}
