package com.perforce.p4java.common.base;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.StringHelper.format;

import java.io.IOException;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.common.function.FunctionWithException;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.AuthenticationFailedException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// p4ic4idea: remove references to sun package
// import sun.misc.Unsafe;

/**
 * @author Sean Shou
 * @since 2/09/2016
 */
public final class P4JavaExceptions {

    // p4ic4idea: allow for easy java exception translation
    public interface WrappedJavaException<T> {
        T call() throws ConnectionException, RequestException, AccessException, P4JavaException;
    }


    private P4JavaExceptions() { /* util */ }

    /**
     * If the check <code>expression</code> fails; then <code>ConnectionException</code> will throw
     * with given error message <code>message</code>
     */
    public static void throwConnectionExceptionIfConditionFails(
            final boolean expression,
            final String message,
            final Object... args) throws ConnectionException {

        if (!expression) {
            String exceptionMessage = format(message, args);
            throw new ConnectionException(exceptionMessage);
        }
    }

    public static void throwConnectionException(String message, Object... args) throws ConnectionException {
        String exceptionMessage = format(message, args);
        throw new ConnectionException(exceptionMessage);
    }

    public static void throwConnectionException(Throwable cause) throws ConnectionException {
        throw new ConnectionException(cause);
    }

    public static void throwConnectionException(Throwable cause, String message, Object... args) throws ConnectionException {
        String exceptionMessage = format(message, args);
        throw new ConnectionException(exceptionMessage, cause);
    }

    /**
     * If the check <code>expression</code> fails; then <code>ConnectionException</code> will throw
     * with given error message <code>message</code>
     */
    public static void throwProtocolErrorIfConditionFails(boolean expression, String message, Object... args) throws ProtocolError {
        if (!expression) {
            String exceptionMessage = format(message, args);
            throw new ProtocolError(exceptionMessage);
        }
    }

    /**
     * If the check <code>expression</code> fails; then <code>ConnectionException</code> will throw
     * with given error message <code>message</code>
     */
    public static void throwP4JavaErrorIfConditionFails(boolean expression, String message, Object... args) throws P4JavaError {
        if (!expression) {
            String exceptionMessage = format(message, args);
            throw new P4JavaError(exceptionMessage);
        }
    }

    public static void throwP4JavaError(Throwable cause, String message, Object... args) throws P4JavaError {
        String exceptionMessage = format(message, args);
        throw new P4JavaError(exceptionMessage, cause);
    }

    public static void throwP4JavaError(String message, Object... args) throws P4JavaError {
        String exceptionMessage = format(message, args);
        throw new P4JavaError(exceptionMessage);
    }

    /**
     * If the check <code>expression</code> fails; then <code>ConnectionException</code> will throw
     * with given error message <code>message</code>
     */
    public static void throwOptionsExceptionIfConditionFails(boolean expression, String message, Object... args) throws OptionsException {
        if (!expression) {
            String exceptionMessage = format(message, args);
            throw new OptionsException(exceptionMessage);
        }
    }

    public static void throwOptionsException(Throwable cause, String message, Object... args) throws OptionsException {
        String exceptionMessage = format(message, args);
        throw new OptionsException(exceptionMessage, cause);
    }

    public static void throwOptionsException(Throwable cause) throws OptionsException {
        throw new OptionsException(cause);
    }

    public static void throwOptionsException(String message, Object... args) throws OptionsException {
        String exceptionMessage = format(message, args);
        throw new OptionsException(exceptionMessage);
    }

    // p4ic4idea: rather than throw RequestException for bad arguments, use IAEs.
    public static void assertInvocation(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    // p4ic4idea: add support for IServerMessage
    public static void throwRequestExceptionIfConditionFails(boolean expression, IServerMessage msg)
            throws RequestException {
        if (!expression) {
            throw new RequestException(msg);
        }
    }

    // p4ic4idea: removed in favor of using an IServerMessage
    ///**
    // * If the check <code>expression</code> fails; then <code>ConnectionException</code> will throw
    // * with given error message <code>message</code>
    // */
    //public static void throwRequestExceptionIfConditionFails(boolean expression, String codeString, String message,
    //        Object... args) throws RequestException {
    //    if (!expression) {
    //        String exceptionMessage = format(message, args);
    //        throw new RequestException(exceptionMessage, codeString);
    //    }
    //}

    // p4ic4idea: removed in favor of using an IServerMessage
    ///**
    // * If the check <code>expression</code> fails; then <code>ConnectionException</code> will throw
    // * with given error message <code>message</code>
    // */
    //public static void throwRequestExceptionIfConditionFails(
    //        final boolean expression,
    //        final String message,
    //        final Object... args) throws RequestException {
    //
    //    if (!expression) {
    //        String exceptionMessage = format(message, args);
    //        throw new RequestException(exceptionMessage);
    //    }
    //}

    public static void throwRequestExceptionIfPerforceServerVersionOldThanExpected(
            final boolean expression,
            final String message,
            final Object... args) throws RequestException {

        if (!expression) {
            String exceptionMessage = format(message, args);
            // FIXME p4ic4idea: use an IServerMessage
            throw new RequestException(
                    exceptionMessage,
                    MessageGenericCode.EV_UPGRADE,
                    MessageSeverityCode.E_FAILED);
        }
    }

    // p4ic4idea: replaced with IServerMessage version.
    ///**
    // * If the check <code>expression</code> fails; then <code>ConnectionException</code> will throw
    // * with given error message <code>message</code>
    // */
    //public static void throwAccessExceptionIfConditionFails(boolean expression, String message, Object... args) throws AccessException {
    //    if (!expression) {
    //        String exceptionMessage = format(message, args);
    //        throw new AccessException(exceptionMessage);
    //    }
    //}

    public static void throwAccessExceptionIfConditionFails(boolean expression, IServerMessage msg) throws AccessException {
        if (!expression) {
            throw new AccessException(msg);
        }
    }


    public static void throwIOException(Throwable cause) throws IOException {
        throw new IOException(cause);
    }

    public static void throwIOException(Throwable cause, String message, Object... args) throws IOException {
        String exceptionMessage = format(message, args);
        throw new IOException(exceptionMessage, cause);
    }

    public static void throwIOException(String message, Object... args) throws IOException {
        String exceptionMessage = format(message, args);
        throw new IOException(exceptionMessage);
    }

    public static void throwIOExceptionIfConditionFails(boolean expression, String message, Object... args) throws IOException {
        if (!expression) {
            String exceptionMessage = format(message, args);
            throw new IOException(exceptionMessage);
        }
    }

    /**
     * @deprecated p4ic4idea {@link P4JavaExceptions#asRequestException(WrappedJavaException)}
     * @param function
     * @param <T>
     * @param <R>
     * @return
     */
    public static <T, R> Function<T, R> rethrowFunction(final FunctionWithException<T, R> function) {
        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                try {
                    return function.apply(t);
                } catch (P4JavaException exc) {
                    //String version = System.getProperty("java.version");
                    throwAsUnchecked(exc);
                    return null;
                }
            }
        };
    }

    /**
     * Rethrowing checked exceptions but it's not required catch statement
     *
     * @apiNote after jdk1.8
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAsUnchecked(Exception exception) {
        // p4ic4idea: upgraded to java 8.
        throwAsUncheckedAfterJava8(exception);
        //String version = System.getProperty("java.version");
        //if (StringUtils.startsWith(version, "1.7")) {
        //    throwAsUncheckedInJava7(exception);
        //} else {
        //    /*
        //    * FIXME: if p4java upgrade to java 8, change to
        //    * <pre>
        //    *     throwAsUncheckedAfterJava8(exception);
        //    * </pre>
        //    *
        //    */
        //    getUnsafe().throwException(exception);
        //}
    }

    /**
     * Rethrowing checked exceptions but it's not required catch statement
     *
     * @apiNote after jdk1.8
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAsUncheckedAfterJava8(Exception exception) throws E {
        throw (E) exception;
    }

    // p4ic4idea: allow for easy java exception translation
    public static <T> T asRequestException(final @Nonnull WrappedJavaException<T> callable) throws ConnectionException, RequestException, AccessException {
        try {
            return callable.call();
        } catch (final ConnectionException | RequestException | AccessException e) {
            throw e;
        } catch (final P4JavaException e) {
            throw new RequestException(e);
        }
    }


    // p4ic4idea: remove references to sun package
    //private static Unsafe getUnsafe(){
    //    try {
    //        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
    //        theUnsafe.setAccessible(true);
    //        return (Unsafe) theUnsafe.get(null);
    //    } catch (Exception e) {
    //        throw new AssertionError(e);
    //    }
    //}

    // p4ic4idea: not needed now that we're at jre 1.8
    ///**
    // * Rethrowing checked exceptions but it's not required catch statement
    // *
    // * @apiNote before jdk1.8
    // */
    //@SuppressWarnings("unchecked")
    //private static <E extends Throwable> void throwAsUncheckedInJava7(Exception exception) {
    //    Thread.currentThread().stop(exception);
    //}
}
