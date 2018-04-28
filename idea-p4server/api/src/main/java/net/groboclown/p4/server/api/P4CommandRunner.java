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

package net.groboclown.p4.server.api;

import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;

/**
 * API for communicating from IDEA to the Perforce server.  This
 * implementation allows for facades that talk to offline caches
 * and the server proper, and enforce concepts of delayed
 * actions due to network latency and server wait time.
 * <p>
 * If the P4ServerExec instance keeps an internal cache, then the
 * cache must be application-wide, and not tied to a project, otherwise
 * issues could arise if the user has two projects open running with
 * the same client workspace.
 */
public interface P4ServerExec {
    // TODO this error category and ResultError are being experimented
    // with.  Need to try them out and see how well the code "smells"
    // versus standard exception handling.  The idea is that normal
    // real problems are propagated through the system with the
    // message bus to inform the user and systems that something isn't
    // right, while this here is for letting the callers know that
    // something went wrong, but they don't need to deal with any
    // details.  This differs from the standard mode, which is to
    // break each one of these into its own top level exception type
    // and report the error as throwable.  However, that doesn't
    // work when dealing with Promises.  By making it a single exception
    // type, hopefully things work out better.  Additionally, by having
    // the messaging handle the errors, and pushing most of the processing
    // into promises, the result for actions can be mostly ignored.
    enum ErrorCategory {
        /** Something in the java code didn't work right. */
        INTERNAL,

        /** The connection to the server failed. */
        CONNECTION,

        /** The user wasn't allowed to perform the operation. */
        ACCESS_DENIED,

        /** The server encountered an error. */
        SERVER_ERROR,

        /** The request took too long to run.  This is different
         * than a connection timeout, and is usually related to other
         * commands blocking this one from running. */
        TIMEOUT
    }

    /***
     * Rather than throwing an exception, this allows the API
     * to tailor the error to the demands of the invoking call.
     * It should be assumed that most error conditions will
     * already be handled through the messaging API.
     */
    interface ResultError {
        // TODO potentially add a messaging interface to pass in here.
        // void handleError(SomeMessageListener listener);

        @NotNull
        ErrorCategory getCategory();

        @Nls
        @NotNull
        Optional<String> getMessage();
    }

    /**
     * The general kind of exception that can cause a promise
     * to fail (the "catch" clause).
     */
    class ServerResultException
            extends Exception {
        private final ResultError error;

        public ServerResultException(ResultError error) {
            this.error = error;
        }

        /**
         * Error that prevented this operation from completing normally.
         *
         * @return the result error.
         */
        @NotNull
        ResultError getResultError() {
            return error;
        }
    }

    // The type system here for results, commands, and requests
    // all is to help keep the user and server code correct in
    // understanding which commands require different connection properties.
    // Note that the way these requests are structured makes this server API
    // on the surface connection-less.  Because server commands are client
    // aware, not project aware, this means that there's a potential for a
    // user to have tow projects open working against the same workspace.
    // That implies that the server exec MUST be application-wide, so that
    // the cache will reflect those shared changes.

    interface ServerCmd {}
    interface ClientCmd {}
    interface ServerNameCmd {}


    /**
     * Generic marker interface for all results from the server.
     * The input to the server will be tailored to match up with a
     * result type.
     */
    interface ServerResult {
        @NotNull
        ServerConfig getServerConfig();
    }

    /**
     * Generic marker interface for all results from the server.
     * The input to the server will be tailored to match up with a
     * result type.
     */
    interface ClientResult {
        @NotNull
        ClientConfig getClientConfig();
    }

    /**
     * Generic marker interface for all results from the server.
     * The input to the server will be tailored to match up with a
     * result type.
     */
    interface ServerNameResult {
        @NotNull
        P4ServerName getServerName();
    }

    /**
     * General interface for all requests to the server API.
     *
     * @param <R> the type of the returned result for this request.
     */
    interface ServerRequest<R extends ServerResult, C extends ServerCmd> {
        @NotNull
        Class<? extends R> getResultType();

        C getCmd();
    }

    interface ClientRequest<R extends ClientResult, C extends ClientCmd> {
        @NotNull
        Class<? extends R> getResultType();
        C getCmd();
    }

    interface ServerNameRequest<R extends ServerNameResult, C extends ServerNameCmd> {
        @NotNull
        Class<? extends R> getResultType();
        C getCmd();
    }

    /**
     * All available behaviors that the plugin allows
     * to make changes to the server state.
     */
    enum ClientActionCmd implements ClientCmd {
        MOVE_FILE,
        ADD_EDIT_FILE,
        DELETE_FILE,
        REVERT_FILE,
        MOVE_FILES_TO_CHANGELIST,
        EDIT_CHANGELIST_DESCRIPTION,
        ADD_JOB_TO_CHANGELIST,
        CREATE_CHANGELIST,
    }

    enum ServerActionCmd implements ServerCmd {
        CREATE_JOB,
    }

    /**
     * Implementations map an ActionType and Result type
     * together with parameters to perform the action.
     * Requests will return a result of the action's type.
     *
     * @param <R> type of result returned by this action.
     */
    @Immutable
    interface ServerAction<R extends ServerResult> extends ServerRequest<R, ServerActionCmd> {
        // The action should be persistable, indeed on par with a LocalHistory
        // action object.
        // Internally, the action will be wrapped in a description that
        // includes whether the action was performed.  This allows the cached
        // backlog (for offline work) to determine if an action needs to happen
        // or not.
        // These internal objects should also have an index, so that they can
        // be properly ordered.
    }

    @Immutable
    interface ClientAction<R extends ClientResult> extends ClientRequest<R, ClientActionCmd> {
    }


    enum ClientQueryCmd implements ClientCmd {
        LIST_OPENED_FILES,
    }

    enum ServerQueryCmd implements ServerCmd {
        LIST_CLIENTS_FOR_USER,
        LIST_CHANGELISTS_FOR_CLIENT,
        LIST_FILES,
        LIST_DIRECTORIES,
        STAT_FILES,
        DESCRIBE_CHANGELIST,
        FILE_CHANGE_HISTORY,
        LIST_SUBMITTED_CHANGELISTS,
    }


    enum ServerNameQueryCmd implements ServerNameCmd {
        LIST_USERS,
    }

    @Immutable
    interface ServerQuery<R extends ServerResult> extends ServerRequest<R, ServerQueryCmd> {
    }

    @Immutable
    interface ClientQuery<R extends ClientResult> extends ClientRequest<R, ClientQueryCmd> {
    }

    @Immutable
    interface ServerNameQuery<R extends ServerNameResult> extends ServerNameRequest<R, ServerNameQueryCmd> {
    }

    // Sync queries block thread execution until the server returns with a result
    // (or some kind of timeout or request stop).  Because of that, these commands
    // must be restricted to non-EDT threads.  If something from the API absolutely
    // requires a sync command response, then a cache should be used where possible,
    // and the Promise should trigger the re-execution of the command against the
    // updated cache.

    enum SyncServerQueryCmd implements ServerCmd {
        LIST_CLIENTS_FOR_USER,
        LIST_CHANGELISTS_FOR_CLIENT,
        LIST_FILES,
        LIST_DIRECTORIES,
        STAT_FILES,
        DESCRIBE_CHANGELIST,
        FILE_CHANGE_HISTORY,
        LIST_SUBMITTED_CHANGELISTS,
    }

    enum SyncClientQueryCmd implements ClientCmd {
        LIST_OPENED_FILES,
    }

    @Immutable
    interface SyncServerQuery<R extends ServerResult> extends ServerRequest<R, SyncServerQueryCmd> {
    }

    @Immutable
    interface SyncClientQuery<R extends ClientResult> extends ClientRequest<R, SyncClientQueryCmd> {
    }


    /**
     * Support for requesting the results from a query, which will
     * return the most recently fetched version for immediate use, along
     * with a {@link Promise} for when the most recent data becomes available.
     *
     * @param <R>
     */
    class FutureResult<R> {
        private final Promise<R> promise;
        private final R last;

        public FutureResult(Promise<R> promise, R last) {
            this.promise = promise;
            this.last = last;
        }

        public R getLast() {
            return last;
        }

        public Promise<R> getPromise() {
            return promise;
        }
    }


    /**
     * Performs the action against the server.  Actions are behaviors
     * that may or may not take a long time to process.  If running
     * disconnected, the action may be queued and processed later.
     *
     * @param action
     * @param <R>
     * @return a promise for the result.  Errors for catching are either general
     *      Java errors (NPE and so on) or {@link ServerResultException}.
     */
    @NotNull
    <R extends ServerResult> Promise<R> perform(@NotNull ServerConfig config, @NotNull ServerAction<R> action);

    @NotNull
    <R extends ClientResult> Promise<R> perform(@NotNull ClientConfig config, @NotNull ClientAction<R> action);

    @NotNull
    <R extends ServerResult> Promise<R> query(@NotNull ServerConfig config, @NotNull ServerQuery<R> query);

    @NotNull
    <R extends ClientResult> Promise<R> query(@NotNull ClientConfig config, @NotNull ClientQuery<R> query);

    @NotNull
    <R extends ServerNameResult> Promise<R> query(@NotNull P4ServerName name, @NotNull ServerNameQuery<R> query);

    /**
     * Returns the cached result from the most recently stored data.
     *
     * @param config
     * @param query
     * @param <R>
     * @return
     * @throws ServerResultException
     */
    @NotNull
    <R extends ServerResult> R syncCachedQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query)
        throws ServerResultException;

    @NotNull
    <R extends ClientResult> R syncCachedQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query)
            throws ServerResultException;

    /**
     * Returns cached results and a Promise for the request.
     *
     * @param config
     * @param query
     * @param <R>
     * @return
     * @throws ServerResultException
     */
    @NotNull
    <R extends ServerResult> FutureResult<R> syncQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query)
            throws ServerResultException;

    @NotNull
    <R extends ClientResult> FutureResult<R> syncQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query)
            throws ServerResultException;
}
