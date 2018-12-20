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

package net.groboclown.p4.server.impl.connection;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MockP4RequestErrorHandler extends P4RequestErrorHandler {
    private final List<Throwable> exceptions = new ArrayList<>();
    private final List<Exception> disconnectExceptions = new ArrayList<>();


    List<Throwable> getExceptions() {
        return exceptions;
    }

    List<Exception> getDisconnectExceptions() {
        return disconnectExceptions;
    }


    @Nonnull
    @Override
    protected P4CommandRunner.ServerResultException handleException(
            @NotNull ConnectionInfo info, @Nonnull Exception e) {
        this.exceptions.add(e);
        return createServerResultException(e, e.getMessage(), P4CommandRunner.ErrorCategory.INTERNAL);
    }

    @Nonnull
    @Override
    protected P4CommandRunner.ServerResultException handleError(@NotNull ConnectionInfo info, @NotNull Error e) {
        exceptions.add(e);
        return createServerResultException(e, e.getMessage(), P4CommandRunner.ErrorCategory.INTERNAL);
    }

    @Override
    protected boolean isRetryableError(@NotNull Exception e) {
        return false;
    }

    @Override
    protected int getMaxRetryCount() {
        return 0;
    }


    @Override
    public void handleOnDisconnectError(@Nonnull ConnectionException e) {
        disconnectExceptions.add(e);
    }


    @Override
    public void handleOnDisconnectError(@Nonnull AccessException e) {
        disconnectExceptions.add(e);
    }
}
