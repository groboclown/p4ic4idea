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

/**
 * Represents a message from the server, along with the localized version of it.
 */
public interface ISingleServerMessage {
    /**
     * Localized message with argument replacements.
     *
     * @return localized message
     */
    String getLocalizedMessage();

    /**
     * The localized message format text, without argument replacements.
     *
     * @return localized format for the message text.
     */
    String getMessageFormat();

    /**
     *
     * @return the documented severity level of the message.
     * @see com.perforce.p4java.exception.MessageSeverityCode
     */
    int getSeverity();

    /**
     *
     * @return subsystem message code
     * @see com.perforce.p4java.exception.MessageSubsystemCode
     */
    int getSubSystem();

    /**
     *
     * @return the documented generic code for the message.
     * @see com.perforce.p4java.exception.MessageGenericCode
     */
    int getGeneric();

    /**
     * The unique code for the message.  Nearly the same as the raw code.
     *
     * @return the unique code
     */
    int getUniqueCode();

    /**
     *
     * @return the sub-code
     * @see IServerMessageCode
     */
    int getSubCode();

    /**
     *
     * @return the raw, unparsed code.
     */
    int getRawCode();

    /**
     * Checks if the given string fragment is in the message.
     *
     * @param fragment
     * @return true if the fragment is in the message.
     * @deprecated here until the string matching is eliminated
     */
    boolean hasMessageFragment(String fragment);

    /**
     *
     * @return string version of the status code
     */
    String getCode();
}
