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

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface P4ClientsReloadedListener {
    public static final Topic<P4ClientsReloadedListener> TOPIC =
            new Topic<P4ClientsReloadedListener>("p4ic.clients.loaded", P4ClientsReloadedListener.class);


    public void clientsLoaded(@NotNull Project project, @NotNull List<Client> clients);
}
