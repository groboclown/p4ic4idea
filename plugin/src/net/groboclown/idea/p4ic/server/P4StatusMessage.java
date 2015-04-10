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
package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileOperationResult;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class P4StatusMessage {
    private static final Logger LOG = Logger.getInstance(P4StatusMessage.class);

    private final IFileOperationResult spec;

    public P4StatusMessage(IFileOperationResult spec) {
        this.spec = spec;
    }

    public boolean isError() {
        return spec.getOpStatus() != FileSpecOpStatus.VALID &&
                spec.getOpStatus() != FileSpecOpStatus.INFO;
    }

    public boolean isFileNotFoundError() {
        return isError() &&
                (getUniqueCode() == 6520);
    }

    public int getErrorCode() {
        return spec.getGenericCode();
    }

    public int getSubCode() {
        return spec.getSubCode();
    }

    public int getUniqueCode() {
        return spec.getUniqueCode();
    }

    public String getMessage() {
        return spec.getStatusMessage();
    }

    public boolean isNoSuchFilesMessage() {
        // Check for "is not under root", which is something fstat
        // returns for files that haven't been added.
        if (spec.getUniqueCode() == 4135) {
            LOG.info("reported as no such file: " + toString());
            return true;
        }

        // NOTE: this may be language specific.  Parts of the
        // client may be localized and return a different message.
        return spec.getStatusMessage() != null &&
                (spec.getStatusMessage().endsWith(" - no such file(s)."));
    }

    @Override
    public String toString() {
        return P4Bundle.message("status.message",
                spec.getStatusMessage(),
                spec.getGenericCode(),
                "." + spec.getSubCode(),
                "." + spec.getUniqueCode(),
                "." + spec.getRawCode(),
                "." + spec.getSeverityCode());
    }


    public static boolean isErrorStatus(IFileOperationResult spec) {
        return spec.getOpStatus() != FileSpecOpStatus.VALID &&
                spec.getOpStatus() != FileSpecOpStatus.INFO;
    }


    public static void throwIfError(Collection<P4StatusMessage> msgs, boolean ignoreFileNotFoundErrors) throws VcsException {
        List<String> errors = new ArrayList<String>(msgs.size());
        for (P4StatusMessage msg: msgs) {
            if (msg.isError() && (! ignoreFileNotFoundErrors || ! msg.isFileNotFoundError())) {
                errors.add(msg.toString());
            }
        }
        if (errors.size() == 1) {
            throw new VcsException(errors.get(0));
        }
        if (! errors.isEmpty()) {
            throw new VcsException(errors.toString());
        }
    }


    @NotNull
    public static List<? extends VcsException> messagesAsErrors(@Nullable List<P4StatusMessage> p4StatusMessages) {
        if (p4StatusMessages == null || p4StatusMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<VcsException> ret = new ArrayList<VcsException>(p4StatusMessages.size());
        for (P4StatusMessage msg: p4StatusMessages) {
            if (msg.isError() && msg.toString().length() > 0) {
                ret.add(new VcsException(msg.toString()));
            }
        }
        return ret;
    }


    @NotNull
    public static <T extends IFileOperationResult> List<P4StatusMessage> getErrors(
            @Nullable Collection<T> specs) {
        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();
        if (specs != null) {
            for (T spec : specs) {
                if (P4StatusMessage.isErrorStatus(spec)) {
                    ret.add(new P4StatusMessage(spec));
                } else if (spec.getOpStatus() == FileSpecOpStatus.INFO) {
                    LOG.info("result: " + spec.getStatusMessage());
                }
            }
        }
        return ret;
    }


    @NotNull
    public static <T extends IFileOperationResult> List<T> getNonErrors(@Nullable Collection<T> specs) {
        List<T> ret = new ArrayList<T>();
        if (specs != null) {
            for (T spec: specs) {
                if (!P4StatusMessage.isErrorStatus(spec)) {
                    ret.add(spec);
                }
            }
        }
        return ret;
    }

    @NotNull
    public static VcsException messageAsError(@NotNull final P4StatusMessage msg) {
        if (msg.isError()) {
            return new VcsException(msg.toString());
        }
        throw new IllegalArgumentException("not an error message: " + msg);
    }
}
