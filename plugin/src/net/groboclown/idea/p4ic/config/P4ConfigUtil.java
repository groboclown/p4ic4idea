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
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.env.PerforceEnvironment;
import com.perforce.p4java.server.IServerAddress;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class P4ConfigUtil {
    private static final Logger LOG = Logger.getInstance(P4ConfigUtil.class);

    public static final String PROTOCOL_SEP = "://";


    @Nullable
    public static String toFullPort(@Nullable IServerAddress.Protocol protocol, @Nullable String simplePort) {
        if (protocol == null && simplePort == null) {
            return null;
        }
        String ret = "";
        if (simplePort != null) {
            ret = simplePort;
        }
        if (protocol != null) {
            switch (protocol) {
                case P4JRPC:
                    ret = "rpc" + PROTOCOL_SEP + ret;
                    break;
                case P4JRPCSSL:
                    ret = "rpcssl" + PROTOCOL_SEP + ret;
                    break;
                case P4JRPCNTS:
                    ret = "nts" + PROTOCOL_SEP + ret;
                    break;
                case P4JRPCNTSSSL:
                    ret = "ntsssl" + PROTOCOL_SEP + ret;
                    break;
                case P4JAVASSL:
                    ret = "ssl" + PROTOCOL_SEP + ret;
                    break;
                case P4JAVA:
                default:
                    // do nothing - it's the default
                    break;
            }
        }
        return ret;
    }

    @Nullable
    public static String getSimplePortFromPort(String port) {
        return portSplit(port)[1];
    }


    @Nullable
    public static IServerAddress.Protocol getProtocolFromPort(String port) {
        String protocol = portSplit(port)[0];

        // Bug #109: Switch the default connections over to the Nts server impl.
        // Only if the user explicitly requests the old one do we use it.

        if (protocol == null) {
            return IServerAddress.Protocol.P4JRPCNTS;
        }
        protocol = protocol.toLowerCase();
        if ("ssl".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCNTSSSL;
        }
        if ("tcp".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCNTS;
        }
        if ("javassl".equals(protocol) ||
                "javas".equals(protocol)) {
            // explicit request for the old ones
            return IServerAddress.Protocol.P4JAVASSL;
        }
        if ("java".equals(protocol)) {
            // explicit request for the old ones
            return IServerAddress.Protocol.P4JAVA;
        }
        if ("rpc".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPC;
        }
        if ("rpcs".equals(protocol) ||
                "rpcssl".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCSSL;
        }
        if ("nts".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCNTS;
        }
        if ("ntss".equals(protocol) ||
                "ntsssl".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCNTSSSL;
        }
        return null;
        //return IServerAddress.Protocol.P4JAVA;
    }


    public static boolean isPortModified(@NotNull P4Config current, @Nullable String setting) {
        String port = getSimplePortFromPort(setting);
        IServerAddress.Protocol protocol = getProtocolFromPort(setting);
        if (current.getProtocol() != protocol) {
            return true;
        }
        if (current.getPort() == null) {
            return port != null;
        } else {
            return current.getPort().equals(port);
        }
    }


    private static String[] portSplit(@Nullable String port) {
        String[] ret = new String[] { null, port };
        if (port != null) {
            int splitter = port.indexOf(PROTOCOL_SEP);
            if (splitter >= port.length() - 1) {
                // ':' is on the last character, which is invalid
                // set the value to an invalid setting, but not null
                // to avoid an NPE.
                ret[1] = ":";
            } else if (splitter >= 0) {
                ret[0] = port.substring(0, splitter);
                ret[1] = port.substring(splitter + PROTOCOL_SEP.length());
            } else {
                // based on http://www.perforce.com/perforce/r14.1/manuals/p4guide/chapter.configuration.html
                // format can be "port", "hostname:port", "ssl:hostname:port", "tcp:hostname:port"
                splitter = port.indexOf(':');
                if (splitter > 0) {
                    int splitter2 = port.indexOf(':', splitter + 1);
                    if (splitter2 > 0) {
                        ret[0] = port.substring(0, splitter);
                        ret[1] = port.substring(splitter + 1);
                    }
                }
            }

            if (ret[1].indexOf(':') < 0) {
                // This is the form "port", which is not supported by the
                // P4 java api.  So we must prepend a localhost to conform
                // to what P4 java supports.
                ret[1] = "localhost:" + ret[1];
            }
        }
        return ret;
    }


    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     * @return the default ticket file, which is OS dependent.
     */
    @NotNull
    public static File getDefaultTrustTicketFile() {
        // Windows check
        String userprofile = System.getenv("USERPROFILE");
        if (userprofile != null) {
            return new File(userprofile +
                File.separator + "p4trust.txt");
        }
        return new File(System.getenv("HOME") + File.separator + ".p4trust");
    }


    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     *
     * @return the default ticket file, which is OS dependent.
     */
    @NotNull
    public static File getDefaultTicketFile() {
        // Windows check
        String userprofile = System.getenv("USERPROFILE");
        if (userprofile != null) {
            return new File(userprofile +
                    File.separator + "p4tickets.txt");
        }
        return new File(System.getenv("HOME") + File.separator + ".p4tickets");
    }


    @NotNull
    public static Map<String, String> getProperties(@Nullable P4Config config) {
        if (config == null) {
            return Collections.emptyMap();
        }
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(PerforceEnvironment.P4CLIENT, config.getClientname());
        ret.put(PerforceEnvironment.P4CONFIG, config.getConfigFile());
        ret.put(PerforceEnvironment.P4PASSWD,
                config.getPassword() == null ? "<no password>" : "<password provided>");
        ret.put(PerforceEnvironment.P4PORT, toFullPort(config.getProtocol(), config.getPort()));
        ret.put(PerforceEnvironment.P4TRUST, config.getTrustTicketPath());
        ret.put(PerforceEnvironment.P4USER, config.getUsername());
        ret.put(PerforceEnvironment.P4TICKETS, config.getAuthTicketPath());

        // Even though this is kind of a back-door item (it can only be set through
        // environment or p4config files), it should still be
        // present so that people can kind of discover it and realize that it's
        // supported without looking at the documentation.
        ret.put(PerforceEnvironment.P4HOST, config.getClientHostname());

        return ret;
    }


    /**
     * Loads a configuration that simulates how the Perforce command-line
     * tool searches for configuration values, but searches the root
     * path down for P4CONFIG files, if any relative config file is
     * specified.
     * <p>
     * P4 cmd actually references relative P4CONFIG files a bit differently.
     * It searches the current path and its  parent directories for
     * a file whose name matches the P4CONFIG value.  This will instead
     * look up for the value, and if a file isn't in the root directory,
     * look down (if searchRootParents is true).
     * </p>
     *
     * @return mapping between directories and their corresponding configuration.
     */
    @NotNull
    public static Map<VirtualFile, P4Config> loadProjectP4Configs(
            @NotNull Project project,
            @NotNull String configFileName,
            boolean searchRootParents) {
        Map<VirtualFile, P4Config> ret = new HashMap<VirtualFile, P4Config>();
        for (VirtualFile root : P4Vcs.getInstance(project).getVcsRoots()) {
            ret.putAll(loadProjectP4ConfigsForPath(root, configFileName, searchRootParents));
        }
        return ret;
    }


    //
    @NotNull
    public static Map<VirtualFile, P4Config> loadCorrectDirectoryP4Configs(
            @NotNull Project project,
            @NotNull String configFileName) {
        Map<VirtualFile, P4Config> ret = new HashMap<VirtualFile, P4Config>();
        for (VirtualFile root : P4Vcs.getInstance(project).getVcsRoots()) {
            loadCorrectRootDirP4ConfigsForPath(root, configFileName, ret);
        }
        return ret;
    }


    /**
     * Loads a configuration that simulates how the Perforce command-line
     * tool searches for configuration values, but searches the root
     * path down for P4CONFIG files, if any relative config file is
     * specified.
     * <p>
     * P4 cmd actually references relative P4CONFIG files a bit differently.
     * It searches the current path and its  parent directories for
     * a file whose name matches the P4CONFIG value.  This will instead
     * look up for the value, and if a file isn't in the root directory,
     * look down (if searchRootParents is true).
     * </p>
     *
     * @return mapping between directories and their corresponding configuration.
     */
    @NotNull
    public static Map<VirtualFile, P4Config> loadProjectP4Configs(
            @NotNull List<VirtualFile> rootFolders,
            @NotNull String configFileName,
            boolean searchRootParents) {
        Map<VirtualFile, P4Config> ret = new HashMap<VirtualFile, P4Config>();
        for (VirtualFile root: rootFolders) {
            ret.putAll(loadProjectP4ConfigsForPath(root, configFileName, searchRootParents));
        }
        return ret;
    }

    /**
     * Loads a configuration that simulates how the Perforce command-line
     * tool searches for configuration values, but searches the root
     * path down for P4CONFIG files, if any relative config file is
     * specified.
     * <p>
     * P4 cmd actually references relative P4CONFIG files a bit differently.
     * It searches the current path and its  parent directories for
     * a file whose name matches the P4CONFIG value.  This will instead
     * look up for the value, and if a file isn't in the root directory,
     * look down (if searchRootParents is true).
     * </p>
     *
     * @return mapping between directories and their corresponding configuration.
     */
    @NotNull
    private static Map<VirtualFile, P4Config> loadProjectP4ConfigsForPath(
            @NotNull VirtualFile rootSearchPath,
            @NotNull String configFileName,
            boolean searchRootParents) {
        Map<VirtualFile, P4Config> ret = new HashMap<VirtualFile, P4Config>();
        if (! rootSearchPath.isDirectory() || ! rootSearchPath.exists()) {
            throw new IllegalArgumentException(P4Bundle.message("error.roots.not-directory", rootSearchPath));
        }
        List<Iterator<VirtualFile>> depthStack = new ArrayList<Iterator<VirtualFile>>();
        depthStack.add(Arrays.asList(rootSearchPath.getChildren()).iterator());
        // bug #32 - make sure to add in the root directory, too.
        depthStack.add(Collections.singleton(rootSearchPath).iterator());
        while (! depthStack.isEmpty()) {
            Iterator<VirtualFile> iter = depthStack.remove(depthStack.size() - 1);
            if (iter.hasNext()) {
                VirtualFile vFile = iter.next();
                if (iter.hasNext()) {
                    depthStack.add(iter);
                }
                if (vFile != null) {
                    // Make sure we use the actual I/O file in order to avoid some
                    // IDEA refresh issues.
                    File file = new File(vFile.getPath());
                    if (file.isDirectory()) {
                        depthStack.add(Arrays.asList(vFile.getChildren()).iterator());
                    } else if (! file.exists()) {
                        LOG.info("Discovered non-existent file in IDEA cache: " + file);
                    } else if (file.getName().equals(configFileName)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found config file " + file);
                        }
                        ManualP4Config config = new ManualP4Config();
                        config.setConfigFile(file.getAbsolutePath());
                        try {
                            final P4Config loadedConfig = loadCmdP4Config(config);
                            ret.put(vFile.getParent(), loadedConfig);
                        } catch (IOException e) {
                            LOG.error("Could not find or read config file " + file.getPath(), e);
                        }
                    }
                }
            }
        }

        if (searchRootParents && ! ret.containsKey(rootSearchPath)) {
            VirtualFile parent = rootSearchPath.getParent();
            while (parent != null) {
                VirtualFile configFile = parent.findChild(configFileName);
                if (configFile != null) {
                    File file = new File(configFile.getPath());
                    if (file.exists() && file.isFile()) {
                        LOG.info("Found config file " + configFile.getPath() +
                                ", but registering it as root of " + rootSearchPath);
                        ManualP4Config config = new ManualP4Config();
                        config.setConfigFile(file.getAbsolutePath());

                        // Set the rootSearchPath as the owner for this
                        // config, even though technically it's at a
                        // higher position.
                        try {
                            final P4Config loadedConfig = loadCmdP4Config(config);
                            ret.put(rootSearchPath, loadedConfig);
                            break;
                        } catch (IOException e) {
                            LOG.error("Could not find or read config file " + configFile.getPath(), e);
                            // keep going up the tree
                        }
                    }
                }
                parent = parent.getParent();
            }
        }
        return ret;
    }


    /**
     * Similar to {@link #loadProjectP4ConfigsForPath(VirtualFile, String, boolean)},
     * but it keeps one file mapping per discovered config file.  That is, it
     * keeps the config file found as the key, rather than truncating it at the
     * root.
     *
     * @return mapping between directories and their corresponding configuration.
     */
    private static void loadCorrectRootDirP4ConfigsForPath(
            @NotNull VirtualFile rootSearchPath,
            @NotNull String configFileName,
            @NotNull Map<VirtualFile, P4Config> mapping) {
        if (!rootSearchPath.isDirectory() || !rootSearchPath.exists()) {
            throw new IllegalArgumentException(P4Bundle.message("error.roots.not-directory", rootSearchPath));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding root directory for " + rootSearchPath + ", named " + configFileName);
        }
        List<Iterator<VirtualFile>> depthStack = new ArrayList<Iterator<VirtualFile>>();
        depthStack.add(Arrays.asList(rootSearchPath.getChildren()).iterator());
        // bug #32 - make sure to add in the root directory, too.
        depthStack.add(Collections.singleton(rootSearchPath).iterator());
        while (!depthStack.isEmpty()) {
            Iterator<VirtualFile> iter = depthStack.remove(depthStack.size() - 1);
            if (iter.hasNext()) {
                VirtualFile vFile = iter.next();
                // put our current directory scanner back into our depth stack
                // for continued future searching.  Whether it has another
                // item or not will be checked when it is pulled the next time.
                depthStack.add(iter);
                if (vFile != null) {
                    // Make sure we use the actual I/O file in order to avoid some
                    // IDEA refresh issues.
                    File file = new File(vFile.getPath());
                    if (file.isDirectory()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("-- added scan for " + file);
                        }
                        depthStack.add(Arrays.asList(vFile.getChildren()).iterator());
                    } else if (mapping.containsKey(vFile.getParent())) {
                        // ignore, because this specific directory has a mapping
                        // already; don't perform the config file loading again.
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("-- already loaded config for " + vFile.getParent());
                        }
                    } else if (!file.exists()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("-- discovered non-existent file in IDEA cache: " + file);
                        }
                    } else if (file.getName().equals(configFileName)) {
                        ManualP4Config config = new ManualP4Config();
                        config.setConfigFile(file.getAbsolutePath());
                        try {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("-- loading config file " + file.getAbsolutePath() + " -> " + vFile
                                        .getParent());
                            }
                            final P4Config loadedConfig = loadCmdP4Config(config);
                            mapping.put(vFile.getParent(), loadedConfig);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("-/\\- " + loadedConfig.toString());
                            }
                        } catch (IOException e) {
                            LOG.warn("Could not find or read config file " + file.getPath(), e);
                        }
                    }
                }
            }
        }

        // No matter what, search up the tree to see if there's some parent config
        // file at a higher level.  This may be a bit inefficient, in so much as
        // multiple calls will do this, but it guarantees results, and looking up
        // a tree isn't too bad performance-wise.
        VirtualFile parent = rootSearchPath.getParent();
        while (parent != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("-- checking parent " + parent);
            }
            VirtualFile configFile = parent.findChild(configFileName);
            if (configFile != null && !mapping.containsKey(parent)) {
                File file = new File(configFile.getPath());
                if (file.exists() && file.isFile()) {
                    LOG.info("Found config file " + configFile.getPath() +
                            ", but registering it as root of " + rootSearchPath);
                    ManualP4Config config = new ManualP4Config();
                    config.setConfigFile(file.getAbsolutePath());

                    try {
                        final P4Config loadedConfig = loadCmdP4Config(config);
                        mapping.put(parent, loadedConfig);
                        // Found the first config file before the root, so stop.
                        break;
                    } catch (IOException e) {
                        LOG.error("Could not find or read config file " + configFile.getPath(), e);
                        // keep going up the tree
                    }
                }
            }
            parent = parent.getParent();
        }
    }


    /**
     * Loads a P4Config that simulates how the Perforce command-line tool
     * searches for configuration values.  This *only* works for
     * absolute config file names.
     *
     * Currently does not simulate the Mac OS X  ~/Library/Preferences
     * folder com.perforce.environment property list or the system-level
     * /Library/Preferences folder properties.
     *
     * @return cmd-based P4 configuration
     */
    @NotNull
    public static P4Config loadCmdP4Config(@Nullable P4Config overrideConfig) throws IOException {
        List<P4Config> hierarchy = new ArrayList<P4Config>();

        // override config is by default always first.
        // Config files area always after the config source that references them.
        // Config files will only be used once.  This keeps us out of trouble
        // in case something in the user's environment (environment variable or
        // registry) is setting this value, but we want to use the IDEA version
        // instead.

        boolean usedConfig = false;

        if (overrideConfig != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("adding override config: " + overrideConfig);
            }
            hierarchy.add(overrideConfig);
            usedConfig = addConfigFile(overrideConfig.getConfigFile(), hierarchy, true);
            if (LOG.isDebugEnabled()) {
                LOG.debug(" - " + hierarchy);
            }
        }

        if (WinRegP4Config.isAvailable()) {
            P4Config userWinConfig = new WinRegP4Config(true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("adding user winreg config: " + userWinConfig);
            }
            hierarchy.add(userWinConfig);
            if (! usedConfig) {
                usedConfig = addConfigFile(userWinConfig.getConfigFile(), hierarchy, false);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(" - " + hierarchy);
            }

            P4Config sysWinConfig = new WinRegP4Config(false);
            if (LOG.isDebugEnabled()) {
                LOG.debug("adding sys winreg config: " + sysWinConfig);
            }
            hierarchy.add(sysWinConfig);
            if (! usedConfig) {
                usedConfig = addConfigFile(sysWinConfig.getConfigFile(), hierarchy, false);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(" - " + hierarchy);
            }
        }

        P4Config envConf = new EnvP4Config();
        if (LOG.isDebugEnabled()) {
            LOG.debug("adding env config: " + envConf);
        }
        if (! usedConfig) {
            addConfigFile(envConf.getConfigFile(), hierarchy, false);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(" - " + hierarchy);
        }

        P4Config ret = new HierarchyP4Config(hierarchy.toArray(new P4Config[hierarchy.size()]));
        if (LOG.isDebugEnabled()) {
            LOG.debug(" +++ " + ret);
        }
        return ret;
    }

    private static boolean addConfigFile(@Nullable String source, @NotNull List<P4Config> hierarchy, boolean required) throws IOException {
        if (source != null) {
            File cf = new File(source);
            if (cf.exists() && cf.isFile() && cf.canRead()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Using config file " + cf.getAbsolutePath());
                }
                P4Config configFile = new FileP4Config(cf);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" - " + configFile);
                }
                hierarchy.add(configFile);
                return true;
            } else if (required) {
                // Because this is an error case, log it all out.
                LOG.info(cf.getAbsolutePath() + ": exists? " + cf.exists() + "; directory? " +
                    cf.isFile() + "; readable? " + cf.canRead());
                throw new FileNotFoundException(cf.getAbsolutePath());
            } else {
                LOG.info("Referenced config file [" + source + "], but it was not found");
            }
        }
        return false;
    }
}
