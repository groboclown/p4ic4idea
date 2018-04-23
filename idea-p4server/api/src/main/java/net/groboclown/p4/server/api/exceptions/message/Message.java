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

package net.groboclown.p4.server.api.exceptions.message;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a message to show to the user when a problem occurs that requires the user to know about it.
 */
public class Message {
    private final String title;
    private final String message;

    public Message(
            @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title,
            @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message) {
        this.title = title;
        this.message = message;
    }

    @NotNull
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getTitle() {
        return title;
    }

    @NotNull
    @Nls(capitalization = Nls.Capitalization.Sentence)
    public String getMessage() {
        return message;
    }
}
