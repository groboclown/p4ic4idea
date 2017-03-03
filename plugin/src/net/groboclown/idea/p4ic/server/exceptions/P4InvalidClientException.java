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
package net.groboclown.idea.p4ic.server.exceptions;

import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import org.jetbrains.annotations.NotNull;

public class P4InvalidClientException extends P4DisconnectedException {
    public P4InvalidClientException(@NotNull String clientName) {
        super(P4Bundle.message("exception.invalid.client", clientName));
    }

    public P4InvalidClientException() {
        super(P4Bundle.message("error.config.no-client"));
    }

    public P4InvalidClientException(@NotNull final ClientServerRef clientServerRef) {
        super(P4Bundle.message("exception.invalid.client-server", clientServerRef.getServerDisplayId(),
                clientServerRef.getClientName()));
    }
}
