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

    private FileCollection sourceSetOutputClassesDirs
    private Set<File> sourceSetAllDirs
    protected FileCollection sourceSetResources
    private Callable<FileCollection> sourceSetCompileClasspath
    private IdeaVersion ideaVersion

    @OutputDirectory
    File outputDir

    protected void fromSourceSet(@Nonnull final SourceSet sourceSet, final IdeaJarsExtension jars, final IdeaVersion version) {
        sourceSetOutputClassesDirs = sourceSet.output.classesDirs
        sourceSetAllDirs = sourceSet.allSource.srcDirs
        sourceSetResources = sourceSet.resources
        ideaVersion = version
        sourceSetCompileClasspath = new Callable<FileCollection>() {
            @Override
            FileCollection call() throws Exception {
                FileCollection ret = sourceSet.compileClasspath
                if (jars != null && version != null) {
                    ret = project.files(jars.getJarsFor(version)) + ret
                }
                return ret
            }
        }
    }

    protected void fromJavaCompile(@Nonnull JavaCompile task, Callable<FileCollection> classpath) {
        sourceSetOutputClassesDirs = task.project.files(task.destinationDirectory)
        sourceSetAllDirs = task.inputs.files.getFiles()
        sourceSetResources = task.project.files()
        sourceSetCompileClasspath = classpath
    }

    @InputFiles
    @SkipWhenEmpty
    FileTree getOriginalClasses() {
        return project.files(sourceSetOutputClassesDirs.from).asFileTree
    }

    @InputFiles
    FileCollection getSourceDirs() {
        return project.files(sourceSetAllDirs.findAll {
            !sourceSetResources.contains(it) && it.exists()
        })
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    void instrumentClasses() {
        def outputDir = getOutputDir()
        copyOriginalClasses(outputDir)

        def classpath = compilerClassPath()

        ant.taskdef(name: 'instrumentIdeaExtensions',
                classpath: classpath.asPath,
                loaderref: LOADER_REF,
                classname: 'com.intellij.ant.InstrumentIdeaExtensions')

        logger.info("Compiling forms and instrumenting code with nullability preconditions")
        boolean instrumentNotNull = prepareNotNullInstrumenting(classpath)
        // instrumentCode(getSourceDirs(), outputDir, instrumentNotNull)
    }


    private void copyOriginalClasses(@Nonnull File outputDir) {
        outputDir.deleteDir()
        outputDir.mkdirs()
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
        if (srcDirs.isEmpty()) {
            return;
        }

        FileCollection cp = sourceSetCompileClasspath.call()
        logger.info("Setting up instrumentation with classpath " + cp.asPath)

        def headlessOldValue = System.setProperty('java.awt.headless', 'true')
        try {
            ant.instrumentIdeaExtensions(
                    srcdir: srcDirs.asPath,
                    destdir: outputDir,
                    classpath: cp.asPath,

                    // Force the class file version, which is necessary if compiling with any other JDK.
                    target: "11", source: "11",

                    "deprecation": true,
                    includeAntRuntime: false,
                    includeJavaRuntime: true,
                    instrumentNotNull: instrumentNotNull) {
                if (instrumentNotNull) {
                    ant.skip(pattern: 'kotlin/Metadata')
                }
                return null
            }
        } finally {
            if (headlessOldValue != null) {
                System.setProperty('java.awt.headless', headlessOldValue)
            } else {
                System.clearProperty('java.awt.headless')
            }
        }
    }

    private String findJModPath() {
        Set<File> dirs = new HashSet<>()
        logger.info("Finding jmod paths in " + System.properties."java.home")
        def jreHome = System.properties."java.home"
        def tree = project.fileTree(jreHome).filter {
            include '*.jar'
        }
        tree.visit { FileVisitDetails details ->
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

    // local compiler
    private FileCollection compilerClassPath() {
        IdeaVersion version = this.ideaVersion
        if (version == null) {
            version = IdeaVersionUtil.lowestCompatibleBuildVersion(project)
        }
        return version.jars(
                // jars required by the instrumentation
                "javac2",
                "compiler-antTasks",
                "compiler-instrumentationUtil",

                //"jgoodies-forms",
                "guiForms-compiler",
                //"forms-rt",

                "jdom",
                "asm",
        )
    }

}
