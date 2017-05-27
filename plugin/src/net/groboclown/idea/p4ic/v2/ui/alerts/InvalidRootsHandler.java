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

package net.groboclown.idea.p4ic.v2.ui.alerts;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.groboclown.idea.p4ic.v2.ui.alerts.DistinctDialog.YES;

public class InvalidRootsHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(InvalidRootsHandler.class);

    private final ClientServerRef clientServerRef;
    private final Collection<String> workspaceRoots;

    public InvalidRootsHandler(@NotNull Project project,
            @NotNull Collection<String> workspaceRoots,
            @NotNull ClientServerRef clientServerRef, @NotNull Exception ex) {
        super(project, FAKE_CONTROLLER, ex);
        this.clientServerRef = clientServerRef;
        this.workspaceRoots = Collections.unmodifiableCollection(new ArrayList<String>(workspaceRoots));
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("Invalid client root for server " + getServerKey() + " with roots " + workspaceRoots,
                getException());

        if (isInvalid()) {
            return;
        }
        final List<VirtualFile> vcsRoots = getVcs().getVcsRoots();
        List<String> vcsPresentableRoots = new ArrayList<String>(vcsRoots.size());
        for (VirtualFile vcsRoot : vcsRoots) {
            vcsPresentableRoots.add(vcsRoot.getPresentableName());
        }

        String messageKey = workspaceRoots.isEmpty()
                ? "error.config.no-workspace-roots"
                : "error.config.invalid-roots";
        DistinctDialog.performOnDialog(
                DistinctDialog.key(this, clientServerRef.getServerName(), clientServerRef.getClientName()),
                getProject(),
                P4Bundle.message(messageKey, workspaceRoots, vcsPresentableRoots),
                P4Bundle.message("error.config.invalid-roots.title", clientServerRef.getClientName()),
                new String[] { P4Bundle.message("error.config.invalid-roots.yes"),
                        P4Bundle.message("error.config.invalid-roots.no") },
                NotificationType.ERROR,
                new DistinctDialog.ChoiceActor() {
                    @Override
                    public void onChoice(int choice) {
                        if (choice == YES) {
                            // Signal to the API to try again only if
                            // the user selected "okay".
                            tryConfigChange(false);
                        }
                        // Don't go offline if not changed.
                    }
                });
    }

    private static final FakeServerConnectedController FAKE_CONTROLLER =
            new FakeServerConnectedController();
    private static class FakeServerConnectedController implements ServerConnectedController {

        @Override
        public boolean isWorkingOnline() {
            return true;
        }

        @Override
        public boolean isWorkingOffline() {
            return false;
        }

        @Override
        public boolean isDisposed() {
            return false;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void disconnect() {
            // do nothing
        }

        @Override
        public void connect(@NotNull final Project project) {
            // do nothing
        }

        @Override
        @NotNull
        public String getServerDescription() {
            return "";
        }
    }

}
