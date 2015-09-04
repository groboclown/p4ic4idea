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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import org.jetbrains.annotations.NotNull;

/**
 * Package-level marker for the cache front ends.  This just enforces
 * that all the front ends conform to the same API.
 */
abstract class CacheFrontEnd {

    // FIXME wrap in a timer to only load at a given maximum frequency

    /**
     * Reload the underlying server cache.  Only called when the client is online.
     *
     * @param exec connected server API
     * @param alerts user message handler
     */
    abstract void loadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts);
}
