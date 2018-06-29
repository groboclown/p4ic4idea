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

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface CachePendingActionHandler {
    <R> R readActions(@NotNull ClientConfig clientConfig, @NotNull Function<Stream<ActionChoice>, R> f)
            throws InterruptedException;
    void readActionItems(@NotNull ClientConfig clientConfig, @NotNull Consumer<ActionChoice> f)
            throws InterruptedException;
    Stream<ActionChoice> copyActions(ClientConfig clientConfig)
            throws InterruptedException;
    void writeActions(ClientServerRef config, Consumer<WriteActionCache> fun)
            throws InterruptedException;
    void writeActions(P4ServerName config, Consumer<WriteActionCache> fun)
            throws InterruptedException;



    interface WriteActionCache
            extends Iterable<ActionChoice> {
        Stream<ActionChoice> getActions();
        Optional<P4CommandRunner.ClientAction<?>> getClientActionById(@NotNull String actionId);
        boolean removeActionById(@NotNull String actionId);
        void addAction(@NotNull P4CommandRunner.ClientAction<?> action);
        void addAction(@NotNull P4CommandRunner.ServerAction<?> action);
    }
}
