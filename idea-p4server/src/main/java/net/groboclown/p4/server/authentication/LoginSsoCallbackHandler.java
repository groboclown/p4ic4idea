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

package net.groboclown.p4.server.authentication;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.perforce.p4java.server.callback.ISSOCallback;
import net.groboclown.p4.server.config.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;

/**
 * Invokes the LOGINSSO command to extract the correct information from the user.
 */
public class LoginSsoCallbackHandler implements ISSOCallback {
    private static final Logger LOG = Logger.getInstance(LoginSsoCallbackHandler.class);

    private final String loginSsoCmd;

    public LoginSsoCallbackHandler(@NotNull String loginSsoCmd) {
        this.loginSsoCmd = loginSsoCmd;
    }

    @Override
    public Status getSSOCredentials(StringBuffer credBuffer, String ssoKey, String userName) {
        try {
            GeneralCommandLine cmd = createCommandLine();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Running login sso command `" + cmd.getCommandLineString() + "`");
            }
            ProcessOutput output = (new CapturingProcessHandler(cmd)).runProcess(
                    UserProjectPreferences.getLockWaitTimeoutMillis(null)
            );
            if (output.getExitCode() != 0) {
                LOG.info("failed with exit code " + output.getExitCode());
                LOG.info("stdout: " + output.getStdout());
                LOG.info("stderr: " + output.getStderr());
                final ExecutionException ex = new ExecutionException("Exit code " + output.getExitCode());
                AlertManager.getInstance().addCriticalError(
                        new LoginSsoExecFailedHandler(
                                loginSsoCmd, output.getStdout(), output.getStderr(), ex),
                        ex
                );
                return Status.FAIL;
            }
            credBuffer.append(output.getStdout());
            return Status.PASS;
        } catch (ExecutionException e) {
            AlertManager.getInstance().addCriticalError(
                    new LoginSsoExecFailedHandler(loginSsoCmd, null, null, e),
                    e
            );
            return Status.FAIL;
        }
    }

    @NotNull
    private GeneralCommandLine createCommandLine() {
        GeneralCommandLine ret = new GeneralCommandLine();
        if (SystemInfo.isWindows) {
            ret.setExePath(ExecUtil.getWindowsShellName());
            ret.addParameters("/c",
                    GeneralCommandLine.inescapableQuote(loginSsoCmd));
        } else if (SystemInfo.isMac) {
            ret.setExePath(ExecUtil.getOpenCommandPath());
            ret.addParameters("-a", loginSsoCmd);
        } else {
            ret.setExePath("/bin/sh");
            ret.addParameters("-c", loginSsoCmd);
        }
        return ret;
    }
}
