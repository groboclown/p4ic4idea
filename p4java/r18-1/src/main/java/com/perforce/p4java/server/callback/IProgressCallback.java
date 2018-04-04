/**
 * 
 */
package com.perforce.p4java.server.callback;

/**
 * Provides a simple server command and command progress callback
 * and control interface for P4Java consumers.<p>
 * 
 * The intention here is to provide consumers with a very simple way to
 * measure command progress and to stop a command mid-stream. The underlying
 * P4Java transport mechanism calls the tick() callback every time a "significant"
 * part of a command is finished, and the callback return value determines whether the
 * command should continue normally or be terminated cleanly ASAP.<p>
 * 
 * In context, the significant event is almost always the
 * start of processing for the next file spec to be processed (in a sync, a
 * long-running files or fstat command, a submit, etc), and the tick marker,
 * if it's not null, will almost always be the full client or depot path of the
 * file about to be processed, or the job or changelist about to be processed, etc.
 * Note that only a few commands can get the file path back in the payload,
 * so you cannot rely on it being non-null; however, if it's not null, it's
 * likely to be of interest to the end-user.<p>
 * 
 * This scheme means the consumer should have some basic idea of the likely
 * order of magnitude of the number of ticks that will be generated, and
 * to be able to generate a suitable visual progress bar or popup using
 * this information.<p>
 * 
 * This callback scheme is only properly implemented in the RPC protocol
 * version of P4Java, and only commands likely to be long-running and / or
 * "interesting" currently participate in this scheme; these are typically commands
 * like submit, sync, files, and fstat, but no guarantee is given yet for
 * any particular command.<p>
 * 
 * NOTE: under error conditions it is not guaranteed that the stop() method
 * will be called; consumers should clear the callback's state manually under
 * these conditions.<p>
 * 
 * NOTE: you <b>must</b> ensure that there are no threading or reentrancy issues
 * with your implementation of this interface, and that calling any of the methods
 * here will not cause the caller (P4Java) to block or spend too much time processing
 * the callback. The callback is called in the middle of Perforce client / server
 * communications in the thread context of the protocol handler, and any significant
 * delay here may cause serious performance issues or even botched commands.<p>
 */

public interface IProgressCallback {

	/**
	 * A new tick sequence has begun. Always issued at the start of a new user-issued
	 * command. The integer key passed into this method should be used as an opaque key
	 * for associated tick and stop events; it is guaranteed to have the same value as
	 * used with command callbacks (see ICommandCallback) for the same command if
	 * command callbacks are enabled (this allows consumers to correlate progress and
	 * results by key matching).<p>
	 * 
	 * In any case, whether command callbacks are enabled or not, this key will be unique
	 * across all calls (simultaneous or otherwise) to a given IServer, but is not
	 * guaranteed to be unique across an entire P4Java instance.
	 * 
	 * @param key -- opaque integer key for correlation with subsidiary calls.
	 */
	void start(int key);
	
	/**
	 * Signal the completion of a significant event, and poll the callback for
	 * whether to continue the command. If this callback returns false, the underlying
	 * protocol will do its best to stop the current command dead in its tracks and
	 * clean up.<p>
	 * 
	 * Note that command cancellation done through this tick method is not guaranteed to
	 * have any effect at all, nor are the side-effects of such a cancellation necessarily
	 * predictable or safe. In general the default RPC implementation attempts to do a
	 * reasonable job of things, but the command-line and RPC-NTS implementations ignore
	 * any cancel notices completely.<p>
	 * 
	 * Note also that command cancellation does <i>not</i> work for connection delays;
	 * current implementations force users to wait for connection timeouts. This will
	 * probably change in future releases.
	 * 
	 * @param key -- opaque integer key as given by the associated start call.
	 * @param tickMarker possibly-interesting possibly non-null tick payload;
	 * 			if not null, will normally be something meaningful to the consumer
	 * 			like a file path (depot or client), or a job ID, or a changelist
	 * 			ID, etc.
	 * @return true if you want the command to continue; false otherwise.
	 */
	boolean tick(int key, String tickMarker);
	
	/**
	 * The current tick sequence has finished. Usually issued when the user command
	 * has completed. Not guaranteed to be delivered when an error has occurred in
	 * the command processing.
	 * 
	 * @param key -- opaque integer key as given by the associated start call.
	 */
	void stop(int key);
}
