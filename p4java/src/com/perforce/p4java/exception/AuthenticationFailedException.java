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

package com.perforce.p4java.exception;

import com.perforce.p4java.server.IServerMessage;

// p4ic4idea: created exception for better precision in understanding the problem
public class AuthenticationFailedException extends AccessException {
    private final ErrorType type;

    public enum ErrorType {
        PASSWORD_INVALID,
        NOT_LOGGED_IN,
        SESSION_EXPIRED,
        SSO_LOGIN
    }

    public AuthenticationFailedException(ErrorType type, IServerMessage err) {
        super(err);
        this.type = type;
    }

    public ErrorType getErrorType() {
        return type;
    }
}
