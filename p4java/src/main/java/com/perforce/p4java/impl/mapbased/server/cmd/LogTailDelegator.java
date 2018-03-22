package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfConditionFails;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DATA;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OFFSET;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.LOGTAIL;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.admin.ILogTail;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.LogTail;
import com.perforce.p4java.option.server.LogTailOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.delegator.ILogTailDelegator;

/**
 * Implementation for logtail.
 */
public class LogTailDelegator extends BaseDelegator implements ILogTailDelegator {
    
    /**
     * Instantiates a new log tail delegator.
     *
     * @param server the server
     */
    public LogTailDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public ILogTail getLogTail(final LogTailOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(LOGTAIL,
                processParameters(opts, server), null);

        String logFile = null;
        long offset = -1;
        List<String> data = new ArrayList<>();
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map)) {
                    // p4ic4idea: use IServerMessage
                    ResultMapParser.handleErrors(ResultMapParser.toServerMessage(map));
                    try {
                        if (map.containsKey(FILE)) {
                            logFile = parseString(map, FILE);
                        }

                        if (map.containsKey(DATA)) {
                            data.add(parseString(map, DATA));
                        }
                        if (map.containsKey(OFFSET)) {
                            offset = parseLong(map, OFFSET);
                        }
                    } catch (Throwable thr) {
                        Log.exception(thr);
                    }
                }
            }
        }
        if (isNotBlank(logFile) && !data.isEmpty() && offset > -1) {
            return new LogTail(logFile, offset, data);
        }
        return null;
    }
}
