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

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.v2.server.connection.ServerStatusController;
import org.jetbrains.annotations.NotNull;

public class MockServerStatusController implements ServerStatusController {
    public boolean connected;
    public boolean autoOffline;
    public boolean valid;




    @Override
    public void onConnected() {
        connected = true;
    }

    @Override
    public void onDisconnected() {
        connected = false;
    }

    @Override
    public void onConfigInvalid() {
        valid = false;
    }

    @Override
    public boolean isWorkingOnline() {
        return connected;
    }

    @Override
    public boolean isWorkingOffline() {
        return ! connected;
    }

    @Override
    public boolean isAutoOffline() {
        return autoOffline;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    @Override
    public void connect(@NotNull final Project project) {
        connected = true;
    }
}
