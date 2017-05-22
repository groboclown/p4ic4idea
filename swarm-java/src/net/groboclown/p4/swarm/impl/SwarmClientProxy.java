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

package net.groboclown.p4.swarm.impl;

import net.groboclown.p4.swarm.SwarmClient;
import net.groboclown.p4.swarm.SwarmConfig;
import net.groboclown.p4.swarm.model.request.ActivityRequest;
import net.groboclown.p4.swarm.model.response.ActivityPage;

public class SwarmClientProxy implements SwarmClient {
    private final SwarmConfig config;

    public SwarmClientProxy(SwarmConfig config) {
        this.config = config;
    }

    @Override
    public ActivityPage getActivityList(ActivityRequest request) {
        return null;
    }
}
