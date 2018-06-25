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

package net.groboclown.p4.server.api.values;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Describes a user client workspace.  Called a "workspace" as a disambiguation from a server/client concept.
 */
public interface P4WorkspaceSummary {
    enum ClientOption {
        ALLWRITE,
        CLOBBER,
        COMPRESS,
        LOCKED,
        MODTIME,
        RMDIR
    }
    enum SubmitOption {
        SUBMIT_UNCHANGED,
        SUBMIT_UNCHANGED_REOPEN,
        REVERT_UNCHANGED,
        REVERT_UNCHANGED_REOPEN,
        LEAVE_UNCHANGED,
        LEAVE_UNCHANGED_REOPEN
    }
    enum LineEnding {
        WIN,
        LOCAL,
        MAC,
        UNIX,
        SHARE
    }
    enum ClientType {
        WRITABLE,
        READONLY
    }


    String getClientName();
    Date getLastUpdate();
    Date getLastAccess();
    String getOwner();
    String getDescription();
    Map<ClientOption, Boolean> getClientOptions();

    // The client specifies a single string, and this returns the corresponding
    // enum representation of that string.
    SubmitOption getSubmitOption();

    LineEnding getLineEnding();

    ClientType getClientType();

    /**
     *
     * @return ordered list of roots, starting with Root, then AltRoot values in order.
     */
    List<String> getRoots();

    String getHost();

    /**
     *
     * @return named server restriction
     */
    String getServerId();

    String getStream();
    int getStreamAtChange();
}
