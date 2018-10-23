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

package net.groboclown.p4.server.impl.util;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class FileSpecBuildUtil {

    public static List<IFileSpec> forFilePaths(FilePath... files) {
        return forFilePaths(Arrays.asList(files));
    }


    public static List<IFileSpec> forFilePaths(Collection<FilePath> files) {
        List<String> src = new ArrayList<>(files.size());
        for (FilePath file : files) {
            src.add(file.getPath());
        }
        return FileSpecBuilder.makeFileSpecList(src);
    }

    public static List<IFileSpec> forFiles(File... files) {
        return forFiles(Arrays.asList(files));
    }


    public static List<IFileSpec> forFiles(Collection<File> files) {
        List<String> src = new ArrayList<>(files.size());
        for (File file : files) {
            src.add(file.getAbsolutePath());
        }
        return FileSpecBuilder.makeFileSpecList(src);
    }

    public static List<IFileSpec> escapedForFilePaths(FilePath... files) {
        return escapedForFilePaths(Arrays.asList(files));
    }


    public static List<IFileSpec> escapedForFilePaths(Collection<FilePath> files) {
        List<String> src = new ArrayList<>(files.size());
        for (FilePath file : files) {
            src.add(escapeToP4Path(file.getPath()));
        }
        return FileSpecBuilder.makeFileSpecList(src);
    }

    public static List<IFileSpec> escapedForFilePathRev(FilePath file, int revision) {
        String depotPath = escapeToP4Path(file.getPath());
        if (revision > 0) {
            depotPath = depotPath + '#' + revision;
        }
        return FileSpecBuilder.makeFileSpecList(depotPath);
    }

    public static List<IFileSpec> escapedForFilePathsAnnotated(Collection<FilePath> files, String annotation,
            boolean allowDirectories) {
        if (annotation == null) {
            annotation = "";
        }
        List<String> src = new ArrayList<>(files.size());
        for (FilePath file : files) {
            String path = escapeToP4Path(file.getPath());
            if (allowDirectories && file.isDirectory()) {
                path += "/...";
            }
            src.add(path + annotation);
        }
        return FileSpecBuilder.makeFileSpecList(src);
    }

    public static List<IFileSpec> escapedForRemoteFileRev(P4RemoteFile file, int revision) {
        // Guaranteed way to ensure that the underlying path is properly escaped.
        String depotPath = escapeToP4Path(file.getDisplayName());
        if (revision > 0) {
            depotPath = depotPath + '#' + revision;
        }
        return FileSpecBuilder.makeFileSpecList(depotPath);
    }

    public static List<IFileSpec> escapedForFiles(File... files) {
        return forFiles(Arrays.asList(files));
    }

    public static List<IFileSpec> escapedForFiles(Collection<File> files) {
        List<String> src = new ArrayList<>(files.size());
        for (File file : files) {
            src.add(escapeToP4Path(file.getAbsolutePath()));
        }
        return FileSpecBuilder.makeFileSpecList(src);
    }

    public static List<IFileSpec> stripDepotRevisions(@NotNull List<IFileSpec> files) {
        List<String> request = new ArrayList<>(files.size());
        for (IFileSpec file : files) {
            String path = file.getDepotPathString();
            if (path == null) {
                throw new IllegalArgumentException("null file spec in arguments");
            }
            int pos = path.lastIndexOf('#');
            if (pos >= 0) {
                path = path.substring(0, pos);
            }
            pos = path.lastIndexOf('@');
            if (pos >= 0) {
                path = path.substring(0, pos);
            }
            request.add(path);
        }
        return FileSpecBuilder.makeFileSpecList(request);
    }


    private static String escapeToP4Path(@NotNull String path) {
        if (path.contains("...")) {
            throw new IllegalArgumentException(path);
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
}
