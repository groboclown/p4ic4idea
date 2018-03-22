/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func;

import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;

import java.util.EnumMap;
import java.util.HashMap;

/**
 * General Perforce RPC function utilities and definitions.<p>
 * 
 * An RPC function in this context is anything that can be encoded
 * with the "func" field in an RPC packet, e.g. "func client-Message",
 * "func user-files", or "func dm-Login".<p>
 * 
 * Note that a lot of optimizations could be done here, but we'll hold
 * off on this until we're clearer about usage models, actual performance,
 * etc. -- HR. (And a lot of this would have been easier if the RPC protocol
 * didn't insist on camel case or mixed case function strings...).
 * 
 *
 */

public class RpcFunction {
	
	public static final String TRACE_PREFIX = "RpcFunction";
	
	/**
	 * Used to signal that there's no corresponding function metadata
	 * for an encoding or name.
	 */
	public static final RpcFunctionMetadata NO_METADATA
		= new RpcFunctionMetadata(RpcFunctionSpec.NONE, RpcFunctionType.NONE, "none");
	
	/*
	 * You should ensure that there's one (and only one) entry in the maps defined
	 * here for each function name.
	 */
	
	/**
	 * What we use to efficiently map function names (in RpcFunctionName enum form)
	 * to the associated function metadata.
	 */
	private static EnumMap<RpcFunctionSpec, RpcFunctionMetadata> nameMap = null;
	
	/**
	 * What we use to efficiently map function string encodings to the associated
	 * function metadata.
	 */
	private static HashMap<String, RpcFunctionMetadata> encodingMap = null;
	
	/**
	 * Where we actually store the function metadata.
	 */
	private static final RpcFunctionMetadata[] functionMetadata = {
		
		NO_METADATA, // special entry for non-matches, etc.
		
		new RpcFunctionMetadata(RpcFunctionSpec.PROTOCOL_PROTOCOL, RpcFunctionType.PROTOCOL, "protocol"),
		new RpcFunctionMetadata(RpcFunctionSpec.PROTOCOL_FLUSH1, RpcFunctionType.PROTOCOL, "flush1"),
		new RpcFunctionMetadata(RpcFunctionSpec.PROTOCOL_FLUSH2, RpcFunctionType.PROTOCOL, "flush2"),
		new RpcFunctionMetadata(RpcFunctionSpec.PROTOCOL_RELEASE, RpcFunctionType.PROTOCOL, "release"),
		new RpcFunctionMetadata(RpcFunctionSpec.PROTOCOL_RELEASE2, RpcFunctionType.PROTOCOL, "release2"),
		new RpcFunctionMetadata(RpcFunctionSpec.PROTOCOL_CRYPTO, RpcFunctionType.PROTOCOL, "crypto"),
		new RpcFunctionMetadata(RpcFunctionSpec.PROTOCOL_COMPRESS1, RpcFunctionType.PROTOCOL, "compress1"),
		new RpcFunctionMetadata(RpcFunctionSpec.PROTOCOL_COMPRESS2, RpcFunctionType.PROTOCOL, "compress2"),
		
		new RpcFunctionMetadata(RpcFunctionSpec.USER_SPECIFIED, RpcFunctionType.USER, "user-specified"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DEPOTS, RpcFunctionType.USER, "user-depots"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_INFO, RpcFunctionType.USER, "user-info"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_FILES, RpcFunctionType.USER, "user-files"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_CLIENTS, RpcFunctionType.USER, "user-clients"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_JOBS, RpcFunctionType.USER, "user-jobs"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_CHANGES, RpcFunctionType.USER, "user-changes"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_LOGIN, RpcFunctionType.USER, "user-login"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_LOGOUT, RpcFunctionType.USER, "user-logout"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_CLIENT, RpcFunctionType.USER, "user-client"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_FSTAT, RpcFunctionType.USER, "user-fstat"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_SYNC, RpcFunctionType.USER, "user-sync"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_CHANGE, RpcFunctionType.USER, "user-change"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DESCRIBE, RpcFunctionType.USER, "user-describe"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_OPENED, RpcFunctionType.USER, "user-opened"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_EDIT, RpcFunctionType.USER, "user-edit"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_ADD, RpcFunctionType.USER, "user-add"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DELETE, RpcFunctionType.USER, "user-delete"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_REVERT, RpcFunctionType.USER, "user-revert"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_SUBMIT, RpcFunctionType.USER, "user-submit"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_FILELOG, RpcFunctionType.USER, "user-filelog"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_PRINT, RpcFunctionType.USER, "user-print"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_WHERE, RpcFunctionType.USER, "user-where"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_HAVE, RpcFunctionType.USER, "user-have"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_REOPEN, RpcFunctionType.USER, "user-reopen"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DIRS, RpcFunctionType.USER, "user-dirs"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_INTEG, RpcFunctionType.USER, "user-integ"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_RESOLVE, RpcFunctionType.USER, "user-resolve"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_RESOLVED, RpcFunctionType.USER, "user-resolved"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_FIXES, RpcFunctionType.USER, "user-fixes"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_JOBSPEC, RpcFunctionType.USER, "user-jobspec"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_FIX, RpcFunctionType.USER, "user-fix"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_JOB, RpcFunctionType.USER, "user-job"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_LOCK, RpcFunctionType.USER, "user-lock"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_UNLOCK, RpcFunctionType.USER, "user-unlock"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DIFF, RpcFunctionType.USER, "user-diff"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_COUNTERS, RpcFunctionType.USER, "user-counters"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_USERS, RpcFunctionType.USER, "user-users"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_MOVE, RpcFunctionType.USER, "user-move"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_LABELS, RpcFunctionType.USER, "user-labels"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_LABEL, RpcFunctionType.USER, "user-label"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_LABELSYNC, RpcFunctionType.USER, "user-labelsync"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_TAG, RpcFunctionType.USER, "user-tag"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_MONITOR, RpcFunctionType.USER, "user-monitor"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_GROUP, RpcFunctionType.USER, "user-group"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_GROUPS, RpcFunctionType.USER, "user-groups"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_BRANCH, RpcFunctionType.USER, "user-branch"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_BRANCHES, RpcFunctionType.USER, "user-branches"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_COUNTER, RpcFunctionType.USER, "user-counter"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_INTEGRATED, RpcFunctionType.USER, "user-integrated"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_ANNOTATE, RpcFunctionType.USER, "user-annotate"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DBSCHEMA, RpcFunctionType.USER, "user-dbschema"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_EXPORT, RpcFunctionType.USER, "user-export"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_SHELVE, RpcFunctionType.USER, "user-shelve"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_UNSHELVE, RpcFunctionType.USER, "user-unshelve"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_PROTECTS, RpcFunctionType.USER, "user-protects"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_PROTECT, RpcFunctionType.USER, "user-protect"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_USER, RpcFunctionType.USER, "user-user"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_REVIEWS, RpcFunctionType.USER, "user-reviews"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_REVIEW, RpcFunctionType.USER, "user-review"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DIFF2, RpcFunctionType.USER, "user-diff2"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_INTERCHANGES, RpcFunctionType.USER, "user-interchanges"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_GREP, RpcFunctionType.USER, "user-grep"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DEPOT, RpcFunctionType.USER, "user-depot"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_ATTRIBUTE, RpcFunctionType.USER, "user-attribute"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_SPEC, RpcFunctionType.USER, "user-spec"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_COPY, RpcFunctionType.USER, "user-copy"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_CONFIGURE, RpcFunctionType.USER, "user-configure"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_PASSWD, RpcFunctionType.USER, "user-passwd"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DISKSPACE, RpcFunctionType.USER, "user-diskspace"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_OBLITERATE, RpcFunctionType.USER, "user-obliterate"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_STREAMS, RpcFunctionType.USER, "user-streams"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_STREAM, RpcFunctionType.USER, "user-stream"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_ISTAT, RpcFunctionType.USER, "user-istat"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_MERGE, RpcFunctionType.USER, "user-merge"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_LOGTAIL, RpcFunctionType.USER, "user-logtail"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_TRUST, RpcFunctionType.USER, "user-trust"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_RECONCILE, RpcFunctionType.USER, "user-reconcile"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_DUPLICATE, RpcFunctionType.USER, "user-duplicate"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_UNLOAD, RpcFunctionType.USER, "user-unload"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_RELOAD, RpcFunctionType.USER, "user-reload"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_POPULATE, RpcFunctionType.USER, "user-populate"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_KEY, RpcFunctionType.USER, "user-key"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_KEYS, RpcFunctionType.USER, "user-keys"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_SEARCH, RpcFunctionType.USER, "user-search"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_PROPERTY, RpcFunctionType.USER, "user-property"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_SIZES, RpcFunctionType.USER, "user-sizes"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_JOURNALWAIT, RpcFunctionType.USER, "user-journalwait"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_TRIGGERS, RpcFunctionType.USER, "user-triggers"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_VERIFY, RpcFunctionType.USER, "user-verify"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_RENAMEUSER, RpcFunctionType.USER, "user-renameuser"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_GRAPH, RpcFunctionType.USER, "user-graph"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_REPOS, RpcFunctionType.USER, "user-repos"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_TRANSMIT, RpcFunctionType.USER, "user-transmit"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_LIST, RpcFunctionType.USER, "user-list"),
		new RpcFunctionMetadata(RpcFunctionSpec.USER_RETYPE, RpcFunctionType.USER, "user-retype"),
		
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_MESSAGE, RpcFunctionType.CLIENT, "client-Message"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_FSTATINFO, RpcFunctionType.CLIENT, "client-FstatInfo"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_PROMPT, RpcFunctionType.CLIENT, "client-Prompt"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_SETPASSWORD, RpcFunctionType.CLIENT, "client-SetPassword"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_CRYPTO, RpcFunctionType.CLIENT, "client-Crypto"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_ACK, RpcFunctionType.CLIENT, "client-Ack"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_CHMODFILE, RpcFunctionType.CLIENT, "client-ChmodFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OPENFILE, RpcFunctionType.CLIENT, "client-OpenFile"),
		//new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OPENDIFF, RpcFunctionType.CLIENT, "client-OpenDiff"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_WRITEFILE, RpcFunctionType.CLIENT, "client-WriteFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_CLOSEFILE, RpcFunctionType.CLIENT, "client-CloseFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_CHECKFILE, RpcFunctionType.CLIENT, "client-CheckFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_INPUTDATA, RpcFunctionType.CLIENT, "client-InputData"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_SENDFILE, RpcFunctionType.CLIENT, "client-SendFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_DELETEFILE, RpcFunctionType.CLIENT, "client-DeleteFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OUTPUTBINARY, RpcFunctionType.CLIENT, "client-OutputBinary"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OUTPUTERROR, RpcFunctionType.CLIENT, "client-OutputError"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OUTPUTTEXT, RpcFunctionType.CLIENT, "client-OutputText"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OUTPUTDATA, RpcFunctionType.CLIENT, "client-OutputData"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OUTPUTINFO, RpcFunctionType.CLIENT, "client-OutputInfo"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_MOVEFILE, RpcFunctionType.CLIENT, "client-MoveFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OPENMERGE3, RpcFunctionType.CLIENT, "client-OpenMerge3"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_WRITEMERGE, RpcFunctionType.CLIENT, "client-WriteMerge"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_CLOSEMERGE, RpcFunctionType.CLIENT, "client-CloseMerge"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_SSO, RpcFunctionType.CLIENT, "client-SSO"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_OPENMERGE2, RpcFunctionType.CLIENT, "client-OpenMerge2"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_RECONCILEEDIT, RpcFunctionType.CLIENT, "client-ReconcileEdit"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_RECONCILEADD, RpcFunctionType.CLIENT, "client-ReconcileAdd"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_RECONCILEFLUSH, RpcFunctionType.CLIENT, "client-ReconcileFlush"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_PROGRESS, RpcFunctionType.CLIENT, "client-Progress"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_RECEIVEFILES, RpcFunctionType.CLIENT, "client-ReceiveFiles"),
		
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_DM_PROMPT, RpcFunctionType.CLIENT_DM, "dm-Prompt"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_DM_OPENFILE, RpcFunctionType.CLIENT_DM, "dm-OpenFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.CLIENT_DM_MOVEFILE, RpcFunctionType.CLIENT_DM, "dm-MoveFile"),
		
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_DM_LOGIN, RpcFunctionType.SERVER, "dm-Login"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_DM_PASSWD, RpcFunctionType.SERVER, "dm-Passwd"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_DM_SUBMITCHANGE, RpcFunctionType.SERVER, "dm-SubmitChange"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_LBR_OPEN, RpcFunctionType.SERVER, "lbr-Open"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_DM_LBR_OPEN, RpcFunctionType.SERVER, "dm-LbrOpen"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_LBR_WRITEFILE, RpcFunctionType.SERVER, "lbr-WriteFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_DM_SUBMITFILE, RpcFunctionType.SERVER, "dm-SubmitFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_DM_COMMITSUBMIT, RpcFunctionType.SERVER, "dm-CommitSubmit"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_RELEASEFILE, RpcFunctionType.SERVER, "server-ReleaseFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_CMPFILE, RpcFunctionType.SERVER, "server-CmpFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_DM_RESOLVEDFILE, RpcFunctionType.SERVER, "dm-ResolvedFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_DM_RESOLVEDFAILED, RpcFunctionType.SERVER, "dm-ResolvedFailed"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_RECONCILEFILE, RpcFunctionType.SERVER, "server-ReconcileFile"),
		new RpcFunctionMetadata(RpcFunctionSpec.SERVER_RECONCILEADDS, RpcFunctionType.SERVER, "server-ReconcileAdds"),
	};
	
	
	/**
	 * Initialize the function maps.
	 */
	static {
		nameMap = new EnumMap<RpcFunctionSpec, RpcFunctionMetadata>(RpcFunctionSpec.class);
		encodingMap = new HashMap<String, RpcFunctionMetadata>();
		
		for (RpcFunctionMetadata md : functionMetadata) {
			nameMap.put(md.name, md);
			encodingMap.put(md.encoding, md);
		}
	}
	
	public static RpcFunctionMetadata getMetadata(RpcFunctionSpec name) {
		if (name == null) {
			throw new NullPointerError(
					"Null name passed to RpcFunction.getMetadata()");
		}
		RpcFunctionMetadata md = nameMap.get(name);
		
		if (md == null) {
			throw new P4JavaError("No metadata defined for RPC function spec: " + name);
		}
		return md;
	}
	
	public static RpcFunctionMetadata getMetadata(String encoding, boolean relaxedCheck) {
		if (encoding == null) {
			throw new NullPointerError(
					"Null encoding passed to RpcFunction.getMetadata()");
		}
		RpcFunctionMetadata md = encodingMap.get(encoding);
		
		if ((md == null) && !relaxedCheck) {
			throw new P4JavaError("No metadata defined for RPC function encoding: " + encoding);
		}
		return md;
	}
}
