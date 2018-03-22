package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleFileErrorStr;
import static com.perforce.p4java.server.CmdSpec.STREAMS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.StreamSummary;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.delegator.IStreamsDelegator;

/**
 * Implementation to handle the Streams command.
 */
public class StreamsDelegator extends BaseDelegator implements IStreamsDelegator {
    /**
     * Instantiate a new StreamsDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public StreamsDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IStreamSummary> getStreams(
            final List<String> streamPaths,
            final GetStreamsOptions opts) throws P4JavaException {

        String[] args = {};
        if (nonNull(streamPaths)) {
            args = streamPaths.toArray(new String[streamPaths.size()]);
        }

        List<IStreamSummary> streamList = new ArrayList<>();

        List<Map<String, Object>> resultMaps = execMapCmdList(
                STREAMS,
                processParameters(opts, null, args, server),
                null);

        if (nonNull(resultMaps)) {
            for (Map<String, Object> streamMap : resultMaps) {
                // p4ic4idea: use IServerMessage
                IServerMessage errStr = handleFileErrorStr(streamMap);
                if (nonNull(errStr)) {
                    Log.error(errStr.toString());
                } else {
                    streamList.add(new StreamSummary(streamMap, true));
                }
            }
        }

        return streamList;
    }
}
