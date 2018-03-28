# List of known current bugs with the library

## com.perforce.p4java.tests.qa.CompressedRshConnectionTest

When using an RSH stream, the compressed stream tries reading beyond the
rsh stream, and it hangs forever on bytes that won't be returned.

This might be a problem with RSH + compressed streams. Need further
testing to verify.


# List of known current bugs with the tests

In general, it looks like the Idea java2 instrumenting compiler is
introducing some problems where a NPE is expected as an argument, but
instead an IAE is thrown.

## com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5DigesterTest

Tests may be running w/ text files stored in a line ending that doesn't
match the expected line endings for the current OS.  The test needs to
be updated to correct the line endings on the file.

May be better just to hard-code the text in the class itself, then write
out the file with each line ending type to ensure that it generates the
MD5 correctly.

## com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelperTest

Windows fails with "A required privilege is not held by the client."
Probably due to the temp file being placed in a restricted location.

## com.perforce.p4java.impl.mapbased.server.cmd.ChangeDelegatorTest.testGetChangelistbyIdAndOptionsNull

???

## com.perforce.p4java.impl.mapbased.server.cmd.ConfigureDelegatorTest

???
