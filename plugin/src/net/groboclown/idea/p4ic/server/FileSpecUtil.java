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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Properly handles the conversion to and from IFileSpec objects,
 * specifically when dealing with filenames that contain wildcards.
 * The <em>ONLY</em> exception to this is when adding a file to
 * Perforce.
 */
public class FileSpecUtil {
    private static final Logger LOG = Logger.getInstance(FileSpecUtil.class);


    /**
     * Convert the path (must not contain Perforce revision or changelist information)
     * into a filespec.
     *
     * @param path filesystem path
     * @return filespec for the path
     */
    @NotNull
    static IFileSpec getOneSpec(@NotNull String path) throws P4Exception {
        VirtualFile f = LocalFileSystem.getInstance().findFileByPath(path.replace(File.separatorChar, '/'));
        if (f != null && f.isDirectory()) {
            throw new P4FileException(P4Bundle.message("error.filespec.directory"), f);
        }
        // Can't help out the user on a null filespec conversion.

        List<IFileSpec> ret = FileSpecBuilder.makeFileSpecList(escapeToP4Path(path));
        if (ret.size() != 1) {
            throw new IllegalStateException(P4Bundle.message("error.annotate.multiple-files", path, ret));
        }
        IFileSpec spec = ret.get(0);
        if (spec == null) {
            throw new IllegalStateException(P4Bundle.message("error.filespec.null", path));
        }
        return spec;
    }


    @NotNull
    public static IFileSpec getOneSpecWithRev(@NotNull FilePath file, int rev) throws P4Exception {
        if (file.isDirectory()) {
            throw new P4FileException(P4Bundle.message("error.filespec.directory"), file);
        }
        // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
        // use this instead: getIOFile().getAbsolutePath()
        String path = escapeToP4Path(file.getIOFile().getAbsolutePath());
        // Doesn't seem to set the values right.
        if (rev > 0) {
            path = path + '#' + Integer.toString(rev);
        }
        final List<IFileSpec> spec = FileSpecBuilder.makeFileSpecList(path);
        if (spec.size() != 1) {
            throw new P4Exception(P4Bundle.message("error.annotate.multiple-files", path, spec));
        }
        IFileSpec ret = spec.get(0);
        //if (rev > 0) {
        //    ret.setBaseRev(rev);
        //    ret.setWorkRev(rev);
        //}
        return ret;
    }


    @NotNull
    public static IFileSpec getFromDepotPath(@NotNull final String depotPath, int rev) throws P4FileException {
        String path = escapeToP4Path(depotPath);
        if (rev > 0) {
            path = path + '#' + Integer.toString(rev);
        }
        final List<IFileSpec> list = FileSpecBuilder.makeFileSpecList(path);
        if (list.size() != 1) {
            throw new P4FileException(P4Bundle.message("error.annotate.multiple-files", path, list));
        }
        return list.get(0);
    }


    @NotNull
    public static IFileSpec getFromFilePath(@NotNull FilePath file) throws P4Exception {
        return getFromFilePathsAt(Collections.singletonList(file), "", false).get(0);
    }


    @NotNull
    public static List<IFileSpec> getFromFilePaths(@NotNull Collection<FilePath> files) throws P4Exception {
        return getFromFilePathsAt(files, "", false);
    }

    @NotNull
    public static List<IFileSpec> getFromVirtualFiles(@NotNull Collection<VirtualFile> files) throws P4Exception {
        return getFromVirtualFilesAt(files, "", false);
    }

    /**
     * Must return the files in the same order in which they are passed in.
     *
     * @param files
     * @param revisionPart
     * @param allowDirectories
     * @return
     * @throws P4Exception
     */
    @NotNull
    public static List<IFileSpec> getFromFilePathsAt(Collection<FilePath> files, @NotNull String revisionPart, boolean allowDirectories)
            throws P4Exception {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> paths = new ArrayList<String>(files.size());
        for (FilePath fp : files) {
            // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
            // use this instead: getIOFile().getAbsolutePath()
            String path = escapeToP4Path(fp.getIOFile().getAbsolutePath());
            if (fp.isDirectory()) {
                if (allowDirectories) {
                    if (!path.endsWith("/") && !path.endsWith("\\") &&
                            !path.endsWith(File.separator)) {
                        path += File.separator + "...";
                    }
                } else {
                    throw new P4FileException(P4Bundle.message("error.filespec.directory"), fp);
                }
            }
            path += revisionPart;
            if (!paths.contains(path)) {
                paths.add(path);
            }
        }

        return FileSpecBuilder.makeFileSpecList(paths);
    }

    @NotNull
    public static List<IFileSpec> getFromVirtualFilesAt(@NotNull Collection<VirtualFile> files, @NotNull String revisionPart, boolean allowDirectories) throws P4Exception {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> paths = new ArrayList<String>(files.size());
        for (VirtualFile vf : files) {
            String path = escapeToP4Path(vf.getPath());
            if (vf.isDirectory()) {
                if (allowDirectories) {
                    if (! path.endsWith("/") && ! path.endsWith("\\") &&
                            ! path.endsWith(File.separator)) {
                        path += File.separator + "...";
                    }
                } else {
                    throw new P4FileException(P4Bundle.message("error.filespec.directory"), vf);
                }
            }
            path += revisionPart;
            if (!paths.contains(path)) {
                //paths.add(escapeToP4Path(vf.getPath().replace('/', File.separatorChar)));
                paths.add(path);
            }
        }

        return FileSpecBuilder.makeFileSpecList(paths);
    }

    /** Use with care... */
    @NotNull
    public static List<IFileSpec> makeRootFileSpecs(@Nullable VirtualFile[] roots) throws P4Exception {
        if (roots == null) {
            return Collections.emptyList();
        }
        List<String> paths = new ArrayList<String>(roots.length);
        for (VirtualFile root : roots) {
            String path = escapeToP4Path(root.getPath()) + "/...";
            if (! paths.contains(path)) {
                paths.add(path);
            }
        }

        return FileSpecBuilder.makeFileSpecList(paths);
    }


    @NotNull
    static IFileSpec getMovedFileSpec(@NotNull IExtendedFileSpec espec) {
        // Note: because this is a path populated by Perforce, it means that it's
        // already escaped.
        String escapedPath = espec.getMovedFile();
        List<IFileSpec> ret = FileSpecBuilder.makeFileSpecList(escapedPath);
        if (ret.size() != 1) {
            throw new IllegalStateException(P4Bundle.message("error.annotate.multiple-files", escapedPath, ret));
        }
        IFileSpec spec = ret.get(0);
        if (spec == null) {
            throw new IllegalStateException(P4Bundle.message("error.filespec.null", escapedPath));
        }
        return spec;
    }


    @NotNull
    static List<IFileSpec> getP4RootFileSpec() {
        return FileSpecBuilder.makeFileSpecList("//...");
    }


    private static String escapeToP4Path(@NotNull String path) throws P4FileException {
        if (path.contains("...")) {
            throw new P4FileException(P4Bundle.message("error.filespec.elipses", path));
        }
        StringBuilder ret = new StringBuilder(path.length() * 3 / 2);
        for (char c: path.toCharArray()) {
            switch (c) {
                case '%':
                    ret.append("%25");
                    break;
                case '*':
                    ret.append("%2A");
                    break;
                case '@':
                    ret.append("%40");
                    break;
                case '#':
                    ret.append("%23");
                    break;
                default:
                    ret.append(c);
            }
        }
        return ret.toString();
    }


    /**
     * Use only when dealing with the depot paths returned directly from the server,
     * where you KNOW that the value returned is already escaped.
     *
     * @param depotPath
     * @return
     */
    @NotNull
    public static IFileSpec getAlreadyEscapedSpec(@NotNull String depotPath) {
        final List<IFileSpec> ret = FileSpecBuilder.makeFileSpecList(depotPath);
        if (ret == null || ret.size() != 1 || ret.get(0) == null) {
            throw new IllegalStateException("P4 returned " + ret + " for depot path " + depotPath);
        }
        return ret.get(0);
    }

    /**
     * Only use if you know what you're doing; specifically, the P4Exec objects
     */
    @Nullable
    public static String unescapeP4PathNullable(@Nullable String path) {
        if (path == null) {
            return null;
        }
        return unescapeP4Path(path);
    }


        /** Only use if you know what you're doing; specifically, the P4Exec objects */
    @NotNull
    public static String unescapeP4Path(@NotNull String path) {
        StringBuilder sb = new StringBuilder(path.length());
        char[] buff = path.toCharArray();
        int pos = 0;
        while (pos < buff.length) {
            char c = buff[pos++];
            if (c == '%' && pos + 2 < buff.length) {
                final char hex1 = buff[pos++];
                final int ihex1 = Character.digit(hex1, 16);
                final char hex2 = buff[pos++];
                final int ihex2 = Character.digit(hex2, 16);
                if (ihex1 < 0 || ihex2 < 0) {
                    LOG.info("Invalid p4 escape code: %" + hex1 + hex2 + "; ignoring");
                    pos -= 2;
                    sb.append(c);
                } else {
                    sb.append((char)((ihex1 << 4) + ihex2));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
