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
package net.groboclown.idea.p4ic.v2.changes;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.impl.DirectoryIndexImpl;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsFileListenerContextHelper;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeImpl;
import com.intellij.openapi.vcs.impl.DefaultVcsRootPolicy;
import com.intellij.openapi.vcs.impl.FileStatusManagerImpl;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vcs.impl.projectlevelman.NewMappings;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.server.IServerAddress;
import net.groboclown.idea.p4ic.ProjectRule;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class ChangeListMatcherTest {
    @Rule
    public ProjectRule projectRule = new ProjectRule("ChangeListMatcherTest");

    P4Vcs vcs;
    ChangeListMatcher matcher;
    P4ServerName serverName;

    @Test
    public void createChange_shelved_added()
            throws Exception {
        ClientServerRef clientServerRef = new ClientServerRef(serverName, "client");
        P4ShelvedFile shelvedFile = new P4ShelvedFile("//path", projectRule.getVcsRoot().getPath(), FileStatus.ADDED);
        Change change = matcher.createChange(clientServerRef, shelvedFile);
        assertThat(change.getBeforeRevision(),
                nullValue());
        assertThat(change.getAfterRevision(),
                notNullValue());

        assertThat(ChangesUtil.findValidParentAccurately(change.getAfterRevision().getFile()),
                is(projectRule.getVcsRoot().getVirtualFile()));

        VcsDirtyScope scope = new VcsDirtyScopeImpl(vcs, projectRule.getProject());
        assertThat(
                scope.belongsTo(change.getAfterRevision().getFile()),
                is(true)
        );

        assertThat(
                ChangeListManagerImpl.isUnder(change, scope),
                is(true));
    }


    @Before
    public void before() {
        Project project = projectRule.getProject();
        matcher = new ChangeListMatcher(project, new P4ChangeListMapping(project));
        serverName = P4ServerName.forPort("1666");
        assertThat(serverName, notNullValue());
        vcs = new P4Vcs(project, new UserProjectPreferences());
    }
}
