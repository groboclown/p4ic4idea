/**
 * Copyright 2013 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.helper;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps track of the user's login/out counts per RPC server.
 */
public class RpcUserAuthCounter {

	/** The line separator */
	private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
	
	/** The counter map (initial capacity of 8, load factor of 0.9, single shard segment). */
	private final ConcurrentMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<String, AtomicInteger>(8, 0.9f, 1);

	/**
	 * Increment and get the count for the specified user auth prefix.
	 *
	 * @param authPrefix the auth prefix
	 * @return the int
	 */
	public int incrementAndGet(String authPrefix) {

		if (authPrefix != null) {
			this.counterMap.putIfAbsent(authPrefix, new AtomicInteger(0));
			return counterMap.get(authPrefix).incrementAndGet();
		}

		return 0;
	}

	/**
	 * Decrement and get the count for the specified user auth prefix.
	 *
	 * @param authPrefix the auth prefix
	 * @return the int
	 */
	public int decrementAndGet(String authPrefix) {

		if (authPrefix != null) {
			if (counterMap.get(authPrefix) != null) {
				return counterMap.get(authPrefix).decrementAndGet();
			}
		}

		return 0;
	}

	/**
	 * Gets the count for the specified user auth prefix.
	 *
	 * @param authPrefix the auth prefix
	 * @return the count
	 */
	public int getCount(String authPrefix) {

		if (authPrefix != null) {
			if (counterMap.get(authPrefix) != null) {
				return counterMap.get(authPrefix).intValue();
			}
		}

		return 0;
	}

	/**
	 * Clears all counts for the RPC server.
	 */
	public void clearCount() {
	
		this.counterMap.clear();
	}

	/**
	 * Returns the string value of the user auth counter.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();

		Iterator<Entry<String, AtomicInteger>> it = this.counterMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, AtomicInteger> entry = (Map.Entry<String, AtomicInteger>)it.next();
	        if (entry != null && entry.getKey() != null && entry.getValue() != null) {
	        	sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(LINE_SEPARATOR);
	        }
	    }

	    return sb.toString();
	}
}
