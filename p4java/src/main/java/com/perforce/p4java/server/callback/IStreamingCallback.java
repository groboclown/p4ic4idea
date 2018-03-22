/**
 * 
 */
package com.perforce.p4java.server.callback;

import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface used to communicate individual results to users using the
 * execStreaminMapCommand and / or other streaming command methods.<p>
 * 
 * Users must implement this interface if they're using the streaming
 * command interface, but little or no restrictions are placed on implementations
 * beyond noting that the callbacks occur during critical sections of the
 * underlying RPC protocol handlers and that callback implementers should
 * ensure that the callback methods return as quickly as possible and that
 * they do not themselves call back into the server layer.<p>
 * 
 * Note that the ability to have the command stopped as a result of a callback
 * returning false is mentioned but not promised -- the RPC layer may
 * continue calling the callbacks (or not), or it may stop calling the callbacks
 * (or not); and in any case, it may still continue to field results from the
 * Perforce server even though it is no longer calling the relevant callback.
 * The reasons for this are mostly due to the protocol itself, and users
 * should write their callbacks accordingly and robustly. In pathological
 * cases it is even possible that a callback's endResults method is never
 * called, so implementers need to also plan for that.<p>
 * 
 * If any of the callback methods throw a P4JavaException the system will
 * rethrow the same exception from the main streaming command method after
 * attempting to clean up; similarly with P4JavaErrors. In either case, while
 * the main method has returned and no more results are available, the state
 * of the underlying protocol layers is not defined, and you should proceed
 * carefully from that point on (in the case of 'ordinary' P4JavaExceptions,
 * the underlying state is actually typically stable and OK for continuing on,
 * with a possible delay due to non-terminated results still being sent to the
 * API; however, all bets are off with the P4JavaError case).<p>
 * 
 * NOTE: 'streaming' here has nothing at all to do with Perforce 'streams', which
 * are (or will be) implemented elsewhere.
 */

public interface IStreamingCallback {
	
	/**
	 * When called, this signals to the consumer that a command has been
	 * issued to the Perforce server.
	 * 
	 * @param key opaque integer key as passed to the associated streaming method.
	 * @return true if the callback wants the command to continue normally;
	 * 				false otherwise.
	 * @throws P4JavaException if any problem that should stop normal command processing.
	 */
	boolean startResults(int key) throws P4JavaException;
	
	/**
	 * When called, this signals to the consumer that a command has been
	 * completed at the Perforce server, and that no more results will be available
	 * from the associated command.
	 * 
	 * @param key opaque integer key as passed to the associated streaming method.
	 * @return true if the callback wants the command to continue normally;
	 * 				false otherwise (ignored with this method).
	 * @throws P4JavaException if any problem that should stop normal command processing.
	 */
	boolean endResults(int key) throws P4JavaException;
	
	/**
	 * When called, this method signals to the consumer that an individual result
	 * or error / warning / info message is available in map form from the Perforce
	 * server.<p>
	 * 
	 * Consumers are assumed to be able to make sense of (i.e. deserialize) the
	 * map payload quickly and on their own -- no help is (currently...) given
	 * to consumers with this, although there are many suitable map-based
	 * constructors available for the common object types within the P4Java
	 * impl packages.<p>
	 * 
	 * NOTE: this method is called in a critical section of the underlying P4Java
	 * client / server protocol handler, and must not cause any undue delays or
	 * calls back into the server. Serious protocol-level errors and / or major
	 * performance problems can result from even minor implementation errors with
	 * this method (and these problems can be extremely difficult to debug), so
	 * you are on your own if you use this streaming feature.<p>
	 * 
	 * @param resultMap non-null map of results from the server. The map may represent
	 * 			a single result from the server, or a single status (error / info, etc.)
	 * 			message either from the server or from the lower levels of the P4Java
	 * 			implementation. This means there may be more than one class to handleResult
	 * 			for each 'result' -- consumers must be able to cope with this.
	 * @param key opaque integer key as passed to the associated streaming method.
	 * @return true if the callback wants the command to continue normally;
	 * 				false otherwise (ignored with this method).
	 * @throws P4JavaException if any problem that should stop normal command processing.
	 */
	boolean handleResult(Map<String, Object> resultMap, int key) throws P4JavaException;
}
