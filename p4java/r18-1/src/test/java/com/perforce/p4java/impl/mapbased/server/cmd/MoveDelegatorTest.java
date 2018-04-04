package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FROM_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TO_FILE;
import static com.perforce.p4java.server.CmdSpec.MOVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.MoveFileOptions;

/**
 * @author Sean Shou
 * @since 22/09/2016
 */
@RunWith(NestedRunner.class)
public class MoveDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String FROM_FILE_PATH_STRING = "//depot/from/test1.txt";
    private static final String TO_FILE_PATH_STRING = "//depot/to/test1.txt";
    private static final int CHANGELIST_ID = 10;
    private static final boolean LIST_ONLY = false;
    private static final boolean FORCE = false;
    private static final String FILE_TYPE = "utf8";
    private boolean noClientMove = false;
    private String[] moveCmdArguments = {
            "-c" + CHANGELIST_ID,
            "-t" + FILE_TYPE,
            FROM_FILE_PATH_STRING,
            TO_FILE_PATH_STRING
    };

    private MoveDelegator moveDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private IFileSpec fromFileSpec;
    private IFileSpec toFileSpec;
    private int serverVersion;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        moveDelegator = new MoveDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = newArrayList(resultMap);

        fromFileSpec = mock(IFileSpec.class);
        toFileSpec = mock(IFileSpec.class);

        FilePath fromFilePath = mock(FilePath.class);
        when(fromFilePath.toString()).thenReturn(FROM_FILE_PATH_STRING);
        when(fromFileSpec.getPreferredPath()).thenReturn(fromFilePath);

        FilePath toFilePath = mock(FilePath.class);
        when(toFilePath.toString()).thenReturn(TO_FILE_PATH_STRING);
        when(toFileSpec.getPreferredPath()).thenReturn(toFilePath);

        serverVersion = 20161;
    }

    /**
     * Test moveFile()
     */
    public class TestMoveFile {
        /**
         * Test moveFile(CHANGELIST_ID, LIST_ONLY, noClientMove, FILE_TYPE, fromFileSpec, toFile)
         */
        public class WhenChangelistIdListOnlyNoClientMoveFileTypeFromFileToFileGiven {
            /**
             * Rule for expected exception verification
             */
            @Rule
            public ExpectedException thrown = ExpectedException.none();

            /**
             * Expected throws <code>NullPointerException</code> when fromFile is null,
             * So precondition check is fail.
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenFromFileIsNull() throws Exception {
                //given
                fromFileSpec = null;
                //then
                executeAndVerifyExpectedPreconditionFailException(NullPointerException.class);
            }

            /**
             * Expected throws <code>NullPointerException</code> when fromFile's preferredPath is null,
             * So precondition check is fail.
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenFromFilePreferredPathIsNull()
                    throws Exception {
                //given
                when(fromFileSpec.getPreferredPath()).thenReturn(null);
                //then
                executeAndVerifyExpectedPreconditionFailException(NullPointerException.class);
            }

            /**
             * Expected throws <code>NullPointerException</code> when toFile is null,
             * So precondition check is fail.
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenToFileIsNull() throws Exception {
                //given
                toFileSpec = null;
                //then
                executeAndVerifyExpectedPreconditionFailException(NullPointerException.class);
            }

            /**
             * Expected throws <code>NullPointerException</code> when toFile's preferredPath is null,
             * So precondition check is fail.
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenToFilePreferredPathIsNull()
                    throws Exception {
                //given
                when(toFileSpec.getPreferredPath()).thenReturn(null);
                //then
                executeAndVerifyExpectedPreconditionFailException(NullPointerException.class);
            }

            /**
             * Expected throws <code>RequestException</code> when server version is less than 20091
             * as it's not support 'move file'
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownRequestExceptionWhenServerVersionLessThan20091() throws Exception {
                //given
                serverVersion = 20090;
                when(fromFileSpec.getPreferredPath()).thenReturn(mock(FilePath.class));
                when(toFileSpec.getPreferredPath()).thenReturn(mock(FilePath.class));
                when(server.getServerVersion()).thenReturn(serverVersion);

                //then
                executeAndVerifyExpectedPreconditionFailException(RequestException.class);
            }

            /**
             * Expected throws <code>RequestException</code> when 'noClientMove' not support by server version is less than 20092
             * as it's not support 'move file'
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownRequestExceptionWhenNoClientMoveOptionNotSupportedByServerVersionLessThan20092() throws Exception {
                //given
                serverVersion = 20091;
                when(fromFileSpec.getPreferredPath()).thenReturn(mock(FilePath.class));
                when(toFileSpec.getPreferredPath()).thenReturn(mock(FilePath.class));
                when(server.getServerVersion()).thenReturn(serverVersion);
                noClientMove = true;

                //then
                executeAndVerifyExpectedPreconditionFailException(RequestException.class);
            }


            private void executeAndVerifyExpectedPreconditionFailException(Class<? extends Throwable> expectedThrownException)
                    throws Exception {

                thrown.expect(expectedThrownException);

                moveDelegator.moveFile(
                        CHANGELIST_ID,
                        LIST_ONLY,
                        noClientMove,
                        FILE_TYPE,
                        fromFileSpec,
                        toFileSpec);

                verify(server, never()).execMapCmdList(eq(MOVE.toString()), any(String[].class), eq(null));
            }

            /**
             * Expected throws <code>ConnectionException</code> or <code>ConnectionException</code> when
             * inner method call throws it
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownExceptionWhenInnerMethodCallThrowsIt() throws Exception {
                thrown.expect(ConnectionException.class);

                //given
                when(server.getServerVersion()).thenReturn(serverVersion);

                //then
                executeThenExpectThrownExceptionOrEmptyList(ConnectionException.class);
            }

            /**
             * Expected return empty moved file list when inner method call throws <code>P4JavaException</code>.
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnEmptyListWhenInnerMethodCallThrowsP4JavaException() throws Exception {
                //when
                List<IFileSpec> fileSpecs = executeThenExpectThrownExceptionOrEmptyList(P4JavaException.class);

                //then
                assertThat(fileSpecs.size(), is(0));
            }

            private List<IFileSpec> executeThenExpectThrownExceptionOrEmptyList(
                    Class<? extends Throwable> expectedThrownException) throws Exception {
                when(server.getServerVersion()).thenReturn(serverVersion);
                doThrow(expectedThrownException).when(server).execMapCmdList(eq(MOVE.toString()), eq(moveCmdArguments), eq(null));
                return moveDelegator.moveFile(
                        CHANGELIST_ID,
                        LIST_ONLY,
                        noClientMove,
                        FILE_TYPE,
                        fromFileSpec,
                        toFileSpec);
            }

            /**
             * Expected return non empty moved file specs
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonEmptyMovedFileSpecs() throws Exception {
                //given
                givenInfoMessageCode();
                when(server.getServerVersion()).thenReturn(serverVersion);
                IFileSpec fileSpec = mock(IFileSpec.class);
                when(fileSpec.getFromFile()).thenReturn(FROM_FILE_PATH_STRING);
                when(fileSpec.getToFile()).thenReturn(TO_FILE_PATH_STRING);

                when(server.execMapCmdList(eq(MOVE.toString()), eq(moveCmdArguments), eq(null))).thenReturn(resultMaps);
                //when
                List<IFileSpec> fileSpecs = moveDelegator.moveFile(
                        CHANGELIST_ID,
                        LIST_ONLY,
                        noClientMove,
                        FILE_TYPE,
                        fromFileSpec,
                        toFileSpec);

                assertThat(fileSpecs.size(), is(1));
                assertThat(fileSpecs.get(0).getFromFile(), is(FROM_FILE_PATH_STRING));
                assertThat(fileSpecs.get(0).getToFile(), is(TO_FILE_PATH_STRING));
            }
        }

        /**
         * Test moveFile(fromFileSpec, toFile, moveFileOptions)
         */
        public class WhenFromFileToFileMoveFileOptionsGiven {
            /**
             * Rule for expected exception verification
             */
            @Rule
            public ExpectedException thrown = ExpectedException.none();
            private MoveFileOptions moveFileOptions;

            /**
             * Runs before every test.
             */
            @Before
            public void beforeEach() {
                moveFileOptions = new MoveFileOptions(
                        CHANGELIST_ID,
                        LIST_ONLY,
                        FORCE,
                        noClientMove,
                        FILE_TYPE);
            }

            /**
             * Expected throws <code>NullPointerException</code> when fromFile is null,
             * So precondition check is fail.
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenFromFileIsNull() throws Exception {
                fromFileSpec = null;
                executeAndVerifyExpectedPreconditionFailException(NullPointerException.class);
            }


            /**
             * Expected throws <code>NullPointerException</code> when fromFile's preferredPath is null,
             * So precondition check is fail.
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenFromFilePreferredPathIsNull() throws Exception {
                when(fromFileSpec.getPreferredPath()).thenReturn(null);
                executeAndVerifyExpectedPreconditionFailException(NullPointerException.class);
            }

            /**
             * Expected throws <code>NullPointerException</code> when toFile is null,
             * So precondition check is fail.
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenToFileIsNull() throws Exception {
                toFileSpec = null;
                executeAndVerifyExpectedPreconditionFailException(NullPointerException.class);
            }


            /**
             * Expected throws <code>NullPointerException</code> when toFile's preferredPath is null,
             * So precondition check is fail.
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenToFilePreferredPathIsNull() throws Exception {
                when(toFileSpec.getPreferredPath()).thenReturn(null);
                executeAndVerifyExpectedPreconditionFailException(NullPointerException.class);
            }

            private void executeAndVerifyExpectedPreconditionFailException(Class<? extends Throwable> expectedThrownException)
                    throws Exception {

                thrown.expect(expectedThrownException);

                moveDelegator.moveFile(
                        fromFileSpec,
                        toFileSpec,
                        moveFileOptions);

                verify(server, never()).execMapCmdList(eq(MOVE.toString()), any(String[].class), eq(null));
            }

            /**
             * Expected return non empty moved file specs
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonEmptyMovedFileSpecs() throws Exception {
                //given
                givenInfoMessageCode();

                IFileSpec fileSpec = mock(IFileSpec.class);
                when(fileSpec.getFromFile()).thenReturn(FROM_FILE_PATH_STRING);
                when(fileSpec.getToFile()).thenReturn(TO_FILE_PATH_STRING);

                when(server.execMapCmdList(eq(MOVE.toString()), eq(moveCmdArguments), eq(null))).thenReturn(resultMaps);
                List<IFileSpec> fileSpecs = moveDelegator.moveFile(fromFileSpec, toFileSpec, moveFileOptions);
                //then
                assertThat(fileSpecs.size(), is(1));
                assertThat(fileSpecs.get(0).getFromFile(), is(FROM_FILE_PATH_STRING));
                assertThat(fileSpecs.get(0).getToFile(), is(TO_FILE_PATH_STRING));
            }
        }
    }

    private void givenInfoMessageCode() {
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get(FROM_FILE)).thenReturn(FROM_FILE_PATH_STRING);
        when(resultMap.get(TO_FILE)).thenReturn(TO_FILE_PATH_STRING);
    }
}