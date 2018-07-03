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

package net.groboclown.p4.server.impl.repository;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import org.jetbrains.annotations.NotNull;

public class RepositoryLocationFactory {
    @NotNull
    public static RepositoryLocation getLocationFor(@NotNull FilePath root, @NotNull ClientConfigRoot clientRoot,
            @NotNull ListFilesDetailsResult details) {
        if (details.getFiles().isEmpty()) {
            String clientName = clientRoot.getClientConfig().getClientname();
            if (clientName == null) {
                // TODO bundle string
                clientName = "<unknown>";
            }
            return new LocalRepositoryLocation(clientRoot.getClientConfig().getClientServerRef(), clientName, root);
        }
        return new P4RepositoryLocationImpl(clientRoot.getClientConfig().getClientServerRef(), details.getFiles().get(0));
    }
}
