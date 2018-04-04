/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc.func.helper;

import java.util.Random;

/**
 * Mildly-useful String and String-related helper methods that
 * should probably be factored out elsewhere...
 */

public class StringHelper {
	
	private static final Random rand = new Random(System.currentTimeMillis());
	
	/**
	 * Return the integer value of the passed-in char interpreted
	 * as a hex digit.
	 * 
	 * FIXME: return -1 on bad conversion -- HR.
	 */
	
	public static int hexcharToInt(char c) {
		return c - ( c > '9' ? ( c >= 'a' ? 'a' - 10 : 'A' - 10 ) : '0' );
	}
	
	/**
	 * Return a plausibly-random number string in hex form.
	 * Used mostly for temp filename generation.<p>
	 * 
	 * Not (yet) synchronised as unlikely to be problem
	 * with threads and contention.
	 */

	public static String getRandomHexString() {
		long n = rand.nextLong();
        if (n == Long.MIN_VALUE) {
            n = 0;      // corner case
        } else {
            n = Math.abs(n);
        }
        
        return Long.toString(n, 16);
	}
}
