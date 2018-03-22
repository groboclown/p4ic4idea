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
package net.groboclown.idea.p4ic;

import com.intellij.idea.IdeaTestApplication;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.util.PlatformUtils;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.ui.UIUtil;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectRule extends ExternalResource {
    private final String name;
    private TempDirTestFixture dirFixture;
    private LocalFileSystem localFileSystem;
    private IdeaProjectTestFixture projectFixture;

    public ProjectRule(String name) {
        this.name = name;
    }


    public Project getProject() {
        return projectFixture.getProject();
    }


    @NotNull
    public FilePath createFilePath(String path, boolean exists, boolean isDirectory, boolean isReadable, boolean isWritable) {
        File file = new File(path);
        FilePath ret = mock(FilePath.class);
        when(ret.getPath()).thenReturn(path);
        when(ret.getIOFile()).thenReturn(file);
        when(ret.isDirectory()).thenReturn(isDirectory);
        when(ret.isNonLocal()).thenReturn(false);
        when(ret.getName()).thenReturn(file.getName());
        when(ret.getVirtualFile()).thenReturn(createVirtualFile(path, exists, isDirectory));
        return ret;
    }


    @NotNull
    public VirtualFile createVirtualFile(String path, boolean exists, boolean isDirectory) {
        File file = new File(path);
        VirtualFile ret = mock(VirtualFile.class);
        when(ret.getName()).thenReturn(file.getName());
        when(ret.isDirectory()).thenReturn(isDirectory);
        when(ret.exists()).thenReturn(exists);
        when(ret.getPath()).thenReturn(path);
        when(ret.getUrl()).thenReturn(file.toURI().toString());
        return ret;
    }


    @NotNull
    public FilePath getVcsRoot() {
        return FilePathUtil.getFilePath(getProject().getBaseDir());
    }


    @Override
    protected void before() throws Throwable {
        super.before();

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);
        // FIXME 2017.1
        //PlatformTestCase.initPlatformLangPrefix();
        IdeaTestApplication.getInstance(null);

        dirFixture = IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture();
        dirFixture.setUp();

        final TestFixtureBuilder<IdeaProjectTestFixture> testFixtureBuilder =
                IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("");
        projectFixture = testFixtureBuilder.getFixture();
        projectFixture.setUp();

        localFileSystem = LocalFileSystem.getInstance();
    }


    @Override
    protected void after() {
        try {
            dirFixture.tearDown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
                try {
                    projectFixture.tearDown();
                } catch (Exception throwable) {
                    throwable.printStackTrace();
                }
            }
        });

        dirFixture = null;
        projectFixture = null;
        super.after();
    }


    public static void edt(@NotNull final ThrowableRunnable<Exception> runnable) throws Exception {
        final AtomicReference<Exception> exception = new AtomicReference<Exception>();
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception throwable) {
                    exception.set(throwable);
                }
            }
        });
        //noinspection ThrowableResultOfMethodCallIgnored
        if (exception.get() != null) {
            throw exception.get();
        }
    }
}
