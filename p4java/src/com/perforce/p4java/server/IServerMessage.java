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

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Represents a message from the server, along with the localized version of it.
 * The message can contain multiple problems.  The top level message represents
 * the highest severity message.
 */
public interface IServerMessage extends ISingleServerMessage {
    ISingleServerMessage[] getAllMessages();

    Map<String, String> getNamedArguments();

    byte[] getBytes(String charsetName) throws UnsupportedEncodingException;
}
