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

package net.groboclown.p4plugin.ui.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SyncOptionConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(SyncOptionConfigurable.class);

    private final Project project;
    private final SyncOptions baseOptions;
    private final SyncOptions pendingOptions;
    private final List<ClientConfig> configs;

    // TODO allow for changelist browsing.
    // For changelist browsing, we can limit the number of changes returned, and have a paging
    // mechanism - "p4 changes -m 10 ...@<(last changelist number)"
    // This, however, requires access to all the source ServerConfig instances.

    public SyncOptionConfigurable(@NotNull Project project, @NotNull SyncOptions baseOptions,
            @NotNull Collection<ClientConfig> configs) {
        this.project = project;
        this.baseOptions = baseOptions;
        this.pendingOptions = new SyncOptions(baseOptions);
        this.configs = new ArrayList<>(configs);
    }



    @Nls
    @Override
    public String getDisplayName() {
        return P4Bundle.getString("sync.options.title");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        reset();
        return new SyncPanel(project, pendingOptions, configs).getPanel();
    }

    @Override
    public boolean isModified() {
        return pendingOptions.equals(baseOptions);
    }

    @Override
    public void apply() throws ConfigurationException {
        baseOptions.copyFrom(pendingOptions);
        if (LOG.isDebugEnabled()) {
            LOG.debug("SyncOptions set to " + pendingOptions);
        }
    }

    @Override
    public void reset() {
        pendingOptions.copyFrom(baseOptions);
    }

    @Override
    public void disposeUIResources() {
        // should dispose of the panel
        // however, we don't keep references to it, so it is
        // automatically cleaned up.
        // TODO is this assumption correct?
    }
}
