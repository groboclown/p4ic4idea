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

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.NoSuchAlgorithmException;

/**
 * Exception to indicate that the low-level SSL socket handshake between
 * the server and this Java library failed.  This is subtly different from
 * a library usage error, where the expectations of this library of the
 * ssl API are incorrect.
 */
// p4ic4idea: special extension that indicates that there's a problem with
// the ssl setup, such as the encryption algorithm isn't supported by the jvm.
public class SslHandshakeException extends SslException {
    public SslHandshakeException(String message, SSLPeerUnverifiedException cause) {
        super(message, cause);
    }

    public SslHandshakeException(String message, NoSuchAlgorithmException e) {
        super(message, e);
    }
}
