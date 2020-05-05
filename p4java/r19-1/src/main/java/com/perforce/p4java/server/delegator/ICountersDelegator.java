/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.GetCountersOptions;

import java.util.Map;

/**
 * Defintion of the counters comamnds supported in P4Java.
 */
public interface ICountersDelegator {

    /**
     * Get a map of the Perforce server's counters.
     *
     * @param opts
     *            GetCountersOptions object describing optional parameters; if
     *            null, no options are set.
     * @return a non-null (but possibly empty) map of counters.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    Map<String, String> getCounters(final GetCountersOptions opts) throws P4JavaException;
    
    /**
     * This is here to allow access via both IOptionsServer and IServer, the underlying
     * delegator implementation provides the concrete method.
     * @see com.perforce.p4java.server.IServer
     * @return a non-null (but possibly empty) map of counters. key and value
     *         semantics and format are not specified here.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    Map<String, String> getCounters() throws ConnectionException, RequestException, AccessException;
    
    /**
     * Get a map of the Perforce server's counters.
     *
     * @param opts
     *            CounterOptions object describing optional parameters; if null,
     *            no options are set.
     * @return a non-null (but possibly empty) map of counters.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2012.2
     * @deprecated As of release 2013.1, replaced by
     *             {@link #getCounters(com.perforce.p4java.option.server.GetCountersOptions)}
     */
    @Deprecated
    Map<String, String> getCounters(final CounterOptions opts) throws P4JavaException;
}