/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.groboclown.p4.server.impl.connection.operations;

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.impl.connection.impl.MessageStatusUtil;
import net.groboclown.p4.server.impl.connection.impl.OpenFileStatus;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * Move File is really complex, so it gets its own class.
 */
public class MoveFile {
    private static final Logger LOG = Logger.getInstance(MoveFile.class);

    public static MoveFile INSTANCE = new MoveFile();

    private P4CommandUtil cmd;

    private MoveFile() {
        // do nothing
    }

    public void withCmd(@NotNull P4CommandUtil cmd) {
        this.cmd = cmd;
    }

    public File getExecDir(P4CommandRunner.ClientAction<?> action) {
        return ((MoveFileAction) action).getTargetFile().getIOFile().getParentFile();
    }

    public MoveFileResult moveFile(IClient client, ClientConfig config, P4CommandRunner.ClientAction<?> baseType)
            throws Exception {
        assert cmd != null;
        MoveFileAction action = (MoveFileAction) baseType;

        List<IFileSpec> srcFile = FileSpecBuildUtil.escapedForFilePaths(action.getSourceFile());
        List<IFileSpec> tgtFile = FileSpecBuildUtil.escapedForFilePaths(action.getTargetFile());
        if (srcFile.size() != 1 || tgtFile.size() != 1) {
            throw new IllegalStateException("Must have 1 source and 1 target, have " + srcFile + "; " + tgtFile);
        }
        LOG.info("Running move command for `" + srcFile + "` to `" + tgtFile + "`");
        // Note the two separate fstat calls.  These are limited, and are okay, but it might
        // be better to join them together into a single call.
        List<IExtendedFileSpec> srcStatusResponse = cmd.getFileDetailsForOpenedSpecs(client.getServer(), srcFile, 1);
        OpenFileStatus srcStatus = new OpenFileStatus(srcStatusResponse);
        OpenFileStatus tgtStatus =
                new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(), tgtFile, 1));
        if (srcStatus.hasAdd()) {
            // source is open for add.  Revert it and mark the target as open for edit/add.
            LOG.debug("Source file is open for add.  Reverting add, and will just open for edit or add the target.");
            List<IFileSpec> reverted = cmd.revertFiles(client, srcFile, false);
            MessageStatusUtil.throwIfError(reverted);
            // TODO bundle message for separator
            if (tgtStatus.hasDelete()) {
                // Currently marked as deleted, so it exists on the server.  Revert the delete then edit it.
                LOG.debug("Target file open for delete.  Reverting that and just opening it for edit.");
                reverted = cmd.revertFiles(client, tgtFile, false);
                MessageStatusUtil.throwIfError(reverted);
                List<IFileSpec> edited = cmd.editFiles(client, tgtFile, null, action.getChangelistId(), null);
                MessageStatusUtil.throwIfError(edited);
                return new MoveFileResult(config, MessageStatusUtil.getMessages(edited, "\n"));
            } else if (tgtStatus.isNotOnServer()) {
                // Target not on server
                LOG.debug("Target file not known by server.  Opening for add.");
                List<IFileSpec> added = cmd.addFiles(client, tgtFile, null, action.getChangelistId(), null);
                MessageStatusUtil.throwIfError(added);
                return new MoveFileResult(config, MessageStatusUtil.getMessages(added, "\n"));
            } else if (!tgtStatus.hasOpen()) {
                // On server and not open
                LOG.debug("Target file not open.  Opening for edit.");
                List<IFileSpec> edited = cmd.editFiles(client, tgtFile, null, action.getChangelistId(), null);
                MessageStatusUtil.throwIfError(edited);
                return new MoveFileResult(config, MessageStatusUtil.getMessages(edited, "\n"));
            } else {
                // Nothing to do
                LOG.debug("Target file already open for edit or add.  Skipping.");
                // TODO bundle message
                return new MoveFileResult(config, "Already open");
            }
        } else if (srcStatus.hasDelete()) {
            // source is already open for delete.  Revert it and continue with normal move.
            LOG.debug("Source is open for delete.  Reverting delete to allow move operation to do it right.");
            List<IFileSpec> reverted = cmd.revertFiles(client, srcFile, false);
            MessageStatusUtil.throwIfError(reverted);
        } else if (srcStatus.isNotOnServer()) {
            // The source is not on the server, so it's an add or edit.
            if (tgtStatus.hasAddEdit() || tgtStatus.hasAdd()) {
                // Do nothing
                LOG.debug("Source not on server, and target already open for add or edit.  Skipping.");
                // TODO bundle message
                return new MoveFileResult(config, "Nothing to do");
            }
            if (tgtStatus.hasOpen()) {
                // Should be delete; add and edit is already handled above.
                LOG.debug("Source not on server, and target already open (for " + tgtStatus +
                        ").  Reverting target operation.");
                List<IFileSpec> reverted = cmd.revertFiles(client, tgtFile, false);
                MessageStatusUtil.throwIfError(reverted);
            } else if (tgtStatus.isNotOnServer()) {
                LOG.debug("Source and target not on server.  Opening target for add.");
                List<IFileSpec> added = cmd.addFiles(client, tgtFile, null, action.getChangelistId(), null);
                MessageStatusUtil.throwIfError(added);
                return new MoveFileResult(config, MessageStatusUtil.getMessages(added, "\n"));
            }
            LOG.debug("Source not on server, target not open.  Opening target for edit.");
            List<IFileSpec> edited = cmd.editFiles(client, tgtFile, null, action.getChangelistId(), null);
            MessageStatusUtil.throwIfError(edited);
            return new MoveFileResult(config, MessageStatusUtil.getMessages(edited, "\n"));
        } else if (!srcStatus.hasOpen()) {
            LOG.debug("Source not open.  Move requires the source to be open for edit.");
            List<IFileSpec> edited = cmd.editFiles(client, srcFile, null, action.getChangelistId(), null);
            MessageStatusUtil.throwIfError(edited);
        } else {
            LOG.debug("Source file is already open for edit");
        }

        // Check target status, to see what we need to do there.
        if (tgtStatus.hasAdd()) {
            // If the file is open for add, then it should be reverted and a normal move happens.
            LOG.debug("Target file open for add.  Reverting before performing move.");
            List<IFileSpec> reverted = cmd.revertFiles(client, tgtFile, false);
            MessageStatusUtil.throwIfError(reverted);
        } else if (! tgtStatus.isNotOnServer()) {
            // See #181
            if (tgtStatus.hasOpen()) {
                // The file is open for delete or edit, then it should be reverted, and fall through.
                LOG.debug("Target file open for edit.  Reverting before performing move.");
                List<IFileSpec> reverted = cmd.revertFiles(client, tgtFile, false);
                MessageStatusUtil.throwIfError(reverted);
            }
            //  The file is on the server but not open, then this should be a manual integrate + source delete.
            List<IFileSpec> results = cmd.integrateFileTo(client, srcFile.get(0), tgtFile.get(0), action.getChangelistId());
            MessageStatusUtil.throwIfError(results);
            results.addAll(cmd.deleteFiles(client, srcFile, action.getChangelistId()));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Move as a integrate+delete messages: " + MessageStatusUtil.getMessages(results, "; "));
            }
            return new MoveFileResult(config, MessageStatusUtil.getMessages(results, "\n"));
        }

        // Standard move operation.
        LOG.debug("Performing move operation");
        List<IFileSpec> results = cmd.moveFile(client, srcFile.get(0), tgtFile.get(0), action.getChangelistId());
        MessageStatusUtil.throwIfError(results);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Move messages: " + MessageStatusUtil.getMessages(results, "; "));
        }
        return new MoveFileResult(config, MessageStatusUtil.getMessages(results, "\n"));
    }
}
