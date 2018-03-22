/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.server.callback;

/**
 * General P4Java-wide logger callback interface. Designed to be
 * used by consumers to receive logging events from the P4Java
 * classes and methods; these can then be logged appropriately
 * in the consumer's own log(s) or reported in a popup, etc.<p>
 * 
 * Logging messages can range from mundane reports of connection
 * startup and shutdown to detailed exception and tracing events,
 * and consumers should plan accordingly. In particular, tracing
 * events should only be enabled when you are debugging P4Java
 * in conjunction with Perforce staff (unless you want to cope
 * with enormous log files...); info and stats messages are entirely
 * optional but might be useful for your own tracing purposes; warnings
 * and errors should always be logged, as P4Java only uses these for
 * things it regards as possible signs of something going wrong ahead of
 * time (warnings), or real errors.<p>
 * 
 * Note that there is only one such listener per instance of P4Java,
 * which effectively means per server factory instance rather than
 * per IServer.<p>
 * 
 * Note also that the format of the strings reported through this
 * mechanism is not well-defined, and that the intention here is that
 * the consumer should wrap the messages in their own logging formatting
 * with date / time prepended, etc.<p>
 * 
 * NOTE: you <b><i>must</i></b> ensure that there are no threading or reentrancy issues
 * with your implementation of this interface, and that calling any of the methods
 * here will not cause the caller (P4Java) to block or spend too much time processing
 * the callback.<p>
 */

public interface ILogCallback {
	
	/**
	 * Defines the various trace levels available. Should be more
	 * or less self-explanatory...
	 */
	enum LogTraceLevel {
		NONE,
		COARSE,
		FINE,
		SUPERFINE,
		ALL;
	};
	
	/**
	 * Report a P4Java-internal error. These are usually fatal
	 * errors encountered deep within P4Java, and will typically be accompanied
	 * by an exception or other throwable. These should always be logged by the
	 * consumer if possible.
	 * 
	 * @param errorString non-null error string; may contain newlines.
	 */
	void internalError(String errorString);
	
	/**
	 * Report an unexpected or otherwise interesting exception seen internally.
	 * Such exceptions are almost always the sign of something going horribly
	 * wrong, so these should always be logged if possible. The exceptions
	 * here are almost always immediately preceded by an internalError message
	 * that gives a more abstract view of what's going wrong.
	 * 
	 * @param thr non-null Throwable.
	 */
	void internalException(Throwable thr);
	
	/**
	 * Report a P4Java-internal warning. These are usually non-fatal
	 * issues encountered withing P4Java, and probably will not be accompanied
	 * by an exception, but should probably be logged by the consumer.
	 * 
	 * @param warnString non-null warning message; may contain newlines.
	 */
	void internalWarn(String warnString);
	
	/**
	 * Report a P4Java-internal informational event. These are typically "just
	 * letting you know" messages that can be logged or ignored with impunity.
	 * 
	 * @param infoString non-null info message; may contain newlines.
	 */
	void internalInfo(String infoString);
	
	/**
	 * Report some P4Java-internal statistics. These statistics are typically
	 * just statistics gathered over the course of some time period that may be
	 * useful to Perforce or end users; these can be logged or ignored with impunity.
	 * 
	 * @param statsString non-null stats message; may contain newlines.
	 */
	void internalStats(String statsString);
	
	/**
	 * Report an internal P4Java trace message at the passed-in trace level.
	 * 
	 * @param traceLevel trace level for associated message
	 * @param traceMessage non-null trace message; may contain newlines.
	 */
	void internalTrace(LogTraceLevel traceLevel, String traceMessage);
	
	/**
	 * Return the trace level being used in the callback. Used within P4Java
	 * to determine whether it's worth constructing trace messages on the fly
	 * or to ignore certain tracing calls.
	 */
	LogTraceLevel getTraceLevel();
}
