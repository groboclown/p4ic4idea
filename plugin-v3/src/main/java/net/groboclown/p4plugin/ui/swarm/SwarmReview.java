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

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ChangeList;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.RootedClientConfig;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.async.AnswerSink;
import net.groboclown.p4.server.api.commands.file.ShelveFilesAction;
import net.groboclown.p4.server.api.commands.file.ShelveFilesResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.messagebus.SwarmErrorMessage;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.impl.commands.AnswerUtil;
import net.groboclown.p4.server.impl.commands.QueryAnswerImpl;
import net.groboclown.p4.simpleswarm.SwarmClient;
import net.groboclown.p4.simpleswarm.exceptions.SwarmServerResponseException;
import net.groboclown.p4.simpleswarm.model.Review;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.SwarmConnectionComponent;
import net.groboclown.p4plugin.messages.UserMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class SwarmReview {
    private static final Logger LOG = Logger.getInstance(SwarmReview.class);


    /**
     *
     * @param project project
     * @param registry config registry
     * @param changelistId changelist to turn into a review
     * @return the review ID, or < 0 if the review wasn't edited or created.
     */
    public static Answer<Integer> createOrEditSwarmReview(@NotNull Project project,
            @NotNull ProjectConfigRegistry registry,
            @NotNull ChangeList ideChangeList,
            @NotNull P4ChangelistId changelistId) {
        if (changelistId.getState() != P4ChangelistId.State.NUMBERED) {
            SwarmErrorMessage.send(project).notNumberedChangelist(new SwarmErrorMessage.SwarmEvent(changelistId));
            return Answer.resolve(-1);
        }
        final ClientConfig clientConfig = getClientConfigFor(registry, changelistId);
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
                    createSwarmReview(project, clientConfig, swarmClient, ideChangeList, changelistId)
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
                    updateSwarmReview(project, clientConfig, changelistId, swarmClient, reviews)
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
            @NotNull final ClientConfig clientConfig,
            @NotNull final SwarmClient swarmClient, @NotNull final ChangeList ideChangelist,
            @NotNull final P4ChangelistId changelistId) {
        return Answer.background(sink ->
            CreateSwarmReviewDialog.show(project, clientConfig, ideChangelist,
                    new CreateSwarmReviewDialog.OnCompleteListener() {
                        @Override
                        public void create(String description, List<SwarmReviewPanel.Reviewer> reviewers,
                                List<FilePath> files) {
                            shelveFiles(project, clientConfig, changelistId, files)
                                    .mapQueryAsync(r -> sendCreateReview(project, swarmClient,
                                            description, changelistId, reviewers))
                                    .whenCompleted(sink::resolve)
                                    .whenServerError(e -> sink.resolve(-1));
                        }

                        @Override
                        public void cancel() {
                            sink.resolve(-1);
                        }
                    })
        );
    }

    @NotNull
    private static Answer<Integer> updateSwarmReview(Project project, ClientConfig clientConfig,
            P4ChangelistId changelistId, SwarmClient swarmClient, List<Review> reviews) {
        // TODO map reviews to fetched changelists.

        // FIXME implement
        LOG.warn("implement update swarm review dialog: " + changelistId + " -> " + reviews);
        SwarmErrorMessage.send(project).problemContactingServer(new SwarmErrorMessage.SwarmEvent(changelistId),
                new Exception("Update swarm review (" + reviews + ") - not implemented yet"));
        return Answer.resolve(-1);
    }

    private static P4CommandRunner.ActionAnswer<ShelveFilesResult> shelveFiles(Project project,
            ClientConfig clientConfig, P4ChangelistId changelistId, List<FilePath> files) {
        return P4ServerComponent.perform(project, clientConfig, new ShelveFilesAction(changelistId, files))
                .whenServerError(e -> {
                    // Error messages are handled by the user error section
                    SwarmErrorMessage.send(project).couldNotShelveFiles(new SwarmErrorMessage.SwarmEvent(changelistId),
                            P4Bundle.message("swarm-client.shelve.failed", e.getLocalizedMessage()));
                })
                .whenOffline(() -> {
                    SwarmErrorMessage.send(project).couldNotShelveFiles(new SwarmErrorMessage.SwarmEvent(changelistId),
                            P4Bundle.message("swarm-client.shelve.offline"));
                });
    }


    private static P4CommandRunner.QueryAnswer<Integer> sendCreateReview(Project project,
            SwarmClient swarmClient,
            String description, P4ChangelistId changelistId,
            List<SwarmReviewPanel.Reviewer> reviewers) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating swarm review for " + changelistId);
            }
            Review review = swarmClient.createReview(description, changelistId.getChangelistId(),
                    reviewers.stream()
                            .filter(r -> !r.required)
                            .map(r -> r.user.getUsername())
                            .toArray(String[]::new),
                    reviewers.stream()
                            .filter(r -> r.required)
                            .map(r -> r.user.getUsername())
                            .toArray(String[]::new));
            LOG.info("Created review " + review.getId() + " for changelist " + changelistId);

            // TODO find a better place to stick this bit of code.
            final URI uri = review.getReviewUri(swarmClient.getConfig());
            UserMessage.showNotification(project,
                    UserMessage.ALWAYS,
                    uri.toString(),
                    "Created Review " + review.getId(),
                    NotificationType.INFORMATION,
                    (notification, hyperlinkEvent) -> {
                        BrowserLauncher.getInstance().browse(uri);
                    },
                    () -> {});

            return new QueryAnswerImpl<>(Answer.resolve(review.getId()));
        } catch (IOException | SwarmServerResponseException e) {
            LOG.warn("Create review for " + changelistId + " caused error", e);
            SwarmErrorMessage.send(project).reviewCreateFailed(new SwarmErrorMessage.SwarmEvent(changelistId), e);
            return new QueryAnswerImpl<>(Answer.resolve(-1));
        }
    }

    @Nullable
    private static ClientConfig getClientConfigFor(@NotNull ProjectConfigRegistry registry,
            @NotNull P4ChangelistId changelistId) {
        // Just return the first one.
        for (final RootedClientConfig config : registry.getClientConfigsForRef(changelistId.getClientServerRef())) {
            return config.getClientConfig();
        }
        return null;
    }
}
