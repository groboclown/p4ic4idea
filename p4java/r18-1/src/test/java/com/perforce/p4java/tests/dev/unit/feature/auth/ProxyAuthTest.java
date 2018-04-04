/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.Test;

import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Attempts to test the IServer.getLoginStatus method.
 */

@TestId("FeaturesAuth_ProxyAuthTest")
public class ProxyAuthTest extends P4JavaTestCase {

	public ProxyAuthTest() {
		super();
	}

	/**
	 * Tests login and execution in proxy auth mode
	 */
	@Test
	public void testLoginStatusSuccess() {
		
		IOptionsServer server = null;
		StringBuffer ticket = new StringBuffer();
		String ipaddr = "10.9.3.83";
		
		try {
		    server = getServer();
            server.login(this.userName, ticket,
                         new LoginOptions().setHost(ipaddr)
                                           .setDontWriteTicket(true));
		    IServerInfo serverInfo = server.getServerInfo();
            this.endServerSession(server);
            assertTrue("no ticket returned",
                       ticket.toString().matches("[A-F0-9]{32}"));
            assertTrue("client address must not match the one we've set",
                    !serverInfo.getClientAddress().equals(ipaddr));
            
            server = getServer();
            server.setUserName(this.superUserName);
            server.login(this.superUserPassword);
            this.endServerSession(server);
            
            
		    Properties props = new Properties();
		    props.put( "svrname", this.superUserName );
            props.put( "port", "10.1.1.0:443" );
            props.put( "ipaddr", ipaddr );
			server = getOptionsServer(getServerUrlString(), props);
			server.setUserName(this.userName);
			server.setAuthTicket(ticket.toString());
			
			String statusString = server.getLoginStatus();
			String expectedValue = "User " + this.userName + " ticket expires in";
			assertNotNull("null login status string returned from IServer.getLoginStatus",
						statusString);
			assertTrue("returned status string '" + statusString + "' wrong for logged-in user;"
					+ " expected string containing: '"
					+ expectedValue + "'...",
					statusString.contains(expectedValue));
			
			serverInfo = server.getServerInfo();
			assertEquals("client address must match the one we've set",
			        serverInfo.getClientAddress(), ipaddr);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
