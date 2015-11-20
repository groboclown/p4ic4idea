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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateGroup;
import net.groboclown.idea.p4ic.v2.server.cache.state.*;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4WorkspaceViewState.ViewMapping;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import net.groboclown.idea.p4ic.v2.ui.alerts.ClientNameMismatchHandler;
import net.groboclown.idea.p4ic.v2.ui.alerts.InvalidClientHandler;
import net.groboclown.idea.p4ic.v2.ui.alerts.InvalidRootsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

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
public class WorkspaceServerCacheSync extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(WorkspaceServerCacheSync.class);

    private final Cache cache;
    private final FileMappingRepo fileRepo;
    private final P4WorkspaceViewState cachedServerWorkspace;

    // per-instance exception to reference the root directory.  It
    // is reset whenever the workspace is reloaded.  It is necessary
    // to be reused so that the user doesn't keep seeing the same error.
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    @NotNull
    private VcsException invalidRootsException = new VcsException("no valid roots");


    public WorkspaceServerCacheSync(@NotNull final Cache cache,
            @NotNull final FileMappingRepo fileRepo,
            @NotNull final P4WorkspaceViewState cachedServerWorkspace) {
        this.cache = cache;
        this.fileRepo = fileRepo;
        this.cachedServerWorkspace = cachedServerWorkspace;
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
    public Collection<P4FileUpdateState> fromOpenedToAction(
            @NotNull final Project project,
            @NotNull final List<IExtendedFileSpec> fileSpecs,
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
                FilePath clientFilePath = clientSpecToFilePath(project, spec, alerts);
                if (depotPath == null || clientFilePath == null) {
                    LOG.error("callee did not remove invalid file specs: " + spec +
                            ": depot " + depotPath + ", client: " + clientFilePath);
                } else {
                    P4ClientFileMapping fileState = fileRepo.getByDepotLocation(depotPath, clientFilePath);
                    final FileUpdateAction action = FileUpdateAction.getFileUpdateAction(
                            UpdateAction.getUpdateActionForOpened(spec.getAction()));
                    if (action == null) {
                        alerts.addNotice(project, P4Bundle.message("error.spec.unknown-open-action", depotPath, spec.getAction()), null);
                    } else {
                        final P4FileUpdateState state =
                                new P4FileUpdateState(fileState, spec.getChangelistId(), action, true);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Mapped " + spec + " to " + state + " with action " + action +
                                " from " + spec.getAction());
                        }
                        ret.add(state);
                    }
                }

                // Check for move source file
                // FIXME debug
                /*
                LOG.info("depot path: " + depotPath);
                LOG.info("from path: " + spec.getFromFile());
                LOG.info("base path: " + spec.getBaseFile());
                String fromPath = spec.getFromFile();
                if (fromPath != null) {

                    FilePath clientFromPath = clientSpecToFilePath(project,
                            FileSpecUtil.getFromDepotPath(fromPath, -1));
                }
                */

            }
        }
        return ret;
    }


    @Nullable
    private FilePath clientSpecToFilePath(@NotNull Project project, @NotNull IFileSpec spec, @NotNull AlertManager alerts) {
        // no need for synchronization here
        String clientPath = spec.getClientPathString();
        if (clientPath == null) {
            LOG.error("File spec has no client path: " + spec);
            return null;
        }
        // Rare circumstances can have quotes surrounding the path.  It shouldn't, in general.
        if (clientPath.startsWith("\"") && clientPath.endsWith("\"")) {
            clientPath = clientPath.substring(1, clientPath.length() - 1);
        }
        String clientPathLower = clientPath.toLowerCase();
        String clientPrefix = "//" + getCachedClientName() + "/";
        if (! clientPathLower.startsWith(clientPrefix.toLowerCase())) {
            // assume it's the actual path to the file system.
            FilePath ret = FilePathUtil.getFilePath(clientPath);
            if (LOG.isDebugEnabled()) {
                LOG.debug(" - converted " + spec + " to file " + ret);
            }
            return ret;
        }
        String relClientPath = clientPath.substring(clientPrefix.length());
        final List<String> workspaceRoots = cachedServerWorkspace.getRoots();
        if (workspaceRoots.isEmpty()) {
            invalidRootsException.fillInStackTrace();
            alerts.addCriticalError(new InvalidRootsHandler(project,
                    cache.getClientServerId(), invalidRootsException), invalidRootsException);
            return null;
        }
        for (String workspaceRoot : workspaceRoots) {
            // Special case for a workspace root that spans windows directories.
            if (workspaceRoot.equals("null")) {
                // Use the path conversion as spelled out in
                // http://www.perforce.com/perforce/r12.1/manuals/cmdref/client.html
                // e.g. Root:   null
                //      View:
                //          //depot/rel1.0/...   //eds_win/d:/old/rel1.0/...
                // Which means the relClientSpec is already the file path.
                return FilePathUtil.getFilePath(relClientPath);
            }

            // According to the P4 "client" documentation, if you're on a Windows computer, the
            // Root must be the Windows path.  We explicitly setup the workspaceRoots list to
            // have the Root first, and the AltRoot values second.
            if (SystemInfo.isWindows) {
                return FilePathUtil.getFilePath(new File(new File(workspaceRoot), relClientPath));
            }

            // This is a big leap of faith; assume the root directory exists, and if it does,
            // use that one.

            File root = new File(workspaceRoot);
            if (root.exists()) {
                File clientFile = new File(root, relClientPath);
                return FilePathUtil.getFilePath(clientFile);
            }
        }

        // None of the workspace root directories exist.  So, we'll assume that
        // the last one in the list is correct.

        File root = new File(workspaceRoots.get(workspaceRoots.size() - 1));
        alerts.addNotice(project,
                P4Bundle.message("client.root.non-existent", root), null,
                FilePathUtil.getFilePath(root));

        File clientFile = new File(root, relClientPath);
        return FilePathUtil.getFilePath(clientFile);
    }

    @NotNull
    private String getCachedClientName() {
        // no need for synchronization here.
        return cachedServerWorkspace.getName();
    }

    public P4ClientFileMapping getClientMappingFor(final FilePath file) {
        return fileRepo.getByLocation(file);
    }

    @NotNull
    List<VirtualFile> getClientRoots(@NotNull Project project, @NotNull AlertManager alerts) {
        if (project.isDisposed()) {
            LOG.info("Called getClientRoots() on a disposed project");
            return Collections.emptyList();
        }
        final List<VirtualFile> projectRoots = P4Vcs.getInstance(project).getVcsRoots();
        final List<String> workspaceRoots = cachedServerWorkspace.getRoots();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding client roots for " + cachedServerWorkspace.getName());
            LOG.debug(" - project roots: " + projectRoots);
            LOG.debug(" - workspace roots: " + workspaceRoots);
        }
        for (String workspaceRoot: workspaceRoots) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(" - root: " + workspaceRoot);
            }
            // Special case for a workspace root that spans windows directories.
            if (workspaceRoot.equals("null")) {
                // "null" mapping only matters if we're on Windows.  Otherwise, ignore it.
                if (SystemInfo.isWindows) {
                    // It should check the mappings to see which ones are under the project roots.
                    return findClientMappingsUnder(projectRoots);
                }
                continue;
            }
            final List<VirtualFile> ret = new ArrayList<VirtualFile>();
            final FilePath fp = FilePathUtil.getFilePath(workspaceRoot);
            if (fp.isDirectory() && fp.getVirtualFile() != null && fp.getVirtualFile().exists()) {
                // Valid workspace root directory (can be on this workstation).
                for (VirtualFile rootVf : projectRoots) {
                    FilePath rootFp = FilePathUtil.getFilePath(rootVf);
                    if (rootFp.isUnder(fp, false)) {
                        ret.add(rootVf);
                        // keep searching the project roots
                    } else if (fp.isUnder(rootFp, false)) {
                        ret.add(fp.getVirtualFile());
                        // matched the workspace root, so use that.
                        break;
                    }

                }
            }
            if (! ret.isEmpty()) {
                return ret;
            }
        }
        // no root found.
        invalidRootsException.fillInStackTrace();
        alerts.addCriticalError(new InvalidRootsHandler(project,
                cache.getClientServerId(), invalidRootsException), invalidRootsException);
        return Collections.emptyList();
    }

    void updateDepotPathFor(@NotNull P4ClientFileMapping mapping, @NotNull String depotPathString) {
        fileRepo.updateDepotPath(mapping, depotPathString);
    }

    @NotNull
    private List<VirtualFile> findClientMappingsUnder(@NotNull List<VirtualFile> projectRoots) {
        // Called when the root is Null.
        // Look for simple mappings - ones that contain "<anything>/...<anything>", then
        // match up to the "/" in the "/...".
        // If there are no matching, then give up and just return the project roots.

        List<VirtualFile> ret = new ArrayList<VirtualFile>();

        for (ViewMapping mapping: cachedServerWorkspace.getViewMappings()) {
            File matchedDir = getSimpleMatchDirectory(mapping.getClient());
            if (matchedDir != null) {
                FilePath base = FilePathUtil.getFilePath(matchedDir);
                for (VirtualFile vf: projectRoots) {
                    FilePath projectFp = FilePathUtil.getFilePath(vf);
                    if (base.isUnder(projectFp, false)) {
                        ret.add(base.getVirtualFile());
                        // all of this mapping is matched, so skip further project root
                        // checking.
                        break;
                    } else if (projectFp.isUnder(base, false)) {
                        ret.add(vf);
                    }
                }
            }
        }
        return ret;
    }


    @Nullable
    static File getSimpleMatchDirectory(@NotNull String clientSpec) {
        // If the mapping matches "//clientname/stuff1/...stuff2", where stuff1 is any
        // string that isn't "..." and doesn't have "*" or "...", then it's a match on
        // stuff1.
        // Note that this needs to be unescaped when returned.  Also, this expects to
        // be part of a "null" spec, so "stuff1" can't be empty.
        if (! clientSpec.startsWith("//")) {
            // invalid spec
            return null;
        }
        int thirdSlashPos = clientSpec.indexOf('/', 2);
        if (thirdSlashPos < 0) {
            // invalid spec
            return null;
        }
        int slashdotPos = clientSpec.indexOf("/...", thirdSlashPos + 1);
        if (slashdotPos < 0) {
            return null;
        }
        String interestingStuff = clientSpec.substring(thirdSlashPos + 1, slashdotPos);
        if (interestingStuff.indexOf('*') >= 0 || interestingStuff.contains("...")) {
            // invalid match
            return null;
        }
        String unescaped = FileSpecUtil.unescapeP4Path(interestingStuff);
        return new File(unescaped);
    }

    @Override
    protected void innerLoadServerCache(@NotNull final P4Exec2 exec, @NotNull final AlertManager alerts) {
        final IClient client;
        try {
            client = exec.getClient();
        } catch (VcsException e) {
            alerts.addCriticalError(
                    new InvalidClientHandler(exec.getProject(), getCachedClientName(),
                            exec.getServerConnectedController(), e), e);
            return;
        }
        if (!client.getName().equals(getCachedClientName())) {
            alerts.addCriticalError(new ClientNameMismatchHandler(exec.getProject(),
                    client.getName(),
                    getCachedClientName(),
                    exec.getServerConnectedController()),
                    null);
        }
        List<String> roots = new ArrayList<String>();
        if (client.getRoot() == null) {
            alerts.addNotice(exec.getProject(),
                    P4Bundle.message("error.primary.client.root.null"),
                    null);
        } else {
            roots.add(client.getRoot());
        }
       if (client.getAlternateRoots() != null) {
            for (String root : client.getAlternateRoots()) {
                if (root != null) {
                    roots.add(root);
                } else {
                    LOG.info("null alt root in " + client.getName());
                }
            }
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
        for (String oldRoot : cachedServerWorkspace.getRoots()) {
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
                if (!newM.getDepotSpec(false).equals(oldM.getDepot()) ||
                        !newM.getClient(false).equals(newM.getClient())) {
                    doRefresh = true;
                    break;
                }
            }
        }
        cachedServerWorkspace.setViewMappings(newMappings);

        // refreshed the root directories, so the exception can be different.
        invalidRootsException = new VcsException("no valid roots");

        if (doRefresh) {
            cachedServerWorkspace.setUpdated();

            // Don't automatically send the warning.  If there are no pending updates, then
            // there's no reason for the user to see it.
            if (cache.hasPendingUpdates()) {
                alerts.addWarning(
                        exec.getProject(),
                        P4Bundle.message("warning.client.updated.title", getCachedClientName()),
                        P4Bundle.message("warning.client.updated", getCachedClientName()),
                        null, FilePathUtil.getFilePathsFsrStrings(roots));
            }
            cache.refreshServerState(exec, alerts);
        }
    }

    @Override
    protected void rectifyCache(@NotNull final Project project,
            @NotNull final Collection<PendingUpdateState> pendingUpdateStates,
            @NotNull final AlertManager alerts) {
        // Nothing to do
    }

    @NotNull
    @Override
    protected Collection<UpdateGroup> getSupportedUpdateGroups() {
        return Collections.emptyList();
    }


    @Override
    protected boolean needsRefresh() {
        return getLastRefreshDate().equals(CachedState.NEVER_LOADED);
    }


    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return cachedServerWorkspace.getLastUpdated();
    }

    @Override
    protected void checkLocalIntegrity(final List<PendingUpdateState> pendingUpdates) {
        // ignore, because there's no local changed versions
    }
}
