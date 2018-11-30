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

package p4ic.ext

import org.gradle.api.GradleException

import javax.annotation.Nonnull
import java.util.regex.Matcher
import java.util.regex.Pattern

class IdeaVersion17x implements IdeaVersionLibMatcher {
    private static final String[] VERSION_TYPE_SUFFIXES = [
            "-SNAPSHOT", "-patched"
    ]
    private static final Pattern VERSION_NUMBER = Pattern.compile("-\\d+(\\.\\d+)*\$")
    private static final Pattern IDE_VERSION = Pattern.compile("^17\\d\$")

    /**
     * Checks if the filename is in the remaining list.  If it is, the item is
     * removed from the remaining list.  The filename ends with ".jar"
     *
     * @param filename
     * @param remaining
     * @return
     */
    @Override
    Pattern getIdeaVersionMatch() {
        return IDE_VERSION
    }

    String[] matches(final String filename, @Nonnull final Collection<String> choices) {
        if (filename == null) {
            return null
        }

        // Check fully qualified name
        if (choices.contains(filename)) {
            return [filename]
        }

        // Check without .jar
        def shortName = strip(filename, ".jar")
        if (choices.contains(shortName)) {
            return [shortName]
        }

        // Check without some version suffixes
        for (String vs: VERSION_TYPE_SUFFIXES) {
            if (shortName.endsWith(vs)) {
                shortName = strip(shortName, vs)
                if (choices.contains(shortName)) {
                    return [shortName]
                }
            }
        }

        // Check for a version number
        Matcher m = VERSION_NUMBER.matcher(shortName)
        if (m.find()) {
            shortName = shortName.substring(0, m.start())
            if (choices.contains(shortName)) {
                return [shortName]
            }
        }

        return null
    }

    @Override
    boolean ignoredChoice(@Nonnull String libChoiceName) {
        return false
    }

    private static String strip(@Nonnull String s, @Nonnull String suffix) {
        if (s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length())
        }
        throw new GradleException("`" + s + "` does not end with `" + suffix + "`")
    }
}
