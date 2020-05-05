/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherMode;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherResult;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcOutputStream;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.callback.IProgressCallback;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

// p4ic4idea: use serverr messages
import com.perforce.p4java.server.IServerMessage;

/**
 * Dispatch incoming client functions from the server. This may
 * involve a great deal of work under the covers, including some
 * extended series of calls back to the server and / or complex
 * flow control management, or it may involve little more than
 * returning what's already been seen.
 * 
 *
 */

public class ClientFunctionDispatcher {
	
public static final String TRACE_PREFIX = "ClientFunctionDispatcher";
	
	@SuppressWarnings("unused")
	private RpcPacketDispatcher mainDispatcher = null;
	
	private ClientUserInteraction userInteractor = null;
    private ClientSystemFileMatchCommands fileMatchCommands = null;
	private ClientSystemFileCommands fileCommands = null;
	private ClientSendFile fileSender = null;
	private ClientMerge clientMerger = null;
	private ClientProgressReport progressReport = null;
	private Properties props = null;
	protected RpcServer server = null;
	
	public ClientFunctionDispatcher(RpcPacketDispatcher mainDispatcher,
											Properties props, RpcServer server) {
		if (mainDispatcher == null) {
			throw new NullPointerError(
				"Null main dispatcher passed to ClientFunctionDispatcher constructor");
		}
		
		this.props = props;
		this.server = server;
		
		this.mainDispatcher = mainDispatcher;
		this.userInteractor = new ClientUserInteraction(this.props, server);
        this.fileMatchCommands  = new ClientSystemFileMatchCommands(this.props, server);
        this.fileCommands = new ClientSystemFileCommands(this.props, server, fileMatchCommands);
		this.fileSender = new ClientSendFile(this.props);
		this.clientMerger = new ClientMerge(this.props);
		this.progressReport = new ClientProgressReport(server);
	}
	
	public RpcPacketDispatcherResult dispatch(RpcPacketDispatcherMode dispatchMode,
			RpcFunctionSpec funcSpec, CommandEnv cmdEnv,
						Map<String, Object> resultsMap) throws ConnectionException {
		if (funcSpec == null) {
			throw new NullPointerError(
				"Null function spec passed to ClientFunctionDispatcher.dispatch()");
		}
		
		if (cmdEnv == null) {
			throw new NullPointerError(
			"Null command environment passed to ClientFunctionDispatcher.dispatch()");
		}
		
		RpcPacketDispatcherResult result = RpcPacketDispatcherResult.NONE;
		RpcConnection rpcConnection = cmdEnv.getRpcConnection();
				
		int cmdCallBackKey = cmdEnv.getCmdCallBackKey();
		IProgressCallback progressCallback = cmdEnv.getProgressCallback();
		
		boolean keepGoing = !cmdEnv.isUserCanceled();
		
		if ((progressCallback != null) && keepGoing) {
			keepGoing = progressReport.report(progressCallback, cmdCallBackKey, funcSpec, cmdEnv, resultsMap);
		}
		
		if (!keepGoing) {
			// Setting userCanceled as true may or may not cause issues elsewhere;
			// we at least try to clean up semi-properly...
			
			cmdEnv.setUserCanceled(true);
		}
			
		switch (funcSpec) {
		
			case CLIENT_MESSAGE:
				
				// Quiet mode - suppress all info level messages
                cmdEnv.clearLastResultMap();
				if (cmdEnv.getProtocolSpecs().isQuietMode()) {
					result = RpcPacketDispatcherResult.CONTINUE;
					break;
				}

			case CLIENT_FSTATINFO:
            case CLIENT_FSTATPARTIAL:
                // 2016/10/05 npoole

				// We have to special-case commands that return file contents in fstatInfo or message
				// packet data fields in order to do proper contents processing for charset
				// translation; so far only annotate and logtail do this, but this may change.
				// We also need to special-case the diff2 command to insert any diff command headers
				// back into the output stream, if it exists, and if the message is an info
				// message rather than an error message. Ditto (in similar ways) for describe and
				// describe's "affected files" list. None of this is quite as straightforward
				// as it probably sounds...
				// We need to insert the print command headers (name, rev, etc.) to the output stream
				// before writing the file content.
				
				if (cmdEnv.getCmdSpec().getCmdName().equalsIgnoreCase(CmdSpec.ANNOTATE.toString())
						|| cmdEnv.getCmdSpec().getCmdName().equalsIgnoreCase(CmdSpec.LOGTAIL.toString())) {
					resultsMap.remove("func");
					Map<String, Object> fileDataMap = this.fileCommands.convertFileDataMap(resultsMap,
											cmdEnv.getRpcConnection().getClientCharset(),
											cmdEnv.getRpcConnection().isUnicodeServer());
					if (funcSpec == RpcFunctionSpec.CLIENT_FSTATPARTIAL) {
                        cmdEnv.handlePartialResult(fileDataMap);
                    } else {
                        cmdEnv.handleResult(fileDataMap);
                    }
				} else if (cmdEnv.getCmdSpec().getCmdName().equalsIgnoreCase(CmdSpec.DIFF2.toString())
							|| cmdEnv.getCmdSpec().getCmdName().equalsIgnoreCase(CmdSpec.DESCRIBE.toString())
							|| cmdEnv.getCmdSpec().getCmdName().equalsIgnoreCase(CmdSpec.PRINT.toString())) {
					// p4ic4idea: use IServerMessage
					IServerMessage infoMsg = server.getErrorOrInfoStr(resultsMap);
					if (infoMsg != null) {
						RpcOutputStream outStream = this.fileCommands.getTempOutputStream(cmdEnv);
						if (outStream != null) {
							String charsetName = (rpcConnection.getClientCharset() == null ?
									CharsetDefs.DEFAULT_NAME : rpcConnection.getClientCharset().name());
							try {
								// p4ic4idea: make infoMsg a String to allow for the correct output.
								String infoStr = infoMsg.getErrorOrInfoStr() + CommandEnv.LINE_SEPARATOR;
								if (cmdEnv.getCmdSpec().getCmdName().equalsIgnoreCase(CmdSpec.DESCRIBE.toString())) {
									outStream.write("... ".getBytes(charsetName));
								}
								outStream.write(infoStr.getBytes(charsetName));
							} catch (IOException ioexc) {
								Log.warn("Unexpected exception in client function dispatch: "
										+ ioexc.getLocalizedMessage());
							}
						}
					}
					resultsMap.remove("func");
					if (funcSpec == RpcFunctionSpec.CLIENT_FSTATPARTIAL) {
                        cmdEnv.handlePartialResult(resultsMap);
					} else {
					    cmdEnv.handleResult(resultsMap);
					}
				} else {
					resultsMap.remove("func");
					if (funcSpec == RpcFunctionSpec.CLIENT_FSTATPARTIAL) {
                        cmdEnv.handlePartialResult(resultsMap);
                    } else {
                        cmdEnv.handleResult(resultsMap);
                    }
				}
				result = RpcPacketDispatcherResult.CONTINUE;
				break;
				
			case CLIENT_PROMPT:

			    cmdEnv.clearLastResultMap();
				result = this.userInteractor.clientPrompt(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_SETPASSWORD:
				
				result = this.userInteractor.clientSetPassword(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_CRYPTO:
				
				result = this.userInteractor.clientCrypto(rpcConnection, cmdEnv, resultsMap);	
				break;
				
			case CLIENT_CHMODFILE:
				
				result = this.fileCommands.chmodFile(rpcConnection, cmdEnv, resultsMap);
				break;

			case CLIENT_OPENFILE:
            case CLIENT_OPENDIFF:
            case CLIENT_OPENMATCH:

				result = this.fileCommands.openFile(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_CHECKFILE:
				
				result = this.fileCommands.checkFile(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_RECONCILEEDIT:
				// 2018/12/10 npoole
				result = this.fileMatchCommands.reconcileEdit(rpcConnection, cmdEnv, resultsMap);
				break;

			case CLIENT_RECONCILEADD:
				// 2018/12/10 npoole
				result = this.fileMatchCommands.reconcileAdd(rpcConnection, cmdEnv, resultsMap);
				break;

			case CLIENT_RECONCILEFLUSH:
				// 2018/12/10 npoole
				result = this.fileMatchCommands.reconcileFlush(rpcConnection, cmdEnv, resultsMap);
				break;

			case CLIENT_WRITEFILE:
            case CLIENT_WRITEDIFF:
            case CLIENT_WRITEMATCH:

				result = this.fileCommands.writeFile(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_CLOSEFILE:
            case CLIENT_CLOSEDIFF:
            case CLIENT_CLOSEMATCH:

				result = this.fileCommands.closeFile(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_ACK:
                // 2016/10/05 npoole
				result = this.userInteractor.clientAck(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_INPUTDATA:

				result = this.userInteractor.clientInputData(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_SENDFILE:

				result = this.fileSender.sendFile(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_DELETEFILE:

				result = this.fileCommands.deleteFile(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_OUTPUTBINARY:
				
				result = this.fileCommands.writeBinary(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_OUTPUTERROR:

                cmdEnv.clearLastResultMap();
				resultsMap.remove("func");
				// p4ic4idea: FIXME replace with using correct charset
				String msg = new String((byte[])resultsMap.remove("data"));
				int code = 1 | // subcode = 1
						(14 << 10) |	// subsystem = ES_P4QT
						(0 << 16) |	// generic = EV_NONE
						(0 << 24) |	// arg count = 0
						(4 << 28);	// severity = E_FATAL
				resultsMap.put(RpcMessage.CODE + 0, String.valueOf(code));
				resultsMap.put(RpcMessage.FMT + 0, msg);
				cmdEnv.handleResult(resultsMap);
				result = RpcPacketDispatcherResult.STOP_NORMAL;
				break;
				
			case CLIENT_OUTPUTTEXT:

                cmdEnv.clearLastResultMap();
				result = this.fileCommands.writeText(rpcConnection, cmdEnv, resultsMap);
				// Special handling of tracking data output.
				// There is no distinction between successive client-OutputText.
				// Thus, we capture all "data" fields
				if (cmdEnv.getProtocolSpecs().isEnableTracking()) {
					cmdEnv.handleResult(this.fileCommands.convertFileDataMap(resultsMap,
							cmdEnv.getRpcConnection().getClientCharset(),
							cmdEnv.getRpcConnection().isUnicodeServer()));						
				}
				break;
				
			case CLIENT_OUTPUTDATA:
			case CLIENT_OUTPUTINFO:

                cmdEnv.clearLastResultMap();
				resultsMap = this.fileCommands.convertFileDataMap(resultsMap,
						cmdEnv.getRpcConnection().getClientCharset(),
						cmdEnv.getRpcConnection().isUnicodeServer());						

				RpcOutputStream dataOutStream = this.fileCommands.getTempOutputStream(cmdEnv);
				if (dataOutStream != null) {
					try {
						String dataString = (String) resultsMap.get(RpcFunctionMapKey.DATA);
						if (dataString != null) {
							// p4ic4idea: FIXME use correct charset
							dataOutStream.write(dataString.getBytes());
						}
					} catch (IOException e) {
						Log.exception(e);
					}
				}
				cmdEnv.handleResult(resultsMap);
				result = RpcPacketDispatcherResult.CONTINUE;
				break;

			case CLIENT_PROGRESS:

                cmdEnv.clearLastResultMap();
				RpcOutputStream progressOutStream = this.fileCommands.getTempOutputStream(cmdEnv);
				if (progressOutStream != null) {
					// Compose the progress indicator message
					StringBuilder sb = new StringBuilder();
					if (resultsMap.get("desc") != null) {
						sb.append(resultsMap.get("desc"));
					}
					if (resultsMap.get("update") != null) {
						sb.append(" ").append(resultsMap.get("update"));
					}
					if (resultsMap.get("done") != null) {
						sb.append(" ").append("finishing");
					}
					
					if (sb.length() > 0) {
						try {
							sb.append(CommandEnv.LINE_SEPARATOR);
							// p4ic4idea: FIXME use correct charset
							progressOutStream.write(sb.toString().getBytes());
						} catch (IOException ioexc) {
							// p4ic4idea: log exception stack trace, too
							Log.warn("Unexpected exception in client function dispatch: "
									+ ioexc.getLocalizedMessage(), ioexc);
						}
					}
				}
				cmdEnv.handleResult(resultsMap);
				result = RpcPacketDispatcherResult.CONTINUE;
				break;

			case CLIENT_MOVEFILE:
				
				result = this.fileCommands.moveFile(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_OPENMERGE3:
				
				result = this.clientMerger.clientOpenMerge3(rpcConnection, cmdEnv, resultsMap, false);
				break;
				
			case CLIENT_OPENMERGE2:
				// Currently reuse clientOpenMerge3 for both two- and three-way merges.
				// This may change with experience...
				
				result = this.clientMerger.clientOpenMerge3(rpcConnection, cmdEnv, resultsMap, true);
				break;
				
			case CLIENT_WRITEMERGE:
				
				result = this.clientMerger.clientWriteMerge(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_CLOSEMERGE:
				
				result = this.clientMerger.clientCloseMerge(rpcConnection, cmdEnv, resultsMap);
				break;
				
			case CLIENT_SSO:
				
				result = this.userInteractor.clientSingleSignon(rpcConnection, cmdEnv, resultsMap);
				break;

            case CLIENT_RECEIVEFILES:
                // 2016/10/05 npoole
                result = this.userInteractor.clientReceiveFiles(rpcConnection, cmdEnv, resultsMap);
                break;

            case CLIENT_ACKMATCH:
                // 2016/10/05 npoole
                result = this.fileMatchCommands.ackMatch(rpcConnection, cmdEnv, resultsMap);
                break;

            case CLIENT_CONVERTFILE:
                // 2016/10/05 npoole
                result = this.fileCommands.convertFile(rpcConnection, cmdEnv, resultsMap);
                break;

            case CLIENT_ACTIONRESOLVE:
                // 2016/10/05 npoole
                result = this.userInteractor.clientActionResolve(rpcConnection, cmdEnv, resultsMap);
                break;

            case CLIENT_EDITDATA:
                // 2016/10/05 npoole
                result = this.userInteractor.clientEditData(rpcConnection, cmdEnv, resultsMap);
                break;

            case CLIENT_EXACTMATCH:
                // 2018/12/10 npoole
                result = this.fileMatchCommands.exactMatch(rpcConnection, cmdEnv, resultsMap);
                break;

            case CLIENT_ERRORPAUSE:
                // 2016/10/05 npoole
                result = this.userInteractor.clientErrorPause(rpcConnection, cmdEnv, resultsMap);
                break;

            case CLIENT_HANDLEERROR:
                // 2016/10/05 npoole
                result = this.userInteractor.clientHandleError(rpcConnection, cmdEnv, resultsMap);
                break;

            case CLIENT_PING:
                // 2016/10/05 npoole
                result = this.userInteractor.clientPing(rpcConnection, cmdEnv, resultsMap);
                break;


			case CLIENT_OPENURL:
				result = this.userInteractor.clientOpenURL(rpcConnection, cmdEnv, resultsMap);
				break;

			case CLIENT_SCANDIR:
				// TODO PTA - fall through to default


			default:
				Log.error("Unimplemented function spec in ClientFunctionDispatcher.dispatch(): '"
						+ funcSpec.toString() + "'");
				throw new P4JavaError(
					"Unimplemented function spec in ClientFunctionDispatcher.dispatch(): '"
					+ funcSpec.toString() + "'");
		}
		
		return result;
	}
	
}
