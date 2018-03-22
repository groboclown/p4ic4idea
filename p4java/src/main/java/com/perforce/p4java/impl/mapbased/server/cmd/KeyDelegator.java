package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.rethrowFunction;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.VALUE;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorStr;
import static com.perforce.p4java.server.CmdSpec.KEY;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.common.function.FunctionWithException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.KeyOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IKeyDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the Key command.
 */
public class KeyDelegator extends BaseDelegator implements IKeyDelegator {
    /**
     * Instantiate a new KeyDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public KeyDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String deleteKey(final String keyName) throws P4JavaException {
        Validate.notBlank(keyName, "Key name shouldn't null or empty");

        return setKey(keyName, null, new KeyOptions(true, false));
    }

    @Override
    public String setKey(final String keyName, final String value, final KeyOptions opts)
            throws P4JavaException {
        Validate.notBlank(keyName, "Key name shouldn't null or empty");
        List<Map<String, Object>> resultMaps = execMapCmdList(
                KEY,
                processParameters(
                        opts,
                        null,
                        new String[]{keyName, value},
                        server),
                null);

        return parseValueFromResultMaps(
                resultMaps,
                rethrowFunction(new FunctionWithException<Map, Boolean>() {
                    @Override
                    public Boolean apply(Map map) throws P4JavaException {
                        return handleErrorStr(map);
                    }
                })
        );
    }

    @Override
    public String getKey(final String keyName) throws P4JavaException {
        Validate.notBlank(keyName, "Key name shouldn't null or empty");

        List<Map<String, Object>> resultMaps = execMapCmdList(KEY, new String[]{keyName}, null);
        return parseValueFromResultMaps(
                resultMaps,
                rethrowFunction(new FunctionWithException<Map, Boolean>() {
                    @Override
                    public Boolean apply(Map map) throws P4JavaException {
                        return handleErrorStr(map);
                    }
                })
        );
    }

    private String parseValueFromResultMaps(
            final List<Map<String, Object>> resultMaps,
            final Function<Map, Boolean> construct) {
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map) && !construct.apply(map) && map.containsKey(VALUE)) {
                    return parseString(map, VALUE);
                }
            }
        }

        return EMPTY;
    }
}
