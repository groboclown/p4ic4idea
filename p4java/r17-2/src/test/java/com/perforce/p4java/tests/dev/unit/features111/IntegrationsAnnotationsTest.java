/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features111;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests annotate -I option. A bit impressionistic. May fail on
 * newly-(re)created servers; if so, play with some of the size constants,
 * etc., below and see what happens...
 */

@TestId("Features111_IntegrationsAnnotationsTest")
public class IntegrationsAnnotationsTest extends P4JavaRshTestCase {
    private static final int TIME_OUT_IN_SECONDS = 60;

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", IntegrationsAnnotationsTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
	}


	@Before
    public void beforeEach() throws Exception {
        Properties rpcTimeOutProperties = configRpcTimeOut("IntegrationsAnnotationsTest", TIME_OUT_IN_SECONDS);
    }

    @Test
    public void testIntegrationsAnnotations() throws Exception {
        final String testRoot = "//depot/102Dev/CopyFilesTest/tgt"; // shared, so be careful...
        final String testFile = testRoot + "/" + "...";
        final List<IFileSpec> testFiles = FileSpecBuilder.makeFileSpecList(testFile);

        IClient client = getDefaultClient(server);
        assertNotNull("null client returned", client);
        server.setCurrentClient(client);
        List<IFileSpec> syncFiles = this.forceSyncFiles(client, testRoot + "/...");
        assertNotNull("null sync files list", syncFiles);
        assertEquals("bad forced sync", 0, FileSpecBuilder.getInvalidFileSpecs(syncFiles).size());
        int annotationsSize = 0;
        List<IFileAnnotation> annotations = server.getFileAnnotations(testFiles, new GetFileAnnotationsOptions());
        assertNotNull("null annotations list returned", annotations);
        annotationsSize = annotations.size();
        assertTrue("annotations list too small", annotationsSize > 300); // rough size...
        for (IFileAnnotation annotation : annotations) {
            assertNotNull("null annotation in list", annotation);
            assertNull("non-null integrations list", annotation.getAllIntegrations());
        }

        annotations = server.getFileAnnotations(testFiles,
                new GetFileAnnotationsOptions().setFollowAllIntegrations(true));
        assertNotNull("null annotations list returned", annotations);
        assertEquals("annotation list size mismatch", annotationsSize, annotations.size()); // Should match
        boolean foundContributing = false;
        boolean foundSpecificContributer = false;
        for (IFileAnnotation annotation : annotations) {
            assertNotNull("null annotation in list", annotation);
            List<IFileAnnotation> contributingAnnotations = annotation.getAllIntegrations();
            if (contributingAnnotations != null) {
                foundContributing = true;
                int i = 0;
                for (IFileAnnotation contAnnotation : contributingAnnotations) {
                    assertNotNull("null contributing annotation", contAnnotation);
                    assertEquals("ordering mismatch", i, contAnnotation.getOrdering());
                    assertNotNull("null depot path in contributing annotation", contAnnotation.getDepotPath());
                    assertNull("non-null data line in contributing annotation", contAnnotation.getLine());
                    i++;
 
						/*
                         * Look for specific integration we know exists:
						 * ... depotFile1 //depot/102Dev/CopyFilesTest/tgt/test01.txt
						 * ... upper1 121
						 * ... lower1 108
						 */

                    if ("//depot/102Dev/CopyFilesTest/tgt/test01.txt".equals(
                            contAnnotation.getDepotPath())
                            && (contAnnotation.getOrdering() == 1)
                            && (contAnnotation.getLower() == 16)
                            && (contAnnotation.getUpper() == 18)) {
                        foundSpecificContributer = true;
                    }
                }
            }
        }
        assertTrue("found no contributing annotations", foundContributing);
        assertTrue("did not find specific contributing annotation", foundSpecificContributer);
    }

    @After
    public void afterEach() throws Exception {
        if (server != null) {
            this.endServerSession(server);
        }
    }
}
