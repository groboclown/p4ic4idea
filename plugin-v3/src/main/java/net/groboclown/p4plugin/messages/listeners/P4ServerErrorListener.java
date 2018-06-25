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

package net.groboclown.p4plugin.messages.listeners;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.P4ServerErrorMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// FIXME implement handlers
public class P4ServerErrorListener implements P4ServerErrorMessage.Listener {
    @Override
    public void requestCausedError(@NotNull P4ServerName name, @Nullable ServerConfig config,
            @NotNull IServerMessage msg, @NotNull RequestException re) {

    }

    @Override
    public void requestCausedWarning(@NotNull P4ServerName name, @Nullable ServerConfig config,
            @NotNull IServerMessage msg, @NotNull RequestException re) {

    }

    @Override
    public void requestCausedInfoMsg(@NotNull P4ServerName name, @Nullable ServerConfig config,
            @NotNull IServerMessage msg, @NotNull RequestException re) {

    }

    @Override
    public void requestException(@NotNull P4ServerName name, @Nullable ServerConfig config,
            @NotNull RequestException re) {

    }

    @Override
    public void requestException(@NotNull P4ServerName name, @Nullable ServerConfig config,
            @NotNull P4JavaException e) {

    }
}
