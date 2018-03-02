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
package net.groboclown.idea.p4ic.mock;

import com.intellij.idea.IdeaTestApplication;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.TestLoggerFactory;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.util.PlatformUtils;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.ui.UIUtil;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// This guy should be defined in a static context; it should only be setup once per VM.
public class HeavyweightProjectRule extends ExternalResource {

    static {
        Logger.setFactory(TestLoggerFactory.class);
    }

    private final String name;
    private IdeaProjectTestFixture projectFixture;
    private TempDirTestFixture dirFixture;
    private TestFixtureBuilder<IdeaProjectTestFixture> projectFixtureBuilder;

    public HeavyweightProjectRule(String name) {
        this.name = name;
    }


    public Project getProject() throws Exception {
        return getProjectFixture().getProject();
    }


    public Module getModule() throws Exception {
        return getProjectFixture().getModule();
    }


    public TempDirTestFixture createVcsRoot(String name) {
        //VcsUtil.getVcsRootFor()
        throw new NullPointerException();
    }


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



    @Override
    protected void before() throws Throwable {
        super.before();

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);
        // FIXME 2017.1
        //PlatformTestCase.initPlatformLangPrefix();
        IdeaTestApplication.getInstance(null);

        projectFixtureBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name);
        dirFixture = IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture();
        dirFixture.setUp();

        // Delay setting up the project fixture until the rest of the settings are in.
        projectFixture = null;
        System.err.println("before!");
    }


    @Override
    protected void after() {
        System.err.println("after!");
        if (projectFixture != null) {
            ((ProjectComponent) VcsDirtyScopeManager.getInstance(projectFixture.getProject())).projectClosed();
            ((ProjectComponent) ChangeListManager.getInstance(projectFixture.getProject())).projectClosed();
            ((ChangeListManagerImpl) ChangeListManager.getInstance(projectFixture.getProject())).stopEveryThingIfInTestMode();
            ((ProjectComponent) ProjectLevelVcsManager.getInstance(projectFixture.getProject())).projectClosed();

            try {
                edt(new ThrowableRunnable<Exception>() {
                    @Override
                    public void run() throws Exception {
                        projectFixture.tearDown();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            dirFixture.tearDown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.after();
    }


    @NotNull
    private IdeaProjectTestFixture getProjectFixture() throws Exception {
        if (projectFixture == null) {
            if (projectFixtureBuilder == null) {
                try {
                    before();
                } catch (Exception e) {
                    throw e;
                } catch (Error e) {
                    throw e;
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
            projectFixture = projectFixtureBuilder.getFixture();
            edt(new ThrowableRunnable<Exception>() {
                @Override
                public void run() throws Exception {
                    projectFixture.setUp();
                }
            });
            final Project project = projectFixture.getProject();

            ((ProjectComponent) ChangeListManager.getInstance(project)).projectOpened();
            //((ChangeListManagerImpl) ChangeListManager.getInstance(projectFixture.getProject())).stopEveryThingIfInTestMode();
            ((ProjectComponent) ProjectLevelVcsManager.getInstance(projectFixture.getProject())).projectOpened();
            ((ProjectComponent) VcsDirtyScopeManager.getInstance(project)).projectOpened();

            ProjectLevelVcsManagerImpl vcsManager =
                    (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(project);
            AbstractVcs vcs = vcsManager.findVcsByName(P4Vcs.VCS_NAME);
            Assert.assertEquals(1, vcsManager.getRootsUnderVcs(vcs).length);
        }
        return projectFixture;
    }


    private static void edt(@NotNull final ThrowableRunnable<Exception> runnable) throws Exception {
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
