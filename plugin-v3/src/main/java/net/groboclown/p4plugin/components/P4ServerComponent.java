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

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.TopCommandRunner;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.impl.connection.ConnectCommandRunner;
import net.groboclown.p4.server.impl.connection.ConnectionManager;
import net.groboclown.p4.server.impl.connection.P4RequestErrorHandler;
import net.groboclown.p4.server.impl.connection.impl.SimpleConnectionManager;
import net.groboclown.p4plugin.messages.MessageErrorHandler;
import net.groboclown.p4plugin.util.TempDirUtil;
import org.jetbrains.annotations.NotNull;

public class P4ServerComponent implements ProjectComponent {
    private static final String COMPONENT_NAME = "Perforce Server Primary Connection";
    private final Project project;
    private P4CommandRunner commandRunner;

    public static P4ServerComponent getInstance(Project project) {
        // a non-registered component can happen when the config is loaded outside a project.
        P4ServerComponent ret = null;
        if (project != null) {
            ret = project.getComponent(P4ServerComponent.class);
        }
        if (ret == null) {
            ret = new P4ServerComponent(project);
        }
        return ret;
    }


    public P4ServerComponent(Project project) {
        this.project = project;
    }

    public P4CommandRunner getCommandRunner() {
        return commandRunner;
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }


    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        if (commandRunner == null) {
            commandRunner = new TopCommandRunner(project, CacheComponent.getInstance(project).getQueryHandler(),
                    new ConnectCommandRunner(createConnectionManager()));
        }
    }

    @Override
    public void disposeComponent() {
        if (commandRunner != null) {
            commandRunner = null;
        }
    }


    @NotNull
    protected ConnectionManager createConnectionManager() {
        UserProjectPreferences preferences = UserProjectPreferences.getInstance(project);
        return new SimpleConnectionManager(
                TempDirUtil.getTempDir(project),
                preferences.getSocketSoTimeoutMillis(),

                // FIXME pull in the version from the classpath file
                // Update P4VcsRootConfigurable when this is fixed.
                "v-10-get-the-right-number",

                createErrorHandler()
        );
    }


    protected P4RequestErrorHandler createErrorHandler() {
        return new MessageErrorHandler(project);
    }
}
