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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test
import p4ic.ext.IdeaJarsExtension
import p4ic.ext.IdeaVersion
import p4ic.tasks.ConfigureInstrumentation
import p4ic.util.IdeaVersionUtil

import javax.annotation.Nonnull

class P4icCompatPlugin implements Plugin<Project> {
    public static final LOG = Logging.getLogger(P4icCompatPlugin)

    public static final String API_JAR_EXTENSION_NAME = "ideaApi"
    public static final String IMPL_JAR_EXTENSION_NAME = "ideaImplementation"

    public static final String BASE_API_CONFIGURATION = "compatApi"
    public static final String BASE_IMPL_CONFIGURATION = "compatImplementation"
    public static final String BASE_TEST_CONFIGURATION = "compatTest"


    @Override
    void apply(Project project) {
        configureExtensions(project)
        configureVersions(project)
        project.sourceSets.all { SourceSet sourceSet ->
            ConfigureInstrumentation.configureInstrumentation(project, sourceSet, null, null)
        }
    }

    private static void configureExtensions(@Nonnull Project project) {
        LOG.info("Configuring extensions")
        project.extensions.create(API_JAR_EXTENSION_NAME, IdeaJarsExtension)
        project.extensions.create(IMPL_JAR_EXTENSION_NAME, IdeaJarsExtension)
    }

    private static void configureVersions(@Nonnull Project project) {
        configureBaseConfigurations(project)
        IdeaVersionUtil.getVersions(project).forEach {
            LOG.info("Configuring additional compile and test tasks for compatibility with v" + it.version)

            if (it.isLowest()) {
                configureDefaultConfigurations(project, it)
            } else {
                configureSourceSets(project, it)
                configureTasks(project, it)
                configureConfigurations(project, it)
            }
            configureDependencies(project, it)
        }
    }

    private static void configureBaseConfigurations(Project project) {
        ConfigurationContainer configurations = project.getConfigurations()

        Configuration api = configurations.create(BASE_API_CONFIGURATION)
        Configuration main = configurations.create(BASE_IMPL_CONFIGURATION)
        main.extendsFrom api
        Configuration test = configurations.create(BASE_TEST_CONFIGURATION)
        test.extendsFrom main
    }

    private static void configureDefaultConfigurations(@Nonnull Project project, @Nonnull IdeaVersion version) {
        ConfigurationContainer configurations = project.getConfigurations()

        Configuration apiConfig = configurations.create(getApiConfigName(version))
        Configuration implConfig = configurations.create(getImplementationConfigName(version))

        configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME).extendsFrom(
                configurations.getByName(BASE_API_CONFIGURATION),
                apiConfig
        )
        configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(
                configurations.getByName(BASE_IMPL_CONFIGURATION),
                implConfig
        )
        configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(
                configurations.getByName(BASE_TEST_CONFIGURATION)
        )
    }

    private static void configureSourceSets(Project project, IdeaVersion version) {
        String suffix = getId(version)
        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class)
        SourceSet main = javaConvention.sourceSets.create("main" + suffix)
        main.java {
            srcDirs = [child(project.projectDir, "src", "main", "java")]
            outputDir = project.file(child(project.buildDir, "classes", "java" + suffix))
        }
        main.resources {
            srcDirs = [child(project.projectDir, "src", "main", "resources")]
            outputDir = project.file(child(project.buildDir, "resources", "main" + suffix))
        }

        SourceSet test = javaConvention.sourceSets.create("test" + suffix)
        test.java {
            srcDirs = [child(project.projectDir, "src", "test", "java")]
            outputDir = project.file(child(project.buildDir, "classes", "test" + suffix))
        }
        test.resources {
            srcDirs = [child(project.projectDir, "src", "main", "resources")]
            outputDir = project.file(child(project.buildDir, "resources", "test" + suffix))
        }
    }

    private static void configureTasks(Project project, IdeaVersion version) {
        String suffix = getId(version)
        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class)
        Test test = project.tasks.create("test" + suffix, Test)
        test.dependsOn "compileTest" + suffix + "Java", "processTest" + suffix + "Resources"
        test.testClassesDirs = project.files(javaConvention.sourceSets.getByName("test" + suffix).java.outputDir)
        test.binResultsDir = project.file(child(project.buildDir, "reports", "tests" + suffix, "binary"))
        test.workingDir = project.file(child(project.buildDir, "test-out", "tests" + suffix))

        project.afterEvaluate {
            project.tasks.getByName("compileJava").dependsOn "compileMain" + suffix + "Java"
            project.tasks.getByName("processResources").dependsOn "processMain" + suffix + "Resources"
            project.tasks.getByName("compileTestJava").dependsOn "compileTest" + suffix + "Java"
            project.tasks.getByName("processTestResources").dependsOn "processTest" + suffix + "Resources"
            project.tasks.getByName("test").dependsOn "test" + suffix
            (project.tasks.getByName("test" + suffix) as Test).classpath =
                    javaConvention.sourceSets.getByName("test" + suffix).runtimeClasspath
        }
    }

    private static void configureConfigurations(@Nonnull Project project, @Nonnull IdeaVersion version) {
        String suffix = getId(version)
        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class)
        ConfigurationContainer configurations = project.getConfigurations()

        Configuration apiConfig = configurations.create(getApiConfigName(version))
        Configuration implConfig = configurations.create(getImplementationConfigName(version))
        implConfig.extendsFrom apiConfig

        Configuration mainConfig = configurations.maybeCreate(
                javaConvention.sourceSets.getByName("main" + suffix).compileClasspathConfigurationName
        )
        mainConfig.extendsFrom configurations.getByName(BASE_IMPL_CONFIGURATION), implConfig

        Configuration testConfig = configurations.maybeCreate(
                javaConvention.sourceSets.getByName("test" + suffix).compileClasspathConfigurationName
        )
        testConfig.extendsFrom configurations.getByName(BASE_TEST_CONFIGURATION), mainConfig
    }

    private static void configureDependencies(Project project, IdeaVersion version) {
        project.afterEvaluate {
            IdeaJarsExtension apiExt = project.extensions.getByName(API_JAR_EXTENSION_NAME) as IdeaJarsExtension
            IdeaJarsExtension implExt = project.extensions.getByName(IMPL_JAR_EXTENSION_NAME) as IdeaJarsExtension
            LOG.info("Adding dependency jars for " + version.version + ": " +
                    (apiExt.getJarsFor(version) + implExt.getJarsFor(version)).files.toString())
            project.dependencies.add(getApiConfigName(version), apiExt.getJarsFor(version))
            project.dependencies.add(getImplementationConfigName(version), implExt.getJarsFor(version))
        }
    }

    private static File child(File parent, String... sub) {
        File r = parent
        for (String s : sub) {
            r = new File(r, s)
        }
        return r
    }

    static String getId(@Nonnull IdeaVersion version) {
        return "Idea" + version.version
    }

    static String getApiConfigName(@Nonnull IdeaVersion version) {
        return "api" + getId(version)
    }

    static String getImplementationConfigName(@Nonnull IdeaVersion version) {
        return "implementation" + getId(version)
    }
}