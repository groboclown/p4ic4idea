/**
 * 
 */
package com.perforce.p4java.server;

import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.callback.IStreamingCallback;

/**
 *
 */
public interface IStreamingServer extends IOptionsServer {

	void execStreamingMapCommand(String cmdName, String[] cmdArgs, Map<String, Object> inMap,
											IStreamingCallback callback, int key) throws P4JavaException;
}
