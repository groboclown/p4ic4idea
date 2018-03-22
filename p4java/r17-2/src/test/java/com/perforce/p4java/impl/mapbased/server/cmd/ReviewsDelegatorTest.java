package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.CORE_AUTH_FAIL_STRING_1;
import static com.perforce.p4java.server.CmdSpec.REVIEWS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.common.collect.Lists;
import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetReviewsOptions;
import com.perforce.p4java.tests.UnitTestGiven;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
@RunWith(NestedRunner.class)
public class ReviewsDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_ERROR_RANGE = "968435456";
    private static final int CHANGELIST_ID = 10;
    private static final String TEST_FILE_DEPOT_PATH = "//depot/dev/test.txt";
    private static final String EMAIL = "p4javatest@perforce.com";
    private ReviewsDelegator reviewsDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private GetReviewsOptions opts;
    private List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(TEST_FILE_DEPOT_PATH);

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws ConnectionException, AccessException, RequestException {
        server = mock(Server.class);
        reviewsDelegator = new ReviewsDelegator(server);

        resultMap = mock(Map.class);
        resultMaps = Lists.newArrayList(resultMap);
    }

    private void buildReviewResultMap() {
        when(resultMap.get("change")).thenReturn(CHANGELIST_ID);
        when(resultMap.get("user")).thenReturn("p4javatest");
        when(resultMap.get("email")).thenReturn(EMAIL);
        when(resultMap.get("name")).thenReturn("javatest");
    }

    /**
     * Test getReviews()
     */
    public class TestGetReviews {
        /**
         * Test getReviews(fileSpecs, getReviewsOptions)
         */
        public class WhenFileSpecsGetReviewsOptionsGiven {
            private final String[] cmdOptions = {"-C" + "p4javaTest", "-c" + CHANGELIST_ID};
            private final String[] cmdArguments = ArrayUtils.add(cmdOptions, TEST_FILE_DEPOT_PATH);
            /**
             * Rule for expected exception verification
             */
            @Rule
            public ExpectedException thrown = ExpectedException.none();

            /**
             * Runs before every test.
             */
            @SuppressWarnings("unchecked")
            @Before
            public void beforeEach() throws ConnectionException, AccessException, RequestException {
                opts = new GetReviewsOptions(cmdOptions);

                when(server.execMapCmdList(eq(REVIEWS.toString()), eq(cmdArguments), any())).thenReturn(resultMaps);
            }

            /**
             * Expected return empty user summaries when command result maps is null
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnEmptyUserSummariesWhenResultMapsIsNull() throws Exception {
                shouldReturnEmptyUserSummaries(new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        when(server.execMapCmdList(eq(REVIEWS.toString()), eq(cmdArguments), any()))
                                .thenReturn(null);
                    }
                });
            }

            /**
             * Expected return empty user summaries when InnerCatchBodyThrownUnexpectedException
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnEmptyUserSummariesWhenInnerCatchBodyThrownUnexpectedException() throws Exception {
                shouldReturnEmptyUserSummaries(new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        doThrow(RequestException.class).when(resultMap).get("user");
                    }
                });
            }

            private void shouldReturnEmptyUserSummaries(UnitTestGiven unitTestGiven) throws Exception {
                unitTestGiven.given();
                //when
                List reviews = reviewsDelegator.getReviews(fileSpecs, opts);

                //then
                assertThat(reviews.size(), is(0));
            }


            /**
             * Expected throws exception when inner 'handleErrorStr()' throws exception
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownExceptionWhenInnerHandleErrorStrCallThrowsIt() throws Exception {
                thrown.expect(P4JavaException.class);
                //given
                givenErrorMessageCode();

                //then
                reviewsDelegator.getReviews(fileSpecs, opts);
            }

            /**
             * Expected return non empty user summaries
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonEmptyUserSummaries() throws Exception {
                //when
                buildReviewResultMap();
                List<IUserSummary> reviews = reviewsDelegator.getReviews(fileSpecs, opts);

                //then
                assertThat(reviews.size(), is(1));
                assertThat(reviews.get(0).getEmail(), is(EMAIL));
                verify(resultMap).get("user");
                verify(resultMap).get("email");
                verify(resultMap).get("name");
            }

            private void givenErrorMessageCode() {
                when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_ERROR_RANGE);
                when(resultMap.get(FMT0)).thenReturn(CORE_AUTH_FAIL_STRING_1);
            }
        }

        /**
         * Test getReviews(changelistId, fileSpecs)
         */
        public class WhenChangelistIdFileSpecsGiven {
            private final String[] cmdOptions = {"-c" + CHANGELIST_ID};
            private final String[] cmdArguments = ArrayUtils.add(cmdOptions, TEST_FILE_DEPOT_PATH);
            /**
             * Rule for expected exception verification
             */
            @Rule
            public ExpectedException thrown = ExpectedException.none();

            /**
             * Runs before every test.
             */
            @SuppressWarnings("unchecked")
            @Before
            public void beforeEach() throws Exception {
                when(server.execMapCmdList(eq(REVIEWS.toString()), eq(cmdArguments), any()))
                        .thenReturn(resultMaps);
            }

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
                expectedThrowsExceptions(AccessException.class, AccessException.class);
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
    }
}