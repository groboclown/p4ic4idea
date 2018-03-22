package com.perforce.p4java.server.delegator;

import javax.annotation.Nonnull;

import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface for a DepotDelegator implementation.
 */
public interface IDepotDelegator {
    /**
     * Create a new depot in the repository. You must be an admin for this
     * operation to succeed.
     *
     * @param newDepot
     *            non-null IDepot object representing the depot to be created.
     * @return possibly-null operation result message string from the Perforce
     *         server.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     * @since 2011.1
     */
    String createDepot(@Nonnull IDepot newDepot) throws P4JavaException;

    /**
     * Delete a named depot from the repository. You must be an admin for this
     * operation to succeed.
     *
     * @param name
     *            non-null IDepot object representing the depot to be deleted
     * @return possibly-null operation result message string from the Perforce
     *         server.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     * @since 2011.1
     */
    String deleteDepot(String name) throws P4JavaException;

    /**
     * Get an individual depot by name. Note that this method will return a fake
     * depot if you ask it for a non-existent depot, so it's not the most useful
     * of operations.
     *
     * @param name
     *            non-null name of the depot to be retrieved.
     * @return IDepot non-null object corresponding to the named depot if it
     *         exists and is retrievable; otherwise an IDepot object that looks
     *         real but does not, in fact, correspond to any known depot in the
     *         repository.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     * @since 2011.1
     */
    IDepot getDepot(String name) throws P4JavaException;
}
