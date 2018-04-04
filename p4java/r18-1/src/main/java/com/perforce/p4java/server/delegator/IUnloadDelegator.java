package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.UnloadOptions;

/**
 * Interface to handle the Unload command.
 */
public interface IUnloadDelegator {
    /**
     * Unloads a client or label to the unload depot.
     * <p>
     * <p>
     * Note that by default, users can only unload their own clients or labels.
     * The -f flag requires 'admin' access, which is granted by 'p4 protect'.
     * The full semantics of this operation are found in the main 'p4 help
     * unload' documentation.
     *
     * @param opts possibly-null UnloadOptions object specifying method options.
     * @return non-null result message string from the unload operation.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2012.3
     */
    String unload(UnloadOptions opts) throws P4JavaException;
}
