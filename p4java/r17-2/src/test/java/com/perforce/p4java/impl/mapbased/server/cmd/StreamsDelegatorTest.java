package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.STREAMS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.tests.UnitTestGiven;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 6/10/2016
 */
public class StreamsDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String[] CMD_OPTIONS = {"-U", "-m20"};
    private static final String[] STREAM_PATHS = {"//depot/streams/dev", "//depot/streams/p16.1"};
    private static final String[] CMD_ARGUMENTS = ArrayUtils.addAll(CMD_OPTIONS, STREAM_PATHS);

    private StreamsDelegator streamsDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private GetStreamsOptions opts;
    private List<String> streamPaths;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws Exception {
        server = mock(Server.class);
        streamsDelegator = new StreamsDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = newArrayList(resultMap);

        opts = new GetStreamsOptions(CMD_OPTIONS);
        streamPaths = newArrayList(STREAM_PATHS);

        when(server.execMapCmdList(eq(STREAMS.toString()), eq(CMD_ARGUMENTS), eq(null)))
                .thenReturn(resultMaps);
    }

    /**
     * Expected return empty stream summaries when command result maps is null
     *
     * @throws Exception
     */
    @Test
    public void tesGetStreamsShouldReturnEmptyStreamSummariesWhenResultMapsIsNull()
            throws Exception {

        getStreamShouldReturnEmptyList(
                new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        when(server.execMapCmdList(
                                eq(STREAMS.toString()),
                                eq(CMD_ARGUMENTS),
                                eq(null))).thenReturn(null);
                    }
                });
    }

    /**
     * Expected return empty stream summaries all result map's file error string is not blank
     *
     * @throws Exception
     */
    @Test
    public void tesGetStreamsShouldReturnEmptyStreamSummariesWhenAllResultMapsHasFileErrorString()
            throws Exception {

        getStreamShouldReturnEmptyList(
                new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        givenHandleFileErrorStrReturnNotBlankErrorString(resultMap);
                        Map<String, Object> resultMap2 = mock(Map.class);
                        resultMaps.add(resultMap2);
                        givenHandleFileErrorStrReturnNotBlankErrorString(resultMap2);
                    }
                });
    }

    private void givenHandleFileErrorStrReturnNotBlankErrorString(Map<String, Object> resultMap) {
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get(FMT0)).thenReturn("Error message!");
    }

    private void getStreamShouldReturnEmptyList(UnitTestGiven unitTestGiven) throws Exception {
        //given
        unitTestGiven.given();
        //when
        List<IStreamSummary> streams = streamsDelegator.getStreams(streamPaths, opts);
        //then
        assertThat(streams.size(), is(0));
    }

    /**
     * Expected return non empty stream summaries
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void tesGetStreamsShouldReturnNonEmptyStreamSummaries() throws Exception {
        // given
        String streamName2 = "streamSummary02";
        String streamName3 = "streamSummary03";

        Map<String, Object> resultMap2 = mock(Map.class);
        when(resultMap2.get(MapKeys.NAME_KEY)).thenReturn(streamName2);
        Map<String, Object> resultMap3 = mock(Map.class);
        when(resultMap3.get(MapKeys.NAME_KEY)).thenReturn(streamName3);
        resultMaps.add(resultMap2);
        resultMaps.add(resultMap3);
        givenHandleFileErrorStrReturnNotBlankErrorString(resultMap);

        // when
        List<IStreamSummary> streams = streamsDelegator.getStreams(streamPaths, opts);
        // then
        assertThat(streams.size(), is(2));
        assertThat(streams.get(0).getName(), is(streamName2));
        assertThat(streams.get(1).getName(), is(streamName3));
    }
}