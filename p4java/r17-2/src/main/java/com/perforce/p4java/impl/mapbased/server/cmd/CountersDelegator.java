package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.rethrowFunction;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.VALUE;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorOrInfoStr;
import static com.perforce.p4java.server.CmdSpec.COUNTERS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.common.function.FunctionWithException;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.GetCountersOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ICountersDelegator;

/**
 * @author Sean Shou
 * @since 26/09/2016
 */
public class CountersDelegator extends BaseDelegator implements ICountersDelegator {
    private FunctionWithException<Map<String, Object>, Boolean> uncheckExceptionFunction =
            new FunctionWithException<Map<String, Object>, Boolean>() {
                @Override
                public Boolean apply(Map<String, Object> map) throws P4JavaException {
                    return handleErrorOrInfoStr(map);
                }
            };

    /**
     * Create a new delegator with a concrete server implementation.
     *
     * @param server the real server implementation
     */
    public CountersDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public Map<String, String> getCounters()
            throws ConnectionException, RequestException, AccessException {

        try {
            return getCounters(new GetCountersOptions());
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public Map<String, String> getCounters(final CounterOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(
                COUNTERS,
                processParameters(opts, server),
                null);

        return parseCounterCommandResultMaps(
                resultMaps,
                rethrowFunction(uncheckExceptionFunction)
        );
    }

    @Override
    public Map<String, String> getCounters(final GetCountersOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(
                COUNTERS,
                processParameters(opts, server),
                null);
        return parseCounterCommandResultMaps(
                resultMaps,
                rethrowFunction(uncheckExceptionFunction)
        );
    }

    /**
     * TODO: This should live with the ResultMapParser class, with a method that takes a string
     * representing the key values to look for & an object that looks for errors/info messages
     * and throws an exception if appropriate.
     */
    private Map<String, String> parseCounterCommandResultMaps(
            final List<Map<String, Object>> resultMaps,
            @Nonnull final Function<Map<String, Object>, Boolean> errorOrInfoStringCheckFunc)
            throws AccessException, RequestException {

        Map<String, String> counterMap = new HashMap<>();
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (!errorOrInfoStringCheckFunc.apply(map)) {
                    counterMap.put(parseString(map, "counter"), parseString(map, VALUE));
                }
            }
        }
        return counterMap;
    }
}
