package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.BRANCH;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.rpc.OneShotServerImpl;
import com.perforce.p4java.option.server.DeleteBranchSpecOptions;
import com.perforce.p4java.option.server.GetBranchSpecOptions;
import com.perforce.p4java.server.delegator.IBranchDelegator;
import com.perforce.p4java.tests.UnitTestGivenThatWillThrowException;

public class BranchDelegatorTest extends AbstractP4JavaUnitTest {
    private IBranchDelegator branchSpecDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private String mockBranchSpecName = "myBranch";
    private IBranchSpec mockBranchSpec;

    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(OneShotServerImpl.class);
        branchSpecDelegator = new BranchDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = new ArrayList<Map<String, Object>>();
        resultMaps.add(resultMap);

        mockBranchSpec = mock(IBranchSpec.class);
    }

    /**
     * Test that a null spec results in an npe.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test(expected=NullPointerException.class)
    public void testNullSpec() throws P4JavaException {
        // then
        branchSpecDelegator.createBranchSpec(null);
    }

    /**
     * Test that the name of a branch spec is returned from a create command.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreate() throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(BRANCH.toString()), any(String[].class), any(Map.class)))
                .thenReturn(resultMaps);
        when(resultMap.get(CODE0)).thenReturn("268435456"); // message code in
                                                            // INFO range
        when(resultMap.get(FMT0)).thenReturn("%branch%");
        when(resultMap.get("branch")).thenReturn(mockBranchSpecName);

        // when
        String branchSpec = branchSpecDelegator.createBranchSpec(mockBranchSpec);

        // then
        assertEquals(mockBranchSpecName, branchSpec);
    }

    /**
     * Test that a connection exception during server interaction gets
     * propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testConnectionExceptionPropagationFromGetBranchSpec() throws P4JavaException {
        checkGetExceptionPropagation(ConnectionException.class, ConnectionException.class);
    }

    /**
     * Test that a access exception during server interaction gets propagated as
     * a access exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testAccessExceptionPropagationFromGetBranchSpec() throws P4JavaException {
        checkGetExceptionPropagation(AccessException.class, AccessException.class);
    }

    /**
     * Test that a request exception during server interaction gets propagated
     * as a request exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testRequestExceptionPropagationFromGetBranchSpec() throws P4JavaException {
        checkGetExceptionPropagation(RequestException.class, RequestException.class);
    }

    /**
     * Test that a p4JavaException exception during server interaction gets
     * propagated as a request exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testP4JavaExceptionPropagationFromGetBranchSpec() throws P4JavaException {
        checkGetExceptionPropagation(P4JavaException.class, RequestException.class);
    }

    /**
     * Test that BranchDelegator sends -o <name> to the server and builds a
     * branchspec object with the name returned in the resultmap provided by the
     * server. Note that this could be improved by setting up a more complete
     * result map to return and testing the fields.
     * 
     * @throws P4JavaException
     *             when the server request is not completed
     */
    @Test
    public void tetsGetBranchSpecByName() throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(BRANCH.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-o", mockBranchSpecName })),
                eq((Map<String,Object>)null))).thenReturn(resultMaps);
        when(resultMap.get("Branch")).thenReturn(mockBranchSpecName);
        // when
        IBranchSpec branchSpec = branchSpecDelegator.getBranchSpec(mockBranchSpecName);

        // then
        Assert.assertNotNull("Get branch spec with -i name, returned null when the server returned a spec", branchSpec);
        assertEquals(mockBranchSpecName, branchSpec.getName());
    }

    /**
     * Test that a null pointer exception is thrown when a null branch name is
     * provided
     * 
     * @throws P4JavaException
     *             when the server request is not completed
     */
    @Test(expected=NullPointerException.class)
    public void testGetNullBranchName() throws P4JavaException {
        // then
        branchSpecDelegator.getBranchSpec(null, mock(GetBranchSpecOptions.class));
    }

    /**
     * Test that an empty result map causes a null branch spec to be returned.
     * 
     * @throws P4JavaException
     *             when the server request is not completed
     */
    @Test
    public void testEmptyResultMap()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(BRANCH.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-o", mockBranchSpecName })),
                eq((Map<String,Object>)null))).thenReturn(new ArrayList<Map<String, Object>>());
        // when
        IBranchSpec branchSpec = branchSpecDelegator.getBranchSpec(mockBranchSpecName,
                mock(GetBranchSpecOptions.class));

        // then
        Assert.assertNull(branchSpec);
    }

    /**
     * Test that any result map from the server returns a branch spec object.
      * @throws P4JavaException
     *             when the server request is not completed
     */
    @Test
    public void testNormalResultMap()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(BRANCH.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-o", mockBranchSpecName })),
                eq((Map<String,Object>)null))).thenReturn(resultMaps);
        // when
        IBranchSpec branchSpec = branchSpecDelegator.getBranchSpec(mockBranchSpecName,
                mock(GetBranchSpecOptions.class));

        // then
        Assert.assertNotNull(branchSpec);
    }

    /**
     * Test that a null pointer exception is thrown when a null branch spec is provided to an
     * update command.
     * 
     * @throws P4JavaException
     *             when the server request is not completed
     */
    @Test(expected=NullPointerException.class)
    public void testUpdateNullBranchSpec()
            throws P4JavaException {
        // then
        branchSpecDelegator.updateBranchSpec(null);
    }

    /**
     * Test that an update request returns the formatted message with the branch number.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateBranchSpec() throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(BRANCH.toString()), any(String[].class), any(Map.class)))
            .thenReturn(resultMaps);
        when(resultMap.get(CODE0)).thenReturn("268435456"); // message code in INFO range
        when(resultMap.get(FMT0)).thenReturn("%branch%");
        when(resultMap.get("branch")).thenReturn(mockBranchSpecName);

        // when
        String updateBranchSpec = branchSpecDelegator.updateBranchSpec(mockBranchSpec);

        // then
        assertEquals(mockBranchSpecName, updateBranchSpec);
    }

    /**
     * Test that a connection exception during server interaction gets
     * propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void deleteBranchSpec_byBranchNameAndForce_shouldThrownConnectionExceptionThatWasThrownFromInnerMethodCall()
            throws P4JavaException {
        checkDeleteExceptionPropagation(ConnectionException.class, ConnectionException.class);
    }

    /**
     * Test that a connection exception during server interaction gets
     * propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void deleteBranchSpec_byBranchNameAndForce_shouldThrownRAccessExceptionThatWasThrownFromInnerMethodCall()
            throws P4JavaException {
        checkDeleteExceptionPropagation(AccessException.class, AccessException.class);
    }

    /**
     * Test that a connection exception during server interaction gets
     * propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void deleteBranchSpec_byBranchNameAndForce_shouldThrownRequestExceptionThatWasThrownFromInnerMethodCall()
            throws P4JavaException {
        checkDeleteExceptionPropagation(RequestException.class, RequestException.class);
    }

    /**
     * Test that a connection exception during server interaction gets
     * propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void deleteBranchSpec_byBranchNameAndForce_shouldThrownRequestExceptionWhenInnerMethodCallThrownP4JavaException()
            throws P4JavaException {
        checkDeleteExceptionPropagation(P4JavaException.class, RequestException.class);
    }

    /**
     * Test that a force delete passes -f -d <name> to the server and correctly process the info
     * message that comes back into a branch spec name.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testForceDeleteBranchSpec()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(BRANCH.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-f", "-d", mockBranchSpecName })),
                (Map<String,Object>)any())).thenReturn(resultMaps);
        when(resultMap.get(CODE0)).thenReturn("268435456"); // message code in INFO range
        when(resultMap.get(FMT0)).thenReturn("%branch%");
        when(resultMap.get("branch")).thenReturn(mockBranchSpecName);

        // when
        @SuppressWarnings("deprecation")
        String deleteBranchSpec = branchSpecDelegator.deleteBranchSpec(mockBranchSpecName, true);
        // then
        assertEquals(mockBranchSpecName, deleteBranchSpec);
    }

    @Test(expected=IllegalArgumentException.class)
    public void deleteBranchSpec_byBranchNameAndDeleteBranchSpecOptions_shouldThrownIllegalArgumentExceptionWhenBranchNameIsBlank()
            throws P4JavaException {
        // then
        branchSpecDelegator.deleteBranchSpec(EMPTY, mock(DeleteBranchSpecOptions.class));
    }

    /**
     * Test that a delete branch spec with options command passes -d <name> and a data map down to
     * the server, and correctly process the info message that comes back into a branch spec name.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteBranchSpecWithEmptyOptions()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(BRANCH.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-d", mockBranchSpecName })),
                (Map<String,Object>)any())).thenReturn(resultMaps);
        when(resultMap.get(CODE0)).thenReturn("268435456"); // message code in INFO range
        when(resultMap.get(FMT0)).thenReturn("%branch%");
        when(resultMap.get("branch")).thenReturn(mockBranchSpecName);

        // when
        String deleteBranchSpec = branchSpecDelegator.deleteBranchSpec(mockBranchSpecName,
                mock(DeleteBranchSpecOptions.class));

        // then
        Assert.assertEquals(mockBranchSpecName, deleteBranchSpec);
        verify(server).execMapCmdList(BRANCH.toString(), new String[] { "-d", mockBranchSpecName }, null);
    }

    /**
     * Wrap the exception handling in the delegator such that we get the correct
     * exception propagation for the given expectations when getting branch
     * specs by name.
     * 
     * @param thrownException
     *            The exception thrown by the lower tier
     * @param expectedThrows
     *            The exception after it has been processed by the delegator
     * @throws P4JavaException
     *             the parent exception type
     */
    private void checkGetExceptionPropagation(Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        
        UnitTestGivenThatWillThrowException unitTestGiven = new UnitTestGivenThatWillThrowException() {
            
            @Override
            public void given(Class<? extends P4JavaException> originalException)
                    throws P4JavaException {
                when(server.execMapCmdList(eq(BRANCH.toString()),
                    argThat(new CommandLineArgumentMatcher(new String[] { "-o", mockBranchSpecName })),
                    eq((Map<String,Object>)null))).thenThrow(originalException);
                
                branchSpecDelegator.getBranchSpec(mockBranchSpecName);
            }
        };

        testIfGivenExceptionWasThrown(thrownException, expectedThrows, unitTestGiven);
    }

    /**
     * Wrap the exception handling in the delegator such that we get the correct
     * exception propagation for the given expectations when deleting branch
     * specs.
     * 
     * @param thrownException
     *            The exception thrown by the lower tier
     * @param expectedThrows
     *            The exception after it has been processed by the delegator
     * @throws P4JavaException
     *             the parent exception type
     */
    private void checkDeleteExceptionPropagation(Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        
        UnitTestGivenThatWillThrowException unitTestGiven = new UnitTestGivenThatWillThrowException() {
            
            @SuppressWarnings("deprecation")
            @Override
            public void given(Class<? extends P4JavaException> originalException)
                    throws P4JavaException {
                when(server.execMapCmdList(eq(BRANCH.toString()),
                    argThat(new CommandLineArgumentMatcher(new String[] { "-d", mockBranchSpecName })),
                    eq((Map<String,Object>)null))).thenThrow(originalException);
                
                branchSpecDelegator.deleteBranchSpec(mockBranchSpecName, false);
            }
        };

        testIfGivenExceptionWasThrown(thrownException, expectedThrows, unitTestGiven);
    }
}