package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.core.file.FileSpecOpStatus.ERROR;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.impl.generic.core.file.FilePath.PathType.DEPOT;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.ATTRIBUTE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.OneShotServerImpl;
import com.perforce.p4java.option.server.SetFileAttributesOptions;
import com.perforce.p4java.tests.UnitTestGiven;

/**
 * @author Sean Shou
 * @since 22/09/2016
 */
public class AttributeDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String STREAM_NAME = "testStream";

    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private AttributeDelegator attributeDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private List<IFileSpec> fileSpecs;
    private IFileSpec mockFileSpec;
    private SetFileAttributesOptions mockOpts;
    private InputStream mockInputStream;
    //private AttributeDelegator.AttributeDelegatorHidden attributeDelegatorHidden = new AttributeDelegator.AttributeDelegatorHidden();
    private Method method = getPrivateMethod(
            AttributeDelegator.class,
            "buildSetFileAttributesFileSpecsFromCommandResultMaps", List.class, Function.class);

    private Function<Map<String, Object>, IFileSpec> mockHandle;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(OneShotServerImpl.class);
        attributeDelegator = new AttributeDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = Lists.newArrayList(resultMap);

        fileSpecs = Lists.newArrayList();
        mockFileSpec = mock(IFileSpec.class);
        fileSpecs.add(mockFileSpec);

        mockOpts = mock(SetFileAttributesOptions.class);
        mockInputStream = mock(InputStream.class);

        mockHandle = mock(Function.class);
    }

    @Test
    public void buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnEmptyListWhenFileSpecIsBlankAnnotatedPathString()
            throws InvocationTargetException, IllegalAccessException, P4JavaException {
        // given
        buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnEmptyList(() -> {
            when(mockHandle.apply(resultMap)).thenReturn(mockFileSpec);
            when(mockFileSpec.getOpStatus()).thenReturn(VALID);
            when(mockFileSpec.getAnnotatedPathString(DEPOT)).thenReturn(EMPTY);
        });
    }

    @Test
    public void buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnEmptyListWhenResultMapsIsEmpty()
            throws InvocationTargetException, IllegalAccessException, P4JavaException {
        // given
        buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnEmptyList(
                () -> resultMaps = Lists.newArrayList());
    }

    @Test
    public void buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnEmptyListWhenResultMapsIsNull()
            throws InvocationTargetException, IllegalAccessException, P4JavaException {
        // given
        buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnEmptyList(
                () -> resultMaps = null);
    }

    @SuppressWarnings("unchecked")
    private void buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnEmptyList(
            UnitTestGiven unitTestGiven)
            throws P4JavaException, InvocationTargetException, IllegalAccessException {
        unitTestGiven.given();
        List<IFileSpec> resultList = (List<IFileSpec>) method.invoke(attributeDelegator,
                resultMaps, mockHandle);
        assertThat(resultList.size(), is(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnNonEmptyListWhenStatusIsNotValid()
            throws InvocationTargetException, IllegalAccessException {
        when(mockHandle.apply(resultMap)).thenReturn(mockFileSpec);
        when(mockFileSpec.getOpStatus()).thenReturn(ERROR);

        List<IFileSpec> resultList = (List<IFileSpec>) method.invoke(attributeDelegator,
                resultMaps, mockHandle);
        assertThat(resultList.size(), is(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldReturnNonEmptyListWhenStatusIsValid()
            throws InvocationTargetException, IllegalAccessException {
        when(mockHandle.apply(resultMap)).thenReturn(mockFileSpec);
        when(mockFileSpec.getOpStatus()).thenReturn(VALID);
        when(mockFileSpec.getAnnotatedPathString(DEPOT)).thenReturn("not blank");

        List<IFileSpec> resultList = (List<IFileSpec>) method.invoke(attributeDelegator,
                resultMaps, mockHandle);
        assertThat(resultList.size(), is(1));
    }

    @Test
    public void buildSetFileAttributesFileSpecsFromCommandResultMaps_shouldThrownExceptionWhenInnerCallThrown()
            throws InvocationTargetException, IllegalAccessException {
        //then
        thrown.expect(InvocationTargetException.class);
        // given
        doThrow(ConnectionException.class).when(mockHandle).apply(any());
        method.invoke(attributeDelegator, resultMaps, mockHandle);
    }

    /**
     * Test that a resultmap with no errors leads to a single filespec being returned.
     *
     * @throws P4JavaException
     */
    @Test
    public void testNormalBehaviour() throws P4JavaException {
        //given
        givenInfoMessageCode();
        when(server.execMapCmdList(eq(ATTRIBUTE.toString()), any(String[].class), eq(null)))
                .thenReturn(resultMaps);
        // when
        List<IFileSpec> actualFileSpecs = attributeDelegator.setFileAttributes(
                fileSpecs,
                createTestAttributes(), mockOpts);

        // then
        assertThat(actualFileSpecs.size(), is(1));
        assertThat(actualFileSpecs.get(0).getStatusMessage(), is(STREAM_NAME));
    }

    private void givenInfoMessageCode() {
        when(resultMap.get(FMT0)).thenReturn("%infoMessage%");
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get("infoMessage")).thenReturn(STREAM_NAME);
    }

    /**
     * Test that when a null attribute value is provided an npe is thrown by the
     * delegator.
     *
     * @throws P4JavaException
     */
    @Test
    public void testNullAttribute() throws P4JavaException {
        // then
        thrown.expect(NullPointerException.class);

        //when
        attributeDelegator.setFileAttributes(fileSpecs, null, mockOpts);
    }

    /**
     * Test that a null pointer exception is thrown when a null value is passed instead of a stream.
     *
     * @throws P4JavaException
     */
    @Test
    public void testNullStream()
            throws P4JavaException {
        expectThrows(NullPointerException.class,
                () -> attributeDelegator.setFileAttributes(fileSpecs, "owner", null, mockOpts));
    }

    /**
     * Test that an access exception is passed back up the stack,
     *
     * @throws P4JavaException
     */
    @Test
    public void testP4ExceptionPropagation() throws P4JavaException {
        // then
        thrown.expect(AccessException.class);

        // given
        when(server.execMapCmdList(eq(ATTRIBUTE.toString()), any(String[].class), eq(null)))
                .thenThrow(AccessException.class);
        // when
        attributeDelegator.setFileAttributes(fileSpecs, createTestAttributes(), mockOpts);
    }

    private Map<String, String> createTestAttributes() {
        Map<String, String> attributes = Maps.newHashMap();
        attributes.put("owner", "seans");
        attributes.put("modifyTime", "2016-09-22 17:00");
        attributes.put("readOnly", "true");
        attributes.put("group", "");

        return attributes;
    }

    /**
     * Test that an illegal argument exception is thrown when an empty attribute name is provided.
     *
     * @throws P4JavaException
     */
    @Test
    public void testStreamEmptyAttributeName() throws P4JavaException {
        // then
        thrown.expect(IllegalArgumentException.class);

        // when
        attributeDelegator.setFileAttributes(fileSpecs, EMPTY, mockInputStream, mockOpts);
    }

    /**
     * Test that
     *
     * @throws P4JavaException
     */
    @Test
    public void testStreamNormalBehaviour() throws P4JavaException {
        // given
        givenInfoMessageCode();
        when(server.execMapCmdList(eq(ATTRIBUTE.toString()), any(String[].class), any(Map.class)))
                .thenReturn(resultMaps);

        // when
        List<IFileSpec> actualFileSpecs = attributeDelegator.setFileAttributes(fileSpecs, "owner",
                mockInputStream, mockOpts);
        // then
        assertThat(actualFileSpecs.size(), is(1));
        assertThat(actualFileSpecs.get(0).getStatusMessage(), is(STREAM_NAME));
    }

    /**
     * Test that when a server call, invoked via the input stream variant of the api, throws an
     * AccessException, then it is propagated through the delegate.
     *
     * @throws P4JavaException
     */
    @Test
    public void testStreamP4JavaPropagation() throws P4JavaException {
        // then
        thrown.expect(AccessException.class);
        // given
        when(server.execMapCmdList(eq(ATTRIBUTE.toString()), any(String[].class), any(Map.class)))
                .thenThrow(AccessException.class);
        // when
        attributeDelegator.setFileAttributes(fileSpecs, "owner", mockInputStream, mockOpts);
    }
}