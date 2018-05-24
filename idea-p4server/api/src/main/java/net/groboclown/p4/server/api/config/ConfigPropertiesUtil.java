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
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper utility to generate P4CONFIG like properties based on the configurations.
 */
public class ConfigPropertiesUtil {
    @NotNull
    public static Map<String, String> toProperties(@NotNull ConfigPart configPart, @Nullable String valueIfUnset,
            @Nullable String valueIfPasswordEmpty, @Nullable String valueIfPasswordSet) {
        Map<String, String> ret = new HashMap<>();
        ret.put(PerforceEnvironment.P4PORT,
                ! configPart.hasServerNameSet() || configPart.getServerName() == null
                        ? valueIfUnset
                        : configPart.getServerName().getDisplayName());
        ret.put(PerforceEnvironment.P4TRUST,
                ! configPart.hasTrustTicketFileSet() || configPart.getTrustTicketFile() == null
                        ? valueIfUnset
                        : configPart.getTrustTicketFile().toString());
        ret.put(PerforceEnvironment.P4USER,
                ! configPart.hasUsernameSet() || configPart.getUsername() == null
                        ? valueIfUnset
                        : configPart.getUsername());
        ret.put(PerforceEnvironment.P4TICKETS,
                ! configPart.hasAuthTicketFileSet() || configPart.getAuthTicketFile() == null
                        ? valueIfUnset
                        : configPart.getAuthTicketFile().toString());
        ret.put(PerforceEnvironment.P4SERVER_FINGERPRINT,
                ! configPart.hasServerFingerprintSet() || configPart.getServerFingerprint() == null
                        ? valueIfUnset
                        : configPart.getServerFingerprint());
        ret.put(PerforceEnvironment.P4PASSWD,
                getPasswordValue(
                        configPart.hasPasswordSet() && !configPart.requiresUserEnteredPassword(),
                        configPart.getPlaintextPassword(),
                        valueIfUnset, valueIfPasswordEmpty, valueIfPasswordSet));
        ret.put(PerforceEnvironment.P4CHARSET,
                ! configPart.hasDefaultCharsetSet() || configPart.getDefaultCharset() == null
                        ? valueIfUnset
                        : configPart.getDefaultCharset());
        ret.put(PerforceEnvironment.P4IGNORE,
                ! configPart.hasIgnoreFileNameSet() || configPart.getIgnoreFileName() == null
                        ? valueIfUnset
                        : configPart.getIgnoreFileName());
        ret.put(PerforceEnvironment.P4CLIENT,
                ! configPart.hasClientnameSet() || configPart.getClientname() == null
                        ? valueIfUnset
                        : configPart.getClientname());
        ret.put(PerforceEnvironment.P4HOST,
                ! configPart.hasClientHostnameSet() || configPart.getClientHostname() == null
                        ? valueIfUnset
                        : configPart.getClientHostname());
        ret.put(PerforceEnvironment.P4LOGINSSO,
                ! configPart.hasLoginSsoSet() || configPart.getLoginSso() == null
                        ? valueIfUnset
                        : configPart.getLoginSso());
        return ret;
    }

    @NotNull
    public static Map<String, String> toProperties(@NotNull ServerConfig config, @Nullable String valueIfUnset,
            @Nullable String valueIfPasswordStored, @Nullable String valueIfPasswordNotStored) {
        Map<String, String> ret = new HashMap<>();
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
                config.usesStoredPassword() ? valueIfPasswordStored : valueIfPasswordNotStored);
        ret.put(PerforceEnvironment.P4LOGINSSO,
                ! config.hasLoginSso() || config.getLoginSso() == null
                        ? valueIfUnset
                        : config.getLoginSso());
        return ret;
    }

    @NotNull
    public static Map<String, String> toProperties(@NotNull ClientConfig config, @Nullable String valueIfUnset,
            @Nullable String valueIfPasswordStored, @Nullable String valueIfPasswordNotStored) {
        Map<String, String> props = toProperties(config.getServerConfig(),
                valueIfUnset, valueIfPasswordStored, valueIfPasswordNotStored);
        props.put(PerforceEnvironment.P4CHARSET,
                config.getDefaultCharSet() == null
                        ? valueIfUnset
                        : config.getDefaultCharSet());
        props.put(PerforceEnvironment.P4IGNORE,
                config.getIgnoreFileName() == null
                        ? valueIfUnset
                        : config.getIgnoreFileName());
        props.put(PerforceEnvironment.P4CLIENT,
                config.getClientname() == null
                        ? valueIfUnset
                        : config.getClientname());
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
