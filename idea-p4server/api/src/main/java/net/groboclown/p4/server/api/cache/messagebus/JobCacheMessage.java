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

package net.groboclown.p4.server.api.cache.messagebus;

import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.values.P4Job;
import org.jetbrains.annotations.NotNull;

public class JobCacheMessage extends AbstractCacheMessage<JobCacheMessage.Event> {
    private static final String DISPLAY_NAME = "p4ic4idea:open cache update";
    private static final Topic<TopicListener<Event>> TOPIC = createTopic(DISPLAY_NAME);

    public interface Listener {
        void jobUpdate(@NotNull Event event);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client, @NotNull String cacheId,
            @NotNull Listener listener) {
        abstractAddListener(client, TOPIC, cacheId, listener::jobUpdate);
    }

    public static void sendEvent(@NotNull Event e) {
        abstractSendEvent(TOPIC, e);
    }

    public enum JobUpdateAction {
        JOB_CREATED,
        JOB_DELETED,
        JOB_DETAILS_UPDATED
    }

    public static class Event extends AbstractCacheUpdateEvent<Event> {
        private final String jobId;
        private final P4Job job;
        private final JobUpdateAction action;


        public Event(@NotNull P4ServerName server,
                String jobId) {
            super(server);
            this.jobId = jobId;
            this.job = null;
            this.action = JobUpdateAction.JOB_DELETED;
        }

        public Event(@NotNull P4ServerName server,
                @NotNull P4Job job, @NotNull JobUpdateAction action) {
            super(server);
            this.jobId = job.getJobId();
            this.job = job;
            this.action = action;
        }
    }
}
