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

package p4ic.util

import org.apache.tools.ant.util.DeweyDecimal
import org.apache.tools.ant.util.JavaEnvUtils
import org.gradle.api.Project
import p4ic.ext.IdeaVersion

import javax.annotation.Nonnull

class IdeaVersionUtil {
    public static final String LOWEST_COMPATIBLE_VERSION_NAME = "171"

    // See https://github.com/JetBrains/gradle-intellij-plugin/issues/235
    // JDK9 only builds using the tools from 182+
    public static final String LOWEST_COMPATIBLE_VERSION_NAME_JDK9 = "182"

    @Nonnull
    static IdeaVersion lowestCompatible(@Nonnull Project project) {
        return new IdeaVersion(project, LOWEST_COMPATIBLE_VERSION_NAME)
    }

    static boolean isJdk9() {
        return JavaEnvUtils.getParsedJavaVersion().isGreaterThanOrEqual(new DeweyDecimal(9))
    }

    @Nonnull
    static IdeaVersion lowestCompatibleBuildVersion(@Nonnull Project project) {
        if (isJdk9()) {
            // Due to module break up, this requires a different version of the IDE.
            return new IdeaVersion(project, LOWEST_COMPATIBLE_VERSION_NAME_JDK9)
        }
        return new IdeaVersion(project, LOWEST_COMPATIBLE_VERSION_NAME)
    }

    @Nonnull
    static Collection<IdeaVersion> getVersions(@Nonnull Project project) {
        List<IdeaVersion> ret = new ArrayList<>();
        File[] libDirs = P4icUtil.checkIsDir(P4icUtil.libDir(project)).listFiles()
        for (File f: libDirs) {
            if (f.isDirectory()) {
                // check if it's a decimal number
                try {
                    Integer.parseUnsignedInt(f.name, 10)
                } catch (NumberFormatException e) {
                    continue
                }
                File licenseFile = new File(f, "LICENSE.txt")
                if (licenseFile.exists() && licenseFile.isFile()) {
                    ret.add(new IdeaVersion(project, f.name))
                }
            }
        }
        return ret
    }
}
