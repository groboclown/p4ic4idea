/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet.helper;

import java.util.Map;

/**
 * Super class representing the rule for skipping the charset conversion of the
 * RPC packet field values (bytes). Leave those field values as bytes instead of
 * converting them to strings. <p>
 */

public abstract class RpcPacketFieldRule {

	/** The key for storing the field pattern in a command map. */
	public static final String FIELD_PATTERN = "fieldPattern";
	
	/** The key for storing the start field name in a command map. */
	public static final String START_FIELD = "startField";

	/** The key for storing the stop field name in a command map. */
	public static final String STOP_FIELD = "stopField";
	
	/**
	 * If true, skip charset conversion; leave the value as is in bytes.
	 */
	protected boolean skipConversion = false;

	/**
	 * Factory static method to create an instance of a subclass based on the
	 * content of a command map. Note that the instance creation will be
	 * processed in the order listed below.<p>
	 * 
	 * The existing of a FIELD_PATTERN key with a string value will create a
	 * RpcPacketFieldPatternRule object.<p>
	 * 
	 * The existing of both the START_FIELD and STOP_FIELD keys with string
	 * values will create a RpcPacketFieldRangeRule object.
	 */
	public static RpcPacketFieldRule getInstance(Map<String, Object> cmdMap) {
		if (cmdMap.containsKey(FIELD_PATTERN)) {
			return new RpcPacketFieldPatternRule(
					(String) cmdMap.get(FIELD_PATTERN));
		}

		if (cmdMap.containsKey(START_FIELD) && cmdMap.containsKey(STOP_FIELD)) {
			return new RpcPacketFieldRangeRule(
					(String) cmdMap.get(START_FIELD),
					(String) cmdMap.get(STOP_FIELD));
		}

		return null;
	}
	
	/**
	 * Updates the conversion rule.
	 * 
	 * @param fieldName
	 *            the field name
	 */
	public abstract void update(String fieldName);

	/**
	 * Checks if is skip conversion.
	 * 
	 * @return true, if is skip conversion
	 */
	public boolean isSkipConversion() {
		return skipConversion;
	}
}
