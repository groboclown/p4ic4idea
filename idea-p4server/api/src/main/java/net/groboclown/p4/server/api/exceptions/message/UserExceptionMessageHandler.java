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

package net.groboclown.p4.server.api.exceptions.message;

import net.groboclown.p4.server.api.exceptions.P4ApiException;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import org.jetbrains.annotations.NotNull;

/**
 * Api that can be implemented once to transform exceptions into a user-consumable string.
 */
public interface UserExceptionMessageHandler {

    @NotNull
    Message internalError(@NotNull P4ApiException ex);

    @NotNull
    Message taskInterrupted(@NotNull VcsInterruptedException ex);
}
