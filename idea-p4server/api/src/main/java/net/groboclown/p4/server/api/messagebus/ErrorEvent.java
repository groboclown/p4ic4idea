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

package net.groboclown.p4.server.api.messagebus;

import org.jetbrains.annotations.NotNull;

public class ErrorEvent<E extends Throwable> extends AbstractMessageEvent {
    private final E error;

    public ErrorEvent(@NotNull E error) {
        this.error = error;
    }

    @NotNull
    public E getError() {
        return error;
    }

    public String getMessage() {
        return error.getMessage();
    }
}