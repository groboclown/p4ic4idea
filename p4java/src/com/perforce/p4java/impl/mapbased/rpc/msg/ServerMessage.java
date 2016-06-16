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

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ServerMessage implements IServerMessage {
    private final ISingleServerMessage[] messages;
    private final ISingleServerMessage highestSeverity;
    private final Map<String, String> argMap;
    private final String str;

    public ServerMessage(final List<ISingleServerMessage> messages, final Map<String, Object> argMap) {
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        this.messages = messages.toArray(new ISingleServerMessage[messages.size()]);

        ISingleServerMessage highest = this.messages[0];
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < this.messages.length; i++) {
            sb.append(this.messages[i]).append('\n');
            if (highest.getSeverity() < this.messages[i].getSeverity()) {
                highest = this.messages[i];
            }
        }
        str = sb.toString().trim();
        highestSeverity = highest;

        Map<String, String> args = new HashMap<String, String>();
        for (Entry<String, Object> entry : argMap.entrySet()) {
            if (entry.getValue() == null) {
                args.put(entry.getKey(), null);
            } else {
                args.put(entry.getKey(), entry.getValue().toString());
            }
        }
        this.argMap = Collections.unmodifiableMap(args);
    }

    @Override
    public ISingleServerMessage[] getAllMessages() {
        return messages;
    }

    @Override
    public Map<String, String> getNamedArguments() {
        return argMap;
    }

    @Override
    public byte[] getBytes(final String charsetName) throws UnsupportedEncodingException {
        String msg = toString() + CommandEnv.LINE_SEPARATOR;
        return msg.getBytes(charsetName);
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
            if (fragment == null || format == null) {
                return false;
            }
            return format.toLowerCase().contains(fragment.toLowerCase());
        }

        @Override
        public String toString() {
            return getLocalizedMessage();
        }
    }
}
