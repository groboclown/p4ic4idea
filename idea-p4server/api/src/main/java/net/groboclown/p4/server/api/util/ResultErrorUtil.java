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

package net.groboclown.p4.server.api.util;

import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Originally part of the testing, but has usage outside this test collection.
 */
public class ResultErrorUtil {
    public static P4CommandRunner.ResultError createResultError(
            @NotNull P4CommandRunner.ErrorCategory category, @Nullable String message) {
        return new SR(category, message);
    }

    public static P4CommandRunner.ServerResultException createException(
            @NotNull P4CommandRunner.ErrorCategory category, @Nullable String message, @Nullable Throwable t) {
        // Avoid needing to initialize the VcsBundle by always including an exception with a message.
        if (t == null) {
            t = new RuntimeException("blah");
        }
        if (message == null) {
            if (t.getMessage() == null) {
                throw new IllegalArgumentException("Better add in a message, or you'll be sorry.");
            }
            message = t.getMessage();
        }
        return new P4CommandRunner.ServerResultException(createResultError(category, message), t);
    }

    public static P4CommandRunner.ServerResultException createInternalException() {
        return createException(P4CommandRunner.ErrorCategory.INTERNAL, "internal", null);
    }


    private static class SR implements P4CommandRunner.ResultError {
        private final P4CommandRunner.ErrorCategory category;
        private final String message;

        public SR(P4CommandRunner.ErrorCategory category, String message) {
            this.category = category;
            this.message = message;
        }

        @NotNull
        @Override
        public P4CommandRunner.ErrorCategory getCategory() {
            return category;
        }

        @Nls
        @NotNull
        @Override
        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }
    }
}
