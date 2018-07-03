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

import com.intellij.openapi.vcs.VcsException;
import net.groboclown.p4.server.api.commands.sync.SyncListFilesDetailsQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

// For now, keep all these inner types within this one class.
// It helps contain all the stuff that the runner does.
// Eventually, this may get pushed out into the commands package.

/**
 * API for communicating from IDEA to the Perforce server.  This
 * implementation allows for facades that talk to offline caches
 * and the server proper, and enforce concepts of delayed
 * actions due to network latency and server wait time.
 * <p>
 * If the P4CommandRunner instance keeps an internal cache, then the
 * cache must be application-wide, and not tied to a project, otherwise
 * issues could arise if the user has two projects open running with
 * the same client workspace.
 */
public interface P4CommandRunner {
    /** TODO this error category and ResultError are being experimented
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
     */
    enum ErrorCategory {
        /** Something in the java code didn't work right. */
        INTERNAL,

        /** The connection to the server failed. */
        CONNECTION,

        /**
         * The user wasn't allowed to perform the operation, potentially because of
         * a login error, or because the user didn't have adequate permissions. */
        ACCESS_DENIED,

        /** The server encountered an error, or otherwise reported a failure with
         * the request.  Hopefully, this is the most common issue the user will
         * encounter during normal use. */
        SERVER_ERROR,

        /** Something on the local computer caused a problem. */
        OS,

        /** The request took too long to run.  This is different
         * than a connection timeout, and is usually related to other
         * commands blocking this one from running.  Or, it could be
         * the user or other system cancelling the request. */
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
            extends VcsException {
        private final ResultError error;

        public ServerResultException(ResultError error) {
            super(error.getMessage().orElse("unknown problem"), false);
            this.error = error;
        }

        public ServerResultException(ResultError error, Throwable cause) {
            super(cause);
            this.error = error;
        }

        /**
         * Error that prevented this operation from completing normally.
         *
         * @return the result error.
         */
        @NotNull
        public ResultError getResultError() {
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

    // Every CMD has its own Request and Result object.  Internally, there will
    // need to be a mapping between the query class and an executor class.

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

    // Requests are intentionally not inheriting from a
    // shared source, to prohibit incorrect usage of the
    // type.  It makes for a bit more code but for enhanced
    // compile-time safety.

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
        /**
         * @see net.groboclown.p4.server.api.commands.file.MoveFileAction
         * @see net.groboclown.p4.server.api.commands.file.MoveFileResult
         */
        MOVE_FILE,

        /**
         * @see net.groboclown.p4.server.api.commands.file.AddEditAction
         * @see net.groboclown.p4.server.api.commands.file.AddEditResult
         */
        ADD_EDIT_FILE,

        /**
         * @see net.groboclown.p4.server.api.commands.file.DeleteFileAction
         * @see net.groboclown.p4.server.api.commands.file.DeleteFileResult
         */
        DELETE_FILE,

        /**
         * @see net.groboclown.p4.server.api.commands.file.RevertFileAction
         * @see net.groboclown.p4.server.api.commands.file.RevertFileResult
         */
        REVERT_FILE,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction
         * @see net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistResult
         */
        MOVE_FILES_TO_CHANGELIST,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.EditChangelistAction
         * @see net.groboclown.p4.server.api.commands.changelist.EditChangelistResult
         */
        EDIT_CHANGELIST_DESCRIPTION,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistAction
         * @see net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistResult
         */
        ADD_JOB_TO_CHANGELIST,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.RemoveJobFromChangelistAction
         * @see net.groboclown.p4.server.api.commands.changelist.RemoveJobFromChangelistResult
         */
        REMOVE_JOB_FROM_CHANGELIST,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction
         * @see net.groboclown.p4.server.api.commands.changelist.CreateChangelistResult
         */
        CREATE_CHANGELIST,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction
         * @see net.groboclown.p4.server.api.commands.changelist.DeleteChangelistResult
         */
        DELETE_CHANGELIST,

        /**
         * @see net.groboclown.p4.server.api.commands.file.FetchFilesAction
         * @see net.groboclown.p4.server.api.commands.file.FetchFilesResult
         */
        FETCH_FILES,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction
         * @see net.groboclown.p4.server.api.commands.changelist.SubmitChangelistResult
         */
        SUBMIT_CHANGELIST,
    }

    enum ServerActionCmd implements ServerCmd {
        /**
         * @see net.groboclown.p4.server.api.commands.changelist.CreateJobAction
         * @see net.groboclown.p4.server.api.commands.changelist.CreateJobResult
         */
        CREATE_JOB,

        /**
         * Explicit login request.  This prevents all kinds of hazardous authentication
         * checks.
         *
         * @see net.groboclown.p4.server.api.commands.server.LoginAction
         * @see net.groboclown.p4.server.api.commands.server.LoginResult
         */
        LOGIN,

        // Note: no DELETE_JOB
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

        /**
         * A unique ID for the action, so that pending vs. completed cached states can be
         * properly tracked.
         *
         * @return ID for the action.
         */
        String getActionId();
    }

    @Immutable
    interface ClientAction<R extends ClientResult> extends ClientRequest<R, ClientActionCmd> {

        /**
         * A unique ID for the action, so that pending vs. completed cached states can be
         * properly tracked.
         *
         * @return ID for the action.
         */
        @NotNull
        String getActionId();
    }


    enum ClientQueryCmd implements ClientCmd {
        /**
         * @see net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesQuery
         * @see net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult
         */
        LIST_OPENED_FILES_CHANGES,

        /**
         * @see net.groboclown.p4.server.api.commands.client.ListClientFetchStatusQuery
         * @see net.groboclown.p4.server.api.commands.client.ListClientFetchStatusResult
         */
        LIST_CLIENT_FETCH_STATUS, // cstat
    }

    enum ServerQueryCmd implements ServerCmd {
        // Perhaps streams support can be added later...
        // LIST_STREAMS,
        // LIST_STREAM_INTEGRATION_STATUS, // istat

        /**
         * @see net.groboclown.p4.server.api.commands.client.ListClientsForUserQuery
         * @see net.groboclown.p4.server.api.commands.client.ListClientsForUserResult
         */
        LIST_CLIENTS_FOR_USER,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.ListChangelistsFixedByJobQuery
         * @see net.groboclown.p4.server.api.commands.changelist.ListChangelistsFixedByJobResult
         */
        LIST_CHANGELISTS_FIXED_BY_JOB,

        /**
         * @see net.groboclown.p4.server.api.commands.file.ListFilesQuery
         * @see net.groboclown.p4.server.api.commands.file.ListFilesResult
         */
        LIST_FILES,

        /**
         * @see net.groboclown.p4.server.api.commands.file.ListDirectoriesQuery
         * @see net.groboclown.p4.server.api.commands.file.ListDirectoriesResult
         */
        LIST_DIRECTORIES,

        /**
         * @see net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery
         * @see net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult
         */
        LIST_FILES_DETAILS,

        /**
         * Describe a remote changelist.  Used for history reporting.
         *
         * @see net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery
         * @see net.groboclown.p4.server.api.commands.changelist.DescribeChangelistResult
         */
        DESCRIBE_CHANGELIST,

        /**
         * @see net.groboclown.p4.server.api.commands.file.ListFilesHistoryQuery
         * @see net.groboclown.p4.server.api.commands.file.ListFilesHistoryResult
         */
        LIST_FILES_HISTORY,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsQuery
         * @see net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsResult
         */
        LIST_SUBMITTED_CHANGELISTS,

        /**
         * @see net.groboclown.p4.server.api.commands.user.ListUsersQuery
         * @see net.groboclown.p4.server.api.commands.user.ListUsersResult
         */
        LIST_USERS,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.ListJobsQuery
         * @see net.groboclown.p4.server.api.commands.changelist.ListJobsResult
         */
        LIST_JOBS,

        /**
         * @see net.groboclown.p4.server.api.commands.file.AnnotateFileQuery
         * @see net.groboclown.p4.server.api.commands.file.AnnotateFileResult
         */
        ANNOTATE_FILE,

        /**
         * @see net.groboclown.p4.server.api.commands.changelist.GetJobSpecQuery
         * @see net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult
         */
        GET_JOB_SPEC,
    }


    enum ServerNameQueryCmd implements ServerNameCmd {
        /**
         * @see net.groboclown.p4.server.api.commands.server.ServerInfoQuery
         * @see net.groboclown.p4.server.api.commands.server.ServerInfoResult
         */
        SERVER_INFO
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

    // Sync queries explicitly return cached results, and so must be
    // referenced separately.  They do not reuse the non-sync enums
    // so that we can limit which commands can be called in a synchronous
    // way, thus limiting the need for cached values.  They do, however,
    // reuse the result type from the non-sync version.

    enum SyncServerQueryCmd implements ServerCmd {
        /* Add items only when absolutely necessary.  These have implications on large cached data.
        SYNC_LIST_CLIENTS_FOR_USER,
        SYNC_LIST_FILES,
        SYNC_LIST_DIRECTORIES,
        SYNC_STAT_FILES,
        SYNC_DESCRIBE_CHANGELIST,
        SYNC_FILE_CHANGE_HISTORY,
        SYNC_LIST_SUBMITTED_CHANGELISTS,
        */

        /**
         * @see net.groboclown.p4.server.api.commands.sync.SyncListFilesDetailsQuery
         * @see net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult
         */
        SYNC_LIST_FILES_DETAILS
    }

    enum SyncClientQueryCmd implements ClientCmd {
        /**
         * @see net.groboclown.p4.server.api.commands.sync.SyncListOpenedFilesChangesQuery
         * @see net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult
         */
        SYNC_LIST_OPENED_FILES_CHANGES,
    }

    @Immutable
    interface SyncServerQuery<R extends ServerResult> extends ServerRequest<R, SyncServerQueryCmd> {
    }

    @Immutable
    interface SyncClientQuery<R extends ClientResult> extends ClientRequest<R, SyncClientQueryCmd> {
    }

    /**
     * Describes the result from the server.  Because server errors should not be
     * handled in an Exception clause, errors are enclosed in an answer.
     * These can potentially run in another thread, but not necessarily.  Do not
     * rely upon a specific thread of execution.
     */
    @Immutable
    interface ActionAnswer<S> {
        /**
         * Executes if the request was not run due to the server being offline.
         * The command might run in the future, if the cache can handle the
         * request, but there's no guarantee that it will.
         *
         * @param r command to run
         */
        @NotNull
        ActionAnswer<S> whenOffline(Runnable r);
        @NotNull
        ActionAnswer<S> whenServerError(Consumer<ServerResultException> c);
        @NotNull
        ActionAnswer<S> whenCompleted(Consumer<S> c);

        @NotNull
        <T> ActionAnswer<T> mapAction(Function<S, T> fun);
        @NotNull
        <T> QueryAnswer<T> mapQuery(Function<S, T> fun);
        @NotNull
        <T> ActionAnswer<T> mapActionAsync(Function<S, ActionAnswer<T>> fun);
        @NotNull
        <T> QueryAnswer<T> mapQueryAsync(Function<S, QueryAnswer<T>> fun);

        void after(Runnable fun);

        /**
         *
         * @param timeout time to wait
         * @param unit time unit
         * @return true if the wait completes as normal, or false if there was a timeout.
         */
        boolean waitForCompletion(int timeout, TimeUnit unit)
                throws InterruptedException;

        /**
         *
         * @param timeout time out for waiting on a response.
         * @param unit time out unit
         * @return the result.
         * @throws InterruptedException thread was interrupted
         * @throws CancellationException timeout occurred
         * @throws ServerResultException error from the server, or if the server was offline.
         */
        @Nullable
        S blockingGet(int timeout, TimeUnit unit)
                throws InterruptedException, CancellationException, ServerResultException;
    }

    /**
     * Describes the result from the server.  Similar to a promise.
     * These can potentially run in another thread, but not necessarily.  Do not
     * rely upon a specific thread of execution.
     *
     * @param <S>
     */
    @Immutable
    interface QueryAnswer<S> {
        @NotNull
        QueryAnswer<S> whenCompleted(Consumer<S> c);
        @NotNull
        QueryAnswer<S> whenServerError(Consumer<ServerResultException> c);

        @NotNull
        <T> ActionAnswer<T> mapAction(Function<S, T> fun);
        @NotNull
        <T> QueryAnswer<T> mapQuery(Function<S, T> fun);
        @NotNull
        <T> ActionAnswer<T> mapActionAsync(Function<S, ActionAnswer<T>> fun);
        @NotNull
        <T> QueryAnswer<T> mapQueryAsync(Function<S, QueryAnswer<T>> fun);

        void after(Runnable fun);

        /**
         *
         * @param timeout time to wait
         * @param unit time unit
         * @return true if the wait completes as normal, or false if there was a timeout.
         */
        boolean waitForCompletion(int timeout, TimeUnit unit);

        S blockingGet(int timeout, TimeUnit unit)
                throws InterruptedException, CancellationException, ServerResultException;
    }


    /**
     * Support for requesting the results from a query, which will
     * return the most recently fetched version for immediate use, along
     * with a {@link QueryAnswer} for when the most recent data becomes available.
     *
     * @param <R>
     */
    class FutureResult<R> {
        private final QueryAnswer<R> promise;
        private final R last;

        FutureResult(QueryAnswer<R> promise, R last) {
            this.promise = promise;
            this.last = last;
        }

        public R getLast() {
            return last;
        }

        public QueryAnswer<R> getPromise() {
            return promise;
        }
    }


    /**
     * Performs the action against the server.  Actions are behaviors
     * that may or may not take a long time to process.  If running
     * disconnected, the action may be queued and processed later.
     * <p>
     * The answer may be something the caller doesn't care about, but some
     * calls, such as fetching data, will require feedback to the user.
     *
     * @param config server configuration
     * @param action action to perform
     * @param <R> type of server result to expect
     * @return a promise for the result.  Errors for catching are either general
     *      Java errors (NPE and so on) or {@link ServerResultException}.
     */
    @NotNull
    <R extends ServerResult> ActionAnswer<R> perform(
            @NotNull ServerConfig config, @NotNull ServerAction<R> action);

    @NotNull
    <R extends ClientResult> ActionAnswer<R> perform(
            @NotNull ClientConfig config, @NotNull ClientAction<R> action);

    /**
     * Attempts to run the query against the server.  If the server is offline, then
     * the cached response is returned.
     *
     * @param config configuration
     * @param query query
     * @param <R> request/response type
     * @return a promise for a future response.
     */
    @NotNull
    <R extends ServerResult> QueryAnswer<R> query(
            @NotNull ServerConfig config, @NotNull ServerQuery<R> query);

    @NotNull
    <R extends ClientResult> QueryAnswer<R> query(
            @NotNull ClientConfig config, @NotNull ClientQuery<R> query);

    @NotNull
    <R extends ServerNameResult> QueryAnswer<R> query(
            @NotNull P4ServerName name, @NotNull ServerNameQuery<R> query);

    /**
     * Returns the cached result from the most recently stored data.
     *
     * @param config server configuration
     * @param query query to perform
     * @param <R> result type
     * @return cached results
     */
    @NotNull
    <R extends ServerResult> R syncCachedQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query);

    @NotNull
    <R extends ClientResult> R syncCachedQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query);

    /**
     * Returns cached results and a Promise for the request.
     *
     * @param config server configuration
     * @param query query to execute
     * @param <R> expected result type
     * @return the cached value + promise when the actual request completes.
     */
    @NotNull
    <R extends ServerResult> FutureResult<R> syncQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query);

    @NotNull
    <R extends ClientResult> FutureResult<R> syncQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query);


}
