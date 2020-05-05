/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.server;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Simple tests for the IServer.getClientTemplate methods.
 */
@TestId("Server_GetClientTemplateTest")
public class GetClientTemplateTest extends P4JavaRshTestCase {

    	public GetClientTemplateTest() {
    	}
    	
    	IClient client = null;

	    @ClassRule
	    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetClientTemplateTest.class.getSimpleName());

	    /**
	     * @Before annotation to a method to be run before each test in a class.
	     */
	    @Before
	    public void beforeEach() throws Exception{
	        setupServer(p4d.getRSHURL(), userName, password, true, props);
	        client = getClient(server);
	     }

	
	@Test
	public void testGetClientTemplates() {
		
		try {
			IClient clientTemplate = server.getClientTemplate(client.getName(), false);
			assertNull(clientTemplate);
			clientTemplate = server.getClientTemplate(client.getName(), true);
			assertNotNull(clientTemplate);
			clientTemplate = server.getClientTemplate(this.getRandomClientName("XyZ"), false);
			assertNotNull(clientTemplate);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
