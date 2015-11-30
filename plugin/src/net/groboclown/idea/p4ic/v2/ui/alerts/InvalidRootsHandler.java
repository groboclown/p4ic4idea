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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class InvalidRootsHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(InvalidRootsHandler.class);

    private final ClientServerId clientServerId;

    public InvalidRootsHandler(@NotNull Project project,
            @NotNull ClientServerId clientServerId, @NotNull Exception ex) {
        super(project, FAKE_CONTROLLER, ex);
        this.clientServerId = clientServerId;
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("Invalid client root", getException());

        if (isInvalid()) {
            return;
        }

        int result = Messages.showYesNoDialog(getProject(),
                P4Bundle.message("error.config.invalid-roots"),
                P4Bundle.message("error.config.invalid-roots.title", clientServerId.getClientId()),
                P4Bundle.message("error.config.invalid-roots.yes"),
                P4Bundle.message("error.config.invalid-roots.no"),
                Messages.getErrorIcon());
        if (result == Messages.YES) {
            // Signal to the API to try again only if
            // the user selected "okay".
            tryConfigChange(false);
        }
        // Don't go offline if not changed.
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
        public boolean isAutoOffline() {
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
    }

}
