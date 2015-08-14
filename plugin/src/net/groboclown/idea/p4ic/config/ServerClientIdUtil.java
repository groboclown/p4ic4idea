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

package net.groboclown.idea.p4ic.config;

import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import org.jetbrains.annotations.NotNull;

public class ServerClientIdUtil {
    public static String getServerClientId(@NotNull P4ChangeListId id) {
        return id.getServerConfigId() + ((char) 1) + id.getClientName();
    }


    public static String getServerClientId(@NotNull Client client) {
        return client.getConfig().getServiceName() + ((char) 1) + client.getClientName();
    }
}
