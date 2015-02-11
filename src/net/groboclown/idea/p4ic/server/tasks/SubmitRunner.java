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
package net.groboclown.idea.p4ic.server.tasks;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.CmdSpec;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CancellationException;

public class SubmitRunner extends ServerTask<List<P4StatusMessage>> {
    @NotNull
    private final Project project;

    @Nullable
    private final List<FilePath> actualFiles;

    @NotNull
    private final Collection<String> jobIds;

    private final int changelistId;

    public SubmitRunner(
            @NotNull Project project, @Nullable List<FilePath> actualFiles,
            @Nullable Collection<String> jobIds,
            int changelistId) {
        this.project = project;
        this.actualFiles = actualFiles;
        if (jobIds == null) {
            jobIds = Collections.emptyList();
        }
        this.jobIds = jobIds;
        this.changelistId = changelistId;
        assert changelistId > 0;
    }

    @Override
    public List<P4StatusMessage> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        IChangelist changelist = exec.getChangelist(project, changelistId);
        if (changelist == null) {
            throw new P4Exception("changelist does not exist: " + changelistId);
        }
        //if (! changelist.getUsername().equals(exec.getOwnerName())) {
        //    throw new P4Exception("changelist not owned by client");
        //}
        if (changelist.getStatus() == ChangelistStatus.SUBMITTED) {
            throw new P4Exception("changelist is already submitted");
        }
        // Should do host check, too.

        throw new P4Exception("submit not supported at the moment (it crashes the server)");

        /*
        if (actualFiles != null) {
            // FIXME manipulate the changelist such that only these
            // files are in the change.  For numbered change lists,
            // this means moving all other files into the default
            // changelist.  For the default changelist, this means
            // creating a new changelist and moving these
            // files into it.

            List<P4FileInfo> files = P4FileInfo.loadP4FileInfo(actualFiles, client);

            // Add the missing files to the changelist
            List<IFileSpec> toMoveFiles = new ArrayList<IFileSpec>();
            for (P4FileInfo file : files) {
                if (file.getChangelist() != changelistId) {
                    toMoveFiles.add(file.toDepotSpec());
                }
            }
            if (! toMoveFiles.isEmpty()) {
                List<P4StatusMessage> errors = getErrors(
                        client.reopenFiles(toMoveFiles, changelistId, null));
                if (!errors.isEmpty()) {
                    return errors;
                }
            }

            // Remove extra files from the changelist.  If we modified
            // the list of files above, then we need to reload the file list.
            Iterator<IFileSpec> iter = changelist.getFiles(
                    !toMoveFiles.isEmpty()).iterator();
            changelistLoop:
            while (iter.hasNext()) {
                IFileSpec next = iter.next();
                for (P4FileInfo file: files) {
                    if (file.getDepotPath().equals(next.getDepotPathString())) {
                        continue changelistLoop;
                    }
                }

                // wasn't found in the desired list, so remove it.
                iter.remove();
            }
        }

        // Submit
        // FIXME make the "reopen" configurable
        // FIXME make the job status configurable
        Server realServer;
        if (server instanceof P4ServerProxy) {
            realServer = (Server) ((P4ServerProxy) server).getRealServer();
        } else {
            realServer = (Server) server;
        }
        //changelist.setServer(realServer);
        //List<P4StatusMessage> errors = getErrors(changelist.submit(false,
        //        new ArrayList<String>(jobIds), null));
        List<P4StatusMessage> errors = getErrors(submit(realServer,
                changelist.getId(), null));
        if (! errors.isEmpty()) {
            System.err.println("Encountered submit problems: " + errors);
        } else {
            P4Vcs.getInstance(project).getChangeListMapping().removePerforceMapping(changelistId);
        }
        return errors;
        */
    }


    private static List<IFileSpec> submit(@NotNull Server server, int changelistId, @Nullable String jobStatus) throws P4JavaException {
        if (changelistId <= 0) {
            throw new RequestException("Can only submit a real changelist, not the default one");
        }

        String[] args;
        if (jobStatus != null) {
            args = new String[] { "-c" + changelistId, "-s" + jobStatus };
        } else {
            args = new String[] { "-c" + changelistId };
        }

        List retMaps = server.execMapCmdList(CmdSpec.SUBMIT,
                Parameters.processParameters((Options) null, (List<IFileSpec>) null, args, server),
                (Map<String, Object>) null);
        ArrayList fileList = new ArrayList();
        if (retMaps != null) {
            Iterator i$ = retMaps.iterator();

            while (i$.hasNext()) {
                Map map = (Map) i$.next();
                if (map.get("submittedChange") != null) {
                    int id = (new Integer((String) map.get("submittedChange"))).intValue();
                    ChangelistStatus status = ChangelistStatus.SUBMITTED;
                    fileList.add(new FileSpec(FileSpecOpStatus.INFO, "Submitted as change " + id));
                } else if (map.get("locked") == null) {
                    fileList.add(server.handleFileReturn(map));
                }
            }
        }

        return fileList;
    }
}
