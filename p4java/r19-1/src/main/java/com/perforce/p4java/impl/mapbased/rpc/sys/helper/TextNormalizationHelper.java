/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import com.perforce.p4java.Log;

import java.lang.reflect.Method;

/**
 * This helper class dynamically loads the "java.text.Normalizer" in JDK 6 or
 * above using reflection. It provides the method normalize which transforms
 * Unicode text into an equivalent composed or decomposed form. The normalize
 * method supports the standard normalization forms described in Unicode
 * Standard Annex #15 — Unicode Normalization Forms.
 */
public class TextNormalizationHelper {

	public static final String NORMALIZER_CLASS_NAME = "java.text.Normalizer";
	public static final String NORMALIZER_FORM_CLASS_NAME = "java.text.Normalizer$Form";

	public static final String NORMALIZER_NORMALIZE_METHOD_NAME = "normalize";
	public static final String NORMALIZER_IS_NORMALIZED_METHOD_NAME = "isNormalized";

	private static Class<?> normalizerClass = null;
	private static Class<?> normalizerFormClass = null;

	private static Method normalizeMethod = null;
	private static Method isNormalizedMethod = null;

	private static Object[] normalizerForms = null;
	private static Object nfcNormalization = null;
	
	private static boolean normalizeCapable = false;
	
	static {
		Log.info("Checking this Java for text normalization support...");

		try {
			// Find classes
			normalizerClass = Class.forName(NORMALIZER_CLASS_NAME);
			normalizerFormClass = Class.forName(NORMALIZER_FORM_CLASS_NAME);

			// Find methods
			normalizeMethod = normalizerClass
					.getDeclaredMethod(NORMALIZER_NORMALIZE_METHOD_NAME, CharSequence.class,
                            normalizerFormClass);
			isNormalizedMethod = normalizerClass.getDeclaredMethod(
					NORMALIZER_IS_NORMALIZED_METHOD_NAME, CharSequence.class,
                    normalizerFormClass);

			// Normalizer forms - enum constants in order of declaration.
			normalizerForms = normalizerFormClass.getEnumConstants();

			// NFC normalization form
            for (int i = 0; i < normalizerForms.length; i++) {
                if (normalizerForms[i].toString().equals("NFC")) {
                	nfcNormalization = normalizerForms[i];
                }				
            }
			
			// Normalization capable?
			if (normalizeMethod != null && isNormalizedMethod != null
					&& normalizerForms != null && nfcNormalization != null) {
					normalizeCapable = true;
				Log.info("It seems this Java supports text normalization.");
			}

		} catch (ClassNotFoundException cnfe) {
			Log.error("Unable to find class: " + cnfe.getLocalizedMessage());
			Log.exception(cnfe);
		} catch (NoSuchMethodException nsme) {
			Log.error("No such method for class: " + nsme.getLocalizedMessage());
			Log.exception(nsme);
		} catch (Throwable thr) {
			Log.error("Unexpected exception introspecting class: "
					+ thr.getLocalizedMessage());
			Log.exception(thr);
		}
	}

	/**
	 * Checks if is normalization capable.
	 * 
	 * @return true, if is normalization capable
	 */
	public static boolean isNormalizationCapable() {
		return normalizeCapable;
	}

	/**
	 * Tests whether the text is normalized.
	 * 
	 * @param text
	 *            the text to be checked for normalization
	 * @return true if the text is normalized; false otherwise.
	 */
	public static boolean isNormalized(String text) {
		if (normalizeCapable && text != null) {
			try {
				return (Boolean) isNormalizedMethod.invoke(null, text,
						nfcNormalization);
			} catch (Throwable thr) {
				Log.error("Unexpected exception invoking method: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return false;
	}

	/**
	 * Normalizes the text.
	 * 
	 * @param text
	 *            the text to be normalized
	 * @return string the normalized text
	 */
	public static String normalize(String text) {
		if (normalizeCapable && text != null) {
			try {
				return (String) normalizeMethod.invoke(null, text,
						nfcNormalization);
			} catch (Throwable thr) {
				Log.error("Unexpected exception invoking method: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return null;
	}
}
