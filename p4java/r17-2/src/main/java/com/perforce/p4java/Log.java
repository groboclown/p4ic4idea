package com.perforce.p4java;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.StringHelper.format;

import javax.annotation.Nullable;

import com.perforce.p4java.server.callback.ILogCallback;
import com.perforce.p4java.server.callback.ILogCallback.LogTraceLevel;

/**
 * Simple P4Java-wide logger class based on the ILogCallback callback
 * interface. Useful for letting P4Java consumers report errors,
 * warnings, etc., generated within P4Java into their own logs.<p>
 *
 * Note that absolutely no guarantees or specifications
 * are made about the format or content of strings that are passed through
 * the logging mechanism, but in general all such strings are useful for
 * Perforce support staff, and many info and stats strings passed to
 * the callback may be generally useful for API consumers.<p>
 *
 * The Log class is itself used from within P4Java to report log
 * messages; the intention here is to allow consumers to call the
 * setLogCallback static method with a suitable log listener that
 * the P4Java API will log to internally. Most of the methods below
 * besides the setLogCallback method are mainly intended for API-internal
 * use, but participating apps may find the other methods useful for
 * interpolating marker text or other messages to the API's log.
 *
 * @author sshou clean code & add vargs like slf4j
 */
public class Log {

  private static ILogCallback logCallback = null;

  /**
   * Get the current log callback, if any. May return null.
   */
  public static ILogCallback getLogCallback() {
    return Log.logCallback;
  }

  /**
   * Set the P4Java API's internal logger to log to the passed-in
   * ILogCallback log callback. If the passed-in parameter is null,
   * no logging will be performed. The caller is responsible for ensuring
   * that there are not thread issues with the passed-in callback, and
   * that callbacks to the callback object will not block or deadlock.
   *
   * @param logCallback callback to be used by P4Java to report log messages to; if null, stop
   *                    logging.
   * @return the previous callback registered, or null if no such callback existed.
   */
  public static ILogCallback setLogCallback(ILogCallback logCallback) {
    ILogCallback oldCallback = Log.logCallback;
    Log.logCallback = logCallback;
    return oldCallback;
  }

  /**
   * Report a P4Java-internal error to the log callback (if it exists).
   *
   * @param errorString non-null error string.
   */
  public static void error(String errorString, @Nullable Object... args) {
    if (nonNull(logCallback)) {
      String errorMessage = errorString;
      if (nonNull(args)) {
        errorMessage = format(errorString, args);
      }
      logCallback.internalError(errorMessage);
    }
  }

  /**
   * Report a P4Java-internal warning to the log callback (if it exists).
   *
   * @param warnString non-null warning message.
   */
  public static void warn(String warnString, @Nullable Object... args) {
    if (nonNull(logCallback)) {
      String warningMessage = warnString;
      if (nonNull(args)) {
        warningMessage = format(warnString, args);
      }
      logCallback.internalWarn(warningMessage);
    }
  }

  /**
   * Report a P4Java-internal informational event to the log callback (if it exists).
   *
   * @param infoString non-null info message.
   */
  public static void info(String infoString, @Nullable Object... args) {
    if (nonNull(logCallback)) {
      String infoMessage = infoString;
      if (nonNull(args)) {
        infoMessage = format(infoString, args);
      }
      logCallback.internalInfo(infoMessage);
    }
  }

  /**
   * Report a P4Java-internal statistics message to the log callback (if it exists).
   *
   * @param statsString non-null stats message.
   */
  public static void stats(String statsString, @Nullable Object... args) {
    if (nonNull(logCallback)) {
      String statsMessage = statsString;
      if (nonNull(args)) {
        statsMessage = format(statsString, args);
      }
      logCallback.internalStats(statsMessage);
    }
  }

  /**
   * Report a P4Java-internal unexpected exception to the log callback
   * (if it exists).
   *
   * @param thr non-null Throwable
   */
  public static void exception(Throwable thr) {
    if (nonNull(logCallback) && nonNull(thr)) {
      logCallback.internalException(thr);
    }
  }

  /**
   * Report a P4Java-internal trace message to the log callback
   * (if it exists).
   */
  public static void trace(LogTraceLevel traceLevel, String traceMessage) {
    if (nonNull(logCallback)
        && nonNull(traceLevel)
        && nonNull(traceMessage)
        && isTracingAtLevel(traceLevel)) {
      logCallback.internalTrace(traceLevel, traceMessage);
    }
  }

  /**
   * Return true if the
   */
  public static boolean isTracingAtLevel(LogTraceLevel traceLevel) {
    return nonNull(logCallback)
        && nonNull(logCallback.getTraceLevel())
        && (traceLevel.compareTo(logCallback.getTraceLevel()) <= 0);
  }
}
