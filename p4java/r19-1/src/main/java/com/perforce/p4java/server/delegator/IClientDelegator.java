package com.perforce.p4java.server.delegator;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.DeleteClientOptions;
import com.perforce.p4java.option.server.GetClientTemplateOptions;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.option.server.UpdateClientOptions;

import javax.annotation.Nonnull;

public interface IClientDelegator {
    IClient getClient(final String clientName)
            throws ConnectionException, RequestException, AccessException;

    IClient getClient(@Nonnull IClientSummary clientSummary)
            throws ConnectionException, RequestException, AccessException;

    IClient getClientTemplate(String clientName)
            throws ConnectionException, RequestException, AccessException;

    IClient getClientTemplate(String clientName, final boolean allowExistent)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Get a template of a non-existent named Perforce client. This will only
     * return an IClient for clients that don't exist unless the allowExistent
     * parameter is set to true. This method is designed to be able to get the
     * server returned default values it uses when a non-existent client is
     * requested.
     *
     * @param clientName               Not blank Perforce client name.
     * @param getClientTemplateOptions GetClientTemplateOptions object describing optional
     *                                 parameters; if null, no options are set.
     * @return IClient representing the specified Perforce client template, or null if no such
     * client template.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    IClient getClientTemplate(String clientName, GetClientTemplateOptions getClientTemplateOptions)
            throws P4JavaException;

    String createClient(@Nonnull final IClient newClient)
            throws ConnectionException, RequestException, AccessException;

    void createTempClient(@Nonnull final IClient newClient)
            throws ConnectionException, RequestException, AccessException;

    String updateClient(@Nonnull final IClient client)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Update an existing Perforce client on the current Perforce server. This
     * client does not need to be the current client, and no association with
     * the passed-in client is made by the server (i.e. it's not made the
     * current client).
     *
     * @param client non-null IClient defining the Perforce client to be updated
     * @param force  if true, tell the server to attempt to force the update regardless of the
     *               consequences. You're on your own with this one...
     * @return possibly-null operation result message string from the Perforce server
     * @throws RequestException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    String updateClient(IClient client, final boolean force)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Update an existing Perforce client on the current Perforce server. This
     * client does not need to be the current client, and no association with
     * the passed-in client is made by the server (i.e. it's not made the
     * current client).
     *
     * @param client non-null IClient defining the Perforce client to be updated
     * @param opts   UpdateClientOptions object describing optional parameters; if null, no options
     *               are set.
     * @return possibly-null operation result message string from the Perforce server
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    String updateClient(IClient client, final UpdateClientOptions opts)
            throws P4JavaException;

    String deleteClient(String clientName, final boolean force)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Delete a Perforce client from a Perforce server. The effects this has on
     * the client and the server are not well-defined here, and you should
     * probably consult the relevant Perforce documentation for your specific
     * case. In any event, you can cause quite a lot of inconvenience (and maybe
     * even damage) doing a forced delete without preparing properly for it,
     * especially if the client is the server object's current client.
     *
     * @param clientName non-null name of the client to be deleted from the server.
     * @param opts       DeleteClientOptions object describing optional parameters; if null, no
     *                   options are set.
     * @return possibly-null operation result message string from the Perforce server.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    String deleteClient(String clientName, final DeleteClientOptions opts)
            throws P4JavaException;

    /**
     * Switch the target client spec's view without invoking the editor. With -t
     * to switch to a view defined in another client spec. Switching views is
     * not allowed in a client that has opened files. The -f flag can be used
     * with -s to force switching with opened files. View switching has no
     * effect on files in a client workspace until 'p4 sync' is run.
     *
     * @param templateClientName non-null name of the template client who's view will be used for
     *                           the target (or current) client to switched to.
     * @param targetClientName   possibly-null name of the target client whose view will be changed
     *                           to the template client's view. If null, the current client will be
     *                           used.
     * @param opts               SwitchClientViewOptions object describing optional parameters; if
     *                           null, no options are set.
     * @return possibly-null operation result message string from the Perforce server
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    String switchClientView(String templateClientName,
                            final String targetClientName,
                            SwitchClientViewOptions opts) throws P4JavaException;

    /**
     * Switch the target client spec's view without invoking the editor. With -S
     * to switch to the specified stream's view. Switching views is not allowed
     * in a client that has opened files. The -f flag can be used with -s to
     * force switching with opened files. View switching has no effect on files
     * in a client workspace until 'p4 sync' is run.
     *
     * @param streamPath       non-null stream's path in a stream depot, of the form
     *                         //depotname/streamname who's view will be used for the target (or
     *                         current) client to switched to.
     * @param targetClientName possibly-null name of the target client whose view will be changed to
     *                         the stream's view. If null, the current client will be used.
     * @param opts             SwitchClientViewOptions object describing optional parameters; if
     *                         null, no options are set.
     * @return possibly-null operation result message string from the Perforce server
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    String switchStreamView(String streamPath,
                            final String targetClientName,
                            SwitchClientViewOptions opts) throws P4JavaException;
}
