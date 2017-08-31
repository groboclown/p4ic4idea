package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetProtectionEntriesOptions;

/**
 * Interface to handle the Protects command.
 */
public interface IProtectsDelegator {
    /**
     * Get a list of Perforce protection entries for the passed-in arguments.
     * <p>
     * <p>
     * Note that the behavior of this method is unspecified when using clashing
     * options (e.g. having both userName and groupName set non-null). Consult
     * the main Perforce admin documentation for semantics and usage.
     * <p>
     * <p>
     * Note that the annotations in the file paths will be dropped. The reason
     * is the Perforce server 'protects' command requires a file list devoid of
     * annotated revision specificity.
     *
     * @param allUsers  if true, protection lines for all users are displayed.
     * @param hostName  only protection entries that apply to the given host (IP
     *                  address) are displayed.
     * @param userName  protection lines Perforce user "userName" are displayed.
     * @param groupName protection lines for Perforce group "groupName" are displayed.
     * @param fileList  if non-null, only those protection entries that apply to the
     *                  specified files are displayed.
     * @return non-null but possibly empty list of protection entries.
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request.
     * @throws AccessException     if the Perforce server denies access to the caller.
     */
    List<IProtectionEntry> getProtectionEntries(
            boolean allUsers,
            String hostName,
            String userName,
            String groupName,
            List<IFileSpec> fileList)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Get a list of Perforce protection entries for the passed-in arguments.<p>
     * <p>
     * Note that the behavior of this method is unspecified when using clashing
     * options (e.g. having both userName and groupName set non-null). Consult the
     * main Perforce admin documentation for semantics and usage.<p>
     * <p>
     * Note that any annotations in the file paths will be ignored. The reason is
     * the Perforce server 'protects' command requires a file list devoid of annotated
     * revision specificity.
     *
     * @param fileList if non-null, only those protection entries that apply to the specified files
     *                 are displayed.
     * @param opts     GetProtectionEntriesOptions object describing optional parameters; if null, no
     *                 options are set.
     * @return non-null but possibly empty list of protection entries.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    List<IProtectionEntry> getProtectionEntries(
            List<IFileSpec> fileList,
            GetProtectionEntriesOptions opts) throws P4JavaException;
}
