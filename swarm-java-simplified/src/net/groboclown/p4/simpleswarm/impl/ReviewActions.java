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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.groboclown.p4.simpleswarm.SwarmConfig;
import net.groboclown.p4.simpleswarm.exceptions.SwarmServerResponseException;
import net.groboclown.p4.simpleswarm.model.Review;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewActions {
    private final SwarmConfig config;

    public ReviewActions(SwarmConfig config) {
        this.config = config;
    }

    public SwarmConfig getConfig() {
        return config;
    }

    public Review create(String description, int changelistId,
            String[] reviewers, String[] requiredReviewers)
            throws IOException, SwarmServerResponseException {
        Map<String, Object> form = new HashMap<String, Object>();
        if (description != null) {
            form.put("description", description);
        }
        form.put("change", changelistId);
        if (reviewers != null && reviewers.length > 0) {
            form.put("reviewers", Arrays.asList(reviewers));
        }
        if (requiredReviewers != null && requiredReviewers.length > 0) {
            form.put("requiredReviewers", Arrays.asList(requiredReviewers));
        }
        BasicResponse resp = BasicRequest.postForm(config, "reviews/", form);
        if (resp.getStatusCode() != 200) {
            throw resp.getResponseException("review", "create");
        }
        return new Review(resp.getBodyAsJson());
    }

    public Review addChangeToReview(int reviewId, int changelistId)
            throws IOException, SwarmServerResponseException {
        Map<String, Object> form = new HashMap<String, Object>();
        form.put("change", changelistId);
        BasicResponse resp = BasicRequest.postForm(config, "reviews/" + reviewId + "/changes", form);
        if (resp.getStatusCode() != 200) {
            throw resp.getResponseException("review", "create");
        }
        return new Review(resp.getBodyAsJson());
    }

    public int[] getReviewIdsForChangelist(int changelistId)
            throws IOException, SwarmServerResponseException {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("change[]", changelistId);
        query.put("fields", "id");

        // Simple paging
        List<Integer> ids = new ArrayList<Integer>();

        while (true) {
            BasicResponse resp = BasicRequest.get(config, "review", query);
            if (resp.getStatusCode() != 200) {
                throw resp.getResponseException("review", "get");
            }
            JsonObject body = resp.getBodyAsJson();

            JsonArray reviews = JsonUtil.getNullableArrayKey(body, "reviews");
            if (reviews != null) {
                for (int i = 0; i < reviews.size(); i++) {
                    JsonElement review = reviews.get(i);
                    if (review != null && review.isJsonObject()) {
                        int val = JsonUtil.getIntKey(review.getAsJsonObject(), "id");
                        ids.add(val);
                    }
                }
            }

            String lastSeen = JsonUtil.getNullableStringKey(body, "lastSeen");
            if (lastSeen == null) {
                break;
            }
            query.put("after", lastSeen);
        }

        int[] ret = new int[ids.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ids.get(i);
        }
        return ret;
    }
}
