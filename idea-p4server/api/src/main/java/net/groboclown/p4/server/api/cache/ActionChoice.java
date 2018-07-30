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

package net.groboclown.p4.server.api.cache;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.Displayable;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.PreviousExecutionProblems;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActionChoice implements Displayable, PreviousExecutionProblems {
    private final P4CommandRunner.ClientAction<?> client;
    private final P4CommandRunner.ServerAction<?> server;

    public ActionChoice(@NotNull P4CommandRunner.ClientAction<?> client) {
        this.client = client;
        this.server = null;
    }

    public ActionChoice(@NotNull P4CommandRunner.ServerAction<?> server) {
        this.client = null;
        this.server = server;
    }

    public ActionChoice(P4CommandRunner.ClientAction<?> client, P4CommandRunner.ServerAction<?> server) {
        if ((client == null && server == null) || (client != null && server != null)) {
            throw new IllegalArgumentException("exactly one argument must be null");
        }
        this.client = client;
        this.server = server;
    }

    @NotNull
    public String getActionId() {
        if (client != null) {
            return client.getActionId();
        }
        if (server != null) {
            return server.getActionId();
        }
        throw new IllegalStateException("unreachable code");
    }

    @SuppressWarnings("unchecked")
    //@Nullable
    public <SC extends P4CommandRunner.ClientResult, SS extends P4CommandRunner.ServerResult, R> R when(
            Function<P4CommandRunner.ClientAction<SC>, R> clientFunc,
            Function<P4CommandRunner.ServerAction<SS>, R> serverFunc) {
        if (this.client != null) {
            return clientFunc.apply((P4CommandRunner.ClientAction<SC>) this.client);
        }
        if (this.server != null) {
            return serverFunc.apply((P4CommandRunner.ServerAction<SS>) this.server);
        }
        throw new IllegalStateException("unreachable code");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <S extends P4CommandRunner.ClientResult, R> R ifClient(Function<P4CommandRunner.ClientAction<S>, R> f) {
        if (this.client != null) {
            return f.apply((P4CommandRunner.ClientAction<S>) this.client);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <S extends P4CommandRunner.ClientResult> ActionChoice whenClient(Consumer<P4CommandRunner.ClientAction<S>> f) {
        if (this.client != null) {
            f.accept((P4CommandRunner.ClientAction<S>) this.client);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <S extends P4CommandRunner.ServerResult, R> R ifServer(Function<P4CommandRunner.ServerAction<S>, R> f) {
        if (this.server != null) {
            return f.apply((P4CommandRunner.ServerAction<S>) this.server);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <S extends P4CommandRunner.ServerResult> ActionChoice whenServer(Consumer<P4CommandRunner.ServerAction<S>> f) {
        if (this.server != null) {
            f.accept((P4CommandRunner.ServerAction<S>) this.server);
        }
        return this;
    }

    @Override
    public String toString() {
        return client != null
                ? ("ClientAction(" + client.getCmd() + ")")
                : ("ServerAction(" + server.getCmd() + ")");
    }

    @Override
    public int hashCode() {
        return (client == null ? 0 : client.hashCode()) + (server == null ? 0 : server.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof ActionChoice) {
            ActionChoice that = (ActionChoice) o;
            return that.client == this.client && that.server == this.server;
        }
        return false;
    }

    @NotNull
    @Override
    public String[] getDisplayParameters() {
        return client != null
                ? client.getDisplayParameters()
                : (server != null
                    ? server.getDisplayParameters()
                    : EMPTY);
    }

    @NotNull
    @Override
    public List<FilePath> getAffectedFiles() {
        return client != null
                ? client.getAffectedFiles()
                : (server != null
                    ? server.getAffectedFiles()
                    : Collections.emptyList());
    }

    @Override
    @NotNull
    public List<P4CommandRunner.ResultError> getPreviousExecutionProblems() {
        return client != null
                ? client.getPreviousExecutionProblems()
                : (server != null
                        ? server.getPreviousExecutionProblems()
                        : Collections.emptyList());
    }

    @Override
    public void addExecutionProblem(@NotNull P4CommandRunner.ResultError error) {
        if (client != null) {
            client.addExecutionProblem(error);
        }
        if (server != null) {
            server.addExecutionProblem(error);
        }
    }
}
