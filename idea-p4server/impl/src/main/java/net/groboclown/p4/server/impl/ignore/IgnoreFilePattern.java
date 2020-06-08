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

package net.groboclown.p4.server.impl.ignore;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single line from an ignore file.
 */
public class IgnoreFilePattern {
    private static final String[] EMPTY = new String[0];

    private final String basePattern;
    private final PathPart rootPart;
    private final boolean isNegative;


    // the parse file is contained in this class to keep the parse logic for the file in one
    // place.
    static List<IgnoreFilePattern> parseFile(@NotNull Reader reader) throws IOException {
        BufferedReader lineReader =
                reader instanceof BufferedReader
                    ? (BufferedReader) reader
                    : new BufferedReader(reader);
        // Note: there is now a "p4 ignores" command, and "p4 ignore" is now
        // also built into the API for newer servers.  For caching and UI display, we still need this.

        // File format:
        // (from https://www.perforce.com/perforce/r15.2/manuals/cmdref/P4IGNORE.html)
        // (Clarifications at http://forums.perforce.com/index.php?/topic/4492-new-p4-ignore-functionality/page__gopid__18940#entry18940)
        // The syntax for ignore rules is not the same as Perforce syntax. Instead, it is similar to that used
        // by other versioning systems:
        //
        // * Rules are specified using local filepath syntax.  Unix style paths will work on Windows for cross
        // platform file support.
        //
        // * A # character at the beginning of a line denotes a comment
        //
        // * A ! character at the beginning of a line line excludes the file specification.These exclusions override rules
        // defined above it in the P4IGNORE file, but may be overridden by later rules.
        //
        // * A / (or \ on Windows) character at the beginning of a line causes the file specification to be
        // considered relative to the P4IGNORE file. This is useful when the rule must apply to files at
        // particular depots of the directory tree.
        //
        // * A / (or \ on Windows) character at the end of a line causes the file specification to only match
        // directories, and not files of the same name.
        //
        // * The * wildcard matches substrings.  Like the Perforce wildcard equivalent, it does not match path
        // separators; however, if it is not used as part of a path, the directory scanning nature of the rule may
        // make it appear to perform like the Perforce "..." wildcard.
        //
        // * The ** wildcard matches substrings including path separators. It is equivalent to the Perforce "..."
        // wildcard, which is not permitted. (Note: the "**" must match at least 1 wild card)
        //
        // For example:
        //
        // # Ignore.p4ignore files
        // .p4ignore
        //
        // # Ignore object files, shared libraries, executables
        // *.dll
        // *.so
        // *.exe
        // *.o
        //
        // # Ignore all HTML files except the readme file
        // *.html
        // !readme.html
        //
        // # Ignore the bin directory
        // bin/
        //
        // # Ignore the build.properties file in this directory
        // /build.properties
        //
        // # Ignore all text files in test directories
        // test/**.txt
        //


        // This description unfortunately does not describe these circumstances:
        //   - if a "!" is found after the matcher, is it still a match?
        //     (does the first match force a result?)
        //     TODO It looks like last match?
        //   - if a path is specified ("temp/*.tmp"), is it relative to the
        //     ignore file?  Assuming not; as per the .gitignore format
        //     (if a file matcher starts with a '/', then it is relative to
        //     the ignore files).

        List<IgnoreFilePattern> ret = new ArrayList<>();

        String line;
        while ((line = lineReader.readLine()) != null) {
            IgnoreFilePattern ifp = parseLinePattern(line);
            if (ifp != null) {
                ret.add(ifp);
            }
        }
        return ret;
    }

    @Nullable
    static IgnoreFilePattern parseLinePattern(final String line) {
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
            return new IgnoreFilePattern(line, createPattern(pattern), isNegative);
        }
        return null;
    }


    IgnoreFilePattern(@NotNull PathPart rootPart, boolean isNegative) {
        this(null, rootPart, isNegative);
    }


    IgnoreFilePattern(@Nullable String original, @NotNull PathPart rootPart, boolean isNegative) {
        this.basePattern = original;
        this.rootPart = rootPart;
        this.isNegative = isNegative;
    }


    public boolean isIgnoreMatchType() {
        return ! isNegative;
    }

    @Override
    public String toString() {
        return basePattern;
    }


    /**
     *
     * @param pathParts the path of the file to check, relative to the source ignore file.  Each
     *                  path element is its own item in the list.
     * @return true if the file matches this pattern; it does not mean that it's ignored.
     */
    public boolean matches(final List<String> pathParts) {
        PathPart next = rootPart;
        for (String pathPart: pathParts) {
            if (next == null) {
                return false;
            }
            PathNameMatchResult result = next.match(pathPart);
            if (result.isLastElementMatch) {
                return true;
            }
            if (!result.isMatch) {
                return false;
            }
            next = result.next;
        }
        return false;
    }

    private enum ParseState {
        END_PATH,
        STAR_PATH,
        JUST_FOUND_STAR_AT_END,
        JUST_FOUND_STAR_IN_MIDDLE,
        JUST_FOUND_STAR_STAR_AT_END,
        JUST_FOUND_STAR_STAR_IN_MIDDLE,
        TEXT_PATH
    }


    @NotNull
    static PathPart createPattern(@NotNull final String pattern) {
        if (pattern.length() <= 0) {
            throw new IllegalArgumentException("pattern cannot be empty");
        }

        // Split the pattern based on the path separator.  From there,
        // parse each part based on the '*' inclusion.

        // In order to make the splitting easier for us, just change separators to a single format.
        char[] pat = pattern.replace('\\', '/').replace(File.separatorChar, '/').toCharArray();
        int lastPos = pat.length - 1;

        // Special case: if the pattern starts with a '/', then start it with a StarStar matcher.
        boolean isAbsolute = false;
        int startPos = 0;
        while (startPos <= lastPos && pat[startPos] == '/' ) {
            isAbsolute = true;
            startPos++;
        }

        // work backwards through the pattern to assemble the parts with the next wiring correctly.
        PathPart ret = null;

        ParseState state = ParseState.END_PATH;
        // Special case: trailing slash means an implicit directory.
        {
            boolean isDir = false;
            while (lastPos > startPos && pat[lastPos] == '/') {
                isDir = true;
                lastPos--;
            }
            if (isDir) {
                ret = new StrictStarStarPart(null);
            }
        }

        List<String> pieces = new ArrayList<>();
        int lastPartPos = lastPos;
        while (lastPos >= startPos) {
            char c = pat[lastPos];
            switch (state) {
                case END_PATH:
                    if (c == '/') {
                        // double slashes - count as just one slash.
                        // adjust the positions to count this as the new end.
                        // don't need to adjust the pieces or state.
                        lastPartPos = lastPos;
                    } else if (c == '*') {
                        pieces.add(0, "*");
                        lastPartPos = lastPos;
                        state = ParseState.JUST_FOUND_STAR_AT_END;
                    } else {
                        state = ParseState.TEXT_PATH;
                    }
                    break;
                case JUST_FOUND_STAR_AT_END:
                    if (c == '*') {
                        // double star at end.
                        ret = new StrictStarStarPart(ret);
                        lastPartPos = lastPos;
                        pieces.clear();
                        state = ParseState.JUST_FOUND_STAR_STAR_AT_END;
                    } else if (c == '/') {
                        ret = new SingleAnyMatchPart(ret);
                        pieces.clear();
                        lastPartPos = lastPos;
                        state = ParseState.END_PATH;
                    } else {
                        state = ParseState.STAR_PATH;
                    }
                    break;
                case JUST_FOUND_STAR_IN_MIDDLE:
                    if (c == '*') {
                        // found a star star, with stuff after it.
                        // keep the stuff after it, marking it as a *a type.
                        ret = new StarMatchPart(ret, pieces.toArray(EMPTY), isFileSystemCaseSensitive());
                        lastPartPos = lastPos;
                        pieces.clear();
                        ret = new StrictStarStarPart(ret);
                        state = ParseState.JUST_FOUND_STAR_STAR_IN_MIDDLE;
                    } else if (c == '/') {
                        ret = new StarMatchPart(ret, pieces.toArray(EMPTY), isFileSystemCaseSensitive());
                        lastPartPos = lastPos;
                        pieces.clear();
                        state = ParseState.END_PATH;
                    } else {
                        state = ParseState.STAR_PATH;
                    }
                    break;
                case JUST_FOUND_STAR_STAR_AT_END:
                case JUST_FOUND_STAR_STAR_IN_MIDDLE:
                    if (c == '*') {
                        // ***, which is weird.  But okay.
                        pieces.add(0, "*");
                        lastPartPos = lastPos;
                        state = ParseState.JUST_FOUND_STAR_AT_END;
                    } else if (c == '/') {
                        // only star star in path.
                        lastPartPos = lastPos;
                        pieces.clear();
                        state = ParseState.END_PATH;
                    } else {
                        // counts as a* format.
                        lastPartPos = lastPos;
                        pieces.add(0, "*");
                        state = ParseState.STAR_PATH;
                    }
                    break;
                case STAR_PATH:
                    if (c == '*') {
                        pieces.add(0, new String(pat, lastPos + 1, lastPartPos - lastPos - 1));
                        pieces.add(0, "*");
                        lastPartPos = lastPos;
                        state = ParseState.JUST_FOUND_STAR_IN_MIDDLE;
                    } else if (c == '/') {
                        pieces.add(0, new String(pat, lastPos, lastPartPos - lastPos - 1));
                        ret = new StarMatchPart(ret, pieces.toArray(EMPTY), isFileSystemCaseSensitive());
                        lastPartPos = lastPos;
                        pieces.clear();
                        state = ParseState.END_PATH;
                    }
                    // else just keep going.
                    break;
                case TEXT_PATH:
                    if (c == '*') {
                        pieces.add(0, new String(pat, lastPos + 1, lastPartPos - lastPos));
                        pieces.add(0, "*");
                        lastPartPos = lastPos;
                        state = ParseState.JUST_FOUND_STAR_IN_MIDDLE;
                    } else if (c == '/') {
                        ret = new ExactMatchPart(ret,
                                new String(pat, lastPos + 1, lastPartPos - lastPos),
                                isFileSystemCaseSensitive());
                        lastPartPos = lastPos;
                        pieces.clear();
                        state = ParseState.END_PATH;
                    }
                    // else just keep going
                    break;
            }
            lastPos--;
        }
        switch (state) {
            case JUST_FOUND_STAR_AT_END:
                ret = new SingleAnyMatchPart(ret);
                break;
            case JUST_FOUND_STAR_IN_MIDDLE:
                ret = new StarMatchPart(ret, pieces.toArray(EMPTY), isFileSystemCaseSensitive());
                break;
            case STAR_PATH:
                pieces.add(0, new String(pat, startPos, lastPartPos - startPos));
                ret = new StarMatchPart(ret, pieces.toArray(EMPTY), isFileSystemCaseSensitive());
                break;
            case TEXT_PATH:
                ret = new ExactMatchPart(ret, new String(pat, startPos, lastPartPos - startPos),
                        isFileSystemCaseSensitive());
                break;
            case JUST_FOUND_STAR_STAR_AT_END:
            case JUST_FOUND_STAR_STAR_IN_MIDDLE:
            case END_PATH:
                // do nothing
                break;
        }
        if ((!isAbsolute && !(ret instanceof StrictStarStarPart)) || (isAbsolute && ret == null)) {
            ret = new StrictStarStarPart(ret);
        }
        if (ret == null) {
            // Weird state.  Allow it, though.
            ret = new ExactMatchPart(null, "", isFileSystemCaseSensitive());
        }
        return ret;
    }


    private static boolean isFileSystemCaseSensitive() {
        // This isn't 100% true - you can configure windows file system to be case sensitive.
        return !SystemInfo.isWindows;
    }

    static class PathNameMatchResult {
        final boolean isMatch;
        final boolean requiresMore;
        final boolean isLastElementMatch;
        final PathPart next;

        PathNameMatchResult(boolean isMatch, @Nullable PathPart next) {
            this(isMatch, next != null, next);
        }

        PathNameMatchResult(boolean isMatch, boolean requiresMore, @Nullable PathPart next) {
            this.isMatch = isMatch;
            this.requiresMore = requiresMore;
            this.next = next;
            this.isLastElementMatch = isMatch && (next == null || !requiresMore);
        }
    }

    private static final PathNameMatchResult NOT_MATCH = new PathNameMatchResult(false, false, null);

    abstract static class PathPart {
        final PathPart next;

        protected PathPart(@Nullable PathPart next) {
            this.next = next;
        }

        @NotNull
        abstract PathNameMatchResult match(@NotNull String name);
    }


    // Matching patterns:
    //    if a pattern ends with a '/', then use StrictStarStarPart with no next.
    //    /a/ -> ExactMatchPart
    //    /*/ -> SingleAnyMatchPart
    //    /**/ -> StrictStarStarPart
    //    /a**b/ -> /a*/**/*b/ -> StarMatchPart, StrictStarStarPart, StarMatchPart
    //    /a**/ -> /a*/**/ -> StarMatchPart, StrictStarStarPart
    //    /**b/ -> /**/*b/ -> StrictStarStarPart, StarMatchPart
    //    /a*/ -> StarMatchPart
    //    /*b/ -> StarMatchPart
    //    /a*b/ -> StarMatchPart
    //    /*a*/ -> StarMatchPart


    static class SingleAnyMatchPart extends PathPart {
        protected SingleAnyMatchPart(@Nullable PathPart next) {
            super(next);
        }

        @NotNull
        @Override
        PathNameMatchResult match(@NotNull String name) {
            return new PathNameMatchResult(true, next);
        }
    }


    static class ExactMatchPart extends PathPart {
        private final String match;
        private final boolean caseInsensitive;

        protected ExactMatchPart(@Nullable PathPart next, @NotNull String match, boolean caseInsensitive) {
            super(next);
            this.match = match;
            this.caseInsensitive = caseInsensitive;
        }

        @NotNull
        @Override
        PathNameMatchResult match(@NotNull String name) {
            boolean isMatch = caseInsensitive ? name.equalsIgnoreCase(match) : name.equals(match);
            return isMatch ? new PathNameMatchResult(true, next) : NOT_MATCH;
        }
    }

    // If P4 ignore ever learns character sets ([ab] meaning a or b), then this will need to take something
    // other than String for the matches.
    static class StarMatchPart extends PathPart {
        final String[] matches;
        private final boolean caseInsensitive;

        // matches implies a '*' at the end, but it isn't there.
        StarMatchPart(@Nullable PathPart next, @NotNull String[] matches, boolean caseInsensitive) {
            super(next);
            if (matches.length < 2) {
                throw new IllegalArgumentException("matches must have at least one star and one non-star");
            }
            this.matches = matches;
            this.caseInsensitive = caseInsensitive;
            if (caseInsensitive) {
                for (int i = 0; i < matches.length; i++) {
                    matches[i] = matches[i].toLowerCase();
                }
            }
        }

        @NotNull
        @Override
        PathNameMatchResult match(@NotNull String name) {
            String remaining = caseInsensitive ? name.toLowerCase() : name;
            return isRecursiveMatches(0, matches.length - 1, remaining)
                    ? new PathNameMatchResult(true, next)
                    : NOT_MATCH;
        }


        boolean isRecursiveMatches(int startMatchPos, int endMatchPos, String remaining) {
            assert startMatchPos >= 0;
            assert endMatchPos < matches.length;
            assert startMatchPos <= endMatchPos;

            // peel away until we get to a non-star.
            // - If match is [a, *], the hasStartStar loop will first not increment startMatchPos.
            //   It will see that remaining starts with the 'a' (if not, quit), so increment to the
            //   '*'.  This equals the endMatchPos, but we'll keep looping.
            //   The second loop will match a star, and keep looking.  Now the startMatchPos > end,
            //   so it will quit with a yes.
            // - If match is [*, a], the hasStartStar will just stop with a hasStartStar = true.
            //   Then, the hasEndStar will find a non-star at the end, find that remaining
            //   matches the end string (if not, quit), so decrement the end by one (start == end now), and loop again.
            //   The second hasEndStar loop will find a star, decrement the end (start > end now), and quit with a yes.
            // - If the match is [*, a, *], the hasStartStar will end with startMatchPos = 1, and hasStartStar = true.
            //   The hasEndStar will end with endMatchPos = 1, and hasEndStar = true.
            // - If the match is [a, *, b, *] or [*, b, *, a], then it's essentially just like the [*, a, *] pattern.
            // - If the match is [*, a, *, b, *], or something more complicated, now we're in trouble.  The
            //   "complicated" section enters with start pointing at "a" and end pointing at "b".
            //   Now we need to recurse for each "a" and "b" position within the remaining string, where the
            //   "a" position < "b" position.


            boolean hasStartStar = false;
            while (!hasStartStar) {
                while (startMatchPos <= endMatchPos && "*".equals(matches[startMatchPos])) {
                    startMatchPos++;
                    hasStartStar = true;
                }
                if (startMatchPos > endMatchPos) {
                    // It's just stars
                    return true;
                }
                if (!hasStartStar) {
                    if (!remaining.startsWith(matches[startMatchPos])) {
                        return false;
                    }
                    remaining = remaining.substring(matches[startMatchPos].length());
                    startMatchPos++;
                }
            }

            // it's possible at this point to have start pos == end pos.

            boolean hasEndStar = false;
            while (!hasEndStar) {
                while (endMatchPos >= startMatchPos && "*".equals(matches[endMatchPos])) {
                    endMatchPos--;
                    hasEndStar = true;
                }
                if (startMatchPos > endMatchPos) {
                    // It's just stars
                    return true;
                }
                if (!hasEndStar) {
                    if (!remaining.endsWith(matches[endMatchPos])) {
                        return false;
                    }
                    remaining = remaining.substring(0, remaining.length() - matches[endMatchPos].length());
                    endMatchPos--;
                }
            }

            // Easy out.  Stars surrounding a single string.
            if (startMatchPos == endMatchPos) {
                // Stars surrounding a single text bit.
                return remaining.contains(matches[startMatchPos]);
            }

            // Complicated.
            // Find all the indexes of the startMatchPos within remaining, and find all the indexes of the
            // endMatchPos within remaining.  Then, run a recurse match on each combination where
            // start index < end index.

            List<Integer> startMatchIndicies = matchPositions(remaining, matches[startMatchPos]);
            List<Integer> endMatchIndicies = matchPositions(remaining, matches[endMatchPos]);
            for (int si = 0; si < startMatchIndicies.size(); si++) {
                // Point the starting string index to where the match completes.
                int sip = startMatchIndicies.get(si) + matches[startMatchPos].length();
                for (int ei = 0; ei < endMatchIndicies.size(); ei++) {
                    // Point the ending string index to where the match starts.
                    int eip = endMatchIndicies.get(ei);
                    if (sip <= eip) {
                        if (isRecursiveMatches(startMatchPos + 1, endMatchPos - 1,
                                remaining.substring(sip, eip))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }


        List<Integer> matchPositions(String str, String match) {
            int len = str.length();
            List<Integer> ret = new ArrayList<>(len);
            int p = 0;
            while (p < len) {
                int mp = str.indexOf(match, p);
                if (mp >= p) {
                    ret.add(mp);
                    p = mp + 1;
                } else {
                    break;
                }
            }
            return ret;
        }
    }

    /** slash '**' slash.  Note that anything that has something '**' something can be split into this and others. */
    static class StrictStarStarPart extends PathPart {
        StrictStarStarPart(@Nullable PathPart next) {
            super(next);
        }

        @NotNull
        @Override
        PathNameMatchResult match(@NotNull String name) {
            if (next == null) {
                // Any child will do
                return new PathNameMatchResult(true, false, this);
            }
            // Totally non-greedy match.
            PathNameMatchResult nextMatch = next.match(name);
            if (nextMatch.isMatch) {
                return nextMatch;
            }
            // Because we have a next-match, this requires another path element.
            return new PathNameMatchResult(true, true, this);
        }
    }
}
