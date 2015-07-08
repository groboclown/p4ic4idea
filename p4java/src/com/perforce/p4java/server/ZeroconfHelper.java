/**
 * 
 */
package com.perforce.p4java.server;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.ConfigException;

/**
 * Class designed to help the server factory with zeroconf-based duties.
 * Not intended for direct use by end-users; uses the JmDNS zeroconf package
 * if it's available through the class loader. See the comments attached to
 * ServerFactory.getZeroconfServers for a fuller explanation.<p>
 * 
 * @deprecated  As of release 2013.1, ZeroConf is no longer supported by the
 * 				Perforce server 2013.1.
 */
@Deprecated
public class ZeroconfHelper {
	
	/**
	 * The name of the class we first try to find for zeroconf services.
	 */
	public static final String ZEROCONF_CLASS_NAME = "javax.jmdns.JmDNS";
	
	/**
	 * The zeroconf type string normally used to register
	 * Perforce servers.
	 */
	public static String P4D_ZEROCONF_TYPE = "_p4._tcp.local.";
	
	/**
	 * The JmDNS zeroconf object. Don't touch this unless you absolutely
	 * know what you're doing.
	 */
	private Object jmdns = null;
	
	/**
	 * Set true if we've already logged a fatal zeroconf error. Used
	 * to stop a flood of annoying messages in the log.
	 */
	private boolean loggedZeroConfError = false;
	
	/**
	 * We don't want this usable outside the main P4Java server package.
	 */
	protected ZeroconfHelper() {
	}
	
	/**
	 * Returns true if this instance of P4Java has a suitable zeroconf implementation
	 * available to it. Should probably be used at least once before calling the
	 * getZeroConfServers method to avoid unnecessary overhead.
	 * 
	 * @return true iff zeroconf services are available to the server factory.
	 */
	
	protected boolean isZeroConfAvailable() {
		try {
			/*
			 * We try to load a suitable JmDNS class and take a look to see
			 * if it's usable and has the correct methods.
			 */
			if (jmdns == null) {
				Class<?> jmdnsClass = Class.forName(ZEROCONF_CLASS_NAME);
				if (jmdnsClass != null) {
					Method createMethod = jmdnsClass.getMethod("create", (Class<?>[]) null);
					
					if (createMethod != null) {
						jmdns = createMethod.invoke(null, (Object[]) null);
					}
				}
			}
			return true;
		} catch (ClassNotFoundException cnfe) {
			return false;
		} catch (Throwable thr) {
			Log.warn("Unexpected exception in ServerFactory.zeroConfAvailable: " + thr.getLocalizedMessage());
			Log.exception(thr);
			loggedZeroConfError = true;
		}
		return false;
	}
	
	protected List<ZeroconfServerInfo> getZeroconfServers() throws ConfigException {
		if (!isZeroConfAvailable()) {
			throw new ConfigException(
					"Zeroconf services are not configured for this instance of P4Java");
		}
		
		/*
		 * Most of the complexity here is a result of needing to *not* package
		 * up JmDNS with P4Java -- we have to find it at runtime using the standard
		 * class loader (or whatever we have) and invoke and do everything through
		 * reflection. This is due to Perforce licensing restrictions.
		 * 
		 * Note that I assume the reader knows about reflection and JmDNS in what
		 * follows, as comments are a little sparse....
		 */
		
		List<ZeroconfServerInfo> serverList = new ArrayList<ZeroconfServerInfo>();
		
		try {
			if (jmdns != null) {
				Method listMethod = jmdns.getClass().getDeclaredMethod("list", String.class);
				if (listMethod != null) {
					Object[] services = (Object[]) listMethod.invoke(jmdns, P4D_ZEROCONF_TYPE);
					if (services != null) {
						Method nameMethod = null;
						Method propMethod = null;
						Method addrMethod = null;
						Method hostNameMethod = null;
						Method portMethod = null;
						
						for (Object info: services) {
							if (info != null) {
								if (nameMethod == null) {
									nameMethod = info.getClass().getDeclaredMethod(
																	"getName", (Class<?>[]) null);
								}
								if (hostNameMethod == null) {
									hostNameMethod = info.getClass().getDeclaredMethod(
																	"getServer", (Class<?>[]) null);
								}
								if (propMethod == null) {
									propMethod = info.getClass().getDeclaredMethod(
																"getPropertyString", String.class);
								}
								if (addrMethod == null) {
									addrMethod = info.getClass().getDeclaredMethod(
																"getHostAddress", (Class<?>[]) null);
								}
								if (portMethod == null) {
									portMethod = info.getClass().getDeclaredMethod(
											"getPort", (Class<?>[]) null);
								}

								serverList.add(new ZeroconfServerInfo(
											(String) nameMethod.invoke(info, (Object[]) null),
											P4D_ZEROCONF_TYPE,
											(String) propMethod.invoke(info, "description"),
											(String) propMethod.invoke(info, "version"),
											(String) addrMethod.invoke(info, (Object[]) null),
											(String) hostNameMethod.invoke(info, (Object[]) null),
											(Integer) portMethod.invoke(info, (Object[]) null)
									));
							}
						}
					}
				}
			}
		} catch (Throwable thr) {
			if (!loggedZeroConfError) {
				Log.warn("Unexpected exception in ZeroconfHelper.getZeroconfServers: "
						+ thr);
				Log.exception(thr);
				loggedZeroConfError = true;
			}
			throw new ConfigException("Unexpected exception in ZeroconfHelper.getZeroconfServers: "
					+ thr);
		}
		
		return serverList;
	}
}
