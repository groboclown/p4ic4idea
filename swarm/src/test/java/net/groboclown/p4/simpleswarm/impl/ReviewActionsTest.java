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
package net.groboclown.p4.simpleswarm.impl;

import net.groboclown.p4.simpleswarm.SwarmConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewActionsTest {

    @Test
    void getConfig() {
        SwarmConfig config = new SwarmConfig();
        ReviewActions ra = new ReviewActions(config);
        assertSame(config, ra.getConfig());
    }

    @Test
    void create() {
        fail("Not implemented");
    }

    @Test
    void addChangeToReview() {
    }

    @Test
    void getReview() {
    }

    @Test
    void getReviewIdsForChangelist() {
    }
}