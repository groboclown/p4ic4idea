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
package net.groboclown.idea.p4ic.server.exceptions;

import com.intellij.openapi.vcs.VcsConnectionProblem;
import org.jetbrains.annotations.NotNull;

public class P4TimingException extends VcsConnectionProblem {
    public P4TimingException(@NotNull Throwable cause) {
        super(P4DisconnectedException.getMessageFor(cause));
        initCause(cause);
    }


    @Override
    public boolean attemptQuickFix(boolean mayDisplayDialogs) {
        return false;
    }
}
