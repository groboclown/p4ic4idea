/**
 * 
 */
package com.perforce.p4java.core.file;

/**
 * Describes the various type of file version diffs and related whitespace options
 * available through the relevant content diff, resolve, annotate, etc., methods,
 * corresponding loosely to the "-d" series of options to the p4 command line app.<p>
 * 
 * The diff types are explained in detail in the main Perforce documentation and will not be
 * detailed here.
 */

public enum DiffType {
	RCS_DIFF,
	CONTEXT_DIFF,
	SUMMARY_DIFF,
	UNIFIED_DIFF,
	IGNORE_WS_CHANGES,
	IGNORE_WS,
	IGNORE_LINE_ENDINGS;
	
	private static String[] argMap =  {
		"n",
		"c",
		"s",
		"u",
		"b",
		"w",
		"l"
	};
	
	/**
	 * Return the value in p4 command single character form.
	 * 
	 * @return single-char String representing the diff format as used by the various P4 commands.
	 */
	
	public String toArgString() {
		
		return argMap[this.ordinal()];
	}
	
	/**
	 * Return true if this is a "whitespace option", i.e. one of the -db, -dl, or -dw options.
	 * 
	 * @return true iff this is a whitespace diff option.
	 */
	
	public boolean isWsOption() {
		switch (this) {
			case IGNORE_WS_CHANGES:
			case IGNORE_WS:
			case IGNORE_LINE_ENDINGS:
				return true;
		}
		return false;
	}
}
