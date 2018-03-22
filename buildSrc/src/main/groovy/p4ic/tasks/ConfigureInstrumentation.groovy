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

package p4ic.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import p4ic.ext.IdeaJarsExtension
import p4ic.ext.IdeaVersion

import javax.annotation.Nonnull
import javax.annotation.Nullable
import java.util.concurrent.Callable

class ConfigureInstrumentation {
    public static final LOG = Logging.getLogger(ConfigureInstrumentation)

    static Task configureInstrumentation(@Nonnull Project project, @Nonnull SourceSet sourceSet, @Nullable IdeaJarsExtension extraJars, @Nullable IdeaVersion version) {
        LOG.info("Creating instrument task named :" + project.name + ":" + sourceSet.getTaskName('instrument', 'code'))
        IntelliJInstrumentCodeTask instrumentTask = project.tasks.create(sourceSet.getTaskName('instrument', 'code'), IntelliJInstrumentCodeTask)
        instrumentTask.fromSourceSet(sourceSet, extraJars, version)
        instrumentTask.with {
            dependsOn sourceSet.classesTaskName

            conventionMapping('outputDir', {
                def output = sourceSet.output
                def classesDir = output.classesDirs.first()
                new File(classesDir.parentFile, "${sourceSet.name}-instrumented")
            })
        }

        LOG.info("Creating instrument task named :" + project.name + ":" + 'post' + instrumentTask.name.capitalize())
        def updateTask = project.tasks.create('post' + instrumentTask.name.capitalize())
        updateTask.with {
            dependsOn instrumentTask

            doLast {
                def sourceSetOutput = sourceSet.output

                def oldClassesDir = sourceSetOutput.classesDirs

                // Set the classes dir to the new one with the instrumented classes

                sourceSetOutput.classesDirs.from = instrumentTask.outputDir

                if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
                    // When we change the output classes directory, Gradle will automatically configure
                    // the test compile tasks to use the instrumented classes. Normally this is fine,
                    // however, it causes problems for Kotlin projects:

                    // The "internal" modifier can be used to restrict access to the same module.
                    // To make it possible to use internal methods from the main source set in test classes,
                    // the Kotlin Gradle plugin adds the original output directory of the Java task
                    // as "friendly directory" which makes it possible to access internal members
                    // of the main module.

                    // This fails when we change the classes dir. The easiest fix is to prepend the
                    // classes from the "friendly directory" to the compile classpath.
                    AbstractCompile testCompile = project.tasks.findByName('compileTestKotlin') as AbstractCompile
                    if (testCompile) {
                        testCompile.classpath = project.files(oldClassesDir, testCompile.classpath)
                    }
                }
            }
        }

        // Ensure that our task is invoked when the source set is built
        sourceSet.compiledBy(updateTask)
        return updateTask
    }

    static Task configureInstrumentation(@Nonnull Project project, @Nonnull JavaCompile compileTask,
             @Nonnull Callable<FileCollection> srcClasspath) {
        LOG.warn("Creating instrument task named :" + project.name + ":" + compileTask.name + 'Instrument')
        IntelliJInstrumentCodeTask instrumentTask = project.tasks.create(compileTask.name + 'Instrument', IntelliJInstrumentCodeTask)
        instrumentTask.fromJavaCompile(compileTask, srcClasspath)
        instrumentTask.with {
            dependsOn compileTask.name

            conventionMapping('outputDir', {
                def output = compileTask.outputs
                File classesDir = output.first()
                new File(classesDir.parentFile, "${compileTask.name}-instrumented")
            })
        }

        LOG.warn("Creating instrument task named :" + project.name + ":" + 'post' + instrumentTask.name.capitalize())
        def updateTask = project.tasks.create('post' + instrumentTask.name.capitalize())
        updateTask.with {
            dependsOn instrumentTask

            doLast {
                def sourceSetOutput = compileTask.outputs

                // def oldClassesDir = sourceSetOutput.classesDirs

                // Set the classes dir to the new one with the instrumented classes

                sourceSetOutput.classesDirs.from = instrumentTask.outputDir
                /*
                if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
                    // When we change the output classes directory, Gradle will automatically configure
                    // the test compile tasks to use the instrumented classes. Normally this is fine,
                    // however, it causes problems for Kotlin projects:

                    // The "internal" modifier can be used to restrict access to the same module.
                    // To make it possible to use internal methods from the main source set in test classes,
                    // the Kotlin Gradle plugin adds the original output directory of the Java task
                    // as "friendly directory" which makes it possible to access internal members
                    // of the main module.

                    // This fails when we change the classes dir. The easiest fix is to prepend the
                    // classes from the "friendly directory" to the compile classpath.
                    AbstractCompile testCompile = project.tasks.findByName('compileTestKotlin') as AbstractCompile
                    if (testCompile) {
                        testCompile.classpath = project.files(oldClassesDir, testCompile.classpath)
                    }
                }
                */
            }
        }
        return updateTask
    }
}
