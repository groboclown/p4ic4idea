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

import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.msg.ServerMessage;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.ISingleServerMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PasswordStoreException extends AccessException {
    public PasswordStoreException(@NotNull final PasswordSafeException t) {
        super(getServiceMessage(t));
        initCause(t);
    }

    @NotNull
    private static IServerMessage getServiceMessage(final PasswordSafeException t) {
        Map<String, Object> format = new HashMap<String, Object>();
        format.put(RpcMessage.FMT + '0', t.getMessage());
        return new ServerMessage(Collections.<ISingleServerMessage>singletonList(
                new ServerMessage.SingleServerMessage("", 0, format)
            ), format);
    }
}
