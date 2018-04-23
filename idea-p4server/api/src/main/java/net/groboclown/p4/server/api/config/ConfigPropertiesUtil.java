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

package net.groboclown.p4.server.api.config;

import com.perforce.p4java.env.PerforceEnvironment;
import net.groboclown.p4.server.api.config.part.DataPart;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ConfigPropertiesUtil {
    @NotNull
    public static Map<String, String> toProperties(@NotNull DataPart dataPart, @Nullable String valueIfUnset,
            @Nullable String valueIfPasswordEmpty, @Nullable String valueIfPasswordSet) {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(PerforceEnvironment.P4PORT,
                ! dataPart.hasServerNameSet() || dataPart.getServerName() == null
                        ? valueIfUnset
                        : dataPart.getServerName().getDisplayName());
        ret.put(PerforceEnvironment.P4TRUST,
                ! dataPart.hasTrustTicketFileSet() || dataPart.getTrustTicketFile() == null
                        ? valueIfUnset
                        : dataPart.getTrustTicketFile().toString());
        ret.put(PerforceEnvironment.P4USER,
                ! dataPart.hasUsernameSet() || dataPart.getUsername() == null
                        ? valueIfUnset
                        : dataPart.getUsername());
        ret.put(PerforceEnvironment.P4TICKETS,
                ! dataPart.hasAuthTicketFileSet() || dataPart.getAuthTicketFile() == null
                        ? valueIfUnset
                        : dataPart.getAuthTicketFile().toString());
        ret.put(PerforceEnvironment.P4SERVER_FINGERPRINT,
                ! dataPart.hasServerFingerprintSet() || dataPart.getServerFingerprint() == null
                        ? valueIfUnset
                        : dataPart.getServerFingerprint());
        ret.put(PerforceEnvironment.P4PASSWD,
                getPasswordValue(
                        dataPart.hasPasswordSet(),
                        dataPart.getPlaintextPassword(),
                        valueIfUnset, valueIfPasswordEmpty, valueIfPasswordSet));
        ret.put(PerforceEnvironment.P4CHARSET,
                ! dataPart.hasDefaultCharsetSet() || dataPart.getDefaultCharset() == null
                        ? valueIfUnset
                        : dataPart.getDefaultCharset());
        ret.put(PerforceEnvironment.P4IGNORE,
                ! dataPart.hasIgnoreFileNameSet() || dataPart.getIgnoreFileName() == null
                        ? valueIfUnset
                        : dataPart.getIgnoreFileName());
        ret.put(PerforceEnvironment.P4CLIENT,
                ! dataPart.hasClientnameSet() || dataPart.getClientname() == null
                        ? valueIfUnset
                        : dataPart.getClientname());
        ret.put(PerforceEnvironment.P4HOST,
                ! dataPart.hasClientHostnameSet() || dataPart.getClientHostname() == null
                        ? valueIfUnset
                        : dataPart.getClientHostname());
        ret.put(PerforceEnvironment.P4LOGINSSO,
                ! dataPart.hasLoginSsoSet() || dataPart.getLoginSso() == null
                        ? valueIfUnset
                        : dataPart.getLoginSso());
        return ret;
    }

    @NotNull
    public static Map<String, String> toProperties(@NotNull ServerConfig config, @Nullable String valueIfUnset,
            @Nullable String valueIfPasswordRequired, @Nullable String valueIfPasswordNotRequired) {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(PerforceEnvironment.P4PORT, config.getServerName().getDisplayName());
        ret.put(PerforceEnvironment.P4TRUST,
                ! config.hasTrustTicket() || config.getTrustTicket() == null
                        ? valueIfUnset
                        : config.getTrustTicket().toString());
        ret.put(PerforceEnvironment.P4USER, config.getUsername());
        ret.put(PerforceEnvironment.P4TICKETS,
                ! config.hasAuthTicket() || config.getAuthTicket() == null
                        ? valueIfUnset
                        : config.getAuthTicket().toString());
        ret.put(PerforceEnvironment.P4SERVER_FINGERPRINT,
                ! config.hasServerFingerprint() || config.getServerFingerprint() == null
                        ? valueIfUnset
                        : config.getServerFingerprint());
        ret.put(PerforceEnvironment.P4PASSWD,
                config.usesStoredPassword() ? valueIfPasswordRequired : valueIfPasswordNotRequired);
        ret.put(PerforceEnvironment.P4LOGINSSO,
                ! config.hasLoginSso() || config.getLoginSso() == null
                        ? valueIfUnset
                        : config.getLoginSso());
        return ret;
    }

    @NotNull
    public static Map<String, String> toProperties(@NotNull ClientConfig config, @Nullable String valueIfUnset,
            @Nullable String valueIfPasswordEmpty, @Nullable String valueIfPasswordSet) {
        Map<String, String> props = toProperties(config.getServerConfig(), valueIfUnset, valueIfPasswordEmpty, valueIfPasswordSet);
        props.put(PerforceEnvironment.P4CHARSET,
                config.getDefaultCharSet() == null
                        ? valueIfUnset
                        : config.getDefaultCharSet());
        props.put(PerforceEnvironment.P4IGNORE,
                config.getIgnoreFileName() == null
                        ? valueIfUnset
                        : config.getIgnoreFileName());
        props.put(PerforceEnvironment.P4CLIENT,
                config.getClientName() == null
                        ? valueIfUnset
                        : config.getClientName());
        props.put(PerforceEnvironment.P4HOST,
                config.getClientHostName() == null
                        ? valueIfUnset
                        : config.getClientHostName());
        return props;
    }


    private static String getPasswordValue(boolean isSet, @Nullable String plaintext, @Nullable String valueIfUnset,
            @Nullable String valueIfPasswordEmpty, @Nullable String valueIfPasswordSet) {
        return isSet
                ? (plaintext == null || plaintext.isEmpty()
                    ? valueIfPasswordEmpty
                    : valueIfPasswordSet)
                : valueIfUnset;
    }
}
