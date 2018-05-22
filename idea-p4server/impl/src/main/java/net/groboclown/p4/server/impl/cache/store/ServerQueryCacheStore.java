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

package net.groboclown.p4.server.impl.cache.store;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.values.P4JobSpec;
import org.jetbrains.annotations.NotNull;

/**
 * All the cached server-side information for query results.
 */
public class ServerQueryCacheStore {
    private static final Logger LOG = Logger.getInstance(ServerQueryCacheStore.class);

    private final P4ServerName serverName;
    private P4JobSpec jobSpec;

    public static class State {
        // FIXME implement
    }


    public ServerQueryCacheStore(@NotNull P4ServerName config) {
        this.serverName = config;
    }


    public P4JobSpec getJobSpec() {
        return jobSpec;
    }

    public void setJobSpec(P4JobSpec jobSpec) {
        this.jobSpec = jobSpec;
    }

    public P4ServerName getServerName() {
        return serverName;
    }

    public State getState() {
        // FIXME implement
        LOG.warn("FIXME implement state construction");
        return null;
    }

    public void setState(State state) {
        // FIXME implement
        LOG.warn("FIXME implement state reading");
    }
}
