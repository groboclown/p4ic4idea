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

package net.groboclown.p4.server.config.part;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import net.groboclown.p4.server.config.ConfigProblem;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class ServerFingerprintDataPart extends DataPartAdapter {
    public static final String TAG_NAME = "server-fingerprint-data-part";
    static final ConfigPartFactory<ServerFingerprintDataPart> FACTORY = new Factory();
    private static final String FINGERPRINT_ATTRIBUTE_NAME = "fingerprint";

    private String fingerprint;

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

    @NotNull
    @Override
    public Element marshal() {
        Element ret = new Element(TAG_NAME);
        if (getServerFingerprint() != null) {
            ret.setAttribute(FINGERPRINT_ATTRIBUTE_NAME, getServerFingerprint());
        }
        return ret;
    }

    private static class Factory
            extends ConfigPartFactory<ServerFingerprintDataPart> {

        @Override
        ServerFingerprintDataPart create(@NotNull Project project, @NotNull Element element) {
            ServerFingerprintDataPart ret = new ServerFingerprintDataPart();
            if (isTag(TAG_NAME, element)) {
                final Attribute attr = element.getAttribute(FINGERPRINT_ATTRIBUTE_NAME);
                if (attr != null) {
                    ret.setServerFingerprint(attr.getValue());
                }
            }
            return ret;
        }
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
}
