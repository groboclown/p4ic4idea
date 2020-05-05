/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java;

import java.nio.charset.Charset;

/**
 * Provides a centralized place to define or specify the various default and
 * working charsets used in the P4Java implementation. Do not change the values
 * here unless you're absolutely certain you know what you're doing, as the
 * definitions here are used deep in the implementation layers to decode RPC
 * packet headers, etc., as well as more normal file contents and command
 * encodings. 
 */

public class CharsetDefs {

	/**
	 * The default charset used for command and rpc header / key encodings when nothing
	 * else has been specified.<p>
	 * 
	 * This charset must have US ASCI as a proper subset, but other than that,
	 * it's not always clear what's the best default charset. Contenders include
	 * ISO-8859-1, ISO-8859-15, windows-1252 (aka winansi); with minor issues, they'll
	 * all work, but those minor issues include things like euro sign misplacement, odd
	 * ligature screwups, etc. In the absence of anything better (or anything defined
	 * through the properties system), we currently use the JVM's default charset, since
	 * it's at least available, but this is certainly subject to further research
	 * and / or rethinking.<p>
	 * 
	 * Note that despite being tagged a constant, it's actually set dynamically in the
	 * static constructor below.
	 */
	public static Charset DEFAULT = Charset.defaultCharset();

	/**
	 * The canonical name of the default charset. Actually set in the static class constructor.
	 */
	public static String DEFAULT_NAME = DEFAULT.name();

	/**
	 * UTF-8 charset. Used for Unicode encodings between the Perforce server and
	 * client. Do not change this...
	 */
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Canonical name of the UTF-8 charset we use.
	 */
	public static final String UTF8_NAME = UTF8.name();
	
	/**
	 * UTF-16 charset.
	 */
	public static final Charset UTF16 = Charset.forName("UTF-16");

	/**
	 * Canonical name of the UTF-16 charset we use.
	 */
	public static final String UTF16_NAME = UTF16.name();

	/**
	 * The current "local" JVM charset, as taken from the JVM itself. This is
	 * the charset that will be used by the JVM to encode and decode strings if
	 * no other encoding is supplied.
	 */
	public static final Charset LOCAL = Charset.defaultCharset();

	/**
	 * The canonical name of the local JVM charset.
	 */
	public static final String LOCAL_NAME = LOCAL.name();
	
	static {
		String defaultCharsetName = System.getProperty(PropertyDefs.DEFAULT_CHARSET_KEY);
		
		if (defaultCharsetName != null) {
			try {
				DEFAULT = Charset.forName(defaultCharsetName);
			} catch (Exception exc) {
				Log.warn("Unable to set P4Java default character set to " + defaultCharsetName
								+ "; using JVM default charset " + Charset.defaultCharset().name()
								+ " instead");
				Log.exception(exc);
				DEFAULT = Charset.defaultCharset();
			}
			DEFAULT_NAME = DEFAULT.name();
		}
	}

}
