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

package com.perforce.p4java.impl.mapbased.rpc.msg;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.MessageSubsystemCode;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.ISingleServerMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.exception.MessageSeverityCode.E_INFO;
import static com.perforce.p4java.exception.MessageSeverityCode.E_WARN;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class ServerMessage implements IServerMessage {
    private final ISingleServerMessage[] messages;
    private final ISingleServerMessage highestSeverity;
    private final String str;

    public ServerMessage(final @Nonnull Iterable<ISingleServerMessage> messages) {
        this(asList(messages));
    }

    public ServerMessage(final @Nonnull List<ISingleServerMessage> messages) {
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        this.messages = messages.toArray(new ISingleServerMessage[0]);

        ISingleServerMessage highest = this.messages[0];
        StringBuilder sb = new StringBuilder();
        for (ISingleServerMessage message : this.messages) {
            sb.append(message.getLocalizedMessage()).append('\n');
            if (highest.getSeverity() < message.getSeverity()) {
                highest = message;
            }
        }
        str = sb.toString().trim();
        highestSeverity = highest;
    }

    @Nonnull
    @Override
    public Iterable<ISingleServerMessage> getAllMessages() {
        return Arrays.asList(messages);
    }

    @Nonnull
    @Override
    public Iterable<ISingleServerMessage> getForSeverity(final int minimum) {
        return getForSeverity(minimum, false);
    }

    @Nonnull
    @Override
    public Iterable<ISingleServerMessage> getForExactSeverity(final int minimum) {
        return getForSeverity(minimum, true);
    }

    private Iterable<ISingleServerMessage> getForSeverity(final int minimum, final boolean isExact) {
        return () -> new Iterator<ISingleServerMessage>() {
            int next = -1;
            @Override
            public boolean hasNext() {
                if (next < 0) {
                    advance();
                }
                return next < messages.length;
            }

            @Override
            public ISingleServerMessage next() {
                if (next < 0) {
                    advance();
                }
                if (next >= messages.length) {
                    throw new NoSuchElementException();
                }
                int pos = next;
                advance();
                return messages[pos];
            }

            void advance() {
                while (++next < messages.length) {
                    final int severity = messages[next].getSeverity();
                    if ((isExact && severity == minimum) || (!isExact && severity >= minimum)) {
                        break;
                    }
                }
            }
        };
    }

    @Override
    public boolean hasSeverity(int minimum) {
        return highestSeverity != null && highestSeverity.getSeverity() >= minimum;
    }

    public boolean isExactSeverity(int value) {
        return highestSeverity != null && highestSeverity.getSeverity() == value;
    }

    @Override
    public boolean isInfoOrError() {
        return hasSeverity(E_INFO);
    }

    @Override
    public boolean isInfo() {
        return isExactSeverity(E_INFO);
    }

    @Override
    public boolean isWarning() {
        return isExactSeverity(E_WARN);
    }

    @Override
    public boolean isError() {
        return hasSeverity(E_FAILED);
    }

    @Nonnull
    @Override
    public String getFirstInfoString() {
        for (ISingleServerMessage message : messages) {
            if (message.getSeverity() == E_INFO) {
                return message.getLocalizedMessage();
            }
        }
        return EMPTY;
    }

    @Nonnull
    @Override
    public String getAllInfoStrings() {
        return getAllInfoStrings("\n");
    }

    @Nonnull
    @Override
    public String getAllInfoStrings(@Nonnull String separator) {
        StringBuilder ret = new StringBuilder(100);
        for (ISingleServerMessage message : messages) {
            if (message.getSeverity() == E_INFO) {
                if (ret.length() > 0) {
                    ret.append(separator);
                }
                ret.append(message.getLocalizedMessage());
            }
        }
        return ret.toString();
    }

    @Override
    public byte[] getBytes(final String charsetName) throws UnsupportedEncodingException {
        String msg = toString() + CommandEnv.LINE_SEPARATOR;
        return msg.getBytes(charsetName);
    }

    @Nullable
    @Override
    public String getErrorOrInfoStr() {
        return str;
    }

    @Override
    public String getLocalizedMessage() {
        return highestSeverity.getLocalizedMessage();
    }

    @Override
    public String getMessageFormat() {
        return highestSeverity.getMessageFormat();
    }

    @Override
    public int getSeverity() {
        return highestSeverity.getSeverity();
    }

    @Override
    public int getSubSystem() {
        return highestSeverity.getSubSystem();
    }

    @Override
    public int getGeneric() {
        return highestSeverity.getGeneric();
    }

    @Override
    public int getUniqueCode() {
        return highestSeverity.getUniqueCode();
    }

    @Override
    public int getSubCode() {
        return highestSeverity.getSubCode();
    }

    @Override
    public int getRawCode() {
        return highestSeverity.getRawCode();
    }

    @Override
    public String getCode() {
        return highestSeverity.getCode();
    }

    /**
     *
     * @param fragment string to check against the messages
     * @return true if the fragment is in one of the messages
     * @deprecated
     */
    @Override
    public boolean hasMessageFragment(final String fragment) {
        for (ISingleServerMessage msg: messages) {
            if (msg.hasMessageFragment(fragment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return str;
    }


    public static class SingleServerMessage implements ISingleServerMessage {
        private final String localized;
        private final String format;
        private final int severity;
        private final int subSystem;
        private final int generic;
        private final int rawCode;
        private final int uniqueCode;
        private final int subCode;

        public SingleServerMessage(String code, int index, Map<String, Object> map) {
            int rawNum;
            int tSeverity;
            int tGeneric;
            int tSubSystem;
            int tSubCode;
            try {
                // See RequestException.setCodes for the decoding
                // RpcMessage includes details on the argument count.

                rawNum = new Integer(code);    // Really need an unsigned here...
                tSeverity = ((rawNum >> 28) & 0x0f);
                tGeneric = ((rawNum >> 16) & 0x0FF);
                tSubSystem = ((rawNum >> 10) & 0x3F);
                tSubCode = ((rawNum >> 0) & 0x3ff);
            } catch (Exception exc) {
                // If there's a conversion error, just let it return below
                Log.exception(exc);
                rawNum = 0;
                tSeverity = MessageSeverityCode.E_EMPTY;
                tGeneric = MessageGenericCode.EV_NONE;
                tSubSystem = MessageSubsystemCode.ES_CLIENT;
                tSubCode = 0;
            }
            rawCode = rawNum;
            uniqueCode = rawNum & 0xffff;
            severity = tSeverity;
            subSystem = tSubSystem;
            generic = tGeneric;
            subCode = tSubCode;

            format = (String) map.get(RpcMessage.FMT + index);
            localized = RpcMessage.interpolateArgs(format, map);
        }

        /** @deprecated only useful in one place */
        public SingleServerMessage(String message) {
            localized = format = message;
            severity = MessageSeverityCode.E_INFO;
            generic = MessageGenericCode.EV_NONE;
            subSystem = MessageSubsystemCode.ES_CLIENT;
            subCode = 0;
            uniqueCode = rawCode = (severity << 28) | (generic << 16) | (subSystem << 10);
        }

        @Override
        public String getLocalizedMessage() {
            return localized;
        }

        @Override
        public String getMessageFormat() {
            return format;
        }

        @Override
        public int getSeverity() {
            return severity;
        }

        @Override
        public int getSubSystem() {
            return subSystem;
        }

        @Override
        public int getGeneric() {
            return generic;
        }

        @Override
        public int getUniqueCode() {
            return uniqueCode;
        }

        @Override
        public int getSubCode() {
            return subCode;
        }

        @Override
        public int getRawCode() {
            return rawCode;
        }

        @Override
        public String getCode() {
            return getGeneric() + ":" +
                    getSubSystem() + ":" +
                    getSubCode() + " (" + getUniqueCode() + ")";
        }

        @Override
        public boolean hasMessageFragment(final String fragment) {
            if (fragment == null) {
                return false;
            }
            return (nonNull(format) && format.toLowerCase().contains(fragment.toLowerCase()))
                    || (nonNull(localized) && localized.toLowerCase().contains(fragment.toLowerCase()));
        }

        @Override
        public String toString() {
            return getLocalizedMessage();
        }
    }

    private static <T> List<T> asList(Iterable<T> iter) {
        List<T> ret = new ArrayList<>();
        for (T t : iter) {
            ret.add(t);
        }
        return ret;
    }
}
