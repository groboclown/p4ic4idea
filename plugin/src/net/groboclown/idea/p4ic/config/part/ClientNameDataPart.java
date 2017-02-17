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

package net.groboclown.idea.p4ic.config.part;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.v2.server.connection.ConnectionUIConfiguration;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectionManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A data part dedicated to selecting the client name.  It's separate
 * from the SimpleDataPart in that the UI component checks the current
 * configuration for a server to load the list of client names.
 */
public class ClientNameDataPart extends DataPartAdapter {
    static final String TAG_NAME = "client-name-data-part";
    static final ConfigPartFactory<ClientNameDataPart> FACTORY = new ClientNameDataPartFactory();
    private static final String CLIENT_NAME_ATTRIBUTE_KEY = "client-name";
    private String clientName;
    private final transient List<ConfigProblem> additionalProblems = new ArrayList<ConfigProblem>();

    @NotNull
    @Override
    public Element marshal() {
        Element ret = new Element(TAG_NAME);
        if (clientName != null) {
            ret.setAttribute(CLIENT_NAME_ATTRIBUTE_KEY, clientName);
        }
        return ret;
    }

    @Override
    public boolean reload() {
        // Nothing to do
        return false;
    }

    private static class ClientNameDataPartFactory
            extends ConfigPartFactory<ClientNameDataPart> {

        @Override
        ClientNameDataPart create(@NotNull Project project, @NotNull Element element) {
            ClientNameDataPart ret = new ClientNameDataPart();
            if (isTag(TAG_NAME, element) && element.getAttribute(CLIENT_NAME_ATTRIBUTE_KEY) != null) {
                // null value is fine here
                ret.setClientname(element.getAttribute(CLIENT_NAME_ATTRIBUTE_KEY).getValue());
            }
            return ret;
        }
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        List<ConfigProblem> problems = new ArrayList<ConfigProblem>(additionalProblems);
        if (clientName == null) {
            problems.add(new ConfigProblem(this, "error.config.no-client"));
        }
        return problems;
    }


    @Override
    public boolean hasClientnameSet() {
        return true;
    }

    @Nullable
    @Override
    public String getClientname() {
        return clientName;
    }

    public void setClientname(@Nullable String clientName) {
        this.clientName = clientName;
    }



    public static Map<ClientConfig, ConnectionUIConfiguration.ClientResult> loadClientNames(@NotNull Collection<ClientConfig> config) {
        return loadClientNames(ServerConnectionManager.getInstance(), config);
    }


    @NotNull
    private static Map<ClientConfig, ConnectionUIConfiguration.ClientResult> loadClientNames(
            @NotNull ServerConnectionManager connectionManager,
            @NotNull Collection<ClientConfig> clientConfigs) {
        return ConnectionUIConfiguration.getClients(clientConfigs, connectionManager);
    }

    public void addAdditionalProblem(@NotNull ConfigProblem problem) {
        additionalProblems.add(problem);
    }

    public void clearAdditionalProblems() {
        additionalProblems.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! getClass().equals(o.getClass())) {
            return false;
        }
        ClientNameDataPart that = (ClientNameDataPart) o;
        return StringUtil.equals(getClientname(), that.getClientname());
    }

    @Override
    public int hashCode() {
        if (getClientname() == null) {
            return 0;
        }
        return getClientname().hashCode();
    }
}
