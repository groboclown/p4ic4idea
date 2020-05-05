/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */

package com.perforce.p4java.server;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.Metadata;
import com.perforce.p4java.common.base.OSUtils;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.NtsServerImpl;
import com.perforce.p4java.impl.mapbased.rpc.OneShotServerImpl;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.WindowsRpcSystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.server.IServerControl;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.impl.mapbased.server.ServerAddressBuilder;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.server.IServerAddress.Protocol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The main P4Java server factory class. This class is used to obtain an IServer interface
 * onto a particular Perforce server using a specific (or default) protocol. Usage
 * is described below with the getServer method.
 */

public class ServerFactory {
	
	public static final String TRACE_PREFIX = "ServerFactory";
	
	/**
	 * The pseudo-protocol used in URIs to signal that the factory should make its own
	 * decision about what protocol and server to use.
	 */
	public static final String DEFAULT_PROTOCOL_SPEC = IServerAddress.Protocol.P4JAVA.toString();
	
	/**
	 * Default protocol name, i.e. the name used when the protocol name is
	 * specified as "p4java" in a suitable URL. Currently it always maps to
	 * the native RPC implementation, but this is not guaranteed.
	 */
	
	public static final String DEFAULT_PROTOCOL_NAME = OneShotServerImpl.PROTOCOL_NAME;
	
	/**
	 * Default SSL protocol name, i.e. the name used when the protocol name is
	 * specified as "p4javassl" in a suitable URL. Currently it always maps to
	 * the native SSL RPC implementation, but this is not guaranteed.
	 */
	
	public static final String DEFAULT_SSL_PROTOCOL_NAME = OneShotServerImpl.SSL_PROTOCOL_NAME;

	public static final String ZEROCONF_CLASS_NAME = "javax.jmdns.JmDNS";
	
	/**
	 * The factory's protocol scheme / implementation map. Not intended
	 * for public consumption; also not really intended for large numbers of
	 * implementation classes.
	 */
	
	@SuppressWarnings("rawtypes")
	private static Map<Protocol, Class> implMap = new HashMap<Protocol, Class>();
	
	private static ISystemFileCommandsHelper rpcFileCommandsHelper = null;
	
	/**
	 * The JmDNS zeroconf helper.
	 */
	@SuppressWarnings("deprecation")
	private static ZeroconfHelper zcHelper = null;
	
	/**
	 * Initialize the factory. This includes setting the rpcFileCommandsHelper
	 * to a default JDK 6 implementation of the ISystemFileCommandsHelper interface;
	 * this can be replaced by an explicit call to setRpcFileSystemHelper later if
	 * this is inappropriate for a specific context.
	 */
	
	static {
		// Set up server protocol name to implementation class mapping:
		implMap.put(Protocol.P4JAVA, OneShotServerImpl.class);
		implMap.put(Protocol.P4JAVASSL, OneShotServerImpl.class);
		implMap.put(Protocol.P4JRPC, OneShotServerImpl.class);
		implMap.put(Protocol.P4JRPCSSL, OneShotServerImpl.class);
		implMap.put(Protocol.P4JRPCNTS, NtsServerImpl.class);
		implMap.put(Protocol.P4JRPCNTSSSL, NtsServerImpl.class);
		implMap.put(Protocol.P4JRSH, OneShotServerImpl.class);
		implMap.put(Protocol.P4JRSHNTS, NtsServerImpl.class);
		Log.info("P4Java server factory loaded; version: " + Metadata.getP4JVersionString()
				+ "; date: " + Metadata.getP4JDateString());
		Log.info("Using default charset: " + CharsetDefs.DEFAULT
				+ "; JVM charset: " + CharsetDefs.LOCAL);
		rpcFileCommandsHelper = OSUtils.isWindows() 
		        ? new WindowsRpcSystemFileCommandsHelper() : new RpcSystemFileCommandsHelper();
		Log.info("Using default RPC system file command helper: "
				+ rpcFileCommandsHelper.getClass().getCanonicalName());
	}
	
	/**
	 * Return a non-null list of implementation metadata about available IServer
	 * implementations. This can be useful for presenting implementation choices to
	 * end users, or for debugging, etc.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<IServerImplMetadata> getAvailableImplementationMetadata() {
		Map<Class, IServerImplMetadata> implMetadataMap = new HashMap<Class, IServerImplMetadata>();
		for (Protocol key : implMap.keySet()) {
			Class implClass = (Class) implMap.get(key);
			
			IServerImplMetadata md = getImplMetadata(implClass);
			
			if (md != null) {
				implMetadataMap.put(implClass, md);
			}
		}
		return new ArrayList<IServerImplMetadata>(implMetadataMap.values());
	}
	
	/**
	 * Return an IServer interface onto an underlying Perforce server at the host
	 * address specified by serverUriString using the protocol implementation and
	 * passed-in properties. Note that we use the term "URL" here a lot when we typically
	 * mean "URI"; this is mostly done to pre-empt confusion, but we may tighten
	 * usage up a lot over time.<p>
	 * 
	 * The format of the server URI string is protocol + "://" + hostaddr [+ ":" + port] [+ queryString],
	 * e.g. "p4java://example.perforce.com:1666" or "p4java://192.168.1.2:1999" or
	 * "p4jrpc://example.perforce.com:1199?progName=p4javaTest21&progVersion=Alpha203B".
	 * The protocol, port, and hostaddr fields can't be missing, but the port and hostaddr fields
	 * can be anything acceptable to the protocol, e.g. typically something like an IP address or
	 * hostname coupled with a port number.<p>
	 * 
	 * The protocol part specifies which network implementation to use; the value "p4java"
	 * (DEFAULT_PROTOCOL_NAME) will tell the factory to use the default protocol, the details
	 * of which are not spelled out here, and which should always be used unless you have
	 * good reason to use an alternative (other protocol part values are possible, but should
	 * not generally be used unless suggested by Perforce support staff).<p>
	 * 
	 * To connect to an SSL-enabled Perforce server, use one of the SSL protocols defined in
	 * the IServerAddress.Protocol enum (i.e. "p4javassl", "p4rpcssl" or "p4jrpcntsssl").<p>
	 * 
	 * For advanced users, the optional queryString part can be used to set P4Java properties
	 * from the URI; these override any correspondingly-named properties sent programmatically through
	 * the props parameter (see next paragraph). These URI query strings are not interpreted,
	 * but are simply put as-is into the properties passed to the individual server implementation
	 * class. Note: this method does not do any query string replacement of things like
	 * %20 to spaces, etc. (this really isn't needed here as the query parts are passed
	 * as-is to the underlying implementation(s)).<p>
	 * 
	 * The props argument can be used to pass in any protocol-specific properties; these
	 * are typically described in usage or implementation notes supplied elsewhere, and are
	 * not typically used by end-users.<p>
	 * 
	 * IServer objects returned by this method may have been retrieved from a cache of
	 * previously-used objects, but they are guaranteed to be ready for use by the consumer,
	 * and to be for that consumer's exclusive use only.
	 * 
	 * @param serverUriString non-null server address in URI form.
	 * @param props protocol-specific properties; may be null
	 * @return a non-null IServer object ready for use by the consumer.
	 * @throws URISyntaxException if the passed-in URI string does not conform to
	 * 			the Perforce URI specs detailed above.
	 * @throws ConnectionException if the factory is unable to connect to the server
	 * 				named by serverUrl
	 * @throws NoSuchObjectException if no implementation class can be found for
	 * 				the protocol specified in the passed-in URI protocol (scheme part);
	 * @throws ConfigException if the underlying protocol supplier detects a misconfiguration
	 * @throws ResourceException if the factory does not have the resources available to
	 * 				service the request
	 */
	
	public static IServer getServer(String serverUriString, Properties props)
					throws URISyntaxException, ConnectionException, NoSuchObjectException,
							ConfigException, ResourceException{
		return getOptionsServer(serverUriString, props);
	}
	
	/**
	 * Return an IOptionsServer onto an underlying Perforce server at the host
	 * address specified by serverUriString using the protocol implementation and
	 * passed-in properties and a default UsageOptions object.<p>
	 * 
	 * Basically a convenience wrapper for calling the main getOptionsServer method
	 * with a null UsageOptions argument -- see that method's Javadoc for full
	 * documentation.
	 * 
	 * @param serverUriString non-null server address in URI form.
	 * @param props protocol-specific properties; may be null
	 * @return a non-null IOptionsServer object ready for use by the consumer.
	 * @throws URISyntaxException if the passed-in URI string does not conform to
	 * 			the Perforce URI specs detailed above.
	 * @throws ConnectionException if the factory is unable to connect to the server
	 * 				named by serverUrl
	 * @throws NoSuchObjectException if no implementation class can be found for
	 * 				the protocol specified in the passed-in URI protocol (scheme part);
	 * @throws ConfigException if the underlying protocol supplier detects a misconfiguration
	 * @throws ResourceException if the factory does not have the resources available to
	 * 				service the request
	 */
	public static IOptionsServer getOptionsServer(String serverUriString, Properties props)
				throws URISyntaxException, ConnectionException, NoSuchObjectException,
						ConfigException, ResourceException {
		return getOptionsServer(serverUriString, props, null);
	}
	
	/**
	 * Return an IOptionsServer interface onto an underlying Perforce server at the host
	 * address specified by serverUriString using the protocol implementation and
	 * passed-in properties and usage options. Note that we use the term "URL" here a lot
	 * when we typically mean "URI"; this is mostly done to pre-empt confusion, but we may
	 * tighten usage up a lot over time.<p>
	 * 
	 * The format of the server URI string is protocol + "://" + hostaddr [+ ":" + port] [+ queryString],
	 * e.g. "p4java://example.perforce.com:1666" or "p4java://192.168.1.2:1999" or
	 * "p4java://example.perforce.com:1199?progName=p4javaTest21&progVersion=Alpha203B".
	 * The protocol, port, and hostaddr fields can't be missing, but the port and hostaddr fields
	 * can be anything acceptable to the protocol, e.g. typically something like an IP address or
	 * hostname coupled with a port number.<p>
	 * 
	 * The protocol part specifies which network implementation to use; the value "p4java"
	 * (DEFAULT_PROTOCOL_NAME) will tell the factory to use the default protocol, the details
	 * of which are not spelled out here, and which should always be used unless you have
	 * good reason to use an alternative (other protocol part values are possible, but should
	 * not generally be used unless suggested by Perforce support staff).<p>
	 * 
	 * To connect to an SSL-enabled Perforce server, use one of the SSL protocols defined in
	 * the IServerAddress.Protocol enum (i.e. "p4javassl", "p4rpcssl" or "p4jrpcntsssl").<p>
	 * 
	 * For advanced users, the optional queryString part can be used to set P4Java properties
	 * from the URI; these override any correspondingly-named properties sent programmatically through
	 * the props parameter (see next paragraph). These URI query strings are not interpreted,
	 * but are simply put as-is into the properties passed to the individual server implementation
	 * class. Note: this method does not do any query string replacement of things like
	 * %20 to spaces, etc. (this really isn't needed here as the query parts are passed
	 * as-is to the underlying implementation(s)).<p>
	 * 
	 * The props argument can be used to pass in any protocol-specific properties; these
	 * are typically described in usage or implementation notes supplied elsewhere, and are
	 * not typically used by end-users.<p>
	 * 
	 * IServer objects returned by this method may have been retrieved from a cache of
	 * previously-used objects, but they are guaranteed to be ready for use by the consumer,
	 * and to be for that consumer's exclusive use only.
	 * 
	 * @param serverUriString non-null server address in URI form.
	 * @param props protocol-specific properties; may be null.
	 * @param opts UsageOptions object to be associated with the new server object
	 * 			specifying the server's usage options; if null, a new usage options object
	 * 			is constructed using the default UsageOptions constructor and associated
	 * 			default values using the passed-in properties object (if not null); this is
	 * 			then associated with the new server object.
	 * @return a non-null IOptionsServer object ready for use by the consumer.
	 * @throws URISyntaxException if the passed-in URI string does not conform to
	 * 			the Perforce URI specs detailed above.
	 * @throws ConnectionException if the factory is unable to connect to the server
	 * 				named by serverUrl
	 * @throws NoSuchObjectException if no implementation class can be found for
	 * 				the protocol specified in the passed-in URI protocol (scheme part);
	 * @throws ConfigException if the underlying protocol supplier detects a misconfiguration
	 * @throws ResourceException if the factory does not have the resources available to
	 * 				service the request
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static IOptionsServer getOptionsServer(String serverUriString, Properties props, UsageOptions opts)
					throws URISyntaxException, ConnectionException, NoSuchObjectException,
							ConfigException, ResourceException {
		if (serverUriString == null) {
			throw new NullPointerError("Null server serverUriString passed to server factory");
		}

		Log.info("P4Java server factory called for Perforce server URI: " + serverUriString);

		ServerAddressBuilder addressBuilder = new ServerAddressBuilder(serverUriString);
		IServerAddress serverAddress = addressBuilder.build();

		if (serverAddress == null) {
			throw new NullPointerError("Null server address constructed with Perforce server URI:" + serverUriString);
		}
		if (serverAddress.getProtocol() == null) {
			throw new URISyntaxException(serverUriString, "unknown protocol");
		}
		if (serverAddress.getHost() == null) {
			throw new URISyntaxException(serverUriString, "missing or malformed Perforce server hostname");
		}
		if (serverAddress.getPort() < 0) {
			throw new URISyntaxException(serverUriString, "missing or malformed Perforce server port specifier");
		}
		
		IServerImplMetadata implMetadata = getImplMetadata(implMap.get(serverAddress.getProtocol()));
		
		if (implMetadata == null) {
			throw new NoSuchObjectException("No such server implementation found for protocol '"
										+ serverAddress.getProtocol().toString() + "'");
		}
		
		// Combine the properties
		if (serverAddress.getProperties() != null) {
			if (props == null) {
				props = new Properties();
			}
			props.putAll(serverAddress.getProperties());
		}
		
		try {
			Class serverImplClass = Class.forName(implMetadata.getImplClassName());
			Log.info("Using Server implementation class: " + serverImplClass.getCanonicalName());
			
			IServerControl serverImpl = (IServerControl) serverImplClass.newInstance();
			serverImpl.init(serverAddress.getHost(), serverAddress.getPort(), props, opts,
					serverAddress.getProtocol().isSecure(), serverAddress.getRsh());
			return (IOptionsServer) serverImpl;	// This part may also cause a cast exception...	
		} catch (ClassNotFoundException cnfe) {
			Log.error("Unable to instantiate Perforce server implementation class '"
					+ implMetadata.getImplClassName() + "' (class not found)");
			Log.exception(cnfe);
			throw new NoSuchObjectException(
								"No such p4j server implementation class found for protocol",
								cnfe);
		} catch(ClassCastException cce) {
			Log.error("Unable to instantiate Perforce server implementation class '"
					+ implMetadata.getImplClassName() + "' (class cast error)");
			Log.exception(cce);
			throw new ConfigException(
				"Specified Perforce server implementation class does not implement required interface(s)",
								cce);
		} catch (InstantiationException ie) {
			Log.error("Unable to instantiate Perforce server implementation class '"
					+ implMetadata.getImplClassName() + "' (instantiation failed)");
			Log.exception(ie);
			throw new ConfigException(
								"Unable to instantiate Perforce server implementation class '"
								+ implMetadata.getImplClassName() + "'",
								ie);
		} catch (IllegalAccessException iae) {
			Log.error("Unable to instantiate Perforce server implementation class '"
					+ implMetadata.getImplClassName() + "' (illegal access exception)");
			Log.exception(iae);
			throw new ConfigException(
								"Unable to instantiate Perforce server class '"
								+ implMetadata.getImplClassName() + "'",
								iae);
		}
	}
	
	/**
	 * Deprecated way to get an IServer object -- see getServer(String, Properties) for
	 * the correct way to get a server.
	 * 
	 * @deprecated as of the 2009.2 release, use the getServer(String, Properties) method
	 * 				due to that Java's URI class does not accept hostnames with anything
	 * 				other than alphanumeric characters: even common hostnames like
	 * 				"perforce_p" will fail, often silently.
	 * @param serverUrl non-null server URI in the format described above
	 * @param props protocol-specific properties; may be null
	 * 
	 * @return a non-null IServer object ready for use by the consumer.
	 * 
	 * @throws ConnectionException if the factory is unable to connect to the server
	 * 				named by serverUrl
	 * @throws NoSuchObjectException if no implementation class can be found for
	 * 				the protocol specified in the passed-in URI protocol (scheme part);
	 * @throws ConfigException if the underlying protocol supplier detects a misconfiguration
	 * @throws ResourceException if the factory does not have the resources available to
	 * 				service the request
	 * @throws URISyntaxException if the passed-in URI is malformed.
	 */
	
	@Deprecated
	public static IServer getServer(URI serverUrl, Properties props)
				throws ConnectionException, NoSuchObjectException,
						ConfigException, ResourceException, URISyntaxException {
		
		if (serverUrl == null) {
			throw new NullPointerError("Null server URI passed to server factory");
		}

		return getServer(serverUrl.toString(), props);
	}
	
	private static IServerImplMetadata getImplMetadata(
								final Class<Server> implClass) {
		
		if (implClass == null) {
			return null;
		}

		return new IServerImplMetadata() {

			public String getScreenName() {
				try {
					return (String) implClass.getField(Server.SCREEN_NAME_FIELD_NAME).get(null);
				} catch (Exception exc) {
					Log.error("Unexpected exception in ServerFactory.getImplMetadata: "
							+ exc.getMessage());
					Log.exception(exc);
				}
				return "Unknown";
			}
			
			public String getComments() {
				try {
					return (String) implClass.getField(Server.IMPL_COMMENTS_FIELD_NAME).get(null);
				} catch (Exception exc) {
					Log.error("Unexpected exception in ServerFactory.getImplMetadata: "
							+ exc.getMessage());
					Log.exception(exc);
				}
				return "No comments available";
			}

			public ImplType getImplType() {
				try {
					return (ImplType) implClass.getField(Server.IMPL_TYPE_FIELD_NAME).get(null);
				} catch (Exception exc) {
					Log.error("Unexpected exception in ServerFactory.getImplMetadata: "
							+ exc.getMessage());
					Log.exception(exc);
				}
				return ImplType.UNKNOWN;
			}

			public int getMinimumServerLevel() {
				try {
					return (Integer) implClass.getField(
							Server.MINIMUM_SUPPORTED_SERVER_LEVEL_FIELD_NAME).get(null);
				} catch (Exception exc) {
					Log.error("Unexpected exception in ServerFactory.getImplMetadata: "
							+ exc.getMessage());
					Log.exception(exc);
				}
				return -1;
			}

			public String getUriScheme() {
				try {
					return (String) implClass.getField(Server.PROTOCOL_NAME_FIELD_NAME).get(null);
				} catch (Exception exc) {
					Log.error("Unexpected exception in ServerFactory.getImplMetadata: "
							+ exc.getMessage());
					Log.exception(exc);
				}
				return "p4j";
			}

			public boolean isDefault() {
				try {
					return (Boolean) implClass.getField(Server.DEFAULT_STATUS_FIELD_NAME).get(null);
				} catch (Exception exc) {
					Log.error("Unexpected exception in ServerFactory.getImplMetadata: "
							+ exc.getMessage());
					Log.exception(exc);
				}
				return false;
			}

			public String getImplClassName() {
				try {
					return implClass.getCanonicalName();
				} catch (Exception exc) {
					Log.error("Unexpected exception in ServerFactory.getImplMetadata: "
							+ exc.getMessage());
					Log.exception(exc);
				}
				return "com.perforce.p4java.NoSuchImplClass";
			}
		};
	}
	
	/**
	 * Register an ISystemFileCommandsHelper for the RPC implementations. See
	 * the documentation for the ISystemFileCommandsHelper interface for more
	 * semantic and usage details.<p>
	 * 
	 * Helper classes are needed for certain hosted implementations and for some
	 * JDK 5 installations, and are shared across an entire instance of P4Java.
	 * Please do not register a helper class unless you know what you're doing and
	 * you're OK with the dire consequences of getting a helper class implementation wrong.<p>
	 * 
	 * Note that if the fsCmdHelper parameter is null, the default internal implementation
	 * will be used, which is usually fine for JDK 6 systems.
	 * 
	 * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper
	 * @param fsCmdHelper file helper interface implementation; if null, use the
	 * 			JVM default implementation(s).
	 */
	public static void setRpcFileSystemHelper(ISystemFileCommandsHelper fsCmdHelper) {
		rpcFileCommandsHelper = fsCmdHelper;
		Log.info("setting RPC system file helper to "
				+ (fsCmdHelper == null ? "null" : "class " + fsCmdHelper.getClass().getCanonicalName()));
	}
	
	/**
	 * Return the current SystemFileCommands helper, if any.
	 */
	public static ISystemFileCommandsHelper getRpcFileSystemHelper() {
		return rpcFileCommandsHelper;
	}
	
	/**
	 * Return a list of Perforce servers registered locally with zeroconf at
	 * the time the method was called. See the Perforce knowledge base
	 * articles at http://kb.perforce.com/AdminTasks/Zeroconf for an
	 * introduction to Perforce zeroconf usage, and zeroconf.org for
	 * zeroconf in general.<p>
	 * 
	 * This method uses the javax.jmdns JmDNS package (available through
	 * Sourceforge, etc.), and that package must be visible to the current
	 * class loader for this method to work. If the server factory is unable
	 * to find a suitable JmDNS package using the current class loader,
	 * this method will throw a ConfigException. The JmDNS package is <i>not</i>
	 * supplied with P4Java and must be downloaded and installed separately.<p>
	 * 
	 * Note that zeroconf discovery can take some time (in the order of tens
	 * of seconds in some cases), and only works for local subnets, so the
	 * first call to this method may return nothing even though there's a
	 * suitable server out there on the same subnet. Subsequent calls are
	 * usually more successful.<p>
	 * 
	 * Note also that we recommend you call the associated isZeroconfAvailable
	 * method first to check whether zeroconf is even available for this P4Java
	 * instance -- this can save a lot of overhead and / or annoying log
	 * messages.
	 * 
	 * @return non-null (but possibly-empty) list of ZeroconfServerInfo objects
	 * 				for Perforce servers registered when this method is called.
	 * @throws ConfigException if the server factory can't load and use a suitable
	 * 				JmDNS zeroconf package using the current class loader.
	 * 
	 * @deprecated  As of release 2013.1, ZeroConf is no longer supported by the
	 * 				Perforce server 2013.1.
	 */
	@Deprecated
	public static List<ZeroconfServerInfo> getZeroconfServers() throws ConfigException {
		if (zcHelper == null) {
			zcHelper = new ZeroconfHelper();
		}
		return zcHelper.getZeroconfServers();
	}
	
	/**
	 * Returns true if the server factory has a suitable zeroconf service browsing
	 * implementation available to it. Should probably be used at least once before
	 * calling the getZeroConfServers method to avoid unnecessary overhead.
	 * 
	 * @return true iff zeroconf browsing services are available to the server factory.
	 * 
	 * @deprecated  As of release 2013.1, ZeroConf is no longer supported by the
	 * 				Perforce server 2013.1.
	 */
	@Deprecated
	public static boolean isZeroConfAvailable() {
		if (zcHelper == null) {
			zcHelper = new ZeroconfHelper();
		}
		
		return zcHelper.isZeroConfAvailable();
	}
}
