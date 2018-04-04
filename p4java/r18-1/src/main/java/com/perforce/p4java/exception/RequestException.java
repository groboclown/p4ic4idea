/**
 * 
 */
package com.perforce.p4java.exception;

import com.perforce.p4java.Log;

/**
 * An exception to be used to signal that the Perforce server has detected
 * an error in processing or fielding a request. This error might be a usage
 * error in a command sent to the server, or a missing or bad parameter, or
 * a semantics error in the request. Note that this is not the same as a
 * connection exception, which is typically signaled when the Perforce server
 * itself can't be reached.<p>
 * 
 * RequestExceptions coming back from the server have a non-zero rawCode field
 * that is set to the corresponding raw code value sent from the server; this
 * code normally requires processing for use by callers, who typically want to
 * see the correspond generic and severity codes, but it's available here if needed.
 * Other segments or interpretations of the raw code -- subCode, subSystem,
 * uniqueCode, and the raw code itself -- are available here if you need them and know what
 * they mean. Note, though, that only the generic and severity codes are documented
 * in the main Perforce documentation, and only those fields are guaranteed to be
 * set to meaningful values here (in fact, all RequestExceptions constructed 'under
 * the covers' in the RPC layer will do what they can to get meaningful value for
 * these other fields, but this is not guaranteed)...<p>
 * 
 * Note that if you set the raw code yourself using the accessor methods, you are
 * required to also set the other fields to appropriate values yourself; failure
 * to do this will cause a lot of confusion up the chain, as the subsidiary codes are
 * only calculated once. The setCodes method is provided to make this easy.<p>
 * 
 * See the MessageSeverityCode and MessageGenericCode definitions for suitable help
 * with those types of code.
 */

public class RequestException extends P4JavaException {
	
	private static final long serialVersionUID = 1L;
	private int rawCode = 0;
	private int severityCode = 0;
	private int genericCode = 0;
	private int uniqueCode = 0;
	private int subCode = 0;
	private int subSystem = 0;

	public RequestException() {
		super();
	}

	public RequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public RequestException(String message) {
		super(message);
	}

	public RequestException(Throwable cause) {
		super(cause);
	}
	
	public RequestException(String message, int rawCode) {
		super(message);
		setCodes(rawCode);
	}
	
	public RequestException(String message, String codeString) {
		super(message);
		if (codeString != null) {
			try {
				setCodes(new Integer(codeString));
			} catch (Exception exc) {
				Log.exception(exc);
			}
		}
	}
	
	public RequestException(String message, int genericCode, int severityCode) {
		super(message);
		this.genericCode = genericCode;
		this.severityCode = severityCode;
	}
	
	public RequestException(Throwable cause, int genericCode, int severityCode) {
		super(cause);
		this.genericCode = genericCode;
		this.severityCode = severityCode;
	}
	
	public RequestException(String message, Throwable cause, int genericCode, int severityCode) {
		super(message, cause);
		this.genericCode = genericCode;
		this.severityCode = severityCode;
	}
	
	/**
	 * Set the raw code and associated subsidiary codes according to
	 * the passed-in values. If you only have the raw code from the server,
	 * this is probably the easiest and least error-prone way to set the
	 * request exception codes.
	 * 
	 * @param rawCode raw code from the server.
	 * @return 'this' for chaining.
	 */
	public RequestException setCodes(int rawCode) {
		this.rawCode = rawCode;
		this.subCode = ((rawCode >> 0) & 0x3FF);
		this.subSystem = ((rawCode >> 10) & 0x3F);
		this.uniqueCode = (rawCode & 0xFFFF);
		this.genericCode = ((rawCode >> 16) & 0xFF);
		this.severityCode = ((rawCode >> 28) & 0x00F);
		return this;
	}

	public void setSeverityCode(int severityCode) {
		this.severityCode = severityCode;
	}


	public void setGenericCode(int genericCode) {
		this.genericCode = genericCode;
	}
	
	public String getDisplayString() {
		return "" + (this.genericCode != 0 ? "Generic: " + this.genericCode : "")
				+ (this.severityCode != 0 ? " Severity: " + this.severityCode + "; " : "")
				+ this.getMessage()
				+ (this.getCause() != null ? this.getCause() : "");
	}

	public int getUniqueCode() {
		return uniqueCode;
	}

	public void setUniqueCode(int uniqueCode) {
		this.uniqueCode = uniqueCode;
	}

	public int getRawCode() {
		return this.rawCode;
	}

	public int getSubCode() {
		return subCode;
	}

	public void setSubCode(int subCode) {
		this.subCode = subCode;
	}

	public int getSubSystem() {
		return this.subSystem;
	}

	public void setSubSystem(int subSystem) {
		this.subSystem = subSystem;
	}

	/**
	 * Get the Perforce severity code associated with this exception, if any.
	 * See the MessageSeverityCode Javadocs for an explanation of these codes.
	 */
	public int getSeverityCode() {
		return this.severityCode;
	}

	/**
	 * Get the Perforce generic code associated with this exception, if any.
	 * See the MessageSGenericCode Javadocs for an explanation of these codes.
	 */
	public int getGenericCode() {
		return this.genericCode;
	}
}
