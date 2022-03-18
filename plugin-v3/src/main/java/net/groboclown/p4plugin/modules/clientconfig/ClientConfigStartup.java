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

package net.groboclown.p4plugin.modules.clientconfig;

import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import net.groboclown.p4plugin.modules.boilerplate.AbstractProjectBackgroundStartup;

/**
 * Startup handler for the module.
 */
public class ClientConfigStartup extends AbstractProjectBackgroundStartup {
    public ClientConfigStartup() {
        super(
                null,
                (p, c) -> {
                    final VcsRootChangeListener changeListener = new VcsRootChangeListener(p);
                    c.add(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, changeListener);
                    c.add(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED_IN_PLUGIN, changeListener);
                },
                null
        );
    }
}
