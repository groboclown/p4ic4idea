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

package net.groboclown.p4.server.impl.cache.store;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A map that stores only Strings, so that it can easily be persisted through XMLSerializer.  It
 * provides conversion methods.
 *
 * Must be public for serializer to work properly.
 */
public final class PrimitiveMap {
    private static final Logger LOG = Logger.getInstance(PrimitiveMap.class);


    // Public so the serializer can work properly.  Do not access directly.
    @SuppressWarnings("WeakerAccess")
    public Map<String, String> $proxy$ = new HashMap<>();


    static class UnmarshalException extends Exception {
        UnmarshalException(String key, String expectedType, Object found) {
            super("Could not unmarshall [" + key + "]: expected " + expectedType + ", found " + found);
        }
        UnmarshalException(String key, String expectedType) {
            super("Could not unmarshall [" + key + "]: expected non-null " + expectedType);
        }
    }


    int getIntNullable(@NotNull String key, int defaultValue)
            throws UnmarshalException {
        return getNotNull(key, defaultValue, "int", Integer::parseInt);
    }

    @NotNull
    PrimitiveMap putInt(@NotNull String key, int value) {
        $proxy$.put(key, Integer.toString(value));
        return this;
    }


    long getLongNullable(@NotNull String key, long defaultValue)
            throws UnmarshalException {
        return getNotNull(key, defaultValue, "long", Long::parseLong);
    }

    @NotNull
    PrimitiveMap putLong(@NotNull String key, long value) {
        $proxy$.put(key, Long.toString(value));
        return this;
    }

    boolean getBooleanNullable(@NotNull String key, boolean defaultValue)
            throws UnmarshalException {
        return getNotNull(key, defaultValue, "boolean", Boolean::parseBoolean);
    }

    @NotNull
    PrimitiveMap putBoolean(@NotNull String key, boolean value) {
        $proxy$.put(key, Boolean.toString(value));
        return this;
    }

    @Nullable
    String getStringNullable(String key, @Nullable String defaultValue)
            throws UnmarshalException {
        return getNullable(key, defaultValue, "string", (s) -> s);
    }

    @NotNull
    String getStringNotNull(String key)
            throws UnmarshalException {
        return getNotNull(key, "string", (s) -> s);
    }

    @NotNull
    PrimitiveMap putString(@NotNull String key, @Nullable String value) {
        if (value != null) {
            $proxy$.put(key, value);
        }
        return this;
    }

    @NotNull
    List<String> getStringList(String key)
            throws UnmarshalException {
        int size = getIntNullable(key + "__len", -1);
        if (size <= 0) {
            return Collections.emptyList();
        }
        List<String> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ret.add(getStringNotNull(key + "__" + i));
        }
        return ret;
    }

    @NotNull
    PrimitiveMap putStringList(@NotNull String key, @Nullable List<String> values) {
        if (values != null) {
            putInt(key + "__len", values.size());
            for (int i = 0; i < values.size(); i++) {
                putString(key + "__" + i, values.get(i));
            }
        }
        return this;
    }

    @NotNull
    PrimitiveMap putStringList(@NotNull String key, @Nullable Stream<String> values) {
        if (values != null) {
            putStringList(key, values.collect(Collectors.toList()));
        }
        return this;
    }

    boolean containsKey(String key) {
        return $proxy$.containsKey(key);
    }


    // Higher level getters and setters.  Still stores only the primitive data, but makes it easier to use.

    @Nullable
    FilePath getFilePathNullable(String key)
            throws UnmarshalException {
        String path = getStringNullable(key, null);
        if (path == null) {
            return null;
        }
        return VcsUtil.getFilePath(path);
    }

    @NotNull
    FilePath getFilePathNotNull(String key)
            throws UnmarshalException {
        String path = getStringNotNull(key);
        return VcsUtil.getFilePath(path);
    }

    @NotNull
    PrimitiveMap putFilePath(@NotNull String key, @Nullable FilePath path) {
        if (path != null) {
            return putString(key, path.getPath());
        }
        return this;
    }

    @NotNull
    List<FilePath> getFilePathList(@NotNull String key)
            throws UnmarshalException {
        return getStringList(key)
                .stream()
                .map(VcsUtil::getFilePath)
                .collect(Collectors.toList());
    }

    @NotNull
    PrimitiveMap putFilePathList(String key, @Nullable List<FilePath> paths) {
        if (paths != null) {
            return putStringList(key, paths.stream()
                .map(FilePath::getPath));
        }
        return this;
    }

    @Nullable
    ClientServerRef getClientServerRefNullable(@NotNull String key)
            throws UnmarshalException {
        String serverPort = getStringNullable(key + "__serverPort", null);
        String clientName = getStringNullable(key + "__clientName", null);
        if (serverPort == null) {
            return null;
        }
        P4ServerName server = P4ServerName.forPortNotNull(serverPort);
        return new ClientServerRef(server, clientName);
    }

    @NotNull
    ClientServerRef getClientServerRefNotNull(@NotNull String key)
            throws UnmarshalException {
        String serverPort = getStringNotNull(key + "__serverPort");
        String clientName = getStringNullable(key + "__clientName", null);
        P4ServerName server = P4ServerName.forPortNotNull(serverPort);
        return new ClientServerRef(server, clientName);
    }

    @NotNull
    PrimitiveMap putClientServerRef(@NotNull String key, @Nullable ClientServerRef ref) {
        if (ref == null) {
            return this;
        }
        return
                putString(key + "__serverPort", ref.getServerName().getFullPort())
                .putString(key + "__clientName", ref.getClientName());
    }

    @Nullable
    P4ChangelistId getChangelistIdNullable(@NotNull String key)
            throws UnmarshalException {
        ClientServerRef ref = getClientServerRefNullable(key + "__ref");
        if (ref == null) {
            return null;
        }
        int id = getIntNullable(key + "__id", -1);
        if (id == -1) {
            return null;
        }
        return new P4ChangelistIdImpl(id, ref);
    }

    @NotNull
    P4ChangelistId getChangelistIdNotNull(@NotNull String key)
            throws UnmarshalException {
        ClientServerRef ref = getClientServerRefNullable(key + "__ref");
        if (ref == null) {
            throw new UnmarshalException(key + "__ref__serverPort", "port");
        }
        int id = getIntNullable(key + "__id", -1);
        // Allow -1
        return new P4ChangelistIdImpl(id, ref);
    }

    @NotNull
    PrimitiveMap putChangelistId(@NotNull String key, @Nullable P4ChangelistId id) {
        if (id == null) {
            return this;
        }
        return putClientServerRef(key + "__ref", id.getClientServerRef())
                .putInt(key + "__id", id.getChangelistId());
    }

    @NotNull
    P4Job getP4Job(@NotNull String key)
            throws UnmarshalException {
        String jobId = getStringNotNull(key + "__id");
        String description = getStringNotNull(key + "__desc");
        List<String> keys = getStringList(key + "__detailskeys");
        Map<String, Object> details = new HashMap<>();

        for (String descKey : keys) {
            String type = getStringNotNull(key + "__detailstype__" + descKey);
            if ("string".equals(type)) {
                details.put(descKey, getStringNotNull(key + "__details__" + descKey));
            } else if ("list".equals(type)) {
                details.put(descKey, getStringList(key + "__details__" + descKey));
            } else if ("int".equals(type)) {
                details.put(descKey, getIntNullable(key + "__details__" + descKey, -1));
            } else if ("long".equals(type)) {
                // covers date type
                details.put(descKey, getLongNullable(key + "__details__" + descKey, -1));
            }
        }

        return new P4JobImpl(jobId, description, details);
    }

    @NotNull
    PrimitiveMap putP4Job(@NotNull String key, @Nullable P4Job job) {
        if (job == null) {
            return this;
        }
        // Need to convert the details to a primitive map.
        putStringList(key + "__detailskeys", job.getRawDetails().keySet().stream());
        for (Map.Entry<String, Object> entry : job.getRawDetails().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                putString(key + "__detailstype__" + entry.getKey(), "string");
                putString(key + "__details__" + entry.getKey(), (String) value);
            } else if (value instanceof List) {
                putString(key + "__detailstype__" + entry.getKey(), "list");
                //noinspection unchecked
                putStringList(key + "__details__" + entry.getKey(), (List) value);
            } else if (value instanceof Integer) {
                putString(key + "__detailstype__" + entry.getKey(), "int");
                putInt(key + "__details__" + entry.getKey(), (Integer) value);
            } else if (value instanceof Number) {
                putString(key + "__detailstype__" + entry.getKey(), "long");
                putLong(key + "__details__" + entry.getKey(), ((Number) value).longValue());
            } else if (value instanceof Date) {
                putString(key + "__detailstype__" + entry.getKey(), "long");
                putLong(key + "__details__" + entry.getKey(), ((Date) value).getTime());
            } else if (value != null) {
                LOG.warn("Unexpected value type in Perforce job details: " + value.getClass() +
                        " (value " + value + "; key " + entry.getKey() + ")");
            }
        }
        return
                putString(key + "__id", job.getJobId())
                .putString(key + "__desc", job.getDescription());
    }





    @Nullable
    private <T> T getNullable(@NotNull String key, @Nullable T defaultValue, @NotNull String type,
            @NotNull Function<String, T> map)
            throws UnmarshalException {
        if (!containsKey(key)) {
            return defaultValue;
        }
        String value = $proxy$.get(key);
        try {
            return map.apply(value);
        } catch (ClassCastException | NumberFormatException e) {
            // No need to log the exception, because we're just at the same level as
            // the low-level cast error source.
            throw new UnmarshalException(key, type, value);
        }
    }

    @NotNull
    private <T> T getNotNull(@NotNull String key, @NotNull String type, @NotNull Function<String, T> map)
            throws UnmarshalException {
        if (!containsKey(key)) {
            throw new UnmarshalException(key, type);
        }
        String value = $proxy$.get(key);
        if (value == null) {
            throw new UnmarshalException(key, type);
        }
        try {
            return map.apply(value);
        } catch (ClassCastException | NumberFormatException e) {
            // No need to log the exception, because we're just at the same level as
            // the low-level cast error source.
            throw new UnmarshalException(key, type, value);
        }
    }

    @NotNull
    private <T> T getNotNull(String key, @NotNull T defaultValue, @NotNull String type,
            @NotNull Function<String, T> map)
            throws UnmarshalException {
        if (!containsKey(key)) {
            return defaultValue;
        }
        return getNotNull(key, type, map);
    }
}
