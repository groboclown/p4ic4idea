/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java;

import com.perforce.p4java.server.IServerMessage;
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
	 * @param logCallback callback to be used by P4Java to report log messages
	 * 			to; if null, stop logging.
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
	public static void error(String errorString) {
		if (logCallback != null) {
			logCallback.internalError(errorString);
		}
	}


	// p4ic4idea: add in error logging for an IServerMessage
	public static void error(final IServerMessage err) {
		if (logCallback != null) {
			logCallback.internalError(err.toString());
		}
	}

	/**
	 * Report a P4Java-internal warning to the log callback (if it exists).
	 * 
	 * @param warnString non-null warning message.
	 */
	public static void warn(String warnString) {
		if (logCallback != null) {
			logCallback.internalWarn(warnString);
		}
	}
	
	/**
	 * Report a P4Java-internal informational event to the log callback (if it exists).
	 * 
	 * @param infoString non-null info message.
	 */
	public static void info(String infoString) {
		if (logCallback != null) {
			logCallback.internalInfo(infoString);
		}
	}

	/**
	 * Report a P4Java-internal informational event to the log callback (if it exists).
	 *
	 * @param info non-null info message.
	 */
	// p4ic4idea: add in info logging for an IServerMessage
	public static void info(IServerMessage info) {
		if (logCallback != null) {
			logCallback.internalInfo(info.toString());
		}
	}

	/**
	 * Report a P4Java-internal statistics message to the log callback (if it exists).
	 * 
	 * @param statsString non-null stats message.
	 */
	public static void stats(String statsString) {
		if (logCallback != null) {
			logCallback.internalStats(statsString);
		}
	}
	
	/**
	 * Report a P4Java-internal unexpected exception to the log callback
	 * (if it exists).
	 * 
	 * @param thr non-null Throwable
	 */
	public static void exception(Throwable thr) {
		if ((logCallback != null) && (thr != null)) {
			logCallback.internalException(thr);
		}
	}
	
	/**
	 * Report a P4Java-internal trace message to the log callback
	 * (if it exists).
	 */
	public static void trace(LogTraceLevel traceLevel, String traceMessage) {
		if ((logCallback != null) && (traceLevel != null) && (traceMessage != null)
				&& isTracingAtLevel(traceLevel)) {
			logCallback.internalTrace(traceLevel, traceMessage);
		}
	}
	
	/**
	 * Return true if the 
	 * @param traceLevel
	 * @return
	 */
	public static boolean isTracingAtLevel(LogTraceLevel traceLevel) {
		if ((logCallback != null) && (logCallback.getTraceLevel() != null)
				&& (traceLevel.compareTo(logCallback.getTraceLevel()) <= 0)) {
			return true;
		}
		return false;
	}
}
