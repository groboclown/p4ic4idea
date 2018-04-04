package com.perforce.p4java.server.delegator;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;

/**
 * Interface for FileLogDelegator implementations.
 */
public interface IFileLogDelegator {
    /**
     * Get the revision history data for one or more Perforce files.
     *
     * @param fileSpecs           fileSpecs to be processed; if null or empty, an empty Map is
     *                            returned.
     * @param maxRevs             If positive, displays at most 'maxRevs' revisions per file of the
     *                            file[rev] argument specified. Corresponds to -m.
     * @param contentHistory      If true, display file content history instead of file name
     *                            history. Corresponds to -h.
     * @param includeInherited    If true, causes inherited file history to be displayed as well.
     *                            Corresponds to -i.
     * @param longOutput          If true, produces long output with the full text of the changelist
     *                            descriptions. Corresponds to -l.
     * @param truncatedLongOutput If true, produces long output with the full text of the changelist
     *                            descriptions truncated to 250 characters. Corresponds to -L.
     */
    Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(
            List<IFileSpec> fileSpecs,
            int maxRevs,
            boolean contentHistory,
            boolean includeInherited,
            boolean longOutput,
            boolean truncatedLongOutput) throws ConnectionException, AccessException;

    /**
     * Get the revision history data for one or more Perforce files.
     * <p>
     *
     * @param fileSpecs fileSpecs to be processed; if null or empty, an empty Map is returned.
     * @param opts      GetRevisionHistoryOptions object describing optional parameters; if null, no
     *                  options are set.
     * @return a non-null map of lists of revision data for qualifying files; the map is keyed by
     * the IFileSpec of the associated file, meaning that errors are signaled using the normal
     * IFileSpec getOpStatus() method.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(
            List<IFileSpec> fileSpecs,
            GetRevisionHistoryOptions opts) throws P4JavaException;
}
