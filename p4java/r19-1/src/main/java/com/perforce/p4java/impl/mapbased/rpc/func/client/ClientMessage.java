/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.NullPointerError;

/**
 * Perforce P4Java client error / info / warning messages.<p>
 * <p>
 * These are messages that the P4Java API itself generates
 * rather than receives from the Perforce server. Most
 * of these are copied fairly closely from the corresponding
 * C++ API class(es) and not all are currently used. The messages
 * defined here all use the common %arg% string interpolation
 * scheme that's used for messages coming in from the server
 * in (e.g.) client-Message packets.<p>
 * <p>
 * The errors here are not typically passed as-is back to the
 * end user, but are translated in the map-based server
 * implementation superclass ServerImpl to more generic
 * P4Java exceptions or filespec statuses.<p>
 * <p>
 * No attempt has (yet) been made to internationalise the
 * corresponding error strings, but that may happen in future
 * releases of the API.<p>
 * <p>
 * FIXME: what to do about P4Java-specific codes? -- HR.(Current
 * strategy is to simply use zero, as we don't actually extract error
 * codes anywhere yet).
 */

public class ClientMessage {

	/**
	 * Where the message originated, or which part
	 * of the command chain it refers to.
	 */
	public enum ClientMessageType {
		UNKNOWN,
		USAGE,
		CONNECTION,
		CLIENT,
		SERVER;
	}

	;

	/**
	 * Basic message ID. Order here is not important; explanation of
	 * each ID's meaning is generally given with the associated error
	 * string -- see the static ClientMessage array below...<p>
	 * <p>
	 * Not all codes are currently applicable, and some of these codes
	 * as P4Java-specific.<p>
	 * <p>
	 * Note that ID's are NOT the same as error codes; codes are given below
	 * for each message as copied from the C++ API, and those codes are
	 * significant (and should be the same as the corresponding C++ API
	 * client message code) but are basically intended to be opaque here.
	 */

	public enum ClientMessageId {
		UNKNOWN,
		CONNECT_FAILED,
		FATAL_CLIENT_ERROR,
		CANT_CLOBBER,
		CANT_CREATE_DIR,
		CANT_EDIT_FILE_TYPE,
		OUT_OF_RESOURCES,
		ASSUMING_FILE_TYPE,
		SUBSTITUTING_FILE_TYPE,
		CANT_ADD_FILE_TYPE,
		CANT_OVERWRITE_FILE,
		CANT_DELETE_FILE,
		CANT_CREATE_FILE,
		CANT_CREATE_FILE_TYPE,
		FILE_WRITE_ERROR,
		FILE_NONEXISTENT,
		FILE_MISSING_ASSUMING_TYPE,
		FILE_SEND_ERROR,
		FILE_MOVE_ERROR,
		MERGE_MESSAGE3,    // sic -- as named in the C++ API
		OS_FILE_READ_ERROR,
		CANT_CHMOD_FILE,
		DIGEST_MISMATCH, // 10.2 sync transfer error
		FILE_OPEN_ERROR,
		FILE_DECODER_ERROR, // 17.3 UTF16 decoder
		FILE_ENCODER_ERROR,  // 17.3 UTF16 encoder
		NO_MODIFIED_FILE,
		NOT_UNDER_CLIENT_PATH
	}

	;

	// Message array. Make sure UNKNOWN is element zero, but otherwise
	// order here is irrelevant.

	private static ClientMessage[] messages = new ClientMessage[]{

			new ClientMessage(ClientMessageId.UNKNOWN,
					0,
					"Unknown client error.",
					null),
			new ClientMessage(ClientMessageId.CONNECT_FAILED,
					1,
					"Connect to server failed; check server URI host and port specs.",
					null),
			new ClientMessage(ClientMessageId.FATAL_CLIENT_ERROR,
					2,
					"Fatal client error; disconnecting!",
					null),
			new ClientMessage(ClientMessageId.CANT_CLOBBER,
					4,
					"Can't clobber writable file %file%",
					new String[]{"file"}),
			new ClientMessage(ClientMessageId.CANT_CREATE_DIR,
					5,
					"can't create directory for %file%",
					new String[]{"file"}),
			new ClientMessage(ClientMessageId.CANT_EDIT_FILE_TYPE,
					7,
					"%type% - can't edit this type of file!",
					new String[]{"type"}),
			new ClientMessage(ClientMessageId.OUT_OF_RESOURCES,
					14,
					"Out of memory, network, or system resources",
					null),
			new ClientMessage(ClientMessageId.ASSUMING_FILE_TYPE,
					30,
					"%file% - %type%, assuming %type2%.",
					new String[]{"file", "type", "type2"}),
			new ClientMessage(ClientMessageId.SUBSTITUTING_FILE_TYPE,
					31,
					"%file% - using %type% instead of %type2%",
					new String[]{"file", "type", "type2"}),
			new ClientMessage(ClientMessageId.CANT_ADD_FILE_TYPE,
					32,
					"%file% - %type% file can't be added.",
					new String[]{"file", "type"}),
			new ClientMessage(ClientMessageId.CANT_OVERWRITE_FILE,
					34,
					"%file% - can't overwrite existing file.",
					new String[]{"file"}),
			new ClientMessage(ClientMessageId.FILE_NONEXISTENT,
					35,
					"%file% - file does not exist.",
					new String[]{"file"}),
			new ClientMessage(ClientMessageId.FILE_SEND_ERROR,
					0,
					"%file% - missing on client, assuming type %type%.",
					new String[]{"file", "type"}),
			new ClientMessage(ClientMessageId.CANT_DELETE_FILE,
					0,
					"operating system will not allow deletion of file %file% on client.",
					new String[]{"file"}),
			new ClientMessage(ClientMessageId.CANT_CREATE_FILE,
					0,
					"operating system will not allow creation of file %file% on client: %reason%.",
					new String[]{"file", "reason"}),
			new ClientMessage(ClientMessageId.CANT_CREATE_FILE_TYPE,
					0,
					"%type% file %file% can't be sync'd or created with this client program.",
					new String[]{"type", "file"}),
			new ClientMessage(ClientMessageId.FILE_WRITE_ERROR,
					0,
					"file %file% operating system write error: %reason%.",
					new String[]{"file", "reason"}),
			new ClientMessage(ClientMessageId.FILE_SEND_ERROR,
					0,
					"file send error for %file%: %reason%",
					new String[]{"file", "reason"}),
			new ClientMessage(ClientMessageId.FILE_MOVE_ERROR,
					0,
					"file move error for %file%: %reason%",
					new String[]{"file", "reason"}),
			new ClientMessage(ClientMessageId.MERGE_MESSAGE3,
					20,
					"Diff chunks: %yours% yours + %theirs% theirs + %both% both + %conflicting% conflicting",
					new String[]{"yours", "theirs", "both", "conflicting"}),
			new ClientMessage(ClientMessageId.OS_FILE_READ_ERROR,
					0,
					"%context%: %osmsg%",
					new String[]{"context", "osmsg"}),
			new ClientMessage(ClientMessageId.CANT_CHMOD_FILE,
					0,
					"can't change mode of file %file%: file does not exist locally",
					new String[]{"file"}),
			new ClientMessage(ClientMessageId.DIGEST_MISMATCH,
					37,
					"%clientFile% corrupted during transfer (or bad on the server) %clientDigest% vs %serverDigest%",
					new String[]{"clientFile", "clientDigest", "serverDigest"}),
			new ClientMessage(ClientMessageId.FILE_OPEN_ERROR,
					40,
					"Error opening file.",
					new String[]{}),
			new ClientMessage(ClientMessageId.FILE_DECODER_ERROR,
					0,
					"%file% - decoding error.",
					new String[]{"file"}),
			new ClientMessage(ClientMessageId.NO_MODIFIED_FILE,
					0,
					"Can't %action% modified file %file%",
					new String[]{"action", "file"}),
			new ClientMessage(ClientMessageId.NOT_UNDER_CLIENT_PATH,
					38,
					"File %clientFile% is not inside permitted filesystem path %clientPath%",
					new String[]{"clientFile", "clientPath"}),
	};

	/**
	 * Return the ClientMessage associated with this ID, if any.
	 * Never returns null, but will return the UNKNOWN message if it
	 * can't find a match.
	 */

	public static ClientMessage getClientMessage(ClientMessageId id) {
		if (id == null) {
			throw new NullPointerError(
					"Null error ID spec passed to ClientMessage.getClientMessage()");
		}

		for (ClientMessage msg : messages) {
			if (msg.getId() == id) {
				return msg;
			}
		}

		Log.warn(
				"Unmatched error ID spec in ClientMessage.getClientMessage()");

		return messages[0]; // i.e. UNKNOWN, unless someone moved it.
	}

	private ClientMessageId id = ClientMessageId.UNKNOWN;
	private int code = 0;
	private String[] msgs = null;
	private String[] msgParamNames = null;

	private ClientMessage(ClientMessageId id, int code,
	                      String msg, String[] msgParamNames) {
		this.id = id;
		this.code = code;
		this.msgs = new String[]{msg};
		this.msgParamNames = msgParamNames;
	}

	public ClientMessageId getId() {
		return id;
	}

	public void setId(ClientMessageId id) {
		this.id = id;
	}

	public String[] getMsgs() {
		return msgs;
	}

	public void setMsgs(String[] msgs) {
		this.msgs = msgs;
	}

	public String[] getMsgParamNames() {
		return msgParamNames;
	}

	public void setMsgParamNames(String[] msgParamNames) {
		this.msgParamNames = msgParamNames;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
}
