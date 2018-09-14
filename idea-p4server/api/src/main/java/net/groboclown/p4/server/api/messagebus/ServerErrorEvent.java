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

package net.groboclown.p4.server.api.messagebus;

import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event classes to handle server errors.
 *
 * @param <E>
 */
public interface ServerErrorEvent<E extends Exception> {
    @NotNull
    P4ServerName getName();
    ServerConfig getConfig();
    E getError();

    class ServerNameErrorEvent<E extends Exception> extends AbstractMessageEvent implements ServerErrorEvent<E> {
        private final P4ServerName name;
        private final ServerConfig config;
        private final E error;

        public ServerNameErrorEvent(@NotNull P4ServerName name, @Nullable ServerConfig config, @NotNull E error) {
            this.name = name;
            this.config = config;
            this.error = error;
        }

        @NotNull
        public P4ServerName getName() {
            return name;
        }

        @Nullable
        public ServerConfig getConfig() {
            return config;
        }

        @NotNull
        public E getError() {
            return error;
        }
    }

    class ServerConfigErrorEvent<E extends Exception> extends AbstractMessageEvent implements ServerErrorEvent<E> {
        private final ServerConfig config;
        private final E error;

        public ServerConfigErrorEvent(@NotNull ServerConfig config, @NotNull E error) {
            this.config = config;
            this.error = error;
        }

        @NotNull
        public P4ServerName getName() {
            return config.getServerName();
        }

        @NotNull
        public ServerConfig getConfig() {
            return config;
        }

        @NotNull
        public E getError() {
            return error;
        }
    }

    class ServerConfigProblemEvent extends AbstractMessageEvent implements ServerErrorEvent<Exception> {
        private final ServerConfig config;

        public ServerConfigProblemEvent(@NotNull ServerConfig config) {
            this.config = config;
        }

        @NotNull
        public P4ServerName getName() {
            return config.getServerName();
        }

        @NotNull
        public ServerConfig getConfig() {
            return config;
        }

        @Nullable
        public Exception getError() {
            return null;
        }
    }

    class ServerNameProblemEvent extends AbstractMessageEvent implements ServerErrorEvent<Exception> {
        private final P4ServerName name;
        private final ServerConfig config;

        public ServerNameProblemEvent(@NotNull P4ServerName name, @Nullable ServerConfig config) {
            this.name = name;
            this.config = config;
        }

        @NotNull
        public P4ServerName getName() {
            return name;
        }

        @Nullable
        public ServerConfig getConfig() {
            return config;
        }

        @Nullable
        public Exception getError() {
            return null;
        }
    }

    class ServerMessageEvent extends ServerNameErrorEvent<RequestException> {
        private final IServerMessage msg;

        public ServerMessageEvent(@NotNull P4ServerName name,
                @Nullable ServerConfig config, @NotNull IServerMessage msg, @NotNull RequestException error) {
            super(name, config, error);
            this.msg = msg;
        }

        public IServerMessage getMsg() {
            return msg;
        }
    }
}
