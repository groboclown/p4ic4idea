/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.server.callback;

/**
 * Provides a simple server command and command results notification callback
 * interface for P4Java consumers.<p>
 * 
 * The intention of the server series of methods is to notify the consumer
 * that P4Java has issued a Perforce server command or received a result
 * back from the server. The format of the results passed to the callback
 * method are generally intended to mimic what would have been seen from
 * a p4 command line command, but they may not always be identical.<p>
 * 
 * NOTE: you <b>must</b> ensure that there are no threading or reentrancy issues
 * with your implementation of this interface, and that calling any of the methods
 * here will not cause the caller (P4Java) to block or spend too much time processing
 * the callback.<p>
 *
 *
 */

public interface ICommandCallback {

	/**
	 * Report that P4Java is about to issue a Perforce server command. This
	 * is called immediately before the command itself is issued.<p>
	 * 
	 * The int return value is intended to be an opaque key for associated
	 * completedServerCommand, receivedServerInfoLine, and receivedServerErrorLine
	 * calls, allowing the consumer to correlate subsequent calls with the
	 * original command issuance. The key will also be the same value as used for any
	 * associated progress callbacks (see IProgressCallback) for the same command.
	 * In any case, this key will be unique across all calls (simultaneous or otherwise)
	 * to a given IServer, but is not guaranteed to be unique across an entire
	 * P4Java instance.
	 * 
	 * @param key -- opaque integer key for correlation with subsidiary calls.
	 * @param commandString non-null "normalised" p4 command string.
	 */
	void issuingServerCommand(int key, String commandString);
	
	/**
	 * Report the completion of a Perforce server command, and, as a bonus,
	 * report how long in milliseconds it took from start to finish. Note that
	 * the duration reported may not be absolutely accurate, but is probably
	 * useful for relative comparisons. 
	 * 
	 * @param key -- opaque integer key as returned from the associated 
	 * 				issuingServerCommand call.
	 * @param millisecsTaken -- rough measure of the milliseconds elapsed
	 * 				since the original issuing of this command to its completion.
	 */
	void completedServerCommand(int key, long millisecsTaken);

	/**
	 * Report receiving an info result message from the Perforce server.
	 * These are typically informational messages from the server that
	 * flag files being opened for edit, etc., but they may also include
	 * trigger output on forms submission, etc.
	 * 
	 * @param key -- opaque integer key as returned from the associated 
	 * 				issuingServerCommand call.
	 * @param infoLine non-null info message. May contain newlines.
	 */
	void receivedServerInfoLine(int key, String infoLine);
	
	/**
	 * Report receiving an error message result from the Perforce server.
	 * 
	 * @param key -- opaque integer key as returned from the associated 
	 * 				issuingServerCommand call.
	 * @param errorLine non-null error message. May contain newlines.
	 */
	void receivedServerErrorLine(int key, String errorLine);
	
	/**
	 * Report receiving a server message, which may be an error, an info message,
	 * a warning, etc., but will not typically be an actual result.
	 * This method can be used in place of the separate receivedServerInfoLine
	 * and receivedServerErrorLine for more general usage.
	 * 
	 * @param key opaque integer key as returned from the associated 
	 * 				issuingServerCommand call.
	 * @param genericCode Perforce generic code, as documented in MessageGenericCode.
	 * @param severityCode Perforce severity code, as documented in MessageSeverityCode.
	 * @param message non-null message. May contain newlines.
	 */
	void receivedServerMessage(int key, int genericCode, int severityCode, String message);
}
