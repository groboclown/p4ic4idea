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

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single line from an ignore file.
 */
public class IgnoreFilePattern {
    // regex is overkill, but it's easy and works.
    private final Pattern pattern;
    private final boolean isNegative;


    public static List<IgnoreFilePattern> parseFile(@NotNull VirtualFile ignoreFile) throws IOException {
        final BufferedReader lineReader =
                new BufferedReader(new InputStreamReader(ignoreFile.getInputStream()));
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
            // foo.txt     Ignore files called "foo.txt"
            // *.exe       Ignore all executables
            // !bar.exe    Exclude bar.exe from being ignored

            // This description unfortunately does not describe these circumstances:
            //   - if a "!" is found after the matcher, is it still a match?
            //     (does the first match force a result?)
            //   - if a path is specified ("temp/*.tmp"), is it relative to the
            //     ignore file?  Assuming not; as per the .gitignore format
            //     (if a file matcher starts with a '/', then it is relative to
            //     the ignore files).

            List<IgnoreFilePattern> ret = new ArrayList<IgnoreFilePattern>();

            String line;
            while ((line = lineReader.readLine()) != null) {
                IgnoreFilePattern ifp = parseLinePattern(line);
                if (ifp != null) {
                    ret.add(ifp);
                }
            }
            return ret;
        } finally {
            lineReader.close();
        }
    }

    @Nullable
    public static IgnoreFilePattern parseLinePattern(final String line) {
        String pattern = line.trim();
        if (pattern.length() <= 0 || pattern.charAt(0) == '#') {
            return null;
        }
        boolean isNegative = false;
        if (pattern.charAt(0) == '!' && pattern.length() > 1) {
            isNegative = true;
            pattern = pattern.substring(1).trim();
        }
        if (pattern.length() > 0) {
            return new IgnoreFilePattern(createPattern(pattern), isNegative);
        }
        return null;
    }


    public IgnoreFilePattern(@NotNull Pattern pattern, boolean isNegative) {
        this.pattern = pattern;
        this.isNegative = isNegative;
    }


    /**
     *
     * @return true if a pattern match means that the file is most definitely not ignored.
     */
    public boolean isNegative() {
        return isNegative;
    }


    public boolean isIgnoreMatchType() {
        return ! isNegative;
    }


    /**
     *
     * @param pathPart the file to check, which has been prepared by a call to {@link #preparePath(FilePath, FilePath)}.
     * @return true if the file matches this pattern; it does not mean that it's ignored.
     */
    public boolean matches(final String pathPart) {
        final Matcher matcher = pattern.matcher(pathPart);
        return matcher.matches();
    }


    static boolean isSeparator(char c) {
        return c == '/' || c == '\\' || c == File.separatorChar;
    }


    @NotNull
    private static Pattern createPattern(final String pattern) {
        // First, normalize the pattern
        StringBuilder patternGlob = new StringBuilder();
        if (isSeparator(pattern.charAt(0))) {
            patternGlob.append('^');
        }
        int pos = 0;
        char[] buff = pattern.toCharArray();
        // strip off leading slashes
        while (pos < buff.length && isSeparator(buff[pos])) {
            pos++;
        }
        for (; pos < buff.length; pos++) {
            char ch = buff[pos];
            if (isSeparator(ch)) {
                patternGlob.append("\\/");
            } else if (ch == '*') {
                // non-greedy splat pattern
                patternGlob.append(".*?");
            } else if (Character.isWhitespace(ch)) {
                patternGlob.append("\\s");
            } else if (Character.isLetterOrDigit(ch)) {
                if (isFileSystemCaseSensitive()) {
                    ch = Character.toLowerCase(ch);
                }
                patternGlob.append(ch);
            } else {
                // assume that it just needs to be escaped
                patternGlob.append('\\').append(ch);
            }
        }
        patternGlob.append("$");
        return Pattern.compile(patternGlob.toString());
    }


    private static boolean isFileSystemCaseSensitive() {
        return SystemInfo.isWindows;
    }

    @Nullable
    static String preparePath(@NotNull FilePath file, @NotNull FilePath ignoreFile) {
        // Asserting pre-conditions
        if (ignoreFile.getParentPath() == null) {
            return null;
        }
        final String baseDir = ignoreFile.getParentPath().getIOFile().getAbsolutePath();
        final String matchPath = file.getIOFile().getAbsolutePath();
        if (! matchPath.startsWith(baseDir)) {
            return null;
        }

        char[] buff = matchPath.toCharArray();
        int pos = baseDir.length();
        // strip off leading path
        while (pos < buff.length && isSeparator(buff[pos])) {
            pos++;
        }

        StringBuilder ret = new StringBuilder();
        // simple conversion
        for (; pos < buff.length; pos++) {
            char ch = buff[pos];
            if (isSeparator(ch)) {
                ret.append('/');
            } else if (isFileSystemCaseSensitive()) {
                ret.append(Character.toLowerCase(ch));
            } else {
                ret.append(ch);
            }
        }
        return ret.toString();
    }
}