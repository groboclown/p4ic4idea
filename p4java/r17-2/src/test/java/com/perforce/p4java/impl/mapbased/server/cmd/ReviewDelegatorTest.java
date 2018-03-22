package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.REVIEW;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.IReviewChangelist;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetReviewChangelistsOptions;

/**
 * @author Sean Shou
 * @since 20/09/2016
 */
public class ReviewDelegatorTest extends AbstractP4JavaUnitTest {
    private static final int CHANGELIST_ID = 10;
    private static final String[] CMD_ARGUMENTS = {"-c" + CHANGELIST_ID, "-t20"};
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private ReviewDelegator reviewDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private GetReviewChangelistsOptions opts;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        reviewDelegator = new ReviewDelegator(server);
        resultMap = Maps.newHashMap();
        resultMaps = Lists.newArrayList(resultMap);

        opts = new GetReviewChangelistsOptions(CMD_ARGUMENTS);
    }

    /**
     * Expected return empty 'review' list when command result maps is null
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyReviewListWhenResultMapsIsNull() throws Exception {
        List<IReviewChangelist> reviewChangelists = executeCmdAndReturn();
        //then
        assertThat(reviewChangelists.size(), is(0));
    }

    /**
     * Expected return empty 'review' list when 'create review changelist' throws exception
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyReviewListWhenCreateReviewChangelistThrownException()
            throws Exception {

        List<IReviewChangelist> reviewChangelists = executeCmdAndReturn();
        //then
        assertThat(reviewChangelists.size(), is(0));
    }

    /**
     * Expected throws exception when command throws exception
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsExceptionWhenInnerMethodCallThrowsIt() throws Exception {
        thrown.expect(AccessException.class);
        //given
        doThrow(AccessException.class).when(server)
                .execMapCmdList(eq(REVIEW.toString()), eq(CMD_ARGUMENTS), eq(null));
        //when
        reviewDelegator.getReviewChangelists(opts);
    }

    /**
     * Expected return non empty 'review' list
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptyReviewList() throws Exception {
        //given
        buildReviewResultMap();

        List<IReviewChangelist> reviewChangelists = executeCmdAndReturn();
        //then
        assertThat(reviewChangelists.size(), is(1));
        assertThat(reviewChangelists.get(0).getChangelistId(), is(CHANGELIST_ID));
    }

    private void buildReviewResultMap() {
        resultMap.put("change", CHANGELIST_ID);
        resultMap.put("user", "p4javatest");
        resultMap.put("email", "p4javatest@perforce.com");
        resultMap.put("name", "javatest");
    }

    private List<IReviewChangelist> executeCmdAndReturn() throws Exception {
        //given
        when(server.execMapCmdList(eq(REVIEW.toString()), eq(CMD_ARGUMENTS), eq(null)))
                .thenReturn(resultMaps);
        //when
        return reviewDelegator.getReviewChangelists(opts);
    }
}