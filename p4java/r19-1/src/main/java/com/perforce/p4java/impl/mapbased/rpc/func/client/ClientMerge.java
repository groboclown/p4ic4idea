/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.FileDecoderException;
import com.perforce.p4java.exception.FileEncoderException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMessage.ClientMessageId;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5Digester;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherResult;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcInputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcOutputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;

/**
 * Support methods for the RPC merge protocols. Will grow as we implement more types
 * of merge; current version is very bare bones and oriented solely at non-interactive
 * three-way merges of text files.
 * 
 *
 */

public class ClientMerge {
	
	/**
	 * Possible choices available to a merge / resolve; should be
	 * fairly self-explanatory.
	 */
	public enum ResolveChoice {
		SKIP,
		YOURS,
		THEIRS,
		EDIT,	// Not currently used
		MERGED
	};

	public static final String MARKER_ORIGINAL = ">>>> ORIGINAL ";
	public static final String MARKER_THEIRS = "==== THEIRS ";
	public static final String MARKER_YOURS = "==== YOURS ";
	public static final String MARKER_BOTH = "==== BOTH ";
	public static final String MARKER_END = "<<<<";
	
	public static final String DEFAULT_TMPFILE_PFX = "p4j";
	public static final String DEFAULT_TMPFILE_SFX = ".p4j";
	
	public static final String SYSTEM_TMPDIR_PROPS_KEY = "java.io.tmpdir";
	public static final String SYSTEM_TMPDIR_DEFAULT = "/tmp";
	
	public static final String TRACE_PREFIX = "ClientMerge";
	
	protected static final String MERGE_STATE_KEY = "MergeState";
	
	protected static final String MERGE_BASE_TMP_FILE_KEY = "tmpFileBase";
	protected static final String MERGE_BASE_TMP_STREAM_KEY = "tmpFileBaseStream";
	
	protected static final String MERGE_THEIRS_TMP_FILE_KEY = "tmpFileTheirs";
	protected static final String MERGE_THEIRS_TMP_STREAM_KEY = "tmpFileTheirsStream";
	
	protected static final String MERGE_YOURS_TMP_FILE_KEY = "tmpFileYours";
	protected static final String MERGE_YOURS_TMP_STREAM_KEY = "tmpFileYoursStream";
	
	private Properties props = null;	
	private String tmpDirName = null;

	@SuppressWarnings("unused")
	private static final String COPY_MERGE = "copy"; // here for completeness
	private static final String SAFE_MERGE = "safe";
	private static final String AUTO_MERGE = "auto";
	private static final String FORCE_MERGE = "force";
	private static final String MERGED_EDITED = "edit";
	@SuppressWarnings("unused")
	private static final String MERGED_FORCE = "force"; // not currently used
	private static final String MERGED_YOURS = "yours";
	private static final String MERGED_THEIRS = "theirs";
	private static final String MERGED_MERGED = "merged";
	private static final String MERGE_UNFORCED = "no";
	
	// Transliteration of bit value constant defines from the C++ API; see
	// the comments below for an explanation.
	
	private static final int SEL_BASE = 0x01;
	private static final int SEL_LEG1 = 0x02;
	private static final int SEL_LEG2 = 0x04;
	private static final int SEL_RSLT = 0x08;
	private static final int SEL_ALL = (SEL_BASE|SEL_LEG1|SEL_LEG2|SEL_RSLT);
	private static final int SEL_CONF = 0x10;

	public ClientMerge(Properties props) {
		this.props = props;
		this.tmpDirName = RpcPropertyDefs.getProperty(this.props,
							PropertyDefs.P4JAVA_TMP_DIR_KEY,
									System.getProperty(SYSTEM_TMPDIR_PROPS_KEY));
		
		if (tmpDirName == null) {
			// This can really only happen if someone has nuked or played with
			// the JVM's system props before we get here... the default will
			// work for most non-Windows boxes in most cases, and may not be
			// needed in many cases anyway.
			
			tmpDirName = SYSTEM_TMPDIR_DEFAULT;
			
			Log.warn("Unable to get tmp name from P4 props or System; using "
					+ tmpDirName + " instead");
			
		}
	}
	
	/**
	 * Implement a non-interactive version of the three-way client-side merge.<p>
	 */
	protected RpcPacketDispatcherResult clientOpenMerge3(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap, boolean twoWayMerge)
								throws ConnectionException {		
		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String handle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		String resultTypeStr = (String) resultsMap.get(RpcFunctionMapKey.TYPE2);
		String clientTypeStr = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST);
		String baseName = (String) resultsMap.get(RpcFunctionMapKey.BASENAME);
		String theirName = (String) resultsMap.get(RpcFunctionMapKey.THEIRNAME);
		String yourName = (String) resultsMap.get(RpcFunctionMapKey.YOURNAME);
		String showAll = (String) resultsMap.get(RpcFunctionMapKey.SHOWALL);
		
		// old servers do not send result type, apparently...
		
		if (resultTypeStr == null) {
		    resultTypeStr = clientTypeStr;
		}
		
		ClientMergeState mergeState = null;
		
		RpcHandler handler = cmdEnv.getHandler(handle);
		
		if (handler == null) {
			handler = cmdEnv.new RpcHandler(handle, false, null);
			cmdEnv.addHandler(handler);
		} else {
			// Clear out any current key mappings to ensure files/streams aren't re-used:
			handler.getMap().remove(MERGE_STATE_KEY);
		}
		
		handler.setError(false);

        RpcPerforceFileType clientType = RpcPerforceFileType.decodeFromServerString(
                                                                            clientTypeStr);
        ClientLineEnding clientLineEnd = ClientLineEnding.decodeFromServerString(
                clientTypeStr, clientType);
		RpcPerforceFileType resultType = RpcPerforceFileType.decodeFromServerString(
																			resultTypeStr);
		ClientLineEnding resultLineEnd = ClientLineEnding.decodeFromServerString(
		        resultTypeStr, resultType);

		if (cmdEnv.getCmdSpec().getInMap() != null) {
			
			// This is an external stream merge; not much to do here except
			// load up the merge state and return...
			
			String tmpFileName = (String) cmdEnv.getCmdSpec().getInMap().get(
												Client.MERGE_TMP_FILENAME_KEY);
			mergeState = new ClientMergeState(clientPath, true, clientType, clientLineEnd, resultType,
			        resultLineEnd, this.tmpDirName, rpcConnection.getClientCharset());
			mergeState.setExternalTmpFilename(tmpFileName);
			mergeState.setShowAll(false);
			mergeState.setTwoWayMerge(twoWayMerge);
			mergeState.setTheirName(theirName);
			handler.getMap().put(MERGE_STATE_KEY, mergeState);
			
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
			
		} else {
			
			// "Normal" three-way merge (limited in our case to non-interactive decisions);
			// this means we have a bit of setting up to do, in contrast to the external
			// stream merge case...
			
			mergeState = new ClientMergeState(clientPath, false, clientType, clientLineEnd, resultType, resultLineEnd,
												this.tmpDirName, rpcConnection.getClientCharset());
			mergeState.setTwoWayMerge(twoWayMerge);
			mergeState.setBaseDigest(digest);
			handler.getMap().put(MERGE_STATE_KEY, mergeState);
			
			try {
				mergeState.setBaseName(baseName);
				mergeState.setTheirName(theirName);
				mergeState.setYourName(yourName);
				mergeState.openMergeFiles(rpcConnection);
				mergeState.setShowAll(showAll != null);

			} catch (IOException ioexc) {
				handler.setError(true);
				cmdEnv.handleResult(
						new RpcMessage(
								ClientMessageId.CANT_CREATE_FILE,
								MessageSeverityCode.E_FAILED,
								MessageGenericCode.EV_CLIENT,
								new String[] {clientPath, ioexc.getLocalizedMessage()}
							).toMap()
					);
				
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
		}	
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}

	/**
	 * Write merge data to the client if necessary.<p>
	 * 
	 * For the external stream merge case, this is a no op, but for the
	 * normal three-way merge case, we not only need to write the relevant
	 * files, but we also need to keep track of the differences so we can determine
	 * whether an automatic and / or safe merge is even possible. This determination
	 * is done in clientCloseMerge using chunk difference counts as supplied by the
	 * server in the bits parameter.<p>
	 * 
	 * For the two-way merge, all we really need to do is write the "their" file
	 * and keep digesting under the covers...
	 */
	protected RpcPacketDispatcherResult clientWriteMerge(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap)
								throws ConnectionException {
		String handle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		RpcHandler handler = cmdEnv.getHandler(handle);
		String bitsStr = (String) resultsMap.get(RpcFunctionMapKey.BITS);
		byte[] data = (byte[]) resultsMap.get(RpcFunctionMapKey.DATA);
		ClientMergeState mergeState = null;
		int bits = 0;
		@SuppressWarnings("unused") // used for debugging
		int markersInFile = 0;
		boolean needNewline = false;
		
		String marker = MARKER_ORIGINAL;

		if (handler == null) {
			throw new NullPointerError("Null client handler in clientWriteMerge");
		}
		
		if (handler.isError()) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		try {
			mergeState = (ClientMergeState) handler.getMap().get(
													ClientMerge.MERGE_STATE_KEY);
		} catch (ClassCastException cce) {
			Log.error("Bad client handler class in clientWriteMerge: "
					+ cce.getLocalizedMessage());
			Log.exception(cce);
			throw new NullPointerError("Bad client handler class in clientWriteMerge: "
					+ cce.getLocalizedMessage());
		}
		
		if (mergeState == null) {
			throw new NullPointerError("Null merge state in clientWriteMerge");
		}
		
		if (mergeState.isExternalStreamMerge()) {
			// We ignore the data here because we already know the merge
			// result, which is in the external stream we deal with later
			// in closeMerge().
			
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		} else if (mergeState.isTwoWayMerge()) {
			try {
				mergeState.writeTheirChunk(data);
			} catch (IOException ioexc) {
				Log.error("I/O exception in clientWriteMerge: "
						+ ioexc.getLocalizedMessage());
				Log.exception(ioexc);
				handler.setError(true);
			} catch (FileDecoderException e) {
				Log.error("Charset converstion exception in clientWriteMerge: "
						+ e.getLocalizedMessage());
				Log.exception(e);
				handler.setError(true);
			} catch (FileEncoderException e) {
				Log.error("Charset converstion exception in clientWriteMerge: "
						+ e.getLocalizedMessage());
				Log.exception(e);
				handler.setError(true);
			}
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		if (bitsStr != null) {
			try {
				bits = new Integer(bitsStr);
			} catch (Throwable thr) {
				Log.error("Unexpected exception in clientWriteMerge: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
				handler.setError(true);
			}
		}
		/*
		 * bits = 0 means no more output; otherwise the bits are set according to what
		 * outputfile is to take the next piece.  The length of DiffMergeRead
		 * can be zero while the bits returned are non-zero: this indicates a
		 * zero length chunk to be placed in the output file.
		 *
		 * SEL_CONF indicates a conflict, and is set for each of the legs that
		 * are in conflict, including the base.  Thus for a conflict the follow
		 * sequence will be seen: 
		 *
		 *		SEL_CONF | SEL_BASE
		 *		SEL_CONF | SEL_LEG1 | SEL_RSLT
		 *		SEL_CONF | SEL_LEG2 | SEL_RSLT
		 *
		 * If changes are identical both lines, they are not in conflict.  The
		 * sequence is:
		 *
		 *		SEL_BASE
		 *		SEL_LEG1 | SEL_LEG2 | SEL_RSLT
		 *
		 * SEL_ALL indicates chunks synchronized between all 3 files.  The
		 * actual text comes from LEG2, so that if the underlying diff is
		 * ignoring certain changes (like whitespace), the resulting merge
		 * will have the last leg (typically "yours") rather than the original
		 * unchanged base.
		 */
		
		// Logic below lifted pretty much as-is from the C++ API equivalent;
		// note deliberate fall-throughs and commented-out marker logic (which
		// will be implemented properly later for real interactive resolves).
		
		int oldBits = mergeState.getOldBits();
		
		try {
			if ((oldBits != 0) && (oldBits != bits)) {
				switch (bits) {
					case SEL_BASE | SEL_CONF:
						mergeState.incrConflictChunks();
						
						// DELIBERATE FALL-THROUGH...
						
					default:
				    case SEL_BASE:
				    case SEL_BASE | SEL_LEG1:
				    case SEL_BASE | SEL_LEG2:
				    	marker = MARKER_ORIGINAL + mergeState.getBaseName();
				    	break;
				    	
				    case SEL_LEG1 | SEL_RSLT:
						mergeState.incrTheirChunks();
						// DELIBERATE FALL-THROUGH...
	
				    case SEL_LEG1 | SEL_RSLT | SEL_CONF:
						marker = MARKER_THEIRS + mergeState.getTheirName();
						break;
	
				    case SEL_LEG2 | SEL_RSLT:
						mergeState.incrYourChunks();
						// DELIBERATE FALL-THROUGH...
	
				    case SEL_LEG2 | SEL_RSLT | SEL_CONF:
				    	marker = MARKER_YOURS + mergeState.getYourName();
				    	break;
	
				    case SEL_LEG1 | SEL_LEG2 | SEL_RSLT:
				    	marker = MARKER_BOTH + mergeState.getTheirName() + " " + mergeState.getYourName();
						mergeState.incrBothChunks();
						break;
	
				    case SEL_ALL:
				    	marker = MARKER_END;
						break;
				}

				if (mergeState.isShowAll() || ((bits & SEL_CONF) != 0)
						|| ((bits == SEL_ALL) && (oldBits & SEL_CONF) != 0)) {

					mergeState.writeMarker(
									needNewline ? "\n" : ""
									+ marker
									+ "\n");
					
					markersInFile++;
					if (needNewline) {
						needNewline = false;
					}
				}
			}		
			
			mergeState.setOldBits(bits);
			
			if ((data != null) && (data.length > 0)) {
				// Note: apparently we sometimes get back empty buffers as marker placeholders,
				// so we don't always get to execute this block...
	
				if ((bits & SEL_BASE) != 0) {
					mergeState.writeBaseChunk(data);
				}
				
				if ((bits & SEL_LEG1) != 0) {
					mergeState.writeTheirChunk(data);
				}
				
				if ((bits & SEL_LEG2) != 0) {
					// NOTE: this doesn't actually write anything to the file,
					// just updates the digest:
					
					mergeState.writeYourChunk(data);
				}
				
				if (((bits & SEL_RSLT) != 0) || (bits == (SEL_BASE | SEL_CONF))) {
					mergeState.writeResultChunk(data);
				}
	
				// If this block didn't end in a linefeed, we may need to add
				// one before putting out the next marker.  This can happen if
				// some yoyo has a conflict on the last line of a file, and that
				// line has no newline. (comment from the C++ API...).
	
				if (data[data.length - 1] == '\n') {
					needNewline = true;
				}
				
			}
		} catch (IOException ioexc) {
			Log.error("I/O exception in clientWriteMerge: "
					+ ioexc.getLocalizedMessage());
			Log.exception(ioexc);
			handler.setError(true);
		} catch (FileDecoderException e) {
			Log.error("Charset converstion exception in clientWriteMerge: "
					+ e.getLocalizedMessage());
			Log.exception(e);
			handler.setError(true);
		} catch (FileEncoderException e) {
			Log.error("Charset converstion exception in clientWriteMerge: "
					+ e.getLocalizedMessage());
			Log.exception(e);
			handler.setError(true);
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Close the merge write (above) and act on the results. The actions needed here
	 * depend on whether we're doing a merge from external stream or a safe auto merge.
	 */
	protected RpcPacketDispatcherResult clientCloseMerge(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap)
			throws ConnectionException {
		
		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String handle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		String mergeConfirm = (String) resultsMap.get(RpcFunctionMapKey.MERGE_CONFIRM);
		String mergeDecline = (String) resultsMap.get(RpcFunctionMapKey.MERGE_DECLINE);
		String mergePerms = (String) resultsMap.get(RpcFunctionMapKey.MERGE_PERMS);
		String mergeAuto = (String) resultsMap.get(RpcFunctionMapKey.MERGE_AUTO);
		
		String mergeResponse = mergeConfirm;
		String mergeHow = null;
		String mergeForced = null;
		ClientMergeState mergeState = null;
		RpcHandler handler = cmdEnv.getHandler(handle);
		String digest = null;
		
		if (handler == null) {
			throw new NullPointerError("Null client handler in clientWriteMerge");
		}

		try {
			mergeState = (ClientMergeState) handler.getMap().get(
													ClientMerge.MERGE_STATE_KEY);
		} catch (ClassCastException cce) {
			Log.error("Bad client handler class in clientCloseMerge: "
					+ cce.getLocalizedMessage());
			Log.exception(cce);
			throw new NullPointerError("Bad client handler class in clientCloseMerge: "
					+ cce.getLocalizedMessage());
		}
		
		if (mergeState == null) {
			throw new NullPointerError("Null merge state in clientWriteMerge");
		}
		
		if ((handler != null) && handler.isError()) {
			mergeResponse = mergeDecline;
		} else {
			
			boolean skip = false;
			if (cmdEnv.getCmdSpec().getInMap() != null) {
				String theirName = mergeState.getTheirName();
				if (theirName != null) {
					int theirRev = Integer.parseInt(theirName
							.substring(theirName.lastIndexOf("#") + 1));
					if (cmdEnv.getCmdSpec().getInMap()
							.containsKey(Client.MERGE_START_FROM_REV_KEY)) {
						int startFromRev = (Integer) cmdEnv.getCmdSpec()
								.getInMap()
								.get(Client.MERGE_START_FROM_REV_KEY);
						if (startFromRev != -1 && startFromRev > theirRev) {
							skip = true;
						}
					}
					if (cmdEnv.getCmdSpec().getInMap()
							.containsKey(Client.MERGE_END_FROM_REV_KEY)) {
						int endFromRev = (Integer) cmdEnv.getCmdSpec()
								.getInMap().get(Client.MERGE_END_FROM_REV_KEY);
						if (endFromRev != -1 && endFromRev < theirRev) {
							skip = true;
						}
					}
				}
			}
			if (skip) {
				mergeResponse = mergeDecline;
			} else if (mergeState.isExternalStreamMerge()) {
				// External stream merge; just get the tmp merge stream and declare
				// it to be the merge result.
				
				String tmpFileName = (String) cmdEnv.getCmdSpec().getInMap().get(
													Client.MERGE_TMP_FILENAME_KEY);
				
				if ((clientPath != null) && (tmpFileName != null)) {
					// Copy the tmp file to the target file:
					
					RpcPerforceFile tmpFile = new RpcPerforceFile(tmpFileName,
														RpcPerforceFileType.FST_TEXT);
					if (!tmpFile.renameTo(new File(clientPath), true)) {
						// Was unable to rename or even copy the file to its target;
						// usually the sign of permissions problems, etc., that we can't
						// fix on the fly, so report it to the user and the log and don't
						// ack a confirm back to the server...
						
						Log.error("Rename failed completely in resolveFile (cause unknown); source file: "
								+ clientPath
								+ "; target file: "
								+ tmpFileName);
						
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.FILE_MOVE_ERROR,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[] {clientPath, "(cause unknown)"}
									).toMap()
							);
						mergeResponse = mergeDecline;
					} else {
						//Attempt to update perms if they are specified
						if (mergePerms != null) {
							ISystemFileCommandsHelper fileCommands = SysFileHelperBridge
									.getSysFileCommands();
							if (mergePerms
									.equalsIgnoreCase(ClientSystemFileCommands.PERMS_RW)) {
								fileCommands.setWritable(clientPath, true);
							} else {
								fileCommands.setWritable(clientPath, false);
							}
						}

						try {
							MD5Digester digester = new MD5Digester();
							RpcInputStream stream = new RpcInputStream(tmpFile, rpcConnection.getClientCharset());

							byte[] bytes = new byte[1024 * 64];
							int bytesRead;
							while ((bytesRead = stream.read(bytes)) > 0) {
								byte[] readBytes = new byte[bytesRead];
								System.arraycopy(bytes, 0, readBytes, 0, bytesRead);
								digester.update(readBytes);
							}
							stream.close();

							digest = digester.digestAs32ByteHex();

						} catch (IOException e) {
							Log.error("Unexpected I/O exception in digest calculation: " + e.getLocalizedMessage());
							Log.exception(e);
						} catch (FileEncoderException e) {
							Log.error("Unexpected Encoding exception in digest calculation: " + e.getLocalizedMessage());
							Log.exception(e);
						}

						mergeHow = MERGED_EDITED;
					}
				}
			} else {
				if (mergeState.isTwoWayMerge()) {
					// Need to calculate fake chunk nunmbers based on the digests.
					// The logic below has been copied wholesale from the corresponding
					// C++ API methods...
					
					// Assign the chunk variables - there can be only one.
				    //
				    // base == yours,   base != theirs,  yours != theirs   1 theirs
				    // base != yours,   base != theirs,  yours != theirs   1 conflicting
				    // base != yours,   base != theirs,  yours == theirs   1 both
				    // base != yours,   base == theirs,  yours != theirs   1 yours
					
					String baseDigest = mergeState.getBaseDigest(); // from the initial open
					String yourDigest = mergeState.getYourDigestString();	// ditto
					String theirDigest = mergeState.getTheirDigestString(); // as calculated during the writes
					
					// Theoretically, the string above should be non-null, but I'm not taking this
					// on faith just yet...
					
					if ((baseDigest != null) && (yourDigest != null) && baseDigest.equals(yourDigest)) {
						if ((theirDigest != null) && !baseDigest.equals(theirDigest)) {
							mergeState.setTheirChunks(1);
						}
					} else if ((baseDigest != null) && (theirDigest != null) && !baseDigest.equals(theirDigest)) {
						if ((yourDigest != null) && !yourDigest.equals(theirDigest)) {
							mergeState.setConflictChunks(1);
						} else {
							mergeState.setBothChunks(1);
						}
					} else {
						mergeState.setYourChunks(1);
					}
				}
				// Traditional full non-interactive three-way auto / safe merge; need to
				// determine whether there were any conflicting chunks, and, if so, reject
				// the merge, otherwise accept the suitable source based on chunk counts.
				
				// First chmod "yours" if necessary; original commentary in C++ API follows below
				// (note that whatever mergePerms is set to, the C++ API just sets your file rw):
			    // Make user's file writeable for the duration of the resolve.
			    // We only do this if mergePerms is set for two reasons: 1) then
			    // we can be sure to revert the file to the proper perms below,
			    // 2) only servers which set mergePerms are smart enough to leave
			    // files opened for integ r/o to begin with.

				ResolveChoice autoChoice = autoResolve(mergeState, mergeAuto);
				
				if (mergePerms != null) {
					ISystemFileCommandsHelper fileCommands
													= SysFileHelperBridge.getSysFileCommands();
						if ((clientPath != null) && !fileCommands.setWritable(clientPath, true)) {
							// Note that failure here is OK on most platforms
							// and situations, but we should probably do a better job
							// in general -- HR.
							
							Log.warn("Unable to set merge target '" + clientPath
									+ "' permissions to writable; merge results should not be affected");
						}
				}
				
				switch (autoChoice) {
					case SKIP:
						// Just politely decline the merge:
						mergeResponse = mergeDecline;
						break;
						
					case YOURS:
						mergeHow = MERGED_YOURS;
						digest = mergeState.getYourDigestString();
						break;
						
					case THEIRS:
						mergeHow = MERGED_THEIRS;
						mergeForced = MERGE_UNFORCED;	// True for current limited implementation only
						digest = mergeState.getTheirDigestString();
						break;
						
					case MERGED:
						mergeHow = MERGED_MERGED;
						digest = mergeState.getMergeDigestString();
						break;
						
					case EDIT:
						mergeHow = MERGED_EDITED;
						break;
						
					default:	// Treat this like a skip for the moment
						mergeResponse = mergeDecline;
						break;
				}
				
				try {
					if (!mergeState.finishMerge(autoChoice)) {					
						// Was unable to rename or even copy the file to its target;
						// usually the sign of permissions problems, etc., that we can't
						// fix on the fly, so report it to the user and the log and don't
						// ack a confirm back to the server...
						
						Log.error("Rename failed completely in resolveFile (cause unknown); source file: "
								+ clientPath == null ? "<unknown>" : clientPath);
						
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.FILE_MOVE_ERROR,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[] {clientPath == null ? "<unknown>" : clientPath,
															"(cause unknown)"}
									).toMap()
							);
						mergeResponse = mergeDecline;
					} else {
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.MERGE_MESSAGE3,
										MessageSeverityCode.E_INFO,
										MessageGenericCode.EV_CLIENT,
										// "yours", "theirs", "both", "conflicting"
										new String[] {"" + mergeState.getYourChunks(),
														"" + mergeState.getTheirChunks(),
														"" + mergeState.getBothChunks(),
														"" + mergeState.getConflictChunks()}
									).toMap()
							);
					}
				} catch (IOException exc) {
					// Error in renaming; we'll decline the merge...
					mergeResponse = mergeDecline;
					
					Log.error("Unexpected I/O exception in closeMerge: " + exc.getLocalizedMessage());
					Log.exception(exc);
				}
				
				// Set permissions back to whatever mergePerms is:
				
				if (mergePerms != null) {
					ISystemFileCommandsHelper fileCommands
													= SysFileHelperBridge.getSysFileCommands();
					
					if ((clientPath != null) && !fileCommands.setWritable(clientPath,
								mergePerms.equalsIgnoreCase(ClientSystemFileCommands.PERMS_RW))) {
						// Note that failure here is OK on most platforms
						// and situations, but we should probably do a better job
						// in general -- HR.
						
						Log.warn("Unable to set merge target '" + clientPath
								+ "' permissions back after merge; merge results should not be affected");
					}	
				}
			}
			
			// Send a result back:
			
			Map<String,Object> respMap = new HashMap<String, Object>();
			
			if (mergeHow != null) {
				respMap.put(RpcFunctionMapKey.MERGE_HOW, mergeHow);
			}
			if (mergeForced != null) {
				respMap.put(RpcFunctionMapKey.FORCE, mergeForced);
			}
			if (digest != null) {
				respMap.put(RpcFunctionMapKey.DIGEST, digest);
			}
			
			for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
				if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)
						&& !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.TYPE)
						&& !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.STATUS)) {
					respMap.put(entry.getKey(), entry.getValue());
				}
			}

			RpcPacket respPacket = RpcPacket.constructRpcPacket(
					mergeResponse,
					respMap,
					null);
			
			rpcConnection.putRpcPacket(respPacket);
		}

		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Determine what we should do if we're doing an auto or safe merge.
	 * Note that this version is only for non-interactive safe / auto merges;
	 * later versions can overload this or extend the logic here radically.
	 * Note also that we've added support for two-way merges (usually done
	 * for binary files); this support may evolve with experience...
	 */
	
	private ResolveChoice autoResolve(ClientMergeState mergeState, String mergeAuto) {
		int conflictChunks = mergeState.getConflictChunks();
		int theirChunks = mergeState.getTheirChunks();
		int yourChunks = mergeState.getYourChunks();
		boolean safeMerge = false;
		boolean autoMerge = false;
		boolean forceMerge = false;

		/*
		 * Automatic logic (copied from the C++ API):
		 */

		/* If the files are identical and it's a two-way merge, autoaccept theirs */
		/* We prefer theirs over ours, because then integration history */
		/* marks it as a "copy" and we know the files are now identical. */
		/* Note that this silently swallows chunksBoth... */
		/*													*/
		/* Otherwise, if showing all markers, always merge */
		/* If conflict -- skip or merge (if forced) */
		/* If we have no unique changes, accept theirs */
		/* If they have no unique changes, take ours */
		/* If we both have changes -- merge or skip (if safe). */
		
		if (mergeAuto != null) {
			if (mergeAuto.equalsIgnoreCase(SAFE_MERGE)) {
				safeMerge = true;
			} else if (mergeAuto.equalsIgnoreCase(FORCE_MERGE)) {
				forceMerge = true;
			} else if (mergeAuto.equalsIgnoreCase(AUTO_MERGE)) {
				autoMerge = true;
			} 
		}
		
		if (mergeState.isTwoWayMerge()) {
			if (conflictChunks > 0) {
				return ResolveChoice.SKIP;
			}
			
			if (yourChunks == 0) {
				return ResolveChoice.THEIRS;
			}
			
			return ResolveChoice.YOURS;
		} else {
			if (mergeAuto != null) {
				if (mergeAuto.equalsIgnoreCase(SAFE_MERGE)) {
					safeMerge = true;
				} else if (mergeAuto.equalsIgnoreCase(FORCE_MERGE)) {
					forceMerge = true;
				} else if (mergeAuto.equalsIgnoreCase(AUTO_MERGE)) {
					autoMerge = true;
				} 
			}
			
			if (conflictChunks > 0) {
				if (forceMerge) {
					return ResolveChoice.EDIT;
				} else {
					return ResolveChoice.SKIP;
				}
			} else if (theirChunks == 0) {
				return ResolveChoice.YOURS;
			} else if (yourChunks == 0) {
				return ResolveChoice.THEIRS;
			} else  {
				// No conflict, but both sides have chunks; action here depends
				// on whether we're doing a safe, forced, or auto merge:
				
				if (safeMerge) {
					return ResolveChoice.SKIP;
				} else if (forceMerge || autoMerge) {
					return ResolveChoice.MERGED;
				}
			}
			
			return ResolveChoice.SKIP;	// Always the safe option at this point...
		}
	}
}
