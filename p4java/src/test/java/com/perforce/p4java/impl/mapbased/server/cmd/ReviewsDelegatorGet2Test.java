package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetReviewsOptions;
import com.perforce.p4java.server.IOptionsServer;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Map;

import static com.perforce.p4java.server.CmdSpec.REVIEWS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
public class ReviewsDelegatorGet2Test {
    private static final String MESSAGE_CODE_IN_ERROR_RANGE = "968435456";
    private static final int CHANGELIST_ID = 10;
    private static final String TEST_FILE_DEPOT_PATH = "//depot/dev/test.txt";
    private static final String EMAIL = "p4javatest@perforce.com";
    private ReviewsDelegator reviewsDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private GetReviewsOptions opts;
    private List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(TEST_FILE_DEPOT_PATH);
    private IOptionsServer server;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws ConnectionException, AccessException, RequestException {
        server = mock(Server.class);
        reviewsDelegator = new ReviewsDelegator(server);

        resultMap = mock(Map.class);
        resultMaps = List.of(resultMap);
    }

    private void buildReviewResultMap()
            throws AccessException, RequestException, ConnectionException {
        when(resultMap.get("change")).thenReturn(CHANGELIST_ID);
        when(resultMap.get("user")).thenReturn("p4javatest");
        when(resultMap.get("email")).thenReturn(EMAIL);
        when(resultMap.get("name")).thenReturn("javatest");

        when(server.execMapCmdList(eq(REVIEWS.toString()), eq(cmdArguments), any()))
                .thenReturn(resultMaps);
    }

    private final String[] cmdOptions = {"-c" + CHANGELIST_ID};
    private final String[] cmdArguments = ArrayUtils.add(cmdOptions, TEST_FILE_DEPOT_PATH);
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Expected throws <code>ConnectionException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsConnectionExceptionWhenInnerMethodCallThrowsIt() throws Exception {
        expectedThrowsExceptions(ConnectionException.class, ConnectionException.class);
    }

    /**
     * Expected throws <code>ConnectioAccessExceptionnException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrownAccessExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {
        // p4ic4idea: use a public, non-abstract class with default constructor
        expectedThrowsExceptions(AccessException.AccessExceptionForTests.class, AccessException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrownRequestExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        expectedThrowsExceptions(RequestException.class, RequestException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws <code>P4JavaException</code>
     *
     * @throws Exception
     */
    @Test
    public void shouldThrownRequestExceptionWhenWhenInnerMethodCallThrowsItThrowsP4JavaException()
            throws Exception {

        expectedThrowsExceptions(P4JavaException.class, RequestException.class);
    }

    private void expectedThrowsExceptions(
            Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {

        //then
        thrown.expect(expectedThrows);

        // given
        doThrow(thrownException).when(server)
                .execMapCmdList(eq(REVIEWS.toString()), eq(cmdArguments), any());

        //when
        reviewsDelegator.getReviews(CHANGELIST_ID, fileSpecs);
    }

    /**
     * Expected return non empty user summaries
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptyUserSummaries() throws Exception {
        //given
        buildReviewResultMap();
        //when
        List<IUserSummary> reviews = reviewsDelegator.getReviews(CHANGELIST_ID, fileSpecs);

        //then
        assertThat(reviews.size(), is(1));
        assertThat(reviews.get(0).getEmail(), is(EMAIL));
    }
}