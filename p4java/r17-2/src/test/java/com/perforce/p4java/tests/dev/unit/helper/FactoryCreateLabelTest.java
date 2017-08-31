/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Basic tests for the Factory createLabel method(s).
 */

@TestId("Commons_FactoryCreateLabelTest")
public class FactoryCreateLabelTest extends P4JavaTestCase {

	public FactoryCreateLabelTest() {
	}

	@Test
	public void testFactoryCreateLabelDefaults() {
		IOptionsServer server = null;
		ILabel label = null;
		final String name = this.getRandomName("label");
		
		try {
			server = getServer();
			label = CoreFactory.createLabel(server, name, null, null, true);
			assertNotNull("null label returned by factory method", label);
			ILabel retLabel = server.getLabel(name);
			assertNotNull("created label not found on server", retLabel);
			assertEquals("name mismatch", name, retLabel.getName());
			assertEquals("description mismatch", Label.DEFAULT_DESCRIPTION,
										retLabel.getDescription());
			assertEquals("owner mismatch", server.getUserName(),
										retLabel.getOwnerName());
			assertEquals("locked field mismatch", false, retLabel.isLocked());
			assertEquals("rev spec mismatch", null, retLabel.getRevisionSpec());
			ViewMap<ILabelMapping> viewMapping = retLabel.getViewMapping();
			assertNotNull("null view mapping in returned label", viewMapping);
			assertEquals("wrong mapping size", 1, viewMapping.getSize());
			assertNotNull("null mapping entry", viewMapping.getEntry(0));
			assertEquals("map mismatch", Label.DEFAULT_MAPPING, viewMapping.getEntry(0).getLeft());
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			try {
				server.deleteLabel(name, null);
			} catch (Exception exc) {
				
			}
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	@Test
	public void testFactoryCreateLabelBasics() {
		IOptionsServer server = null;
		ILabel label = null;
		final String name = this.getRandomName("label");
		final String description = this.getRandomName(" description ");
		final String[] mapping = {
			"//depot/dev/...",
			"//depot/jteam/tests/..."
		};
		
		try {
			server = getServer();
			label = CoreFactory.createLabel(server, name, description, mapping, true);
			assertNotNull("null label returned by factory method", label);
			ILabel retLabel = server.getLabel(name);
			assertNotNull("created label not found on server", retLabel);
			assertEquals("name mismatch", name, retLabel.getName());
			assertEquals("description mismatch", description,
										retLabel.getDescription());
			assertEquals("owner mismatch", server.getUserName(),
										retLabel.getOwnerName());
			assertEquals("locked field mismatch", false, retLabel.isLocked());
			assertEquals("rev spec mismatch", null, retLabel.getRevisionSpec());
			ViewMap<ILabelMapping> viewMapping = retLabel.getViewMapping();
			assertNotNull("null view mapping in returned label", viewMapping);
			assertEquals("wrong view map size", mapping.length, viewMapping.getSize());
			for (int i = 0; i < mapping.length; i++) {
				assertNotNull("null mapping entry", viewMapping.getEntry(i));
				assertEquals("map mismatch", mapping[i], viewMapping.getEntry(i).getLeft());
			}
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			try {
				server.deleteLabel(name, null);
			} catch (Exception exc) {
				
			}
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
