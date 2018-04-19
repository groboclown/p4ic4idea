# List of known current bugs with the library

## com.perforce.p4java.tests.qa.CompressedRshConnectionTest

When using an RSH stream, the compressed stream tries reading beyond the
rsh stream, and it hangs forever on bytes that won't be returned.

This might be a problem with RSH + compressed streams. Need further
testing to verify.


# List of known current bugs with the tests

## Known Problems That Are Worked Around

### Referencing Internal Perforce Test Servers

Many tests attempt to connect to Perforce test servers located within their
firewall.  The tests fail when a connection attempt is made.  Work has been
done to fail early when one of these tests run.

The fix is to instead use a TestServer instance to start up the correct server
configuration, so no pre-configured server is necessary.

A list of these tests is below.  They have been disabled until the server stuff
is set up.

### IllegalArgumentException thrown instead of NullPointerException

If you run the tests from Idea, then its compiler instruments the classes marked
with `@Nonnull` to throw an IAE when a null argument is passed in.  Some of the
unit tests check that an NPE (what should be the correct exception) is thrown, and
they fail because of the instrumentation.

This is only an issue when running tests inside the IDE.  The gradle scripts do not
run the Idea instrumentation, and do not fail for these tests.  To work around this, you need
to globally remove the
[runtime assertions for annotated methods and parameters](https://www.jetbrains.com/help/idea/compiler.html).

### Symbolic link issues with Windows

* com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelperTest
* com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelperTest
* com.perforce.p4java.tests.dev.unit.bug.r132.CreateSymbolicLinkTest
* com.perforce.p4java.tests.dev.unit.features123.SymbolicLinkHelperTest

If you're running on windows, it will usually fail with "A required privilege is not held by the client."
You need the permission to create symbolic links.  A full detail on the solution is outlined
[in this discussion](https://superuser.com/questions/104845/permission-to-make-symbolic-links-in-windows-7).
I've noticed on Windows 10, even that doesn't always work.

### Problems when running tests in Docker on shared Windows drive

Some tests rely upon testing the file mode (read/write/etc), and with a connected Windows drive
in a Docker image, the files do not correctly set the writable flag.  These tests end up failing.

* com.perforce.p4java.tests.qa.EditFilesOptionsTest

### fail() when an exception happens

This is an anti-pattern that happens frequently in the tests.  Instead, it should use
`throw new AssertionError(message, exc)`.  This should be encased in a helper function.

Normally, the exceptions should just be thrown.  If the code isn't directly in a test, though,
then we can't make certain that the outer caller isn't trapping and hiding those failures.
Thus, we need an assertion error.
n).

### Tests with unsupported API

These tests were written to try to run server commands that aren't implemented yet.

* com.perforce.p4java.tests.qa.GetDiffFilesOptionsTest
    * "No metadata defined for RPC function encoding: client-OpenDiff"
    * client-OpenDiff is not implemented in the API, so this test just won't ever run.  Failing tests disabled.

### com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5DigesterTest

Two methods here fail on non-Windows OS (or any system where the line.separator
property is not `\n`).  It appears that the underlying logic will check if the
native line ending is the same as the server line ending, and if so, it won't
convert line endings.

Need to investigate if this code works correctly when the p4d server is on
Windows, and the client is on Linux.

This is because isRequireConvertClientOrLocalLineEndingToServerFormat
sees that the native line ending ("\n") is the same as the server line ending
(which the ClientLineEnding assumes is \n).


## Observed Failures That Need Fixing

### com.perforce.p4java.tests.qa.CopyFilesTest

* usingSpecifiedParent() - Looks like the view isn't setup right to support the test.
* failure() - expects an ERROR, but is getting an INFO (actually, error code 2, which is a warning).  Probably
  due to another underlying setup issue.

### com.perforce.p4java.tests.qa.CoreFactoryTest

Looks like unexpected error message.  Could be that the error messages are now more specific.

### com.perforce.p4java.tests.qa.ExecStreamingMapCommandTest

null vs. empty list, while running the 'export' command.  The returned map does not have the expected value.
The issue shows regardless of RSH vs. port connection.

### com.perforce.p4java.tests.qa.GetDirectoriesTest

Wrong number of paths from getDirectories on the root directory.

### com.perforce.p4java.tests.qa.GetExportRecordTest

Expected a path, but returned null.  Another case of `HAdfile` issue, just like `ExecStreamingMapCommandTest`.

### com.perforce.p4java.impl.mapbased.server.cmd.DirsDelegatorTest

getDirectories returns 0 paths instead of 1.

### com.perforce.p4java.impl.mapbased.server.cmd.KeysDelegatorTest

RequestException for handleError on getKeys()

### com.perforce.p4java.impl.mapbased.server.cmd.MoveDelegatorTest

null vs. text

### com.perforce.p4java.tests.qa.GetExportRecordTest

null vs. text

### com.perforce.p4java.impl.mapbased.server.cmd.PropertyDelegatorTest

Null vs. empty string.

### com.perforce.p4java.impl.mapbased.server.cmd.TagDelegatorTest

The underlying implementation returns the standard INFO message without looking at the
`depotFile` field.  This is either an incorrect test, or the implementation isn't returning
what the test expects.

### com.perforce.p4java.tests.dev.unit.bug.r152.RshTest

Pipe closed error.

### Throwable tests

* com.perforce.p4java.tests.dev.unit.feature.error.ThrowableSuite
* com.perforce.p4java.tests.dev.unit.feature.error.AccessExceptionTest
* com.perforce.p4java.tests.dev.unit.feature.error.RequestExceptionTest

This is a bad test suite in general, now that the exceptions have become more explicit in terms of when they can be used.

### com.perforce.p4java.tests.qa.GetLogtailTest

log text incorrect.

### com.perforce.p4java.tests.qa.ResolveFileTest

resolvedFile file spec is returned null.

### Program name property not found in log file

* com.perforce.p4java.tests.qa.ServerFactoryTest
* com.perforce.p4java.tests.qa.SocketPoolTest

Looks like possibly a bad test.

### com.perforce.p4java.tests.qa.Authentication102Test

Session logged out; need to log in again.

### com.perforce.p4java.impl.mapbased.server.cmd.ClientDelegatorTest

NPE thrown instead of RequestException.  Need to understand why.

### com.perforce.p4java.impl.mapbased.server.cmd.JobDelegatorTest

Expected IAE.

### com.perforce.p4java.tests.dev.unit.bug.r101.Job038602Test

No such file on server.  It probably wasn't setup right.

### Could not find resource /data/server-20101/depot.tar.gz

* com.perforce.p4java.tests.dev.unit.bug.r101.Job040601Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040680Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040829Test

### com.perforce.p4java.tests.dev.unit.bug.r132.InferFileTypeTest

Could not find the Linux.jpg file.

### Could not create a unicode character directory

* com.perforce.p4java.tests.dev.unit.bug.r132.StreamCmdDiff2UnicodeTest
* com.perforce.p4java.tests.dev.unit.bug.r141.GetDiffFilesUnicodeBOMTest

Could be an issue only on Windows.

### References External p4d Host

`''(java)
@Disabled("Uses external p4d server")
@Ignore("Uses external p4d server")
`''

* com.perforce.p4java.tests.dev.unit.bug.r123.AutoloadUserAuthTicketTest
* com.perforce.p4java.tests.dev.unit.bug.r123.BrokerTest
* com.perforce.p4java.tests.dev.unit.bug.r123.CancelStreamingCallbackTest
* com.perforce.p4java.tests.dev.unit.bug.r123.ClientViewMapTest
* com.perforce.p4java.tests.dev.unit.bug.r123.GetFileDiffsTypesTest
* com.perforce.p4java.tests.dev.unit.bug.r123.NewDepotTest
* com.perforce.p4java.tests.dev.unit.bug.r123.PasswordSecretKeyTest
* com.perforce.p4java.tests.dev.unit.bug.r123.PasswordTest
* com.perforce.p4java.tests.dev.unit.bug.r123.ProtocolAppTagTest
* com.perforce.p4java.tests.dev.unit.bug.r123.ReplicaAuthTicketsTest
* com.perforce.p4java.tests.dev.unit.bug.r123.ReplicaAutoloadUserAuthTicketTest
* com.perforce.p4java.tests.dev.unit.bug.r123.StreamingSyncPerformanceTest
* com.perforce.p4java.tests.dev.unit.bug.r123.SyncJapaneseFilesCharsetTest
* com.perforce.p4java.tests.dev.unit.bug.r123.SyncPerformanceTest
* com.perforce.p4java.tests.dev.unit.bug.r123.UpdateLabelTest
* com.perforce.p4java.tests.dev.unit.bug.r131.AddFilesCheckSymlinkTest
* com.perforce.p4java.tests.dev.unit.bug.r131.ConcurrentRpcConnectionsTest
* com.perforce.p4java.tests.dev.unit.bug.r131.FileActionReplacedTest
* com.perforce.p4java.tests.dev.unit.bug.r131.FstatResolveTypeTest
* com.perforce.p4java.tests.dev.unit.bug.r131.GetStreamBaseParentFieldTest
* com.perforce.p4java.tests.dev.unit.bug.r131.GetStreamsBaseParentFilterTest
* com.perforce.p4java.tests.dev.unit.bug.r131.HighASCIIClientNameTest
* com.perforce.p4java.tests.dev.unit.bug.r131.HighASCIIPasswordTest
* com.perforce.p4java.tests.dev.unit.bug.r131.LoginAsAnotherUserTest
* com.perforce.p4java.tests.dev.unit.bug.r131.SyncAppleFileTypeTest
* com.perforce.p4java.tests.dev.unit.bug.r162.Job089581Test
* com.perforce.p4java.tests.dev.unit.bug.r162.Job089596Test
* com.perforce.p4java.tests.dev.unit.features112.DeleteFilesOptionsTest
* com.perforce.p4java.tests.dev.unit.features112.GetExportRecordsTest
* com.perforce.p4java.tests.dev.unit.features131.FilterCallbackTest
* com.perforce.p4java.tests.dev.unit.features131.QuietModeTest
* com.perforce.p4java.tests.dev.unit.features132.GetFileAnnotationsTest
* com.perforce.p4java.tests.dev.unit.features132.GetFileSizesTest
* com.perforce.p4java.tests.dev.unit.features132.GetOpenedFilesShortOutputTest
* com.perforce.p4java.tests.dev.unit.features132.GetProtectionsTableTest
* com.perforce.p4java.tests.dev.unit.features132.GetUserGroupTest
* com.perforce.p4java.tests.dev.unit.features132.SubmitShelvedChangelistTest
* com.perforce.p4java.tests.dev.unit.features132.UnloadReloadTaskStreamTest
* com.perforce.p4java.tests.dev.unit.features132.ClientCompressSyncTest
* com.perforce.p4java.tests.dev.unit.bug.r101.Job039015Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job039304Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job039331Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job039525Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job039641Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job039953Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040205Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040241Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040299Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040316Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040346Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040562Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040649Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040656Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040703Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040762Test
* com.perforce.p4java.tests.dev.unit.bug.r101.Job040877Test
* com.perforce.p4java.tests.dev.unit.bug.r111.Job042258Test
* com.perforce.p4java.tests.dev.unit.bug.r111.Job043468Test
* com.perforce.p4java.tests.dev.unit.bug.r111.Job043500Test
* com.perforce.p4java.tests.dev.unit.bug.r111.Job043524Test
* com.perforce.p4java.tests.dev.unit.bug.r112.ChangelistSubmitTest
* com.perforce.p4java.tests.dev.unit.bug.r112.ChangePasswordTest
* com.perforce.p4java.tests.dev.unit.bug.r112.CreateStreamsTest
* com.perforce.p4java.tests.dev.unit.bug.r112.FiletypesTest
* com.perforce.p4java.tests.dev.unit.bug.r112.GetChangelistsTest
* com.perforce.p4java.tests.dev.unit.bug.r112.GetFileContentsTest
* com.perforce.p4java.tests.dev.unit.bug.r112.GetProtectionsTest
* com.perforce.p4java.tests.dev.unit.bug.r112.HighSecurityLevelPasswordTest
* com.perforce.p4java.tests.dev.unit.bug.r112.LoginExceptionTest
* com.perforce.p4java.tests.dev.unit.bug.r112.ServerConfigurationTest
* com.perforce.p4java.tests.dev.unit.bug.r112.StreamingMethodsTest
* com.perforce.p4java.tests.dev.unit.bug.r112.UpdateChangelistDateTest
* com.perforce.p4java.tests.dev.unit.bug.r121.AddFilesHighASCIITest
* com.perforce.p4java.tests.dev.unit.bug.r121.GetChangelistsDateRangeTest
* com.perforce.p4java.tests.dev.unit.bug.r121.GetChangelistsOptionsTest
* com.perforce.p4java.tests.dev.unit.bug.r121.GetDirsTest
* com.perforce.p4java.tests.dev.unit.bug.r121.ServerInfoTest
* com.perforce.p4java.tests.dev.unit.bug.r121.SyncRmDirTest
* com.perforce.p4java.tests.dev.unit.bug.r123.TrustExceptionTest
* com.perforce.p4java.tests.dev.unit.bug.r141.GetDiffFilesUnchangedTest
* com.perforce.p4java.tests.dev.unit.bug.r141.LabelLockedAutoReloadTest
* com.perforce.p4java.tests.dev.unit.bug.r151.ReconcileWorkspaceFilesTest
* com.perforce.p4java.tests.dev.unit.bug.r151.ReplacementFingerprintTest
* com.perforce.p4java.tests.dev.unit.bug.r151.ServerClusterAuthTicketsTest
* com.perforce.p4java.tests.dev.unit.feature.client.SimpleEditRevertTest
* com.perforce.p4java.tests.dev.unit.helper.FactoryCreateClientTest
* com.perforce.p4java.tests.dev.unit.features123.InMemoryFingerprintsTest
