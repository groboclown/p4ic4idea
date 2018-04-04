package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.INTERCHANGES;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Executable;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.google.common.collect.Lists;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.server.GetInterchangesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.UnitTestGiven;
import com.perforce.p4java.tests.UnitTestGivenThatWillThrowException;

/**
 * Exercise the p4java support for p4 interchanges commands and their associated
 * options. TODO: This is inadequate, -f -l -r -t -u -F are not tested and -S
 * does not look at any actual differences.
 */
@RunWith(JUnitPlatform.class)
public class InterchangesDelegatorTest extends AbstractP4JavaUnitTest {
    private InterchangesDelegator interchangesDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private IFileSpec mockFromFile;
    private IFileSpec mockToFile;
    private List<IChangelist> mockInterchanges;
    private GetInterchangesOptions mockOpts;
    private Method isListIndividualFilesThatRequireIntegration;
    private InterchangesDelegator.InterchangesDelegatorHidden interchangesDelegatorHidden;

    @BeforeEach
    public void beforeEach() {
        server = mock(IOptionsServer.class);
        interchangesDelegator = new InterchangesDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = Lists.newArrayList(resultMap);

        mockFromFile = mock(IFileSpec.class);
        mockToFile = mock(IFileSpec.class);
        mockInterchanges = mock(List.class);
        mockOpts = mock(GetInterchangesOptions.class);

        isListIndividualFilesThatRequireIntegration = getPrivateMethod(
                InterchangesDelegator.InterchangesDelegatorHidden.class,
                "isListIndividualFilesThatRequireIntegration", GetInterchangesOptions.class);
        interchangesDelegatorHidden = new InterchangesDelegator.InterchangesDelegatorHidden();
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementaion is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testConnectionException() throws P4JavaException {
        propagateExceptionsWithoutABranch(ConnectionException.class, ConnectionException.class);
    }

    /**
     * Test that an access exception thrown by the underlying server
     * implementaion is correctly propagated as an access exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testAccessException() throws P4JavaException {
        propagateExceptionsWithoutABranch(AccessException.class, AccessException.class);
    }

    /**
     * Test that a request exception thrown by the underlying server
     * implementaion is correctly propagated as a request exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testRequestException() throws P4JavaException {
        propagateExceptionsWithoutABranch(RequestException.class, RequestException.class);
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementaion is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void tetsP4JavaException() throws P4JavaException {
        propagateExceptionsWithoutABranch(P4JavaException.class, RequestException.class);
    }

    /**
     * Test that we get a list of tiles and a long changelist description back from an interchanges
     * command.
     * 
     * TODO: This only really tests that we pass the -l flag as a command line parameter, we 
     * need to actually provide a description longer than nnn in the resltmap.get mocking to
     * test the -l response processing.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testLongDescription()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(INTERCHANGES.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-f", "-l", "-C100" })),
                eq(null))).thenReturn(resultMaps);
        when(resultMap.get("change")).thenReturn("10");
        // when
        List<IChangelist> interchanges = interchangesDelegator.getInterchanges(mockFromFile,
                mockToFile, true, true, 100);
        // then
        assertEquals(1, interchanges.size());
        assertEquals(10, interchanges.get(0).getId());
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementaion is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testBranchConnectionException() throws P4JavaException {
        propagateExceptionsWithBranch(ConnectionException.class, ConnectionException.class);
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementation is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testBranchAccessException() throws P4JavaException {
        propagateExceptionsWithBranch(AccessException.class, AccessException.class);
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementaion is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testRequestExceptionWithBranch() throws P4JavaException {
        propagateExceptionsWithBranch(RequestException.class, RequestException.class);
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementaion is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testP4JavaExceptionWithBranch() throws P4JavaException {
        propagateExceptionsWithBranch(P4JavaException.class, RequestException.class);
    }

    /**
     * Test that the branch command line parameter is passed on to the server correctly.
     * 
     * TODO: Work out whether the response can be tested more than it currently is and what
     * benefits the other command line parameters provide to this test
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testBranchInterchanges()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(INTERCHANGES.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-b", "myBranch" })),
                eq(null))).thenReturn(resultMaps);
        when(resultMap.get("change")).thenReturn("10");
        // when
        List<IChangelist> changelists = interchangesDelegator.getInterchanges("myBranch",
                Lists.newArrayList(mockFromFile), Lists.newArrayList(mockToFile), mockOpts);
        // then
        assertEquals(1, changelists.size());
        assertEquals(10, changelists.get(0).getId());
    }

    /**
     * Test that the command line parameters are passed on to the server correctly.
     * 
     * TODO: Work out whether there is benefit in this test over the previous one, which seems
     * to test more. This one probably needs resultMaps which reflect the options passed in and
     * checks to make sure they are applied properly
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testBranchInterchangesWithFilters()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(INTERCHANGES.toString()),
                argThat(new CommandLineArgumentMatcher(
                        new String[] { "-f", "-l", "-C100", "-r", "-b", "myBranch", "-s" })),
                eq(null))).thenReturn(resultMaps);
        when(resultMap.get("change")).thenReturn("10");
        List<IChangelist> interchanges = interchangesDelegator.getInterchanges("myBranch",
                Lists.newArrayList(mockFromFile), Lists.newArrayList(mockToFile), true, true, 100,
                true, true);
        // then
        assertEquals(1, interchanges.size());
    }

    /**
     * Test that a standard interchanges command provides no additional command line parameters
     * and processes the server response correctly.
     * 
     * TODO: Provide a long changelist description to show that it is limited.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testInterchanges()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(INTERCHANGES.toString()),
                argThat(new CommandLineArgumentMatcher(new String[0])), eq(null)))
                        .thenReturn(resultMaps);
        when(resultMap.get("change")).thenReturn("10");
        // when
        List<IChangelist> interchanges = interchangesDelegator.getInterchanges(mockFromFile,
                mockToFile, mockOpts);
        // then
        assertEquals(1, interchanges.size());
        assertEquals(10, interchanges.get(0).getId());
    }

    /**
     * Test that when the server replies with a null result map, an empty list is returned.
     * 
     * TODO: Check that the server can reply with null and that this is not really a request
     * exception. Also, this passes a true flag so we should be checking that there is also a
     * list of files returned.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testNullResultMap()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(INTERCHANGES.toString()),
                argThat(new CommandLineArgumentMatcher(new String[0])), eq(null))).thenReturn(null);
        when(server.execMapCmdList(eq(INTERCHANGES.toString()), any(String[].class), eq(null)))
                .thenReturn(null);
        // when
        List<IChangelist> changelists = interchangesDelegator.processInterchangeMaps(null, true);
        // then
        assertNotNull(changelists);
        assertEquals(0, changelists.size());
    }

    /**
     * Test that an error message results in an empty list of changelists even if the server were
     * to reply with a contradictory return code.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testErrorMessageWithValidReturnCode()
            throws P4JavaException {
        checkEmptyChangelistResult(() -> {
            // given
            when(server.handleFileErrorStr(resultMap)).thenReturn("error was occurred");
            when(server.getGenericCode(resultMap)).thenReturn(17);
        });
    }

    /**
     * Test that an error message results in an empty list of changelists even if the server were
     * to reply with a contradictory severity code.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testErrorMessageWithValidSeverityCode()
            throws P4JavaException {
        checkEmptyChangelistResult(() -> {
            // given
            when(server.handleFileErrorStr(resultMap)).thenReturn("error was occurred");
            when(server.getSeverityCode(resultMap)).thenReturn(2);
        });
    }

    /**
     * Test that an empty changelist list is returned when revisions are already integrated.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testAlreadyIntegrated()
            throws P4JavaException {
        checkEmptyChangelistResult(() -> {
            when(server.getGenericCode(resultMap)).thenReturn(17);
            when(server.getSeverityCode(resultMap)).thenReturn(2);
            when(server.handleFileErrorStr(resultMap))
                    .thenReturn("all revision(s) already integrated");
        });
    }

    /**
     * Test that when a server replies with an error message then we get an empty changelist list.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testServerErrorMessage()
            throws P4JavaException {
        // given
        when(server.handleFileErrorStr(resultMap)).thenReturn("error was occurred");
        int isNot17 = 19;
        int isNot2 = 3;
        when(server.getGenericCode(resultMap)).thenReturn(isNot17);
        when(server.getSeverityCode(resultMap)).thenReturn(isNot2);
        // then
        expectThrows(RequestException.class,
                () -> interchangesDelegator.processInterchangeMaps(resultMaps, true));
    }

    /**
     * Test that an error message results in single changelist with an empty list of files
     * when the server response contains no error code.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testEmptyErrorsResult()
            throws P4JavaException {
        // given
        when(server.handleFileErrorStr(resultMap)).thenReturn(EMPTY);

        // when
        List<IChangelist> changelists = interchangesDelegator.processInterchangeMaps(resultMaps,
                false);
        // then
        assertEquals(1, changelists.size());
        assertEquals(0, ((Changelist) changelists.get(0)).getFileSpecs().size());
    }

    /**
     * Test that non-null depot files in the result map lead to file spec objects in the
     * resulting changelist data and that null depot files are ignored.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testDepotFiles()
            throws P4JavaException {
        // given
        when(server.handleFileErrorStr(resultMap)).thenReturn(EMPTY);
        Object nonNullObj = new Object();
        when(resultMap.get("depotFile0")).thenReturn(nonNullObj);
        when(resultMap.get("depotFile1")).thenReturn(nonNullObj);
        when(resultMap.get("depotFile2")).thenReturn(null);

        // when
        List<IChangelist> changelists = interchangesDelegator.processInterchangeMaps(resultMaps,
                true);
        // then
        assertEquals(1, changelists.size());
        assertEquals(2, ((Changelist) changelists.get(0)).getFileSpecs().size());
    }

    /**
     * Test that show files returns false if no options are passed.
     * @throws InvocationTargetException when there is a problem with reflection
     * @throws IllegalAccessException when there is a problem with reflection
     */
    @Test
    public void testShowFilesWithoutOptions()
            throws InvocationTargetException, IllegalAccessException {
        // when
        GetInterchangesOptions options = null;
        boolean actual = (boolean) isListIndividualFilesThatRequireIntegration
                .invoke(interchangesDelegatorHidden, options);

        // then
        assertFalse(actual);
    }

    /**
     * Test that show files returns false when the options object has false set.
     * @throws InvocationTargetException when there is a problem with reflection
     * @throws IllegalAccessException when there is a problem with reflection
     */
    @Test
    public void testShowFilesWithFalse()
            throws InvocationTargetException, IllegalAccessException {
        // when
        boolean actual = (boolean) isListIndividualFilesThatRequireIntegration
                .invoke(interchangesDelegatorHidden, new  GetInterchangesOptions().setShowFiles(false));

        // then
        assertFalse(actual);
    }

    /**
     * Test that show files returns false when the options object has false set.
     * @throws InvocationTargetException when there is a problem with reflection
     * @throws IllegalAccessException when there is a problem with reflection
     */
    @Test
    public void testShowFilesWithTrue()
            throws InvocationTargetException, IllegalAccessException {
        // when
        boolean actual = (boolean) isListIndividualFilesThatRequireIntegration
                .invoke(interchangesDelegatorHidden, new  GetInterchangesOptions().setShowFiles(true));

        // then
        assertTrue(actual);
    }

    /**
     * Test that a given server response, provided by the interceptor, results in an empty
     * list of changelists.
     * 
     * @param unitTestGiven the mocking interceptor to apply for this test
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    private void checkEmptyChangelistResult(UnitTestGiven unitTestGiven) throws P4JavaException {
        // given
        unitTestGiven.given();
        // when
        List<IChangelist> changelists = interchangesDelegator.processInterchangeMaps(resultMaps,
                true);
        // then
        assertNotNull(changelists);
        assertEquals(0, changelists.size());
    }

    /**
     * Simulate an exception being thrown from the underlying server back to the interchanges
     * delegator and verify that it propagates the appropriate exception.
     * 
     * @param thrownException the exception thrown by the server
     * @param expectedThrows the exception expected from the delegator
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    private void propagateExceptionsWithoutABranch(Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        Executable executable = () -> interchangesDelegator.getInterchanges(mockFromFile,
                mockToFile, true, true, 100);
        UnitTestGivenThatWillThrowException unitTestGiven = (originalException) -> {
            when(server.execMapCmdList(eq(INTERCHANGES.toString()), any(String[].class), eq(null)))
                    .thenThrow(originalException);
        };

        testIfGivenExceptionWasThrown(thrownException, expectedThrows, executable, unitTestGiven);
    }

    /**
     * Simulate an exception being thrown from the underlying server back to the interchanges
     * delegator when a branch parameter is provided and verify that it propagates the appropriate
     * exception.
     * 
     * @param thrownException the exception thrown by the server
     * @param expectedThrows the exception expected from the delegator
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    private void propagateExceptionsWithBranch(Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        Executable executable = () -> interchangesDelegator.getInterchanges("myBranch",
                Lists.newArrayList(mockFromFile), Lists.newArrayList(mockToFile), true, true, 100,
                true, true);
        UnitTestGivenThatWillThrowException unitTestGiven = (originalException) -> {
            when(server.execMapCmdList(eq(INTERCHANGES.toString()), any(String[].class), eq(null)))
                    .thenThrow(originalException);
        };

        testIfGivenExceptionWasThrown(thrownException, expectedThrows, executable, unitTestGiven);
    }
}