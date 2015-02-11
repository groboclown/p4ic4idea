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
package net.groboclown.idea.p4ic.server;

import com.intellij.util.messages.Topic;
import net.groboclown.idea.p4ic.background.VcsFutureSetter;
import net.groboclown.idea.p4ic.config.ServerConfig;


/**
 * Called when the server needs to
 */
public interface OnServerDisconnectListener {
    public static final Topic<OnServerDisconnectListener> TOPIC =
            new Topic<OnServerDisconnectListener>("p4ic.remote.ui.disconnect", OnServerDisconnectListener.class);


    static enum OnDisconnectAction {
        WORK_OFFLINE,
        RETRY
    }


    /**
     * Called when the system detects a disconnection from the server, and
     * a retry failed.  All listeners should first run:
     * <pre>
     *     if (future.isDone()) {
     *         return;
     *     }
     * </pre>
     * This way the user isn't asked the same question multiple times.
     *
     * @param config server that was disconnected
     * @param future action to perform.
     */
    void onDisconnect(ServerConfig config, VcsFutureSetter<OnDisconnectAction> future);

}
