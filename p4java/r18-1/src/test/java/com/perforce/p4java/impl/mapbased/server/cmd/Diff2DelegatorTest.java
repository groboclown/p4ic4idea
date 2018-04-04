package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.core.file.DiffType.SUMMARY_DIFF;
import static com.perforce.p4java.server.CmdSpec.DIFF2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetFileDiffsOptions;

/**
 * @author Sean Shou
 * @since 27/09/2016
 */
@RunWith(NestedRunner.class)
public class Diff2DelegatorTest extends AbstractP4JavaUnitTest {
    private Diff2Delegator diff2Delegator;
    private Map<String, Object> resultMap1;
    private Map<String, Object> resultMap2;
    private List<Map<String, Object>> resultMaps;

    private String file1 = "//depot/test1.txt";
    private String file2 = "//depot/test2.txt";
    private IFileSpec fileSpec1;
    private IFileSpec fileSpec2;

    private String branchName = "myBranch";

    private GetFileDiffsOptions fileDiffsOptions;
    private String[] cmdArguments = {"-b", branchName, file1, file2};
    private String[] summaryDiffCmdArguments = {"-ds", "-b", branchName, file1, file2};

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        diff2Delegator = new Diff2Delegator(server);
        resultMap1 = mock(Map.class);
        resultMap2 = mock(Map.class);
        resultMaps = newArrayList(resultMap1, resultMap2);
        fileSpec1 = new FileSpec(file1);
        fileSpec2 = new FileSpec(file2);
        fileDiffsOptions = new GetFileDiffsOptions();
    }

    /**
     * Test getFileDiffs()
     */
    public class TestGetFileDiffs {
        /**
         * Test getFileDiffs() by 'fileSpec, branchSpecName, fileDiffOptions' arguments.
         */
        public class WhenFileSpecBranchSpecNameAndGetFileDiffsOptionsGiven {
            /**
             * Test get file diffs by <code>IFileSpec</code>, branchName, diffType and <code>GetFileDiffsOptions</code>.
             * It's expected thrown <code>ConnectionException</code>.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = ConnectionException.class)
            public void shouldThrownConnectionExceptionWhenInnerMethodThrownIt() throws Exception {
                doThrow(ConnectionException.class).when(server).execMapCmdList(
                        eq(DIFF2.toString()),
                        eq(cmdArguments),
                        eq(null));

                diff2Delegator.getFileDiffs(
                        fileSpec1,
                        fileSpec2,
                        branchName,
                        fileDiffsOptions);
            }

            /**
             * Test get file diffs by <code>IFileSpec</code> and <code>GetFileDiffsOptions</code>.
             * It's expected return a non-empty list.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test
            public void shouldReturnNonEmptyList() throws Exception {
                //given
                when(server.execMapCmdList(
                        eq(DIFF2.toString()),
                        eq(cmdArguments),
                        eq(null))).thenReturn(resultMaps);
                //when
                List<IFileDiff> fileDiffs = diff2Delegator.getFileDiffs(
                        fileSpec1,
                        fileSpec2,
                        branchName,
                        fileDiffsOptions);
                //then
                assertThat(fileDiffs.size(), is(2));
            }
        }

        /**
         * Test getFileDiffs() by 'fileSpec, branchSpecName, diffType, quiet, includeNonTextDiffs' arguments.
         */
        public class WhenFileSpecBranchSpecNameDiffTypeQuietAndIncludeNonTextDiffsGiven {
            private DiffType diffType;

            /**
             * Runs before every test.
             */
            @Before
            public void beforeEach() {
                diffType = SUMMARY_DIFF;
            }

            /**
             * Test get file diffs by <code>IFileSpec</code>, branchName, diffType and <code>GetFileDiffsOptions</code>.
             * It's expected thrown <code>ConnectionException</code>.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = ConnectionException.class)
            public void shouldThrownConnectionExceptionWhenCommandThrownIt() throws Exception {
                executeAndThrowsException(ConnectionException.class);
            }

            /**
             * Test get file diffs by <code>IFileSpec</code>, branchName, diffType and <code>GetFileDiffsOptions</code>.
             * It's expected thrown <code>ConnectionException</code>.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = RequestException.class)
            public void shouldThrownRequestExceptionWhenCommandThrownP4JavaException()
                    throws Exception {
                executeAndThrowsException(P4JavaException.class);
            }

            private void executeAndThrowsException(Class<? extends Throwable> toBeThrown)
                    throws Exception {
                doThrow(toBeThrown).when(server).execMapCmdList(eq(DIFF2.toString()),
                        eq(summaryDiffCmdArguments),
                        eq(null));

                diff2Delegator.getFileDiffs(
                        fileSpec1,
                        fileSpec2,
                        branchName,
                        diffType,
                        false,
                        false,
                        false);
            }

            /**
             * Test expected return non empty file diffs
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonEmptyFileDiffs() throws Exception {
                when(server.execMapCmdList(eq(DIFF2.toString()),
                        eq(summaryDiffCmdArguments),
                        eq(null))).thenReturn(resultMaps);

                List<IFileDiff> fileDiffs = diff2Delegator.getFileDiffs(
                        fileSpec1,
                        fileSpec2,
                        branchName,
                        diffType,
                        false,
                        false,
                        false);

                assertThat(fileDiffs.size(), is(2));
            }
        }
    }

    /**
     * test getFileDiffsStream()
     */
    public class TestGetFileDiffsStream {
        /**
         * Test get file diffs <code>InputStream</code> by <code>IFileSpec</code>, branchName, diffType and <code>GetFileDiffsOptions</code>.
         * It's expected return non-null <code>InputStream</code>
         *
         * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
         */
        @Test
        public void shouldReturnFileDiffsInputStream() throws Exception {
            //given
            InputStream mockInputStream = mock(InputStream.class);
            when(server.execStreamCmd(
                    eq(DIFF2.toString()),
                    eq(cmdArguments)))
                    .thenReturn(mockInputStream);
            //when
            InputStream fileDiffsStream = diff2Delegator.getFileDiffsStream(
                    fileSpec1,
                    fileSpec2,
                    branchName,
                    fileDiffsOptions);
            //then
            assertThat(fileDiffsStream, is(mockInputStream));
        }
    }

    /**
     * test getServerFileDiffs()
     */
    public class TestGetServerFileDiffs {
        /**
         * Test get server file diffs by <code>IFileSpec</code>, branchName, diffType, quiet, includeNonTextDiffs and gnuDiffs
         * It's expected thrown <code>ConnectionException</code>.
         *
         * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
         */
        @Test(expected = ConnectionException.class)
        public void shouldThrowConnectionExceptionWhenInnerCmdCallThrowIt() throws Exception {
            executeAndThrowException(ConnectionException.class);
        }

        /**
         * Test get server file diffs by <code>IFileSpec</code>, branchName, diffType, quiet, includeNonTextDiffs and gnuDiffs
         * It's expected thrown <code>RequestException</code>.
         *
         * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
         */
        @Test(expected = RequestException.class)
        public void shouldThrowRequestExceptionWhenInnerCmdCallThrowP4JavaException()
                throws Exception {
            executeAndThrowException(P4JavaException.class);
        }

        private void executeAndThrowException(Class<? extends Throwable> toBeThrown)
                throws Exception {
            doThrow(toBeThrown).when(server).execStreamCmd(
                    eq(DIFF2.toString()),
                    eq(summaryDiffCmdArguments));

            diff2Delegator.getServerFileDiffs(
                    fileSpec1,
                    fileSpec2,
                    branchName,
                    DiffType.SUMMARY_DIFF,
                    false,
                    false,
                    false);
        }

        /**
         * Test get file diffs <code>InputStream</code> by <code>IFileSpec</code>, branchName, diffType, quiet, includeNonTextDiffs and gnuDiffs.
         * It's expected return non-null <code>InputStream</code>
         *
         * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
         */
        @Test
        public void shouldReturnServerFileDiffsInputStream() throws Exception {
            //given
            InputStream mockInputStream = mock(InputStream.class);
            when(server.execStreamCmd(
                    eq(DIFF2.toString()),
                    eq(summaryDiffCmdArguments)))
                    .thenReturn(mockInputStream);
            //when
            InputStream fileDiffsStream = diff2Delegator.getServerFileDiffs(
                    fileSpec1,
                    fileSpec2,
                    branchName,
                    SUMMARY_DIFF,
                    false,
                    false,
                    false);
            //then
            assertThat(fileDiffsStream, is(mockInputStream));
        }
    }
}