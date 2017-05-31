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

package net.groboclown.p4.simpleswarm;

import net.groboclown.p4.simpleswarm.exceptions.SwarmServerResponseException;
import net.groboclown.p4.simpleswarm.model.Review;

import java.io.IOException;

public interface SwarmClient {
    Review createReview(String description, int changelistId,
            String[] reviewers, String[] requiredReviewers)
            throws IOException, SwarmServerResponseException;

    Review addChangelistToReview(int reviewId, int changelistId)
            throws IOException, SwarmServerResponseException;

    int[] getReviewIdsForChangelist(int changelistId)
            throws IOException, SwarmServerResponseException;
}
