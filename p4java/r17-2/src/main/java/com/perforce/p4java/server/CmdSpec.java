/**
 * 
 */
package com.perforce.p4java.server;

import java.util.Locale;

/**
 * An enumeration of the minimum set of Perforce server commands recognized and implemented
 * by P4Java. Actual implementations may accept and implement more commands than is listed
 * here, but this is the safe subset you can depend on.
 */

public enum CmdSpec {
	INFO,
	DEPOTS,
	LOGIN,
	LOGOUT,
	CLIENTS,
	CLIENT,
	FILES,
	FSTAT,
	SYNC,
	CHANGES,
	CHANGE,
	DESCRIBE,
	OPENED,
	EDIT,
	ADD,
	DELETE,
	REVERT,
	SUBMIT,
	FILELOG,
	PRINT,
	WHERE,
	HAVE,
	REOPEN,
	DIRS,
	INTEG,
	RESOLVE,
	RESOLVED,
	JOBS,
	FIXES,
	JOBSPEC,
	FIX,
	JOB,
	LOCK,
	UNLOCK,
	DIFF,
	COUNTERS,
	USERS,
	MOVE,
	LABELS,
	LABEL,
	LABELSYNC,
	TAG,
	MONITOR,
	GROUPS,
	GROUP,
	BRANCH,
	BRANCHES,
	COUNTER,
	INTEGRATED,
	ANNOTATE,
	DBSCHEMA,
	EXPORT,
	SHELVE,
	UNSHELVE,
	PROTECTS,
	PROTECT,
	USER,
	REVIEWS,
	REVIEW,
	DIFF2,
	INTERCHANGES,
	GREP,
	DEPOT,
	ATTRIBUTE,
	SPEC,
	COPY,
	CONFIGURE,
	PASSWD,
	DISKSPACE,
	OBLITERATE,
	STREAMS,
	STREAM,
	ISTAT,
	MERGE,
	LOGTAIL,
	TRUST,
	RECONCILE,
	DUPLICATE,
	UNLOAD,
	RELOAD,
	POPULATE,
	KEY,
	KEYS,
	SEARCH,
	PROPERTY,
	SIZES,
	JOURNALWAIT,
	TRIGGERS,
	VERIFY,
	RENAMEUSER,
	GRAPH,
	REPOS,
	TRANSMIT,
	LIST,
	RETYPE;

	/**
	 * Return true iff the passed-in string can be decoded as a valid
	 * P4CJCmdSpec. Matching is done case-regardless, and some leeway is
	 * allowed, but the decoding is done in the local locale, which may cause
	 * issues with arbitrary user-generated commands.
	 * 
	 * @param str
	 *            candidate string
	 * @return true iff the candidate string can be decoded into a valid
	 *         P4CJCmdSpec.
	 */

	public static boolean isValidP4JCmdSpec(String str) {
		return getValidP4JCmdSpec(str) != null;
	}

	/**
	 * Return non-null p4j cmd spec iff the passed-in string can be decoded as a
	 * valid P4CJCmdSpec. Matching is done case-regardless, and some leeway is
	 * allowed, but the decoding is done in the English locale, which may cause
	 * issues with arbitrary user-generated commands but should help solve issues
	 * such as the dotted-i problem with Turkish locales.
	 * 
	 * @param str
	 *            candidate string
	 * @return non-null p4j cmd spec iff the candidate string can be decoded
	 *         into a valid P4CJCmdSpec.
	 */

	public static CmdSpec getValidP4JCmdSpec(String str) {
		if (str != null) {
			try {
				return CmdSpec.valueOf(str.toUpperCase(Locale.ENGLISH));
			} catch (IllegalArgumentException exc) {
				return null;
			}
		}

		return null;
	}
	
	/**
	 * Returns a string suitable for passing to the lower levels of an IServer
	 * object as a Perforce command name. Usually means it's just the lower case
	 * representation of the name, but this is not guaranteed to be true. Most
	 * useful with the execMapXXX series of methods on IServer. Note the use of
	 * the English locale; this is to try to ensure that we don't trip up on
	 * off default locales like Turkish (with its dotted-i issue -- see e.g.
	 * job037128).
	 * 
	 * @see java.lang.Enum#toString()
	 */
	public String toString() {
		return this.name().toLowerCase(Locale.ENGLISH);
	}
}
