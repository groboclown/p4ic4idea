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

package net.groboclown.p4.server.api.exceptions;

import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates that a timer set on how long to allow for a request was surpassed.
 */
public class ConnectionTimeoutException  extends VcsException {
    private final long timeoutSeconds;

    public ConnectionTimeoutException(@NotNull String operation, long timeoutSeconds) {
        super("Timed out connecting to server after " + timeoutSeconds + " seconds");
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     *
     * @return how long the timeout was set for.
     */
    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
