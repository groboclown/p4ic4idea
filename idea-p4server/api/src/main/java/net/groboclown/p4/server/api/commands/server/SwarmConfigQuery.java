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

package net.groboclown.p4.server.api.commands.server;

import com.intellij.credentialStore.OneTimeString;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.simpleswarm.SwarmLogger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

import static net.groboclown.p4.server.api.P4CommandRunner.ServerQueryCmd.GET_SWARM_CONFIG;

public class SwarmConfigQuery implements P4CommandRunner.ServerQuery<SwarmConfigResult> {

    public static class AuthorizationOption {
        private final Supplier<Answer<OneTimeString>> passwordGenerator;
        private final Runnable authenticationFailed;
        private final Supplier<Answer<String>> ticketGenerator;

        public AuthorizationOption(
                @NotNull Supplier<Answer<OneTimeString>> passwordGenerator,
                @NotNull Runnable authenticationFailed) {
            this.passwordGenerator = passwordGenerator;
            this.authenticationFailed = authenticationFailed;
            this.ticketGenerator = null;
        }

        public AuthorizationOption(@NotNull Supplier<Answer<String>> ticketGenerator) {
            this.passwordGenerator = null;
            this.authenticationFailed = null;
            this.ticketGenerator = ticketGenerator;
        }

        public <R> Answer<R> on(@NotNull Function<OneTimeString, Answer<R>> password,
                @NotNull Function<String, Answer<R>> ticket) {
            if (passwordGenerator != null) {
                return passwordGenerator.get().mapAsync(password);
            } else if (ticketGenerator != null) {
                return ticketGenerator.get().mapAsync(ticket);
            } else {
                throw new IllegalStateException("invalid function state");
            }
        }

        public void onAuthenticationFailure() {
            if (authenticationFailed != null) {
                authenticationFailed.run();
            }
        }
    }

    private final Function<ServerConfig, Answer<AuthorizationOption>> authorization;
    private final SwarmLogger logger;

    public SwarmConfigQuery(@NotNull Function<ServerConfig, Answer<AuthorizationOption>> authorization,
            @NotNull SwarmLogger logger) {
        this.authorization = authorization;
        this.logger = logger;
    }


    @NotNull
    @Override
    public Class<? extends SwarmConfigResult> getResultType() {
        return SwarmConfigResult.class;
    }

    @Override
    public P4CommandRunner.ServerQueryCmd getCmd() {
        return GET_SWARM_CONFIG;
    }

    public Answer<AuthorizationOption> getAuthorization(ServerConfig config) {
        return authorization.apply(config);
    }

    public SwarmLogger getLogger() {
        return logger;
    }
}
