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
package net.groboclown.idea.p4ic.background;

import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/**
 * A Vcs specific version of a {@link java.util.concurrent.Future} class.
 * It doesn't directly implement Future, so that it can have
 * the better throwable clauses.
 *
 * @param <T>
 */
public interface VcsFuture<T> /*extends Future<T>*/ {
    public boolean cancel(boolean mayInterruptIfRunning);
    public boolean isCancelled();
    public boolean isDone();
    public T get() throws VcsException, CancellationException;
    public T get(long timeout, @NotNull TimeUnit unit) throws VcsException, CancellationException;


    /**
     * Immediately gets the value for the future.  If the future isn't
     * ready yet, it cancels the future and throws a CancellationException.
     *
     * @return the value set to the future.
     * @throws com.intellij.openapi.vcs.VcsException
     * @throws java.util.concurrent.CancellationException
     */
    public T getImmediately() throws VcsException, CancellationException;
}
