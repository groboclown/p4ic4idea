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
package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

public class VcsExceptionUtil {
    private static final Logger LOG = Logger.getInstance(VcsExceptionUtil.class);


    /**
     * Rethrows immediately any Throwable that should never be caught or wrapped.
     *
     * @param t
     */
    public static void alwaysThrown(@NotNull Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
        if (t instanceof AssertionError) {
            throw (AssertionError) t;
        }
    }


    /**
     * Does the exception indicate a task cancellation?
     *
     * @param t thrown to check
     * @return true if it indicates a cancellation.
     */
    public static boolean isCancellation(@Nullable Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof CancellationException) {
            return true;
        }
        if (t instanceof InterruptedException) {
            return true;
        }
        if (t instanceof TimeoutException) {
            // the equivalent of a cancel, because the limited time window
            // ran out.
            return true;
        }
        if (t.getMessage() != null && t.getMessage().equals("Task was cancelled.")) {
            if (t.getCause() != null && t.getCause() != t) {
                LOG.info("Ignoring cancel message with underling cause", t.getCause());
            } else {
                LOG.info("Ignoring message", t);
            }
            return true;
        }
        return false;
    }

}
