/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketField;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Defines the various Perforce RPC function names.<p>
 * <p>
 * If you add or change something here, make sure you also
 * change the corresponding definitions elsewhere in places
 * like the RpcFunction class.<p>
 */

public enum RpcFunctionSpec {

	NONE,

	// Protocol functions -- basically RPC meta functions:

	// (from: p4/msgs/p4tags.cc P4TAG::p_*)
	PROTOCOL_COMPRESS1,
	PROTOCOL_COMPRESS2,
	PROTOCOL_ECHO,
	PROTOCOL_ERRORHANDLER,
	PROTOCOL_FLUSH1,
	PROTOCOL_FLUSH2,
	PROTOCOL_FUNCHANDLER,
	PROTOCOL_PROTOCOL,
	PROTOCOL_RELEASE,
	PROTOCOL_RELEASE2,
	PROTOCOL_CRYPTO,

	// User functions, i.e. functions corresponding to
	// direct end-user commands:

	// (from: ??? p4/server/user.cc ???)
	USER_SPECIFIED,    // fake user command for execMapCmd usage with relaxed checking
	USER_LOGIN,
	USER_LOGIN2,
	USER_LOGOUT,
	USER_INTEG,
	USER_MOVE,
	USER_TAG,
	USER_MONITOR,
	USER_ANNOTATE,
	USER_DBSCHEMA,
	USER_EXPORT,
	USER_SHELVE,
	USER_UNSHELVE,
	USER_PROTECTS,
	USER_INTERCHANGES,
	USER_GREP,
	USER_ATTRIBUTE,
	USER_SPEC,
	USER_COPY,
	USER_CONFIGURE,
	USER_DISKSPACE,
	USER_ISTAT,
	USER_MERGE,
	USER_LOGTAIL,
	USER_TRUST,
	USER_DUPLICATE,
	USER_UNLOAD,
	USER_RELOAD,
	USER_POPULATE,
	USER_KEY,
	USER_KEYS,
	USER_SEARCH,
	USER_PROPERTY,
	USER_SIZES,
	USER_JOURNALWAIT,
	USER_RENAMEUSER,
	USER_GRAPH,
	USER_REPOS,
	USER_TRANSMIT,
	USER_LIST,
	USER_RETYPE,

	// (from: p4/msgs/p4tags.cc P4TAG::u_*)
	USER_ADD,
	USER_ADMIN,
	USER_BRANCH,
	USER_BRANCHES,
	USER_CHANGE,
	USER_CHANGES,
	USER_CLIENT,
	USER_CLIENTS,
	USER_COUNTER,
	USER_COUNTERS,
	USER_DELETE,
	USER_DEPOT,
	USER_DEPOTS,
	USER_DESCRIBE,
	USER_DIFF,
	USER_DIFF2,
	USER_DIRS,
	USER_EDIT,
	USER_FAILOVER,
	USER_FETCH,
	USER_FILELOG,
	USER_FILES,
	USER_FIX,
	USER_FIXES,
	USER_FLUSH,
	USER_FSTAT,
	USER_GROUP,
	USER_GROUPS,
	USER_HAVE,
	USER_HELP,
	USER_INFO,
	USER_INTEGRATE,
	USER_INTEGRATED,
	USER_JOB,
	USER_JOBS,
	USER_JOBSPEC,
	USER_LABEL,
	USER_LABELS,
	USER_LABELSYNC,
	USER_LOCK,
	USER_OBLITERATE,
	USER_OPENED,
	USER_PASSWD,
	USER_PRINT,
	USER_PROTECT,
	USER_PUSH,
	USER_RECONCILE,
	USER_REMOTE,
	USER_REMOTES,
	USER_RENAME,
	USER_REOPEN,
	USER_RESOLVE,
	USER_RESOLVED,
	USER_RESUBMIT,
	USER_REVERT,
	USER_REVIEW,
	USER_REVIEWS,
	USER_SET,
	USER_STREAM,
	USER_STREAMS,
	USER_SUBMIT,
	USER_SWITCH,
	USER_SYNC,
	USER_TRIGGERS,
	USER_TYPEMAP,
	USER_UNDO,
	USER_UNLOCK,
	USER_UNSUBMIT,
	USER_UNZIP,
	USER_USER,
	USER_USERS,
	USER_VERIFY,
	USER_WHERE,
	USER_ZIP,

	// Client functions -- functions the client must process in response
	// to a server request

	// (from: p4/msgs/p4tags.cc P4TAG::c_*)
	CLIENT_ACK,
	CLIENT_ACKMATCH,
	CLIENT_ACTIONRESOLVE,
	CLIENT_CHECKFILE,
	CLIENT_RECONCILEEDIT,
	CLIENT_CHMODFILE,
	CLIENT_CLOSEDIFF,
	CLIENT_CLOSEFILE,
	CLIENT_CLOSEMATCH,
	CLIENT_CLOSEMERGE,
	CLIENT_CONVERTFILE,
	CLIENT_CRYPTO,
	CLIENT_DELETEFILE,
	CLIENT_EDITDATA,
	CLIENT_ERRORPAUSE,
	CLIENT_FSTATINFO,
	CLIENT_FSTATPARTIAL,
	CLIENT_HANDLEERROR,
	CLIENT_INPUTDATA,
	CLIENT_MESSAGE,
	CLIENT_OPENDIFF,
	CLIENT_OPENFILE,
	CLIENT_OPENMATCH,
	CLIENT_OPENMERGE2,
	CLIENT_OPENMERGE3,
	CLIENT_OPENURL,
	CLIENT_OUTPUTBINARY,
	CLIENT_OUTPUTDATA,
	CLIENT_OUTPUTERROR,
	CLIENT_OUTPUTINFO,
	CLIENT_OUTPUTTEXT,
	CLIENT_PING,
	CLIENT_PROGRESS,
	CLIENT_PROMPT,
	CLIENT_MOVEFILE,
	CLIENT_RECONCILEADD,
	CLIENT_RECONCILEFLUSH,
	CLIENT_RECEIVEFILES,
	CLIENT_EXACTMATCH,
	CLIENT_SCANDIR,
	CLIENT_SENDFILE,
	CLIENT_SETPASSWORD,
	CLIENT_SSO,
	CLIENT_WRITEDIFF,
	CLIENT_WRITEFILE,
	CLIENT_WRITEMATCH,
	CLIENT_WRITEMERGE,

	// Client dm functions, a sub-species of client functions from our
	// point of view...

	// (from: ??? p4/server/user.cc ???)
	CLIENT_DM_PROMPT,
	CLIENT_DM_OPENFILE,
	CLIENT_DM_MOVEFILE,

	// Server-side functions that we as a client request the Perforce
	// server to process

	// (from: ??? p4/server/user.cc ???)
	SERVER_DM_LOGIN,
	SERVER_DM_LOGIN2,
	SERVER_DM_PASSWD,
	SERVER_DM_SUBMITCHANGE,
	SERVER_LBR_OPEN,
	SERVER_DM_LBR_OPEN,
	SERVER_LBR_WRITEFILE,
	SERVER_DM_SUBMITFILE,
	SERVER_DM_COMMITSUBMIT,
	SERVER_RELEASEFILE,
	SERVER_CMPFILE,
	SERVER_DM_RESOLVEDFILE,
	SERVER_DM_RESOLVEDFAILED,
	SERVER_RECONCILEFILE,
	SERVER_RECONCILEADDS,;

	/**
	 * Decode a RpcFunctionSpec from the passed-in string, assumed to be
	 * in RPC wire form.
	 */
	public static RpcFunctionSpec decode(String str) {
		if (str == null) {
			throw new NullPointerError(
					"Null string passed to RpcFunction.decode()");
		}
		return RpcFunction.getMetadata(str, false).getName();
	}

	/**
	 * A version of decode that handles the specialised case of
	 * relaxed checking for USER commands (and user commands *only*).
	 */
	public static RpcFunctionSpec decode(String str, boolean relaxedCheck) {
		if (str == null) {
			throw new NullPointerError(
					"Null string passed to RpcFunction.decode()");
		}

		RpcFunctionMetadata metadata = RpcFunction.getMetadata(str, relaxedCheck);
		if (metadata == null) {
			// Need to fake a suitable answer. This is incredibly dangerous,
			// but it's what the user asked us to do in this specific case...

			return RpcFunctionSpec.USER_SPECIFIED;
		}

		return metadata.getName();
	}

	/**
	 * Decode a RpcFunctionSpec from a string passed in from the upper
	 * levels of P4Java as an end-user command, e.g. decode "depots" to
	 * USER_DEPOTS. This is subtly different to decoding it from the wire;
	 * also, it's only applicable for user commands.
	 */
	public static RpcFunctionSpec decodeFromEndUserCmd(String str, boolean relaxedCheck) {
		if (str == null) {
			throw new NullPointerError(
					"Null string passed to RpcFunction.decodeFromEndUserCmd()");
		}

		return decode(RpcFunctionType.USER.getEncodingPrefix() + str, relaxedCheck);
	}

	public String getEncoding() {
		return RpcFunction.getMetadata(this).getEncoding();
	}

	public RpcFunctionType getType() {
		return RpcFunction.getMetadata(this).getType();
	}

	public void marshal(ByteBuffer buf) {
		if (buf == null) {
			throw new NullPointerError(
					"Null byte buffer passed to RpcFunctionSpec.marshal()");
		}

		try {
			RpcPacketField.marshal(buf,
					RpcFunctionMapKey.FUNCTION,
					this.getEncoding().getBytes(CharsetDefs.DEFAULT.name()));
		} catch (UnsupportedEncodingException uee) {
			// This shouldn't happen...
			Log.exception(uee);
			throw new P4JavaError(uee);
		}
	}
}
