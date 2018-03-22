package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.KeyOptions;


public interface IKeyDelegator {
    /**
     * Delete a key on a Perforce server.
     *
     * @param keyName non-null key name.
     * @return non-null result message string (empty) from the delete operation.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    String deleteKey(String keyName)
            throws P4JavaException;

    /**
     * Create, set or delete a key on a Perforce server. This method can be used
     * to create, set, increment, or delete a key according to the specific
     * options set in the associated options object.
     *
     * @param keyName non-null key name.
     * @param value   value the key should be set to; can be null if the set
     *                operation is an increment.
     * @param opts    KeyOptions object describing optional parameters; if null, no
     *                options are set.
     * @return possibly-null current (post-set, post-increment) value; may be
     * empty if the operation was a delete.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    String setKey(String keyName, String value, KeyOptions opts)
            throws P4JavaException;

    /**
     * Get the value of a named Perforce key from the Perforce server. Note that
     * this method will return a zero string (i.e. "0") if the named key doesn't
     * exist (rather than throw an exception); use getKeys to see if a key
     * actually exists before you use it.
     *
     * @param keyName non-null key name.
     * @return non-null (but possibly zero, if non-existing) key value
     * associated with keyName.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    String getKey(String keyName)
            throws P4JavaException;
}
