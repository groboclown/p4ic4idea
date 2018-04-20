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

import com.perforce.p4java.exception.AccessException;

/**
 * Generated when a login attempt failed because no password
 * was known, and the user is required to have one.
 * This happens most often after a password was removed from the
 * password store because it was considered incorrect.
 */
public class LoginRequiresPasswordException extends AccessException {
    public LoginRequiresPasswordException(AccessException ex) {
        super(ex.getServerMessage());
    }
}
