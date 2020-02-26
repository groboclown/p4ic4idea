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
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4FileAnnotation;
import net.groboclown.p4.server.api.values.P4FileRevision;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class AnnotateFileResult implements P4CommandRunner.ClientResult {
    private final ClientConfig config;
    private final P4FileAnnotation annotatedFile;
    private final P4FileRevision headRevision;
    private final String content;

    public AnnotateFileResult(@NotNull ClientConfig config,
            @NotNull P4FileAnnotation annotatedFile,
            @NotNull P4FileRevision headRevision,
            @Nullable String content) {
        this.config = config;
        this.annotatedFile = annotatedFile;
        this.headRevision = headRevision;
        this.content = content;
    }

    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }

    @NotNull
    public P4FileAnnotation getAnnotatedFile() {
        return annotatedFile;
    }

    @NotNull
    public P4FileRevision getHeadRevision() {
        return headRevision;
    }

    @Nullable
    public String getContent() {
        return content;
    }
}
