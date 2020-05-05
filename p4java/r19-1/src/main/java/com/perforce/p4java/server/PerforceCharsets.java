/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.server;

import com.perforce.p4java.charset.PerforceCharsetProvider;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates Perforce-wide charset information for servers.
 * <p>
 * <p>
 * Unfortunately, support for Unicode and associated charsets is somewhat
 * server- and installation-dependent, and not easy to divine unless you're
 * already talking to a Perforce server -- by which time it's too late. This
 * class is an attempt to make it easier to cut that Gordian knot...
 * <p>
 * <p>
 * Note that the names below are not actually the standard name for the charset
 * in some cases: e.g. UTF-8 should be "utf-8" not "utf8", but we follow the
 * Perforce server's rules here.
 * <p>
 * <p>
 * The following are special cases.
 * <p>
 * <pre>
 * "auto" (Guess a P4CHARSET based on client OS params)
 * "none" (same as unsetting P4CHARSET)
 * </pre>
 */

public class PerforceCharsets {
	// See Perforce C++ i18n code for more info
	// p4/i18n/i18napi.cc
	/**
	 * <pre>
	 * 		auto				(Guess a P4CHARSET based on client OS params)
	 * 		none				(same as unsetting P4CHARSET)
	 * 		utf8 				(UTF-8)
	 * 		iso8859-1			(ISO-8859-1, Latin Alphabet No. 1)
	 * 		utf16-nobom			(UTF-16 client's byte ordering without Byte-Order-Mark)
	 * 		shiftjis			(Windows Japanese - MS932)
	 * 	 	eucjp				(JISX 0201, 0208 and 0212, EUC encoding Japanese)
	 * 		winansi				(Windows code page 1252)
	 * 		winoem				(Windows code page 437)
	 * 		macosroman			(Macintosh Roman)
	 * 		iso8859-15			(ISO-8859-15, Latin Alphabet No. 9)
	 * 		iso8859-5			(ISO-8859-5, Latin/Cyrillic Alphabet)
	 * 		koi8-r				(KOI8-R, Russian)
	 * 		cp1251				(Windows code page 1251 - Cyrillic)
	 * 		utf16le				(UTF-16 with little endian byte ordering)
	 * 		utf16be				(UTF-16 with big endian byte ordering)
	 * 		utf16le-bom			(UTF-16 with little endian Byte-Order-Mark)
	 * 		utf16be-bom			(UTF-16 with big endian Byte-Order-Mark)
	 * 		utf16				(UTF-16 with client's byte ordering and Byte-OrderMark)
	 * 		utf8-bom			(UTF-8 with Byte-Order-Mark)
	 * 		utf32-nobom			(UTF-32 client's byte ordering without Byte-Order-Mark)
	 * 		utf32le				(UTF-32 with little endian byte ordering)
	 * 		utf32be				(UTF-32 with big endian byte ordering)
	 * 		utf32le-bom			(UTF-32 with little endian Byte-Order-Mark)
	 * 		utf32be-bom			(UTF-32 with big endian Byte-Order-Mark)
	 * 		utf32				(UTF-32 with client's byte ordering and Byte-OrderMark)
	 * 		utf8unchecked		(UTF-8 unchecked)
	 * 		utf8unchecked-bom	(UTF-8 unchecked with Byte-Order-Mark)
	 * 		cp949				(Windows code page 949  - Korean)
	 * 		cp936				(Windows code page 936  - Simplified Chinese)
	 * 		cp950				(Windows code page 950  - Traditional Chinese)
	 * 		cp850				(Windows code page 850  - MS-DOS Latin-1)
	 * 		cp858				(Windows code page 858  - Variant of Cp850 with Euro character)
	 * 		cp1253				(Windows code page 1253 - Windows Greek)
	 * 		cp737				(Windows code page 737  - PC Greek)
	 * 		iso8859-7			(ISO-8859-7, Latin/Greek Alphabet)
	 *      cp1250              (Windows code page 1250 - Eastern European)
	 *      cp852               (Windows code page 855  - MS-DOS Latin-2)
	 *      iso8859-2           (ISO-8859-2, Latin Alphabet No. 2)
	 * </pre>
	 */
	/*
	 * Conversions from p4 charsets to Java charsets take from from
     * http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html
     */
	private static final Map<String, String> p4ToJavaCharsets;

	static {
		Map<String, String> map = new HashMap<>();
		map.put("none", "none");
		map.put("utf8", "UTF-8");
		map.put("iso8859-1", "ISO-8859-1");
		map.put("utf16-nobom", "UTF-16");
		// shiftjis (JDK Shift_JIS charset is NOT the same as Perforce shiftjis)
		// Perforce shiftjis (MS932) is a superset of Shift_JIS (SJIS).
		// p4ToJavaCharsets.put(knownCharsets[count++], "Shift_JIS");
		// shiftjis (Perforce implementation of Microsoft code page 932)
		// P4-ShiftJIS is a charset wrapped around the JDK MS932 charset,
		// with some Perforce specific updates.
		// Note: Perforce shiftjis is suppose to be a full MS932 implementation.
		map.put("shiftjis", "P4-ShiftJIS");
		map.put("eucjp", "EUC-JP");
		map.put("winansi", "windows-1252");
		map.put("winoem", "IBM437");
		map.put("macosroman", "x-MacRoman");
		map.put("iso8859-15", "ISO-8859-15");
		map.put("iso8859-5", "ISO-8859-5");
		map.put("koi8-r", "KOI8-R");
		map.put("cp1251", "windows-1251");
		map.put("utf16le", "UTF-16LE");
		map.put("utf16be", "UTF-16BE");
		map.put("utf16le-bom", "UTF-16LE");
		map.put("utf16be-bom", "UTF-16BE");
		map.put("utf16", "UTF-16");
		map.put("utf8-bom", "UTF-8");
		map.put("utf32-nobom", "UTF-32");
		map.put("utf32le", "UTF-32LE");
		map.put("utf32be", "UTF-32BE");
		map.put("utf32le-bom", "UTF-32LE");
		map.put("utf32be-bom", "UTF-32BE");
		map.put("utf32", "UTF-32");
		map.put("utf8unchecked", "UTF-8");
		map.put("utf8unchecked-bom", "UTF-8");
		map.put("cp949", "x-windows-949");
		map.put("cp936", "x-mswin-936");
		map.put("cp950", "x-windows-950");
		map.put("cp850", "IBM850");
		map.put("cp858", "IBM00858");
		map.put("cp1253", "windows-1253");
		map.put("cp737", "x-IBM737");
		map.put("iso8859-7", "ISO-8859-7");
		map.put("cp1250", "windows-1250");
		map.put("cp852", "IBM852");
		map.put("iso8859-2", "ISO-8859-2");
		p4ToJavaCharsets = Collections.unmodifiableMap(map);
	}

	/**
	 * Map for all Perforce charsets that require a BOM when sync'ed to a client.
	 */
	private static final Map<String, Boolean> p4ToClientBOM;

	static {
		Map<String, Boolean> map = new HashMap<>();
		map.put("utf8-bom", true);
		map.put("utf16le-bom", true);
		map.put("utf16be-bom", true);
		map.put("utf32le-bom", true);
		map.put("utf32be-bom", true);
		p4ToClientBOM = Collections.unmodifiableMap(map);
	}

	private static PerforceCharsetProvider p4CharsetProvider
			= new PerforceCharsetProvider();

	/**
	 * Get known P4 charsets
	 *
	 * @return - array of p4 charset names
	 */
	public static String[] getKnownCharsets() {
		Set<String> keySet = p4ToJavaCharsets.keySet();
		String[] charsets = new String[keySet.size()];
		keySet.toArray(charsets);
		return charsets;
	}

	/**
	 * Get the first matching Perforce equivalent charset name for a given Java
	 * charset name. Multiple Perforce charsets can be mapped to a Java charset
	 * (i.e. Perforce "utf8-bom" and "utf8unchecked" are mapped to Java "UTF-8")
	 *
	 * @return - Perforce charset name
	 */
	public static String getP4CharsetName(String javaCharsetName) {
		String p4CharsetName = null;
		if (javaCharsetName != null && !javaCharsetName.isEmpty()) {
			for (Map.Entry<String, String> entry : p4ToJavaCharsets.entrySet()) {
				if (entry.getValue().equalsIgnoreCase(javaCharsetName)) {
					if (entry.getValue().equalsIgnoreCase(javaCharsetName)) {
						p4CharsetName = entry.getKey();
						break;
					}
				}
			}
		}
		return p4CharsetName;
	}

	/**
	 * Get the matching Java charset for a given P4 charset name.
	 *
	 * @return - Java charset
	 */
	public static Charset getP4Charset(String p4CharsetName) {
		String javaCharsetName = getJavaCharsetName(p4CharsetName);
		Charset charset = null;
		if (javaCharsetName != null && !javaCharsetName.isEmpty()) {
			charset = p4CharsetProvider.charsetForName(javaCharsetName);
			if (charset == null) {
				charset = Charset.forName(javaCharsetName);
			}
		}
		return charset;
	}

	/**
	 * Get Java equivalent charset name for a p4 charset name
	 *
	 * @return - Java charset name
	 */
	public static String getJavaCharsetName(String p4CharsetName) {
		String javaCharsetName = null;
		if (p4CharsetName != null && !p4CharsetName.isEmpty()) {
			javaCharsetName = p4ToJavaCharsets.get(p4CharsetName);
		}
		return javaCharsetName;
	}

	public static boolean hasClientBOM(String p4CharsetName) {
		if (p4CharsetName != null && !p4CharsetName.isEmpty()) {
			return p4ToClientBOM.containsKey(p4CharsetName);
		}
		return false;
	}

	/**
	 * Is the P4 charset name specified supported?
	 *
	 * @return - true if supported, false otherwise
	 */
	public static boolean isSupported(String p4CharsetName) {
		boolean supported = false;
		if (p4CharsetName != null && !p4CharsetName.isEmpty()) {
			supported = p4ToJavaCharsets.containsKey(p4CharsetName);
		}
		return supported;
	}
}
