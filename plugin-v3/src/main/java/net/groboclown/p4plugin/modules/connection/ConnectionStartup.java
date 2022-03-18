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

package net.groboclown.p4plugin.modules.connection;

import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4plugin.modules.boilerplate.AbstractProjectBackgroundStartup;

/**
 * Sets up the {@link net.groboclown.p4.server.api.ProjectConfigRegistry} after the IDE starts up.
 */
public class ConnectionStartup extends AbstractProjectBackgroundStartup {
    public ConnectionStartup() {
        super(
                // TODO this should be split out into the component parts to make it easier to maintain.
                (p) -> p.getService(ProjectConfigRegistry.class).initializeService(),
                null,
                null
        );
    }
}
