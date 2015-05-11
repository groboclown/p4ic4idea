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
package net.groboclown.idea.p4ic.server.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.p4ic.server.P4Exec;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;

/**
 * Higher level actions on Perforce objects.
 */
public abstract class ServerTask<T> {
    public abstract T run(@NotNull P4Exec exec)
            throws VcsException, CancellationException;
}
