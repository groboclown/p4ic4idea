package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.STREAM;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.core.IStream;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetStreamOptions;
import com.perforce.p4java.option.server.StreamOptions;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 6/10/2016
 */
public class StreamDelegatorTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private String streamName = "my Stream";

    private StreamDelegator streamDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    private StreamOptions streamOptions;
    private IStream stream;
    private IOptionsServer server;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach()
            throws AccessException, RequestException, ConnectionException {
        server = mock(Server.class);
        streamDelegator = new StreamDelegator(server);

        resultMap = mock(Map.class);
        resultMaps = List.of(resultMap);
        stream = mock(IStream.class);
        streamOptions = new StreamOptions();

        streamPath = "//depot/stream/dev";

        when(server.execMapCmdList(eq(STREAM.toString()), eq(createCmdArguments), anyMap()))
                .thenReturn(resultMaps);

        when(resultMap.get(MapKeys.NAME_KEY)).thenReturn(streamName);
        when(server.execMapCmdList(eq(STREAM.toString()), eq(getCmdArguments), eq(null)))
                .thenReturn(resultMaps);

        when(resultMap.get(MapKeys.NAME_KEY)).thenReturn(streamName);
        when(server.execMapCmdList(eq(STREAM.toString()), eq(getCmdArguments), eq(null)))
                .thenReturn(resultMaps);

        when(server.execMapCmdList(eq(STREAM.toString()), eq(updateCmdArguments), anyMap()))
                .thenReturn(resultMaps);

        deleteCmdArguments = new String[]{"-d", streamPath};
        when(server.execMapCmdList(eq(STREAM.toString()), eq(deleteCmdArguments), eq(null)))
                .thenReturn(resultMaps);
    }

    private void givenInfoMessageCode(String mapKey) {
        when(resultMap.get(FMT0)).thenReturn("%" + mapKey + "%");
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get(mapKey)).thenReturn(streamName);
    }


    private final String[] createCmdArguments = {"-i"};

    /**
     * Expected throws <code><NullPointerException/code> when input 'stream' is null
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsNullPointerExceptionWhenStreamIsNull() throws Exception {
        thrown.expect(NullPointerException.class);

        // given
        stream = null;
        // then
        streamDelegator.createStream(stream);
    }

    /**
     * Expected return non blank created stream name
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonBlankCreatedStreamName() throws Exception {
        // given
        givenInfoMessageCode("createStreamName");

        // when
        String createStreamName = streamDelegator.createStream(stream);
        // then
        assertThat(createStreamName, is(streamName));
    }

    private String streamPath;
    private final String[] getCmdOptions = {"-o"};
    private final String[] getCmdArguments = ArrayUtils.add(getCmdOptions, streamPath);

    /**
     * Expected return non null <code>IStream</code>
     *
     * @throws Exception
     */
    @Test
    public void getStreamPathShouldReturnNonNullStream() throws Exception {
        // when
        IStream actualStream = streamDelegator.getStream(streamPath);
        // then
        assertThat(actualStream, notNullValue());
        assertThat(actualStream.getName(), is(streamName));
    }

    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private GetStreamOptions opts = new GetStreamOptions();

    /**
     * Expected throws <code>IllegalArgumentException</code> when 'streamPath' is blank.
     *
     * @throws Exception
     */
    @Test
    public void getStreamOptsShouldThrownIllegalArgumentExceptionWhenStreamPathIsBlank()
            throws Exception {

        thrown.expect(IllegalArgumentException.class);

        streamDelegator.getStream(EMPTY, opts);
    }

    /**
     * Expected return non null <code>IStream</code> instance
     *
     * @throws Exception
     */
    @Test
    public void getStreamShouldReturnNonNullStream() throws Exception {
        // when
        IStream actualStream = streamDelegator.getStream(streamPath, opts);
        // then
        assertThat(actualStream.getName(), is(streamName));
    }


    private final String[] updateCmdArguments = {"-i"};

    /**
     * Expected throws <code>NullPointerException</code> when input stream is null
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowNullPointerExceptionWhenStreamIsNull() throws Exception {
        thrown.expect(NullPointerException.class);

        // given
        stream = null;
        // then
        streamDelegator.updateStream(stream, streamOptions);
    }

    /**
     * Expected return non blank updated stream name
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnUpdatedStreamName() throws Exception {
        givenInfoMessageCode("updateStreamName");

        // when
        String updateStream = streamDelegator.updateStream(stream, streamOptions);
        // then
        assertThat(updateStream, is(streamName));
    }


    private String[] deleteCmdArguments;

    /**
     * Expected throws <code>NullPointerException</code> when 'streamPath' is blank
     *
     * @throws Exception
     */
    @Test
    public void shouldThrownIllegalArgumentExceptionWhenStreamPathIsBlank()
            throws Exception {
        // then
        thrown.expect(IllegalArgumentException.class);
        // given
        streamPath = EMPTY;

        //when
        streamDelegator.deleteStream(EMPTY, streamOptions);
    }


    /**
     * Expected return non blank deleted stream name
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonBlankDeletedStreamName() throws Exception {
        givenInfoMessageCode("deleteStreamName");
        // when
        String deleteStream = streamDelegator.deleteStream(streamPath, streamOptions);
        // then
        assertThat(deleteStream, is(streamName));
    }

}