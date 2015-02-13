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
package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class P4FileInfo {
    private static final Logger LOG = Logger.getInstance(P4FileInfo.class);

    public static enum ClientAction {
        ADD(FileStatus.ADDED, true, false, false, false, FileGroup.CREATED_ID),
        DELETE(FileStatus.DELETED, false, true, false, false, FileGroup.REMOVED_FROM_REPOSITORY_ID),
        EDIT(FileStatus.MODIFIED, false, false, true, false, FileGroup.MODIFIED_ID),

        ADD_INTEGRATE(FileStatus.ADDED, true, false, false, true, FileGroup.CREATED_ID),
        DELETE_INTEGRATE(FileStatus.DELETED, false, true, false, true, FileGroup.REMOVED_FROM_REPOSITORY_ID),
        EDIT_INTEGRATE(FileStatus.MERGE, false, false, true, true, FileGroup.MERGED_ID),

        NONE(FileStatus.NOT_CHANGED, false, false, false, false, FileGroup.SKIPPED_ID);

        private final FileStatus fileStatus;
        private final boolean add;
        private final boolean delete;
        private final boolean edit;
        private final boolean integrate;
        private final String fileGroupId;

        ClientAction(FileStatus fileStatus, boolean add, boolean delete, boolean edit, boolean integrate, String fileGroupId) {
            this.fileStatus = fileStatus;
            this.add = add;
            this.delete = delete;
            this.edit = edit;
            this.integrate = integrate;
            this.fileGroupId = fileGroupId;
        }

        public boolean isAdd() {
            return add;
        }

        public boolean isDelete() {
            return delete;
        }

        public boolean isEdit() {
            return edit;
        }

        public boolean isIntegrate() {
            return integrate;
        }

        @NotNull
        public String getFileGroupId() {
            return fileGroupId;
        }

        public FileStatus getFileStatus() {
            return fileStatus;
        }

    }

    @NotNull
    private final FilePath path;

    @Nullable
    private final IFileSpec spec;

    @Nullable
    private final IExtendedFileSpec extendedSpec;

    private final boolean inDepot;
    private final boolean onClient;
    private final int haveRev;
    private final int headRev;
    private final boolean inClientView;
    private final boolean isDeletedInDepot;
    private final int inChangelist;

    @NotNull
    private final ClientAction clientAction;

    @NotNull
    private final String name;

    @NotNull
    public FilePath getPath() {
        return path;
    }

    @Nullable
    public IExtendedFileSpec getExtendedSpec() {
        return extendedSpec;
    }

    public boolean isInDepot() {
        return inDepot;
    }

    public boolean isOnClient() {
        return onClient;
    }

    public int getHaveRev() {
        return haveRev;
    }

    public int getHeadRev() {
        return headRev;
    }

    public boolean isInClientView() {
        return inClientView;
    }

    public boolean isDeletedInDepot() {
        return isDeletedInDepot;
    }

    @NotNull
    public ClientAction getClientAction() {
        return clientAction;
    }

    public boolean isOpenInClient() {
        return clientAction != ClientAction.NONE;
    }

    public boolean isOpenForDelete() {
        return clientAction.isDelete();
    }

    public int getChangelist() {
        return inChangelist;
    }

    /**
     * Checks if the file is open in the client for edit or add, but not
     * as an integration
     *
     * @return true of the file is open for add or edit, but not as an integration.
     */
    public boolean isOpenForEditOrAdd() {
        return (clientAction.isEdit() || clientAction.isAdd()) &&
                ! clientAction.isIntegrate();
    }


    /**
     * Converts this to a client-local version of the
     * IFileSpec.  If it is not in the client view, then
     * it throws an exception.
     *
     * @return client (local file path) spec of this file.
     */
    @NotNull
    public IFileSpec toClientSpec() throws P4Exception {
        if (!inClientView) {
            throw new IllegalStateException("not in client view: " + this);
        }
        // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
        // use this instead: getIOFile().getAbsolutePath()
        return FileSpecUtil.getOneSpec(path.getIOFile().getAbsolutePath().replace('/', File.separatorChar));
    }


    @NotNull
    public IFileSpec toDepotSpec() throws P4Exception {
        if (spec != null && spec.getDepotPathString() != null) {
            return FileSpecUtil.getOneSpec(spec.getDepotPathString());
        }
        throw new IllegalStateException("not in depot view: " + this);
    }


    @Nullable
    public String getDepotPath() {
        return spec == null ? null : spec.getDepotPathString();
    }


    /**
     * FSTAT source
     *
     * @param path file
     * @param extendedSpec fstat source
     */
    private P4FileInfo(@NotNull FilePath path, @NotNull IExtendedFileSpec extendedSpec) {
        checkForInvalidPath(path);
        this.path = path;
        this.spec = extendedSpec;
        this.extendedSpec = extendedSpec;
        this.inDepot = extendedSpec.getHeadAction() != null;
        this.onClient = extendedSpec.getHaveRev() > 0;
        this.haveRev = extendedSpec.getHaveRev();
        this.headRev = extendedSpec.getHeadRev();
        this.inClientView = true;
        this.isDeletedInDepot = extendedSpec.getHeadRev() == 0;
        this.clientAction = fromAction(
                extendedSpec.getOpenAction() == null ?
                        extendedSpec.getAction() : extendedSpec.getOpenAction());
        //this.inChangelist = extendedSpec.getOpenChangelistId();
        this.inChangelist = extendedSpec.getChangelistId();
        //System.out.println("- in change " + inChangelist + ";" + extendedSpec.getChangelistId());

        this.name = extendedSpec.getDepotPathString() +
                "#h" + headRev + "#v" + haveRev + "@" + inChangelist;
    }


    /**
     * From "p4 opened", but not on the server.
     *
     * @param path source file
     * @param spec opened spec
     */
    private P4FileInfo(@NotNull FilePath path, @NotNull IFileSpec spec, boolean opened) {
        checkForInvalidPath(path);
        this.path = path;
        this.spec = spec;
        this.extendedSpec = null;
        this.inDepot = false;
        this.onClient = true; // it can't be deleted, because that means it's on the server.
        this.haveRev = 0;
        this.headRev = -1;
        this.inClientView = true;
        this.isDeletedInDepot = false;
        this.clientAction = fromAction(spec.getAction());
        this.inChangelist = spec.getChangelistId();

        this.name = spec.getDepotPathString() + "#new";
    }


    /**
     * From "p4 where", which means it's not in the changelist or server.
     *
     * @param path file
     * @param spec where spec
     * @param view marker for this constructor
     */
    private P4FileInfo(@NotNull FilePath path, @NotNull IFileSpec spec, int view) {
        checkForInvalidPath(path);
        this.path = path;
        this.spec = spec;
        this.extendedSpec = null;
        this.inDepot = false;
        this.onClient = false;
        this.haveRev = -1;
        this.headRev = -1;
        this.inClientView = true;
        this.isDeletedInDepot = false;
        this.clientAction = fromAction(spec.getAction());
        this.inChangelist = -100;

        this.name = spec.getDepotPathString() + "#view";
    }


    /**
     * Not in the client view
     *
     * @param path path
     */
    private P4FileInfo(@NotNull FilePath path) {
        checkForInvalidPath(path);
        this.path = path;
        this.spec = null;
        this.extendedSpec = null;
        this.inDepot = false;
        this.onClient = false;
        this.haveRev = -1;
        this.headRev = -1;
        this.inClientView = false;
        this.isDeletedInDepot = false;
        this.clientAction = ClientAction.NONE;
        inChangelist = -100;

        // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
        // use this instead: getIOFile().getAbsolutePath()
        this.name = path.getIOFile().getAbsolutePath();
    }


    @Override
    public String toString() {
        return name;
    }


    @Override
    public int hashCode() {
        return this.name.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj.getClass().equals(P4FileInfo.class)) {
            return this.name.equals(obj.toString());
        }
        return false;
    }


    public boolean isSameFile(@Nullable IFileSpec spec) {
        if (spec == null) {
            return false;
        }
        if (spec.getLocalPathString() != null) {
            File localPath = path.getIOFile();
            File specFile = new File(spec.getLocalPathString());
            return FileUtil.filesEqual(localPath, specFile);
        } else if (spec.getDepotPathString() != null & getDepotPath() != null) {
            String specPath = stripAnnotationsFromPath(spec.getDepotPathString());
            String ourPath = stripAnnotationsFromPath(getDepotPath());
            return specPath.equals(ourPath);
        }
        return false;
    }

    private static String stripAnnotationsFromPath(String p4Path) {
        int pos = p4Path.indexOf('#');
        if (pos > 0) {
            p4Path = p4Path.substring(0, pos);
        }
        pos = p4Path.indexOf('@');
        if (pos > 0) {
            p4Path = p4Path.substring(0, pos);
        }
        return p4Path;
    }


    /**
     * If this file was opened in a "move" operation, then it
     * has a paired file (either the source or the target
     * of the move).
     *
     * @return null if there is no paired file, otherwise the
     *   paired file.
     */
    @Nullable
    public IFileSpec getOpenedPair() {
        if (extendedSpec != null && extendedSpec.getMovedFile() != null) {
            // this file will be already escaped, so we need to
            // avoid a double-escape.
            return FileSpecUtil.getMovedFileSpec(extendedSpec);

            // to/from pairs reflect integration, not moves.
            // So a to/from pair won't have a second half.
        }
        return null;
    }


    public boolean isSameFile(@Nullable FilePath path) {
        return path != null && path.equals(this.path);
    }


    public static ClientAction fromAction(FileAction action) {
        if (action == null) {
            return ClientAction.NONE;
        }
        switch (action) {
            case ADD:
            case ADDED:
                return ClientAction.ADD;

            case MOVE_ADD:
            case MOVE:
            case COPY_FROM:
            case IMPORT:
                return ClientAction.ADD_INTEGRATE;

            case BRANCH:
            case INTEGRATE:
            case UPDATED:
            case REFRESHED:
            case REPLACED:
            case IGNORED:
            case ABANDONED:
            case RESOLVED:
            case UNRESOLVED:
            case MERGE_FROM:
            case EDIT_IGNORED:
            case EDIT_FROM:
                return ClientAction.EDIT_INTEGRATE;

            case EDIT:
                return ClientAction.EDIT;

            case DELETE:
            case DELETED:
                return ClientAction.DELETE;

            case MOVE_DELETE:
            case PURGE:
                return ClientAction.DELETE_INTEGRATE;
            case SYNC:
            case UNKNOWN:
            default:
                return ClientAction.NONE;
        }
    }


    public static List<IFileSpec> toClientList(Collection<P4FileInfo> files) throws P4Exception {
        List<IFileSpec> ret = new ArrayList<IFileSpec>(files.size());
        for (P4FileInfo file: files) {
            ret.add(file.toClientSpec());
        }
        return ret;
    }


    public static List<IFileSpec> toClientList(P4FileInfo... files) throws P4Exception {
        List<IFileSpec> ret = new ArrayList<IFileSpec>(files.length);
        for (P4FileInfo file : files) {
            ret.add(file.toClientSpec());
        }
        return ret;
    }


    public static List<IFileSpec> toDepotList(Collection<P4FileInfo> files) throws P4Exception {
        List<IFileSpec> ret = new ArrayList<IFileSpec>(files.size());
        for (P4FileInfo file : files) {
            ret.add(file.toDepotSpec());
        }
        return ret;
    }


    public static List<IFileSpec> toDepotList(P4FileInfo... files) throws P4Exception {
        List<IFileSpec> ret = new ArrayList<IFileSpec>(files.length);
        for (P4FileInfo file : files) {
            ret.add(file.toDepotSpec());
        }
        return ret;
    }


    public static List<IFileSpec> toDefaultSpecList(P4FileInfo... files) {
        List<IFileSpec> ret = new ArrayList<IFileSpec>(files.length);
        for (P4FileInfo file : files) {
            if (file.spec != null) {
                ret.add(file.spec);
            } else {
                LOG.error("ERROR: could not convert to IFileSpec: " + file);
            }
        }
        return ret;
    }


    public static List<FilePath> toFilePathList(Collection<P4FileInfo> files) {
        List<FilePath> ret = new ArrayList<FilePath>();
        for (P4FileInfo file: files) {
            ret.add(file.getPath());
        }
        return ret;
    }



    static class FstatLoadSpecs implements P4Exec.WithClient<List<P4FileInfo>> {
        private final List<IFileSpec> specs;

        FstatLoadSpecs(List<IFileSpec> specs) {
            this.specs = specs;
        }

        @Override
        public List<P4FileInfo> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException {
            if (specs.isEmpty()) {
                return Collections.emptyList();
            }
            List<P4FileInfo> ret = new ArrayList<P4FileInfo>(specs.size());
            List<IFileSpec> remaining = new ArrayList<IFileSpec>(specs);
            List<IExtendedFileSpec> mapped = server.getExtendedFiles(remaining,
                    -1, -1, -1,
                    //new FileStatOutputOptions(true, true, true, true, true, true),
                    new FileStatOutputOptions(false, false, false, false, false, false),
                    new FileStatAncilliaryOptions(
                            false, false, true, true, false
                    ));
            for (IExtendedFileSpec spec : mapped) {
                LOG.debug("client spec processing: " + spec + "@" + spec.getChangelistId());
                if (P4StatusMessage.isErrorStatus(spec)) {
                    P4StatusMessage message = new P4StatusMessage(spec);
                    if (message.isNoSuchFilesMessage()) {
                        // a purely local file
                        LOG.debug("a not-known-by-server file: " + message.getMessage());
                        continue;
                    } else {
                        LOG.info("fstat reported error '" + message + "' for files " + specs);
                        throw new P4JavaException(message.toString());
                    }
                }

                removeFrom(spec, remaining);
                if (spec.getLocalPathString() == null) {
                    if (spec.getClientPathString() != null && !spec.getClientPathString().startsWith("//")) {
                        // unescape name
                        FilePath path = VcsUtil.getFilePath(FileSpecUtil.unescapeP4Path(spec.getClientPathString()), false);
                        ret.add(new P4FileInfo(path, spec));
                    } else {
                        P4JavaException e = new P4JavaException("expected file spec, but found " + spec +
                                " p[" + spec.getAnnotatedPreferredPathString() +
                                " s[" + spec.getStatusMessage() +
                                " a[" + spec.getAction() +
                                " b[" + spec.getBaseFile() +
                                " f[" + spec.getFromFile() +
                                " c#" + spec.getRawCode() +
                                "." + spec.getUniqueCode() +
                                "." + spec.getGenericCode() +
                                "." + spec.getSeverityCode() +
                                "." + spec.getSubCode()
                        );
                        // Throwing an exception will hide the stack trace.
                        LOG.info(e);
                        throw e;
                    }
                    // else some commands return an empty last file spec; it's the
                    // status message..
                } else {
                    // unescape name
                    FilePath path = VcsUtil.getFilePath(FileSpecUtil.unescapeP4Path(spec.getLocalPathString()), false);
                    ret.add(new P4FileInfo(path, spec));
                }
            }
            if (! remaining.isEmpty()) {
                // Try client "where" command, as these files are not known by
                // the server.

                // Where cannot take a # or @ specification on the files.
                stripSpecification(remaining);

                List<IFileSpec> opened = client.where(remaining);
                for (IFileSpec spec : opened) {
                    if (P4StatusMessage.isErrorStatus(spec)) {
                        P4StatusMessage message = new P4StatusMessage(spec);
                        if (message.isNoSuchFilesMessage()) {
                            // outside the client view
                            LOG.debug("a not-in-view file: " + message.getMessage());
                            continue;
                        } else {
                            throw new P4JavaException(message.toString());
                        }
                    }

                    removeFrom(spec, remaining);
                    if (spec.getLocalPathString() != null) {
                        // unescape name
                        FilePath path = VcsUtil.getFilePath(FileSpecUtil.unescapeP4Path(spec.getLocalPathString()));
                        ret.add(new P4FileInfo(path, spec, 1));
                    } else if (spec.getOriginalPathString() != null) {
                        // unescape name
                        FilePath path = VcsUtil.getFilePath(FileSpecUtil.unescapeP4Path(spec.getOriginalPathString()));
                        ret.add(new P4FileInfo(path, spec, 1));
                    } else {
                        LOG.info("don't know what this is: " + spec);
                    }
                }

                // It's not in the client view.
                for (IFileSpec spec : remaining) {
                    String origPath = spec.getOriginalPathString();
                    if (origPath != null && !origPath.startsWith("//")) {
                        // unescape name
                        FilePath path = VcsUtil.getFilePath(FileSpecUtil.unescapeP4Path(origPath));
                        ret.add(new P4FileInfo(path));
                    } else {
                        LOG.info("Ignoring invalid path " + origPath);
                    }
                }
            }
            return ret;
        }
    }

    private static void stripSpecification(List<IFileSpec> remaining) {
        List<String> replaced = new ArrayList<String>(remaining.size());
        Iterator<IFileSpec> iter = remaining.iterator();
        while (iter.hasNext()) {
            IFileSpec spec = iter.next();
            String path = spec.getAnnotatedPreferredPathString();
            int pos = Math.min(path.indexOf('@'), path.indexOf('#'));
            if (pos >= 0) {
                path = path.substring(0, pos);
                replaced.add(path);
                iter.remove();
            }
        }
        remaining.addAll(FileSpecBuilder.makeFileSpecList(replaced));
    }

    static class OpenedSpecs implements P4Exec.WithClient<List<P4FileInfo>> {
        private final List<IFileSpec> specs;

        OpenedSpecs(@NotNull List<IFileSpec> specs) {
            this.specs = specs;
        }

        @Override
        public List<P4FileInfo> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4Exception, P4JavaException {
            if (specs.isEmpty()) {
                return Collections.emptyList();
            }
            List<IFileSpec> files = client.openedFiles(
                    specs,
                    0, IChangelist.UNKNOWN);
            List<P4FileInfo> ret = new ArrayList<P4FileInfo>();
            List<IFileSpec> clientFiles = new ArrayList<IFileSpec>();
            for (IFileSpec spec : files) {
                if (P4StatusMessage.isErrorStatus(spec)) {
                    P4Exception e = new P4Exception(new P4StatusMessage(spec).toString());
                    e.printStackTrace();
                    throw e;
                }
                if (spec instanceof IExtendedFileSpec) {
                    // unescape name
                    FilePath path = VcsUtil.getFilePath(FileSpecUtil.unescapeP4Path(spec.getLocalPathString()), false);
                    ret.add(new P4FileInfo(path, (IExtendedFileSpec) spec));
                } else if (spec.getDepotPathString() != null && spec.getClientPathString() != null &&
                        spec.getLocalPathString() != null) {
                    // looks like it's already loaded up.
                    // unescape name
                    FilePath path = VcsUtil.getFilePath(FileSpecUtil.unescapeP4Path(spec.getLocalPathString()), false);
                    ret.add(new P4FileInfo(path, spec, true));
                } else if (spec.getDepotPathString() != null || spec.getClientPathString() != null ||
                        spec.getLocalPathString() != null || spec.getOriginalPathString() != null) {
                    if (fromAction(spec.getAction()).isAdd()) {
                        // The spec can have the annotation (revision) set, but
                        // on add, that can cause a failure on fstat.
                        String path = spec.getDepotPathString();
                        spec = new FileSpec();
                        spec.setDepotPath(path);
                    }
                    clientFiles.add(spec);
                    LOG.debug("Add action with d[" +
                            spec.getDepotPathString() +
                            "; s[" + spec.toString() + "; w#" +
                            spec.getWorkRev() + "; c[" +
                            spec.getClientPathString() + "; b#" +
                            spec.getBaseRev() + "; a[" +
                            spec.getAnnotatedPreferredPathString());
                } else if (spec.getUniqueCode() != 0) {
                    P4Exception e = new P4Exception("expected file spec, but found " + spec +
                            " p[" + spec.getAnnotatedPreferredPathString() +
                            " s[" + spec.getStatusMessage() +
                            " a[" + spec.getAction() +
                            " b[" + spec.getBaseFile() +
                            " f[" + spec.getFromFile() +
                            " c#" + spec.getRawCode() +
                            ".u" + spec.getUniqueCode() +
                            ".g" + spec.getGenericCode() +
                            ".v" + spec.getSeverityCode() +
                            ".s" + spec.getSubCode()
                    );
                    // Throwing an exception will hide the stack trace.
                    LOG.info(e);
                    throw e;
                    //} else {
                    // This is a null spec, which usually means it was a blank
                    // info line that came back from the server.
                    // P4JavaException e = new P4JavaException("could not map spec: "
                    // e.printStackTrace();
                    // throw e;
                }
            }
            if (! clientFiles.isEmpty()) {
                LOG.info("Loading " + clientFiles.size() + " files through fstat due to 'opened' only giving us information for " + ret.size() + " files");
                ret.addAll(new FstatLoadSpecs(clientFiles).run(server, client));
            }

            return ret;
        }
    }


    private static void removeFrom(@NotNull IFileSpec spec, @NotNull List<IFileSpec> specs) {
        Iterator<IFileSpec> iter = specs.iterator();
        boolean matched = false;
        final String specDepot = spec.getDepotPathString();
        final String specClient = spec.getClientPathString();
        final String specLocal = spec.getLocalPathString();
        final String specOriginal = spec.getOriginalPathString();
        final String specUnescapedOriginal =
                specOriginal == null ? null : FileSpecUtil.unescapeP4Path(specOriginal);

        while (iter.hasNext()) {
            final IFileSpec next = iter.next();
            final String nextDepot = next.getDepotPathString();

            if (nextDepot != null && nextDepot.equals(specDepot)) {
                iter.remove();
                matched = true;
                continue;
            }

            final String nextClient = next.getClientPathString();
            if (nextClient != null && nextClient.equals(specClient)) {
                iter.remove();
                matched = true;
                continue;
            }

            final String nextLocal = next.getLocalPathString();
            if (nextLocal != null && (
                    isSameFile(nextLocal, specLocal) ||
                    isSameFile(nextLocal, specUnescapedOriginal))) {
                iter.remove();
                matched = true;
                continue;
            }

            // original paths will be escaped due to our
            // policy of always escaping the name.  However,
            // the server returns the local path string
            // unescaped (native OS path name).
            final String nextOriginal = next.getOriginalPathString();
            if (nextOriginal != null) {
                final String nextUnescapedOriginal = FileSpecUtil.unescapeP4Path(nextOriginal);
                if (nextOriginal.equals(specOriginal) ||
                        nextUnescapedOriginal.equals(specUnescapedOriginal) ||
                        isSameFile(nextOriginal, specOriginal) ||
                        isSameFile(nextUnescapedOriginal, specUnescapedOriginal) ||
                        isSameFile(nextOriginal, specDepot) ||
                        isSameFile(nextUnescapedOriginal, specLocal)) {
                    iter.remove();
                    matched = true;
                    continue;
                }
            }
        }
        if (!matched) {
            LOG.debug("P4FileInfo: no known spec " + spec + " in " + specs +
                    ": " + spec.getOriginalPathString() + "; " + spec.getLocalPathString());
        }
    }


    private static boolean isSameFile(String path1, String path2) {
        if (path1 == null || path2 == null) {
            return false;
        }
        File f1 = new File(path1);
        File f2 = new File(path2);
        return FileUtil.filesEqual(f1, f2);
    }


    private static void checkForInvalidPath(FilePath path) {
        // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
        // use this instead: getIOFile().getAbsolutePath()
        String real = path.getIOFile().getAbsolutePath();
        if (real.startsWith("//") ||
                real.startsWith("\\\\")) {
            throw new IllegalArgumentException("incorrect path format: " + real);
        }
    }
}
