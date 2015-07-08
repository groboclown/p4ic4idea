/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet.helper;

import java.util.regex.Pattern;

/**
 * Defines the rule for pattern matching the field names.
 */

public class RpcPacketFieldPatternRule extends RpcPacketFieldRule {

	/**
	 * The regex pattern for matching fields which the field values (bytes) will
	 * not be converted to strings. <p>
	 * 
	 * Pattern object to be compiled with the passed-in field pattern parameter.
	 * It is reusable and thus better performance overall.
	 */
	protected Pattern fieldPattern = null;
	
	/**
	 * Constructor for creating a field pattern rule.
	 * 
	 * @param fieldRegex
	 *            the field regex, not null
	 */
	public RpcPacketFieldPatternRule(String fieldRegex) {
		if (fieldRegex == null) {
			throw new IllegalArgumentException(
					"Null fieldRegex passed to the RpcPacketFieldRule constructor.");
		}
		this.fieldPattern = Pattern.compile(fieldRegex);
	}

	/**
	 * Updates the conversion rule.
	 * 
	 * @param fieldName
	 *            the field name
	 */
	public void update(String fieldName) {
		if (fieldName != null) {
			skipConversion = fieldPattern.matcher(fieldName).matches();
		}
	}

	/**
	 * Gets the field pattern.
	 * 
	 * @return the field pattern
	 */
	public Pattern getFieldPattern() {
		return fieldPattern;
	}
}
