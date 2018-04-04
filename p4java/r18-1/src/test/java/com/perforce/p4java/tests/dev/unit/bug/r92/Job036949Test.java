/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r92;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Note: test file in depot must have no line ending on the last line, and
 * the last line (and only the last line) must end with '(no newline)' (without
 * the quotes).
 *
 * @testid Job036949Test
 * @job job036949
 */

@TestId("Job036949Test")
@Jobs({"job036949"})
public class Job036949Test extends P4JavaTestCase {

  @Test
  public void testAnnotations() throws Exception {
    final String testFileDepotPath = "//depot/92bugs/Job036949Test/annotatetest.txt";
    final String lastLineEnd = "(no newline)";
    IServer server = null;
    IClient client = null;
    server = getServer();
    assertNotNull(server);
    client = getDefaultClient(server);
    assertNotNull(client);

    List<IFileAnnotation> annotationList = server.getFileAnnotations(
        FileSpecBuilder.makeFileSpecList(
            testFileDepotPath),
        null, false, false, false);
    assertNotNull(annotationList);
    boolean found = false;
    for (IFileAnnotation annotation : annotationList) {
      assertNotNull(annotation);
      String line = annotation.getLine(true);
      assertNotNull(line);
      if (line.contains(lastLineEnd)) {
        assertTrue(line.endsWith(lastLineEnd));
        found = true;
      }
    }
    assertTrue("Test-specific line not found", found);
  }
}
