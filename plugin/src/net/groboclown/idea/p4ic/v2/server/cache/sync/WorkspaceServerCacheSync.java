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
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.P4ConfigUtil;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.state.FileMappingRepo;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4WorkspaceViewState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4WorkspaceViewState.ViewMapping;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import net.groboclown.idea.p4ic.v2.server.connection.ServerQuery;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import net.groboclown.idea.p4ic.v2.ui.alerts.InvalidClientHandler;
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
public class WorkspaceServerCacheSync {
    private static final Logger LOG = Logger.getInstance(WorkspaceServerCacheSync.class);

    private final Cache cache;
    private final FileMappingRepo fileRepo;
    private final P4WorkspaceViewState cachedServerWorkspace;

    // per-instance exception to reference the root directory.  It
    // is reset whenever the workspace is reloaded.  It is necessary
    // to be reused so that the user doesn't keep seeing the same error.
    @NotNull
    private VcsException invalidRootsException = new VcsException("no valid roots");


    public WorkspaceServerCacheSync(@NotNull final Cache cache,
            @NotNull final FileMappingRepo fileRepo,
            @NotNull final P4WorkspaceViewState cachedServerWorkspace) {
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
    public Collection<P4FileUpdateState> fromOpenedToAction(@NotNull final List<IFileSpec> fileSpecs,
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
        String relClientPath = clientPath.substring(clientPrefix.length());
        final List<String> workspaceRoots = cachedServerWorkspace.getRoots();
        if (workspaceRoots.isEmpty()) {
            alerts.addWarning(P4Bundle.message("error.config.invalid-roots", cache.getClientName()),
                    invalidRootsException);
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
        alerts.addNotice(P4Bundle.message("client.root.non-existent", root), null);

        File clientFile = new File(root, relClientPath);
        return FilePathUtil.getFilePath(clientFile);
    }

    @NotNull
    private String getCachedClientName() {
        // no need for synchronization here.
        return cachedServerWorkspace.getName();
    }

    public P4ClientFileMapping getClientMappingFor(final FilePath file) {
        ServerConnection.assertInServerConnection();
        return fileRepo.getByLocation(file);
    }

    @NotNull
    List<VirtualFile> getClientRoots(@NotNull Project project, @NotNull AlertManager alerts) {
        final List<VirtualFile> projectRoots = P4ConfigUtil.getVcsRootFiles(project);
        final List<String> workspaceRoots = cachedServerWorkspace.getRoots();
        for (String workspaceRoot : workspaceRoots) {
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
        alerts.addWarning(P4Bundle.message("error.config.invalid-roots", cache.getClientName()), invalidRootsException);
        return Collections.emptyList();
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


    @Nullable
    VirtualFile getBestClientRoot(@NotNull File referenceDir, @NotNull AlertManager alerts) {
        final List<String> workspaceRoots = cachedServerWorkspace.getRoots();
        final FilePath reference = FilePathUtil.getFilePath(referenceDir);
        for (String workspaceRoot : workspaceRoots) {
            // Special case for a workspace root that spans windows directories.
            if (workspaceRoot.equals("null")) {
                return reference.getVirtualFile();
            }
            final FilePath fp = FilePathUtil.getFilePath(workspaceRoot);
            if (fp.isDirectory() && fp.getVirtualFile() != null && fp.getVirtualFile().exists()) {
                if (reference.isUnder(fp, false)) {
                    // reference is more precise
                    return reference.getVirtualFile();
                } else if (fp.isUnder(reference, false)) {
                    // workspace root is more precise
                    return fp.getVirtualFile();
                }
            }
        }
        LOG.info("Did not find roots matching " + referenceDir + "; roots = " + workspaceRoots);
        // no root found.
        alerts.addWarning(P4Bundle.message("error.config.invalid-roots", cache.getClientName()), invalidRootsException);
        return null;
    }


    class WorkspaceRefreshServerQuery implements ServerQuery<WorkspaceServerCacheSync> {

        @Override
        public WorkspaceServerCacheSync query(@NotNull P4Exec2 exec, @NotNull ClientCacheManager cacheManager,
                @NotNull ServerConnection connection, @NotNull AlertManager alerts) {
            ServerConnection.assertInServerConnection();

            final IClient client;
            try {
                client = exec.getClient();
            } catch (VcsException e) {
                alerts.addCriticalError(new InvalidClientHandler(exec.getProject(), getCachedClientName(), e.getMessage()), e);
                return null;
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

            // refreshed the root directories, so the exception can be different.
            invalidRootsException = new VcsException("no valid roots");

            if (doRefresh) {
                alerts.addWarning(P4Bundle.message("warning.client.updated", getCachedClientName()), null);
                cache.refreshServerState();
            }

            return WorkspaceServerCacheSync.this;
        }
    }
}
