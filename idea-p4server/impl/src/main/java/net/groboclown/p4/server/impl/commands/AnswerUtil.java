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

package net.groboclown.p4.server.impl.commands;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AnswerUtil {
    private static final Logger LOG = Logger.getInstance(AnswerUtil.class);

    private static final P4CommandRunner.ResultError OFFLINE_RESULT_ERROR = new OfflineResultError();

    private static class OfflineResultError implements P4CommandRunner.ResultError {
        @NotNull
        @Override
        public P4CommandRunner.ErrorCategory getCategory() {
            return P4CommandRunner.ErrorCategory.CONNECTION;
        }

        @Nls
        @NotNull
        @Override
        public Optional<String> getMessage() {
            // FIXME better error message for localization.
            LOG.warn("FIXME better error message for localization.");
            return Optional.of("Server Offline");
        }
    }

    public static P4CommandRunner.ServerResultException createOfflineError() {
        return new P4CommandRunner.ServerResultException(OFFLINE_RESULT_ERROR);
    }
}
