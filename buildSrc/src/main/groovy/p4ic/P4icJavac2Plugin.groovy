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
import p4ic.tasks.ConfigureInstrumentation

import javax.annotation.Nonnull

class P4icJavac2Plugin implements Plugin<Project> {
    public static final LOG = Logging.getLogger(P4icJavac2Plugin)


    @Override
    void apply(Project project) {
        configureTasks(project)
    }

    protected static void configureTasks(Project project) {
        configureInstrumentation(project)
    }

    private static void configureInstrumentation(@Nonnull Project project) {
        LOG.info("Configuring IntelliJ compile tasks")
        project.sourceSets.all { SourceSet sourceSet ->
            ConfigureInstrumentation.configureInstrumentation(project, sourceSet, null, null)
        }
    }

}
