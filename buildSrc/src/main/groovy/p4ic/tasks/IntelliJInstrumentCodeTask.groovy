package p4ic.tasks

import org.apache.tools.ant.BuildException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.JavaCompile
import p4ic.ext.IdeaJarsExtension
import p4ic.ext.IdeaVersion
import p4ic.util.IdeaVersionUtil

import javax.annotation.Nonnull
import java.util.concurrent.Callable

/*
 * Taken from https://github.com/JetBrains/gradle-intellij-plugin/blob/master/src/main/groovy/org/jetbrains/intellij/tasks/IntelliJInstrumentCodeTask.groovy
 *
 * Under the Apache 2.0 license.
 */

class IntelliJInstrumentCodeTask extends ConventionTask {
    private static final String FILTER_ANNOTATION_REGEXP_CLASS = 'com.intellij.ant.ClassFilterAnnotationRegexp'
    private static final LOADER_REF = "java2.loader"

    private FileCollection originalClassesDirs
    private Set<File> allSources
    protected FileCollection resources
    private Callable<FileCollection> classPath

    @OutputDirectory
    File outputDir

    protected void fromSourceSet(@Nonnull final SourceSet sourceSet, final IdeaJarsExtension jars, final IdeaVersion version) {
        originalClassesDirs = sourceSet.output.classesDirs
        allSources = sourceSet.allSource.srcDirs
        resources = sourceSet.resources
        classPath = new Callable<FileCollection>() {
            @Override
            FileCollection call() throws Exception {
                FileCollection ret = sourceSet.compileClasspath
                if (jars != null && version != null) {
                    ret = jars.getJarsFor(version) + ret
                }
                return ret
            }
        }
    }

    protected void fromJavaCompile(@Nonnull JavaCompile task, Callable<FileCollection> classpath) {
        originalClassesDirs = task.project.files(task.destinationDir)
        allSources = task.inputs.files.getFiles()
        resources = task.project.files()
        classPath = classpath
    }

    @InputFiles
    @SkipWhenEmpty
    FileTree getOriginalClasses() {
        return project.files(originalClassesDirs.from).asFileTree
    }

    @InputFiles
    FileCollection getSourceDirs() {
        return project.files(allSources.findAll { !resources.contains(it) && it.exists() })
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    void instrumentClasses() {
        def outputDir = getOutputDir()
        copyOriginalClasses(outputDir)

        def classpath = IdeaVersionUtil.lowestCompatibleBuildVersion(project).jars(
                // jars required by the instrumentation
                "javac2",
                "jdom",
                "asm-all",
                "jgoodies-forms",
                "instrumentation-util",
                "forms-compiler",
                "forms_rt"
        )

        ant.taskdef(name: 'instrumentIdeaExtensions',
                classpath: classpath.asPath,
                loaderref: LOADER_REF,
                classname: 'com.intellij.ant.InstrumentIdeaExtensions')

        logger.info("Compiling forms and instrumenting code with nullability preconditions")
        boolean instrumentNotNull = prepareNotNullInstrumenting(classpath)
        instrumentCode(getSourceDirs(), outputDir, instrumentNotNull)
    }


    private void copyOriginalClasses(@Nonnull File outputDir) {
        outputDir.deleteDir()
        project.copy {
            from getOriginalClasses()
            into outputDir
        }
    }

    private boolean prepareNotNullInstrumenting(@Nonnull FileCollection classpath) {
        try {
            ant.typedef(name: 'skip', classpath: classpath.asPath, loaderref: LOADER_REF,
                    classname: FILTER_ANNOTATION_REGEXP_CLASS)
        } catch (BuildException e) {
            def cause = e.getCause()
            if (cause instanceof ClassNotFoundException && FILTER_ANNOTATION_REGEXP_CLASS == cause.getMessage()) {
                logger.warn("Old version of Javac2 is used, " +
                        "instrumenting code with nullability will be skipped. Use IDEA >14 SDK (139.*) to fix this")
                return false
            } else {
                throw e
            }
        }
        return true
    }

    private void instrumentCode(@Nonnull FileCollection srcDirs, @Nonnull File outputDir, boolean instrumentNotNull) {
        def headlessOldValue = System.setProperty('java.awt.headless', 'true')
        FileCollection cp = classPath.call()
        logger.info("Setting up instrumentation with classpath " + cp.asPath)

        ant.instrumentIdeaExtensions(srcdir: srcDirs.asPath,
                destdir: outputDir, classpath: cp.asPath,

                // This doesn't cause jdk9 to start working
                modulepath: modpath,

                includeantruntime: false, instrumentNotNull: instrumentNotNull) {
            if (instrumentNotNull) {
                ant.skip(pattern: 'kotlin/Metadata')
            }
        }
        if (headlessOldValue != null) {
            System.setProperty('java.awt.headless', headlessOldValue)
        } else {
            System.clearProperty('java.awt.headless')
        }
    }

    private String findJModPath() {
        Set<File> dirs = new HashSet<>()
        logger.info("Finding jmod paths in " + System.properties."java.home")
        def jreHome = System.properties."java.home"
        project.fileTree(jreHome).visit { FileVisitDetails details ->
            if (details.file.name.endsWith('.jmod')) {
                dirs.add(details.file.parentFile)
            }
        }
        def ret = ""
        for (File f in dirs) {
            if (ret.length() > 0) {
                ret += File.pathSeparator
            }
            ret += f.getAbsolutePath()
        }
        return ret
    }
}
