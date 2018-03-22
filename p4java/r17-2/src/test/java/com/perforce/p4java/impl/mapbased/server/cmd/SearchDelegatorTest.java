package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.CORE_AUTH_FAIL_STRING_1;
import static com.perforce.p4java.server.CmdSpec.SEARCH;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.SearchJobsOptions;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 14/09/2016
 */
public class SearchDelegatorTest extends AbstractP4JavaUnitTest {
    private static final int MAX = 10;
    private static final String[] CMD_OPTIONS = {"-m" + MAX};
    private static final String MESSAGE_CODE_IN_ERROR_RANGE = "968435456";
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String SEARCH_JOB_ID = "job11233";
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private SearchDelegator searchDelegator;

    private Map<String, Object> resultMap;

    private String words;
    private SearchJobsOptions opts;
    private String[] cmdArgument;


    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws ConnectionException, AccessException, RequestException {
        server = mock(Server.class);
        searchDelegator = new SearchDelegator(server);

        resultMap = mock(Map.class);
        List<Map<String, Object>> resultMaps = Lists.newArrayList(resultMap);

        opts = new SearchJobsOptions(CMD_OPTIONS);

        words = "my search words";
        cmdArgument = ArrayUtils.add(CMD_OPTIONS, words);

        when(server.execMapCmdList(eq(SEARCH.toString()), eq(cmdArgument), any()))
                .thenReturn(resultMaps);
    }

    /**
     * Expected throws <code>IllegalArgumentException</code> when search 'words' is blank
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsIllegalArgumentExceptionWhenSearchWordsIsBlank() throws Exception {
        //then
        thrown.expect(IllegalArgumentException.class);
        //given
        words = EMPTY;
        opts = null;
        //when
        searchDelegator.searchJobs(words, opts);
    }

    /**
     * Expected return empty serach jobs when command return null result maps
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptySearchJobsWhenCommandReturnNullResultMaps() throws Exception {
        cmdArgument = new String[]{words};
        when(server.execMapCmdList(eq(SEARCH.toString()), eq(cmdArgument), eq(null)))
                .thenReturn(null);
        opts = null;
        assertThat(searchDelegator.searchJobs(words, opts).size(), is(0));
    }

    /**
     * Expected throws exception when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsRequestExceptionWhenInnerMethodCallThrowsIt() throws Exception {
        // then
        thrown.expect(RequestException.class);
        // given
        givenErrorMessageCode();
        // when
        searchDelegator.searchJobs(words, opts);
    }

    /**
     * Expected return non empty search jobs
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptySearchJobs() throws Exception {
        // given
        givenInfoMessageCode();

        // when
        List<String> searchJobs = searchDelegator.searchJobs(words, opts);

        // then
        assertThat(searchJobs.size(), is(1));
        assertThat(searchJobs.get(0), is(SEARCH_JOB_ID));
    }

    private void givenErrorMessageCode() {
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_ERROR_RANGE);
        when(resultMap.get(FMT0)).thenReturn(CORE_AUTH_FAIL_STRING_1);
    }

    private void givenInfoMessageCode() {
        when(resultMap.get(FMT0)).thenReturn("%searchResult%");
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get("searchResult")).thenReturn(SEARCH_JOB_ID);
    }
}