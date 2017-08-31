/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests for extraneous (or otherwise) newlines on label descriptions
 * (on both summary and full labels).
 * 
 * @testid LabelNewlineTest01
 * @job job035249
 */

@Jobs({"job035249"})
@TestId("LabelNewlineTest01")
public class LabelNewlineTest extends P4JavaTestCase {
	
	public LabelNewlineTest() {
	}

	@Test
	public void testNewlines() throws Exception {
		
		IServer server = null;
		String labelName = testId + "LabelTest";
		String labelDescription = "Two-line description for " + testId + "\n"
									+ "second line ("
									+ new Date() + ")";
		try {
			server = getServer();
			assertNotNull(server);
			
			ILabel label = server.getLabel(labelName);
			
			if (label == null) {
				label = new Label(
						labelName,
						userName,
						null,
						null,
						labelDescription,
						null,
						false,
						new ViewMap<ILabelMapping>()
					);
				String resultStr = server.createLabel(label);
				assertNotNull(resultStr);
				assertTrue(resultStr.contains("saved"));
				label = server.getLabel(labelName);
				assertNotNull(label);
			}
			
			label.setDescription(labelDescription);
			label.update();
			List<ILabelSummary> summaryList = server.getLabels(null, 0, null, null);
			assertNotNull(summaryList);
			for (ILabelSummary summary : summaryList) {
				assertNotNull(summary);
				assertNotNull(summary.getName());
				if (summary.getName().equalsIgnoreCase(labelName)) {
					assertNotNull(summary.getDescription());
					assertEquals(labelDescription, summary.getDescription());
				}
			}
			
			ILabel retrievedLabel = server.getLabel(labelName);
			assertNotNull(retrievedLabel);
			assertNotNull(retrievedLabel.getDescription());
			assertEquals("Description mismatch",
									labelDescription, retrievedLabel.getDescription());
		} finally {
			if (server != null) {
				server.disconnect();
			}
		}
	}
}
