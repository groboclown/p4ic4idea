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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;

/**
 * Puts the executing thread into the correct read or write state.
 * This should be used in lieu of the
 * {@link com.intellij.openapi.application.Application#runReadAction(Computable)}
 * and
 * {@link com.intellij.openapi.application.Application#runWriteAction(Computable)},
 * because it may include extra synchronization protections.
 */
public interface SynchronizedActionRunner {
    <T, E extends Throwable> T read(ThrowableComputable<T, E> runner) throws E;

    <T> T read(Computable<T> runner);

    void read(Runnable runner);

    <T, E extends Throwable> T write(ThrowableComputable<T, E> runner) throws E;

    <T> T write(Computable<T> runner);

    void write(Runnable runner);
}
