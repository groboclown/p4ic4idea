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
    String getLocalizedMessage();

    String getMessageFormat();

    int getSeverity();

    int getSubSystem();

    int getGeneric();

    int getUniqueCode();

    int getSubCode();

    int getRawCode();

    /**
     * Checks if the given string fragment is in the message.
     *
     * @param fragment
     * @return true if the fragment is in the message.
     * @deprecated here until the string matching is eliminated
     */
    boolean hasMessageFragment(String fragment);
}
