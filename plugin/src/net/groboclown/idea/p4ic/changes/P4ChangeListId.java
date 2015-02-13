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
package net.groboclown.idea.p4ic.changes;

import net.groboclown.idea.p4ic.config.Client;
import org.jetbrains.annotations.NotNull;

/**
 * Perforce changelists must be identified by their ID and their origin
 * server.
 */
public interface P4ChangeListId {
    public int getChangeListId();

    @NotNull
    public String getServerConfigId();

    @NotNull
    public String getClientName();

    public boolean isNumberedChangelist();

    /**
     * All changelists will either return true for this call or {@link #isNumberedChangelist()},
     * but not both.  Unknown changelists are represented as "null" objects.
     *
     * @return true if this is the default changelist.
     */
    public boolean isDefaultChangelist();

    /**
     * Is this changelist in the given client?  Matches on the client name
     * and the server config id.
     *
     * @param client client to check
     * @return true if the given client matches the server config and client name.
     */
    public boolean isIn(@NotNull Client client);
}
