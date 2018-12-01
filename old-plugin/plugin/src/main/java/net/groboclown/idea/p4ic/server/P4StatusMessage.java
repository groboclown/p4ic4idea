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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileOperationResult;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSubsystemCode;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class P4StatusMessage {
    private static final Logger LOG = Logger.getInstance(P4StatusMessage.class);
    public static final String P4MSG_NO_SUCH_FILE = " - no such file(s).";
    public static final String P4MSG_WRONG_CLIENT = " - must refer to client '";

    private final IFileOperationResult spec;

    public P4StatusMessage(IFileOperationResult spec) {
        this.spec = spec;
    }

    public boolean isError() {
        return spec.getOpStatus() != FileSpecOpStatus.VALID &&
                spec.getOpStatus() != FileSpecOpStatus.INFO;
    }

    public boolean isWarningOrError() {
        return spec.getOpStatus() != FileSpecOpStatus.VALID;
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

    public IServerMessage getMessage() {
        return spec.getStatusMessage();
    }

    public boolean isFileNotFoundError() {
        return isFileNotFoundError(spec);
    }

    @Nullable
    public FilePath getFilePath() {
        if (spec instanceof IFileSpec) {
            return FilePathUtil.getFilePath((IFileSpec) spec);
        }
        return null;
    }

    @Override
    public String toString() {
        return P4Bundle.message("status.message",
                getStringMessage(),
                spec.getGenericCode(),
                spec.getSubCode(),
                spec.getUniqueCode(),
                spec.getSeverityCode());
    }

    @NotNull
    private String getStringMessage() {
        IServerMessage msg = spec.getStatusMessage();
        String ret = msg.getLocalizedMessage();
        if (ret != null && ret.trim().length() > 0) {
            return ret;
        }
        ret = msg.getMessageFormat();
        if (ret == null || ret.trim().length() <= 0) {
            ret = msg.getCode();
            if (ret == null || ret.trim().length() <= 0) {
                ret = "(empty status message)";
            }
        }
        return ret;
    }


    public static boolean isValid(@NotNull IFileOperationResult spec) {
        return spec.getOpStatus() == FileSpecOpStatus.VALID;
    }


    public static boolean isErrorStatus(@NotNull IFileOperationResult spec) {
        return spec.getOpStatus() != FileSpecOpStatus.VALID &&
                spec.getOpStatus() != FileSpecOpStatus.INFO;
    }


    public static void throwIfError(@NotNull Collection<P4StatusMessage> msgs, boolean ignoreFileNotFoundErrors) throws VcsException {
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
        return messagesAsErrors(p4StatusMessages, false);
    }


    @NotNull
    public static List<? extends VcsException> messagesAsErrors(@Nullable List<P4StatusMessage> p4StatusMessages, boolean ignoreFileNotFoundErrors) {
        if (p4StatusMessages == null || p4StatusMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<VcsException> ret = new ArrayList<VcsException>(p4StatusMessages.size());
        for (P4StatusMessage msg: p4StatusMessages) {
            if (msg.isError() && msg.toString().length() > 0 && (!ignoreFileNotFoundErrors || !msg.isFileNotFoundError())) {
                ret.add(new VcsException(msg.toString()));
            }
        }
        return ret;
    }


    @NotNull
    public static <T extends IFileOperationResult> List<P4StatusMessage> getErrors(
            @Nullable Collection<T> specs) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation result: " + specs);
        }
        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();
        if (specs != null) {
            for (T spec : specs) {
                if (P4StatusMessage.isErrorStatus(spec)) {
                    ret.add(new P4StatusMessage(spec));
                } else if (spec.getOpStatus() == FileSpecOpStatus.INFO) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("result: [" + spec.getStatusMessage() + "] (" + spec + ")");
                    }
                }
            }
        }
        return ret;
    }


    public static <T extends IFileOperationResult> boolean hasErrors(
            @Nullable Collection<T> specs) {
        if (specs != null) {
            for (T spec : specs) {
                if (P4StatusMessage.isErrorStatus(spec)) {
                    return true;
                }
            }
        }
        return false;
    }


    @NotNull
    public static <T extends IFileOperationResult> List<P4StatusMessage> getErrorsAndWarnings(
            @Nullable Collection<T> specs) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation result: " + specs);
        }
        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();
        if (specs != null) {
            for (T spec : specs) {
                if (spec.getOpStatus() != FileSpecOpStatus.VALID) {
                    ret.add(new P4StatusMessage(spec));
                }
            }
        }
        return ret;
    }


    @NotNull
    public static List<? extends VcsException> getErrorsAndWarningsAsExceptions(
            @Nullable List<P4StatusMessage> p4StatusMessages, boolean ignoreFileNotFoundErrors) {
        if (p4StatusMessages == null || p4StatusMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<VcsException> ret = new ArrayList<VcsException>(p4StatusMessages.size());
        for (P4StatusMessage msg : p4StatusMessages) {
            if (msg.isWarningOrError() && msg.toString().length() > 0 &&
                    (!ignoreFileNotFoundErrors || !msg.isFileNotFoundError())) {
                ret.add(new VcsException(msg.toString()));
            }
        }
        return ret;
    }


    @NotNull
    public static <T extends IFileOperationResult> List<T> getNonErrors(@Nullable Collection<T> specs) {
        List<T> ret = new ArrayList<T>();
        if (specs != null) {
            for (T spec: specs) {
                if (spec != null &&
                        !P4StatusMessage.isErrorStatus(spec) && spec.getOpStatus() != FileSpecOpStatus.INFO) {
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
        throw new IllegalArgumentException(P4Bundle.message("error.not-error", msg));
    }

    public static boolean isFileNotFoundError(@NotNull IFileOperationResult spec) {
        // Check for "is not under root", which is something fstat
        // returns for files that haven't been added.
        if (spec.getGenericCode() == MessageGenericCode.EV_EMPTY) {
            LOG.info("reported as no such file: " + new P4StatusMessage(spec));
            return true;
        }

        // NOTE: this may be language specific.  Parts of the
        // client may be localized and return a different message.
        return spec.getStatusMessage() != null &&
                (spec.getStatusMessage().hasMessageFragment(P4MSG_NO_SUCH_FILE));
    }

    /**
     * There are some status messages that are this, but really the error is just
     * that the file doesn't exist.
     *
     * @param spec
     * @return
     */
    public static boolean isFileNotOnClientError(@NotNull IFileOperationResult spec) {
        if (spec.getGenericCode() == MessageGenericCode.EV_UNKNOWN &&
                spec.getSubCode() == 100) {
            return true;
        }

        return spec.getStatusMessage() != null &&
                spec.getStatusMessage().hasMessageFragment(P4MSG_WRONG_CLIENT);
    }
}
