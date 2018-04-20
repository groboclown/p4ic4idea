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

package net.groboclown.p4.server.exceptions;

import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;
import org.jetbrains.annotations.NotNull;

public class P4RequestException extends P4Exception {
    private final IServerMessage msg;

    public P4RequestException(@NotNull IServerMessage msg) {
        // TODO needs to be handled better.
        super(msg.toString());
        this.msg = msg;
    }

    public P4RequestException(@NotNull RequestException ex) {
        super(ex);
        // TODO if null, build from the RequestException problem information.
        this.msg = ex.getServerMessage();
    }

    public IServerMessage getServerMessage() {
        return msg;
    }
}
