package com.perforce.p4java.impl.mapbased.server.cmd;

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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class ClientDelegatorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private String MESSAGE_CODE_NOT_IN_INFO_RANGE = "168435456";
    private ClientDelegator clientDelegator;
    private String clientName = "myClient";
    private IClient mockClient;
    private IClientSummary clientSummary;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private Function<Map<String, Object>, Boolean> handle = mock(Function.class);
    private SwitchClientViewOptions mockSwitchClientViewOptions;
    private IOptionsServer server;

    @Before
    public void beforeEach() {
        server = mock(Server.class);
        clientDelegator = spy(new ClientDelegator(server));
        resultMap = mock(Map.class);
        resultMaps = List.of(resultMap);
        mockClient = mock(IClient.class);
        when(mockClient.getName()).thenReturn(clientName);
        clientSummary = mock(IClientSummary.class);
        when(clientSummary.getName()).thenReturn(clientName);

        mockSwitchClientViewOptions = mock(SwitchClientViewOptions.class);
    }

    @Test
    public void getClient_shouldThrowNullPointerExceptionWhenClientNameIsNull() throws Exception {
        //given
        clientName = null;
        //then
        thrown.expect(NullPointerException.class);
        clientDelegator.getClient(clientName);
    }

    @Test
    public void getClient_shouldThrowAccessExceptionThatWasThrownFromGetClientOrNullFromHelixResultMap() throws Exception {
        // p4ic4idea: use a public, non-abstract class with default constructor
        getClient_Exceptions(AccessException.AccessExceptionForTests.class, AccessException.class);
    }

    private void getClient_Exceptions(Class<? extends P4JavaException> thrownException, Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        thrown.expect(expectedThrows);
        doThrow(thrownException)
                .when(server)
                .execMapCmdList(
                        eq(CLIENT.toString()),
                        eq(new String[]{"-o", clientName}),
                        eq(null));
        clientDelegator.getClient(clientName);
    }

    @Test
    public void getClient_shouldReturnNonNullIClientWithGivenName() throws Exception {
        //given
        doReturn(resultMaps).when(server)
                .execMapCmdList(eq(CLIENT.toString()),
                        eq(new String[]{"-o", clientName}),
                        eq(null));
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_NOT_IN_INFO_RANGE);
        when(resultMap.containsKey(MapKeys.UPDATE_KEY)).thenReturn(true);

        //when
        IClient client = clientDelegator.getClient(clientName);

        //then
        assertNotNull(client);
    }


    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNullWhenResultMapsIsNull() throws Exception {
        //when
        IClient client = ClientDelegator.getClientOrNullFromHelixResultMap(
                null,
                mock(GetClientTemplateOptions.class),
                server,
                mock(BiPredicate.class),
                mock(Function.class));

        //then
        assertNull(client);
    }

    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNullWhenResultMapsIsEmpty() throws Exception {
        //given
        resultMaps = Collections.EMPTY_LIST;

        //when
        IClient client = ClientDelegator.getClientOrNullFromHelixResultMap(
                resultMaps,
                mock(GetClientTemplateOptions.class),
                server,
                mock(BiPredicate.class),
                mock(Function.class));

        //then
        assertNull(client);
    }

    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNullBecauseAllResultMapIsInfoMessage() throws Exception {
        //given
        when(handle.apply(any())).thenReturn(true);

        //when
        IClient client = ClientDelegator.getClientOrNullFromHelixResultMap(
                resultMaps,
                mock(GetClientTemplateOptions.class),
                server,
                mock(BiPredicate.class),
                handle);

        //then
        assertNull(client);
    }

    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNullBecauseAllResultMapPredicateTestFailed() throws Exception {
        //given
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        BiPredicate<Map<String, Object>, GetClientTemplateOptions> conditions = mock(BiPredicate.class);
        when(conditions.test(any(), any())).thenReturn(false);
        when(handle.apply(any())).thenReturn(true);
        //when
        IClient client = ClientDelegator.getClientOrNullFromHelixResultMap(
                resultMaps,
                mock(GetClientTemplateOptions.class),
                server,
                conditions,
                handle);

        //then
        assertNull(client);
    }

    // p4ic4idea: the original unit test was wrong.  Non-explicit exceptions thrown by the handler should never be
    // wrapped.
    @Test
    public void getClientOrNullFromHelixResultMap_shouldThrowAccessExceptionThatWasThrownFromHandleErrorStr() throws Exception {
        //given
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);

        //then
        thrown.expect(IllegalStateException.class);
        ClientDelegator.getClientOrNullFromHelixResultMap(
                resultMaps,
                mock(GetClientTemplateOptions.class),
                server,
                mock(BiPredicate.class),
                // p4ic4idea: change handler so it explicitly throws a runtime exception.
                (o) -> {
                    throw new IllegalStateException();
                });
    }

    @Test
    public void getClientOrNullFromHelixResultMap_shouldReturnNonNullClient() throws Exception {
        //given
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        BiPredicate conditions = mock(BiPredicate.class);
        when(conditions.test(any(), any())).thenReturn(true);
        when(handle.apply(any())).thenReturn(true);

        //when
        IClient client = ClientDelegator.getClientOrNullFromHelixResultMap(
                resultMaps,
                mock(GetClientTemplateOptions.class),
                server,
                conditions,
                handle);

        //then
        assertNotNull(client);
    }

    @Test
    public void getClient_ByClientSummary_shouldReturnNullPointerExceptionWhenClientSummaryIsNull() throws Exception {
        //given
        clientSummary = null;

        //then
        thrown.expect(NullPointerException.class);
        clientDelegator.getClient(clientSummary);
    }

    @Test
    public void getClient_ByClientSummary_shouldReturnNonNullClient() throws Exception {
        //given
        doReturn(mockClient).when(clientDelegator).getClient(any(String.class));
        //when
        IClient client = clientDelegator.getClient(clientSummary);

        //then
        assertThat(client, is(mockClient));
        verify(clientSummary).getName();
    }

    @Test
    public void getClientTemplate_shouldReturnConnectionExceptionThatWasThrownFromInnerGetClientTemplate() throws Exception {
        //given
        doThrow(ConnectionException.class)
                .when(clientDelegator)
                .getClientTemplate(eq(clientName), eq(false));

        //then
        thrown.expect(ConnectionException.class);
        clientDelegator.getClientTemplate(clientName);
    }

    @Test
    public void getClientTemplate_shouldReturnNonNullClient() throws Exception {
        //given
        doReturn(mockClient)
                .when(clientDelegator)
                .getClientTemplate(eq(clientName), eq(false));

        //when
        IClient client = clientDelegator.getClientTemplate(clientName);

        //then
        assertThat(client, is(mockClient));
    }

    @Test
    public void getClientTemplate_ByNameAndAllowExistent_shouldThrowConnectionExceptionThatWasThrownFromInnerMethodCall() throws Exception {
        getClientTemplate_ByNameAndAllowExistent_Exceptions(ConnectionException.class,
                ConnectionException.class);
    }

    @Test
    public void getClientTemplate_ByNameAndAllowExistent_shouldThrowAccessExceptionThatWasThrownFromInnerMethodCall() throws Exception {
        // p4ic4idea: use a public, non-abstract class with default constructor
        getClientTemplate_ByNameAndAllowExistent_Exceptions(AccessException.AccessExceptionForTests.class,
                AccessException.class);
    }

    @Test
    public void getClientTemplate_ByNameAndAllowExistent_shouldThrowRequestExceptionThatWasThrownFromInnerMethodCall() throws Exception {
        getClientTemplate_ByNameAndAllowExistent_Exceptions(RequestException.class,
                RequestException.class);
    }

    @Test
    public void getClientTemplate_ByNameAndAllowExistent_shouldThrowRequestExceptionWhenInnerMethodCallThrownP4JavaException() throws Exception {
        getClientTemplate_ByNameAndAllowExistent_Exceptions(P4JavaException.class,
                RequestException.class);
    }

    private void getClientTemplate_ByNameAndAllowExistent_Exceptions(Class<? extends P4JavaException> thrownException, Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        thrown.expect(expectedThrows);
        doThrow(thrownException)
                .when(clientDelegator)
                .getClientTemplate(any(String.class), any(GetClientTemplateOptions.class));
        clientDelegator.getClientTemplate(clientName, false);
    }

    @Test
    public void getClientTemplate_ByNameAndAllowExistent_shouldReturnNonNullClient() throws Exception {
        //given
        doReturn(mockClient).when(clientDelegator)
                .getClientTemplate(eq(clientName), any(GetClientTemplateOptions.class));

        //when
        IClient client = clientDelegator.getClientTemplate(clientName, true);

        //then
        assertThat(client, is(mockClient));
    }

    @Test
    public void getClientTemplate_byNameAndGetClientTemplateOptions_shouldThrowIllegalArgumentExceptionWhenClientNameIsBlank() throws P4JavaException {
        //then
        thrown.expect(IllegalArgumentException.class);
        clientDelegator.getClientTemplate(EMPTY,
                        mock(GetClientTemplateOptions.class));
    }

    @Test
    public void getClientTemplate_byNameAndGetClientTemplateOptions_shouldReturnNonNullClient() throws P4JavaException {
        //given
        doReturn(resultMaps)
                .when(server)
                .execMapCmdList(eq(CLIENT.toString()), any(String[].class), any());
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_NOT_IN_INFO_RANGE);
        when(resultMap.containsKey(MapKeys.UPDATE_KEY)).thenReturn(false);
        when(resultMap.containsKey(MapKeys.ACCESS_KEY)).thenReturn(false);

        //when
        IClient client = clientDelegator.getClientTemplate(
                clientName,
                mock(GetClientTemplateOptions.class));

        //then
        assertNotNull(client);
    }

    @Test
    public void createClient_shouldThrowNullPointExceptionWhenClientIsNull() throws Exception {
        //then
        thrown.expect(NullPointerException.class);
        clientDelegator.createClient(null);
    }

    @Test
    public void createClient_shouldThrowConnectionExceptionThatWasThrownFromParseCommandResultMapIfIsInfoMessageAsString() throws Exception {
        //given
        doThrow(ConnectionException.class)
                .when(server)
                .execMapCmdList(eq(CLIENT.toString()), eq(new String[]{"-i"}), any(Map.class));

        //then
        thrown.expect(ConnectionException.class);
        clientDelegator.createClient(mockClient);
    }

    @Test
    public void createClient_shouldReturnClientName() throws Exception {
        //given
        when(server.execMapCmdList(
                eq(CLIENT.toString()),
                eq(new String[]{"-i"}),
                any(Map.class))).thenReturn(resultMaps);
        givenInfoMessageResultMap();

        //when
        String actualClientName = clientDelegator.createClient(mockClient);
        assertThat(actualClientName, is(clientName));
    }

    private void givenInfoMessageResultMap() {
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get(FMT0)).thenReturn(clientName);
    }

    @Test
    public void updateClient_shouldThrownNullPointerExceptionWhenClientIsNull() throws Exception {
        //then
        thrown.expect(NullPointerException.class);
        clientDelegator.updateClient(null);
    }

    @Test
    public void updateClient_shouldThrowConnectionExceptionThatWasThrownFromParseCommandResultMapIfIsInfoMessageAsString() throws Exception {
        //given
        doThrow(ConnectionException.class)
                .when(server)
                .execMapCmdList(eq(CLIENT.toString()), eq(new String[]{"-i"}), any(Map.class));

        //then
        thrown.expect(ConnectionException.class);
        clientDelegator.updateClient(mockClient);
    }

    @Test
    public void updateClient_shouldReturnNonNullClient() throws Exception {
        //given
        when(server.execMapCmdList(
                eq(CLIENT.toString()),
                eq(new String[]{"-i"}),
                any(Map.class))).thenReturn(resultMaps);
        givenInfoMessageResultMap();
        //when
        String actualClientName = clientDelegator.updateClient(mockClient);

        //then
        assertThat(actualClientName, is(clientName));
    }

    @Test
    public void updateClient_byClientAndForce_shouldThrowConnectionExceptionThatWasThrownFromInnerUpdateClientCall() throws Exception {
        updateClient_byClientAndBoolean_Exceptions(ConnectionException.class,
                ConnectionException.class);
    }

    @Test
    public void updateClient_byClientAndForce_shouldThrowRequestExceptionThatWasThrownFromInnerUpdateClientCall() throws Exception {
        updateClient_byClientAndBoolean_Exceptions(RequestException.class, RequestException.class);
    }

    @Test
    public void updateClient_byClientAndForce_shouldThrowAccessExceptionThatWasThrownFromInnerUpdateClientCall() throws Exception {
        // p4ic4idea: use a public, non-abstract class with default constructor
        updateClient_byClientAndBoolean_Exceptions(AccessException.AccessExceptionForTests.class,
                AccessException.class);
    }

    @Test
    public void updateClient_byClientAndForce_shouldThrowRequestExceptionWhenP4JavaExceptionWasThrowned() throws Exception {
        updateClient_byClientAndBoolean_Exceptions(P4JavaException.class, RequestException.class);
    }

    private void updateClient_byClientAndBoolean_Exceptions(Class<? extends P4JavaException> thrownException, Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        thrown.expect(expectedThrows);
        doThrow(thrownException)
                .when(clientDelegator)
                .updateClient(any(IClient.class), any(UpdateClientOptions.class));
        clientDelegator.updateClient(mockClient, false);
    }

    @Test
    public void updateClient_byClientAndForce_shouldReturnNonNullClient() throws Exception {
        //given
        doReturn(clientName)
                .when(clientDelegator)
                .updateClient(any(IClient.class), any(UpdateClientOptions.class));

        //when
        String actualClientName = clientDelegator.updateClient(mockClient, false);
        assertThat(actualClientName, is(clientName));
    }

    @Test
    public void updateClient_byClientAndUpdateClientOptions_shouldThrownNullPointerExceptionWhenClientIsNull() throws P4JavaException {
        //then
        thrown.expect(NullPointerException.class);
        clientDelegator.updateClient(null, mock(UpdateClientOptions.class));
    }

    @Test
    public void updateClient_byClientAndUpdateClientOptions_shouldThrownIllegalArgumentExceptionWhenClientNameIsBlank() throws P4JavaException {
        //given
        when(mockClient.getName()).thenReturn(EMPTY);

        //then
        thrown.expect(IllegalArgumentException.class);
        clientDelegator.updateClient(mockClient, mock(UpdateClientOptions.class));
    }

    @Test
    public void updateClient_byClientAndUpdateClientOptions_shouldReturnNonNullClientName() throws P4JavaException {
        //given
        when(server.execMapCmdList(
                eq(CLIENT.toString()),
                any(String[].class),
                any(Map.class))).thenReturn(resultMaps);
        givenInfoMessageResultMap();
        //when
        String actualClientName = clientDelegator.updateClient(mockClient,
                mock(UpdateClientOptions.class));

        //then
        assertThat(actualClientName, is(clientName));
    }

    @Test
    public void deleteClient_byClientNameAndForce_shouldThrownConnectionExceptionThatWasThrownInnerDeleteClient() throws Exception {
        deleteClient_byClientNameAndForce_Exceptions(ConnectionException.class,
                ConnectionException.class);
    }

    @Test
    public void deleteClient_byClientNameAndForce_shouldThrownAccessExceptionThatWasThrownInnerDeleteClient() throws Exception {
        // p4ic4idea: use a public, non-abstract class with default constructor
        deleteClient_byClientNameAndForce_Exceptions(AccessException.AccessExceptionForTests.class,
                AccessException.class);
    }

    @Test
    public void deleteClient_byClientNameAndForce_shouldThrownRequestExceptionThatWasThrownInnerDeleteClient() throws Exception {
        deleteClient_byClientNameAndForce_Exceptions(RequestException.class,
                RequestException.class);
    }

    @Test
    public void deleteClient_byClientNameAndForce_shouldThrownRequestExceptionWhenP4JavaExceptionWasThrown() throws Exception {
        deleteClient_byClientNameAndForce_Exceptions(P4JavaException.class, RequestException.class);
    }

    @Test
    public void deleteClient_byClientNameAndForce_shouldReturnDeletedClientName() throws Exception {
        //given
        doReturn(clientName).when(clientDelegator)
                .deleteClient(any(String.class), any(DeleteClientOptions.class));
        //when
        String actualClientName = clientDelegator.deleteClient(clientName, false);
        //then
        assertThat(actualClientName, is(clientName));
    }

    private void deleteClient_byClientNameAndForce_Exceptions(Class<? extends P4JavaException> thrownException, Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        thrown.expect(expectedThrows);
        doThrow(thrownException).when(clientDelegator)
                .deleteClient(any(String.class), any(DeleteClientOptions.class));
        clientDelegator.deleteClient(clientName, false);
    }

    @Test
    public void deleteClient_byClientNameAndDeleteClientOptions_shouldThrownIllegalArgumentExceptionWhenClientNameIsBlank() throws Exception {
        //then
        thrown.expect(IllegalArgumentException.class);
        clientDelegator.deleteClient(EMPTY, mock(DeleteClientOptions.class));
    }

    @Test
    public void deleteClient_byClientNameAndDeleteClientOptions_shouldReturnNonNullDeletedClientName() throws Exception {
        //given
        when(server.execMapCmdList(
                eq(CLIENT.toString()),
                any(String[].class),
                any()))
                .thenReturn(resultMaps);
        givenInfoMessageResultMap();

        //when
        String actualClientName = clientDelegator.deleteClient(
                clientName,
                mock(DeleteClientOptions.class));

        //then
        assertThat(actualClientName, is(clientName));
    }

    @Test
    public void replaceWithUnderscoreIfClientNameContainsWhitespacesOrTabs() throws Exception {
        //given
        String clientNameWithWhitespace = "my client\t\tname";
        String expectNewClientName = "my_client__name";
        mockClient = mock(IClient.class);
        when(mockClient.getName()).thenReturn(clientNameWithWhitespace);

        //when
        ClientDelegator.replaceWithUnderscoreIfClientNameContainsWhitespacesOrTabs(mockClient);
        //then
        verify(mockClient).setName(expectNewClientName);
    }

    @Test
    public void switchClientView_byTemplateNameTargetNameAndSwitchClientViewOptions_shouldThrownIllegalArgumentExceptionWhenTemplateClientNameIsBlank() throws Exception {
        //then
        thrown.expect(IllegalArgumentException.class);
        clientDelegator.switchClientView(EMPTY,
                        "myTargetClient",
                        mockSwitchClientViewOptions);
    }

    @Test
    public void switchClientView_byTemplateNameTargetNameAndSwitchClientViewOptions_shouldReturnNonBlankSwitchedClientViewName() throws Exception {
        //given
        when(server.execMapCmdList(eq(CLIENT.toString()), any(String[].class), eq(null)))
                .thenReturn(resultMaps);
        givenInfoMessageResultMap();

        //when
        String switchClientView = clientDelegator.switchClientView("clientName",
                "myTargetClient",
                mockSwitchClientViewOptions);
        //then
        assertThat(switchClientView, is(clientName));
    }

    @Test
    public void switchStreamView_byStreamPathTargetNameAndSwitchClientViewOptions_shouldThrownIllegalArgumentExceptionWhenStreamPathIsBlank() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        clientDelegator.switchStreamView(EMPTY,
                        "myTargetClient",
                        mockSwitchClientViewOptions);
    }

    @Test
    public void switchStreamView_byStreamPathTargetNameAndSwitchClientViewOptions_shouldReturnSwitchedStreamViewName() throws Exception {
        //given
        when(server.execMapCmdList(eq(CLIENT.toString()),
                any(String[].class),
                eq(null))).thenReturn(resultMaps);
        givenInfoMessageResultMap();

        //when
        String switchStreamView = clientDelegator.switchStreamView(
                "streamPath",
                "myTargetClient",
                mockSwitchClientViewOptions);
        //then
        assertThat(switchStreamView, is(clientName));
    }
}