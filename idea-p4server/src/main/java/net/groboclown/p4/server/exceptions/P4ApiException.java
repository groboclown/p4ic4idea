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
package net.groboclown.p4.server.exceptions;

import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import org.jetbrains.annotations.NotNull;

public class P4ApiException extends P4DisconnectedException {
    public P4ApiException(@NotNull P4JavaError e) {
        super(e);
    }

    public P4ApiException(@NotNull P4JavaException e) {
        super(e);
    }

    public P4ApiException(String message) {
        super(message);
    }
}
