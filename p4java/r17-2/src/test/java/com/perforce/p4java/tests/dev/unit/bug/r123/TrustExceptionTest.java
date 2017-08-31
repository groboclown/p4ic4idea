package com.perforce.p4java.tests.dev.unit.bug.r123;

import static com.perforce.p4java.PropertyDefs.TRUST_PATH_KEY_SHORT_FORM;
import static com.perforce.p4java.exception.TrustException.Type.NEW_CONNECTION;
import static com.perforce.p4java.exception.TrustException.Type.NEW_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test fingerprint and 'p4 trust' exception message.
 * <p>
 * Sean comments:
 * <pre>
 * download jce "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files"
 * from oracle web site. Then uncompress & replace your
 * ${JAVA_HOME}/jre/lib/security/local_policy.jar
 * $ {JAVA_HOME}/jre/lib/security/US_export_policy.jar
 * with file from new jce package.
 * </pre>
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job056729"})
@TestId("Dev121_TrustExceptionTest")
public class TrustExceptionTest extends P4JavaTestCase {

    @Before
    public void beforeEach() throws Exception {
        initialP4JavaTestCase();
    }
    /**
     * Test add trust
     */
    @Test
    public void testAddTrust() throws Exception {

        String result;
        String serverUri = "p4javassl://eng-p4java-vm.perforce.com:30121";
        String trustFilePath = System.getProperty("user.dir") + "/" + ".testP4trust_" + System.currentTimeMillis();
        props.setProperty(TRUST_PATH_KEY_SHORT_FORM, trustFilePath);
        server = ServerFactory.getOptionsServer(serverUri, props);
        assertThat(server, notNullValue());

        try {
            // Register callback
            server.registerCallback(createCommandCallback());

            // Run remove trust first
            result = server.removeTrust();
            assertThat(result, notNullValue());

            // Should get 'new connection' trust exception
            try {
                // Connect to the server.
                server.connect();
                fail("should get 'new connection' trust exception");
            } catch (P4JavaException e) {
                assertThat(e, notNullValue());
                assertThat(e.getCause(), instanceOf(TrustException.class));
                TrustException trustException = (TrustException) e.getCause();
                assertThat(trustException.getType() == NEW_CONNECTION, is(true));
                String fingerprint = trustException.getFingerprint();
                assertThat(fingerprint, notNullValue());

                // Add the key (new connection)
                try {
                    result = server.addTrust(fingerprint);
                    assertThat(result, notNullValue());
                    assertThat(result, containsString("not known"));
                    assertThat(result, containsString("Added trust for Perforce server"));
                } catch (P4JavaException e2) {
                    assertThat(e2, notNullValue());
                }
            }

            // Add the key again
            result = server.addTrust(new TrustOptions());
            assertThat(result, notNullValue());
            assertThat(result, is("Trust already established."));

            // Add a specific fake fingerprint
            result = server.addTrust("B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2");
            assertThat(result, notNullValue());
            assertThat(result, containsString("Added trust for Perforce server"));

            // Add trust WITHOUT 'force' and 'autoAccept' options
            // Should get 'new key' exception
            try {
                server.addTrust(new TrustOptions());
                fail("Should get 'new key' exception as Add trust WITHOUT 'force' and 'autoAccept' options");
            } catch (P4JavaException e) {
                assertThat(e, notNullValue());
                assertThat(e, instanceOf(TrustException.class));
                assertThat(((TrustException) e).getType(), is(NEW_KEY));
                String message = e.getMessage();

                assertThat(message, containsString("Can't trust mismatched Perforce server key without both 'force' and 'autoAccept' options."));
                assertThat(message, containsString("IDENTIFICATION HAS CHANGED"));
            }

            // Add trust WITH 'autoAccept' and WITHOUT 'force'
            try {
                server.addTrust(new TrustOptions().setAutoAccept(true));
                fail("should failed as Add trust WITH 'autoAccept' and WITHOUT 'force'");
            } catch (P4JavaException e) {
                assertThat(e, notNullValue());
                assertThat(e.getMessage(), containsString("Can't trust mismatched Perforce server key without both 'force' and 'autoAccept' options."));
            }

            // Register callback
            //server.registerCallback(createCommandCallback());

            // Run remove trust first
            result = server.removeTrust();

            assertNotNull(result);

            // Should get 'new connection' trust exception
            try {
                // Connect to the server.
                server.connect();
                fail("Should get 'new connection' trust exception");
            } catch (P4JavaException e) {
                assertNotNull(e);
                StringWriter stringWriter = new StringWriter(100);
                e.printStackTrace(new PrintWriter(stringWriter));

                assertThat(stringWriter.toString(), e.getCause(), instanceOf(TrustException.class));
                assertThat(((TrustException) e.getCause()).getType(), is(NEW_CONNECTION));
                assertNotNull(((TrustException) e.getCause()).getFingerprint());

                // Add the key (new connection)
                result = server.addTrust(((TrustException) e.getCause()).getFingerprint());
                assertNotNull(result);
                assertThat(result, containsString("not known"));
                assertThat(result, containsString("Added trust for Perforce server"));
            }

            // Add the key again
            result = server.addTrust(new TrustOptions());
            assertNotNull(result);
            assertEquals(result, "Trust already established.");


            // Add a specific fake fingerprint
            result = server.addTrust("B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2");
            assertNotNull(result);
            assertThat(result, containsString("Added trust for Perforce server"));

            // Add trust WITHOUT 'force' and 'autoAccept' options
            // Should get 'new key' exception
            try {
                server.addTrust(new TrustOptions());
                fail("Should get 'new key' exception as Add trust WITHOUT 'force' and 'autoAccept' options");
            } catch (P4JavaException e) {
                assertNotNull(e);
                assertThat(e, instanceOf(TrustException.class));
                assertThat(((TrustException) e).getType(), is(NEW_KEY));
                assertThat(e.getMessage(), containsString("Can't trust mismatched Perforce server key without both 'force' and 'autoAccept' options."));
                assertThat(e.getMessage(), containsString("IDENTIFICATION HAS CHANGED"));
            }

            // Add trust WITH 'autoAccept' and WITHOUT 'force'
            try {
                server.addTrust(new TrustOptions().setAutoAccept(true));
                fail("should get exception as Add trust WITH 'autoAccept' and WITHOUT 'force'");
            } catch (P4JavaException e) {
                assertNotNull(e);
                assertThat(e.getMessage(), containsString("Can't trust mismatched Perforce server key without both 'force' and 'autoAccept' options."));
            }

            // Add trust WITH 'force' and 'autoAccept' optionstry {
            result = server.addTrust(new TrustOptions()
                    .setForce(true).setAutoAccept(true));
            assertNotNull(result);
            assertThat(result, containsString("Added trust for Perforce server"));
        } finally {
            try {
                ISystemFileCommandsHelper helper = ServerFactory.getRpcFileSystemHelper();
                helper.setWritable(trustFilePath, true);
                Files.deleteIfExists(Paths.get(trustFilePath));
                Files.deleteIfExists(Paths.get(trustFilePath + ".lck"));
            } catch (Exception ignore) {
            }
        }
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
