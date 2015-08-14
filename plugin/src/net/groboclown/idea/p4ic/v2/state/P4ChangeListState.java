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

package net.groboclown.idea.p4ic.v2.state;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class P4ChangeListState implements CachedState {
    private int id;
    private Set<P4JobState> jobs;
    private String comment;
    private Set<P4FileState> added;
    private Set<P4FileState> deleted;
    private Set<P4FileState> edited;

    // key: the depot path to the origin (it doesn't have to be from within this client)
    // value: the destination within this client.
    private Map<String, P4FileState> integrated;

    private Map<P4FileState, P4FileState> moved;

    public boolean isDefault() {
        return id == 0;
    }

    @Override
    public Date getLastUpdated() {
        return null;
    }
}
