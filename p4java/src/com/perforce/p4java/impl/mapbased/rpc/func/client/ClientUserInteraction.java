/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5Digester;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MapUnmapper;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherResult;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.callback.ISSOCallback;
import com.perforce.p4java.server.callback.ISSOCallback.Status;

/**
 * Class for Perforce client end-user interaction commands like
 * prompting or password-setting ("end-user interaction" is being
 * rather broadly-defined here...).<p>
 * 
 * Note that in general we don't actually do any end-user interaction
 * in P4Java -- it's all done before a call by the app that P4Java's
 * been embedded in.
 */

public class ClientUserInteraction {
	
	public static final String TRACE_PREFIX = "ClientUserInteraction";
	
	protected static final int RESP_LENGTH = 32;	// Response to server needs to be exactly this long,
													// in bytes
	protected static final int TRUNCATE_LENGTH = 16;	// Server-commanded candidate password truncation
														// length in bytes
		
	@SuppressWarnings("unused")  // used for debugging
	private Properties props = null;
	private RpcServer server = null;
	
	protected ClientUserInteraction(Properties props, RpcServer server) {
		this.props = props;
		this.server = server;
	}
	
	/**
	 * Prompt the end-user (i.e. the upper-levels of P4Java...) in response to a
	 * server request. The various parameters in resultsMap determine things like
	 * whether the prompt uses echoing, what digest to use (if any), what prompt
	 * string to use, etc., many of which aren't relevant in our context.<p>
	 * 
	 * In the most common usage -- password extraction from the user -- we
	 * follow a fairly simple scheme where we first hash the password with
	 * an MD5 digest, then pass the results of that operation through a new
	 * MD5 cycle and hash it against the "digest" string passed in from
	 * the server. Everything else that's sent to us from the server is
	 * simply echoed for the server's own purposes (i.e. I still don't know
	 * what some of this stuff does...).<p>
	 * 
	 * Note that we have to do what the C++ API does in the same circumstances;
	 * this means converting the hash hex string results into upper-case, etc.,
	 * and several other mild quirks whose use or motivation aren't always
	 * obvious.<p>
	 * 
	 * Note also that we're not currently implementing the full panoply of
	 * possible processing here, just the subset that's useful to us for P4WSAD
	 * and that presumes a 2003.2 or later server in not-too-strict mode.
	 */
	
	protected RpcPacketDispatcherResult clientPrompt(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in clientPrompt().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in clientPrompt().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in clientPrompt().");
		}
		
		// Much of what follows is just a fairly straight transliteration from
		// the original C++ (as best as I've been able to follow it). Not all
		// of it is implemented yet (either correctly or at all)...
		
		String data2 = null;

		String digest = null;
		String func2 = null;
		String state = null;
		String userName = null;
		String host = null;
		String confirm = null;
		String mangle = null;
		boolean truncate = false;
		boolean noecho = false;
		
		MD5Digester digester = null;
		
		String passwd = null;
		String resp = null;
				
		try {
			data2 = (String) resultsMap.get(RpcFunctionMapKey.DATA2);

			digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST);
			truncate = resultsMap.containsKey(RpcFunctionMapKey.TRUNCATE);
			func2 = (String) resultsMap.get(RpcFunctionMapKey.FUNC2);
			userName = (String) resultsMap.get(RpcFunctionMapKey.USER);
			host = (String) resultsMap.get(RpcFunctionMapKey.HOST);
			state =  (String) resultsMap.get(RpcFunctionMapKey.STATE);
			confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
			noecho = resultsMap.containsKey(RpcFunctionMapKey.NOECHO);
			mangle = (String) resultsMap.get(RpcFunctionMapKey.MANGLE);
			
			if (confirm == null) {
				throw new ProtocolError("No confirm server function in clientPrompt.");
			}
			RpcFunctionSpec funcSpec = RpcFunctionSpec.decode(confirm);
			if (funcSpec == RpcFunctionSpec.NONE) {
				throw new ProtocolError("Unable to decode confirm server function '"
						+ confirm + "' in clientPrompt.");
			}
			
			// Due to a quirk in the sibling p4 cmd server implementation,
			// we get the password in the inMap here rather than in the cmdArgs.
			// C'est la vie, I guess.
			
			Map<String, Object> inMap = cmdEnv.getCmdSpec().getInMap();
			if (inMap == null) {
				throw new NullPointerError("No input map passed to client prompt.");
			}

			// Handle confirmation to server functions dm-Login and dm-Password
			switch (funcSpec) {
			case SERVER_DM_LOGIN: // login password confirmation
				if (inMap.get(RpcFunctionMapKey.PASSWORD) != null) {
					passwd = ((String)inMap.get(RpcFunctionMapKey.PASSWORD)).replace(MapKeys.LF, MapKeys.EMPTY).replace(MapKeys.CR, MapKeys.EMPTY);
				}
				break;
			case SERVER_DM_PASSWD: // change password confirmation
				if (inMap.get(RpcFunctionMapKey.OLD_PASSWORD) != null) {
					passwd = ((String)inMap.remove(RpcFunctionMapKey.OLD_PASSWORD)).replace(MapKeys.LF, MapKeys.EMPTY).replace(MapKeys.CR, MapKeys.EMPTY);
				}
				if (passwd == null) {
					if (inMap.get(RpcFunctionMapKey.NEW_PASSWORD) != null) {
						passwd = ((String)inMap.remove(RpcFunctionMapKey.NEW_PASSWORD)).replace(MapKeys.LF, MapKeys.EMPTY).replace(MapKeys.CR, MapKeys.EMPTY);
					}
				}
				if (passwd == null) {
					if (inMap.get(RpcFunctionMapKey.NEW_PASSWORD2) != null) {
						passwd = ((String)inMap.remove(RpcFunctionMapKey.NEW_PASSWORD2)).replace(MapKeys.LF, MapKeys.EMPTY).replace(MapKeys.CR, MapKeys.EMPTY);
					}
				}
				break;
			default:
				throw new UnimplementedError("Unimplemented confirmation to server function '"
						+ funcSpec.getEncoding() + "' in clientPrompt.");
			}

			if (passwd == null) {
				throw new NullPointerError("No password passed to clientPrompt.");
			}
			
			if (truncate && (passwd.length() > TRUNCATE_LENGTH)) {
				passwd = passwd.substring(0, TRUNCATE_LENGTH);
			}
			
			if (digest != null) {
				digester = new MD5Digester();
				
				digester.update(passwd.getBytes(CharsetDefs.UTF8.name()));
				resp = digester.digestAs32ByteHex();
				// Salt this away for the incoming clientSetPassword call later
				// in this interaction.
				// If this is the digest of the old password it will be used as
				// salt for the key in mangling the new password
				this.server.setSecretKey(userName, resp);
				digester.reset();
				digester.update(resp.getBytes(CharsetDefs.UTF8.name()));
				digester.update(digest.getBytes(CharsetDefs.UTF8.name()));
				
				resp = digester.digestAs32ByteHex();
			}
			
			// Now construct a response back to the server:
			
			Map<String, Object> respMap = new HashMap<String, Object>();
			if (digest != null) {
				respMap.put(RpcFunctionMapKey.DIGEST, digest);
			}
			
			if( mangle != null) {
				respMap.put(RpcFunctionMapKey.MANGLE, mangle);
				
				// Hash mangle and username
				MD5Digester mangleDigester = new MD5Digester();
				mangleDigester.update(mangle);
				mangleDigester.update(userName);
				// Add salt (from the old password digest) to the key.
				if (this.server.getSecretKey(userName) != null) {
					mangleDigester.update(this.server.getSecretKey(userName));
				}
				Mangle jMangle = new Mangle();
				String toMangle = resp;
				if( toMangle == null) {
					toMangle = passwd;
				}
				String digestedMangle = mangleDigester.digestAs32ByteHex();
				resp = jMangle.encrypt(toMangle, digestedMangle);
			}
			respMap.put(RpcFunctionMapKey.DATA, resp);
			if (data2 != null) {
				respMap.put(RpcFunctionMapKey.DATA2, resp);
			}
			if (truncate) {
				respMap.put(RpcFunctionMapKey.TRUNCATE, MapKeys.EMPTY);
			}
			respMap.put(RpcFunctionMapKey.FUNC2, func2);
			respMap.put(RpcFunctionMapKey.STATE, state);
			if (noecho) {
				respMap.put(RpcFunctionMapKey.NOECHO, MapKeys.EMPTY);
			}
			respMap.put(RpcFunctionMapKey.USER, userName);
			if (host != null) {
				respMap.put(RpcFunctionMapKey.HOST, host);
			}
			respMap.put(RpcFunctionMapKey.CONFIRM, confirm);
			
			RpcPacket respPacket = RpcPacket.constructRpcPacket(
								confirm,
								respMap,
								null);
			
			rpcConnection.putRpcPacket(respPacket);
			
		} catch (Exception exc) {
			Log.exception(exc);
			throw new P4JavaError(
					"Unexpected exception in ClientUserInteraction.clientPrompt:"
					+ exc.getLocalizedMessage(),
					exc);
		}
		
		return RpcPacketDispatcherResult.CONTINUE;
	}
	
	/**
	 * Set the client-side password in response to a Perforce server command
	 * telling us to do just that, usually as a result of an earlier
	 * successful login attempt in the same session.<p>
	 * 
	 * In this context setting the password really just means performing a few
	 * sanity and consistency checks, then returning a suitable ticket for use
	 * with the -P option in future commands. This can be an arbitrarily
	 * complex process...<p>
	 */
	
	protected RpcPacketDispatcherResult clientSetPassword(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in clientSetPassword().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in clientSetPassword().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in clientSetPassword().");
		}
		
		if (this.server != null) {
			String data2 = (String) resultsMap.get(RpcFunctionMapKey.DATA2);
			String userName = (String) resultsMap.get(RpcFunctionMapKey.USER);
			String serverId = (String) resultsMap.get(RpcFunctionMapKey.SERVERADDRESS);
			String ticket = null;
			
			if (serverId != null) {
				this.server.setServerId(serverId);
			}
			if (userName == null) {
				userName = this.server.getUserName();
			}
			String serverAddress = (serverId != null ? serverId : this.server.getServerAddress());
			String key = serverAddress + "=" + userName;
			
			if (CmdSpec.LOGOUT.toString().equals(data2)) {
				cmdEnv.getCmdSpec().setCmdTicket(null);
				// Decrement the user's login counter
				// Remove the user's ticket from cache when the count is zero
				if (!(this.server.getAuthCounter().decrementAndGet(key) > 0)) {
					this.server.setAuthTicket(userName, null);
				}
				try {
					this.server.saveTicket(userName, null);
				} catch (ConfigException e) {
					throw new ConnectionException(e);
				}
			} else if (CmdSpec.LOGIN.toString().equals(data2)) {
				byte[] data = (byte[]) resultsMap.get(RpcFunctionMapKey.DATA);
				
				String digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST); // aka "token" in C++ API
				
				if (digest != null) {
					if (digest.length() != 32) {
						throw new P4JavaError("bad digest size");
					}
					
					String secretKey = null;
					secretKey = this.server.getSecretKey(userName); // from the earlier clientGetPrompt() call...
					if (secretKey == null) {
						secretKey = this.server.getAuthTicket(userName);
					}
					if (secretKey == null) {
						secretKey = this.server.getAuthTicket();
					}

					ticket = new String(data);
					Mangle mangler = new Mangle();
					String token2 = mangler.mangle(digest, secretKey, true);
					ticket = mangler.xor32(ticket, token2);
				}
				
				if (data != null) {
					if (ticket == null) {
						ticket = new String(data);
					}
					this.server.setAuthTicket(userName, ticket);
					if(!cmdEnv.isDontWriteTicket()) { // skip if "login -p"
						try {
							this.server.saveTicket(userName, ticket);
						} catch (ConfigException e) {
							throw new ConnectionException(e);
						}
					}

					// Increment the user's login counter
					this.server.getAuthCounter().incrementAndGet(key);
				}
			}

			// Clear the secret key
			// Not useful any more
			this.server.setSecretKey(userName, null);
		}
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Implements the client-side of the Perforce single sign on (SSO) protocol.
	 * Basically defers to the registered SSO callback (if it exists) and simply
	 * responds back to the server appropriately.
	 */
	
	protected RpcPacketDispatcherResult clientSingleSignon(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in clientSingleSignon().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in clientSingleSignon().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in clientSingleSignon().");
		}
		
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		ISSOCallback.Status status = null;
		StringBuffer credBuf = new StringBuffer();
		
		if (this.server != null) {
			ISSOCallback ssoCallback = this.server.getSSOCallback();
			if (ssoCallback != null) {
				status = ssoCallback.getSSOCredentials(credBuf,
								this.server.getSSOKey(), this.server.getUserName());
			}
		} else {
			// How did this happen?!
			Log.error(
					"null server object in ClientUserInteraction.clientSingleSignon method");
		}
		
		if( status == null) {
			status = Status.UNSET;
		}
		
		if (confirm != null) {
			Map<String,Object> respMap = new HashMap<String, Object>();
			
			for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
				if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)) {
					respMap.put(entry.getKey(), entry.getValue());
				}
			}
			
			respMap.put(RpcFunctionMapKey.STATUS, status.toString()
					.toLowerCase());
			
			String sso = null;
			// If not unset then set the SSO key based on the buffered passed to
			// the sso callback
			if (status != Status.UNSET) {
				if ((credBuf != null)
						&& (credBuf.length() > ISSOCallback.MAX_CRED_LENGTH)) {
					sso = credBuf.substring(0, ISSOCallback.MAX_CRED_LENGTH);
				} else {
					// Note: credBuf could be null, which is OK in this context
					sso = credBuf.toString();
				}
			}
			respMap.put(RpcFunctionMapKey.SSO, sso);
			
			RpcPacket respPacket = RpcPacket.constructRpcPacket(
															confirm,
															respMap,
															null);
			
			rpcConnection.putRpcPacket(respPacket);
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Respond back to the server with what amounts to a yes / no response
	 * in the face of errors or success. Intended for interactive clients,
	 * I guess; we just decline automatically if anything went wrong,
	 * and say yes ("dm-OpenFile") if things went fine... in effect, we
	 * just do a pass-through. This may change as we gain experience.
	 */
	
	protected RpcPacketDispatcherResult clientAck(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in clientAck().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in clientAck().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in clientAck().");
		}
		
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		String decline = (String) resultsMap.get(RpcFunctionMapKey.DECLINE);
		String handle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		
		RpcHandler handler = cmdEnv.getHandler(handle);
		
		if (handler != null) {
			if (handler.isError()) {
				confirm = decline;
			}
		}
		
		if (confirm != null) {
			
			// Copy all incoming vars to outgoing vars; this is
			// a bit of a no-op here, of course, except we have to get rid of
			// the original function entry...; we do this in a copy in
			// case someone is using it elsewhere; this can be changed
			// later if needed (HR).
			
			Map<String,Object> respMap = new HashMap<String, Object>();
			
			for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
				if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)) {
					respMap.put(entry.getKey(), entry.getValue());
				}
			}
			
			RpcPacket respPacket = RpcPacket.constructRpcPacket(
															confirm,
															respMap,
															null);
			
			rpcConnection.putRpcPacket(respPacket);
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Process the client-Crypto command from the Perforce server. This is typically
	 * called in response to an earlier login using the ticket feature (which is how
	 * we normally do logins in P4Java).<p>
	 * 
	 * In the P4Java context, this really means first MD5-hashing the incoming token, then
	 * hashing the previously-returned ticket, then returning the results to the server for
	 * inspection.
	 */
	
	protected RpcPacketDispatcherResult clientCrypto(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in clientCrypto().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in clientCrypto().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in clientCrypto().");
		}
		
		String ticketStr = cmdEnv.getCmdSpec().getCmdTicket();
		MD5Digester digester = null;
		
		// We actually get token, serverAddress, confirm, truncate fields incoming,
		// but only use the token here.
		
		String token = null;	
		String resp = null;
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		Map<String, Object> respMap = null;
		
		try {
			// Use the auth ticket associated with specified server address
			String serverId = (String) resultsMap.get(RpcFunctionMapKey.SERVERADDRESS);
			if (serverId != null) {
				this.server.setServerId(serverId);
			}

			// Get the auth ticket from cache (if it exists)
			// Might be asking for another server address's auth ticket
			// (in the case of a replica sitting in front of the server) 
			if (this.server.getAuthTicket() != null) {
				ticketStr = this.server.getAuthTicket();
			}

			// Load the auth ticket from file/memory storage
			if (ticketStr == null) {
				ticketStr = this.server.loadTicket(serverId);
				// Cache the auth ticket if found
				if (ticketStr != null) {
					this.server.setAuthTicket(ticketStr);
				}
			}
			
			if( ticketStr == null) {
				ticketStr = MapKeys.EMPTY;		// which should fail on the server...
			}
			
			String daddr = rpcConnection.getServerIpPort();
			token = (String) resultsMap.get(RpcFunctionMapKey.TOKEN);

			digester = new MD5Digester();
			digester.reset();
			
			digester.update(token.getBytes(CharsetDefs.UTF8.name()));
			digester.update(ticketStr.getBytes());
			resp = digester.digestAs32ByteHex();

			respMap = new HashMap<String, Object>();
			
			// Add 'daddr' for Perforce server configurable 'net.mimcheck=5' (or >= 4)
			// See job081080
			if (daddr != null) {
				digester.reset();
				digester.update(resp.getBytes());
				digester.update(daddr.getBytes());
				resp = digester.digestAs32ByteHex();

				respMap.put(RpcFunctionMapKey.DADDR, daddr);
			}

			respMap.put(RpcFunctionMapKey.TOKEN, resp);
			
			RpcPacket respPacket = RpcPacket.constructRpcPacket(
					confirm,
					respMap,
					null);

			rpcConnection.putRpcPacket(respPacket);
			
		} catch (Exception exc) {
			Log.exception(exc);
			throw new P4JavaError(
					"Unexpected exception in ClientUserInteraction.clientCrypto:"
					+ exc.getLocalizedMessage(),
					exc);
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Get some requested data (typically something like a submit form) from
	 * somewhere (typically a map passed in from the upper levels of the API) and
	 * pass it back to the server, properly munged.
	 */
	
	protected RpcPacketDispatcherResult clientInputData(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in clientInputData().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in clientInputData().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in clientInputData().");
		}
		
		cmdEnv.newHandler();
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		
		Map<String, Object> inMap = cmdEnv.getCmdSpec().getInMap();
		String inString = cmdEnv.getCmdSpec().getInString();
		Map<String, Object> respMap = new HashMap<String, Object>();
		
		if (inString == null) {
			
			// We have to convert the inMap back to a suitable form as a single
			// data argument, which is irritating but doable...
			
			StringBuffer strBuf = new StringBuffer();
			byte[] bytes = null;
			String cmdName = cmdEnv.getCmdSpec().getCmdName();
			if (cmdName == null) {
				throw new NullPointerError("null command name");
			}
    		CmdSpec cmdSpec = CmdSpec.getValidP4JCmdSpec(cmdName);
    		if (cmdSpec != null) {
				switch (cmdSpec) {
				case JOB:
					MapUnmapper.unmapJobMap(inMap, strBuf);
					break;
				case CHANGE:
				case SUBMIT:
				case SHELVE:
					MapUnmapper.unmapChangelistMap(inMap, strBuf);
					break;
				case LABEL:
					MapUnmapper.unmapLabelMap(inMap, strBuf);
					break;
				case CLIENT:
					MapUnmapper.unmapClientMap(inMap, strBuf);
					break;
				case BRANCH:
					MapUnmapper.unmapBranchMap(inMap, strBuf);
					break;
				case USER:
					MapUnmapper.unmapUserMap(inMap, strBuf);
					break;
				case GROUP:
					MapUnmapper.unmapUserGroupMap(inMap, strBuf);
					break;
				case DEPOT:
					MapUnmapper.unmapDepotMap(inMap, strBuf);
					break;
				case PROTECT:
					MapUnmapper.unmapProtectionEntriesMap(inMap, strBuf);
					break;
				case STREAM:
					MapUnmapper.unmapStreamMap(inMap, strBuf);
					break;
				case ATTRIBUTE:
					// Have to treat this rather differently, as we're reading a byte stream in
					// and not a string. We may need to factor this out elsewhere in the long term -- HR.

					InputStream inStream = (InputStream) inMap.get(Server.ATTRIBUTE_STREAM_MAP_KEY);
					if (inStream == null) {
						throw new NullPointerError("null input stream in getStreamBytes.inMap");
					}
					bytes = getStreamBytes(cmdName, inStream);
					break;
				case TRIGGERS:
					MapUnmapper.unmapTriggerEntriesMap(inMap, strBuf);
					break;
				default:
					break;
				}
    		}
			
			if (bytes != null) {
				respMap.put(RpcFunctionMapKey.DATA, bytes);
			} else {
				respMap.put(RpcFunctionMapKey.DATA, strBuf);
			}
		} else {
			respMap.put(RpcFunctionMapKey.DATA, inString);
		}
		
		for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
			if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)) {
				respMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		RpcPacket respPacket = RpcPacket.constructRpcPacket(
									confirm,
									respMap,
									null);

		rpcConnection.putRpcPacket(respPacket);
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Get bytes from the stream passed in.
	 */
	protected byte[] getStreamBytes(String cmdName, InputStream inStream) {

		if (inStream != null) {
			try {
				byte[] bytes = new byte[8192];	// deliberately fairly small to start with -- most uses are
												// probably for small thumbnail files, etc.
				int bytesRead = 0;
				int bytePos = 0;
				
				while ((bytesRead = inStream.read(bytes, bytePos, bytes.length - bytePos)) > 0) {
					bytePos += bytesRead;
					if (bytePos >= bytes.length) {
						byte[] newBytes = new byte[bytes.length * 4];
						System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
						bytes = newBytes;
					}
				}
				byte[] retBytes = new byte[bytePos];
				System.arraycopy(bytes, 0, retBytes, 0, bytePos);
				return retBytes;
			} catch (Throwable thr) {
				Log.warn("problem reading input stream for user input for " + cmdName + " operation");
				Log.exception(thr);
			}
		}

		return null;
	}
}
