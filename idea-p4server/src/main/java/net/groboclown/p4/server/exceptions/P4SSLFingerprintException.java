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

import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.TrustException;
import net.groboclown.idea.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Indicates that the host server reported a different SSL fingerprint
 * than the one declared by either the trust file, or the user.
 */
public class P4SSLFingerprintException extends P4SSLException {

    public P4SSLFingerprintException(@Nullable final String serverFingerprint, @NotNull final TrustException e){
        super(P4Bundle.message("exception.ssl.fingerprint", serverFingerprint, e.getFingerprint()), e);
    }

    public P4SSLFingerprintException(@Nullable final String serverFingerprint, @NotNull final ConnectionException e) {
        super(P4Bundle.message("exception.ssl.fingerprint", serverFingerprint, e.getMessage()), e);
    }
}
