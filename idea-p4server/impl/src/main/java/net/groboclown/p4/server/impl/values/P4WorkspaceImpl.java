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

package net.groboclown.p4.server.impl.values;

import net.groboclown.p4.server.api.values.P4Workspace;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class P4WorkspaceImpl implements P4Workspace {

    public static class State {

    }


    @Override
    public List<String> getChangeViewLines() {
        return null;
    }

    @Override
    public Map<String, String> getViewLines() {
        return null;
    }

    @Override
    public String getClientName() {
        return null;
    }

    @Override
    public Date getLastUpdate() {
        return null;
    }

    @Override
    public Date getLastAccess() {
        return null;
    }

    @Override
    public String getOwner() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Map<ClientOption, Boolean> getClientOptions() {
        return null;
    }

    @Override
    public SubmitOption getSubmitOption() {
        return null;
    }

    @Override
    public LineEnding getLineEnding() {
        return null;
    }

    @Override
    public ClientType getClientType() {
        return null;
    }

    @Override
    public List<String> getRoots() {
        return null;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public String getServerId() {
        return null;
    }

    @Override
    public String getStream() {
        return null;
    }

    @Override
    public int getStreamAtChange() {
        return 0;
    }


    @NotNull
    public State getState() {
        State ret = new State();

        return ret;
    }
}
