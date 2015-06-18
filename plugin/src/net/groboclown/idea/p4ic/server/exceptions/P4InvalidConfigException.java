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
package net.groboclown.idea.p4ic.server.exceptions;

import com.perforce.p4java.exception.P4JavaException;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigUtil;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ConfigurationProblem;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class P4InvalidConfigException extends P4DisconnectedException {
    public P4InvalidConfigException(String message) {
        super(message);
    }

    public P4InvalidConfigException(P4JavaException e) {
        super(e);
    }

    public P4InvalidConfigException(URISyntaxException e) {
        super(e);
    }

    public P4InvalidConfigException(@NotNull final ServerConfig config) {
        super(P4Bundle.message("error.config.setup", config));
    }

    public P4InvalidConfigException(@NotNull final P4Config config) {
        super(P4Bundle.message("error.config.setup", P4ConfigUtil.getProperties(config)));
    }

    public P4InvalidConfigException(@NotNull final ServerConfig config, @NotNull final List<ConfigurationProblem> problems) {
        super(P4Bundle.message("error.config.setup.problems", config, problems));
    }

    public P4InvalidConfigException(final IOException e) {
        super(e);
    }
}
