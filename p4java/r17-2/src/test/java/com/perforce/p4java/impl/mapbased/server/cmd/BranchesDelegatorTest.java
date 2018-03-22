package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.BRANCHES;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Executable;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.google.common.collect.Lists;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.rpc.OneShotServerImpl;
import com.perforce.p4java.option.server.GetBranchSpecsOptions;
import com.perforce.p4java.tests.UnitTestGivenThatWillThrowException;

/**
 * @author Sean Shou
 * @since 21/09/2016
 */
@RunWith(JUnitPlatform.class)
public class BranchesDelegatorTest extends AbstractP4JavaUnitTest {
    private static BranchesDelegator branchesDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private IBranchSpec mockBranchSpec;
    private IBranchSpecSummary mockBranchSpecSummary;

    @BeforeEach
    public void beforeEach() {
        server = mock(OneShotServerImpl.class);
        branchesDelegator = new BranchesDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = Lists.newArrayList(resultMap);
        // server = mock(Server.class);
        mockBranchSpec = mock(IBranchSpec.class);
    }

    /**
     * Check that a null result map results in an empty list.
     * @throws P4JavaException
     */
    @Test
    public void testNullServerResponse()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(BRANCHES.toString()), any(String[].class), eq(null)))
            .thenReturn(null);

        // when
        List<IBranchSpecSummary> branchSpecs = branchesDelegator
                .getBranchSpecs(mock(GetBranchSpecsOptions.class));

        // then
        assertThat(branchSpecs.size(), is(0));

    }

    /**
     * Check that a result map from a server gets converted into an IBranchSpec list.
     * @throws P4JavaException thrown by the delegate
     */
    @Test
    public void testNormalServerResponse()
            throws P4JavaException {
         
        // When the server is given a branches command, return a single map
        when(server.execMapCmdList(eq(BRANCHES.toString()), any(String[].class), eq(null)))
            .thenReturn(resultMaps);
        when(resultMap.get(MapKeys.ACCESS_KEY)).thenReturn("0");
        when(resultMap.get(MapKeys.UPDATE_KEY)).thenReturn("0");

        // when
        List<IBranchSpecSummary> branchSpecs = branchesDelegator
                .getBranchSpecs(new GetBranchSpecsOptions());

        // then
        assertThat(branchSpecs.size(), is(1));
    }

    /**
     * Test that a ConnectionException thrown by the underlying server implementation is passed
     * back up the chain.
     * @throws P4JavaException thrown by the delegate
     */
    @Test
    public void testServerConnectionException()
            throws P4JavaException {
        checkExceptionFilter(ConnectionException.class, ConnectionException.class);
    }

    /**
     * Test that a AccessException thrown by the underlying server implementation is passed
     * back up the chain.
     * @throws P4JavaException thrown by the delegate
     */
    @Test
    public void testAccessExceptiong()
            throws P4JavaException {
        checkExceptionFilter(AccessException.class, AccessException.class);
    }

    /**
     * Test that a RequestException thrown by the underlying server implementation is passed
     * back up the chain.
     * @throws P4JavaException thrown by the delegate
     */
    @Test
    public void testRequestException()
            throws P4JavaException {
        checkExceptionFilter(RequestException.class, RequestException.class);
    }

    /**
     * Test that a P4JavaException thrown by the underlying server implementation is passed
     * back up the chain as a RequestException.
     * @throws P4JavaException thrown by the delegate
     */
    @Test
    public void testP4JavaException()
            throws P4JavaException {
        checkExceptionFilter(P4JavaException.class, RequestException.class);
    }

    /**
     * Test that the result list from a filtered branches command contains the data from a
     * defined server response.
     * TODO: Add tests that exercise the actual filtering; e.g. no more than max
     * @throws P4JavaException
     */
    @Test
    public void testFilteredResultList()
            throws P4JavaException {
        final String knownName="testKnownName";
        final String knownDescription="testKnownSummaryDescription";
        // given
        when(server.getServerVersion()).thenReturn(20161);
        when(server.execMapCmdList(eq(BRANCHES.toString()), any(String[].class), eq(null)))
            .thenReturn(resultMaps);
        // use predictable data
        when(resultMap.get(MapKeys.BRANCH_LC_KEY)).thenReturn(knownName);
        when(resultMap.get(MapKeys.DESCRIPTION_KEY)).thenReturn(knownDescription);
        when(resultMap.get(MapKeys.ACCESS_KEY)).thenReturn("0");
        when(resultMap.get(MapKeys.UPDATE_KEY)).thenReturn("0");

        // when
        List<IBranchSpecSummary> branchSpecs = branchesDelegator.getBranchSpecs(
                "seans",
                "myFilter",
                10);

        // then
        assertEquals(1, branchSpecs.size());
        assertEquals(knownName, branchSpecs.get(0).getName());
        assertEquals(knownDescription, branchSpecs.get(0).getDescription());

    }

    /**
     * Test that when a server is less than 2005.1, a request exception will be thrown if a user
     * name filter is provided to the branches command.
     * @throws P4JavaException when supported version is too low
     */
    @Test
    public void testUserNameFilterSupportMinVersion()
            throws P4JavaException {
        // given
        when(server.getServerVersion()).thenReturn(20051);
//        doCallRealMethod().when(server).checkMinSupportedPerforceVersion(any(String.class),
//                any(int.class), any(String.class), eq("branch"));

        // then
        expectThrows(RequestException.class,
                () -> branchesDelegator.getBranchSpecs("sean", "Date_Modified>453470485", 10));
    }

    /**
     * Test that when a servers is less than 2006.1, a request exception will be thrown if a 
     * max results filter is provided to the branches command.
     * @throws P4JavaException when supported version is too low
     */
    @Test
    public void testMaxResultsFilterSupportMinVersion()
        throws P4JavaException {
        // given
        when(server.getServerVersion()).thenReturn(20051);
//        doCallRealMethod().when(server).checkMinSupportedPerforceVersion(any(String.class),
//                any(int.class), any(String.class), eq("branch"));

        // then
        expectThrows(RequestException.class,
                () -> branchesDelegator.getBranchSpecs(EMPTY, "Date_Modified>453470485", 10));
    }

    /**
     * Test that when the server is less than 2008.2, a request exception will be thrown if
     * a query filter is provided to the branches command.
     * @throws P4JavaException when supported version is too low
     */
    @Test
    public void testQueryFilterSupportMinVersion()
            throws P4JavaException {
        // given
        when(server.getServerVersion()).thenReturn(20071);
//        doCallRealMethod().when(server).checkMinSupportedPerforceVersion(any(String.class),
//                any(int.class), any(String.class), eq("branch"));

        // then
        expectThrows(RequestException.class,
                () -> branchesDelegator.getBranchSpecs(EMPTY, "Date_Modified>453470485", -1));
    }
    
    /**
     * Wrap the exception handling in the delegator such that we get the correct exception
     * propagation for the given expectations.
     * @param thrownException The exception thrown by the lower tier
     * @param expectedThrows The exception after it has been processed by the delegator
     * @throws P4JavaException the parent exception type
     */
    private void checkExceptionFilter(
            Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        Executable executable = () -> branchesDelegator.getBranchSpecs("seans", "myFilter", 10);
        UnitTestGivenThatWillThrowException unitTestGiven = (originalException) -> {
            when(server.getServerVersion()).thenReturn(20161);
            when(server.execMapCmdList(eq(BRANCHES.toString()), any(String[].class), eq(null)))
                .thenThrow(originalException);
        };

        testIfGivenExceptionWasThrown(thrownException, expectedThrows, executable, unitTestGiven);
    }
}