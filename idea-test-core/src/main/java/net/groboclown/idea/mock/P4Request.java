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

package net.groboclown.idea.mock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

public final class P4Request {
    private final String cmd;
    private final String[] args;
    private final Map<String, ?> request;
    private final String inputData;

    public P4Request(@NotNull String cmd, @NotNull String[] args, @Nullable Map<String, ?> request,
            @Nullable String inputData) {
        this.cmd = cmd;
        this.args = args;
        this.request = request;
        this.inputData = inputData;
    }

    public P4Request(@NotNull String cmd, @NotNull String... args) {
        this.cmd = cmd;
        this.args = args;
        this.request = null;
        this.inputData = null;
    }

    @NotNull
    public String getCmd() {
        return cmd;
    }

    @NotNull
    public String[] getArgs() {
        return args;
    }

    @Nullable
    public Map<String, ?> getRequest() {
        return request;
    }

    @Nullable
    public String getInputData() {
        return inputData;
    }

    @Override
    public int hashCode() {
        int ret = cmd.hashCode() + Arrays.hashCode(args);
        if (request != null && ! request.isEmpty()) {
            ret += request.hashCode();
        }
        if (inputData != null && inputData.length() > 0) {
            ret += inputData.hashCode();
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (! (o.getClass().equals(P4Request.class))) {
            return false;
        }
        P4Request that = (P4Request) o;
        if (request == null || request.isEmpty()) {
            if (that.request != null && ! that.request.isEmpty()) {
                return false;
            }
        }
        if (inputData == null || inputData.length() <= 0) {
            if (that.inputData != null && that.inputData.length() > 0) {
                return false;
            }
        }
        return cmd.equals(that.cmd) &&
                Arrays.equals(args, that.args);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(cmd);
        for (String arg : args) {
            ret.append(' ').append(arg);
        }
        if (request != null) {
            ret.append(' ').append(request);
        }
        if (inputData != null) {
            ret.append(" [").append(inputData).append(']');
        }
        return ret.toString();
    }
}
