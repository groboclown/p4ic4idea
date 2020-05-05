/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java;

import com.perforce.p4java.server.callback.ILogCallback.LogTraceLevel;

/**
 * Simple P4Java tracer class based on the Log trace callback
 * interface. Useful for tracing and debugging from within P4Java;
 * probably not of interest to most end-users.<p>
 * 
 * Not especially thread-safe, nor recommended for real debugging.<p>
 * 
 * Note that in general, GA releases disable internal traces, and this
 * class is not actively used in those releases.
 */

public class Tracer {
	
	public static void trace(LogTraceLevel traceLevel, String msg) {
		// Disabled for release; uncomment the following line for debug releases -- HR.
		// Log.trace(traceLevel, msg);
	}
	
	public static void all(String msg) {
		trace(LogTraceLevel.ALL, msg);
	}
	
	public static void superfine(String msg) {
		trace(LogTraceLevel.SUPERFINE, msg);
	}
	
	public static void fine(String msg) {
		trace(LogTraceLevel.FINE, msg);
	}
	
	public static void coarse(String msg) {
		trace(LogTraceLevel.COARSE, msg);
	}
	
	public static boolean isTracingAtLevel(LogTraceLevel traceLevel) {
		return Log.isTracingAtLevel(traceLevel);
	}
}
