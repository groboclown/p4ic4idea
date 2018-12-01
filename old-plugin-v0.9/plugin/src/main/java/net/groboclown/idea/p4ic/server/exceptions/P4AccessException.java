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

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.P4JavaException;
import org.jetbrains.annotations.NotNull;

public class P4AccessException extends P4DisconnectedException {
    public P4AccessException(@NotNull String message) {
        super(message);
    }

    public P4AccessException(@NotNull AccessException cause) {
        super(cause);
    }

    public P4JavaException getP4JavaException() {
        if (getCause() != null && getCause() instanceof P4JavaException) {
            return (P4JavaException) getCause();
        }
        return null;
    }
}
