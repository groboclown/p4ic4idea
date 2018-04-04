package com.perforce.p4java.server.delegator;

import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetKeysOptions;

/**
 * Implementation to handle the Keys command.
 */
public interface IKeysDelegator {
    /**
     * Get a map of the Perforce server's keys.
     *
     * @param opts GetKeysOptions object describing optional parameters; if null,
     *             no options are set.
     * @return a non-null (but possibly empty) map of keys.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    Map<String, String> getKeys(GetKeysOptions opts) throws P4JavaException;
}
