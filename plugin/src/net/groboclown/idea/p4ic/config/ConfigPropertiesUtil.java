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

package net.groboclown.idea.p4ic.config;

import com.perforce.p4java.env.PerforceEnvironment;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.DataPart;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ConfigPropertiesUtil {
    private static final String SERVER_FINGERPRINT_KEY =
            P4Bundle.getString("configuration.resolve.key.server-fingerprint");

    @NotNull
    public static Map<String, String> toProperties(@NotNull DataPart dataPart) {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(PerforceEnvironment.P4PORT,
                ! dataPart.hasServerNameSet() || dataPart.getServerName() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getServerName().getDisplayName());
        ret.put(PerforceEnvironment.P4TRUST,
                ! dataPart.hasTrustTicketFileSet() || dataPart.getTrustTicketFile() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getTrustTicketFile().toString());
        ret.put(PerforceEnvironment.P4USER,
                ! dataPart.hasUsernameSet() || dataPart.getUsername() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getUsername());
        ret.put(PerforceEnvironment.P4TICKETS,
                ! dataPart.hasAuthTicketFileSet() || dataPart.getAuthTicketFile() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getAuthTicketFile().toString());
        ret.put(SERVER_FINGERPRINT_KEY,
                ! dataPart.hasServerFingerprintSet() || dataPart.getServerFingerprint() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getServerFingerprint());
        ret.put(PerforceEnvironment.P4PASSWD,
                dataPart.hasPasswordSet()
                        ? (dataPart.getPlaintextPassword() == null || dataPart.getPlaintextPassword().isEmpty()
                            ? P4Bundle.getString("configuration.resolve.password.empty")
                            : P4Bundle.getString("configuration.resolve.password.set"))
                        : P4Bundle.getString("configuration.resolve.password.unset"));
        ret.put(PerforceEnvironment.P4CHARSET,
                ! dataPart.hasDefaultCharsetSet() || dataPart.getDefaultCharset() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getDefaultCharset());
        ret.put(PerforceEnvironment.P4IGNORE,
                ! dataPart.hasIgnoreFileNameSet() || dataPart.getIgnoreFileName() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getIgnoreFileName());
        ret.put(PerforceEnvironment.P4CLIENT,
                ! dataPart.hasClientnameSet() || dataPart.getClientname() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getClientname());
        ret.put(PerforceEnvironment.P4HOST,
                ! dataPart.hasClientHostnameSet() || dataPart.getClientHostname() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getClientHostname());
        ret.put("P4LOGINSSO",
                ! dataPart.hasLoginSsoSet() || dataPart.getLoginSso() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : dataPart.getLoginSso().toString());
        return ret;
    }

    @NotNull
    public static Map<String, String> toProperties(@NotNull ServerConfig config) {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(PerforceEnvironment.P4PORT, config.getServerName().getDisplayName());
        ret.put(PerforceEnvironment.P4TRUST,
                ! config.hasTrustTicket() || config.getTrustTicket() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : config.getTrustTicket().toString());
        ret.put(PerforceEnvironment.P4USER, config.getUsername());
        ret.put(PerforceEnvironment.P4TICKETS,
                ! config.hasAuthTicket() || config.getAuthTicket() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : config.getAuthTicket().toString());
        ret.put(SERVER_FINGERPRINT_KEY,
                ! config.hasServerFingerprint() || config.getServerFingerprint() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : config.getServerFingerprint());
        ret.put(PerforceEnvironment.P4PASSWD,
                config.getPlaintextPassword() == null
                        ? P4Bundle.getString("configuration.resolve.password.unset")
                        : P4Bundle.getString("configuration.resolve.password.set"));
        return ret;
    }

    @NotNull
    public static Map<String, String> toProperties(@NotNull ClientConfig config) {
        Map<String, String> props = toProperties(config.getServerConfig());
        props.put(PerforceEnvironment.P4CHARSET,
                config.getDefaultCharSet() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : config.getDefaultCharSet());
        props.put(PerforceEnvironment.P4IGNORE,
                config.getIgnoreFileName() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : config.getIgnoreFileName());
        props.put(PerforceEnvironment.P4CLIENT,
                config.getClientName() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : config.getClientName());
        props.put(PerforceEnvironment.P4HOST,
                config.getClientHostName() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : config.getClientHostName());
        return props;
    }
}
