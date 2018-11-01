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

public class SwarmServerResponseException extends InvalidSwarmServerException {
    private final String requestObject;
    private final String requestAction;
    private final int responseCode;

    public SwarmServerResponseException(String message, String object, String action, int code) {
        super(message);
        this.requestObject = object;
        this.requestAction = action;
        this.responseCode = code;
    }

    public SwarmServerResponseException(String message, Throwable cause) {
        super(message, cause);
        this.requestObject = null;
        this.requestAction = null;
        this.responseCode = -1;
    }

    public SwarmServerResponseException(String message) {
        this(message, null, null, -1);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getRequestObject() {
        return requestObject;
    }

    public String getRequestAction() {
        return requestAction;
    }
}
