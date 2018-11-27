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
import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.util.FileTreeUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4RemoteFileImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SubmitChangelist {
    private static final Logger LOG = Logger.getInstance(SubmitChangelist.class);

    public static SubmitChangelist INSTANCE = new SubmitChangelist();

    private P4CommandUtil cmd;

    private SubmitChangelist() {
        // do nothing
    }

    public void withCmd(@NotNull P4CommandUtil cmd) {
        this.cmd = cmd;
    }

    // Submit needs a directory for AltRoot purposes, for when the
    // code is updated to shuffle non-project files out of the changelist.
    public File getExecDir(P4CommandRunner.ClientAction<?> baseType) {
        SubmitChangelistAction action = (SubmitChangelistAction) baseType;
        // Get most common root path.
        FilePath root = FileTreeUtil.getCommonRoot(action.getFiles());
        if (root == null) {
            return null;
        }
        return root.getIOFile();
    }

    public SubmitChangelistResult submitChangelist(IClient client, ClientConfig config,
            P4CommandRunner.ClientAction<?> baseType)
            throws P4JavaException {
        SubmitChangelistAction action = (SubmitChangelistAction) baseType;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running submit against the server for " + action.getChangelistId());
        }

        IChangelist change;
        if (action.getChangelistId().getState() == P4ChangelistId.State.PENDING_CREATION) {
            change = cmd.createChangelist(client, action.getUpdatedDescription());
        } else if (action.getChangelistId().isDefaultChangelist()) {
            // See #176.  Because we're changing the list of files to submit, we must create
            // a new changelist to put those files into.  It's just how Perforce works.
            change = cmd.createChangelist(client, action.getUpdatedDescription());
        } else {
            change = cmd.getChangelistDetails(client.getServer(), action.getChangelistId().getChangelistId());
        }
        if (change == null) {
            throw new P4JavaException("No such pending change on server: " + action.getChangelistId());
        } else if (change.getStatus() == ChangelistStatus.SUBMITTED) {
            throw new P4JavaException("Change " + change.getId() + " already submitted");
        }
        if (action.getUpdatedDescription() != null && !action.getUpdatedDescription().isEmpty()) {
            change.setDescription(action.getUpdatedDescription());
        } else if (change.getDescription() == null) {
            throw new P4JavaException("Must include a description for new changelists");
        }


        if (LOG.isDebugEnabled()) {
            LOG.debug("Submitting changelist " + action.getChangelistId());
        }
        List<IFileSpec> res = cmd.submitChangelist(client,
                action.getJobStatus(), action.getUpdatedJobs(), change, action.getFiles());


        List<P4RemoteFile> submitted = new ArrayList<>(res.size());
        IServerMessage info = null;
        for (IFileSpec spec : res) {
            IServerMessage msg = spec.getStatusMessage();
            if (msg != null) {
                if (msg.isInfo()) {
                    info = msg;
                }
                if (msg.isWarning() || msg.isError()) {
                    throw new RequestException(msg);
                }
            } else {
                submitted.add(new P4RemoteFileImpl(spec));
            }
        }

        // Submitting a change successfully requires that the corresponding changelist is deleted in the cache.

        return new SubmitChangelistResult(config, new P4ChangelistIdImpl(change.getId(), config.getClientServerRef()),
                submitted, info == null ? null : info.getLocalizedMessage());
    }
}
