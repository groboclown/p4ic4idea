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

package net.groboclown.p4plugin.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.Consumer;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.progress.ProgressIndicator;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs an asynchronous process in a background thread, that dribbles out values
 * incrementally.  Each value that is produced is consumed by consumers in the EDT.
 * The objects consumed can be grouped together into batch sizes.  When the producer
 * finishes, a series of final runners can execute.
 * <p>
 * The instances can be safely reused for multiple runs.
 */
public class EdtSinkProcessor<E> {
    private final List<Consumer<Collection<E>>> batchItemConsumers = new ArrayList<>();
    private final List<Consumer<E>> itemConsumers = new ArrayList<>();
    private final List<Runnable> starters = new ArrayList<>();
    private final List<Runnable> finalizers = new ArrayList<>();

    public interface Sink<E> {
        /**
         * Offers an item to the sink, with an estimate for the number of remaining items
         * (used for progress bars).
         *
         * @param item item sent to the consumers.
         * @param estimatedRemainingItems an estimate of the remaining number of items
         *                                to process; used for the progress indicator.
         * @throws CancellationException if the user cancelled the progress.
         */
        void offer(E item, int estimatedRemainingItems)
                throws CancellationException;

        /**
         * Called when the process is finished.  Alternatively, when the process finishes
         * on its own, this will be automatically called.  Multiple calls will be ignored.
         * This is handy if there's cleanup work that needs to run in the background, but
         * the EDT finalizers can run before the cleanup.
         */
        void end();
    }

    public interface Producer<E> {
        void produce(Sink<E> sink);
    }

    public void addConsumer(@NotNull Consumer<E> consumer) {
        synchronized (itemConsumers) {
            itemConsumers.add(consumer);
        }
    }

    public void addBatchConsumer(@NotNull Consumer<Collection<E>> consumer) {
        synchronized (batchItemConsumers) {
            batchItemConsumers.add(consumer);
        }
    }

    public void addIcon(@NotNull final AsyncProcessIcon icon) {
        addStarter(() -> icon.setVisible(true));
        addFinalizer(() -> icon.setVisible(false));
    }

    /**
     * Disables the component while the action runs.  Useful for controlling buttons.
     * If the component state depends on more than just running/not running, then
     * a controlling runner should be added.
     *
     * @param component component that is disabled while running, and enabled when it finishes.
     */
    public void addDisabledWhileRunningComponent(@NotNull final JComponent component) {
        addStarter(() -> component.setEnabled(false));
        addFinalizer(() -> component.setEnabled(true));
    }

    public void addStarter(@NotNull Runnable runner) {
        synchronized (starters) {
            starters.add(runner);
        }
    }

    public void addFinalizer(@NotNull Runnable runner) {
        synchronized (finalizers) {
            finalizers.add(runner);
        }
    }

    public void process(@NotNull final Producer<E> producer,
            @Nullable final ProgressIndicator progress,
            final int batchSize, final boolean runEdtAsync) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            onStart(runEdtAsync);
            final QueueSink<E> sink = new QueueSink<>(batchSize, progress,
                    () -> onEnd(runEdtAsync),
                    (e) -> consumeOne(e, runEdtAsync),
                    (ee) -> consumeBatch(ee, runEdtAsync));
            producer.produce(sink);

            // Force the end to be called.  The sink will ignore this if it was
            // already called by the producer.
            sink.end();
        });
    }

    private void consumeOne(E item, boolean runEdtAsync) {
        List<Consumer<E>> consumers;
        synchronized (itemConsumers) {
            consumers = new ArrayList<>(itemConsumers);
        }
        for (Consumer<E> consumer : consumers) {
            runEdt(consumer, item, runEdtAsync);
        }
    }

    private void consumeBatch(List<E> items, boolean runEdtAsync) {
        List<Consumer<Collection<E>>> consumers;
        synchronized (batchItemConsumers) {
            consumers = new ArrayList<>(batchItemConsumers);
        }
        for (Consumer<Collection<E>> consumer : consumers) {
            runEdt(consumer, items, runEdtAsync);
        }
    }

    private void onStart(boolean runEdtAsync) {
        List<Runnable> runners;
        synchronized (starters) {
            runners = new ArrayList<>(starters);
        }
        for (Runnable runner : runners) {
            runEdt(runner, runEdtAsync);
        }
    }

    private void onEnd(boolean runEdtAsync) {
        // This will run from within the thread that runs the producer, which is
        // a pooled thread.  Therefore, we don't need to explicitly run this in a
        // pooled thread.
        List<Runnable> runners;
        synchronized (finalizers) {
            runners = new ArrayList<>(finalizers);
        }
        for (Runnable runner : runners) {
            runEdt(runner, runEdtAsync);
        }
    }

    private <T> void runEdt(final Consumer<T> consumer, final T item, boolean runEdtAsync) {
        runEdt(() -> consumer.consume(item), runEdtAsync);
    }

    private void runEdt(Runnable runner, boolean runEdtAsync) {
        if (runEdtAsync) {
            ApplicationManager.getApplication().invokeLater(runner);
        } else {
            ApplicationManager.getApplication().invokeAndWait(runner);
        }
    }

    private static class QueueSink<E> implements Sink<E> {
        private final Object sync = new Object();
        private final ProgressIndicator progress;
        private final int batchSize;
        private final E[] batch;
        private final Consumer<E> itemConsumer;
        private final Consumer<List<E>> batchConsumer;
        private final Runnable onEnd;
        private int batchPos;
        private AtomicInteger itemCount;
        private boolean ended;

        @SuppressWarnings("unchecked")
        QueueSink(int batchSize, @Nullable ProgressIndicator progress, Runnable onEnd,
                Consumer<E> itemConsumer, Consumer<List<E>> batchConsumer) {
            assert batchSize > 0;
            this.progress = progress;
            this.batchSize = batchSize;
            this.batch = (E[]) new Object[batchSize];
            this.itemConsumer = itemConsumer;
            this.batchConsumer = batchConsumer;
            this.onEnd = onEnd;

            batchPos = 0;
            itemCount = new AtomicInteger(0);
        }

        @Override
        public void offer(E item, int estimatedRemainingItems)
                throws CancellationException {
            if (progress != null && progress.isCanceled()) {
                throw new CancellationException();
            }
            double pos = itemCount.incrementAndGet();
            List<E> toOffer = null;
            synchronized (sync) {
                if (ended) {
                    throw new IllegalStateException("already ended");
                }
                batch[batchPos++] = item;
                if (batchPos >= batchSize) {
                    toOffer = Arrays.asList(batch);
                    batchPos = 0;
                }
            }
            if (progress != null) {
                // TODO does the progress need to be updated in the EDT?
                if (estimatedRemainingItems >= 0) {
                    progress.setIndeterminate(true);
                } else {
                    progress.setIndeterminate(false);
                    progress.setFraction(
                            pos / (((double) estimatedRemainingItems) + pos)
                    );
                }
            }
            itemConsumer.consume(item);
            if (toOffer != null) {
                batchConsumer.consume(toOffer);
            }
        }

        @Override
        public void end() {
            synchronized (sync) {
                if (!ended) {
                    ended = true;
                    onEnd.run();
                }
            }
        }
    }
}
