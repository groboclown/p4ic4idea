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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.part.*;
import net.groboclown.idea.p4ic.util.EqualUtil;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class P4ProjectConfigStack implements P4ProjectConfig {
    private static final Logger LOG = Logger.getInstance(P4ProjectConfigStack.class);


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
    private Set<ClientConfigSetup> configSetups = Collections.emptySet();

    @NotNull
    private Set<ConfigProblem> problems = Collections.emptySet();

    public P4ProjectConfigStack(@NotNull Project project, @NotNull List<ConfigPart> userParts) {
        this.project = project;
        MutableCompositePart newParts = new MutableCompositePart(new DefaultDataPart());
        for (ConfigPart userPart : userParts) {
            newParts.addPriorityConfigPart(userPart);
        }
        loadConfigs(newParts);
    }

    @Override
    public void refresh() {
        loadConfigs(parts);
    }

    @NotNull
    @Override
    public Collection<ClientConfigSetup> getClientConfigSetups() {
        return configSetups;
    }

    @NotNull
    @Override
    public Collection<ClientConfig> getClientConfigs() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Client configs: " + configs);
        }
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

    @Override
    public boolean hasConfigErrors() {
        for (ConfigProblem configProblem : getConfigProblems()) {
            if (configProblem.isError()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }

    boolean isEmpty() {
        return configs.isEmpty();
    }

    private void loadConfigs(ConfigPart rootPart) {
        LOG.debug("Creating configuration from parts");
        // Null can happen if we're using the default settings.
        @Nullable
        VirtualFile projectRoot = project.getBaseDir();
        if (projectRoot != null && ! projectRoot.isDirectory()) {
            projectRoot = projectRoot.getParent();
        }
        final Map<VirtualFile, List<DataPart>> dirToParts = new HashMap<VirtualFile, List<DataPart>>();
        final Set<ConfigProblem> configProblems = new HashSet<ConfigProblem>();

        List<ConfigPart> stack = new ArrayList<ConfigPart>();
        stack.add(rootPart);
        while (! stack.isEmpty()) {
            ConfigPart part = stack.remove(stack.size() - 1);
            configProblems.addAll(part.getConfigProblems());
            if (part instanceof CompositePart) {
                // Preserve the correct ordering; as this is a stack, we need to
                // add the items in reverse order.
                List<ConfigPart> toAdd = new ArrayList<ConfigPart>(((CompositePart) part).getConfigParts());
                Collections.reverse(toAdd);
                stack.addAll(toAdd);
            } else if (part instanceof DataPart) {
                final DataPart dataPart = (DataPart) part;
                VirtualFile partRoot = dataPart.getRootPath();
                // Push parent directories of the project root into the project root.
                // #148: Don't set the root directory to the parent project if the
                // given file is lower than the project root.
                // if (partRoot == null || (projectRoot != null && FilePathUtil.isSameOrUnder(partRoot, projectRoot))) {
                if (partRoot == null) {
                    partRoot = projectRoot;
                }
                List<DataPart> partList = dirToParts.get(partRoot);
                if (partList == null) {
                    partList = new ArrayList<DataPart>();
                    dirToParts.put(partRoot, partList);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Adding " + dataPart + " into " + partRoot);
                }
                partList.add(dataPart);
            } else {
                throw new IllegalStateException("Unknown config part " + part);
            }
        }

        final Set<ClientConfigSetup> clientConfigSetups = new HashSet<ClientConfigSetup>();
        final Map<VirtualFile, ClientConfig> tmpConfigs = convertToClients(projectRoot, dirToParts, configProblems,
                clientConfigSetups);

        synchronized (sync) {
            parts = rootPart;
            configs = tmpConfigs;
            problems = configProblems;
            configSetups = clientConfigSetups;
        }
    }

    private Map<VirtualFile, ClientConfig> convertToClients(
            @Nullable VirtualFile projectRoot, @NotNull Map<VirtualFile, List<DataPart>> parts,
            @NotNull Set<ConfigProblem> configProblems,
            @NotNull Set<ClientConfigSetup> clientConfigSetups) {
        // For each part file, climb up our tree and add its parent as a lower priority
        // item.
        for (Map.Entry<VirtualFile, List<DataPart>> entry : parts.entrySet()) {
            @Nullable
            VirtualFile previous = entry.getKey();
            // #148: Don't set the root to the project root if it's lower than it.
            // if (previous == null || (projectRoot != null && FilePathUtil.isSameOrUnder(previous, projectRoot))) {
            if (previous == null) {
                previous = projectRoot;
            }
            if (previous != null) {
                for (FilePath parent : FilePathUtil.getTreeTo(previous, projectRoot)) {
                    // Put all the parent's data parts into the child, as lower priority
                    // items (at the end of the child's list).
                    List<DataPart> parentPartList = parts.get(parent.getVirtualFile());
                    if (parentPartList != null) {
                        List<DataPart> childParts = entry.getValue();
                        for (DataPart dataPart : parentPartList) {
                            if (! childParts.contains(dataPart)) {
                                childParts.add(dataPart);
                            }
                        }
                    }
                }
            }
        }


        // We now have the complete list of client servers, mapped to
        // their section in the project tree.  We now need to organize these
        // into shared ClientConfig objects, while maintaining their
        // organization in the tree.

        // With this, there are two structures to maintain - the desired
        // list, which may have errors, and the validated list.

        // We need to share the ServerConfig object across clients.
        final Map<String, ServerConfig> serverIdMap = new HashMap<String, ServerConfig>();

        // Map each root directory to the setup object.
        final List<ClientServerSetup> cachedSetups = new ArrayList<ClientServerSetup>();

        for (Map.Entry<VirtualFile, List<DataPart>> entry : parts.entrySet()) {
            // path can be null if we're in a default settings panel.
            @Nullable
            VirtualFile path = entry.getKey();
            MultipleDataPart part = new MultipleDataPart(path, entry.getValue());
            final Collection<ConfigProblem> partProblems = ServerConfig.getErrors(part);
            configProblems.addAll(partProblems);
            final String serverId = ServerConfig.getServerIdForDataPart(part);
            // We only want to create a server config if there are no problems.
            // Otherwise, a validation error will be created.
            final ServerConfig serverConfig;
            if (partProblems.isEmpty()) {
                if (serverIdMap.containsKey(serverId)) {
                    // Throws away the just constructed server config object.
                    serverConfig = serverIdMap.get(serverId);
                } else {
                    serverConfig = ServerConfig.createFrom(part);
                    serverIdMap.put(serverId, serverConfig);
                }
            } else {
                serverConfig = null;
            }

            // Find the cached client server setup object to add this to, if it exists.
            if (path != null) {
                boolean found = false;
                for (ClientServerSetup clientServerSetup : cachedSetups) {
                    if (clientServerSetup.addIfSame(part, path)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // need to create a new one.
                    cachedSetups.add(new ClientServerSetup(serverConfig, part, path));
                }
            }
        }

        // Create the file tree of client server mappings.
        Map<VirtualFile, ClientServerSetup> fileMappedSetup = new HashMap<VirtualFile, ClientServerSetup>();
        for (ClientServerSetup cachedSetup : cachedSetups) {
            for (VirtualFile root : cachedSetup.roots) {
                fileMappedSetup.put(root, cachedSetup);
            }
        }

        // We now need the final step of cleaning up the roots.
        // There can be a situation where some roots in the list are below the project root.
        // In this case, find the root that is "closest" to the base.
        List<VirtualFile> belowProjectRoot = new ArrayList<VirtualFile>();
        VirtualFile bestProjectRoot = null;
        int bestProjectRootDistance = Integer.MIN_VALUE;
        for (Map.Entry<VirtualFile, ClientServerSetup> entry : fileMappedSetup.entrySet()) {
            final VirtualFile base = entry.getKey();
            final int depth = getDepthDistance(projectRoot, base);
            if (depth <= 0 && depth > bestProjectRootDistance) {
                belowProjectRoot.add(base);
                bestProjectRoot = base;
                bestProjectRootDistance = depth;
            }
        }
        if (bestProjectRoot != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Best root config: " + bestProjectRoot);
                LOG.debug("All root configs " + belowProjectRoot);
            }
            final ClientServerSetup bestConfig = fileMappedSetup.get(bestProjectRoot);
            belowProjectRoot.remove(bestProjectRoot);
            for (VirtualFile file : belowProjectRoot) {
                fileMappedSetup.remove(file);
            }
            fileMappedSetup.put(projectRoot, bestConfig);
        } else {
            LOG.debug("No root configs found at or under the project root.");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Final pruned config directories: " + fileMappedSetup.keySet());
        }

        // Now we can create the valid configs and the requested setup configs.
        Map<VirtualFile, ClientConfig> ret = new HashMap<VirtualFile, ClientConfig>();
        for (Map.Entry<VirtualFile, ClientServerSetup> entry : fileMappedSetup.entrySet()) {
            final ClientConfig clientConfig = entry.getValue().getClientConfig(project);
            if (clientConfig != null) {
                ret.put(entry.getKey(), clientConfig);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("null client config for directory " + entry.getKey() + ", value " + entry.getValue());
            }
            clientConfigSetups.add(entry.getValue().getClientConfigSetup(project));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Computed config directory to client config mapping: " + ret);
        }

        return ret;
    }


    private static int getDepthDistance(@Nullable VirtualFile base, @NotNull VirtualFile cmp) {
        int depth = getSingleDepthDistance(base, cmp);
        if (depth >= 0) {
            return depth;
        }

        // cmp is not under base; see if base is under cmp.
        if (base == null) {
            return Integer.MIN_VALUE;
        }
        depth = getSingleDepthDistance(cmp, base);
        if (depth >= 0) {
            // base is under cmp, so return the negative depth.
            return -depth;
        }

        // base is not under the same root as cmp.
        return Integer.MIN_VALUE;
    }

    private static int getSingleDepthDistance(@Nullable VirtualFile base, @NotNull VirtualFile cmp) {
        VirtualFile previous = null;
        VirtualFile current = cmp;
        int depth = 0;
        while (current != null && ! current.equals(previous)) {
            if (EqualUtil.isEqual(base, current)) {
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
        private final Set<ConfigProblem> problems = new HashSet<ConfigProblem>();
        private ClientConfig clientConfig;
        private ClientConfigSetup clientSetup;


        private ClientServerSetup(@Nullable ServerConfig serverConfig, @NotNull MultipleDataPart dataPart,
                @NotNull VirtualFile path) {
            if (serverConfig != null && ! serverConfig.isSameServer(dataPart)) {
                LOG.warn("Server config " + serverConfig + " does not match "
                        + ConfigPropertiesUtil.toProperties(dataPart) + ".  Turn on debugging for category #"
                        + ServerConfig.class.getName() + " to understand where the mismatch occurred.");
            }
            if (LOG.isDebugEnabled() && serverConfig == null) {
                LOG.debug("Created ClientServerSetup with null server config for " +
                    dataPart);
            }
            this.serverConfig = serverConfig;
            this.clientName = dataPart.getClientname();
            this.dataPart = dataPart;
            this.problems.addAll(dataPart.getConfigProblems());
            this.roots.add(path);
        }

        ClientConfig getClientConfig(@NotNull Project project) {
            // Data part must be valid to have a client config.
            if (clientConfig == null && ! dataPart.hasError() && serverConfig != null) {
                clientConfig = ClientConfig.createFrom(project, serverConfig, dataPart, roots);
            }
            if (LOG.isDebugEnabled() && clientConfig == null) {
                LOG.debug("null clientConfig: has errors? " + dataPart.hasError() +
                        ", server config: " + serverConfig);
                if (dataPart.hasError()) {
                    LOG.debug(" - errors: " + dataPart.getConfigProblems());
                }
            }
            return clientConfig;
        }

        ClientConfigSetup getClientConfigSetup(@NotNull Project project) {
            if (clientSetup == null) {
                clientSetup = new ClientConfigSetup(getClientConfig(project), problems, dataPart);
            }
            return clientSetup;
        }

        /**
         *
         * @return true if the same config.
         */
        boolean addIfSame(@NotNull MultipleDataPart dataPart, @NotNull VirtualFile path) {
            final P4ServerName serverName = dataPart.getServerName();
            if (EqualUtil.isEqual(serverName, this.dataPart.getServerName()) &&
                    EqualUtil.isEqual(clientName, dataPart.getClientname())) {
                problems.addAll(dataPart.getConfigProblems());
                roots.add(path);
                return true;
            }
            return false;
        }
    }
}
