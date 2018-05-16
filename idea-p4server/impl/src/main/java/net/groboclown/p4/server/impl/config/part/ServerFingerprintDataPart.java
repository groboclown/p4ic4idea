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

package net.groboclown.p4.server.impl.config.part;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.part.ConfigPartAdapter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerFingerprintDataPart extends ConfigPartAdapter implements ConfigStateProvider {
    private String fingerprint;

    public ServerFingerprintDataPart(@Nls @NotNull String sourceName) {
        super(sourceName);
    }

    // for ConfigStateProvider
    public ServerFingerprintDataPart(@NotNull String sourceName, VirtualFile root,
            @NotNull Map<String, String> values) {
        super(sourceName);
        this.fingerprint = values.get("f");
    }

    @Override
    public boolean hasServerFingerprintSet() {
        return fingerprint != null;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return fingerprint;
    }

    public void setServerFingerprint(@Nullable String value) {
        if (value != null) {
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            }
        }
        this.fingerprint = value;
        reload();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! getClass().equals(o.getClass())) {
            return false;
        }
        ServerFingerprintDataPart that = (ServerFingerprintDataPart) o;
        return StringUtil.equals(that.getServerFingerprint(), getServerFingerprint());
    }

    @Override
    public int hashCode() {
        String h = getServerFingerprint();
        return h == null ? 0 : h.hashCode();
    }

    @Override
    public boolean reload() {
        return false;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Map<String, String> getState() {
        Map<String, String> ret = new HashMap<>();
        ret.put("f", fingerprint);
        return ret;
    }
}
