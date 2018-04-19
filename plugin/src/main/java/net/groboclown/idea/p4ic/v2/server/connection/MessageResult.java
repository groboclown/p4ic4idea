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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.IFileOperationResult;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.MessageSeverityCode;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MessageResult<T> {
    private static final Logger LOG = Logger.getInstance(MessageResult.class);


    private final T result;
    private final List<P4StatusMessage> messages;
    private final boolean error;

    public static <T> MessageResult<List<T>> emptyList() {
        return new MessageResult<List<T>>(new ArrayList<T>(), new ArrayList<P4StatusMessage>());
    }


    public static <T extends IFileOperationResult> MessageResult<List<T>> create(@NotNull List<T> specs) {
        return update(MessageResult.<T>emptyList(), specs);
    }


    public static <T extends IFileOperationResult> MessageResult<List<T>> create(
            @NotNull List<T> specs, boolean markFileNotFoundAsValid) {
        return update(MessageResult.<T>emptyList(), specs, markFileNotFoundAsValid);
    }


    @NotNull
    public static MessageResult<List<FilePath>> createForFilePath(
            @NotNull List<FilePath> originalFiles,
            @NotNull List<IFileSpec> specs,
            boolean markFileNotFoundAsValid) {
        return updateForFilePath(
                new MessageResult<List<FilePath>>(
                    new ArrayList<FilePath>(), new ArrayList<P4StatusMessage>()),
                originalFiles, specs, markFileNotFoundAsValid);
    }


    public static <T extends IFileOperationResult> MessageResult<List<T>> update(
            @NotNull MessageResult<List<T>> base,
            @NotNull List<T> specs) {
        List<T> results = new ArrayList<T>(base.result);
        List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>(base.messages);
        for (T spec : specs) {
            if (P4StatusMessage.isValid(spec)) {
                results.add(spec);
            } else {
                final P4StatusMessage msg = new P4StatusMessage(spec);
                messages.add(msg);
            }
        }
        return new MessageResult<List<T>>(results, messages);
    }


    public static <T extends IFileOperationResult> MessageResult<List<T>> update(
            @NotNull MessageResult<List<T>> base,
            @NotNull List<T> specs, boolean markFileNotFoundAsValid) {
        List<T> results = new ArrayList<T>(base.result);
        List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>(base.messages);
        for (T spec : specs) {
            if (P4StatusMessage.isValid(spec)) {
                results.add(spec);
            } else if (markFileNotFoundAsValid && P4StatusMessage.isFileNotFoundError(spec)) {
                results.add(spec);
            } else {
                final P4StatusMessage msg = new P4StatusMessage(spec);
                messages.add(msg);
            }
        }
        return new MessageResult<List<T>>(results, messages);
    }





    @NotNull
    public static MessageResult<List<FilePath>> updateForFilePathMessages(
            @NotNull MessageResult<List<FilePath>> base,
            @NotNull List<FilePath> originalFiles,
            @NotNull List<P4StatusMessage> newMessages) {
        List<FilePath> results = new ArrayList<FilePath>(base.result);
        results.addAll(originalFiles);
        List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>(base.messages);
        messages.addAll(newMessages);
        return new MessageResult<List<FilePath>>(results, messages);
    }


    @NotNull
    public static MessageResult<List<FilePath>> updateForFilePath(
            @NotNull MessageResult<List<FilePath>> base,
            @NotNull List<FilePath> originalFiles,
            @NotNull List<IFileSpec> specs,
            boolean markFileNotFoundAsValid) {
        List<FilePath> results = new ArrayList<FilePath>(base.result);
        List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>(base.messages);
        Iterator<FilePath> iter = originalFiles.iterator();
        for (IFileSpec spec : specs) {
            if (P4StatusMessage.isValid(spec) ||
                    (markFileNotFoundAsValid && P4StatusMessage.isFileNotFoundError(spec))) {
                if (!iter.hasNext()) {
                    // This can happen when reverting both sides of the
                    // move operation at the same time.
                    // TODO figure out how to handle this situation better.
                    LOG.warn("No direct mapping between files " + originalFiles +
                        " to messages " + specs);
                } else {
                    results.add(iter.next());
                }
            } else {
                // Bug #121 - need to check if the iterator has a next.
                if (spec.getSeverityCode() != MessageSeverityCode.E_FATAL && iter.hasNext()) {
                    // advance but don't use the file reference
                    iter.next();
                }
                final P4StatusMessage msg = new P4StatusMessage(spec);
                messages.add(msg);
            }
        }
        return new MessageResult<List<FilePath>>(results, messages);
    }


    public MessageResult(final T result, final List<P4StatusMessage> messages) {
        this.result = result;
        this.messages = Collections.unmodifiableList(messages);
        boolean err = false;
        for (P4StatusMessage message : messages) {
            if (message == null || message.isError()) {
                err = true;
            }
        }
        error = err;
    }

    public T getResult() {
        return result;
    }

    @NotNull
    public List<P4StatusMessage> getMessages() {
        return messages;
    }

    public boolean isError() {
        return error;
    }

    @NotNull
    public Collection<? extends VcsException> messagesAsExceptions() {
        return P4StatusMessage.messagesAsErrors(getMessages());
    }

    @NotNull
    @Override
    public String toString() {
        if (isError()) {
            return getMessages().toString();
        }
        return getResult().toString();
    }

}
