package p4ic.tasks

import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test
import p4ic.ext.IdeaJarsExtension
import p4ic.ext.IdeaVersion
import p4ic.util.P4icUtil

import javax.annotation.Nonnull
import java.nio.file.AccessDeniedException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class IntelliJInstrumentCodeTaskTest {
    Path tmpDir

    @Test
    void instrumentClasses() {
        String libDir = System.getProperty("PROJECT_LIB_DIR", System.getenv("PROJECT_LIB_DIR"))
        assertNotNull(libDir)
        Project project = ProjectBuilder.builder().withProjectDir(tmpDir.toFile()).build()
        project.getPluginManager().apply(BasePlugin.class)
        project.getPluginManager().apply(JvmEcosystemPlugin.class)
        SourceSetContainer sourceSets = (SourceSetContainer)project.getExtensions().getByName("sourceSets")
        SourceSet sourceSet = sourceSets.create('main')
        sourceSet.java {
            srcDir tmpDir.resolve('java')
        }
        sourceSet.compileClasspath = project.files()
        println("Source dirs: " + sourceSet.allSource.srcDirs)
        tmpDir.resolve('java').toFile().mkdirs()
        tmpDir.resolve('java').resolve("X.java").toFile().text = 'class X {}'
        // project.getExtensions().add(SourceSetContainer.class, "sourceSets", sourceSets)
        def task = project.task('instrument', type: IntelliJInstrumentCodeTask)
        assertTrue(task instanceof IntelliJInstrumentCodeTask)
        task.outputDir = tmpDir.resolve("out").toFile()
        task.fromSourceSet(
                sourceSet,
                new IdeaJarsExtension(),
                new MockIdeaVersion(project, "203", project.file(libDir))
        )
        task.instrumentClasses()
    }

    @Before
    void before() {
        tmpDir = Files.createTempDirectory("test")
    }

    @After
    void after() {
        println("Cleaning out " + tmpDir)
        Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                try {
                    Files.delete(file);
                } catch (AccessDeniedException ex) {
                    // Ignore
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException {
                if (e == null) {
                    try {
                        Files.delete(dir);
                    } catch (DirectoryNotEmptyException ex) {
                        // ignore
                    }
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }
        })
    }


    static class MockIdeaVersion extends IdeaVersion {
        List<File> jars = new ArrayList<>()
        File libDir

        MockIdeaVersion(@Nonnull Project project, @Nonnull String version, @Nonnull File libDir) {
            super(project, version)
            this.libDir = libDir
        }

//        @Nonnull
//        List<File> findJars(@Nonnull Collection<String> names) {
//            return this.jars
//        }
    }
}
