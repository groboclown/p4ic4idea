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

package net.groboclown.p4.server.api.messagebus;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

class ListenerProxy<L> implements InvocationHandler {
    private static final Logger LOG = Logger.getInstance(ListenerProxy.class);

    private final Object owner;
    private final L listener;

    static <L> L createProxy(@NotNull L listener, @NotNull Class<? extends L> listenerClass,
            @NotNull Object listenerOwner) {
        return listenerClass.cast(Proxy.newProxyInstance(listener.getClass().getClassLoader(),
                new Class[]{ listenerClass }, new ListenerProxy<L>(listenerOwner, listener)));
    }

    private ListenerProxy(Object owner, L listener) {
        this.owner = owner;
        this.listener = listener;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        assert args.length >= 1;
        assert args[0] instanceof AbstractMessageEvent :
                (method.getName() + " has first argument not AbstractMessageEvent, but " + args);

        AbstractMessageEvent evt = (AbstractMessageEvent) args[0];
        if (evt.visit(owner)) {
            // It has been visited before
            LOG.warn("DUPLICATE LISTENER VISITED: " + owner + " for " + method.getName());
            // Don't allow the duplicate execution.
            return null;
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Event call " + method + " for " + owner);
        }

        return method.invoke(listener, args);
    }
}
