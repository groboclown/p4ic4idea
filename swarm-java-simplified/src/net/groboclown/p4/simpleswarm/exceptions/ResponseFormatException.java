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

package net.groboclown.p4.simpleswarm.exceptions;

import com.google.gson.JsonElement;

public class ResponseFormatException extends SwarmServerResponseException {
    public ResponseFormatException(String element, JsonElement el, Throwable cause) {
        super("Incorrect Json format in " + element + ": " + (el == null ? "null data" : el.toString()), cause);
    }

    public ResponseFormatException(String element, JsonElement el) {
        super("Incorrect Json format in " + element + ": " + (el == null ? "null data" : el.toString()));
    }
}
