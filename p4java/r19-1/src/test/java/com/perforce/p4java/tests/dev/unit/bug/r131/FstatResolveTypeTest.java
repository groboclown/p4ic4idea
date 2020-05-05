package com.perforce.p4java.tests.dev.unit.bug.r131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertTrue;

/**
 * Test "p4 fstat -Or" output: "resolveType[x]" and "reresolvable" tags.
 */

@Jobs({"job059627"})
@TestId("Dev131_FstatResolveTypeTest")
public class FstatResolveTypeTest extends P4JavaRshTestCase {

  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", FstatResolveTypeTest.class.getSimpleName());

  public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
  private IClient client = null;
  private IChangelist changelist = null;


  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), userName, password, true, props);
    client = getClient(server);
    createTextFileOnServer(client, "112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_it.properties", "desc");
  }

  @After
  public void tearDown() {
    afterEach(server);
  }

  /**
   * Test "p4 fstat -Or" output: "resolveType[x]" and "reresolvable" tags.
   */
  @Test
  public void testFstatOutputPendingResolved() throws Exception {
    int randNum = getRandomInt();
    String dir = "branch" + randNum;

    // Source and target files for integrate with content changes
    String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_it.properties";
    String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/" + dir + "/MessagesBundle_it.properties";
    String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/" + dir + randNum + "/MessagesBundle_it.properties";

    String testText = "///// added test text 1 ///// - " + randNum + LINE_SEPARATOR;
    String testText2 = "///// added test text 2 ///// - " + randNum + LINE_SEPARATOR;

    // Error message indicating merges still pending
    String mergesPending = "Merges still pending -- use 'resolve' to merge files.";

    // Info message indicating submitted change
    String submittedChange = "Submitted as change";

    try {
      // Copy a file to be used as a target for integrate content changes
      changelist = getNewChangelist(
          server,
          client,
          "Dev112_ResolveIgnoreWhitespaceTest copy files changelist");
      assertThat(changelist, notNullValue());
      changelist = client.createChangelist(changelist);
      assertThat(changelist, notNullValue());
      List<IFileSpec> files = client.copyFiles(
          new FileSpec(sourceFile),
          new FileSpec(targetFile), null,
          new CopyFilesOptions().setChangelistId(changelist.getId()));
      assertThat(files, notNullValue());
      changelist.refresh();
      files = changelist.submit(new SubmitOptions());
      assertThat(files, notNullValue());

      // Integrate targetFile to targetFile2
      changelist = getNewChangelist(
          server,
          client,
          "Dev112_ResolveIgnoreWhitespaceTest integrate files changelist");
      assertThat(changelist, notNullValue());
      changelist = client.createChangelist(changelist);
      assertThat(changelist, notNullValue());
      List<IFileSpec> integrateFiles = client.integrateFiles(
          new FileSpec(targetFile),
          new FileSpec(targetFile2), null,
          new IntegrateFilesOptions().setChangelistId(changelist.getId()));
      assertThat(integrateFiles, notNullValue());
      changelist.refresh();
      integrateFiles = changelist.submit(new SubmitOptions());
      assertThat(integrateFiles, notNullValue());

      // Edit targetFile (change type to binary)
      changelist = getNewChangelist(
          server,
          client,
          "Dev112_ResolveIgnoreWhitespaceTest edit files changelist");
      assertThat(changelist, notNullValue());
      changelist = client.createChangelist(changelist);
      assertThat(changelist, notNullValue());
      files = client.editFiles(
          FileSpecBuilder.makeFileSpecList(targetFile),
          new EditFilesOptions()
              .setChangelistId(changelist.getId())
              .setFileType("binary"));
      assertThat(files, notNullValue());

      // Append some text (tab between the brackets) to targetFile
      assertThat(files.get(0).getClientPathString(), notNullValue());
      writeFileBytes(files.get(0).getClientPathString(), testText, true);

      // Submit the changes
      changelist.refresh();
      List<IFileSpec> submittedFiles = changelist.submit(new SubmitOptions());
      assertThat(submittedFiles, notNullValue());

      // Edit targetFile2
      changelist = getNewChangelist(
          server,
          client,
          "Dev112_ResolveIgnoreWhitespaceTest edit files changelist");
      assertThat(changelist, notNullValue());
      changelist = client.createChangelist(changelist);
      assertThat(changelist, notNullValue());
      files = client.editFiles(
          FileSpecBuilder.makeFileSpecList(targetFile2),
          new EditFilesOptions().setChangelistId(changelist.getId()));
      assertThat(files, notNullValue());

      // Append some text (spaces between the brackets) to targetFile2
      assertThat(files.get(0).getClientPathString(), notNullValue());
      writeFileBytes(files.get(0).getClientPathString(), testText2, true);

      // Submit the changes
      changelist.refresh();
      submittedFiles = changelist.submit(new SubmitOptions());
      assertThat(submittedFiles, notNullValue());

      changelist = getNewChangelist(
          server,
          client,
          "Dev112_ResolveIgnoreWhitespaceTest integrate files changelist");
      assertThat(changelist, notNullValue());
      changelist = client.createChangelist(changelist);
      assertThat(changelist, notNullValue());

      // Run integrate
      integrateFiles = client.integrateFiles(
          new FileSpec(targetFile),
          new FileSpec(targetFile2), null,
          new IntegrateFilesOptions().setChangelistId(changelist.getId()));

      // Check for null
      assertThat(integrateFiles, notNullValue());

      // Check for invalid filespecs
      List<IFileSpec> invalidFiles = FileSpecBuilder.getInvalidFileSpecs(integrateFiles);
      if (invalidFiles.size() != 0) {
        Assert.fail(invalidFiles.get(0).getOpStatus() + ": "
            + invalidFiles.get(0).getStatusMessage());
      }

      // Refresh changelist
      changelist.refresh();

      // Check for correct number of valid filespecs in changelist
      List<IFileSpec> changelistFiles = changelist.getFiles(true);
      assertThat("Wrong number of filespecs in changelist",
          FileSpecBuilder.getValidFileSpecs(changelistFiles).size(),
          is(1));

      // Validate file action type
      assertThat(changelistFiles.get(0).getAction(), is(FileAction.INTEGRATE));

      // Run resolve with 'resolve file content changes'
      List<IFileSpec> resolveFiles = client.resolveFilesAuto(
          integrateFiles,
          new ResolveFilesAutoOptions().setChangelistId(changelist.getId()));

      // Check for null
      assertThat(resolveFiles, notNullValue());

      // Check for correct number of filespecs
      assertThat(resolveFiles.size(), is(5));

      // Check file operation status info message
      assertThat(resolveFiles.get(1).getOpStatus(), is(FileSpecOpStatus.VALID));
      assertTrue(resolveFiles.get(1).getStatusMessage() == null
              || resolveFiles.get(1).getStatusMessage().isEmpty());

      // Refresh changelist
      changelist.refresh();

      // Submit should fail, since the file with branching is not resolved
      List<IFileSpec> submitFiles = changelist.submit(new SubmitOptions());

      // Check for null
      assertThat(submitFiles, notNullValue());

      // Check for correct number of filespecs
      assertThat(submitFiles.size(), is(3));

      // Check for 'must resolve' and 'Merges still pending' in info and
      // error messages
      assertThat(submitFiles.get(0).getOpStatus(), is(FileSpecOpStatus.INFO));
      assertThat(submitFiles.get(0).getStatusMessage()
          , containsString(" - must resolve " + targetFile));
      assertThat(submitFiles.get(2).getOpStatus(), is(FileSpecOpStatus.ERROR));
      assertThat(submitFiles.get(2).getStatusMessage()
          , containsString(mergesPending));

      // Resolving the file with the forceTextualMerge option
      resolveFiles = client.resolveFilesAuto(
          files,
          new ResolveFilesAutoOptions()
              .setChangelistId(changelist.getId())
              .setAcceptTheirs(true).setForceTextualMerge(true).setForceResolve(true));
      assertThat(resolveFiles, notNullValue());

      // Use fstat -Or to verify the file action and file type on the pending resolved file
      List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(
          FileSpecBuilder.makeFileSpecList(targetFile2),
          new GetExtendedFilesOptions().setAncilliaryOptions(new FileStatAncilliaryOptions(false,
              false, false, true, false)));
      assertThat(extendedFiles, notNullValue());
      assertThat(extendedFiles.get(0).isResolved(), is(true));
      assertThat(extendedFiles.get(0).isReresolvable(), is(true));
      assertThat(extendedFiles.get(0).getResolveRecords().size() > 0, is(true));

      changelist.refresh();
      submitFiles = changelist.submit(new SubmitOptions());
      assertThat(submitFiles, notNullValue());

      // There should be 5 filespecs (triggers set up on machine)
      assertThat(submitFiles.size(), is(2));

      // Check the status and file action of the submitted file
      assertThat(submitFiles.get(0).getOpStatus(), is(FileSpecOpStatus.VALID));
      assertThat(submitFiles.get(0).getAction(), is(FileAction.INTEGRATE));

      // Check for 'Submitted as change' in the info message
      assertThat(submitFiles.get(1).getStatusMessage(), containsString(submittedChange + " " + changelist.getId()));

      // Make sure the changelist is submitted
      changelist.refresh();
      assertThat(changelist.getStatus(), is(ChangelistStatus.SUBMITTED));


    } finally {
      if (client != null && changelist != null) {
        if (changelist.getStatus() == ChangelistStatus.PENDING) {
          try {
            // Revert files in pending changelist
            client.revertFiles(
                changelist.getFiles(true),
                new RevertFilesOptions()
                    .setChangelistId(changelist.getId()));
          } catch (P4JavaException e) {
            // Can't do much here...
          }
        }
      }
      if (client != null && server != null) {
        try {
          // Delete submitted test files
          IChangelist deleteChangelist = getNewChangelist(server,
              client,
              "Dev112_ResolveIgnoreWhitespaceTest delete submitted test files changelist");
          deleteChangelist = client
              .createChangelist(deleteChangelist);
          client.deleteFiles(FileSpecBuilder
              .makeFileSpecList(new String[]{targetFile,
                  targetFile2}), new DeleteFilesOptions()
              .setChangelistId(deleteChangelist.getId()));
          deleteChangelist.refresh();
          deleteChangelist.submit(null);
        } catch (P4JavaException e) {
          // Can't do much here...
        }
      }
    }
  }
}
