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

import com.intellij.openapi.vcs.VcsBundle;
import com.perforce.p4java.core.IChangelist;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import org.jetbrains.annotations.NotNull;

/**
 * Perforce changelists must be identified by their ID and their origin
 * server.
 */
public interface P4ChangeListId {

    int P4_DEFAULT = IChangelist.DEFAULT;
    int P4_UNKNOWN = IChangelist.UNKNOWN;

    /**
     * highest number for a local changelist
     */
    int P4_LOCAL = -1000;

    /** Default changelist name (in UI) */
    String DEFAULT_CHANGE_NAME = VcsBundle.message("changes.default.changelist.name");


    int getChangeListId();

    @NotNull
    String getServerConfigId();

    @NotNull
    String getClientName();

    @NotNull
    ClientServerId getClientServerId();

    boolean isNumberedChangelist();

    /**
     * All changelists will either return true for this call or {@link #isNumberedChangelist()},
     * but not both.  Unknown changelists are represented as "null" objects.
     *
     * @return true if this is the default changelist.
     */
    boolean isDefaultChangelist();

    /**
     *
     * @return true if the changelist has not been created on the server.
     */
    boolean isUnsynchedChangelist();

    /**
     * Is this changelist in the given client?  Matches on the client name
     * and the server config id.
     *
     * @param client client to check
     * @return true if the given client matches the server config and client name.
     */
    boolean isIn(@NotNull P4Server client);
}
