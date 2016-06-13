/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.server;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates Perforce-wide charset information for servers.
 * <p>
 * 
 * Unfortunately, support for Unicode and associated charsets is somewhat
 * server- and installation-dependent, and not easy to divine unless you're
 * already talking to a Perforce server -- by which time it's too late. This
 * class is an attempt to make it easier to cut that Gordian knot...
 * <p>
 * 
 * Note that the names below are not actually the standard name for the charset
 * in some cases: e.g. UTF-8 should be "utf-8" not "utf8", but we follow the
 * Perforce server's rules here.
 * 
 *
 */

public class PerforceCharsets {

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
	 * </pre>
	 */
	private static final String[] knownCharsets = {
	    "auto",
	    "none",
	    "utf8",
	    "iso8859-1",
	    "utf16-nobom",
	    "shiftjis",
	    "eucjp",
	    "winansi",
	    "winoem",
	    "macosroman",
	    "iso8859-15",
	    "iso8859-5",
	    "koi8-r",
	    "cp1251",
	    "utf16le",
	    "utf16be",
	    "utf16le-bom",
	    "utf16be-bom",
	    "utf16",
	    "utf8-bom",
	    "utf32-nobom",
	    "utf32le",
	    "utf32be",
	    "utf32le-bom",
	    "utf32be-bom",
	    "utf32",
	    "utf8unchecked",
	    "utf8unchecked-bom",
	    "cp949",
	    "cp936",
	    "cp950",
	    "cp850",
	    "cp858",
	    "cp1253",
	    "cp737",
	    "iso8859-7"
	};
	
	private static final Map<String, String> p4ToJavaCharsets;

	static {
		// Conversions from p4 charsets to Java charsets take from from
		// http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html
		p4ToJavaCharsets = new HashMap<String, String>();

		int count = 0;
		
		// auto
		p4ToJavaCharsets.put(knownCharsets[count++], "auto");

		// none
		p4ToJavaCharsets.put(knownCharsets[count++], "none");

		// utf8
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-8");

		// iso8859-1
		p4ToJavaCharsets.put(knownCharsets[count++], "ISO-8859-1");

		// utf16-nobom
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-16");

		// shiftjis (JDK Shift_JIS charset is NOT the same as Perforce shiftjis)
		// Perforce shiftjis (MS932) is a superset of Shift_JIS (SJIS).
		//p4ToJavaCharsets.put(knownCharsets[count++], "Shift_JIS");

		// shiftjis (Perforce implementation of Microsoft code page 932)
		// P4-ShiftJIS is a charset wrapped around the JDK MS932 charset,
		// with some Perforce specific updates.
		// Note: Perforce shiftjis is suppose to be a full MS932 implementation.
		p4ToJavaCharsets.put(knownCharsets[count++], "P4-ShiftJIS");
		
		// eucjp
		p4ToJavaCharsets.put(knownCharsets[count++], "EUC-JP");

		// winansi
		p4ToJavaCharsets.put(knownCharsets[count++], "windows-1252");

		// winoem
		p4ToJavaCharsets.put(knownCharsets[count++], "IBM437");

		// macosroman
		p4ToJavaCharsets.put(knownCharsets[count++], "x-MacRoman");

		// iso8859-15
		p4ToJavaCharsets.put(knownCharsets[count++], "ISO-8859-15");

		// iso8859-5
		p4ToJavaCharsets.put(knownCharsets[count++], "ISO-8859-5");

		// koi8-r
		p4ToJavaCharsets.put(knownCharsets[count++], "KOI8-R");
		
		// cp1251
		p4ToJavaCharsets.put(knownCharsets[count++], "windows-1251");

		// utf16le
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-16LE");

		// utf16be
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-16BE");

		// utf16le-bom
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-16LE");

		// utf16be-bom
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-16BE");
		
		// utf16
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-16");

		// utf8-bom
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-8");
		
		// utf32-nobom
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-32");

		// utf32le
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-32LE");

		// utf32be
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-32BE");

		// utf32le-bom
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-32LE");

		// utf32be-bom
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-32BE");

		// utf32
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-32");

		// utf8unchecked
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-8");
		
		// utf8unchecked-bom
		p4ToJavaCharsets.put(knownCharsets[count++], "UTF-8");
		
		// cp949
		p4ToJavaCharsets.put(knownCharsets[count++], "x-windows-949");

		// cp936
		p4ToJavaCharsets.put(knownCharsets[count++], "x-mswin-936");
		
		// cp950
		p4ToJavaCharsets.put(knownCharsets[count++], "x-windows-950");

		// cp850
		p4ToJavaCharsets.put(knownCharsets[count++], "IBM850");

		// cp858
		p4ToJavaCharsets.put(knownCharsets[count++], "IBM00858");
		
		// cp1253
		p4ToJavaCharsets.put(knownCharsets[count++], "windows-1253");

		// cp737
		p4ToJavaCharsets.put(knownCharsets[count++], "x-IBM737");

		// iso8859-7
		p4ToJavaCharsets.put(knownCharsets[count++], "ISO-8859-7");
}

	/**
	 * Get known P4 charsets
	 * 
	 * @return - array of p4 charset names
	 */
	public static String[] getKnownCharsets() {
		return knownCharsets;
	}

	/**
	 * Get the first matching Perforce equivalent charset name for a given Java
	 * charset name. Multiple Perforce charsets can be mapped to a Java charset
	 * (i.e. Perforce "utf8-bom" and "utf8unchecked" are mapped to Java "UTF-8")
	 * 
	 * @param javaCharsetName
	 * @return - Perforce charset name
	 */
	public static String getP4CharsetName(String javaCharsetName) {
		String p4CharsetName = null;
		if (javaCharsetName != null) {
			for(Map.Entry<String, String> entry : p4ToJavaCharsets.entrySet()) {
				if (entry.getValue() != null) {
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
	 * Get Java equivalent charset name for a p4 charset name
	 * 
	 * @param p4CharsetName
	 * @return - Java charset name
	 */
	public static String getJavaCharsetName(String p4CharsetName) {
		String javaCharsetName = null;
		if (p4CharsetName != null) {
			javaCharsetName = p4ToJavaCharsets.get(p4CharsetName);
		}
		return javaCharsetName;
	}

	/**
	 * Get Java equivalent charset name for a p4 charset index (knownCharsets)
	 * 
	 * @param p4CharsetIndex
	 * @return - Java charset name
	 */
	public static String getJavaCharsetName(int p4CharsetIndex) {
		String javaCharsetName = null;
		if (p4CharsetIndex >=0 && p4CharsetIndex < knownCharsets.length) {
			javaCharsetName = p4ToJavaCharsets.get(knownCharsets[p4CharsetIndex]);
		}
		return javaCharsetName;
	}

	/**
	 * Get Java equivalent charset for a p4 charset index (knownCharsets)
	 * 
	 * @param p4CharsetIndex
	 * @return - Java charset
	 */
	public static Charset getJavaCharset(int p4CharsetIndex) {
		Charset javaCharset = null;
		if (p4CharsetIndex >=0 && p4CharsetIndex < knownCharsets.length) {
			javaCharset = Charset.forName(p4ToJavaCharsets.get(knownCharsets[p4CharsetIndex]));
		}
		return javaCharset;
	}

	/**
	 * Is the P4 charset name specified supported?
	 * 
	 * @param p4CharsetName
	 * @return - true if supported, false otherwise
	 */
	public static boolean isSupported(String p4CharsetName) {
		boolean supported = false;
		if (p4CharsetName != null) {
			supported = p4ToJavaCharsets.containsKey(p4CharsetName);
		}
		return supported;
	}
}
