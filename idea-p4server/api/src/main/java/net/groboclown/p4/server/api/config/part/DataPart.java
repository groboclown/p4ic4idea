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

package net.groboclown.p4.server.api.config.part;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.P4ServerName;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public interface DataPart extends ConfigPart {
    @Nullable
    VirtualFile getRootPath();

    boolean hasServerNameSet();

    @Nullable
    P4ServerName getServerName();

    boolean hasClientnameSet();

    @Nullable
    String getClientname();

    boolean hasUsernameSet();

    @Nullable
    String getUsername();

    boolean hasPasswordSet();

    @Nullable
    String getPlaintextPassword();

    boolean hasAuthTicketFileSet();

    @Nullable
    File getAuthTicketFile();

    boolean hasTrustTicketFileSet();

    @Nullable
    File getTrustTicketFile();

    boolean hasServerFingerprintSet();

    @Nullable
    String getServerFingerprint();

    boolean hasClientHostnameSet();

    /**
     * Allow for custom setting the client hostname.
     *
     * @return hostname of the client.
     */
    @Nullable
    String getClientHostname();

    boolean hasIgnoreFileNameSet();

    @Nullable
    String getIgnoreFileName();

    boolean hasDefaultCharsetSet();

    @Nullable
    String getDefaultCharset();

    boolean hasLoginSsoSet();

    @Nullable
    String getLoginSso();
}
