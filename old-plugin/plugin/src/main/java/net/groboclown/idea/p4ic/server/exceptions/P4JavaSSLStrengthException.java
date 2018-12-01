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

import com.perforce.p4java.exception.ConnectionException;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.NotNull;

public class P4JavaSSLStrengthException extends P4SSLException {
    public P4JavaSSLStrengthException(@NotNull ConnectionException e) {
        super(P4Bundle.message("exception.java.ssl.keystrength",
                System.getProperty("java.version") == null ? "<unknown>" : System.getProperty("java.version"),
                System.getProperty("java.vendor") == null ? "<unknown>" : System.getProperty("java.vendor"),
                System.getProperty("java.vendor.url") == null ? "<unknown>" : System.getProperty("java.vendor.url"),
                System.getProperty("java.home") == null ? "<unknown>" : System.getProperty("java.home")
        ), e);
    }


    @Override
    public boolean attemptQuickFix(boolean mayDisplayDialogs) {
        return false;
    }

}
