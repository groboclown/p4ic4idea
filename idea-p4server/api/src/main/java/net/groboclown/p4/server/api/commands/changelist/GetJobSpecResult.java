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

package net.groboclown.p4.server.api.commands.changelist;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4JobSpec;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;

@Immutable
public class GetJobSpecResult implements P4CommandRunner.ServerResult {
    private final OptionalClientServerConfig config;
    private final P4JobSpec spec;

    public GetJobSpecResult(@NotNull OptionalClientServerConfig config, P4JobSpec jobSpec) {
        this.config = config;
        this.spec = jobSpec;
    }

    @NotNull
    @Override
    public ServerConfig getServerConfig() {
        return config.getServerConfig();
    }

    public P4JobSpec getJobSpec() {
        return spec;
    }
}
