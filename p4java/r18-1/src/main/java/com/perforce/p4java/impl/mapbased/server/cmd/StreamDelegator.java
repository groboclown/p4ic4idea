package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNullableObjectFromNonInfoMessageCommandResultMaps;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString;
import static com.perforce.p4java.server.CmdSpec.STREAM;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetStreamOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IStreamDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the Stream command.
 */
public class StreamDelegator extends BaseDelegator implements IStreamDelegator {
    /**
     * Instantiate a new StreamDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public StreamDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String createStream(@Nonnull final IStream stream) throws P4JavaException {
        Validate.notNull(stream);

        List<Map<String, Object>> resultMaps = execMapCmdList(
                STREAM,
                new String[]{"-i"},
                InputMapper.map(stream));
        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    @Override
    public IStream getStream(@Nonnull final String streamPath) throws P4JavaException {
        return getStream(streamPath, new GetStreamOptions());
    }

    @Override
    public IStream getStream(
            @Nonnull final String streamPath,
            final GetStreamOptions opts) throws P4JavaException {

        Validate.notBlank(streamPath, "Stream name shouldn't null or empty.");
        List<Map<String, Object>> resultMaps = execMapCmdList(
                STREAM,
                processParameters(opts, null, new String[]{"-o", streamPath}, server),
                null);

        return buildNullableObjectFromNonInfoMessageCommandResultMaps(
                resultMaps,
                new Function<Map, IStream>() {
                    @Override
                    public IStream apply(Map map) {
                        return new Stream(map, server);
                    }
                }
        );
    }

    @Override
    public String updateStream(
            @Nonnull final IStream stream,
            final StreamOptions opts) throws P4JavaException {

        Validate.notNull(stream);

        List<Map<String, Object>> resultMaps = execMapCmdList(
                STREAM,
                processParameters(opts, null, "-i", server),
                InputMapper.map(stream));

        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    @Override
    public String deleteStream(
            @Nonnull final String streamPath,
            final StreamOptions opts) throws P4JavaException {

        Validate.notBlank(streamPath, "Stream name shouldn't null or empty.");

        List<Map<String, Object>> resultMaps = execMapCmdList(
                STREAM,
                processParameters(opts, null, new String[]{"-d", streamPath}, server),
                null);

        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }
}
