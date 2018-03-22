/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.charset;

import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * CharsetProvider implementation which makes available the charsets provided by
 * Perforce.
 */
public class PerforceCharsetProvider extends CharsetProvider {
	private static final String CHARSET_NAME = "P4ShiftJIS";
	private Charset charset = null;
  private static Map<String, Charset> charsetNameMap = new HashMap<>();

	/**
	 * Instantiate a charset object.
	 */
	public PerforceCharsetProvider() {
      charset = new PerforceShiftJISCharset(CHARSET_NAME, new String[]{"P4-ShiftJIS", "p4shiftjis", "p4-shiftjis"});
      charsetNameMap.put(charset.name(), charset);
      for (Iterator<String> aliases = charset.aliases().iterator(); aliases.hasNext(); ) {
        charsetNameMap.put(aliases.next(), charset);
      }
	}

	/**
	 * Called by Charset static methods to find a particular named Charset.
	 */
	public Charset charsetForName(String charsetName) {
		if (charsetNameMap.containsKey(charsetName)) {
			return (this.charset);
		}
		return (null);
	}

	/**
	 * Return an Iterator over the set of Charset objects we provide.
	 */
	public Iterator<Charset> charsets() {
      HashSet<Charset> set = new HashSet<>();
      set.add(this.charset);
		return (set.iterator());
	}
}
