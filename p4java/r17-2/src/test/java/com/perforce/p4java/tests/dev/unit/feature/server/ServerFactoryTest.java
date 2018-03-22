package com.perforce.p4java.tests.dev.unit.feature.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.common.base.OSUtils;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.WindowsRpcSystemFileCommandsHelper;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.IServerImplMetadata;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.ZeroconfServerInfo;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple server factory tests. Not a lot is actually tested here
 * because the factory's behaviour is commonly tested many times with
 * pretty much each unit test elsewhere.
 * 
 * @testid ServerFactoryTest01
 */

@TestId("Server_ServerFactoryTest01")
public class ServerFactoryTest extends P4JavaTestCase {

	@Test
	public void testGetServerDefaultProtocol() {
		try {
			IServer server = ServerFactory.getServer(serverUrlString, null);
			assertNotNull("Null server returned", server);
			// Not much else we can say at this point...
		} catch (ConnectionException e) {
			fail("ConnectionException thrown: " + e.getLocalizedMessage());
		} catch (NoSuchObjectException e) {
			fail("NoSuchObjectException thrown: " + e.getLocalizedMessage());
		} catch (ConfigException e) {
			fail("ConfigException thrown: " + e.getLocalizedMessage());
		} catch (ResourceException e) {
			fail("ResourceException thrown: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("URISyntaxException thrown: " + e.getLocalizedMessage());
		}
	}
	
	@Test
	public void testGetServerBadProtocol() {
		final String badUri = "xyz://localhost:1666";
		try {
			@SuppressWarnings("unused")
			IServer server = ServerFactory.getServer(badUri, null);
			fail("No exception thrown for bad server URI");
		} catch (ConnectionException e) {
			fail("ConnectionException thrown: " + e.getLocalizedMessage());
		} catch (ConfigException e) {
			fail("ConfigException thrown: " + e.getLocalizedMessage());
		} catch (NoSuchObjectException e) {
			// Expect this one...
		} catch (ResourceException e) {
			fail("ResourceException thrown: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			//expected
		}
	}
	
	@Test
	public void testGetServerBadHost() {
		final String badUri = "p4java://x1234567890:1666"; // probably want to make the value
														// of this a property at some stage -- HR.		
		try {
			IServer server = ServerFactory.getServer(badUri, null);
			assertNotNull("null server returned", server);
			server.connect();
			fail("No exception thrown for bad server host: " + badUri);
		} catch (ConnectionException e) {
			// Expect this one...
			assertNotNull("Null ConnectionException error message", e.getLocalizedMessage());
		} catch (NoSuchObjectException e) {
			fail("NoSuchObjectException thrown: " + e.getLocalizedMessage());
		} catch (ConfigException e) {
			fail("ConfigException thrown: " + e.getLocalizedMessage());
		} catch (ResourceException e) {
			fail("ResourceException thrown: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("URISyntaxException thrown: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("AccessException thrown:" + e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("RequestException thrown:" + e.getLocalizedMessage());
		}
	}
	
	/**
	 * Only tests for non-null, non-empty list return; can't in general
	 * check the actual individual results for anything much except non-nullness
	 * as these are not really well-specified.
	 */
	@Test
	public void testGetAvailableImplementationMetadata() {
		try {
			List<IServerImplMetadata> implList = ServerFactory.getAvailableImplementationMetadata();
			assertNotNull("null implementation metadata list returned from server factory", implList);
			assertTrue("no implementation metadat objects returned", implList.size() > 0);
			for (IServerImplMetadata implMd : implList) {
				assertNotNull(implMd);
				assertNotNull(implMd.getComments());
				assertNotNull(implMd.getImplClassName());
				assertNotNull(implMd.getImplType());
				assertNotNull(implMd.getMinimumServerLevel());
				assertNotNull(implMd.getScreenName());
				assertNotNull(implMd.getUriScheme());
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * Attempts a sanity check only -- anything more can take forever.
	 */
	@Test
	public void testZeroConfMethods() {
		try {
			boolean zcAvailable = ServerFactory.isZeroConfAvailable();
			if (zcAvailable) {
				List<ZeroconfServerInfo> zcServers =
								ServerFactory.getZeroconfServers();
				assertNotNull(zcServers);
			} else {				
				try {
					@SuppressWarnings("unused")
					List<ZeroconfServerInfo> zcServers =
												ServerFactory.getZeroconfServers();
					fail("no config exception on invalid ServerFactory.getZeroconfServers call");
				} catch (ConfigException ce) {
					
				}
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * Sanity checks only here...
	 */
	@Test
	public void testRpcFileSystemHelper() {
	    
	    	ISystemFileCommandsHelper origSFCH = ServerFactory.getRpcFileSystemHelper();
	    	
		try {
			// Test for default value:
		    Class<? extends SymbolicLinkHelper> implementation =
		            OSUtils.isWindows() 
		                ? WindowsRpcSystemFileCommandsHelper.class
		                : RpcSystemFileCommandsHelper.class;
			assertNotNull(ServerFactory.getRpcFileSystemHelper());
			assertEquals("Server factory default helper class not RpcSystemFileCommandsHelper",
					ServerFactory.getRpcFileSystemHelper().getClass(),
					implementation);
			
			// Set it and see what happens:
			ISystemFileCommandsHelper helper = new ISystemFileCommandsHelper() {

				public boolean canExecute(String fileName) {
					return false;
				}

				public boolean isSymlink(String fileName) {
					return false;
				}

				public boolean setExecutable(String fileName,
						boolean executable, boolean ownerOnly) {
					return false;
				}

				public boolean setOwnerReadOnly(String fileName) {
					return false;
				}

				public boolean setReadable(String fileName, boolean readable,
						boolean ownerOnly) {
					return false;
				}

				public boolean setWritable(String fileName, boolean writable) {
					return false;
				}
				
			};
			
			ServerFactory.setRpcFileSystemHelper(helper);
			assertNotNull(ServerFactory.getRpcFileSystemHelper());
			assertEquals("Server factory helper class not replaced",
					ServerFactory.getRpcFileSystemHelper().getClass(),
					helper.getClass());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
		    
			ServerFactory.setRpcFileSystemHelper(origSFCH);
		    
		}
	}
}
