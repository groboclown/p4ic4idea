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

class IdeaVersion181 implements IdeaVersionLibMatcher {
    private static final String[] VERSION_TYPE_SUFFIXES = [
        "-SNAPSHOT", "-patched"
    ]

    // May want to eventually reverse this mapping, and just have an explicit list of new to old names.
    // Or maybe just alter the names of the jars in the old versions, and record that in their README.md files.
    private static final String[] INTELLIJ_181_PREFIXES = [
        "intellij.platform.", "intellij."
    ]
    private static final Map<String, String> INTELLIJ_181_SPECIAL_MAPPINGS = [
        // Oddball mappings with no direct rename schemes
        "intellij.platform.vcs.core" : "vcs-api-core",
        "intellij.platform.ide" : "platform-api",
        "intellij.platform.ide.impl" : "platform-impl",
        "intellij.java" : "openapi",
        "intellij.platform.editor" : "editor-ui-api",
        "intellij.java.guiForms.rt" : "forms_rt",
        "intellij.java.rt" : "java-runtime",
        "intellij.java.compiler.instrumentationUtil" : "instrumentation-util",
        "intellij.java.compiler.antTasks" : "javac2",
        "intellij.java.guiForms.compiler" : "forms-compiler",
    ]
    private static final Pattern VERSION_NUMBER = Pattern.compile("-\\d+(\\.\\d+)*\$")
    private static final Pattern IDEA_VERSION = Pattern.compile("^181\$")

    @Override
    Pattern getIdeaVersionMatch() {
        return IDEA_VERSION
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
        if (shortName == 'kotlin-stdlib') {
            shortName = 'kotlin-runtime'
        }
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

        // Check for Idea 18x naming convention
        def idea18Name = INTELLIJ_181_SPECIAL_MAPPINGS[shortName]
        if (choices.contains(idea18Name)) {
            return [idea18Name]
        }
        for (String ip: INTELLIJ_181_PREFIXES) {
            if (shortName.startsWith(ip)) {
                idea18Name = shortName.substring(ip.length()).replace('.', '-')
                if (choices.contains(idea18Name)) {
                    return [idea18Name]
                }
                // API can be hidden...
                idea18Name += '-api'
                if (choices.contains(idea18Name)) {
                    return [idea18Name]
                }
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
