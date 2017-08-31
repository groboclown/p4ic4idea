package com.perforce.p4java.tests.dev.unit.feature.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;



/**
 * 
 * This test class exercised the API of P4Java Changelist. It was created as a catch-all for
 * issues that were found or questions that were asked during development of other tests
 * that used this API. Therefore, it is in no way exhaustive. 
 */

@TestId("ChangelistAPITest01")
public class ChangelistAPITest extends P4JavaTestCase {
	
	@Test(expected=com.perforce.p4java.exception.RequestException.class)
	public void testCreateChangelistImplDefaultId() throws AccessException, ConnectionException, RequestException { 
		
			IServer server = null;
			IClient client = null;
		
			try {
				server = getServer();
				assertNotNull("Server unexpectedly Null.", server);
				client = this.getDefaultClient(server);
				assertNotNull("Null client returned", client);
				server.setCurrentClient(client);
				
			} catch (Exception exc) {
				System.err.println("Unexpected Exception: " + exc.getLocalizedMessage());
				fail("Unexpected Exception: " + exc.getLocalizedMessage());			
			} 
			
			@SuppressWarnings("unused")
			IChangelist changelist = createTestChangelist(server, client, "This creation should fail", true); 

	}


	@Test(expected=com.perforce.p4java.exception.RequestException.class)
	public void testDefaultConstructorGetFilesTrue()
				throws AccessException, ConnectionException, RequestException { 
		
		Changelist changelist = new Changelist();
		
		List<IFileSpec> files = changelist.getFiles(true);
		debugPrint("DefaultConstructor changelist.getFiles()" + files);
		assertEquals("DefaultConstructor changelist.getFiles() should be Null", null, files);
		
	}

	@Test(expected=com.perforce.p4java.exception.RequestException.class)
	public void testDefaultConstructorGetFilesFalse()
				throws AccessException, ConnectionException, RequestException { 
		
		Changelist changelist = new Changelist();
		
		List<IFileSpec> files = changelist.getFiles(false);
		debugPrint("By default files is " + files);
		assertEquals("Files should be null for a new changelist", null, files);
		
	}

	
	@Test
	public void testDefaultConstructorGetFileSpecs() throws AccessException, ConnectionException, RequestException { 
		
		Changelist changelist = new Changelist();
		
		List<IFileSpec> files = changelist.getFileSpecs();
		debugPrint("By default filespecs is " + files);
		assertEquals("Files should be null for a new changelist", null, files);
	}

	@Test
	public void testChangelistDefaultConstructorGetters() {
		
		debugPrintTestName();
		
		try {
		
			Changelist changelist = new Changelist();
		
			int id = changelist.getId();
			debugPrint("DefaultConstructor changelist.getId()" + id);
			assertEquals("DefaultConstructor changelist.getId() should return IChangelist.UNKNOWN", IChangelist.UNKNOWN, id);
			
			boolean bShelved = changelist.isShelved();
			debugPrint("DefaultConstructor changelist.isShelved()" + bShelved);
			assertFalse("DefaultConstructor changelist.isShelved() should return False", bShelved);
			
			String clientId = changelist.getClientId();
			debugPrint("DefaultConstructor changelist.getClientId()" + clientId);
			assertEquals("DefaultConstructor changelist.getClientId() should be Null", null, clientId);
						
			String uName = changelist.getUsername();
			debugPrint("DefaultConstructor changelist.getUsername()" + uName);
			assertEquals("DefaultConstructor changelist.getUsername() should be Null", null, uName);
			
			ChangelistStatus changeStat = changelist.getStatus();
			debugPrint("DefaultConstructor changelist.getStatus()" + changeStat);
			assertEquals("DefaultConstructor changelist.getStatus() should be Null", null, changeStat);
				
			java.util.Date changeDate = changelist.getDate();
			debugPrint("DefaultConstructor changelist.getUsername()" + changeDate);
			assertEquals("DefaultConstructor changelist.getUsername() should be Null", null, changeDate);
						
			String descr = changelist.getDescription();
			debugPrint("DefaultConstructor changelist.getDescription()" + descr);
			assertEquals("DefaultConstructor changelist.getDescription() should be Null", null, descr);			
			

		} catch (Exception exc) {
			System.err.println("Unexpected Exception: " + exc.getLocalizedMessage());
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		} 


	
	}

	public Changelist createNewChangelistImpl(IServer server, IClient client, String chgDescr, boolean bUseDefault) {

		int cId = IChangelist.DEFAULT;
		if(bUseDefault == false) {
			cId = IChangelist.UNKNOWN;
		}
		Changelist changeListImpl = null;
		try {
			changeListImpl = new Changelist(
					cId,
					client.getName(),
					userName,
					ChangelistStatus.NEW,
					new Date(),
					chgDescr,
					false,
					(Server) server
			);
		} catch (Exception exc) {
			System.err.println("Unexpected Exception when setting changelist: " + exc.getLocalizedMessage());
			fail("Unexpected Exception when setting changelist: " + exc.getLocalizedMessage());
		} 

		debugPrint("Created Changelist (-1=UNKNOWN 0=DEFAULT): " + cId);
		return changeListImpl;
	}

	public IChangelist createTestChangelist(IServer server, IClient client, String chgDescr, boolean bUseDefault) throws
			 ConnectionException, RequestException, AccessException {
	
		Changelist changeListImpl = createNewChangelistImpl(server, client, chgDescr, bUseDefault);
		IChangelist changelist = client.createChangelist(changeListImpl);
		
		debugPrint("Created Changelist ID: " + changelist.getId());

		return changelist;
	}
}
