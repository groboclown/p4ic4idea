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

package p4ic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.SourceSet
import p4ic.ext.IdeaVersionExtension
import p4ic.tasks.ConfigureInstrumentation

import javax.annotation.Nonnull

class P4icVersionPlugin implements Plugin<Project> {
    public static final LOG = Logging.getLogger(P4icVersionPlugin)


    @Override
    void apply(Project project) {
        configureExtensions(project)
        project.sourceSets.all { SourceSet sourceSet ->
            ConfigureInstrumentation.configureInstrumentation(project, sourceSet, null, null)
        }
    }

    private static void configureExtensions(@Nonnull Project project) {
        LOG.info("Configuring extensions")
        project.extensions.add("ideaVersion", new IdeaVersionExtension(project))
    }
}
