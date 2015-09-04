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

package net.groboclown.idea.p4ic.v2.server.cache.local;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A hybrid local cached file.  The ignore file is stored entirely on the client (it can be in Perforce,
 * but the storage will still be considered local), but we'll treat it like the local cache.
 */
public class IgnoreFiles {
    private static final Logger LOG = Logger.getInstance(IgnoreFiles.class);

    private final String ignoreFileName;


    public IgnoreFiles(@NotNull final ServerConfig config) {
        this.ignoreFileName = config.getIgnoreFileName();
    }

    public String getIgnoreFileName() {
        return ignoreFileName;
    }

    public boolean isFileIgnored(@Nullable final FilePath file) {
        if (file == null) {
            return true;
        }
        final FilePath ignoreFile = findApplicableIgnoreFile(file);
        return !(ignoreFile == null || ignoreFile.getVirtualFile() == null) && isMatch(file, ignoreFile);
    }

    private boolean isMatch(@NotNull final FilePath file, @NotNull final FilePath ignoreFile) {
        LOG.debug("Checking ignore status on " + file + " against ignore file " + ignoreFile);
        assert file.isUnder(ignoreFile, false) && ignoreFile.getVirtualFile() != null && ignoreFile.getParentPath() != null;

        String subpath = ignoreFile.getParentPath().getPath().replace(File.separatorChar, '/');
        String checkpath = file.getPath().replace(File.separatorChar, '/').substring(subpath.length());
        while (checkpath.startsWith("/")) {
            checkpath = checkpath.substring(1);
        }


        try {
            final BufferedReader lineReader =
                    new BufferedReader(new InputStreamReader(ignoreFile.getVirtualFile().getInputStream()));
            try {
                // File format:
                // (from http://ftp.perforce.com/pub/perforce/r14.1/doc/help/p4vs-html/en/HtmlHelp/p4vs_ignore.html)
                // The syntax for ignore rules is not the same as Perforce syntax. Instead, it is similar to that used
                // by other versioning systems:
                //
                // Files are specified in local syntax
                //   # at the beginning of a line denotes a comment
                //   ! at the beginning of a line excludes the file specification
                //   * wildcard matches substrings
                //
                // For example:
                // foo.txt 	Ignore files called "foo.txt"
                // *.exe 	Ignore all executables
                // !bar.exe 	Exclude bar.exe from being ignored

                // This description unfortunately does not describe these circumstances:
                //   - if a "!" is found after the matcher, is it still a match?
                //     (does the first match force a result?)
                //   - if a path is specified ("temp/*.tmp"), is it relative to the
                //     ignore file?

                String line;
                while ((line = lineReader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() <= 0 || line.charAt(0) == '#') {
                        continue;
                    }
                    boolean isIgnoreMatchType = true;
                    if (line.charAt(0) == '!' && line.length() > 1) {
                        isIgnoreMatchType = false;
                        line = line.substring(1).trim();
                    }
                    line = line.replace('\\', '/');
                    if (globMatch(line, checkpath)) {
                        return isIgnoreMatchType;
                    }
                }
            } finally {
                lineReader.close();
            }
        } catch (IOException e) {
            // problem reading; assume it's not ignored
            LOG.info(e);
            return false;
        }
        return false;
    }

    private boolean globMatch(@NotNull String line, @NotNull String checkpath) {
        // FIXME use a real glob matcher
        throw new IllegalStateException("not implemented");
    }


    /**
     * Search up the directory tree for the most applicable p4ignore file.
     *
     * @param file source file
     * @return the ignore file, or {@code null} if it wasn't found
     */
    @Nullable
    private FilePath findApplicableIgnoreFile(@NotNull FilePath file) {
        String ignoreFileName = getIgnoreFileName();
        if (ignoreFileName != null) {
            File prevDir = file.getIOFile();
            File f = prevDir.getParentFile();
            while (f != null && f.isDirectory() && !FileUtil.filesEqual(f, prevDir)) {
                File c = new File(f, ignoreFileName);
                if (c.exists() && c.isFile() && c.canRead()) {
                    return FilePathUtil.getFilePath(c);
                }
                prevDir = f;
                f = f.getParentFile();
            }
        }
        return null;
    }
}
