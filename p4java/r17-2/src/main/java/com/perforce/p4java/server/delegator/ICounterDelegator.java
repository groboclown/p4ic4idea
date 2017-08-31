package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.CounterOptions;

public interface ICounterDelegator {
    String getCounter(final String counterName)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Get the value of a named Perforce counter from the Perforce server. Note
     * that this method will return a zero string (i.e. "0") if the named
     * counter doesn't exist (rather than throw an exception); use getCounters
     * to see if a counter actually exists before you use it.
     * <p>
     *
     * Note that despite their name, counters can be any value, not just a
     * number; hence the string return value here.
     *
     * @param counterName
     *            non-null counter name.
     * @param opts
     *            CounterOptions object describing optional parameters; if null,
     *            no options are set.
     * @return non-null (but possibly empty or useless) counter value associated
     *         with counterName.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2012.2
     */
    String getCounter(final String counterName, final CounterOptions opts)
            throws P4JavaException;

    void setCounter(final String counterName, final String value,
            final boolean perforceCounter)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Create, set or delete a counter on a Perforce server. This method can be
     * used to create, set, increment, or delete a counter according to the
     * specific options set in the associated options object. Note that the
     * increment operation does not work on servers earlier than 10.1, and that
     * the return value is <i>never</i> guaranteed to be non-null -- use with
     * caution.
     *
     * @param counterName
     *            non-null counter name.
     * @param value
     *            value the counter should be set to; can be null if the set
     *            operation is an increment.
     * @param opts
     *            CounterOptions object describing optional parameters; if null,
     *            no options are set.
     * @return possibly-null current (post-set, post-increment) value; may be
     *         zero if the operation was a delete; may not be reliable for pre
     *         10.1 servers.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     */
    String setCounter(final String counterName, final String value,
            final CounterOptions opts) throws P4JavaException;

    void deleteCounter(final String counterName, final boolean perforceCounter)
            throws ConnectionException, RequestException, AccessException;
}
