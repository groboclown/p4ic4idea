package com.perforce.p4java.server.delegator;

import java.io.InputStream;
import java.util.List;
import javax.annotation.Nonnull;

import com.perforce.p4java.admin.ITriggerEntry;
import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface to handle the Triggers command.
 */
public interface ITriggersDelegator {
    /**
     * Create or replace the triggers table data on the Perforce server with
     * these new trigger entries.
     * <p>
     * <p>
     * This method require that the user have 'super' access granted by 'p4
     * protect'.
     * <p>
     * <p>
     * Warning: this will overwrite the existing triggers table data.
     *
     * @param entryList non-null list of trigger entries.
     * @return possibly-null status message string as returned from the server
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2014.1
     */
    String createTriggerEntries(@Nonnull List<ITriggerEntry> entryList) throws P4JavaException;

    /**
     * Get a list of Perforce trigger entries.
     * <p>
     * <p>
     * This method require that the user have 'super' access granted by 'p4
     * protect'.
     *
     * @return non-null but possibly empty list of trigger entries.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2014.1
     */
    List<ITriggerEntry> getTriggerEntries() throws P4JavaException;

    /**
     * Replace the triggers table data on the Perforce server with these new
     * triggers entries.
     * <p>
     * <p>
     * This method require that the user have 'super' access granted by 'p4
     * protect'.
     * <p>
     * <p>
     * Warning: this will overwrite the existing triggers table data.
     *
     * @param entryList non-null list of trigger entries.
     * @return possibly-null status message string as returned from the server
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2014.1
     */
    String updateTriggerEntries(@Nonnull List<ITriggerEntry> entryList) throws P4JavaException;

    /**
     * Get an InputStream onto the entries of the Perforce triggers table.
     * <p>
     * <p>
     * This method require that the user have 'super' access granted by 'p4
     * protect'.
     *
     * @return a non-null but possibly empty InputStream onto the triggers
     * table's entries.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2014.1
     */
    InputStream getTriggersTable() throws P4JavaException;
}
