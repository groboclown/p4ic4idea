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

import net.groboclown.p4.simpleswarm.SwarmClient;
import net.groboclown.p4.simpleswarm.SwarmConfig;
import net.groboclown.p4.simpleswarm.exceptions.SwarmServerResponseException;
import net.groboclown.p4.simpleswarm.model.Review;

import java.io.IOException;

public class SimpleClient implements SwarmClient {
    private final ReviewActions review;

    public SimpleClient(SwarmConfig config) {
        review = new ReviewActions(config);
    }

    @Override
    public SwarmConfig getConfig() {
        return review.getConfig();
    }

    @Override
    public Review createReview(String description, int changelistId, String[] reviewers, String[] requiredReviewers)
            throws IOException, SwarmServerResponseException {
        return review.create(description, changelistId, reviewers, requiredReviewers);
    }

    @Override
    public Review addChangelistToReview(int reviewId, int changelistId)
            throws IOException, SwarmServerResponseException {
        return review.addChangeToReview(reviewId, changelistId);
    }

    @Override
    public int[] getReviewIdsForChangelist(int changelistId)
            throws IOException, SwarmServerResponseException {
        return review.getReviewIdsForChangelist(changelistId);
    }
}
