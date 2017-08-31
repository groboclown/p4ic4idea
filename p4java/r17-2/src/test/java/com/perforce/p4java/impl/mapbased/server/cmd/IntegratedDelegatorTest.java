package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.INTEGRATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetSubmittedIntegrationsOptions;

/**
 * Tests p4 integrated - example of output as below.
 * 
 * <pre>
 * p4 -ztag integrated //depot/main/revisions.h 
 * ... toFile //depot/main/revisions.h 
 * ... fromFile //depot/dev/revisions.h 
 * ... startToRev #none 
 * ... endToRev #1 
 * ... startFromRev #none 
 * ... endFromRev #1 
 * ... how add into
 * ... change 12345
 * </pre>
 */
public class IntegratedDelegatorTest extends AbstractP4JavaUnitTest {
    
    /** The integrated delegator. */
    private IntegratedDelegator integratedDelegator;
    
    /** Example values. */
    private static final String FILE_SPEC = "//depot/main/revisions.h";
    
    /** Example values. */
    private static final String END_REV = "#1";
    
    /** Example values. */
    private static final String START_REV = "#none";
    
    /** Example values. */
    private static final String CHANGE = "12345";
    /** Simple matcher. */
    private static final CommandLineArgumentMatcher SIMPLE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { FILE_SPEC });

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        integratedDelegator = new IntegratedDelegator(server);
    }

    /**
     * Test integrated opt connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testIntegratedOptConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        integratedDelegator.getSubmittedIntegrations(specs, new GetSubmittedIntegrationsOptions());
    }

    /**
     * Test integrated connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testIntegratedConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        integratedDelegator.getSubmittedIntegrations(specs, null, false);
    }

    /**
     * Test integrated opt AccessException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testIntegratedOptAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        integratedDelegator.getSubmittedIntegrations(specs, new GetSubmittedIntegrationsOptions());
    }

    /**
     * Test integrated AccessException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testIntegratedAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        integratedDelegator.getSubmittedIntegrations(specs, null, false);
    }

    /**
     * Test integrated opt RequestException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testIntegratedOptRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        integratedDelegator.getSubmittedIntegrations(specs, new GetSubmittedIntegrationsOptions());
    }

    /**
     * Test integrated RequestException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testIntegratedRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        integratedDelegator.getSubmittedIntegrations(specs, null, false);
    }

    /**
     * Test integrated opt P4JavaException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testIntegratedOptP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        integratedDelegator.getSubmittedIntegrations(specs, new GetSubmittedIntegrationsOptions());
    }

    /**
     * Test integrated P4JavaException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testIntegratedP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        integratedDelegator.getSubmittedIntegrations(specs, null, false);
    }

    /**
     * Test integrated opt.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testIntegratedOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        List<IFileSpec> result = integratedDelegator.getSubmittedIntegrations(specs,
                new GetSubmittedIntegrationsOptions());
        verify(server).execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null));
        assertFileSpecs(result);
    }
    
    /**
     * Test integrated.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testIntegrated() throws P4JavaException {
        when(server.execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        List<IFileSpec> result = integratedDelegator.getSubmittedIntegrations(specs, null, false);
        verify(server).execMapCmdList(eq(INTEGRATED.toString()), argThat(SIMPLE_MATCHER), eq(null));
        assertFileSpecs(result);
    }

    /**
     * Builds the valid result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("toFile", FILE_SPEC);
        result.put("fromFile", FILE_SPEC);
        result.put("startToRev", START_REV);
        result.put("endToRev", END_REV);
        result.put("startFromRev", START_REV);
        result.put("change", CHANGE);
        results.add(result);
        return results;
    }

    /**
     * Assert that the file specs built are as expected.
     *
     * @param specs
     *            the specs
     */
    private void assertFileSpecs(final List<IFileSpec> specs) {
        assertNotNull(specs);
        assertEquals(1, specs.size());
        IFileSpec fs = specs.get(0);
        assertNotNull(fs);
        assertEquals(Integer.valueOf(CHANGE).intValue(), fs.getChangelistId());
        assertEquals(FILE_SPEC, fs.getFromFile());
        assertEquals(FILE_SPEC, fs.getToFile());
        assertEquals(Integer.valueOf(END_REV.replace("#", "")).intValue(), fs.getEndToRev());
        // TODO Surely this should be NONE?
        // https://jira.perforce.com:8443/browse/P4JAVA-1090
        assertEquals(FileSpec.NO_FILE_REVISION, fs.getStartToRev());
        assertEquals(FileSpec.NO_FILE_REVISION, fs.getStartFromRev());
    }
}