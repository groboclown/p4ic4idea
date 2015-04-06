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
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URISyntaxException;
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
    }

    public static boolean isPasswordWrongMessage(@Nullable String message) {
        if (message == null) {
            return false;
        }
        return message.contains("Perforce password (P4PASSWD) invalid or unset.");
    }


    public static boolean isLoginWrongMessage(@Nullable String message) {
        if (message == null) {
            return false;
        }
        return (message.contains("Access for user '") &&
                message.contains("' has not been enabled by 'p4 protect'") ||
                message.contains("Unable to resolve Perforce server host name '"));
    }


    public static boolean isDisconnectErrorMessage(@Nullable String message) {
        return (message != null && ("Disconnected from Perforce server".equals(message) ||
                "Not currently connected to a Perforce server".equals(message) ||
                message.contains("Unable to connect to Perforce server at ") ||
                message.endsWith("Read timed out") ||
                isPasswordWrongMessage(message) ||
                isLoginWrongMessage(message)));
    }


    public static boolean isDisconnectError(@Nullable Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof VcsConnectionProblem || t instanceof ConnectionException ||
                t instanceof AccessException || t instanceof URISyntaxException) {
            return true;
        }

        if (t.getCause() != null && t.getCause() != t) {
            Throwable cause = t.getCause();
            if (t instanceof P4JavaException || t instanceof P4JavaError) {
                if (cause instanceof IOException) {
                    return true;
                }
            }
        }

        return isDisconnectErrorMessage(t.getMessage());
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
