/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet.helper;


/**
 * Defines the rule for a range of fields with a start field (inclusive) and a stop
 * field (non-inclusive).
 */

public class RpcPacketFieldRangeRule extends RpcPacketFieldRule {

	/**
	 * The start field (inclusive) marking the beginning of a series of fields
	 * which the field values (bytes) will not be converted to strings. <p>
	 * 
	 * Note: this applies to the "RANGE" rule type only.
	 */
	protected String startField = null;

	/**
	 * The stop field (non-inclusive) marking the end of a series of fields
	 * which the field values (bytes) will not be converted to strings. <p>
	 * 
	 * Note: this applies to the "RANGE" rule type only.
	 */
	protected String stopField = null;

	/**
	 * Constructor for creating a field range rule.
	 * 
	 * @param startField
	 *            the start field, not null.
	 * @param stopField
	 *            the stop field, not null.
	 */
	public RpcPacketFieldRangeRule(String startField, String stopField) {
		if (startField == null) {
			throw new IllegalArgumentException(
					"Null startField passed to the RpcPacketFieldRule constructor.");
		}
		if (stopField == null) {
			throw new IllegalArgumentException(
					"Null stopField passed to the RpcPacketFieldRule constructor.");
		}
		this.startField = startField;
		this.stopField = stopField;
	}

	/**
	 * Updates the conversion rule.
	 * 
	 * @param fieldName
	 *            the field name
	 */
	public void update(String fieldName) {
		if (fieldName != null) {
			if (skipConversion) {
				if (stopField.equalsIgnoreCase(fieldName)) {
					skipConversion = false;
				}
			} else {
				if (startField.equalsIgnoreCase(fieldName)) {
					skipConversion = true;
				}
			}
		}
	}

	/**
	 * Gets the start field.
	 * 
	 * @return the start field
	 */
	public String getStartField() {
		return startField;
	}

	/**
	 * Gets the stop field.
	 * 
	 * @return the stop field
	 */
	public String getStopField() {
		return stopField;
	}
}
