/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Basic tests for the 10.2 IOptionsServer.createDepot method; includes deleteDepot
 * test as a bonus. Only test local depot creation and deletion...
 */
@TestId("Features102_CreateDepotTest")
public class CreateDepotTest extends P4JavaTestCase {

	public CreateDepotTest() {
	}

	@Test
	public void testCreateDeleteDepotBasics() {
		final String depotName = this.getRandomName(false, "depottest");
		final String depotMap = depotName + "/...";
		final String depotDescription = "temp depot for test " + this.testId;
		final String expectedCreationResultString = "Depot " + depotName + " saved.";
		final String expectedDeletionResultString = "Depot " + depotName + " deleted.";
		
		IOptionsServer server = null;
		IDepot depot = null;

		try {
			server = this.getServerAsSuper();
			assertNotNull(server);
			depot = new Depot(
							depotName,
							server.getUserName(),
							null,
							depotDescription,
							DepotType.LOCAL,
							null,
							null,
							depotMap
						);
			String resultStr = server.createDepot(depot);
			assertNotNull("null result string from createDepot()", resultStr);
			assertEquals(expectedCreationResultString, resultStr);
			
			IDepot newDepot = server.getDepot(depotName);
			assertNotNull("null depot returned from getDepot method", newDepot);
			assertEquals("depot address mismatch", depot.getAddress(), newDepot.getAddress());
			assertEquals("depot name mismatch", depot.getName(), newDepot.getName());
			assertEquals("depot type mismatch", depot.getDepotType(), newDepot.getDepotType());
			assertEquals("depot description mismatch", depot.getDescription(), newDepot.getDescription());
			assertEquals("depot map mismatch", depot.getMap(), newDepot.getMap());
			assertEquals("depot owner mismatch", depot.getOwnerName(), newDepot.getOwnerName());
			assertEquals("depot suffix mismatch", depot.getSuffix(), newDepot.getSuffix());
			
			resultStr = server.deleteDepot(depotName);
			assertNotNull("null result string returned by deleteDepot", resultStr);
			assertEquals(expectedDeletionResultString, resultStr);
			
			newDepot = server.getDepot(depotName); // Note: no way to tell if the depot really exists or not... -- HR.
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
