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

package net.groboclown.p4plugin.ui.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.cache.ActionChoice;
import net.groboclown.p4plugin.components.CacheComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectionTreeRootNode
        extends DefaultMutableTreeNode {
    private static final Logger LOG = Logger.getInstance(ConnectionTreeRootNode.class);

    public void refresh(@NotNull Project project) {
        removeAllChildren();
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            return;
        }
        registry.getClientConfigRoots().forEach((root) -> addClientConfigRoot(project, root));
    }


    private void addClientConfigRoot(@NotNull Project project, @NotNull ClientConfigRoot root) {
        DefaultMutableTreeNode fileRoot = new DefaultMutableTreeNode(root, true);

        fileRoot.add(new DefaultMutableTreeNode(root.getClientConfig().getClientServerRef().getServerName(), false));
        fileRoot.add(new DefaultMutableTreeNode(root.getClientConfig().getClientServerRef(), false));

        try {
            List<ActionChoice> pendingActions =
                    CacheComponent.getInstance(project).getCachePending().copyActions(root.getClientConfig())
                            .collect(Collectors.toList());
            DefaultMutableTreeNode pendingNode =
                    new DefaultMutableTreeNode(new PendingParentNode(pendingActions.size()), true);
            pendingActions.forEach((ac) -> pendingNode.add(new DefaultMutableTreeNode(ac)));
            fileRoot.add(pendingNode);
        } catch (InterruptedException e) {
            LOG.warn(e);
        }

        add(fileRoot);
    }
}
