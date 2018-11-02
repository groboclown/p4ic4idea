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

package net.groboclown.p4plugin.ui.swarm;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.async.AnswerSink;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.messagebus.SwarmErrorMessage;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.impl.commands.AnswerUtil;
import net.groboclown.p4.simpleswarm.SwarmClient;
import net.groboclown.p4.simpleswarm.exceptions.SwarmServerResponseException;
import net.groboclown.p4.simpleswarm.model.Review;
import net.groboclown.p4plugin.components.SwarmConnectionComponent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class SwarmReview {
    private static final Logger LOG = Logger.getInstance(SwarmReview.class);


    /**
     *
     * @param project
     * @param registry
     * @param changelistId
     * @return the review ID, or < 0 if the review wasn't edited or created.
     */
    public static Answer<Integer> createOrEditSwarmReview(@NotNull Project project,
            @NotNull ProjectConfigRegistry registry, @NotNull P4ChangelistId changelistId) {
        if (changelistId.getState() != P4ChangelistId.State.NUMBERED) {
            SwarmErrorMessage.send(project).notNumberedChangelist(new SwarmErrorMessage.SwarmEvent(changelistId));
            return Answer.resolve(-1);
        }
        final ClientConfig clientConfig =
                registry.getRegisteredClientConfigState(changelistId.getClientServerRef());
        if (clientConfig == null) {
            LOG.info("Skipping changelist " + changelistId + " because it has no registered client");
            SwarmErrorMessage.send(project).notNumberedChangelist(new SwarmErrorMessage.SwarmEvent(changelistId));
            return Answer.resolve(-2);
        }
        return Answer.resolve(0)
        .futureMap((BiConsumer<Integer, AnswerSink<SwarmClient>>) (x, sink) ->
                SwarmConnectionComponent.getInstance(project).getSwarmClientFor(clientConfig)
                .whenCompleted(result -> sink.resolve(result.getSwarmClient()))
                .whenServerError(e -> {
                    LOG.info("Problem with swarm server", e);
                    sink.reject(e);
                }))
        .futureMap((BiConsumer<SwarmClient, AnswerSink<Integer>>) (swarmClient, sink) -> {
            try {
                int[] reviewIds = swarmClient.getReviewIdsForChangelist(changelistId.getChangelistId());
                if (reviewIds == null || reviewIds.length <= 0) {
                    createSwarmReview(project, clientConfig, changelistId)
                            .whenCompleted(sink::resolve)
                            .whenFailed(sink::reject);
                } else {
                    List<Review> reviews = new ArrayList<>(reviewIds.length);
                    for (int reviewId : reviewIds) {
                        Review review = swarmClient.getReview(reviewId);
                        if (review != null) {
                            reviews.add(review);
                        }
                    }
                    updateSwarmReview(project, clientConfig, changelistId, reviews)
                            .whenCompleted(sink::resolve)
                            .whenFailed(sink::reject);
                }
            } catch (IOException | SwarmServerResponseException e) {
                sink.reject(AnswerUtil.createSwarmError(e));
            }
        })
        .whenFailed(e -> {
            LOG.info("Problem with swarm server", e);
            SwarmErrorMessage.send(project).problemContactingServer(new SwarmErrorMessage.SwarmEvent(changelistId), e);
        });
        // Errors should be redirected to the event listener
    }

    @NotNull
    private static Answer<Integer> createSwarmReview(@NotNull final Project project,
            @NotNull final ClientConfig clientConfig, @NotNull final P4ChangelistId changelistId) {
        return Answer.background(sink -> {
            CreateSwarmReviewDialog.show(project, clientConfig, changelistId,
                    new CreateSwarmReviewDialog.OnCompleteListener() {
                        @Override
                        public void create(List<SwarmReviewPanel.Reviewer> reviewers, P4ChangelistId changelistId) {
                            // FIXME implement
                            LOG.warn("FIXME implement create swarm review for " + changelistId);
                            SwarmErrorMessage.send(project).problemContactingServer(new SwarmErrorMessage.SwarmEvent(changelistId),
                                    new Exception("Create swarm review: Not implemented yet"));
                            sink.resolve(-1);
                        }

                        @Override
                        public void cancel() {
                            sink.resolve(-1);
                        }
                    });
        });
    }

    @NotNull
    private static Answer<Integer> updateSwarmReview(Project project, ClientConfig clientConfig, P4ChangelistId changelistId,
            List<Review> reviews) {
        // TODO map reviews to fetched changelists.

        // FIXME implement
        LOG.warn("implement update swarm review dialog: " + changelistId + " -> " + reviews);
        SwarmErrorMessage.send(project).problemContactingServer(new SwarmErrorMessage.SwarmEvent(changelistId),
                new Exception("Not implemented yet"));
        return Answer.resolve(-1);
    }
}
