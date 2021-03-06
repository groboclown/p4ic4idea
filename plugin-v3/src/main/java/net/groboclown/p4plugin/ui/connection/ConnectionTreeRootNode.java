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
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.cache.ActionChoice;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4plugin.components.CacheComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectionTreeRootNode
        extends DefaultMutableTreeNode {
    private static final Logger LOG = Logger.getInstance(ConnectionTreeRootNode.class);

    private final Object sync = new Object();

    public Collection<TreeNode> refresh(@NotNull Project project) {
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            synchronized (sync) {
                removeAllChildren();
            }
            return Collections.emptyList();
        }

        synchronized (sync) {
            removeAllChildren();
            registry.getClientConfigRoots().forEach((root) ->
                    loadClientConfigRoot(project, root));
        }
        return Collections.singleton(this);
    }


    @Override
    public void removeAllChildren()
    {
        try {
            super.removeAllChildren();
        } catch (ArrayIndexOutOfBoundsException e) {
            // If this does still happen, don't die.
            LOG.warn("UI Synchronization issue: refreshed connection tree while refreshing?");
        }
    }


    private void loadClientConfigRoot(@NotNull Project project, @NotNull ClientConfigRoot root) {
        RootNode fileRoot = createRootNode(root);

        try {
            List<ActionChoice> pendingActions =
                    CacheComponent.getInstance(project).getCachePending().copyActions(root.getClientConfig())
                            .collect(Collectors.toList());
            fileRoot.pending.setPendingCount(pendingActions.size());
            pendingActions.forEach((ac) -> {
                DefaultMutableTreeNode actionNode = new DefaultMutableTreeNode(ac);
                fileRoot.pendingNode.add(actionNode);
                // File information on an action is static.
                for (FilePath affectedFile : ac.getAffectedFiles()) {
                    actionNode.add(new DefaultMutableTreeNode(affectedFile));
                }
                for (P4CommandRunner.ResultError previousExecutionProblem : ac.getPreviousExecutionProblems()) {
                    actionNode.add(new DefaultMutableTreeNode(previousExecutionProblem));
                }
            });
        } catch (InterruptedException e) {
            InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(e)));
        }
    }


    private RootNode createRootNode(ClientConfigRoot root) {
        RootNode fileRoot = new RootNode(root, new DefaultMutableTreeNode(root, true));

        // The client root details should never change, so only add them when the root itself is added.
        // They are never included in the content map.
        fileRoot.fileRoot.add(new DefaultMutableTreeNode(
                root.getClientConfig().getClientServerRef().getServerName(), false));
        fileRoot.fileRoot.add(new DefaultMutableTreeNode(
                root.getClientConfig().getClientServerRef(), false));
        fileRoot.fileRoot.add(fileRoot.pendingNode);
        add(fileRoot.fileRoot);
        return fileRoot;
    }


    private static class RootNode {
        final ClientConfigRoot root;
        final DefaultMutableTreeNode fileRoot;
        final PendingParentNode pending;
        final DefaultMutableTreeNode pendingNode;

        private RootNode(ClientConfigRoot root, DefaultMutableTreeNode fileRoot) {
            this.root = root;
            this.fileRoot = fileRoot;
            this.pending = new PendingParentNode();
            this.pendingNode = new DefaultMutableTreeNode(pending, true);
        }
    }
}
