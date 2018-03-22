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
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import p4ic.ext.IdeaJarsExtension
import p4ic.ext.IdeaVersion
import p4ic.tasks.ConfigureInstrumentation
import p4ic.util.IdeaVersionUtil

import javax.annotation.Nonnull
import java.util.concurrent.Callable

class P4icCompatBadPlugin implements Plugin<Project> {
    public static final LOG = Logging.getLogger(P4icCompatBadPlugin)

    public static final String JAR_EXTENSION_NAME = "ideaCompat"

    @Override
    void apply(Project project) {
        configureExtensions(project)
        configureTasks(project)
    }

    private static void configureExtensions(@Nonnull Project project) {
        LOG.info("Configuring extensions")
        project.extensions.create(JAR_EXTENSION_NAME, IdeaJarsExtension)
    }

    private static void configureTasks(@Nonnull Project project) {
        IdeaVersionUtil.getVersions(project).forEach {
            LOG.info("Configuring additional compile and test tasks for compatibility with v" + it.version)

            configureConfigurations(project, it)
            configureVersionedTasks(project, it)
        }
        configureDefaultTasks(project, IdeaVersionUtil.lowestCompatible(project))
    }

    private static void configureConfigurations(@Nonnull Project project, @Nonnull IdeaVersion version) {
        String suffix = "Idea" + version.version
        IdeaJarsExtension ext = project.extensions.getByName(JAR_EXTENSION_NAME) as IdeaJarsExtension
        //JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class)

        ConfigurationContainer configurations = project.getConfigurations()

        Configuration ideaConfiguration = project.configurations.create(suffix)
        project.dependencies.add(ideaConfiguration.name, ext.getJarsFor(version))

        Configuration compileConfiguration = configurations.create(JavaPlugin.API_CONFIGURATION_NAME + suffix)
        compileConfiguration.extendsFrom(
                configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME),
                ideaConfiguration)
        compileConfiguration.setDescription("Compile dependencies for Idea " + version.version)

        Configuration testImplementationConfiguration = configurations.create(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME + suffix)
        testImplementationConfiguration.extendsFrom(
                configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME),
                ideaConfiguration)
        testImplementationConfiguration.setDescription("Test compile dependencies for Idea " + version.version)

        Configuration testRuntimeConfiguration = configurations.create(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME + suffix)
        testRuntimeConfiguration.extendsFrom(
                configurations.getByName(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME),
                ideaConfiguration
        )
        testRuntimeConfiguration.setDescription("Test runtime dependencies for Idea " + version.version)
    }

    private static void configureVersionedTasks(@Nonnull Project project, @Nonnull IdeaVersion version) {
        String suffix = "Idea" + version.version
        IdeaJarsExtension ext = project.extensions.getByName(JAR_EXTENSION_NAME) as IdeaJarsExtension

        JavaCompile baseCompile = project.tasks.getByPath(JavaPlugin.COMPILE_JAVA_TASK_NAME) as JavaCompile
        JavaCompile baseCompileTest = project.tasks.getByPath(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME) as JavaCompile
        Test baseTest = project.tasks.getByPath(JavaPlugin.TEST_TASK_NAME) as Test


        JavaCompile compile = project.tasks.create(baseCompile.name + suffix, JavaCompile)
        compile.destinationDir = child(project.buildDir, "classes", "java", "main" + suffix)
        compile.source(baseCompile.source)
        compile.sourceCompatibility = baseCompile.sourceCompatibility
        compile.targetCompatibility = baseCompile.targetCompatibility
        Callable<FileCollection> compileCp = new Callable<FileCollection>() {
            FileCollection call() throws Exception {
                FileCollection jars = ext.getJarsFor(version)
                LOG.info("Adding IDEA jars (" + ext.jarNames + ") to classpath: " + jars.files)
                return jars + baseCompile.classpath
            }
        }
        compile.getConventionMapping().map("classpath", compileCp)
        Task compileInst = ConfigureInstrumentation.configureInstrumentation(project, compile, compileCp)
        baseCompile.dependsOn compileInst


        JavaCompile compileTest = project.tasks.create(baseCompileTest.name + suffix, JavaCompile)
        compileTest.dependsOn compileInst
        compileTest.destinationDir = child(project.buildDir, "classes", "java", "test" + suffix)
        compileTest.source(baseCompileTest.source)
        compileTest.sourceCompatibility = baseCompileTest.sourceCompatibility
        compileTest.targetCompatibility = baseCompileTest.targetCompatibility
        Callable<FileCollection> compileTestCp = new Callable<FileCollection>() {
            @Override
            FileCollection call() throws Exception {
                FileCollection jars = ext.getJarsFor(version)
                LOG.info("Adding IDEA jars (" + ext.jarNames + ") to test classpath: " + jars.files)
                return compileInst.outputs.files + jars + baseCompileTest.classpath
            }
        }
        compileTest.getConventionMapping().map("classpath", compileTestCp)
        Task compileTestInst = ConfigureInstrumentation.configureInstrumentation(project, compileTest, compileTestCp)
        baseCompileTest.dependsOn compileTestInst


        Test test = project.tasks.create(baseTest.name + suffix, Test)
        test.binResultsDir = child(project.buildDir, "test-results", "test" + suffix, "binary")
        test.testClassesDirs = project.files(compileTest.destinationDir)
        test.workingDir = child(project.buildDir, "test-work", "test" + suffix)
        test.dependsOn compileTestInst
        Callable<FileCollection> testCp = new Callable<FileCollection>() {
            @Override
            FileCollection call() throws Exception {
                FileCollection jars = ext.getJarsFor(version)
                LOG.info("Adding IDEA jars (" + ext.jarNames + ") to classpath: " + jars.files)
                return compileInst.outputs.files + compileTestInst.outputs.files + jars + baseTest.classpath
            }
        }
        test.getConventionMapping().map("classpath", testCp)
        baseTest.dependsOn test
    }

    private static void configureDefaultTasks(@Nonnull Project project, @Nonnull IdeaVersion version) {
        IdeaJarsExtension ext = project.extensions.getByName(JAR_EXTENSION_NAME) as IdeaJarsExtension
        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class)


        JavaCompile compile = project.tasks.getByPath(JavaPlugin.COMPILE_JAVA_TASK_NAME) as JavaCompile
        Callable<FileCollection> compileCp = new Callable<FileCollection>() {
            FileCollection call() throws Exception {
                FileCollection jars = ext.getJarsFor(version)
                LOG.warn("Adding IDEA jars (" + ext.jarNames + ") to classpath: " + jars.files)
                return jars + javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getCompileClasspath()
            }
        }
        compile.getConventionMapping().map("classpath", compileCp)
        Task compileInst = ConfigureInstrumentation.configureInstrumentation(project,
                javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME),
                ext,
                version)


        JavaCompile compileTest = project.tasks.getByPath(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME) as JavaCompile
        Callable<FileCollection> compileTestCp = new Callable<FileCollection>() {
            @Override
            FileCollection call() throws Exception {
                FileCollection jars = ext.getJarsFor(version)
                LOG.warn("Adding IDEA jars (" + ext.jarNames + ") to test classpath: " + jars.files)
                return compileInst.outputs.files + jars + javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getCompileClasspath()
            }
        }
        compileTest.getConventionMapping().map("classpath", compileTestCp)
        compileTest.dependsOn compileInst
        Task compileTestInst = ConfigureInstrumentation.configureInstrumentation(project,
                javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME),
                ext,
                version)


        Test test = project.tasks.getByPath(JavaPlugin.TEST_TASK_NAME) as Test
        Callable<FileCollection> testCp = new Callable<FileCollection>() {
            @Override
            FileCollection call() throws Exception {
                FileCollection jars = ext.getJarsFor(version)
                LOG.info("Adding IDEA jars (" + ext.jarNames + ") to classpath: " + jars.files)
                return compileInst.outputs.files + compileTestInst.outputs.files + jars +
                        javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getRuntimeClasspath()
            }
        }
        test.getConventionMapping().map("classpath", testCp)
        test.dependsOn compileTestInst
    }

    private static File child(File parent, String... sub) {
        File r = parent
        for (String s : sub) {
            r = new File(r, s)
        }
        return r
    }
}
