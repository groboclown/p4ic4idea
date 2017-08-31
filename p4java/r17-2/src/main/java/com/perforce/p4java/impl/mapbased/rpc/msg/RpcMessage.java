/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.msg;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.MessageSubsystemCode;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMessage;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMessage.ClientMessageId;


/**
 * Definitions and methods for processing, encapsulating, and
 * handling RPC error and info codes on and off the wire.<p>
 * 
 * The Perforce RPC scheme encodes messages for the client -- warnings,
 * fatal errors, user errors, infomercials, etc., with an integer code
 * (usually codeX in the incoming packet, where X is 0, 1, 2, etc.,
 * i.e. there can be multiple codes in one packet) and a corresponding
 * formatted message (keyed with fmtX, where the X corresponds to the
 * code, etc.). The integer code encodes severity and generic levels,
 * at least, and needs to be unpacked carefully before interpretation,
 * especially as it comes across the wire as a string.<p>
 * 
 * The apparent generic packing for codes looks like this:
 * <pre>
 * ((sev<<28)|(arg<<24)|(gen<<16)|(sub<<10)|cod)
 * </pre>
 * where sev == severity, arg == arg count, gen == generic code,
 * sub == subsystem (client vs. server. etc.), and cod is the
 * actual individual error code.<p>
 * 
 * The integer code is assumed by all sides of the wire to decode into
 * four bytes. We attempt to make this so....
 * 
 *
 */

public class RpcMessage {
	
	/**
	 * What getGeneric() returns if it can't find a plausible
	 * error generic level in a candidate string.
	 */
	public static final int NO_GENERIC_CODE_FOUND = -1;
	
	/**
	 * CODE - code
	 */
	public static final String CODE = "code";
	
	/**
	 * FMT - fmt
	 */
	public static final String FMT = "fmt";
	
	
	private int severity = MessageGenericCode.EV_NONE;
	private int subSystem = MessageSubsystemCode.ES_CLIENT; // FIXME -- safe default? -- HR
	private int generic = 0;
	private int code = 0;
	private String[] fmtStrs = null;
	private String[] argNameStrs = null;
	private String[] argStrs = null;
	
	// Pattern matching defs for error / info (etc.) message parsing:
	
	private static final String ALT_PATTERN = "\\[[^\\[^\\]]*\\]";
	private static final String PC_PATTERN = "%[^%]*%";
	private static final String SPLIT_PATTERN = "\\|";
	private static final String SPLIT_MARKER = "|";
	private static final String PC_MARKER = "%";
	
	private static Pattern altPat = Pattern.compile(ALT_PATTERN);
	private static Pattern pcPat = Pattern.compile(PC_PATTERN);
	
	/**
	 * Try to fill in the %...% bits in a typical server text message. Example:
	 * <pre>
	 * fmtStr="Access for user '%user%' has not been enabled by 'p4 protect'."
	 * args[0]="nouser"
	 * </pre>
	 * Harder example, with alternates:
	 * <pre>
	 * [%argc% - no|No] such file(s).
	 * </pre>
	 * Another difficult example, with a different type of alternate:
	 * <pre>fmtStr=%change% created[ with %workCount% open file(s)][ fixing %jobCount% job(s)]
	 * </pre>
	 * Typically used in this implementation for error messages coming back from
	 * the server, but can have broader uses with untagged server output in general.<p>
	 * 
	 * FIXME: this is a rather ad-hoc and not particularly efficient algorithm,
	 * which will be replaced by a better implementation when I get more experience
	 * with relative efficiencies, etc. -- (HR).<p>
	 * 
	 * FIXME: provide a version that works with multiple format strings -- HR.
	 */
	
	public static String interpolateArgs(String fmtStr, Map<String, Object> map) {
		
		if ((fmtStr != null) && (map != null) && (fmtStr.contains("%") || fmtStr.contains("|"))) {
			return doParse(fmtStr, map);
		}
		
		return fmtStr;
	}
	
	public RpcMessage(int subSystem, int code,
			int severity, int generic,
			String[] fmtStrs, String[] argNameStrs, String[] argStrs) {
		this.subSystem = subSystem;
		this.code = code;
		this.severity = severity;
		this.generic = generic;
		this.fmtStrs = fmtStrs;
		this.argNameStrs = argNameStrs;
		this.argStrs = argStrs;
	}
	
	public RpcMessage(ClientMessageId id,
			int severity, int generic,
			String[] argStrs) {
		if (id == null) {
			throw new NullPointerError(
					"Null client message ID passed to RpcMessage constructor");
		}
		
		this.subSystem = MessageSubsystemCode.ES_CLIENT;
		ClientMessage msg = ClientMessage.getClientMessage(id);
		this.code = msg.getCode();
		this.severity = severity;
		this.generic = generic;
		this.argStrs = argStrs;
		this.fmtStrs = msg.getMsgs();
		this.argNameStrs = msg.getMsgParamNames();
		
		if ((this.argNameStrs != null) && (this.argStrs != null)
				&& (this.argNameStrs.length != this.argStrs.length)) {
			// FIXME: too draconian? -- HR.
			throw new P4JavaError("Spec length mismatch in RpcMessage constructor");
		}
	}
	
	/**
	 * Return a Map'd version of this error in the format expected
	 * by the upper levels of the API.
	 */
	
	public Map<String, Object> toMap() {		
		Map<String, Object> retMap = new HashMap<String, Object>();
		
		retMap.put("code0", makeErrorCodeString());
		
		if (fmtStrs != null) {
			int i = 0;
			for (String fmtStr : fmtStrs) {
				if (fmtStr != null) {
					retMap.put("fmt" + i++, fmtStr);
				}
			}
		}
		
		if (argNameStrs != null) {
			int i = 0;
			
			for (String argNameStr : argNameStrs) {
				if ((argNameStr != null) && (argStrs != null)
							&& (argStrs.length > i) && (argStrs[i] != null)) {
					retMap.put(argNameStr, argStrs[i]);
				} else {
					retMap.put(argNameStr, "");
				}
				i++;
			}
		}
		
		return retMap;
	}
	
	/**
	 * Given a string encoding of a complete error code off the wire,
	 * return its severity level, if possible. Will return NONE if it
	 * can't decode the string into a suitable level (or if it was null).
	 * 
	 * @param codeStr candidate error code
	 * @return corresponding MessageSeverityCode level, or MessageSeverityCode.E_EMPTY
	 */
	
	public static int getSeverity(String codeStr) {
		if (codeStr != null) {
			try {
				int rawNum = new Integer(codeStr);	// Really need an unsigned here...
				return ((rawNum >> 28) & 0x0f);

			} catch (Exception exc) {
				// If there's a conversion error, just let it return below
				Log.exception(exc);
			}
		}
		return MessageSeverityCode.E_EMPTY;
	}
	
	/**
	 * Given a string encoding of a complete error code, return
	 * its generic error code value ("generic" being Perforce's
	 * rather odd name for the specific error value). Will return
	 * NO_GENERIC_CODE_FOUND if it can't find a suitable value
	 * in the passed-in string (or if the string was null).
	 * 
	 * @param codeStr candidate error code 
	 * @return corresponding generic level, or NO_GENERIC_CODE_FOUND
	 */
	
	public static int getGeneric(String codeStr) {
		if (codeStr != null) {
			try {
				int rawNum = new Integer(codeStr);	// Really need an unsigned here...
				int severity = ((rawNum >> 16) & 0x0FF);
				
				return severity;

			} catch (Exception exc) {
				Log.exception(exc);
				// just let it return default below
			}
		}
		
		return MessageGenericCode.EV_NONE;
	}
	
	/**
	 * Given a string encoding of a complete error code, return
	 * its subsystem error code value. Will return
	 * CLIENT if it can't find a suitable value
	 * in the passed-in string (or if the string was null).
	 * 
	 * @param codeStr candidate error code 
	 * @return corresponding subsystem, or CLIENT if none found (FIXME? -- HR)
	 */
	
	public static int getSubsystem(String codeStr) {
		
		if (codeStr != null) {
			try {
				int rawNum = new Integer(codeStr);	// Really need an unsigned here...
				int subSystem = ((rawNum >> 10) & 0x3F);
				
				// FIXME -- HR.
				
				return subSystem;
			} catch (Exception exc) {
				// just let it fall through below
				Log.exception(exc);
			}
		}
		
		return MessageSubsystemCode.ES_CLIENT; // FIXME -- HR.
	}
	
	/**
	 * Encode the various error code subcodes as a string
	 * as seen on the wire. The general encoding (taken straight
	 * from the C++ API) is as follows:
	 * <pre>
	 * # define ErrorOf( sub, cod, sev, gen, arg ) \
	 * ((sev<<28)|(arg<<24)|(gen<<16)|(sub<<10)|cod)
	 * </pre>
	 */
	
	public String makeErrorCodeString() {
		// We use Long here to try to force non-negative values before
		// conversion. This may change -- HR.
		
		return "" + new Long(
				((this.severity << 28)
						| ((this.argStrs == null ? 0 : this.argStrs.length) << 24)
						| (this.generic << 16) | (this.subSystem << 10) | this.code ));
	}

	public int getSeverity() {
		return this.severity;
	}

	public void setSeverity(int severity) {
		this.severity = severity;
	}

	public int getSubSystem() {
		return this.subSystem;
	}

	public void setSubSystem(int subSystem) {
		this.subSystem = subSystem;
	}

	public int getGeneric() {
		return this.generic;
	}

	public void setGeneric(int generic) {
		this.generic = generic;
	}

	public int getCode() {
		return this.code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String[] getFmtStrs() {
		return this.fmtStrs;
	}

	public void setFmtStrs(String[] fmtStrs) {
		this.fmtStrs = fmtStrs;
	}

	public String[] getArgStrs() {
		return this.argStrs;
	}

	public void setArgStrs(String[] argStrs) {
		this.argStrs = argStrs;
	}

	public String[] getArgNameStrs() {
		return this.argNameStrs;
	}

	public void setArgNameStrs(String[] argNameStrs) {
		this.argNameStrs = argNameStrs;
	}
	
	private static String doParse(String fmtStr, Map<String, Object> argMap) {
		
		// Note also that a format string containing "[" ... "]" pairs is
		// (usually, as far as I can tell) a simple alternate message element
		// keyed on the existence of the contained args; this complicates things
		// a little but is crucial for correct string interpretation.
		
		StringBuilder strBuf = new StringBuilder();
		StringBuilder outBuf = new StringBuilder();
		
		Matcher altMatcher = altPat.matcher(fmtStr);
		int i = 0;
		while (altMatcher.find()) {
			String match = altMatcher.group();
			
			strBuf.append(fmtStr.subSequence(i, altMatcher.start()));
		
			String matchStr = (String) match.subSequence(1, match.length() - 1);
			
			if (matchStr.contains(SPLIT_MARKER)) {

				String[] splitMatch = matchStr.split(SPLIT_PATTERN);
				if ((splitMatch.length == 2) && (splitMatch[0] != null) && (splitMatch[1] != null)) {
					// Only one of these should contain % match markers...
					
					int useIndx = -1;
					
					if (splitMatch[0].contains(PC_MARKER)) {
						if (containsValueMatches(splitMatch[0], argMap)) {
							useIndx = 0;
						} else {
							useIndx = 1;
						}
					} else if (splitMatch[1].contains(PC_MARKER)) {
						if (containsValueMatches(splitMatch[1], argMap)) {
							useIndx = 1;
						} else {
							useIndx = 0;
						}
					}
					
					if ((useIndx < 0) || (useIndx > 1)) {
	                    strBuf.append("[").append(matchStr).append("]");
					} else {
						strBuf.append(splitMatch[useIndx]);
					}
				}
			} else {
				if (containsValueMatches(matchStr, argMap)) {
					strBuf.append(matchStr);
				} else if (!matchStr.contains(PC_MARKER)) {
				    strBuf.append("[").append(matchStr).append("]");
				}
			}
			i = altMatcher.start();
			i += (match.length());
		}
		
		if (i == 0) {
			strBuf.append(fmtStr);
		} else {
			if (i < fmtStr.length()) {
				strBuf.append(fmtStr.subSequence(i, fmtStr.length()));
			}
		}
		
		Matcher pcMatcher = pcPat.matcher(strBuf);
		
		int j = 0;
		while (pcMatcher.find()) {
			String match = pcMatcher.group();
			String repl = null;
			
			outBuf.append(strBuf.subSequence(j, pcMatcher.start()));
			if(isUniquote(match)) {
				repl = match.substring(2, match.length() - 2);
			} else {
				repl = (String) argMap.get(match.subSequence(1, match.length() - 1));
			}
			
			if (repl != null) {
				outBuf.append(repl);
			} else {
				outBuf.append(match);
			}
			
			j = pcMatcher.start();
			j += match.length();
		}
		
		if (j == 0) {
			outBuf.append(strBuf);
		} else {
			if (j < strBuf.length()) {
				outBuf.append(strBuf.subSequence(j, strBuf.length()));
			}
		}
		
		return outBuf.toString();
	}
	
	private static boolean containsValueMatches(String str, Map<String, Object> map) {
		
		if ((str != null) && (map != null)) {
			Pattern pcPat = Pattern.compile(PC_PATTERN);
			Matcher pcMatcher = pcPat.matcher(str);
			
			while (pcMatcher.find()) {
				String pcMatch = pcMatcher.group();
				if(isUniquote(pcMatch)) {
					return true;
				}
				
				String repl = (String) map.get(
				        pcMatch.subSequence(1, pcMatch.length() - 1));
				
				if (repl != null && repl.length() > 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static boolean isUniquote(String str) {
		// Handle non-translated literals.
		// These look like %'value'% and the %' and '% should just be removed
		return  str != null &&
			str.length() >= 4 &&
			str.charAt(1) == '\''&&
			str.charAt(str.length() - 2) == '\'';
	}
}
