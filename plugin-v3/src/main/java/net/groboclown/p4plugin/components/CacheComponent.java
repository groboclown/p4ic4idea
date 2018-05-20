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

package net.groboclown.p4plugin.components;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.cache.CacheQueryHandler;
import net.groboclown.p4.server.impl.cache.CacheQueryHandlerImpl;
import net.groboclown.p4.server.impl.cache.store.ProjectCacheStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "P4ProjectCache"
)
public class CacheComponent implements ProjectComponent, PersistentStateComponent<ProjectCacheStore.State> {
    private static final String COMPONENT_NAME = "Perforce Project Cached Data";
    private final Project project;
    private final ProjectCacheStore projectCache = new ProjectCacheStore();
    private CacheQueryHandler queryHandler;

    public static CacheComponent getInstance(Project project) {
        // a non-registered component can happen when the config is loaded outside a project.
        CacheComponent ret = null;
        if (project != null) {
            ret = project.getComponent(CacheComponent.class);
        }
        if (ret == null) {
            ret = new CacheComponent(project);
        }
        return ret;
    }

    public CacheComponent(Project project) {
        this.project = project;
    }

    public CacheQueryHandler getQueryHandler() {
        return queryHandler;
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }


    @Override
    public void projectOpened() {

    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        queryHandler = new CacheQueryHandlerImpl(projectCache);

        // FIXME add message listeners
    }

    @Override
    public void disposeComponent() {
        if (queryHandler != null) {
            queryHandler = null;
        }
    }

    @Nullable
    @Override
    public ProjectCacheStore.State getState() {
        return projectCache.getState();
    }

    @Override
    public void loadState(ProjectCacheStore.State state) {
        this.projectCache.setState(state);
    }

    @Override
    public void noStateLoaded() {
        // do nothing
    }
}
