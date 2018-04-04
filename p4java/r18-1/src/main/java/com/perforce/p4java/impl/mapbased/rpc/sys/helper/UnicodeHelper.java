/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * Provides default and / or bridged (to Eclipse, etc.) Unicode helper
 * methods. May disappear or be moved in a future refactoring.
 * 
 *
 */

public class UnicodeHelper {

	/**
	 * Try to determine whether a byte buffer's character encoding is that of the
	 * passed-in charset. Uses inefficient
	 * heuristics that will be revisited when we're more familiar with likely
	 * usage patterns.
	 * 
	 * Note this has been heavily changed since inception and will
	 * almost certainly disappear in the 10.x timeframe -- HR.
	 */
	public static boolean inferCharset(byte[] bytes, int bytesRead, Charset clientCharset) {
		ByteBuffer byteBuf = ByteBuffer.wrap(bytes, 0, bytesRead);
		CharBuffer charBuf = CharBuffer.allocate(byteBuf.capacity() * 2);
		
		if (clientCharset != null) {
			CharsetDecoder decoder = clientCharset.newDecoder();
			decoder.onMalformedInput(CodingErrorAction.REPORT);
			decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
			CoderResult coderResult = decoder.decode(byteBuf, charBuf, false);
			if (coderResult != null) {
				if (coderResult.isError()) {
					// Wasn't this one...
					return false;
				} else {
					return true;	// Still only *probably* true, dammit...
				}
			}
		}
		
		return true;
	}
}
