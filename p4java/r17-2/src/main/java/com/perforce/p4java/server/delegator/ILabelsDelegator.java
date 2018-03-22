package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetLabelsOptions;

/**
 * Interface to handle the Labels command.
 */
public interface ILabelsDelegator {
    /**
     * Get a list of Perforce labels, optionally tied to a specific set of
     * files.
     * <p>
     * <p>
     * Note that the ILabel objects returned here do not have views associated
     * with them (i.e. the getViewMapping() method will return an empty list. If
     * you need to get the view mapping for a specific label, use the getLabel()
     * method.
     *
     * @param user       if non-null, limit labels to those owned by the named user
     * @param maxLabels  if larger than zero, return only the first maxLabels (or
     *                   fewer) qualifying labels
     * @param nameFilter if not null, limits output to labels whose name matches the
     *                   nameFilter pattern, e.g. -e 'svr-dev-rel*'
     * @param fileList   if not null, limits its report to labels that contain those
     *                   files
     * @return non-null (but possibly-empty) list of qualifying Perforce labels
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    List<ILabelSummary> getLabels(
            String user,
            int maxLabels,
            String nameFilter,
            List<IFileSpec> fileList) throws ConnectionException, RequestException, AccessException;

    /**
     * Get a list of Perforce labels, optionally tied to a specific set of files.<p>
     * <p>
     * Note that the ILabel objects returned here do not have views associated with
     * them (i.e. the getViewMapping() method will return an empty list. If you need
     * to get the view mapping for a specific label, use the getLabel() method.
     *
     * @param fileList if not null, limits its report to labels that contain those files
     * @param opts     GetLabelsOptions object describing optional parameters; if null, no options are
     *                 set.
     * @return non-null (but possibly-empty) list of qualifying Perforce labels
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    List<ILabelSummary> getLabels(
            List<IFileSpec> fileList,
            GetLabelsOptions opts) throws P4JavaException;
}
