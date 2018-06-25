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

import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import org.jetbrains.annotations.NotNull;

// FIXME implement handlers
public class LoginFailureListener implements LoginFailureMessage.Listener {
    @Override
    public void singleSignOnFailed(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {

    }

    @Override
    public void singleSignOnExecutionFailed(@NotNull ServerConfig config,
            @NotNull LoginFailureMessage.SingleSignOnExecutionFailureEvent e) {

    }

    @Override
    public void sessionExpired(@NotNull ServerConfig clientConfig, @NotNull AuthenticationFailedException e) {

    }

    @Override
    public void passwordInvalid(@NotNull ServerConfig serverConfig, @NotNull AuthenticationFailedException e) {

    }

    @Override
    public void passwordUnnecessary(@NotNull ServerConfig serverConfig, @NotNull AuthenticationFailedException e) {

    }
}
