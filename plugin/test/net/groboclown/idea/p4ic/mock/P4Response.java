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

package net.groboclown.idea.p4ic.mock;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.callback.IFilterCallback;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class P4Response {
    private final List<Map<String, Object>> values;
    private final String responseStream;
    private final P4JavaException exception;


    public static Map<String, Object> values(String... vals) {
        assertThat("Not an even number of values", vals.length % 2, is(0));
        Map<String, Object> ret = new TreeMap<String, Object>();
        int i = 0;
        while (i < vals.length) {
            String key = vals[i++];
            String val = vals[i++];
            ret.put(key, val);
        }
        return Collections.unmodifiableMap(ret);
    }

    /**
     *
     * @return ServerInfo compatible response
     * @see com.perforce.p4java.impl.mapbased.server.ServerInfo#setFromMap(Map)
     */
    @NotNull
    public static P4Response serverInfo(@NotNull ServerConfig config, boolean caseSensitive, boolean unicode) {
        return new P4Response(values(
                "userName", config.getUsername(),
                "serverAddress", config.getPort(),
                "serverEncryption", "unencrypted", // "encrypted" is the key

                // IMPORTANT
                "serverVersion", "P4D/NTX64/2014.1/886167 (2014/06/25)",
                "caseHandling", caseSensitive ? "sensitive" : "insensitive",
                "unicode", unicode ? "enabled" : "disabled"
        ));
    }

    /**
     * @return Client compatible response
     * @see com.perforce.p4java.impl.mapbased.client.Client#Client(IServer, Map)
     * @see com.perforce.p4java.impl.mapbased.client.ClientSummary#ClientSummary(Map, boolean)
     */
    @NotNull
    public static P4Response client(@NotNull String clientName, @NotNull FilePath rootDir, @Nullable String stream) {
        return new P4Response(values(
                "Client", clientName,
                "client", clientName,
                "Root", rootDir.getIOFile().getAbsolutePath(),
                "View0", "//... //...",
                "Stream", stream,

                // IMPORTANT
                "Access", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())
        ));
    }


    public P4Response(@NotNull Map<String, Object>... values) {
        this(Arrays.asList(values));
    }

    public P4Response(@NotNull List<Map<String, Object>> values) {
        this.values = Collections.unmodifiableList(values);
        this.responseStream = null;
        this.exception = null;
    }

    public P4Response(@NotNull String responseStream) {
        this.values = null;
        this.responseStream = responseStream;
        this.exception = null;
    }

    public P4Response(@NotNull P4JavaException exception) {
        this.values = null;
        this.responseStream = null;
        this.exception = exception;
    }


    @NotNull
    public List<Map<String, Object>> getList() throws P4JavaException {
        if (exception != null) {
            throw exception;
        }
        assertThat(values, not(nullValue()));
        return new ArrayList<Map<String, Object>>(values);
    }

    @NotNull
    public Map<String, Object>[] getArray() throws P4JavaException {
        if (exception != null) {
            throw exception;
        }
        assertThat(values, not(nullValue()));
        return values.toArray(new Map[values.size()]);
    }

    @NotNull
    public List<Map<String, Object>> getList(IFilterCallback filter) throws P4JavaException {
        if (exception != null) {
            throw exception;
        }
        assertThat(values, not(nullValue()));
        if (filter == null) {
            return values;
        }
        final ArrayList<Map<String, Object>> ret = new ArrayList<Map<String, Object>>(values.size());
        AtomicBoolean skipping = new AtomicBoolean(false);
        Set<String> dontSkip = null;
        for (Map<String, Object> map: values) {
            Map<String, Object> retMap = new HashMap<String, Object>();
            ret.add(retMap);
            filter.reset();
            for (Entry<String, Object> entry : map.entrySet()) {
                if (skipping.get()) {
                    if (dontSkip != null && dontSkip.contains(entry.getKey())) {
                        retMap.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    final boolean skip = filter.skip(entry.getKey(), entry.getValue(), skipping);
                    if (! skip) {
                        retMap.put(entry.getKey(), entry.getValue());
                    }
                    if (skipping.get()) {
                        final Map<String, String> doNotSkip = filter.getDoNotSkipKeysMap();
                        if (doNotSkip != null) {
                            dontSkip = doNotSkip.keySet();
                        }
                    }
                }
            }
        }
        return ret;
    }

    @NotNull
    public Map<String, Object>[] getArray(IFilterCallback filter) throws P4JavaException {
        final List<Map<String, Object>> ret = getList(filter);
        return ret.toArray(new Map[ret.size()]);
    }

    @Nullable
    public InputStream getResponse() throws P4JavaException {
        if (exception != null) {
            throw exception;
        }
        assertThat(responseStream, not(nullValue()));
        try {
            return new ByteArrayInputStream(responseStream.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new P4JavaException(e);
        }
    }
}
