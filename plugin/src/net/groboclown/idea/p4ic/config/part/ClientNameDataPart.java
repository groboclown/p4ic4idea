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
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.v2.server.connection.ConnectionUIConfiguration;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectionManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A data part dedicated to selecting the client name.  It's separate
 * from the SimpleDataPart in that the UI component checks the current
 * configuration for a server to load the list of client names.
 */
public class ClientNameDataPart implements DataPart {
    static final String TAG_NAME = "client-name-data-part";
    static final ConfigPartFactory<ClientNameDataPart> FACTORY = new ClientNameDataPartFactory();
    private static final String CLIENT_NAME_ATTRIBUTE_KEY = "client-name";
    private String clientName;
    private final List<ConfigProblem> additionalProblems = new ArrayList<ConfigProblem>();

    @NotNull
    @Override
    public Element marshal() {
        Element ret = new Element(TAG_NAME);
        if (clientName != null) {
            ret.setAttribute(CLIENT_NAME_ATTRIBUTE_KEY, clientName);
        }
        return ret;
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
            problems.add(new ConfigProblem("error.config.no-client"));
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



    public static ConnectionUIConfiguration.ClientResult loadClientNames(@NotNull ClientConfig config) {
        /*
        final ClientConfig clientConfig = ClientConfig.createFrom(project,
                ServerConfig.createFrom(config), config,
                Collections.singletonList(project.getBaseDir()));
        */
        return loadClientNames(ServerConnectionManager.getInstance(), config);
    }


    @NotNull
    private static ConnectionUIConfiguration.ClientResult loadClientNames(
            @NotNull ServerConnectionManager connectionManager,
            @NotNull ClientConfig clientConfig) {
        final Map<ClientConfig, ConnectionUIConfiguration.ClientResult> clientResults =
                ConnectionUIConfiguration.getClients(
                        Collections.singletonList(clientConfig), connectionManager);
        if (clientResults == null || ! clientResults.containsKey(clientConfig)) {
            throw new IllegalStateException("Did not generate key for config");
        }
        return clientResults.get(clientConfig);
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


    // --------------------------------------------------------------------
    // All the non-implemented methods.

    @Override
    public boolean reload() {
        return false;
    }

    @Nullable
    @Override
    public VirtualFile getRootPath() {
        return null;
    }

    @Override
    public boolean hasServerNameSet() {
        return false;
    }

    @Nullable
    @Override
    public P4ServerName getServerName() {
        return null;
    }

    @Override
    public boolean hasUsernameSet() {
        return false;
    }

    @Nullable
    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public boolean hasPasswordSet() {
        return false;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        return null;
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        return false;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        return null;
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        return false;
    }

    @Nullable
    @Override
    public File getTrustTicketFile() {
        return null;
    }

    @Override
    public boolean hasServerFingerprintSet() {
        return false;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return null;
    }

    @Override
    public boolean hasClientHostnameSet() {
        return false;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        return null;
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        return false;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        return null;
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        return false;
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        return null;
    }
}
