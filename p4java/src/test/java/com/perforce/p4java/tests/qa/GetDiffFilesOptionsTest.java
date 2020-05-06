/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
Setting multiple -s options (only one is honored)
-m flag is ignored so don't bother checking
-t flag is ignored so don't bother checking
*/

package com.perforce.p4java.tests.qa;


import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.GetDiffFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.option.client.GetDiffFilesOptions.OPTIONS_SPECS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

@RunWith(JUnitPlatform.class)
public class GetDiffFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IClient client = null;
    private static File unopenedSame = null;
    private static File unopenedDifferent = null;
    private static File unopenedMissing = null;
    private static File openedSame = null;
    private static File openedDifferent = null;
    private static File openedDifferentBinary = null;
    private static File openedMissing = null;
    private static File openedForIntegResolvedNotModified = null;
    private static File openedForIntegResolvedModified = null;
    private static File integSource = null;
    private static boolean ConnectionExceptionWhileImmutable = false;

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());

        ts.initialize();
        // just use RSH
        //ts.start();

        IOptionsServer server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        List<IFileSpec> tempFileSpecs = null;

        unopenedSame = new File(client.getRoot(), "unopened-same.txt");
        helper.addFile(server, user, client, unopenedSame.getAbsolutePath(), "GetDiffFilesOptions", "text");

        unopenedDifferent = new File(client.getRoot(), "unopened-different.txt");
        helper.addFile(server, user, client, unopenedDifferent.getAbsolutePath(), "GetDiffFilesOptions", "text");
        unopenedDifferent.delete();
        helper.createFile(unopenedDifferent.getAbsolutePath(), "edited");

        unopenedMissing = new File(client.getRoot(), "unopened-missing.txt");
        helper.addFile(server, user, client, unopenedMissing.getAbsolutePath(), "GetDiffFilesOptions", "text");
        unopenedMissing.delete();

        openedSame = new File(client.getRoot(), "opened-same.txt");
        tempFileSpecs = helper.addFile(server, user, client, openedSame.getAbsolutePath(), "GetDiffFilesOptions", "text");
        helper.validateFileSpecs(client.editFiles(tempFileSpecs, new EditFilesOptions()));

        openedDifferent = new File(client.getRoot(), "opened-different.txt");
        tempFileSpecs = helper.addFile(server, user, client, openedDifferent.getAbsolutePath(), "GetDiffFilesOptions", "text");
        helper.validateFileSpecs(client.editFiles(tempFileSpecs, new EditFilesOptions()));
        helper.createFile(openedDifferent.getAbsolutePath(), "edited");

        openedDifferentBinary = new File(client.getRoot(), "opened-different-binary.bin");
        tempFileSpecs = helper.addFile(server, user, client, openedDifferentBinary.getAbsolutePath(), "123", "binary");
        helper.validateFileSpecs(client.editFiles(tempFileSpecs, new EditFilesOptions()));
        helper.createFile(openedDifferentBinary.getAbsolutePath(), "456");

        openedMissing = new File(client.getRoot(), "opened-missing.txt");
        tempFileSpecs = helper.addFile(server, user, client, openedMissing.getAbsolutePath(), "GetDiffFilesOptions", "text");
        helper.validateFileSpecs(client.editFiles(tempFileSpecs, new EditFilesOptions()));
        openedMissing.delete();

        integSource = new File(client.getRoot(), "integ-source.txt");
        List<IFileSpec> integSourceFileSpecs = helper.addFile(server, user, client, integSource.getAbsolutePath(), "integ source", "text");

        openedForIntegResolvedNotModified = new File(client.getRoot(), "opened-for-integ-resolved-not-modified.txt");
        List<IFileSpec> integTargetNotModifiedFileSpecs = helper.addFile(server, user, client, openedForIntegResolvedNotModified.getAbsolutePath(), "GetDiffFilesOptions", "text");
        helper.validateFileSpecs(client.integrateFiles(integSourceFileSpecs.get(0), integTargetNotModifiedFileSpecs.get(0), null, new IntegrateFilesOptions().setDoBaselessMerge(true)));
        helper.validateFileSpecs(client.resolveFilesAuto(integTargetNotModifiedFileSpecs, new ResolveFilesAutoOptions().setForceResolve(true)));

        openedForIntegResolvedModified = new File(client.getRoot(), "opened-for-integ-resolved-modified.txt");
        List<IFileSpec> integTargetModifiedFileSpecs = helper.addFile(server, user, client, openedForIntegResolvedModified.getAbsolutePath(), "GetDiffFilesOptions", "text");
        helper.validateFileSpecs(client.integrateFiles(integSourceFileSpecs.get(0), integTargetModifiedFileSpecs.get(0), null, new IntegrateFilesOptions().setDoBaselessMerge(true)));
        helper.validateFileSpecs(client.resolveFilesAuto(integTargetModifiedFileSpecs, new ResolveFilesAutoOptions().setForceResolve(true)));
        openedForIntegResolvedModified.delete();
        helper.createFile(openedForIntegResolvedModified.getAbsolutePath(), "edited");
    }


    @Test
    public void optionsSpecs() throws Throwable {
        assertThat(OPTIONS_SPECS, is("i:m:gtz b:t b:sa b:sb b:sd b:se b:sl b:sr"));
    }


    @Test
    @Ignore
    public void defaultConstructor() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        Valids valids = new Valids();
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void explicitConstructorDefaults() throws Throwable {
	    GetDiffFilesOptions getDiffFilesOptions = null;
        getDiffFilesOptions = new GetDiffFilesOptions(0, false, false, false, false, false, false, false);
        Valids valids = new Valids();
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }


    @Test
    public void explicitConstructorOpenedDifferentMissing() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions(0, false, true, false, false, false, false, false);
        Valids valids = new Valids();
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setOpenedDifferentMissingFalse() throws Throwable {
	    GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setOpenedDifferentMissing(false);
        Valids valids = new Valids();
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setOpenedDifferentMissingTrue() throws Throwable {
	    GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
	    testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void stringConstructorOpenedDifferentMissing() throws Throwable {
	    GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-sa");
	    getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.openedDifferentMissing = true;
	    valids.openedDifferentMissingGet = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableFalseOpenedDifferentMissing() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(false);
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.immutable = false;
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableTrueOpenedDifferentMissing() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(true);
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(false, valids, getDiffFilesOptions);
    }


    @Test
    public void explicitConstructorOpenedForIntegrate() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions = new GetDiffFilesOptions(0, false, false, true, false, false, false, false);
        Valids valids = new Valids();
        valids.openedForIntegrateGet = true;
        valids.openedForIntegrate = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setOpenedForIntegrateFalse() throws Throwable {
	    GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setOpenedForIntegrate(false);
        Valids valids = new Valids();
        testMethod(false, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setOpenedForIntegrateTrue() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setOpenedForIntegrate(true);
        Valids valids = new Valids();
        valids.openedForIntegrateGet = true;
        valids.openedForIntegrate = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void stringConstructorOpenedForIntegrate() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-sb");
        getDiffFilesOptions.setOpenedForIntegrate(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.openedForIntegrate = true;
        valids.openedForIntegrateGet = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableFalseOpenedForIntegrate() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(false);
        getDiffFilesOptions.setOpenedForIntegrate(true);
        Valids valids = new Valids();
        valids.openedForIntegrateGet = true;
        valids.openedForIntegrate = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableTrueOpenedForIntegrate() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(true);
        getDiffFilesOptions.setOpenedForIntegrate(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.openedForIntegrateGet = true;
        valids.openedForIntegrate = true;
        testMethod(false, valids, getDiffFilesOptions);
    }


    @Test
    public void explicitConstructorOpenedSame() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions(0, false, false, false, false, false, false, true);
        Valids valids = new Valids();
        valids.openedSameGet = true;
        valids.openedSame = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setOpenedSameFalse() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setOpenedSame(false);
        Valids valids = new Valids();
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setOpenedSameTrue() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setOpenedSame(true);
        Valids valids = new Valids();
        valids.openedSameGet = true;
        valids.openedSame = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void stringConstructorOpenedSame() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-sr");
        getDiffFilesOptions.setOpenedSame(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.openedSame = true;
        valids.openedSameGet = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableFalseOpenedSame() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(false);
        getDiffFilesOptions.setOpenedSame(true);
        Valids valids = new Valids();
        valids.openedSameGet = true;
        valids.openedSame = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableTrueOpenedSame() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(true);
        getDiffFilesOptions.setOpenedSame(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.openedSameGet = true;
        valids.openedSame = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void explicitConstructorUnopenedDifferent() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions(0, false, false, false, false, true, false, false);
        Valids valids = new Valids();
        valids.unopenedDifferentGet = true;
        valids.unopenedDifferent = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setUnopenedDifferentFalse() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setUnopenedDifferent(false);
        Valids valids = new Valids();
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setUnopenedDifferentTrue() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setUnopenedDifferent(true);
        Valids valids = new Valids();
        valids.unopenedDifferentGet = true;
        valids.unopenedDifferent = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void stringConstructorUnopenedDifferent() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-se");
        getDiffFilesOptions.setUnopenedDifferent(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.unopenedDifferent = true;
        valids.unopenedDifferentGet = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableFalseUnopenedDifferent() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(false);
        getDiffFilesOptions.setUnopenedDifferent(true);
        Valids valids = new Valids();
        valids.unopenedDifferentGet = true;
        valids.unopenedDifferent = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableTrueUnopenedDifferent() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(true);
        getDiffFilesOptions.setUnopenedDifferent(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.unopenedDifferentGet = true;
        valids.unopenedDifferent = true;
        testMethod(false, valids, getDiffFilesOptions);
    }


    @Test
    public void explicitConstructorUnopenedMissing() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions(0, false, false, false, true, false, false, false);
        Valids valids = new Valids();
        valids.unopenedMissingGet = true;
        valids.unopenedMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setUnopenedMissingFalse() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setUnopenedMissing(false);
        Valids valids = new Valids();
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setUnopenedMissingTrue() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setUnopenedMissing(true);
        Valids valids = new Valids();
        valids.unopenedMissingGet = true;
        valids.unopenedMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void stringConstructorUnopenedMissing() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-sd");
        getDiffFilesOptions.setUnopenedMissing(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.unopenedMissing = true;
        valids.unopenedMissingGet = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableFalseUnopenedMissing() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(false);
        getDiffFilesOptions.setUnopenedMissing(true);
        Valids valids = new Valids();
        valids.unopenedMissingGet = true;
        valids.unopenedMissing = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableTrueUnopenedMissing() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(true);
        getDiffFilesOptions.setUnopenedMissing(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.unopenedMissingGet = true;
        valids.unopenedMissing = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void explicitConstructorUnopenedWithStatus() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions(0, false, false, false, false, false, true, false);
        Valids valids = new Valids();
        valids.unopenedWithStatusGet = true;
        valids.unopenedWithStatus = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setUnopenedWithStatusFalse() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setUnopenedWithStatus(false);
        Valids valids = new Valids();
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setUnopenedWithStatusTrue() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setUnopenedWithStatus(true);
        Valids valids = new Valids();
        valids.unopenedWithStatusGet = true;
        valids.unopenedWithStatus = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void stringConstructorUnopenedWithStatus() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-sl");
        getDiffFilesOptions.setUnopenedWithStatus(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.unopenedWithStatus = true;
        valids.unopenedWithStatusGet = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableFalseUnopenedWithStatus() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(false);
        getDiffFilesOptions.setUnopenedWithStatus(true);
        Valids valids = new Valids();
        valids.unopenedWithStatusGet = true;
        valids.unopenedWithStatus = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setImmutableTrueUnopenedWithStatus() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(true);
        getDiffFilesOptions.setUnopenedWithStatus(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.unopenedWithStatusGet = true;
        valids.unopenedWithStatus = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void explicitConstructorMax() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions(1, false, true, false, false, false, false, false);
        Valids valids = new Valids();
        valids.maxGet = 1;
        valids.max = 1;
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setMaxZero() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setMaxFiles(0);
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setMaxOne() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setMaxFiles(1);
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.maxGet = 1;
        valids.max = 1;
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void stringConstructorMaxOne() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-m 1", "-sa");
        getDiffFilesOptions.setMaxFiles(1);
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.max = 1;
        valids.maxGet = 1;
        valids.openedDifferentMissing = true;
        valids.openedDifferentMissingGet = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setImmutableFalseMax() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(false);
        getDiffFilesOptions.setMaxFiles(1);
        Valids valids = new Valids();
        valids.maxGet = 1;
        valids.max = 1;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setImmutableTrueMax() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(true);
        getDiffFilesOptions.setMaxFiles(1);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.maxGet = 1;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void explicitConstructorDiffNonText() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions(0, true, true, false, false, false, false, false);
        Valids valids = new Valids();
        valids.diffNonTextGet = true;
        valids.diffNonText = true;
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setDiffNonTextFalse() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setDiffNonTextFiles(false);
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setDiffNonTextTrue() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setDiffNonTextFiles(true);
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        Valids valids = new Valids();
        valids.diffNonTextGet = true;
        valids.diffNonText = true;
        valids.openedDifferentMissingGet = true;
        valids.openedDifferentMissing = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void stringConstructorDiffNonText() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-t", "-sa");
        getDiffFilesOptions.setOpenedDifferentMissing(true);
        getDiffFilesOptions.setDiffNonTextFiles(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.diffNonText = true;
        valids.diffNonTextGet = true;
        valids.openedDifferentMissing = true;
        valids.openedDifferentMissingGet = true;
        testMethod(true, valids, getDiffFilesOptions);
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setImmutableFalseDiffNonText() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(false);
        getDiffFilesOptions.setDiffNonTextFiles(true);
        Valids valids = new Valids();
        valids.diffNonTextGet = true;
        valids.diffNonText = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    @Ignore
    public void setImmutableTrueDiffNonText() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        getDiffFilesOptions.setImmutable(true);
        Valids valids = new Valids();
        valids.immutable = true;
        testMethod(false, valids, getDiffFilesOptions);
        getDiffFilesOptions.setDiffNonTextFiles(true);
        valids = new Valids();
        valids.immutable = true;
        valids.diffNonTextGet = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @Test
    public void setterReturns() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions();
        assertThat(getDiffFilesOptions.setDiffNonTextFiles(true), instanceOf(GetDiffFilesOptions.class));
        assertThat(getDiffFilesOptions.setMaxFiles(1), instanceOf(GetDiffFilesOptions.class));
        assertThat(getDiffFilesOptions.setOpenedDifferentMissing(true), instanceOf(GetDiffFilesOptions.class));
        assertThat(getDiffFilesOptions.setOpenedForIntegrate(true), instanceOf(GetDiffFilesOptions.class));
        assertThat(getDiffFilesOptions.setOpenedSame(true), instanceOf(GetDiffFilesOptions.class));
        assertThat(getDiffFilesOptions.setUnopenedDifferent(true), instanceOf(GetDiffFilesOptions.class));
        assertThat(getDiffFilesOptions.setUnopenedMissing(true), instanceOf(GetDiffFilesOptions.class));
        assertThat(getDiffFilesOptions.setUnopenedWithStatus(true), instanceOf(GetDiffFilesOptions.class));
    }

    @Test
    public void overrideStringConstructor() throws Throwable {
        GetDiffFilesOptions getDiffFilesOptions = new GetDiffFilesOptions("-m 1", "-t", "-sa");
        getDiffFilesOptions.setMaxFiles(0);
        getDiffFilesOptions.setDiffNonTextFiles(false);
        getDiffFilesOptions.setOpenedDifferentMissing(false);
        getDiffFilesOptions.setOpenedForIntegrate(true);
        Valids valids = new Valids();
        valids.immutable = true;
        valids.max = 1;
        valids.diffNonText = true;
        valids.openedDifferentMissing = true;
        valids.openedForIntegrateGet = true;
        valids.openedForIntegrate = true;
        testMethod(false, valids, getDiffFilesOptions);
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }

    private void testMethod(boolean useOldMethod, Valids valids, GetDiffFilesOptions getDiffFilesOptions) throws Throwable {

        assertThat(getDiffFilesOptions.isImmutable(), is(valids.immutable));
        assertThat(getDiffFilesOptions.getMaxFiles(), is(valids.maxGet));
        assertThat(getDiffFilesOptions.isDiffNonTextFiles(), is(valids.diffNonTextGet));
        assertThat(getDiffFilesOptions.isOpenedDifferentMissing(), is(valids.openedDifferentMissingGet));
        assertThat(getDiffFilesOptions.isOpenedForIntegrate(), is(valids.openedForIntegrateGet));
        assertThat(getDiffFilesOptions.isOpenedSame(), is(valids.openedSameGet));
        assertThat(getDiffFilesOptions.isUnopenedDifferent(), is(valids.unopenedDifferentGet));
        assertThat(getDiffFilesOptions.isUnopenedMissing(), is(valids.unopenedMissingGet));
        assertThat(getDiffFilesOptions.isUnopenedWithStatus(), is(valids.unopenedWithStatusGet));

        List<IFileSpec> diffFileSpecs;
        try {
            if (useOldMethod) {
                diffFileSpecs = client.getDiffFiles(null, getDiffFilesOptions.getMaxFiles(), getDiffFilesOptions.isDiffNonTextFiles(), getDiffFilesOptions.isOpenedDifferentMissing(), getDiffFilesOptions.isOpenedForIntegrate(), getDiffFilesOptions.isUnopenedMissing(), getDiffFilesOptions.isUnopenedDifferent(), getDiffFilesOptions.isUnopenedWithStatus(), getDiffFilesOptions.isOpenedSame());
            } else {
                diffFileSpecs = client.getDiffFiles(null, getDiffFilesOptions);
            }

            ConnectionExceptionWhileImmutable = false;
        } catch (ConnectionException e) {
            if ((getDiffFilesOptions.isOpenedDifferentMissing() ||
                    getDiffFilesOptions.isOpenedForIntegrate() ||
                    getDiffFilesOptions.isOpenedSame() ||
                    getDiffFilesOptions.isUnopenedDifferent() ||
                    getDiffFilesOptions.isUnopenedMissing() ||
                    getDiffFilesOptions.isUnopenedWithStatus()) &&
                    !ConnectionExceptionWhileImmutable) {
                throw e;
            } else {
                ConnectionExceptionWhileImmutable = getDiffFilesOptions.isImmutable();
                return;
            }
        }

        helper.validateFileSpecs(diffFileSpecs);

        Set<File> validFiles = new HashSet<>();

        if (valids.openedDifferentMissing) {

            validFiles.add(openedDifferent);
            validFiles.add(openedDifferent);
            validFiles.add(openedDifferentBinary);
            validFiles.add(openedMissing);
            validFiles.add(openedForIntegResolvedModified);

        }

        if (valids.openedForIntegrate) {

            validFiles.add(openedForIntegResolvedModified);

        }

        if (valids.openedSame) {

            validFiles.add(openedSame);
            validFiles.add(openedForIntegResolvedNotModified);

        }

        if (valids.unopenedDifferent) {

            validFiles.add(unopenedDifferent);

        }

        if (valids.unopenedMissing) {

            validFiles.add(unopenedMissing);

        }

        if (valids.unopenedWithStatus) {

            validFiles.add(unopenedMissing);
            validFiles.add(unopenedDifferent);
            validFiles.add(unopenedSame);
            validFiles.add(integSource);

        }

        assertThat(validFiles.size(), is(diffFileSpecs.size()));
        for (File file : validFiles) {
            boolean fileFoundInFileSpecs = false;
            for (IFileSpec fileSpec : diffFileSpecs) {
                if (file.getAbsolutePath().equals(fileSpec.getClientPathString())) {
                    fileFoundInFileSpecs = true;
                    assertThat(fileSpec.getOpStatus(), is(VALID));
                    if (valids.unopenedWithStatus) {
                        if (file.equals(unopenedDifferent)) {
                            assertThat(fileSpec.getDiffStatus(), is("diff"));
                        }
                        if (file.equals(unopenedMissing)) {
                            assertThat(fileSpec.getDiffStatus(), is("missing"));
                        }

                        if (file.equals(unopenedSame)) {
                            assertThat(fileSpec.getDiffStatus(), is("same"));
                        }
                    }
                }
            }

            if (!fileFoundInFileSpecs) {
                throw new Exception("Valid file '" + file.getAbsolutePath() + "' not found in file specs.");
            }
        }
    }

    //@Ignore
    private static class Valids {
        private boolean immutable = false;
        private boolean diffNonTextGet = false;
        @SuppressWarnings("unused")
        private boolean diffNonText = false;
        private int maxGet = 0;
        @SuppressWarnings("unused")
        private int max = 0;
        private boolean openedDifferentMissingGet = false;
        private boolean openedDifferentMissing = false;
        private boolean openedForIntegrateGet = false;
        private boolean openedForIntegrate = false;
        private boolean openedSameGet = false;
        private boolean openedSame = false;
        private boolean unopenedDifferentGet = false;
        private boolean unopenedDifferent = false;
        private boolean unopenedMissingGet = false;
        private boolean unopenedMissing = false;
        private boolean unopenedWithStatusGet = false;
        private boolean unopenedWithStatus = false;
    }
}