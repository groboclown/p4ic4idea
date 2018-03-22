package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.PRINT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.tests.UnitTestGivenThatWillThrowException;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 5/10/2016
 */
public class PrintDelegatorTest extends AbstractP4JavaUnitTest {
    private static final boolean ALL_REVS = true;
    private static final boolean NO_HEADER_LINE = true;
    private static final String[] CMD_OPTIONS = {"-a", "-q"};
    private static final String TEST_FILE_DEPOT_PATH = "//depot/dev/test.txt";
    private static final String[] PRINT_CMD_ARGUMENTS = ArrayUtils.add(CMD_OPTIONS, TEST_FILE_DEPOT_PATH);
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private PrintDelegator printDelegator;
    private List<IFileSpec> mockFileSpecs;
    private InputStream mockInputStream;

    /**
     * Runs before every test.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        printDelegator = new PrintDelegator(server);
        mockFileSpecs = FileSpecBuilder.makeFileSpecList(TEST_FILE_DEPOT_PATH);
        mockInputStream = mock(InputStream.class);
    }

    /**
     * Expected throws <code>ConnectionException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void testGetFileContentsShouldThrownConnectionExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        executeAndExpectedThrowsTheExceptions(ConnectionException.class, ConnectionException.class);
    }

    /**
     * Expected throws <code>AccessException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void testGetFileContentsShouldThrownRAccessExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        executeAndExpectedThrowsTheExceptions(AccessException.class, AccessException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void testGetFileContentsShouldThrownRequestExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        executeAndExpectedThrowsTheExceptions(RequestException.class, RequestException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws <code>P4JavaException</code>
     *
     * @throws Exception
     */
    @Test
    public void testGetFileContentsShouldThrownRequestExceptionWhenInnerMethodCallThrownP4JavaException()
            throws Exception {
        executeAndExpectedThrowsTheExceptions(P4JavaException.class, RequestException.class);
    }

    private void executeAndExpectedThrowsTheExceptions(
            Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {

        thrown.expect(expectedThrows);

        UnitTestGivenThatWillThrowException unitTestGiven = new UnitTestGivenThatWillThrowException() {
            @Override
            public void given(Class<? extends P4JavaException> originalException) throws P4JavaException {
                doThrow(originalException).when(server)
                        .execStreamCmd(eq(PRINT.toString()), eq(PRINT_CMD_ARGUMENTS));
            }
        };

        unitTestGiven.given(thrownException);
        printDelegator.getFileContents(mockFileSpecs, ALL_REVS, NO_HEADER_LINE);
    }

    /**
     * Expected return non null <code>InputStream</code>
     *
     * @throws Exception
     */
    @Test
    public void testGetFileContentsShouldReturnNonNullInputStream() throws Exception {
        //given
        when(server.execStreamCmd(eq(PRINT.toString()), eq(PRINT_CMD_ARGUMENTS)))
                .thenReturn(mockInputStream);
        //when
        InputStream fileContents = printDelegator.getFileContents(
                mockFileSpecs,
                ALL_REVS,
                NO_HEADER_LINE);
        //then
        assertThat(fileContents, is(mockInputStream));
    }

    /**
     * Expected return non null <code>InputStream</code>
     *
     * @throws Exception
     */
    @Test
    public void testGetFileContentsByFileSpecsAndGetFileContentsOptions() throws Exception {
        //given
        when(server.execStreamCmd(eq(PRINT.toString()), eq(new String[]{TEST_FILE_DEPOT_PATH})))
                .thenReturn(mockInputStream);
        //when
        InputStream fileContents = printDelegator.getFileContents(mockFileSpecs, null);
        //then
        assertThat(fileContents, is(mockInputStream));
    }
}