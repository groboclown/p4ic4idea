package com.perforce.p4java.server.delegator;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.MoveFileOptions;

/**
 * Interface to handle the Move command.
 */
public interface IMoveDelegator {
    /**
     * Move a file already opened for edit or add (the fromFile) to the
     * destination file (the toFile). A file can be moved many times before it
     * is submitted; moving it back to its original location will reopen it for
     * edit. The full semantics of this operation (which can be confusing) are
     * found in the main 'p4 help' documentation.
     * <p>
     * <p>
     * Note that this operation is not supported on servers earlier than 2009.1;
     * any attempt to use this on earlier servers will result in a
     * RequestException with a suitable message. Similarly, not all underlying
     * IServer implementations will work with this either, and will also result
     * in a suitable RequestException.
     * <p>
     * <p>
     * Note also that the move command is special in that almost alone among
     * Perforce file-based commands, it does not allow full filespecs with
     * version specifiers; these are currently quietly stripped off in the move
     * command implementation here, which may lead to unexpected behaviour if
     * you pass in specific versions expecting them to be honoured.
     *
     * @param changelistId if not IChangelist.UNKNOWN, the files are opened in the
     *                     numbered pending changelist instead of the 'default'
     *                     changelist.
     * @param listOnly     if true, don't actually perform the move, just return what
     *                     would happen if the move was performed
     * @param noClientMove if true, bypasses the client file rename. This option can be
     *                     used to tell the server that the user has already renamed a
     *                     file on the client. The use of this option can confuse the
     *                     server if you are wrong about the client's contents. Only
     *                     works for 2009.2 and later servers; earlier servers will
     *                     produce a RequestException if you set this true.
     * @param fileType     if not null, the file is reopened as that filetype.
     * @param fromFile     the original file; must be already open for edit.
     * @param toFile       the target file.
     * @return list of IFileSpec objects representing the results of this move
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    List<IFileSpec> moveFile(
            int changelistId,
            boolean listOnly,
            boolean noClientMove,
            String fileType,
            @Nonnull IFileSpec fromFile,
            @Nonnull IFileSpec toFile)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Move a file already opened for edit or add (the fromFile) to the
     * destination file (the toFile). A file can be moved many times before it
     * is submitted; moving it back to its original location will reopen it for
     * edit. The full semantics of this operation (which can be confusing) are
     * found in the main 'p4 help' documentation.
     * <p>
     * <p>
     * Note that this operation is not supported on servers earlier than 2009.1;
     * any attempt to use this on earlier servers will result in a
     * RequestException with a suitable message.
     * <p>
     * <p>
     * Note also that the move command is special in that almost alone among
     * Perforce file-based commands, it does not allow full filespecs with
     * version specifiers; these are currently quietly stripped off in the move
     * command implementation here, which may lead to unexpected behaviour if
     * you pass in specific versions expecting them to be honoured.
     *
     * @param fromFile the original file; must be already open for edit.
     * @param toFile   the target file.
     * @param opts     MoveFileOptions object describing optional parameters; if
     *                 null, no options are set.
     * @return list of IFileSpec objects representing the results of this move.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    List<IFileSpec> moveFile(
            @Nonnull IFileSpec fromFile,
            @Nonnull IFileSpec toFile,
            @Nullable MoveFileOptions opts) throws P4JavaException;
}
