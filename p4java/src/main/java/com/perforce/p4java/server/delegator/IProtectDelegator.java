package com.perforce.p4java.server.delegator;

import java.io.InputStream;
import java.util.List;
import javax.annotation.Nonnull;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface to handle the Protect command.
 */
public interface IProtectDelegator {
    /**
     * Create or replace the protections table data on the Perforce server with
     * these new protection entries.
     * <p>
     * <p>
     * Each entry in the table contains a protection mode, a group/user
     * indicator, the group/user name, client host ID and a depot file path
     * pattern. Users receive the highest privilege that is granted on any
     * entry.
     * <p>
     * Warning: this will overwrite the existing protections table data.
     *
     * @param entryList non-null list of protection entries.
     * @return possibly-null status message string as returned from the server
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2011.2
     */
    String createProtectionEntries(@Nonnull List<IProtectionEntry> entryList)
            throws P4JavaException;

    /**
     * Replace the protections table data on the Perforce server with these new
     * protection entries.
     * <p>
     * <p>
     * Each entry in the table contains a protection mode, a group/user
     * indicator, the group/user name, client host ID and a depot file path
     * pattern. Users receive the highest privilege that is granted on any
     * entry.
     * <p>
     * <p>
     * Warning: this will overwrite the existing protections table data.
     *
     * @param entryList non-null list of protection entries.
     * @return possibly-null status message string as returned from the server
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2011.2
     */
    String updateProtectionEntries(@Nonnull List<IProtectionEntry> entryList)
            throws P4JavaException;

    /**
     * Get an InputStream onto the entries of the Perforce protections table.
     * <p>
     *
     * @return a non-null but possibly empty InputStream onto the protections
     * table's entries.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    InputStream getProtectionsTable() throws P4JavaException;
}
