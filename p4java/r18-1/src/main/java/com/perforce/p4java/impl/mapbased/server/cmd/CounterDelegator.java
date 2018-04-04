package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.VALUE;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.COUNTER;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ICounterDelegator;
import org.apache.commons.lang3.Validate;

/**
 * @author Sean Shou
 * @since 26/09/2016
 */
public class CounterDelegator extends BaseDelegator implements ICounterDelegator {

    /**
     * Instantiates a new change delegator.
     * 
     * @param server
     *            the server
     */
    public CounterDelegator(final IOptionsServer server) {
        super(server);
    }	
	
    @Override
    public String getCounter(final String counterName)
            throws ConnectionException, RequestException, AccessException {

        try {
            return getCounter(counterName, null);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public String getCounter(final String counterName, final CounterOptions opts)
            throws P4JavaException {

        Validate.notBlank(counterName, "Counter name shouldn't be null or empty");

        List<Map<String, Object>> resultMaps = execMapCmdList(COUNTER,
                processParameters(opts, null, new String[] { counterName }, server), null);
        
        return ResultListBuilder.buildNullableObjectFromNonInfoMessageCommandResultMaps(
                resultMaps,
                new Function<Map, String>() {
                    @Override
                    public String apply(Map map) {
                        return parseString(map, VALUE);
                    }
                }
        );
    }

    @Override
    public void setCounter(final String counterName, final String value,
            final boolean perforceCounter)
            throws ConnectionException, RequestException, AccessException {

        Validate.notBlank(counterName, "Counter name shouldn't be null or empty");
        Validate.notBlank(value, "Counter value shouldn't be null or empty");

        try {
            CounterOptions counterOptions = new CounterOptions()
                    .setPerforceCounter(perforceCounter);

            setCounter(counterName, value, counterOptions);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public String setCounter(final String counterName, final String value,
            final CounterOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(COUNTER,
                processParameters(opts, null, new String[] { counterName, value }, server), null);

        return ResultListBuilder.buildNullableObjectFromNonInfoMessageCommandResultMaps(
                resultMaps,
                new Function<Map, String>() {
                    @Override
                    public String apply(Map map) {
                        return parseString(map, VALUE);
                    }
                }
        );
    }

    @Override
    public void deleteCounter(final String counterName, final boolean perforceCounter)
            throws ConnectionException, RequestException, AccessException {

        Validate.notBlank(counterName, "Counter name shouldn't null or empty");

        try {
            CounterOptions counterOptions = new CounterOptions(perforceCounter, true, false);
            setCounter(counterName, null, counterOptions);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }
}
