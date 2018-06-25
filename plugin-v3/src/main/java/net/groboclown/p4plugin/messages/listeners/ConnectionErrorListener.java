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

package net.groboclown.p4plugin.messages.listeners;

import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.FileSaveException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.exception.ZeroconfException;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// FIXME implement handlers
public class ConnectionErrorListener implements ConnectionErrorMessage.Listener {
    @Override
    public void unknownServer(@NotNull P4ServerName name, @Nullable ServerConfig config, @NotNull Exception e) {

    }

    @Override
    public void couldNotWrite(@NotNull ServerConfig config, @NotNull FileSaveException e) {

    }

    @Override
    public void zeroconfProblem(@NotNull P4ServerName name, @Nullable ServerConfig config,
            @NotNull ZeroconfException e) {

    }

    @Override
    public void sslHostTrustNotEstablished(@NotNull ServerConfig serverConfig) {

    }

    @Override
    public void sslHostFingerprintMismatch(@NotNull ServerConfig serverConfig, @NotNull TrustException e) {

    }

    @Override
    public void sslAlgorithmNotSupported(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig) {

    }

    @Override
    public void sslPeerUnverified(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig,
            @NotNull SslHandshakeException e) {

    }

    @Override
    public void sslCertificateIssue(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
            @NotNull SslException e) {

    }

    @Override
    public void connectionError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
            @NotNull ConnectionException e) {

    }

    @Override
    public void resourcesUnavailable(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
            @NotNull ResourceException e) {

    }
}
