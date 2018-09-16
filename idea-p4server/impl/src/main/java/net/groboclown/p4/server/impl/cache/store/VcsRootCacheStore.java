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

package net.groboclown.p4.server.impl.cache.store;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4.server.impl.config.part.ConfigStateProvider;
import net.groboclown.p4.server.impl.config.part.PartStateLoader;
import net.groboclown.p4.server.impl.util.ClassLoaderUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Central storage for all VCS roots.  Persistence is handled by
 * {@link net.groboclown.p4.server.api.config.PersistentRootConfigComponent}.
 */
public class VcsRootCacheStore {
    private static final Logger LOG = Logger.getInstance(VcsRootCacheStore.class);


    private VirtualFile rootDirectory;
    private List<ConfigPart> configParts;

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public String rootDirectory;
        public List<ConfigPartState> configParts;
    }


    @SuppressWarnings("WeakerAccess")
    public static class ConfigPartState {
        public String className;
        public String sourceName;
        public Map<String, String> values;
        public List<ConfigPartState> children;
    }


    public VcsRootCacheStore(@NotNull VirtualFile rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.configParts = new ArrayList<>();
    }


    public VcsRootCacheStore(@NotNull State root, @Nullable ClassLoader classLoader) {
        // It's possible for a mis-configuration to setup a null root directory.
        this.rootDirectory = root.rootDirectory == null ? null : VcsUtil.getVirtualFile(root.rootDirectory);
        if (root.configParts == null) {
            this.configParts = new ArrayList<>();
        } else {
            this.configParts = new ArrayList<>(root.configParts.size());
            for (ConfigPartState configPart : root.configParts) {
                this.configParts.add(convert(configPart, rootDirectory, classLoader));
            }
        }
    }


    public void setConfigParts(@NotNull List<ConfigPart> configParts) {
        this.configParts = new ArrayList<>(configParts.size());
        for (ConfigPart configPart : configParts) {
            if (configPart == null) {
                LOG.warn("Tried setting null config part: " + configParts);
            } else {
                this.configParts.add(configPart);
            }
        }
    }

    @NotNull
    public List<ConfigPart> getConfigParts() {
        return configParts;
    }

    @Nullable
    public VirtualFile getRootDirectory() {
        return rootDirectory;
    }

    @NotNull
    public State getState() {
        State ret = new State();
        ret.rootDirectory = rootDirectory.getPath();
        ret.configParts = new ArrayList<>(configParts.size());
        for (ConfigPart configPart : configParts) {
            if (configPart != null) {
                ret.configParts.add(convert(configPart));
            } else {
                LOG.warn("Encountered null config part in state");
            }
        }
        return ret;
    }


    private static ConfigPartState convert(@NotNull ConfigPart part) {
        ConfigPartState cps = new ConfigPartState();
        cps.sourceName = part.getSourceName();
        if (part instanceof MultipleConfigPart) {
            MultipleConfigPart mcp = (MultipleConfigPart) part;
            List<ConfigPart> children = mcp.getChildren();
            cps.children = new ArrayList<>(children.size());
            for (ConfigPart child : children) {
                cps.children.add(convert(child));
            }
        } else if (part instanceof ConfigStateProvider) {
            cps.className = part.getClass().getName();
            cps.values = ((ConfigStateProvider) part).getState();
        } else {
            throw new IllegalStateException("contains non-serializable ConfigPart " + part + " (" +
                    part.getClass() + ")");
        }
        return cps;
    }


    @Nullable
    private static ConfigPart convert(ConfigPartState state, VirtualFile root, ClassLoader classLoader) {
        if (state.children != null) {
            List<ConfigPart> children = new ArrayList<>(state.children.size());
            for (ConfigPartState child : state.children) {
                children.add(convert(child, root, classLoader));
            }
            return new MultipleConfigPart(state.sourceName, children);
        }
        Class<?> ret;
        try {
            ret = ClassLoaderUtil.loadClass(state.className, classLoader);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            LOG.warn("Configuration state references unknown class " + state.className, e);
            return null;
        }
        if (state.values == null) {
            state.values = Collections.emptyMap();
        }
        if (state.sourceName == null) {
            state.sourceName = "<unknown>";
        }
        return PartStateLoader.load(ret, root, state.sourceName, state.values);
    }
}
