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

package net.groboclown.p4.server.impl.connection.impl;

import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class MessageStatusUtil {
    /**
     * If the messages contain an error, then throw an exception.
     */
    public static void throwIfError(Collection<IFileSpec> specs)
            throws RequestException {
        for (IFileSpec spec : specs) {
            IServerMessage msg = spec.getStatusMessage();
            if (msg != null && msg.isError()) {
                throw new RequestException(msg);
            }
        }
    }

    public static void throwIf(Collection<IFileSpec> specs, Predicate<IServerMessage> predicate)
            throws RequestException {
        for (IFileSpec spec : specs) {
            IServerMessage msg = spec.getStatusMessage();
            if (msg != null && predicate.test(msg)) {
                throw new RequestException(msg);
            }
        }
    }

    public static void throwIfMessageOrEmpty(String operation, List<? extends IFileSpec> ret)
            throws P4JavaException {
        if (ret.isEmpty()) {
            throw new P4JavaException("Unexpected error when performing " + operation +
                    " on file: no results from server");
        }
        if (ret.get(0).getOpStatus() != FileSpecOpStatus.VALID) {
            IServerMessage msg = ret.get(0).getStatusMessage();
            if (msg != null) {
                throw new RequestException(msg);
            }
            throw new P4JavaException("Unexpected error when performing " + operation +
                    " on file: " + ret.get(0));
        }
    }

    public static StringBuilder getMessages(StringBuilder sb, Collection<IFileSpec> specs, String separator) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        boolean first = true;
        for (IFileSpec spec : specs) {
            IServerMessage msg = spec.getStatusMessage();
            if (msg != null && (msg.isError() || msg.isInfo() || msg.isWarning())) {
                if (first) {
                    first = false;
                } else {
                    sb.append(separator);
                }
                sb.append(msg.getAllInfoStrings(separator));
            }
        }
        return sb;
    }

    public static String getMessages(Collection<IFileSpec> specs, String separator) {
        return getMessages(null, specs, separator).toString();
    }

    public static String getExtendedMessages(Collection<IExtendedFileSpec> specs, String separator) {
        return getMessages(null, new ArrayList<>(specs), separator).toString();
    }
}
