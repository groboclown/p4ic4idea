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

package net.groboclown.p4plugin.modules.boilerplate;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nullable;

/**
 * A stateless task runner for modules that need to inject on-startup behavior.
 */
public abstract class AbstractDisposable
        implements Disposable {
    private boolean disposed = false;
    private final Disposable proxy;

    protected AbstractDisposable() {
        this(null);
    }

    protected AbstractDisposable(@Nullable Disposable proxy) {
        this.proxy = proxy;
    }

    @Override
    public synchronized void dispose() {
        if (!disposed) {
            disposed = true;
            if (this.proxy != null) {
                this.proxy.dispose();
            }
            Disposer.dispose(this);
        }
    }
}
