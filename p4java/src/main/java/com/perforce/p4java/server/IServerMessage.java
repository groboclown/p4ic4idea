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

package com.perforce.p4java.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Represents a message from the server, along with the localized version of it.
 * The message can contain multiple problems.  The top level message represents
 * the highest severity message.
 * <p>
 * To create this, see {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#toServerMessage(Map)}
 */
public interface IServerMessage extends ISingleServerMessage {
    @Nonnull
    Iterable<ISingleServerMessage> getAllMessages();

    /**
     * @return the list of messages that had at least the minimum severity.
     */
    @Nonnull
    Iterable<ISingleServerMessage> getForSeverity(int minimum);

    /**
     * @return the list of messages that had at least the minimum severity.
     */
    @Nonnull
    Iterable<ISingleServerMessage> getForExactSeverity(int minimum);

    boolean hasSeverity(int minimum);

    /**
     * @return true if the message is an informative message (info, error, fatal, etc.)
     */
    boolean isInfoOrError();

    boolean isInfo();

    boolean isWarning();

    boolean isError();

    @Nonnull
    String getFirstInfoString();

    @Nonnull
    String getAllInfoStrings();

    @Nonnull
    String getAllInfoStrings(@Nonnull String separator);

    byte[] getBytes(String charsetName) throws UnsupportedEncodingException;

    /**
     * Returns the raw string that the server call to getErrorOrInfoStr originally returned.
     * This is for writing data back to the RPC output stream.
     *
     * @return raw info string
     */
    @Nullable
    String getErrorOrInfoStr();
}
