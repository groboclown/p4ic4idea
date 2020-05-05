package com.perforce.p4java.tests.dev.unit.feature.server;

import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple tests for the IServer getDepotList method.
 * Uses play:1999 due to the interesting set of depots
 * on that system. Might get it to use p4prod at some
 * point too...<p>
 * 
 * Test is most useful to test for new and weird depot types.
 * 
 * @testid Server_SimpleDepotListTest01
 */

@TestId("Server_SimpleDepotListTest01")
public class SimpleDepotListTest extends P4JavaRshTestCase {
	
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SimpleDepotListTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void beforeEach() throws Exception{
        setupServer(p4d.getRSHURL(), userName, password, true, props);
     }
    
    @Test
	public void testGetDepotListBasics() {
		try {
			List<IDepot> depotList = server.getDepots();
			assertNotNull("Null depot list returned", depotList);
			assertTrue("Depot list is empty", depotList.size() > 0);
			
			for (IDepot depot : depotList) {
				// Note: it's legal to have a null owner name...
				// (but is it OK to have a null description?)
				
				assertNotNull("null depot in depot list", depot);
				assertNotNull("null depot type", depot.getDepotType());
				assertNotNull("null depot name", depot.getName());
				assertNotNull("null depot date", depot.getModDate());
				assertNotNull("null depot description", depot.getDescription()); 
				assertNotNull("null depot map", depot.getMap());
				switch (depot.getDepotType()) {
					case REMOTE:
						assertNotNull("null address for remote depot", depot.getAddress());
						break;
					
					case SPEC:
						assertNotNull("null suffix for spec depot", depot.getSuffix());
						break;
						
					default:
						break;
				}
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				endServerSession(server);
			}
		}
	}
}
