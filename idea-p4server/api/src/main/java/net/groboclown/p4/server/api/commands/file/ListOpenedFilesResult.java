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

package net.groboclown.p4.server.api.commands.file;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4LocalFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListOpenedFilesResult implements P4CommandRunner.ClientResult {
    private final ClientConfig config;
    private final List<P4LocalFile> openedFiles;

    public ListOpenedFilesResult(@NotNull ClientConfig config, @NotNull List<P4LocalFile> openedFiles) {
        this.config = config;
        this.openedFiles = openedFiles;
    }


    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }

    public List<P4LocalFile> getOpenedFiles() {
        return openedFiles;
    }
}