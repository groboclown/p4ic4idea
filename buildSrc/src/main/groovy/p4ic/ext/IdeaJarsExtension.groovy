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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.AbstractFileCollection
import org.gradle.api.tasks.TaskDependency

import javax.annotation.Nonnull

class IdeaJarsExtension {
    private final List<String> jars = new ArrayList<>()

    void uses(String... jarNames) {
        jars.addAll(Arrays.asList(jarNames))
    }

    ConfigurableFileCollection getJarsFor(@Nonnull IdeaVersion version) {
        return version.jars(jars.toArray(new String[0]))
    }

    List<String> getJarNames() {
        return jars
    }

    // This seems to cause NPEs
    FileCollection lazy(@Nonnull final IdeaVersion version) {
        return new AbstractFileCollection() {
            @Override
            Set<File> getFiles() {
                // Wait for the absolute last moment!
                List<String> names = getJarNames()
                if (names.isEmpty()) {
                    throw new GradleException("Asked for the idea v" + version.version + " jars before they were ready")
                }
                return getJarsFor(version).getFiles()
            }

            @Override
            boolean isEmpty() {
                return getJarNames().isEmpty()
            }

            @Override
            String getDisplayName() {
                return "idea-jars-v" + version.version
            }
        }
    }
}
