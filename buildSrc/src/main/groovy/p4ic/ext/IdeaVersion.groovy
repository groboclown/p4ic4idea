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

import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import p4ic.util.IdeaVersionUtil
import p4ic.util.P4icUtil

import javax.annotation.Nonnull

class IdeaVersion {
    private final static IdeaVersionLibMatcher[] VERSION_LIB_MATCHERS = [
            new IdeaVersion171_172_173(),
            new IdeaVersion181(),
            new IdeaVersion182_183(),
            new IdeaVersion192(),
            new IdeaVersion193(),
            new IdeaVersion201(),
            new IdeaVersion202(),
    ]

    @Nonnull
    private final String version

    @Nonnull
    private final Project project

    IdeaVersion(@Nonnull Project project, @Nonnull String version) {
        this.project = project
        this.version = version
    }

    boolean isLowest() {
        return version == IdeaVersionUtil.LOWEST_COMPATIBLE_VERSION_NAME
    }

    @Nonnull
    String getVersion() {
        return version
    }

    @Nonnull
    File getLibDir() {
        return P4icUtil.libDir(project, version)
    }

    @Nonnull
    ConfigurableFileCollection jars(@Nonnull List<String> names) {
        List<File> jarFiles = findJars(names)
        return project.files(jarFiles.toArray(new File[0]))
    }

    @Nonnull
    ConfigurableFileCollection jars(@Nonnull String... names) {
        List<File> jarFiles = findJars(Arrays.asList(names))
        return project.files(jarFiles.toArray(new File[0]))
    }

    @Nonnull
    List<File> findJars(@Nonnull Collection<String> names) {
        def libDir = getLibDir()
        if (!libDir.isDirectory()) {
            throw new GradleException("Unsupported IDEA version " + version)
        }
        // This may need to pull multiple files with the same base name.
        def matcher = getLibVersionMatcher()
        Set<String> notFound = new HashSet<>(names)
        Map<String, File> remainingLibs = new HashMap<>()
        project.fileTree(libDir).forEach {
            if (it.isFile() && it.name.endsWith(".jar")) {
                remainingLibs.put(it.getName(), it)
            }
        }

        List<File> ret = new ArrayList<>()
        names.forEach {
            def libSet = matcher.getNamedLib(it)
            if (libSet == null) {
                throw new GradleException("Failed to find IDEA version " + version + " jars for " + it)
            }
            def matched = libSet.find(remainingLibs)
            if (!matched.missed.isEmpty()) {
                throw new GradleException("Failed to find IDEA version " + version + " jars for " + it +
                        ": could not find jars " + matched.missed)
            }
            ret.addAll(matched.matched)
        }
        return ret
    }


    private IdeaVersionLibMatcher getLibVersionMatcher() {
        for (IdeaVersionLibMatcher matcher : VERSION_LIB_MATCHERS) {
            if (matcher.ideaVersionMatch.matcher(version).matches()) {
                return matcher
            }
        }
        throw new GradleException("Failed to find library name handler for IDEA version " + version)
    }

    private static String strip(@Nonnull String s, @Nonnull String suffix) {
        if (s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length())
        }
        throw new GradleException("`" + s + "` does not end with `" + suffix + "`")
    }
}
