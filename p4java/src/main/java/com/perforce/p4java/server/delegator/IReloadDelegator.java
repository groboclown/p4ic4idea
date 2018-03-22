package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.ReloadOptions;

/**
 * Interface to handle the Reload command.
 */
public interface IReloadDelegator {
    /**
     * Reload an unloaded client or label.
     * <p>
     * <p>
     * Note that by default, users can only unload their own clients or labels.
     * The -f flag requires 'admin' access, which is granted by 'p4 protect'.
     * The full semantics of this operation are found in the main 'p4 help
     * unload' documentation.
     *
     * @param opts possibly-null ReloadOptions object specifying method options.
     * @return non-null result message string from the reload operation.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2012.3
     */
    String reload(final ReloadOptions opts) throws P4JavaException;
}
