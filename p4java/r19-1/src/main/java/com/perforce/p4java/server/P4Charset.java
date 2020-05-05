package com.perforce.p4java.server;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.charset.PerforceCharsetProvider;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.server.PerforceCharsets.getJavaCharsetName;
import static com.perforce.p4java.server.PerforceCharsets.hasClientBOM;
import static com.perforce.p4java.server.PerforceCharsets.isSupported;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class P4Charset {

	private String charsetName;
	private Charset charset;
	private boolean clientBOM;

	public P4Charset(String charsetName) {
		this.charsetName = charsetName;
		setCharsetName(charsetName);
	}

	public static P4Charset getUTF8() {
		return new P4Charset("utf8");
	}

	public static P4Charset getDefault() {
		return new P4Charset(CharsetDefs.DEFAULT, false);
	}

	private P4Charset(Charset charset, boolean clientBOM) {
		this.charset = charset;
		this.clientBOM = clientBOM;
		charsetName = null;
	}

	public Charset getCharset() {
		return charset;
	}

	public boolean isClientBOM() {
		return clientBOM;
	}

	public String getCharsetName() {
		return charsetName;
	}

	public static boolean isUnicodeServer(P4Charset p4Charset) {
		if(p4Charset == null) {
			return false;
		}
		if(p4Charset.getCharset() == null) {
			return false;
		}
		return true;
	}

	private void setCharsetName(final String charsetName) throws UnsupportedCharsetException {
		// "auto" (Guess a P4CHARSET based on client OS params)
		// "none" (same as unsetting P4CHARSET)
		if (isNotBlank(charsetName)
				&& !("none".equals(charsetName) || "auto".equals(charsetName))) {
			// Check if it is a supported Perforce charset
			if (!isSupported(charsetName)) {
				throw new UnsupportedCharsetException(charsetName);
			}
			// Get the Java equivalent charset for this Perforce charset
			String javaCharsetName = getJavaCharsetName(charsetName);
			if (isNotBlank(javaCharsetName)) {
				try {
					this.charset = Charset.forName(javaCharsetName);
					this.clientBOM = hasClientBOM(charsetName);
				} catch (UnsupportedCharsetException uce) {
					// In case P4Java's Perforce extended charsets are not
					// loaded in the VM's bootstrap classpath (i.e. P4Java JAR
					// file is inside a WAR deployed in a web app container like
					// Jetty, Tomcat, etc.), we'll instantiate it and lookup the
					// Perforce extended charsets.
					PerforceCharsetProvider p4CharsetProvider = new PerforceCharsetProvider();
					this.charset = p4CharsetProvider.charsetForName(javaCharsetName);
					this.clientBOM = false;

					// Throw the unsupported charset exception that was catched.
					if (isNull(this.charset)) {
						throw uce;
					}
				} catch (IllegalCharsetNameException icne) {
					// Throw a unsupported charset exception wrapped around
					// the illegal charset name exception.
					throw new UnsupportedCharsetException(icne.getLocalizedMessage());
				}
				// Set the new charset name
				this.charsetName = charsetName;
			}
		} else { // Reset the charset to "no charset"
			this.charsetName = null;
			this.charset = null;
			this.clientBOM = false;
		}
	}
}
