/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features151;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'rsh' mode server.
 * Set up a local p4d with user "test" and workspace "test_ws"
 * and upload file(s) to //depot/... to run this test
 */
@Jobs({ "job034706" })
@TestId("Dev151_RshServerTestThreaded")
public class RshServerTestThreaded extends P4JavaTestCase {
    
	IOptionsServer server = null;
    IClient client = null;

    class SyncDepot implements Runnable {


    	String syncPath = null;

    	SyncDepot(String path) {
    		this.syncPath = path;
    	}

    	public void run() {

    		try
    		{
    			String rshCmdUri = "p4jrsh:///usr/local/bin/p4d -r /tmp/rsh-root -i --java";                                

    			//String rshCmdUri = "p4jrshnts:///usr/bin/p4d -r /tmp/rsh-root -i --java -vserver=5 -vrpc=5 -vnet=5 -L/tmp/rsh-root/log";

    			try {
    				// Connect to a replica server
    				server = ServerFactory.getOptionsServer(rshCmdUri, null);
    				assertNotNull(server);

    				// Connect to the server.
    				server.connect();
    				if (server.isConnected()) {
    					if (server.supportsUnicode()) {
    						server.setCharsetName("utf8");
    					}
    				}

    				// Get server info
    				IServerInfo serverInfo = server.getServerInfo();
    				assertNotNull(serverInfo);

    				System.out.println("Server Version: " + serverInfo.getServerVersion());
    				System.out.println("Server Root: " + serverInfo.getServerRoot());


    				server.setUserName("test");

    				client = server.getClient("test_ws");
    				assertNotNull(client);
    				server.setCurrentClient(client);

    				client.sync( FileSpecBuilder.makeFileSpecList(syncPath), new SyncOptions());

    			} catch (P4JavaException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}

    		} catch (URISyntaxException e) {
    			fail("Unexpected exception: " + e.getLocalizedMessage());
    		}
    	}
    }


    /**
     * @BeforeClass annotation to a method to be run before all the tests in a
     *              class.
     */
    @BeforeClass
    public static void oneTimeSetUp() {
            // one-time initialization code (before all the tests).
    }

    /**
     * @AfterClass annotation to a method to be run after all the tests in a
     *             class.
     */
    @AfterClass
    public static void oneTimeTearDown() {
            // one-time cleanup code (after all the tests).
    }

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
            // initialization code (before each test).
            File files = new File("/tmp/rsh-root");
            files.mkdirs();
    }

    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @After
    public void tearDown() {
            // cleanup code (after each test).
            if (server != null) {
                    this.endServerSession(server);
            }
    }

    /**
     * Test 'rsh' mode server.
     */
    @Test
    public void testRshServer() {

    	try{        
    		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    		//adjust number of threads as needed for testing		
    		for (int i = 1; i <= 2; i++)
    		{
    			String depotPath = "//depot/...";
    			SyncDepot task = new SyncDepot(depotPath);
    			System.out.println("A new task has been added to sync : " + depotPath);
    			executor.execute(task);
    		}
    		executor.shutdown();

    		while (!executor.isTerminated()) {
    			System.out.println("Threads are still running...");
    			Thread.sleep(1000);
    		}

    		System.out.println("Finished all threads");

    	} catch (Exception exc) {                            
    		fail("Unexpected exception: " + exc.getLocalizedMessage());
    	}
    }
}
                        
                       