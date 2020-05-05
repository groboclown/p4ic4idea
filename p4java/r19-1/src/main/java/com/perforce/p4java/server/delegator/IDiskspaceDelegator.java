package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.admin.IDiskSpace;
import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface to handle the Diskspace command.
 */
public interface IDiskspaceDelegator {
    /**
     * Get a list of disk space information about the current availability of
     * disk space on the server. This command requires that the user be an
     * operator or have 'super' access granted by 'p4 protect'.
     * <p>
     *
     * If no arguments are specified, disk space information for all relevant
     * file systems is displayed; otherwise the output is restricted to the
     * named filesystem(s).
     * <p>
     *
     * filesystems: P4ROOT | P4JOURNAL | P4LOG | TEMP | journalPrefix | depot
     *
     * See the main 'p4 diskspace' command documentation for full semantics and
     * usage details.
     *
     * @param filesystems if not null, specify a list of Perforce named filesystem(s).
     * @return non-null but possibly empty list of disk space information.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2011.2
     */
    List<IDiskSpace> getDiskSpace(List<String> filesystems) throws P4JavaException;
}
