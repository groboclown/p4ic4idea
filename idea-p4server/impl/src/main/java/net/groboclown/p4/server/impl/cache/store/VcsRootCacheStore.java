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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
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
 * Central storage for all VCS roots.
 */
public class VcsRootCacheStore {
    private VirtualFile rootDirectory;
    private List<ConfigPart> configParts;

    public static class State {
        public String rootDirectory;
        public List<ConfigPartState> configParts;
    }


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
        this.rootDirectory = VcsUtil.getVirtualFile(root.rootDirectory);
        this.configParts = new ArrayList<>(root.configParts.size());
        for (ConfigPartState configPart : root.configParts) {
            this.configParts.add(convert(configPart, rootDirectory, classLoader));
        }
    }


    public void setConfigParts(@NotNull List<ConfigPart> configParts) {
        this.configParts = new ArrayList<>(configParts);
    }

    @NotNull
    public List<ConfigPart> getConfigParts() {
        return configParts;
    }

    @NotNull
    public State getState() {
        State ret = new State();
        ret.rootDirectory = rootDirectory.getPath();
        ret.configParts = new ArrayList<>(configParts.size());
        for (ConfigPart configPart : configParts) {
            ret.configParts.add(convert(configPart));
        }
        return ret;
    }


    private static ConfigPartState convert(ConfigPart part) {
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


    private static ConfigPart convert(ConfigPartState state, VirtualFile root, ClassLoader classLoader) {
        if (state.children != null) {
            List<ConfigPart> children = new ArrayList<>(state.children.size());
            for (ConfigPartState child : state.children) {
                children.add(convert(child, root, classLoader));
            }
            return new MultipleConfigPart(state.sourceName, children);
        }
        Class<?> ret = null;
        try {
            ret = ClassLoaderUtil.loadClass(state.className, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
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
