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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.perforce.p4java.env.PerforceEnvironment;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.ConfigPropertiesUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EnvCompositePart extends CompositePart {
    static final String TAG_NAME = "env-composite-part";
    static final ConfigPartFactory<EnvCompositePart> FACTORY = new Factory();
    private static final Logger LOG = Logger.getInstance(EnvCompositePart.class);


    private final Project project;

    @NotNull
    private List<ConfigPart> parts;

    private final List<Exception> generatedProblems = new ArrayList<Exception>();

    public EnvCompositePart(@NotNull Project project) {
        this.project = project;
        this.parts = new ArrayList<ConfigPart>();
        loadEnvironmentParts();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean reload() {
        loadEnvironmentParts();

        // generatedProblems are not thrown here.  ENV loading should not cause exceptions,
        // but instead report problems.

        return hasError();
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        List<ConfigProblem> ret = new ArrayList<ConfigProblem>();
        for (Exception generatedProblem : generatedProblems) {
            ret.add(new ConfigProblem(this, generatedProblem));
        }
        for (ConfigPart part : parts) {
            ret.addAll(part.getConfigProblems());
        }
        return ret;
    }

    @NotNull
    @Override
    public List<ConfigPart> getConfigParts() {
        return Collections.unmodifiableList(parts);
    }

    private void loadEnvironmentParts() {
        generatedProblems.clear();
        parts = new ArrayList<ConfigPart>();

        String p4config = null;
        String p4enviro = null;

        if (WinRegDataPart.isAvailable()) {
            WinRegDataPart userReg = new WinRegDataPart(true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded user windows registry: " + ConfigPropertiesUtil.toProperties(userReg));
            }
            // p4config is always null at this point.
            // if (p4config == null) {
                p4config = userReg.getP4ConfigFile();
            // }
            // if (p4enviro == null) {
                p4enviro = userReg.getP4EnviroFile();
            // }
            WinRegDataPart sysReg = new WinRegDataPart(false);
            if (p4config == null) {
                p4config = sysReg.getP4ConfigFile();
            }
            if (p4enviro == null) {
                p4enviro = sysReg.getP4EnviroFile();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded system windows registry: " + ConfigPropertiesUtil.toProperties(sysReg));
            }
            parts.add(userReg);
            parts.add(sysReg);
        }

        parts.add(new EnvPassword());

        SimpleDataPart envData = new SimpleDataPart(project, (Map<String, String>) null);
        envData.setServerName(PerforceEnvironment.getP4Port());
        envData.setUsername(PerforceEnvironment.getP4User());
        envData.setClientname(PerforceEnvironment.getP4Client());
        envData.setClientHostname(PerforceEnvironment.getP4Host());
        envData.setDefaultCharset(PerforceEnvironment.getP4Charset());
        envData.setAuthTicketFile(PerforceEnvironment.getP4Tickets());
        envData.setTrustTicketFile(PerforceEnvironment.getP4Trust());
        envData.setIgnoreFilename(PerforceEnvironment.getP4Ignore());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loaded environment variables: " + ConfigPropertiesUtil.toProperties(envData));
        }

        if (p4config == null) {
            p4config = PerforceEnvironment.getP4Config();
        }
        if (p4enviro == null) {
            p4enviro = PerforceEnvironment.getP4Enviro();
        }

        // P4CONFIG loading
        {
            if (p4config != null) {
                if (p4config.indexOf('/') >= 0 || p4config.indexOf('\\') >= 0 || p4config.indexOf(File.separatorChar) >= 0) {
                    // File location
                    File f = new File(p4config);
                    if (!f.isAbsolute()) {
                        f = new File(new File(project.getBaseDir().getPath()), p4config);
                    }

                    final FileDataPart envConf = new FileDataPart(project, f);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Env defined absolute P4CONFIG: " + f + ": " +
                                ConfigPropertiesUtil.toProperties(envConf));
                    }
                    parts.add(envConf);
                } else {
                    final RelativeConfigCompositePart envConf = new RelativeConfigCompositePart(project, p4config);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Env defined relative P4CONFIG: " + p4config);
                    }
                    parts.add(envConf);
                }
            }
        }

        // P4ENVIRO loading
        {
            if (p4enviro != null) {
                File f = new File(p4enviro);
                if (!f.isAbsolute()) {
                    f = new File(new File(project.getBaseDir().getPath()), p4enviro);
                }
                // The P4ENVIRO will have a default value if the user didn't specify one.
                // Therefore, if the file doesn't exist, don't complain.
                if (f.exists()) {
                    final FileDataPart envConf = new FileDataPart(project, f);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Env defined P4ENVIRO: " + f + ": " +
                                ConfigPropertiesUtil.toProperties(envConf));
                    }
                    parts.add(envConf);
                }
            }
        }
    }

    @NotNull
    @Override
    public Element marshal() {
        return new Element(TAG_NAME);
    }

    private static class Factory extends ConfigPartFactory<EnvCompositePart> {
        @Override
        EnvCompositePart create(@NotNull Project project, @NotNull Element element) {
            return new EnvCompositePart(project);
        }
    }


    private static class EnvPassword extends DataPartAdapter {
        @Override
        public boolean hasPasswordSet() {
            // Note: not trimmed, not empty string checked.
            return PerforceEnvironment.getP4Passwd() != null;
        }

        @Nullable
        @Override
        public String getPlaintextPassword() {
            return PerforceEnvironment.getP4Passwd();
        }

        @NotNull
        @Override
        public Element marshal() {
            throw new IllegalStateException("should never be called");
        }

        @Override
        public boolean reload() {
            return true;
        }

        @NotNull
        @Override
        public Collection<ConfigProblem> getConfigProblems() {
            return Collections.emptyList();
        }
    }
}
