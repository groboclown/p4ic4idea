/**
 * Copyright 2013 Perforce Software. All Rights Reserved.
 */
package com.perforce.p4java.server.callback;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface used to filter out individual key/value pairs from the results map
 * using the execMapCmd and/or other command methods. Users can implement this
 * interface if they want to skip certain key/value pairs or all subsequence
 * pairs from getting into the results map.<p>
 * 
 * The callbacks occur during critical sections of the underlying RPC protocol
 * handlers and that callback implementers should ensure that the callback
 * methods return as quickly as possible and that they do not themselves call
 * back into the server layer.<p>
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
 */

public interface IFilterCallback {
	
	/**
	 * Per every RCP packet received from the Perforce server, callback to the
	 * consumer to reset any counters and other variables.
	 * 
	 * @throws P4JavaException if any problem that should stop normal command processing.
	 */
	void reset() throws P4JavaException;
	
	/**
	 * When called, this method signals to the consumer that an individual
	 * key/value pair is being processed for the current received RPC packet
	 * from the Perforce server. The consumer needs to make a decision to skip
	 * or keep this key/value pair in the results map. The consumer can also
	 * decide to skip all (with some exceptions) subsequent pairs for the rest
	 * of the current received RPC packet.<p>
	 * 
	 * Consumers are assumed to be able to make sense of the key/value pair
	 * quickly and on their own -- no help is (currently...) given to consumers
	 * with this.<p>
	 * 
	 * NOTE: this method is called in a critical section of the underlying P4Java
	 * client / server protocol handler, and must not cause any undue delays or
	 * calls back into the server. Serious protocol-level errors and / or major
	 * performance problems can result from even minor implementation errors with
	 * this method (and these problems can be extremely difficult to debug), so
	 * you are on your own if you use this feature.
	 * 
	 * @param key possibly-null key (string) part of the key/value pair.
	 * @param value possibly-null value (object) part of the key/value pair.
	 * @param skipSubsequent reference parameter passed-in from the RPC layer.
	 * 				Set this to true if the callback wants to skip subsequent
	 * 				key/value pairs for the rest of the current received RPC packet.
	 * 				Note that some RPC protocol related key/value pairs (i.e.
	 * 				'func', etc.) are not skipped.
	 * @return true if the callback wants skip this key/value pair from getting
	 * 				into the results map; false otherwise (keep this key/value
	 * 				pair in the results map).
	 * @throws P4JavaException if any problem that should stop normal command processing.
	 */
	boolean skip(String key, Object value, final AtomicBoolean skipSubsequent) throws P4JavaException;

	/**
	 * Per every RCP packet received from the Perforce server, callback to the
	 * consumer to get a map of do-not-skip keys for informing the RPC layer not
	 * to skip them.<p>
	 * 
	 * Only the map keys are used for lookups; the map values are not used. This
	 * is normally used coincide with the 'skipSubsequent' callback reference
	 * parameter set to true in the 'skip()' method.<p>
	 * 
	 * NOTE: since this method is called frequently, the do-not-skip keys map
	 * should probably be constructed once and outside of this method. Maybe a
	 * good place to build this map is in a constructor or an initialization
	 * method.
	 * 
	 * @return possibly-null map of do-not-skip keys.
	 * @throws P4JavaException if any problem that should stop normal command processing.
	 */
	Map<String, String> getDoNotSkipKeysMap() throws P4JavaException;

}
