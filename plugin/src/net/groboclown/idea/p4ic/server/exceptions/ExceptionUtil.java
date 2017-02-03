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
import com.perforce.p4java.exception.AuthenticationFailedException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import org.jetbrains.annotations.NotNull;

/**
 * Special checks for
 */
public class ExceptionUtil
{
    private static final String SESSION_EXPIRED_MESSAGE_1 = "Your session has expired, please login again.";
    private static final String SESSION_EXPIRED_MESSAGE_2 = "Your session has expired";
    private static final String SESSION_EXPIRED_MESSAGE_3 = "Your session has expired, please %'login'% again.";
    private static final String PASSWORD_INVALID_MESSAGE_1 = "Perforce password (P4PASSWD) invalid or unset.";
    private static final String PASSWORD_INVALID_MESSAGE_2 = "Perforce password (P4PASSWD)";
    private static final String PASSWORD_INVALID_MESSAGE_3 = "Perforce password (%'P4PASSWD'%)";

    private static final String PASSWORD_UNNECCESSARY_MESSAGE_1 = "'login' not necessary";
    private static final String PASSWORD_UNNECCESSARY_MESSAGE_2 = "%'login'% not necessary";

    public static boolean isAuthenticationProblem(@NotNull P4JavaException ex) {
        return isLoginPasswordProblem(ex)
                || isLoginRequiresPasswordProblem(ex)
                || isSessionExpiredProblem(ex)
                || isLoginUnnecessary(ex);
    }

    public static boolean isLoginRequiresPasswordProblem(@NotNull P4JavaException ex) {
        return ex instanceof LoginRequiresPasswordException;
    }

    public static boolean isLoginPasswordProblem(@NotNull P4JavaException ex) {
        // TODO extend with correct error code checking.

        if (ex instanceof RequestException) {
            RequestException rex = (RequestException) ex;
            return (rex.hasMessageFragment(PASSWORD_INVALID_MESSAGE_1)
                    || rex.hasMessageFragment(PASSWORD_INVALID_MESSAGE_2)
                    || rex.hasMessageFragment(PASSWORD_INVALID_MESSAGE_3)
                    || (rex.getGenericCode() == MessageGenericCode.EV_CONFIG &&
                        rex.getSubCode() == 21));
        }
        if (ex instanceof AuthenticationFailedException) {
            AuthenticationFailedException afex = (AuthenticationFailedException) ex;
            return afex.getErrorType() == AuthenticationFailedException.ErrorType.PASSWORD_INVALID;
        }
        if (ex instanceof AccessException) {
            AccessException aex = (AccessException) ex;
            // see P4ServerName for a list of the authentication failure messages.
            return (aex.hasMessageFragment(PASSWORD_INVALID_MESSAGE_1)
                    || aex.hasMessageFragment(PASSWORD_INVALID_MESSAGE_2)
                    || aex.hasMessageFragment(PASSWORD_INVALID_MESSAGE_3));
        }
        return false;
    }

    public static boolean isLoginUnnecessary(@NotNull P4JavaException ex) {
        // TODO extend with correct error code checking.

        if (ex instanceof RequestException) {
            RequestException rex = (RequestException) ex;
            return (rex.hasMessageFragment(PASSWORD_UNNECCESSARY_MESSAGE_1)
                    || rex.hasMessageFragment(PASSWORD_UNNECCESSARY_MESSAGE_2));
        }
        if (ex instanceof AccessException) {
            AccessException aex = (AccessException) ex;
            return (aex.hasMessageFragment(PASSWORD_UNNECCESSARY_MESSAGE_1)
                    || aex.hasMessageFragment(PASSWORD_UNNECCESSARY_MESSAGE_2));
        }
        return false;
    }

    public static boolean isSessionExpiredProblem(@NotNull P4JavaException ex) {
        // TODO extend with correct error code checking.

        if (ex instanceof RequestException) {
            RequestException rex = (RequestException) ex;
            return (rex.hasMessageFragment(SESSION_EXPIRED_MESSAGE_1)
                    || rex.hasMessageFragment(SESSION_EXPIRED_MESSAGE_2)
                    || rex.hasMessageFragment(SESSION_EXPIRED_MESSAGE_3)
                    || (rex.getGenericCode() == MessageGenericCode.EV_CONFIG &&
                    rex.getSubCode() == 21));
        }
        if (ex instanceof AuthenticationFailedException) {
            AuthenticationFailedException afex = (AuthenticationFailedException) ex;
            return afex.getErrorType() == AuthenticationFailedException.ErrorType.SESSION_EXPIRED;
        }
        if (ex instanceof AccessException) {
            AccessException aex = (AccessException) ex;
            // see P4ServerName for a list of the authentication failure messages.
            return (aex.hasMessageFragment(SESSION_EXPIRED_MESSAGE_1)
                    || aex.hasMessageFragment(SESSION_EXPIRED_MESSAGE_2)
                    || aex.hasMessageFragment(SESSION_EXPIRED_MESSAGE_3));
        }
        return false;
    }
}
