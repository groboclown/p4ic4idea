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
package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.part.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class P4ProjectConfigStack implements P4ProjectConfig {
    private final Object sync = new Object();

    private final Project project;

    @NotNull
    private ConfigPart parts = new MutableCompositePart();

    // Maps the directory tree of the client config root directory
    // to the corresponding client config.  The client config root
    // directory that is "closest" to the config file wins.
    @NotNull
    private Map<VirtualFile, ClientConfig> configs = Collections.emptyMap();

    @NotNull
    private Set<ConfigProblem> problems = Collections.emptySet();

    public P4ProjectConfigStack(@NotNull Project project, @NotNull List<ConfigPart> userParts) {
        this.project = project;
        MutableCompositePart newParts = new MutableCompositePart();
        newParts.addConfigPart(new DefaultDataPart());
        for (ConfigPart userPart : userParts) {
            newParts.addPriorityConfigPart(userPart);
        }
        loadValidConfigs(newParts);
    }

    @Override
    public void refresh() {
        loadValidConfigs(parts);
    }

    @NotNull
    @Override
    public Collection<ClientConfig> getClientConfigs() {
        return new HashSet<ClientConfig>(configs.values());
    }

    @NotNull
    @Override
    public Collection<ServerConfig> getServerConfigs() {
        Set<ServerConfig> servers = new HashSet<ServerConfig>();
        for (ClientConfig clientConfig : configs.values()) {
            servers.add(clientConfig.getServerConfig());
        }
        return servers;
    }

    @Nullable
    @Override
    public ClientConfig getClientConfigFor(@NotNull FilePath file) {
        return getClientConfigFor(file.getVirtualFile());
    }

    @Nullable
    @Override
    public ClientConfig getClientConfigFor(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }

        VirtualFile current = file;
        while (true) {
            if (configs.containsKey(current)) {
                return configs.get(current);
            }

            VirtualFile next = current.getParent();
            if (next == null || next.equals(current)) {
                break;
            }
            current = next;
        }

        return null;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return problems;
    }

    boolean isEmpty() {
        return configs.isEmpty();
    }

    private void loadValidConfigs(ConfigPart rootPart) {
        VirtualFile projectRoot = project.getBaseDir();
        if (! projectRoot.isDirectory()) {
            projectRoot = projectRoot.getParent();
        }
        final Map<VirtualFile, List<DataPart>> dirToParts = new HashMap<VirtualFile, List<DataPart>>();
        final Set<ConfigProblem> configProblems = new HashSet<ConfigProblem>();

        List<ConfigPart> stack = new ArrayList<ConfigPart>();
        stack.add(rootPart);
        while (! stack.isEmpty()) {
            ConfigPart part = stack.remove(stack.size() - 1);
            if (part.getConfigProblems().isEmpty()) {
                if (part instanceof CompositePart) {
                    stack.addAll(((CompositePart) part).getConfigParts());
                } else if (part instanceof DataPart) {
                    final DataPart dataPart = (DataPart) part;
                    VirtualFile partRoot = dataPart.getRootPath();
                    if (partRoot == null) {
                        partRoot = projectRoot;
                    }
                    List<DataPart> partList = dirToParts.get(partRoot);
                    if (partList == null) {
                        partList = new ArrayList<DataPart>();
                        dirToParts.put(partRoot, partList);
                    }
                    partList.add(dataPart);
                } else {
                    throw new IllegalStateException("Unknown config part " + part);
                }
            } else {
                configProblems.addAll(part.getConfigProblems());
            }
        }

        Map<VirtualFile, ClientConfig> tmpConfigs = convertToClients(projectRoot, dirToParts, configProblems);

        synchronized (sync) {
            parts = rootPart;
            configs = tmpConfigs;
            problems = configProblems;
        }
    }

    private Map<VirtualFile, ClientConfig> convertToClients(
            @NotNull VirtualFile projectRoot, @NotNull Map<VirtualFile, List<DataPart>> parts,
            @NotNull Set<ConfigProblem> configProblems) {
        // For each part file, climb up our tree and add its parent as a lower priority
        // item.
        for (Map.Entry<VirtualFile, List<DataPart>> entry : parts.entrySet()) {
            VirtualFile previous = entry.getKey();
            VirtualFile current = entry.getKey().getParent();
            while (current != null && ! current.equals(previous) && ! projectRoot.equals(current)) {
                if (parts.containsKey(current)) {
                    for (DataPart dataPart : parts.get(current)) {
                        if (! entry.getValue().contains(dataPart)) {
                            entry.getValue().add(dataPart);
                        }
                    }
                }
            }
        }

        // We now have the complete list of client servers, mapped to
        // their section in the project tree.  We now need to organize these
        // into shared ClientConfig objects, while maintaining their
        // organization in the tree.

        // We need to share the ServerConfig object across clients.
        final Map<String, ServerConfig> serverIdMap = new HashMap<String, ServerConfig>();

        // Map each root directory to the setup object.
        List<ClientServerSetup> cachedSetups = new ArrayList<ClientServerSetup>();

        for (Map.Entry<VirtualFile, List<DataPart>> entry : parts.entrySet()) {
            MultipleDataPart part = new MultipleDataPart(entry.getKey(), entry.getValue());
            Collection<ConfigProblem> partProblems = ServerConfig.getProblems(part);
            if (partProblems.isEmpty()) {
                // Note: for a bit of efficiency, we can maybe have a static function
                // that constructs the server ID, so that we don't have excess ServerConfig
                // objects created.  However, the construction of the server ID would require
                // much of the same logic as the construction of the ServerConfig object.
                // There's a bit of memory allocation waste, but that's not significant.
                ServerConfig serverConfig = ServerConfig.createFrom(part);
                final String serverId = serverConfig.getServerId();
                if (serverIdMap.containsKey(serverId)) {
                    // Throws away the just constructed server config object.
                    serverConfig = serverIdMap.get(serverId);
                } else {
                    serverIdMap.put(serverId, serverConfig);
                }

                // Find the cached client server setup object to add this to, if it exists.
                boolean found = false;
                for (ClientServerSetup clientServerSetup : cachedSetups) {
                    if (clientServerSetup.addIfSame(serverConfig, part, entry.getKey())) {
                        found = true;
                        break;
                    }
                }
                if (! found) {
                    // need to create a new one.
                    cachedSetups.add(new ClientServerSetup(serverConfig, part, entry.getKey()));
                }
            } else {
                configProblems.addAll(partProblems);
            }
        }

        Map<VirtualFile, ClientConfig> ret = new HashMap<VirtualFile, ClientConfig>();
        for (ClientServerSetup cachedSetup : cachedSetups) {
            for (VirtualFile root : cachedSetup.roots) {
                ret.put(root, cachedSetup.getClientConfig(project));
            }
        }

        return ret;
    }


    private static int getDepthDistance(@NotNull VirtualFile base, @NotNull VirtualFile cmp) {
        int depth = getSingleDepthDistance(base, cmp);
        if (depth >= 0) {
            return depth;
        }

        // cmp is not under base; see if base is under cmp.
        depth = getSingleDepthDistance(cmp, base);
        if (depth >= 0) {
            // base is under cmp, so return the negative depth.
            return -depth;
        }

        // base is not under the same root as cmp.
        return Integer.MIN_VALUE;
    }

    private static int getSingleDepthDistance(@NotNull VirtualFile base, @NotNull VirtualFile cmp) {
        VirtualFile previous = null;
        VirtualFile current = cmp;
        int depth = 0;
        while (current != null && ! current.equals(previous)) {
            if (base.equals(current)) {
                return depth;
            }
            previous = current;
            current = previous.getParent();
            depth++;
        }

        // cmp is not under base.
        return Integer.MIN_VALUE;
    }

    /**
     * Class used as a way-point in the construction of a
     * ClientConfig.  It has the messy implications of the
     * limitations inherent in the ClientServerRef object.
     */
    private static class ClientServerSetup {
        private final ServerConfig serverConfig;
        private final String clientName;
        private final MultipleDataPart dataPart;
        private final Set<VirtualFile> roots = new HashSet<VirtualFile>();
        private ClientConfig clientConfig;


        private ClientServerSetup(@NotNull ServerConfig serverConfig, @NotNull MultipleDataPart dataPart,
                @NotNull VirtualFile path) {
            this.serverConfig = serverConfig;
            this.clientName = dataPart.getClientname();
            this.dataPart = dataPart;
            this.roots.add(path);
        }

        ClientConfig getClientConfig(@NotNull Project project) {
            if (clientConfig == null) {
                clientConfig = ClientConfig.createFrom(project, serverConfig, dataPart, roots);
            }
            return clientConfig;
        }

        /**
         *
         * @return true if the same config.
         */
        boolean addIfSame(@NotNull ServerConfig serverConfig, @NotNull MultipleDataPart dataPart,
                @NotNull VirtualFile path) {
            if (serverConfig.getServerName().equals(this.serverConfig.getServerName())) {
                if ((clientName == null && dataPart.getClientname() == null) ||
                        (clientName != null && clientName.equals(dataPart.getClientname()))) {
                    roots.add(path);
                    return true;
                }
            }
            return false;
        }
    }
}
