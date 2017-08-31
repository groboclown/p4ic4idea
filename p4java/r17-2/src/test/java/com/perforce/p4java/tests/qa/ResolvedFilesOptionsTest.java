/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
showBaseRevision apparently does nothing.
*/

package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.option.client.ResolvedFilesOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.ResolvedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class ResolvedFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static ResolvedFilesOptions resolvedFilesOptions = null;
    private static Valids valids = null;
    private static List<IFileSpec> sourceFiles = null;
    private static List<IFileSpec> targetFiles = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        sourceFiles = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "source.txt", "Source", "text");

        targetFiles = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "target.txt", "Target", "text");

        client.integrateFiles(sourceFiles.get(0), targetFiles.get(0), null, new IntegrateFilesOptions().setDoBaselessMerge(true));

        client.resolveFilesAuto(targetFiles, new ResolveFilesAutoOptions().setForceResolve(true));
    }


    // OPTIONS SPECS
    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("b:o", OPTIONS_SPECS);
    }


    // DEFAULTS
    @Test
    public void defaultConstructor() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions();
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions(false);
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }


    // SHOW BASE REVISION
    @Test
    public void explicitConstructorShowBaseRevision() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions(true);
        valids = new Valids();
        valids.showBaseRevisionGet = true;
        valids.showBaseRevision = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setShowBaseRevision() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions();
        resolvedFilesOptions.setShowBaseRevision(true);
        valids = new Valids();
        valids.showBaseRevisionGet = true;
        valids.showBaseRevision = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void stringConstructorShowBaseRevision() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions("-o");
        valids = new Valids();
        valids.immutable = true;
        valids.showBaseRevision = true;
        testMethod(false);
    }

    @Test
    public void setImmutableFalseShowBaseRevision() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions();
        resolvedFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        resolvedFilesOptions.setShowBaseRevision(true);
        valids = new Valids();
        valids.showBaseRevisionGet = true;
        valids.showBaseRevision = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setImmutableTrueShowBaseRevision() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions();
        resolvedFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        resolvedFilesOptions.setShowBaseRevision(true);
        valids = new Valids();
        valids.immutable = true;
        valids.showBaseRevisionGet = true;
        testMethod(true);
        testMethod(false);
    }


    // SETTER RETURNS
    @Test
    public void setterReturns() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions();
        assertEquals(ResolvedFilesOptions.class, resolvedFilesOptions.setShowBaseRevision(true).getClass());
    }


    // OVERRIDE STRING CONSTRUCTOR
    @Test
    public void overrideStringConstructor() throws Throwable {
        resolvedFilesOptions = new ResolvedFilesOptions("-o");
        resolvedFilesOptions.setShowBaseRevision(false);
        valids = new Valids();
        valids.immutable = true;
        valids.showBaseRevisionGet = false;
        valids.showBaseRevision = true;
        testMethod(false);
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

    private static void testMethod(boolean useOldMethod) throws Throwable {

        assertEquals(valids.immutable, resolvedFilesOptions.isImmutable());
        assertEquals(valids.showBaseRevisionGet, resolvedFilesOptions.isShowBaseRevision());

        List<IFileSpec> resolvedFiles = null;

        if (useOldMethod) {

            resolvedFiles = client.resolvedFiles(null, resolvedFilesOptions.isShowBaseRevision());

        } else {

            resolvedFiles = client.resolvedFiles(null, resolvedFilesOptions);

        }

        h.validateFileSpecs(resolvedFiles);

        assertEquals(1, resolvedFiles.size());
        assertEquals(sourceFiles.get(0).getOriginalPathString(), resolvedFiles.get(0).getFromFile());

    }

    @Ignore
    private static class Valids {

        private boolean immutable = false;
        private boolean showBaseRevisionGet = false;
        @SuppressWarnings("unused")
        private boolean showBaseRevision = false;

    }

}