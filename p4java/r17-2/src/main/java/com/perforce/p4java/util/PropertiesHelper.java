package com.perforce.p4java.util;

import com.perforce.p4java.Log;

import java.util.Arrays;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Properties helper class with generally useful methods.
 */
public class PropertiesHelper {

	/**
	 * Return the first property string value found from the passed-in
	 * properties with the specified keys.
	 */
	public static String getProperty(Properties props, String[] keys) {
		return getProperty(props, keys, null);
	}

	/**
	 * Return the first property string value found from the passed-in
	 * properties with the specified keys. If it can't find a value,
	 * then return the passed-in defaultValue.
	 */
	public static String getProperty(Properties props, String[] keys, String defaultValue) {

		if ((props != null) && (keys != null)) {
			String propStr = null;
			for (String key : keys) {
				if (key != null) {
					if (props.get(key) != null) {
						propStr = String.valueOf(props.get(key));
					}
					if (propStr != null) {
						return propStr;
					}
				}
			}
		}

		return defaultValue;
	}

	/**
	 * Return the first property value found as an int, if possible.
	 * If it can't find a value, then return the passed-in defaultValue.
	 */
	public static int getPropertyAsInt(Properties props, String[] keys, int defaultValue) {
		String propStr = getProperty(props, keys, null);
		int retVal = defaultValue;

		if (propStr != null) {
			try {
				retVal = new Integer(propStr);
			} catch (Exception exc) {
				Log.warn("Integer property conversion error; prop name: '"
						+ Arrays.toString(keys) + "'; prop value: "
						+ propStr);
				Log.exception(exc);
			}
		}

		return retVal;
	}

	/**
	 * Return the property value as a long, if possible. If it can't find
	 * a value by the specified key, then return the passed-in defaultValue.
	 */
	public static long getPropertyAsLong(Properties props, String[] keys, long defaultValue) {
		String propStr = getProperty(props, keys, null);
		long retVal = defaultValue;

		if (propStr != null) {
			try {
				retVal = new Long(propStr);
			} catch (Exception exc) {
				Log.warn("Long property conversion error; prop name: '"
						+ Arrays.toString(keys) + "'; prop value: "
						+ propStr);
				Log.exception(exc);
			}
		}

		return retVal;
	}

	public static String getPropertyByKeys(Properties props, String key, String alternativeKey, String defaultValue) {
		return props.getProperty(key, props.getProperty(alternativeKey, defaultValue));
	}

	public static String getPropertyByKeys(Properties props, String key, String alternativeKey) {
		return getPropertyByKeys(props, key, alternativeKey, null);
	}

	public static boolean isExistProperty(Properties props, String key, String alternativeKey, boolean defaultValue) {
		String value = props.getProperty(key, props.getProperty(alternativeKey));
		if (isNotBlank(value)) {
			return value.equalsIgnoreCase("true");
		}
		return defaultValue;
	}
}
