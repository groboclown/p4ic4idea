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
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import org.jetbrains.annotations.NotNull;

/**
 * Special checks for
 */
public class ExceptionUtil
{
    public static boolean isPasswordProblem(@NotNull P4JavaException ex) {
        // TODO extend with correct error code checking.

        if (ex instanceof RequestException) {
            RequestException rex = (RequestException) ex;
            return (rex.hasMessageFragment("Your session has expired, please login again.")
                    || rex.hasMessageFragment("Perforce password (P4PASSWD) invalid or unset.")
                    || (rex.getGenericCode() == MessageGenericCode.EV_CONFIG &&
                        rex.getSubCode() == 21));
        }
        if (ex instanceof AccessException) {
            AccessException aex = (AccessException) ex;
            // see Server for a list of the authentication failure messages.
            return (aex.hasMessageFragment("Perforce password (P4PASSWD)")
                    || aex.hasMessageFragment("Your session has expired"));
        }
        // Not needed - we'll return false anyway.
        //if (ex instanceof LoginRequiresPasswordException) {
        //    LOG.info("No password specified, but one is needed", ex);
        //    return false;
        //}
        return false;
    }
}
