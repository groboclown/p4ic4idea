package com.perforce.p4java.tests.dev.unit.bug.r161;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.common.base.OSUtils;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.WindowsRpcSystemFileCommandsHelper;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;



/**
 * @author Sean Shou
 * @since 18/07/2016
 */

public class SyncExecutableFileTest extends P4JavaRshTestCase {
    private static final String RELATIVE_DEPOT_PATH = "/152Bugs/job081399/setEnv.sh";
    private static final String TEST_FILE_DEPOT_PATH = "//depot" + RELATIVE_DEPOT_PATH;
    /** The files helper. */
    private static SymbolicLinkHelper filesHelper;
    
    
    @ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncExecutableFileTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	filesHelper = OSUtils.isWindows() 
              ? new WindowsRpcSystemFileCommandsHelper() : new RpcSystemFileCommandsHelper();
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }
    
   
    @Test
    public void syncExecutableFileWithUnixLineEnding() throws Exception {
        syncExecutableFile("p4TestUnixLineend");
    }

    private void syncExecutableFile(String clientName) throws Exception {
        connectToServer(clientName);

        Path localFile = Paths.get(client.getRoot(), RELATIVE_DEPOT_PATH);
        filesHelper.setWritable(localFile.toString(), true);
        Files.deleteIfExists(localFile);

        syncFileFromDepot(TEST_FILE_DEPOT_PATH);
        Assert.assertEquals(Files.exists(localFile), true);
        Assert.assertEquals(Files.isExecutable(localFile), true);
        Assert.assertEquals(Files.isRegularFile(localFile), true);
        Assert.assertEquals(Files.isReadable(localFile), true);
    }

    private void syncFileFromDepot(String depotFile) throws P4JavaException{
        List<IFileSpec> files = client.sync(
                FileSpecBuilder.makeFileSpecList(depotFile),
                new SyncOptions().setForceUpdate(true));

        if (files.size() < 1) {
        	Assert.fail("Sync test file: " + depotFile + "failed");
        }
        Assert.assertEquals(TEST_FILE_DEPOT_PATH , files.get(0).getDepotPathString());
    }

    @Test
    public void syncExecutableFileWithWinLineEnding() throws Exception {
        syncExecutableFile("p4TestUserWSLineEndWin");
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
