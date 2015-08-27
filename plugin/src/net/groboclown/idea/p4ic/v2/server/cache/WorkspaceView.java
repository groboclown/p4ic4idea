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

package net.groboclown.idea.p4ic.v2.server.cache;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.state.*;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4WorkspaceViewState.ViewMapping;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A representation of the server stored workspace view.  This class system doesn't support
 * changing the workspace view (must be done through the Perforce tools), but updates to
 * the view cause ripple effects in the rest of the system.
 * <p/>
 * All file spec mappings are handled through this class.
 * <p/>
 * This isn't a CacheFrontEnd object, because it is the thing that forces
 * a cache refresh.
 */
public class WorkspaceView {
    private static final Logger LOG = Logger.getInstance(WorkspaceView.class);

    // an invalid file path
    private static final FilePath NULL_FILE_PATH = FilePathUtil.getFilePath(".*.");

    private final Project project;
    private final Cache cache;
    private final FileMappingRepo fileRepo;
    private final P4WorkspaceViewState cachedServerWorkspace;
    private FilePath bestRoot;


    public WorkspaceView(@NotNull final Project project, @NotNull final Cache cache,
            @NotNull final FileMappingRepo fileRepo,
            @NotNull final P4WorkspaceViewState cachedServerWorkspace) {
        this.project = project;
        this.cache = cache;
        this.fileRepo = fileRepo;
        this.cachedServerWorkspace = cachedServerWorkspace;
    }

    public ServerQuery createWorkspaceRefreshQuery() {
        return new WorkspaceRefreshServerQuery();
    }

    /**
     * Translate the valued returned by "opened" into update states.  This only
     * handles the caching of the underlying P4ClientFileMapping, not any of the
     * server state.
     * <p/>
     * Must be run from within the {@link ServerConnection}
     *
     *
     * @param fileSpecs specs returned by "p4 opened"; non-valid specs are ignored
     *                  (they should be handled separately).
     * @return update states
     */
    @NotNull
    Collection<P4FileUpdateState> fromOpenedToAction(@NotNull final List<IFileSpec> fileSpecs,
            @NotNull AlertManager alerts) {
        ServerConnection.assertInServerConnection();

        // For an example of what is returned, try out:
        //    p4 -Ztag opened ...
        // ... depotFile //depot/projecta/main/app/build.gradle
        // ... clientFile //c1/app/build.gradle
        // ... rev 1
        // ... haveRev none
        // ... action add
        // ... change 65
        // ... type xtext
        // ... user user
        // ... client c1

        // Note that the command can also run:
        //  p4 -Ztag opened -s ...
        // ... depotFile //depot/projecta/main/app/build.gradle
        // ... action add
        // ... change 65
        // ... user user
        // ... client c1
        // This is slightly faster to run, but still contains the
        // information we want.

        List<P4FileUpdateState> ret = new ArrayList<P4FileUpdateState>(fileSpecs.size());
        for (IFileSpec spec : fileSpecs) {
            if (spec != null && P4StatusMessage.isValid(spec)) {
                String depotPath = spec.getDepotPathString();
                FilePath clientFilePath = clientSpecToFilePath(spec, alerts);
                if (depotPath == null || clientFilePath == null) {
                    LOG.error("callee did not remove invalid file specs");
                } else {
                    P4ClientFileMapping fileState = fileRepo.getByDepotLocation(depotPath, clientFilePath);
                    final FileUpdateAction action = FileUpdateAction.getFileUpdateAction(
                            UpdateAction.getUpdateActionForOpened(spec.getAction()));
                    if (action == null) {
                        alerts.addNotice(P4Bundle.message("error.spec.unknown-open-action", depotPath, spec.getAction()), null);
                    } else {
                        ret.add(new P4FileUpdateState(fileState, spec.getChangelistId(), action));
                    }
                }
            }
        }
        return ret;
    }


    @Nullable
    private FilePath clientSpecToFilePath(@NotNull IFileSpec spec, @NotNull AlertManager alerts) {
        // no need for synchronization here
        String clientPath = spec.getClientPathString();
        if (clientPath == null) {
            LOG.debug("File spec has no client path: " + spec);
            return null;
        }
        // Rare circumstances can have quotes surrounding the path.  It shouldn't, in general.
        if (clientPath.startsWith("\"") && clientPath.endsWith("\"")) {
            clientPath = clientPath.substring(1, clientPath.length() - 1);
        }
        String clientPathLower = clientPath.toLowerCase();
        String clientPrefix = "//" + getCachedClientName() + "/";
        if (! clientPathLower.startsWith(clientPrefix.toLowerCase())) {
            alerts.addWarning(P4Bundle.message("error.filespec.incorrect-client", clientPath, getCachedClientName()), null);
            return null;
        }
        final FilePath root = getBestRoot(alerts);
        if (root == null) {
            // already reported the problem
            return null;
        }
        String relClientPath = clientPath.substring(clientPrefix.length());
        if (root == NULL_FILE_PATH) {
            // the relative path IS the full path in this circumstance.
            return FilePathUtil.getFilePath(relClientPath);
        }
        File clientFile = new File(root.getIOFile(), relClientPath);
        return FilePathUtil.getFilePath(clientFile);
    }

    @NotNull
    private String getCachedClientName() {
        // no need for synchronization here.
        return cachedServerWorkspace.getName();
    }


    /**
     *
     * @param alerts alert manager, for dealing gracefully with errors.
     * @return the best fitting <em>workspace</em> root directory; which maps to the "//clientname/"
     *      pattern.
     */
    @Nullable
    private FilePath getBestRoot(@NotNull AlertManager alerts) {
        if (bestRoot == null) {
            final List<FilePath> roots;
            try {
                roots = cache.getClient().getFilePathRoots();
            } catch (P4InvalidConfigException e) {
                alerts.addWarning(P4Bundle.message("error.config.no-roots", cache.getClientName()), e);
                return null;
            }
            final List<String> workspaceRoots = cachedServerWorkspace.getRoots();
            for (String workspaceRoot : workspaceRoots) {
                // Special case for a workspace root that spans windows directories.
                if (workspaceRoot.equals("null")) {
                    bestRoot = NULL_FILE_PATH;
                    return NULL_FILE_PATH;
                }
                FilePath fp = FilePathUtil.getFilePath(workspaceRoot);
                if (fp.isDirectory() && fp.getVirtualFile() != null && fp.getVirtualFile().exists()) {
                    // Valid workspace root directory (can be on this workstation).
                    for (FilePath root : roots) {
                        if (root.isUnder(fp, false) || fp.isUnder(root, false)) {
                            bestRoot = root;
                            return root;
                        }
                    }
                }
            }
            // no root found.
            alerts.addWarning(P4Bundle.message("error.config.invalid-roots", cache.getClientName()), null);
            // keep best root null
        }
        return bestRoot;
    }

    public P4ClientFileMapping getClientMappingFor(final FilePath file) {
        ServerConnection.assertInServerConnection();
        return fileRepo.getByLocation(file);
    }


    class WorkspaceRefreshServerQuery implements ServerQuery {

        @Override
        public void query(@NotNull final P4Exec2 exec, @NotNull final P4Server server,
                @NotNull final ServerConnection connection,
                @NotNull final AlertManager alerts) {
            ServerConnection.assertInServerConnection();
            Project project = server.getProject();

            final IClient client;
            try {
                client = exec.getClient(project);
            } catch (VcsException e) {
                ErrorHandlers.handle(P4Bundle.message("action.getClient", getCachedClientName()), e, alerts);
                return;
            }
            if (! client.getName().equals(getCachedClientName())) {
                // FIXME critical error for wrong client
                throw new IllegalStateException("not implemented");
            }
            List<String> roots = new ArrayList<String>();
            roots.add(client.getRoot());
            for (String root : client.getAlternateRoots()) {
                roots.add(root);
            }

            boolean doRefresh = false;

            // the roots can be different; if the new roots are not a superset
            // of the existing roots, then we trigger a refresh.
            // Loop through the old roots, and compare to the list of new roots.
            // If a new root doesn't match the old root, assume it's inserted,
            // and keep searching through the new roots.  If we run out of new roots
            // before we hit the end of the old root list, then we know that the list
            // has changed.

            Iterator<String> newRootsIter = roots.iterator();
            oldRootLoop:
            for (String oldRoot: cachedServerWorkspace.getRoots()) {
                while (newRootsIter.hasNext()) {
                    String newRoot = newRootsIter.next();
                    if (newRoot.equals(oldRoot)) {
                        continue oldRootLoop;
                    }
                }
                // finished the new roots loop without a match to the current old root.
                // refresh!
                doRefresh = true;
                break;
            }
            cachedServerWorkspace.setRoots(roots);


            // The mappings need to match up exactly.
            // There are circumstances where this doesn't need to be the case
            // (new depot locations are added to the client that weren't there
            // originally), but that is troublesome to detect.

            final List<ViewMapping> oldMappings = cachedServerWorkspace.getViewMappings();
            final List<IClientViewMapping> newMappings = client.getClientView().getEntryList();
            if (oldMappings.size() != newMappings.size()) {
                doRefresh = true;
            } else {
                final Iterator<IClientViewMapping> newIter = newMappings.iterator();
                final Iterator<ViewMapping> oldIter = oldMappings.iterator();
                while (newIter.hasNext() && oldIter.hasNext()) {
                    final IClientViewMapping newM = newIter.next();
                    final ViewMapping oldM = oldIter.next();
                    if (! newM.getDepotSpec(false).equals(oldM.getDepot()) ||
                            ! newM.getClient(false).equals(newM.getClient())) {
                        doRefresh = true;
                        break;
                    }
                }
            }
            cachedServerWorkspace.setViewMappings(newMappings);

            if (doRefresh) {
                alerts.addWarning(P4Bundle.message("warning.client.updated", getCachedClientName()), null);
                cache.refreshServerState();
            }
        }
    }





}
