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

package net.groboclown.idea.mock;

import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.PicoContainer;

public class P4icMockProject extends MockProject {
    private final MessageBus messageBus;

    public P4icMockProject(
            @NotNull PicoContainer parent,
            @NotNull MessageBus messageBus,
            @NotNull Disposable parentDisposable) {
        super(parent, parentDisposable);
        this.messageBus = messageBus;
    }

    @Override
    @NotNull
    public MessageBus getMessageBus() {
        return this.messageBus;
    }

    @Override
    public void dispose() {
        Disposer.dispose(this.messageBus);
        super.dispose();
    }

}
