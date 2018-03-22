package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.VALUE;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorOrInfoStr;
import static com.perforce.p4java.server.CmdSpec.KEYS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetKeysOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IKeysDelegator;

/**
 * Implementation to handle the Keys command.
 */
public class KeysDelegator extends BaseDelegator implements IKeysDelegator {
    private static final String KEY_NAME = "key";
    private static final String VALUE_NAME = VALUE;

    /**
     * Instantiate a new KeysDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public KeysDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public Map<String, String> getKeys(final GetKeysOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(
                KEYS,
                processParameters(opts, server),
                null);

        return parseKeysCommandResultMaps(resultMaps);
    }

    private Map<String, String> parseKeysCommandResultMaps(
            final List<Map<String, Object>> resultMaps) throws P4JavaException {

        Map<String, String> expectedMap = new HashMap<>();
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (!handleErrorOrInfoStr(map)) {
                    expectedMap.put(parseString(map, KEY_NAME), parseString(map, VALUE_NAME));
                }
            }
        }
        return expectedMap;
    }
}
