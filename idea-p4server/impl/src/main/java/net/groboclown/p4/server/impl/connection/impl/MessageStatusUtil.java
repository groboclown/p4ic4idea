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

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;

import java.util.Collection;

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
}
