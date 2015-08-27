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

package net.groboclown.idea.p4ic.v2.server;

import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;

/**
 * The front-end view of an action on a file object.  This is a copy of the local
 * cached version.
 */
public class P4FileAction {
    private final P4FileUpdateState local;
    private final UpdateAction action;


    public P4FileAction(final P4FileUpdateState local, final UpdateAction action) {
        this.local = local;
        this.action = action;
    }
}
