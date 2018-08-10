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

package net.groboclown.p4plugin;

import com.intellij.lifecycle.PeriodicalTasksCloser;
import com.intellij.mock.MockCommandProcessor;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsFileListenerContextHelper;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.mock.MockLocalChangeList;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistType;
import net.groboclown.p4.server.impl.ProjectConfigRegistryImpl;
import net.groboclown.p4.server.impl.cache.store.ClientQueryCacheStore;
import net.groboclown.p4.server.impl.cache.store.ClientServerRefStore;
import net.groboclown.p4.server.impl.cache.store.IdeChangelistCacheStore;
import net.groboclown.p4.server.impl.cache.store.P4ChangelistIdStore;
import net.groboclown.p4.server.impl.cache.store.P4LocalChangelistStore;
import net.groboclown.p4.server.impl.cache.store.ProjectCacheStore;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.extension.P4Vcs;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.groboclown.p4.server.api.P4VcsKey.VCS_NAME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginSetup
        implements BeforeEachCallback, AfterEachCallback {
    public IdeaLightweightExtension idea;
    public P4Vcs vcs;
    public ProjectConfigRegistry registry;
    public P4ServerComponent server;
    public CacheComponent cacheComponent;
    private List<VirtualFile> roots = new ArrayList<>();

    @Override
    public void beforeEach(ExtensionContext extensionContext)
            throws Exception {
        idea = new IdeaLightweightExtension();
        idea.beforeEach(extensionContext);
        // Basic IDE dependency setup
        setupIdeRequirements(idea);

        vcs = new P4Vcs(idea.getMockProject());
        registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        server = new P4ServerComponent(idea.getMockProject());
        cacheComponent = new CacheComponent(idea.getMockProject());

        idea.registerProjectComponent(ProjectConfigRegistry.COMPONENT_NAME, registry);
        idea.registerProjectComponent(ProjectConfigRegistry.class, registry);
        idea.registerProjectComponent(P4ServerComponent.COMPONENT_NAME, server);
        idea.registerProjectComponent(P4ServerComponent.class, server);
        idea.registerProjectComponent(CacheComponent.class, cacheComponent);

        ProjectLevelVcsManager mockVcsMgr = ProjectLevelVcsManager.getInstance(idea.getMockProject());
        when(mockVcsMgr.findVcsByName(VCS_NAME)).thenReturn(vcs);
        when(mockVcsMgr.checkVcsIsActive(VCS_NAME)).thenReturn(true);

        vcs.doStart();
        registry.initComponent();
        server.initComponent();
        vcs.doActivate();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext)
            throws Exception {
        vcs.doDeactivate();
        vcs.doShutdown();
        vcs = null;

        registry.disposeComponent();
        registry = null;

        server.disposeComponent();
        server = null;

        cacheComponent.disposeComponent();
        cacheComponent = null;
    }

    public Project getMockProject() {
        return idea.getMockProject();
    }

    public ClientConfigRoot addClientConfigRoot(TemporaryFolder tmp, String clientRootDir) {
        File base = tmp.newFile(clientRootDir);
        if (!base.mkdirs()) {
            throw new RuntimeException("Could not create directory " + base);
        }

        MockConfigPart cp1 = new MockConfigPart()
                .withClientname("cn")
                .withUsername("u")
                .withServerName("s:123");
        ClientConfig cc = ClientConfig.createFrom(ServerConfig.createFrom(cp1), cp1);
        FilePath fp = VcsUtil.getFilePath(base);
        VirtualFile rootDir = Objects.requireNonNull(fp.getVirtualFile());
        registry.addClientConfig(cc, rootDir);
        roots.add(rootDir);
        return Objects.requireNonNull(registry.getClientFor(rootDir));
    }

    public void goOffline(ClientConfigRoot root) {
        UserSelectedOfflineMessage.requestOffline(idea.getMockProject(),
                root.getClientConfig().getClientServerRef().getServerName());
    }

    public P4ChangelistId addDefaultChangelist(ClientConfigRoot root, String name) {
        CacheComponent cc = CacheComponent.getInstance(idea.getMockProject());
        // Use the state object to avoid all the work to populate the cache.
        ProjectCacheStore.State state = cc.getState();
        assertNotNull(state);
        P4LocalChangelistStore.State defaultChangelist = new P4LocalChangelistStore.State();
        defaultChangelist.changelistId = new P4ChangelistIdStore.State();
        defaultChangelist.changelistId.id = 0;
        defaultChangelist.changelistId.ref = new ClientServerRefStore.State();
        defaultChangelist.changelistId.ref.clientName = root.getClientConfig().getClientname();
        defaultChangelist.changelistId.ref.serverPort = root.getServerConfig().getServerName().getFullPort();
        defaultChangelist.type = P4ChangelistType.PUBLIC;
        defaultChangelist.deleted = false;
        defaultChangelist.username = root.getServerConfig().getUsername();
        defaultChangelist.clientname = root.getClientConfig().getClientname();
        defaultChangelist.shelvedFiles = new ArrayList<>();
        defaultChangelist.containedFiles = new ArrayList<>();
        defaultChangelist.jobs = new ArrayList<>();

        ClientQueryCacheStore.State clientQueryCacheStore = new ClientQueryCacheStore.State();
        clientQueryCacheStore.changelists = new ArrayList<>();
        clientQueryCacheStore.files = new ArrayList<>();
        clientQueryCacheStore.source = defaultChangelist.changelistId.ref;
        clientQueryCacheStore.changelists.add(defaultChangelist);
        state.clientState = state.clientState == null ? new ArrayList<>() : state.clientState;
        state.clientState.add(clientQueryCacheStore);

        state.changelistState = state.changelistState == null ? new IdeChangelistCacheStore.State() : state.changelistState;
        state.changelistState.linkedChangelistMap = state.changelistState.linkedChangelistMap  == null
            ? new ArrayList<>() : state.changelistState.linkedChangelistMap;
        IdeChangelistCacheStore.LinkedChangelistState linkedState = new IdeChangelistCacheStore.LinkedChangelistState();
        linkedState.changelistId = defaultChangelist.changelistId;
        linkedState.linkedLocalChangeId = name;
        state.changelistState.linkedChangelistMap.add(linkedState);
        cc.loadState(state);

        addIdeChangelist(LocalChangeList.DEFAULT_NAME, null, true);

        return P4ChangelistIdStore.read(defaultChangelist.changelistId);
    }

    public ChangeListManager getMockChangelistManager() {
        return ChangeListManager.getInstance(idea.getMockProject());
    }

    public MockLocalChangeList addIdeChangelist(String name, String comment, boolean isDefault) {
        MockLocalChangeList ideChangeList = new MockLocalChangeList()
                .withName(name)
                .withComment(comment)
                .withIsDefault(isDefault);
        ChangeListManager cm = getMockChangelistManager();
        if (isDefault) {
            when(cm.getDefaultChangeList()).thenReturn(ideChangeList);
        }
        when(cm.getChangeList(name)).thenReturn(ideChangeList);
        when(cm.findChangeList(name)).thenReturn(ideChangeList);
        List<LocalChangeList> currentChanges = cm.getChangeLists();
        if (currentChanges == null) {
            currentChanges = new ArrayList<>();
        } else {
            currentChanges = new ArrayList<>(currentChanges);
        }
        currentChanges.add(ideChangeList);
        when(cm.getChangeLists()).thenReturn(currentChanges);
        when(cm.getChangeListsCopy()).thenReturn(currentChanges);
        return ideChangeList;
    }

    private void setupIdeRequirements(IdeaLightweightExtension idea) {
        EditorColorsManager mgr = mock(EditorColorsManager.class);
        idea.registerApplicationService(EditorColorsManager.class, mgr);
        EditorColorsScheme scheme = mock(EditorColorsScheme.class);
        when(mgr.getGlobalScheme()).thenReturn(scheme);
        when(mgr.getSchemeForCurrentUITheme()).thenReturn(scheme);
        when(scheme.getColor(any())).thenReturn(JBColor.BLACK);

        PeriodicalTasksCloser closer = new PeriodicalTasksCloser();
        idea.registerApplicationComponent(PeriodicalTasksCloser.class, closer);

        ChangeListManager clMgr = mock(ChangeListManager.class);
        idea.registerProjectComponent(ChangeListManager.class, clMgr);

        VcsDirtyScopeManager dirtyScopeMgr = mock(VcsDirtyScopeManager.class);
        idea.registerProjectComponent(VcsDirtyScopeManager.class, dirtyScopeMgr);

        ProjectLevelVcsManagerEx vcsMgr = mock(ProjectLevelVcsManagerEx.class);
        when(vcsMgr.getRootsUnderVcs(any())).then(
                (Answer<VirtualFile[]>) invocationOnMock -> roots.toArray(new VirtualFile[0]));
        idea.registerProjectComponent(ProjectLevelVcsManager.class, vcsMgr);

        MockCommandProcessor cmd = new MockCommandProcessor();
        idea.registerApplicationService(CommandProcessor.class, cmd);

        VcsFileListenerContextHelper fileListenerHelper = mock(VcsFileListenerContextHelper.class);
        idea.registerProjectService(VcsFileListenerContextHelper.class, fileListenerHelper);
    }
}
