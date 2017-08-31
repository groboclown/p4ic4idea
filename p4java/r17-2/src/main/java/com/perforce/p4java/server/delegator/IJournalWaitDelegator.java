package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.JournalWaitOptions;

/**
 * Interface for journal wait.
 */
public interface IJournalWaitDelegator {
    
    /**
     * Turns on/off journal-wait. The client application can specify "noWait"
     * replication when using a forwarding replica or an edge server.<p>
     *
     * Note that this method uses a deep undoc 'p4 journalwait [-i]' command.<p>
     *
     * @param opts JournalWaitOptions object describing optional parameters; if null, no options are
     *             set.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.2
     */
    void journalWait(JournalWaitOptions opts) throws P4JavaException;
}
