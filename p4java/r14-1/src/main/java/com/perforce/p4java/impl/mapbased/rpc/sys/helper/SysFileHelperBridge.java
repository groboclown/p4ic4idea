/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.server.ServerFactory;

/**
 * Intended as a small helper class to bridge any system / JDK impedance
 * mismatches. Not much here is explained...
 */
public class SysFileHelperBridge {
	private static ISystemFileCommandsHelper defaultHelper = new RpcSystemFileCommandsHelper();

	public static ISystemFileCommandsHelper getSysFileCommands() {
		ISystemFileCommandsHelper helper = ServerFactory.getRpcFileSystemHelper();

		if (helper == null) {
			helper = defaultHelper;
		}
		
		return helper;
	}
}
