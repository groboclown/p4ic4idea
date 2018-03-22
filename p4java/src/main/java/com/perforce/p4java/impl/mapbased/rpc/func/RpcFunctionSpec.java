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
 * 
 * If you add or change something here, make sure you also
 * change the corresponding definitions elsewhere in places
 * like the RpcFunction class.<p>
 */

public enum RpcFunctionSpec {
	
	NONE,
	
	// Protocol functions -- basically RPC meta functions:
	
	PROTOCOL_PROTOCOL,
	PROTOCOL_FLUSH1,
	PROTOCOL_FLUSH2,
	PROTOCOL_RELEASE,
	PROTOCOL_RELEASE2,
	PROTOCOL_CRYPTO,
	PROTOCOL_COMPRESS1,
	PROTOCOL_COMPRESS2,

	// User functions, i.e. functions corresponding to
	// direct end-user commands:

	USER_SPECIFIED,	// fake user command for execMapCmd usage with relaxed checking
	USER_DEPOTS,
	USER_INFO,
	USER_FILES,
	USER_CLIENTS,
	USER_JOBS,
	USER_CHANGES,
	USER_LOGIN,
	USER_LOGOUT,
	USER_CLIENT,
	USER_FSTAT,
	USER_SYNC,
	USER_CHANGE,
	USER_DESCRIBE,
	USER_OPENED,
	USER_EDIT,
	USER_ADD,
	USER_DELETE,
	USER_REVERT,
	USER_SUBMIT,
	USER_FILELOG,
	USER_PRINT,
	USER_WHERE,
	USER_HAVE,
	USER_REOPEN,
	USER_DIRS,
	USER_INTEG,
	USER_RESOLVE,
	USER_RESOLVED,
	USER_FIXES,
	USER_JOBSPEC,
	USER_FIX,
	USER_JOB,
	USER_LOCK,
	USER_UNLOCK,
	USER_DIFF,
	USER_COUNTERS,
	USER_USERS,
	USER_MOVE,
	USER_LABELS,
	USER_LABEL,
	USER_LABELSYNC,
	USER_TAG,
	USER_MONITOR,
	USER_GROUP,
	USER_GROUPS,
	USER_BRANCH,
	USER_BRANCHES,
	USER_COUNTER,
	USER_INTEGRATED,
	USER_ANNOTATE,
	USER_DBSCHEMA,
	USER_EXPORT,
	USER_SHELVE,
	USER_UNSHELVE,
	USER_PROTECTS,
	USER_PROTECT,
	USER_USER,
	USER_REVIEWS,
	USER_REVIEW,
	USER_DIFF2,
	USER_INTERCHANGES,
	USER_GREP,
	USER_DEPOT,
	USER_ATTRIBUTE,
	USER_SPEC,
	USER_COPY,
	USER_CONFIGURE,
	USER_PASSWD,
	USER_DISKSPACE,
	USER_OBLITERATE,
	USER_STREAMS,
	USER_STREAM,
	USER_ISTAT,
	USER_MERGE,
	USER_LOGTAIL,
	USER_TRUST,
	USER_RECONCILE,
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
	USER_TRIGGERS,
	USER_VERIFY,
	USER_RENAMEUSER,
	USER_GRAPH,
	USER_REPOS,
	USER_TRANSMIT,
	USER_LIST,
	USER_RETYPE,

	// Client functions -- functions the client must process in response
	// to a server request
	
	CLIENT_OPENFILE,
	//TODO:     CLIENT_OPENDIFF,
    //TODO:     CLIENT_OPENMATCH,
    CLIENT_WRITEFILE,
    //TODO:     CLIENT_WRITEDIFF,
    //TODO:     CLIENT_WRITEMATCH,
    CLIENT_CLOSEFILE,
    //TODO:     CLIENT_CLOSEDIFF,
    //TODO:     CLIENT_CLOSEMATCH,
    //TODO:     CLIENT_ACKMATCH,
    
	CLIENT_DELETEFILE,
	CLIENT_CHMODFILE,
    CLIENT_CHECKFILE,
    //TODO:     CLIENT_CONVERTFILE,
	CLIENT_RECONCILEEDIT,
	CLIENT_MOVEFILE,
	
    //TODO: 	CLIENT_ACTIONRESOLVE,

    CLIENT_OPENMERGE2,
    CLIENT_OPENMERGE3,
	CLIENT_WRITEMERGE,
	CLIENT_CLOSEMERGE,

    CLIENT_RECEIVEFILES,
    CLIENT_SENDFILE,
    //TODO:     CLIENT_EDITDATA,
	CLIENT_INPUTDATA,
	CLIENT_RECONCILEADD,
    CLIENT_RECONCILEFLUSH,
    //TODO:     CLIENT_EXACTMATCH,
	
	CLIENT_PROMPT,
	CLIENT_PROGRESS,
    //TODO:     CLIENT_ERRORPAUSE,
    //TODO:     CLIENT_HANDLEERROR,
    CLIENT_MESSAGE,
	CLIENT_OUTPUTERROR,
	CLIENT_OUTPUTINFO,
	CLIENT_OUTPUTDATA,
	CLIENT_OUTPUTTEXT,
	CLIENT_OUTPUTBINARY,
    CLIENT_FSTATINFO,
    //TODO: CLIENT_FSTATPARTIAL,

    //TODO: CLIENT_PING,
    CLIENT_ACK,
	
	CLIENT_CRYPTO,
	CLIENT_SETPASSWORD,
	CLIENT_SSO,
	
	// Client dm functions, a sub-species of client functions from our
	// point of view...
	
	CLIENT_DM_PROMPT,
	CLIENT_DM_OPENFILE,
	CLIENT_DM_MOVEFILE,
	
	// Server-side functions that we as a client request the Perforce
	// server to process
	
	SERVER_DM_LOGIN,
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
	SERVER_RECONCILEADDS,
	;
	
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
