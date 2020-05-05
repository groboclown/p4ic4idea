package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.CLIENT;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.common.function.BiPredicate;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.DeleteClientOptions;
import com.perforce.p4java.option.server.GetClientTemplateOptions;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.option.server.UpdateClientOptions;
import com.perforce.p4java.server.IOptionsServer;

public class ClientDelegatorTest extends AbstractP4JavaUnitTest {
    private String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private String MESSAGE_CODE_NOT_IN_INFO_RANGE = "168435456";
    private ClientDelegator clientDelegator;
    private String clientName = "myClient";
    private IClient mockClient;
    private IClientSummary clientSummary;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private Method getClientOrNullFromHelixResultMap = getPrivateMethod(ClientDelegator.class,
            "getClientOrNullFromHelixResultMap", List.class, GetClientTemplateOptions.class,
            IOptionsServer.class, BiPredicate.class, Function.class);
    @SuppressWarnings("unchecked")
    private Function<Map<String, Object>, Boolean> handle = mock(Function.class);
    private SwitchClientViewOptions mockSwitchClientViewOptions;

    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        clientDelegator = spy(new ClientDelegator(server));
        resultMap = mock(Map.class);
        resultMaps = new ArrayList<>();
        resultMaps.add(resultMap);
        mockClient = mock(IClient.class);
        when(mockClient.getName()).thenReturn(clientName);
        clientSummary = mock(IClientSummary.class);
        when(clientSummary.getName()).thenReturn(clientName);

        mockSwitchClientViewOptions = mock(SwitchClientViewOptions.class);
    }

    @Test(expected = NullPointerException.class)
    public void getClient_shouldThrowNullPointerExceptionWhenClientNameIsNull() throws Exception {
        // given
        clientName = null;
        // then
        clientDelegator.getClient(clientName);
    }

    @Test(expected = AccessException.class)
    public void getClient_shouldThrowAccessExceptionThatWasThrownFromGetClientOrNullFromHelixResultMap()
            throws Exception {
        doThrow(AccessException.class).when(server).execMapCmdList(eq(CLIENT.toString()),
                eq(new String[] { "-o", clientName }), eq((Map<String, Object>) null));
        clientDelegator.getClient(clientName);
    }

    @Test
    public void getClient_shouldReturnNonNullIClientWithGivenName() throws Exception {
        // given
        doReturn(resultMaps).when(server).execMapCmdList(eq(CLIENT.toString()),
                eq(new String[] { "-o", clientName }), eq((Map<String, Object>) null));
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_NOT_IN_INFO_RANGE);
        when(resultMap.containsKey(MapKeys.UPDATE_KEY)).thenReturn(true);

        // when
        IClient client = clientDelegator.getClient(clientName);

        // then
        assertNotNull(client);
    }

    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNullWhenResultMapsIsNull()
            throws Exception {
        // when
        IClient client = (IClient) getClientOrNullFromHelixResultMap.invoke(clientDelegator, null,
                mock(GetClientTemplateOptions.class), server, mock(BiPredicate.class),
                mock(Function.class));

        // then
        assertNull(client);
    }

    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNullWhenResultMapsIsEmpty()
            throws Exception {
        // given
        resultMaps = new ArrayList<Map<String, Object>>();

        // when
        IClient client = (IClient) getClientOrNullFromHelixResultMap.invoke(clientDelegator,
                resultMaps, mock(GetClientTemplateOptions.class), server, mock(BiPredicate.class),
                mock(Function.class));

        // then
        assertNull(client);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNullBecauseAllResultMapIsInfoMessage()
            throws Exception {
        // given
        when(handle.apply((Map<String, Object>) any())).thenReturn(true);

        // when
        Object client = getClientOrNullFromHelixResultMap.invoke(clientDelegator, resultMaps,
                mock(GetClientTemplateOptions.class), server, mock(BiPredicate.class), handle);

        // then
        assertNull(client);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNullBecauseAllResultMapPredicateTestFailed()
            throws Exception {
        // given
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        @SuppressWarnings("rawtypes")
        BiPredicate conditions = mock(BiPredicate.class);
        when(conditions.test(any(), any())).thenReturn(false);
        when(handle.apply((Map<String, Object>) any())).thenReturn(true);
        // when
        IClient client = (IClient) getClientOrNullFromHelixResultMap.invoke(clientDelegator,
                resultMaps, mock(GetClientTemplateOptions.class), server, conditions, handle);

        // then
        assertNull(client);
    }

    @Test(expected = InvocationTargetException.class)
    public void getClientOrNullFromHelixResultMap_shouldThrowAccessExceptionThatWasThrownFromHandleErrorStr()
            throws Exception {
        // given
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);

        // then
        getClientOrNullFromHelixResultMap.invoke(clientDelegator, resultMaps,
                mock(GetClientTemplateOptions.class), server, mock(BiPredicate.class),
                mock(Function.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNonNullClient() throws Exception {
        // given
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        @SuppressWarnings("rawtypes")
        BiPredicate conditions = mock(BiPredicate.class);
        when(conditions.test(any(), any())).thenReturn(true);
        when(handle.apply((Map<String, Object>) any())).thenReturn(true);

        // when
        IClient client = (IClient) getClientOrNullFromHelixResultMap.invoke(clientDelegator,
                resultMaps, mock(GetClientTemplateOptions.class), server, conditions, handle);

        // then
        assertNotNull(client);
    }

    @Test(expected = NullPointerException.class)
    public void getClient_ByClientSummary_shouldReturnNullPointerExceptionWhenClientSummaryIsNull()
            throws Exception {
        // given
        clientSummary = null;

        // then
        clientDelegator.getClient(clientSummary);
    }

    @Test
    public void getClient_ByClientSummary_shouldReturnNonNullClient() throws Exception {
        // given
        doReturn(mockClient).when(clientDelegator).getClient(any(String.class));
        // when
        IClient client = clientDelegator.getClient(clientSummary);

        // then
        assertThat(client, is(mockClient));
        verify(clientSummary).getName();
    }

    @Test(expected = ConnectionException.class)
    public void getClientTemplate_shouldReturnConnectionExceptionThatWasThrownFromInnerGetClientTemplate()
            throws Exception {
        // given
        doThrow(ConnectionException.class).when(clientDelegator).getClientTemplate(eq(clientName),
                eq(false));

        // then
        clientDelegator.getClientTemplate(clientName);
    }

    @Test
    public void getClientTemplate_shouldReturnNonNullClient() throws Exception {
        // given
        doReturn(mockClient).when(clientDelegator).getClientTemplate(eq(clientName), eq(false));

        // when
        IClient client = clientDelegator.getClientTemplate(clientName);

        // then
        assertThat(client, is(mockClient));
    }

    @Test(expected = ConnectionException.class)
    public void getClientTemplate_ByNameAndAllowExistent_shouldThrowConnectionExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        doThrow(ConnectionException.class).when(clientDelegator)
                .getClientTemplate(any(String.class), any(GetClientTemplateOptions.class));

        clientDelegator.getClientTemplate(clientName, false);
    }

    @Test(expected = AccessException.class)
    public void getClientTemplate_ByNameAndAllowExistent_shouldThrowAccessExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        doThrow(AccessException.class).when(clientDelegator).getClientTemplate(any(String.class),
                any(GetClientTemplateOptions.class));

        clientDelegator.getClientTemplate(clientName, false);
    }

    @Test(expected = RequestException.class)
    public void getClientTemplate_ByNameAndAllowExistent_shouldThrowRequestExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        doThrow(RequestException.class).when(clientDelegator).getClientTemplate(any(String.class),
                any(GetClientTemplateOptions.class));

        clientDelegator.getClientTemplate(clientName, false);
    }

    @Test(expected = RequestException.class)
    public void getClientTemplate_ByNameAndAllowExistent_shouldThrowRequestExceptionWhenInnerMethodCallThrownP4JavaException()
            throws Exception {
        doThrow(P4JavaException.class).when(clientDelegator).getClientTemplate(any(String.class),
                any(GetClientTemplateOptions.class));

        clientDelegator.getClientTemplate(clientName, false);
    }

    @Test
    public void getClientTemplate_ByNameAndAllowExistent_shouldReturnNonNullClient()
            throws Exception {
        // given
        doReturn(mockClient).when(clientDelegator).getClientTemplate(eq(clientName),
                any(GetClientTemplateOptions.class));

        // when
        IClient client = clientDelegator.getClientTemplate(clientName, true);

        // then
        assertThat(client, is(mockClient));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getClientTemplate_byNameAndGetClientTemplateOptions_shouldThrowIllegalArgumentExceptionWhenClientNameIsBlank()
            throws P4JavaException {
        // then
        clientDelegator.getClientTemplate(EMPTY, mock(GetClientTemplateOptions.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getClientTemplate_byNameAndGetClientTemplateOptions_shouldReturnNonNullClient()
            throws P4JavaException {
        // given
        doReturn(resultMaps).when(server).execMapCmdList(eq(CLIENT.toString()), any(String[].class),
                (Map<String, Object>) any());
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_NOT_IN_INFO_RANGE);
        when(resultMap.containsKey(MapKeys.UPDATE_KEY)).thenReturn(false);
        when(resultMap.containsKey(MapKeys.ACCESS_KEY)).thenReturn(false);

        // when
        IClient client = clientDelegator.getClientTemplate(clientName,
                mock(GetClientTemplateOptions.class));

        // then
        assertNotNull(client);
    }

    @Test(expected = NullPointerException.class)
    public void createClient_shouldThrowNullPointExceptionWhenClientIsNull() throws Exception {
        // then
        clientDelegator.createClient(null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ConnectionException.class)
    public void createClient_shouldThrowConnectionExceptionThatWasThrownFromParseCommandResultMapIfIsInfoMessageAsString()
            throws Exception {
        // given
        doThrow(ConnectionException.class).when(server).execMapCmdList(eq(CLIENT.toString()),
                eq(new String[] { "-i" }), any(Map.class));

        // then
        clientDelegator.createClient(mockClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createClient_shouldReturnClientName() throws Exception {
        // given
        when(server.execMapCmdList(eq(CLIENT.toString()), eq(new String[] { "-i" }),
                any(Map.class))).thenReturn(resultMaps);
        givenInfoMessageResultMap();

        // when
        String actualClientName = clientDelegator.createClient(mockClient);
        assertThat(actualClientName, is(clientName));
    }

    private void givenInfoMessageResultMap() {
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get(FMT0)).thenReturn(clientName);
    }

    @Test(expected = NullPointerException.class)
    public void updateClient_shouldThrownNullPointerExceptionWhenClientIsNull() throws Exception {
        // then
        clientDelegator.updateClient(null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ConnectionException.class)
    public void updateClient_shouldThrowConnectionExceptionThatWasThrownFromParseCommandResultMapIfIsInfoMessageAsString()
            throws Exception {
        // given
        doThrow(ConnectionException.class).when(server).execMapCmdList(eq(CLIENT.toString()),
                eq(new String[] { "-i" }), any(Map.class));

        // then
        clientDelegator.updateClient(mockClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateClient_shouldReturnNonNullClient() throws Exception {
        // given
        when(server.execMapCmdList(eq(CLIENT.toString()), eq(new String[] { "-i" }),
                any(Map.class))).thenReturn(resultMaps);
        givenInfoMessageResultMap();
        // when
        String actualClientName = clientDelegator.updateClient(mockClient);

        // then
        assertThat(actualClientName, is(clientName));
    }

    @Test(expected = ConnectionException.class)
    public void updateClient_byClientAndForce_shouldThrowConnectionExceptionThatWasThrownFromInnerUpdateClientCall()
            throws Exception {
        doThrow(ConnectionException.class).when(clientDelegator).updateClient(any(IClient.class),
                any(UpdateClientOptions.class));
        clientDelegator.updateClient(mockClient, false);
    }

    @Test(expected = RequestException.class)
    public void updateClient_byClientAndForce_shouldThrowRequestExceptionThatWasThrownFromInnerUpdateClientCall()
            throws Exception {
        doThrow(RequestException.class).when(clientDelegator).updateClient(any(IClient.class),
                any(UpdateClientOptions.class));
        clientDelegator.updateClient(mockClient, false);
    }

    @Test(expected = AccessException.class)
    public void updateClient_byClientAndForce_shouldThrowAccessExceptionThatWasThrownFromInnerUpdateClientCall()
            throws Exception {
        doThrow(AccessException.class).when(clientDelegator).updateClient(any(IClient.class),
                any(UpdateClientOptions.class));
        clientDelegator.updateClient(mockClient, false);
    }

    @Test(expected = RequestException.class)
    public void updateClient_byClientAndForce_shouldThrowRequestExceptionWhenP4JavaExceptionWasThrowned()
            throws Exception {
        doThrow(P4JavaException.class).when(clientDelegator).updateClient(any(IClient.class),
                any(UpdateClientOptions.class));
        clientDelegator.updateClient(mockClient, false);
    }

    @Test
    public void updateClient_byClientAndForce_shouldReturnNonNullClient() throws Exception {
        // given
        doReturn(clientName).when(clientDelegator).updateClient(any(IClient.class),
                any(UpdateClientOptions.class));

        // when
        String actualClientName = clientDelegator.updateClient(mockClient, false);
        assertThat(actualClientName, is(clientName));
    }

    @Test(expected = NullPointerException.class)
    public void updateClient_byClientAndUpdateClientOptions_shouldThrownNullPointerExceptionWhenClientIsNull()
            throws P4JavaException {
        // then
        clientDelegator.updateClient(null, mock(UpdateClientOptions.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateClient_byClientAndUpdateClientOptions_shouldThrownIllegalArgumentExceptionWhenClientNameIsBlank()
            throws P4JavaException {
        // given
        when(mockClient.getName()).thenReturn(EMPTY);

        // then
        clientDelegator.updateClient(mockClient, mock(UpdateClientOptions.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateClient_byClientAndUpdateClientOptions_shouldReturnNonNullClientName()
            throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(CLIENT.toString()), any(String[].class), any(Map.class)))
                .thenReturn(resultMaps);
        givenInfoMessageResultMap();
        // when
        String actualClientName = clientDelegator.updateClient(mockClient,
                mock(UpdateClientOptions.class));

        // then
        assertThat(actualClientName, is(clientName));
    }

    @Test(expected = ConnectionException.class)
    public void deleteClient_byClientNameAndForce_shouldThrownConnectionExceptionThatWasThrownInnerDeleteClient()
            throws Exception {
        doThrow(ConnectionException.class).when(clientDelegator).deleteClient(any(String.class),
                any(DeleteClientOptions.class));
        clientDelegator.deleteClient(clientName, false);
    }

    @Test(expected = AccessException.class)
    public void deleteClient_byClientNameAndForce_shouldThrownAccessExceptionThatWasThrownInnerDeleteClient()
            throws Exception {
        doThrow(AccessException.class).when(clientDelegator).deleteClient(any(String.class),
                any(DeleteClientOptions.class));
        clientDelegator.deleteClient(clientName, false);
    }

    @Test(expected = RequestException.class)
    public void deleteClient_byClientNameAndForce_shouldThrownRequestExceptionThatWasThrownInnerDeleteClient()
            throws Exception {
        doThrow(RequestException.class).when(clientDelegator).deleteClient(any(String.class),
                any(DeleteClientOptions.class));
        clientDelegator.deleteClient(clientName, false);
    }

    @Test(expected = RequestException.class)
    public void deleteClient_byClientNameAndForce_shouldThrownRequestExceptionWhenP4JavaExceptionWasThrown()
            throws Exception {
        doThrow(P4JavaException.class).when(clientDelegator).deleteClient(any(String.class),
                any(DeleteClientOptions.class));
        clientDelegator.deleteClient(clientName, false);
    }

    @Test
    public void deleteClient_byClientNameAndForce_shouldReturnDeletedClientName() throws Exception {
        // given
        doReturn(clientName).when(clientDelegator).deleteClient(any(String.class),
                any(DeleteClientOptions.class));
        // when
        String actualClientName = clientDelegator.deleteClient(clientName, false);
        // then
        assertThat(actualClientName, is(clientName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClient_byClientNameAndDeleteClientOptions_shouldThrownIllegalArgumentExceptionWhenClientNameIsBlank()
            throws Exception {
        // then
        clientDelegator.deleteClient(EMPTY, mock(DeleteClientOptions.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteClient_byClientNameAndDeleteClientOptions_shouldReturnNonNullDeletedClientName()
            throws Exception {
        // given
        when(server.execMapCmdList(eq(CLIENT.toString()), any(String[].class),
                (Map<String, Object>) any())).thenReturn(resultMaps);
        givenInfoMessageResultMap();

        // when
        String actualClientName = clientDelegator.deleteClient(clientName,
                mock(DeleteClientOptions.class));

        // then
        assertThat(actualClientName, is(clientName));
    }

    @Test
    public void replaceWithUnderscoreIfClientNameContainsWhitespacesOrTabs() throws Exception {
        // given
        Method method = getPrivateMethod(ClientDelegator.class,
                "replaceWithUnderscoreIfClientNameContainsWhitespacesOrTabs", IClient.class);

        String clientNameWithWhitespace = "my client\t\tname";
        String expectNewClientName = "my_client__name";
        mockClient = mock(IClient.class);
        when(mockClient.getName()).thenReturn(clientNameWithWhitespace);

        // when
        method.invoke(clientDelegator, mockClient);
        // then
        verify(mockClient).setName(expectNewClientName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void switchClientView_byTemplateNameTargetNameAndSwitchClientViewOptions_shouldThrownIllegalArgumentExceptionWhenTemplateClientNameIsBlank()
            throws Exception {
        // then
        clientDelegator.switchClientView(EMPTY, "myTargetClient", mockSwitchClientViewOptions);
    }

    @Test
    public void switchClientView_byTemplateNameTargetNameAndSwitchClientViewOptions_shouldReturnNonBlankSwitchedClientViewName()
            throws Exception {
        // given
        when(server.execMapCmdList(eq(CLIENT.toString()), any(String[].class),
                eq((Map<String, Object>) null))).thenReturn(resultMaps);
        givenInfoMessageResultMap();

        // when
        String switchClientView = clientDelegator.switchClientView("clientName", "myTargetClient",
                mockSwitchClientViewOptions);
        // then
        assertThat(switchClientView, is(clientName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void switchStreamView_byStreamPathTargetNameAndSwitchClientViewOptions_shouldThrownIllegalArgumentExceptionWhenStreamPathIsBlank()
            throws Exception {
        clientDelegator.switchStreamView(EMPTY, "myTargetClient", mockSwitchClientViewOptions);
    }

    @Test
    public void switchStreamView_byStreamPathTargetNameAndSwitchClientViewOptions_shouldReturnSwitchedStreamViewName()
            throws Exception {
        // given
        when(server.execMapCmdList(eq(CLIENT.toString()), any(String[].class),
                eq((Map<String, Object>) null))).thenReturn(resultMaps);
        givenInfoMessageResultMap();

        // when
        String switchStreamView = clientDelegator.switchStreamView("streamPath", "myTargetClient",
                mockSwitchClientViewOptions);
        // then
        assertThat(switchStreamView, is(clientName));
    }
}