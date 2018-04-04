# List of known current bugs with the library

## com.perforce.p4java.tests.qa.CompressedRshConnectionTest

When using an RSH stream, the compressed stream tries reading beyond the
rsh stream, and it hangs forever on bytes that won't be returned.

This might be a problem with RSH + compressed streams. Need further
testing to verify.


# List of known current bugs with the tests

## Referencing Internal Perforce Test Servers

Many tests attempt to connect to Perforce test servers located within their
firewall.  The tests fail when a connection attempt is made.  Work has been
done to fail early when one of these tests run.

The fix is to instead use a TestServer instance to start up the correct server
configuration, so no pre-configured server is necessary.

## IllegalArgumentException thrown instead of NullPointerException

If you run the tests from Idea, then its compiler instruments the classes marked
with `@Nonnull` to throw an IAE when a null argument is passed in.  Some of the
unit tests check that an NPE (what should be the correct exception) is thrown, and
they fail because of the instrumentation.

This is only an issue when running tests inside the IDE.  The gradle scripts do not
run the Idea instrumentation, and do not fail for these tests.

## Symbolic link issues with Windows

* com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelperTest
* com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelperTest
* com.perforce.p4java.tests.dev.unit.bug.r132.CreateSymbolicLinkTest
* com.perforce.p4java.tests.dev.unit.features123.SymbolicLinkHelperTest

If you're running on windows, it will usually fail with "A required privilege is not held by the client."
You need the permission to create symbolic links.  A full detail on the solution is outlined
[in this discussion](https://superuser.com/questions/104845/permission-to-make-symbolic-links-in-windows-7).
I've noticed on Windows 10, even that doesn't always work.

## fail() when an exception happens

This is an anti-pattern that happens frequently in the tests.  Instead, it should use
`throw new AssertionError(message, exc)`.  This should be encased in a helper function.

Normally, the exceptions should just be thrown.  If the code isn't directly in a test, though,
then we can't make certain that the outer caller isn't trapping and hiding those failures.
Thus, we need an assertion error.

## com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5DigesterTest

Two methods here fail on non-Windows OS (or any system where the line.separator
property is not `\n`).  It appears that the underlying logic will check if the
native line ending is the same as the server line ending, and if so, it won't
convert line endings.

Need to investigate if this code works correctly when the p4d server is on
Windows, and the client is on Linux.

## com.perforce.p4java.impl.mapbased.server.cmd.InterchangesDelegatorTest

???

## com.perforce.p4java.option.OptionsTest

???

## The validated character sequence is blank

???

* com.perforce.p4java.tests.qa.Authentication102Test
* com.perforce.p4java.tests.qa.AuthenticationTest
* com.perforce.p4java.tests.qa.GetLoginStatusTest
* com.perforce.p4java.tests.qa.GetUsersTest

## Could not delete a db file from the temp p4d server dir

* com.perforce.p4java.tests.qa.CompressedRshConnectionTest
* com.perforce.p4java.tests.qa.CreateUserTest
* com.perforce.p4java.tests.qa.DeleteDepotTest
* com.perforce.p4java.tests.qa.GetLabelsTest
* com.perforce.p4java.tests.qa.GetRevisionHistoryTest
* com.perforce.p4java.tests.qa.IntegrateFilesTest
* com.perforce.p4java.tests.qa.ObliterateTest
* com.perforce.p4java.tests.qa.OpenedFilesOptionsTest
* com.perforce.p4java.tests.qa.ReopenFilesOptionsTest
* com.perforce.p4java.tests.qa.ReopenFilesTest
* com.perforce.p4java.tests.qa.RevertFilesOptionsTest
* com.perforce.p4java.tests.qa.ShelveFilesOptionsTest

Looks like a timing issue.  Could not delete a db file from the temp server.
Maybe a server was left running?

## com.perforce.p4java.tests.qa.CopyFilesTest

Looks like a formatting problem + unexpected error message.

## com.perforce.p4java.tests.qa.CoreFactoryTest

Looks like unexpected error message.

## com.perforce.p4java.tests.qa.CreateDepotTest

Looks like incorrect null message returned.

## com.perforce.p4java.tests.qa.CreateUserGroupTest

???

## com.perforce.p4java.tests.qa.EditFilesTest

???

## com.perforce.p4java.tests.qa.ExecMapCmdTest

Looks like incorrectly returning an empty string instead of null.

## com.perforce.p4java.tests.qa.ExecStreamingMapCommandTest

null vs. empty list.

## com.perforce.p4java.tests.qa.FileSpecTest

null vs. real string

## com.perforce.p4java.tests.qa.GetChangelistDiffsStreamTest

Extra new line expected?

## com.perforce.p4java.tests.qa.GetChangelistsTest

Same null vs. nouser issue

## com.perforce.p4java.tests.qa.GetDiffFilesOptionsTest

NPE in test

## com.perforce.p4java.tests.qa.GetDirectoriesTest

NPE in test

## com.perforce.p4java.tests.qa.GetExportRecordTest

null vs. text

## com.perforce.p4java.tests.qa.GetFileAnnotationsTest

NPE in test

## com.perforce.p4java.impl.mapbased.server.cmd.Diff2DelegatorTest

NPE in test

## com.perforce.p4java.impl.mapbased.server.cmd.DirsDelegatorTest

???

## com.perforce.p4java.impl.mapbased.server.cmd.IntegratedDelegatorTest

???

## com.perforce.p4java.impl.mapbased.server.cmd.KeysDelegatorTest

???

## com.perforce.p4java.impl.mapbased.server.cmd.MoveDelegatorTest

null vs. text

## com.perforce.p4java.impl.mapbased.server.cmd.ObliterateDelegatorTest

null vs. text

## com.perforce.p4java.impl.mapbased.server.cmd.PropertyDelegatorTest

??? - bad test, needs something better than "true/false"

## com.perforce.p4java.impl.mapbased.server.cmd.TagDelegatorTest

null vs. text

## com.perforce.p4java.impl.mapbased.server.cmd.VerifyDelegatorTest

NPE in test

## com.perforce.p4java.option.client.OptionsDefaultConstructorsTest

Incompatible version of com.google.common.collect and org.reflections.Reflections.

## com.perforce.p4java.tests.dev.unit.bug.r152.RshTest

Pipe closed error.

## Could not create file/directory

* com.perforce.p4java.tests.dev.unit.bug.r132.GetDiffFilesWindowsLineEndingsTest
* com.perforce.p4java.tests.dev.unit.bug.r132.InferFileTypeTest
* com.perforce.p4java.tests.dev.unit.bug.r132.RequestTicketForHostTest
* com.perforce.p4java.tests.dev.unit.bug.r132.StreamCmdDiff2UnicodeTest
* com.perforce.p4java.tests.dev.unit.bug.r132.SymbolicLink2DirectoryTest
* com.perforce.p4java.tests.dev.unit.bug.r132.SymbolicLinksWithConnectionPoolTest
* com.perforce.p4java.tests.dev.unit.bug.r132.SymbolicLinkWithNonExistingTargetTest
* com.perforce.p4java.tests.dev.unit.bug.r132.SyncBinaryFilesTest
* com.perforce.p4java.tests.dev.unit.bug.r132.SyncTextRevWithNoTargetSymlinkHeadTest
* com.perforce.p4java.tests.dev.unit.bug.r141.CreateClientWithWhitespaceTest
* com.perforce.p4java.tests.dev.unit.bug.r141.GetFileContentNonExistingFileTest
* com.perforce.p4java.tests.dev.unit.bug.r141.GetJobRawFieldsTest
* com.perforce.p4java.tests.dev.unit.bug.r141.LabelLockedAutoReloadTest
* com.perforce.p4java.tests.dev.unit.bug.r141.ProtectionsQuotedExcludePathTest
* com.perforce.p4java.tests.dev.unit.bug.r141.ReconcileFilesOutputLocalFileSyntaxTest
* com.perforce.p4java.tests.dev.unit.bug.r141.ReconcileFilesWhitespacePathTest
* com.perforce.p4java.tests.dev.unit.bug.r141.SyncDeletedFilesTest
* com.perforce.p4java.tests.dev.unit.bug.r151.SyncFilesWithExecBitTest
* com.perforce.p4java.tests.dev.unit.bug.r152.IntegrateNoServerClientTest
* com.perforce.p4java.tests.dev.unit.bug.r152.ProtectionsCreateUpdateOrderingTest
* com.perforce.p4java.tests.dev.unit.bug.r152.ServerConnectionMimCheckTest
* com.perforce.p4java.tests.dev.unit.bug.r152.ShelveUnshelveUtf6leWindowsTest
* com.perforce.p4java.tests.dev.unit.bug.r152.SyncUnicodeXFilesTest
* com.perforce.p4java.tests.dev.unit.bug.r152.SyncUtf16beFilesWinLocalTest
* com.perforce.p4java.tests.dev.unit.bug.r152.SyncUtf16leFilesTest
* com.perforce.p4java.tests.dev.unit.bug.r152.UnshelveUtf6leWindowsTest
* com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUnicodeFileTypeOnNonUnicodeEnabledServerTest
* com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUnicodeFileTypeOnUnicodeEnabledServerTest
* com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUtf16BETest
* com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUtf16FileTypeTest
* com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUtf8FileTypeTest
* com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUtf8FileTypeUnderServer20161Test
* com.perforce.p4java.tests.dev.unit.bug.r161.SyncExecutableFileTest
* com.perforce.p4java.tests.dev.unit.features111.IntegrationsAnnotationsTest
* com.perforce.p4java.tests.dev.unit.features171.GraphCatFileTest
* com.perforce.p4java.tests.dev.unit.features171.GraphClientDepotTypeTest
* com.perforce.p4java.tests.dev.unit.features171.GraphCommitLogTest
* com.perforce.p4java.tests.dev.unit.features171.GraphDepotsTest
* com.perforce.p4java.tests.dev.unit.features171.GraphDescribeCommitTest
* com.perforce.p4java.tests.dev.unit.features171.GraphFilesTest
* com.perforce.p4java.tests.dev.unit.features171.GraphHaveTest
* com.perforce.p4java.tests.dev.unit.features171.GraphLsTree
* com.perforce.p4java.tests.dev.unit.features171.GraphReceivePackTest
* com.perforce.p4java.tests.dev.unit.features171.GraphRevListTest
* com.perforce.p4java.tests.dev.unit.features171.ParallelCallbackTest
* com.perforce.p4java.tests.dev.unit.features172.FilesysUTF8bomTest
* com.perforce.p4java.tests.dev.unit.features172.IntegrationOutputTest
* com.perforce.p4java.tests.dev.unit.features172.ListCommandTest
* com.perforce.p4java.tests.dev.unit.features173.GraphShowRefTest
* com.perforce.p4java.tests.dev.unit.features173.UnicodeBufferTest


Failure to create a temp file.  Problem in `com.perforce.test.P4ExtFileUtils`.

## com.perforce.p4java.tests.dev.unit.dev101.options.OptionsSpecTest

null argument to test

## null server object passed to endServerSession

* com.perforce.p4java.tests.dev.unit.feature.client.SimpleEditRevertTest
* com.perforce.p4java.tests.dev.unit.helper.FactoryCreateClientTest


## com.perforce.p4java.tests.dev.unit.feature.error.AccessExceptionTest

AccessException rather than Exception

## com.perforce.p4java.tests.dev.unit.feature.error.RequestExceptionTest

expected null, found error

## com.perforce.p4java.tests.dev.unit.feature.error.ThrowableSuite

expected Exception, found AccessException

## com.perforce.p4java.tests.dev.unit.features123.InMemoryFingerprintsTest

assertion failure (bad test - don't use true/false)

## NtsServerImpl.disconnect NPE

* com.perforce.p4java.tests.dev.unit.features131.FilterCallbackTest
* com.perforce.p4java.tests.dev.unit.features131.QuietModeTest
* com.perforce.p4java.tests.dev.unit.features132.JournalWaitTest
* com.perforce.p4java.tests.dev.unit.features132.ClientCompressSyncTest

NPE when disconnecting Nts, but never connected.  Should be fixed now.

## NPE in validateFileSpecs test code.

* com.perforce.p4java.tests.qa.GetFileDiffsTest
* com.perforce.p4java.tests.qa.GetInterchangesTest
* com.perforce.p4java.tests.qa.IntegrateFilesOptionsTest
* com.perforce.p4java.tests.qa.ResolvedFilesOptionsTest
* com.perforce.p4java.tests.qa.ResolveFilesAutoOptionsTest

## com.perforce.p4java.tests.qa.GetLogtailTest

log text missing

## com.perforce.p4java.tests.qa.GetProtectionEntriesTest

RequestException: Can't delete last valid 'super' entry from protections table.

## com.perforce.p4java.tests.qa.GetStreamsTest

NPE when matching strings

## com.perforce.p4java.tests.qa.ResolveFileTest

resolvedFile file spec is returned null.

## getLog but file exists

* com.perforce.p4java.tests.qa.ServerFactoryTest
* com.perforce.p4java.tests.qa.SocketPoolTest

Fails due to the new file already exists.  Looks like it was using the wrong API here.
It should be fixed now.

However, there are issues with the server actually producing stuff to the log...

