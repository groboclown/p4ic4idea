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

package net.groboclown.p4.server.impl.util;

import com.intellij.openapi.vcs.VcsException;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.async.Answer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ErrorCollectors {
    private ErrorCollectors() {
        // Should never be instantiated.
    }

    public static <R>
    Collector<P4CommandRunner.ActionAnswer<R>, Answer<R>, Answer<R>>
    collectActionErrors(@NotNull final List<? super VcsException> errors) {
        return new Collector<P4CommandRunner.ActionAnswer<R>, Answer<R>, Answer<R>>() {
            @Override
            public Supplier<Answer<R>> supplier() {
                return () -> Answer.resolve(null);
            }

            @Override
            public BiConsumer<Answer<R>, P4CommandRunner.ActionAnswer<R>> accumulator() {
                return (answer, action) -> answer.futureMap((r, sink) -> {
                    action
                            .whenCompleted(sink::resolve)
                            .whenServerError((e) -> {
                                errors.add(e);
                                sink.resolve(r);
                            })
                            .whenOffline(() -> sink.resolve(r));
                });
            }

            @Override
            public BinaryOperator<Answer<R>> combiner() {
                return (a, b) -> a.mapAsync((x) -> b);
            }

            @Override
            public Function<Answer<R>, Answer<R>> finisher() {
                return i -> i;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
            }
        };
    }
}
