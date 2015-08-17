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

package net.groboclown.idea.p4ic.v2.server.cache;

import java.util.List;

public interface CacheUpdatesReconciler<C extends CachedServerState<C>> {
    // TODO needs to take an extra argument which allows executing an update, and marking errors or other
    // user input requests
    void reconcileServerUpdate(C original, C current, List<PendingUpdate> updates);
}
