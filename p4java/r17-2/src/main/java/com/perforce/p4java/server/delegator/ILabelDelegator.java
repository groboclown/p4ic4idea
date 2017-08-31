package com.perforce.p4java.server.delegator;

import javax.annotation.Nonnull;

import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.DeleteLabelOptions;

/**
 * Interface to handle the Label command.
 */
public interface ILabelDelegator {
    /**
     * Get a specific named Perforce label.
     * <p>
     * <p>
     * Unlike the getLabelList method, the getViewMapping method on the returned
     * label will be valid. Note though that changes to the returned label or
     * its view will not be reflected on to the server unless the updateLabel
     * method is called with the label as an argument.
     *
     * @param labelName non-null label name
     * @return ILabel representing the associated Perforce label, or null if no
     * such label exists on the server.
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    ILabel getLabel(String labelName)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Create a new Perforce label in the Perforce server.
     *
     * @param label non-null ILabel to be saved
     * @return non-null result message string from the Perforce server; this may
     * include form trigger output pre-pended and / or appended to the
     * "normal" message.
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    String createLabel(@Nonnull ILabel label)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Update an existing Perforce label in the Perforce server.
     *
     * @param label non-null ILabel to be updated
     * @return non-null result message string from the Perforce server; this may
     * include form trigger output pre-pended and / or appended to the
     * "normal" message.
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    String updateLabel(@Nonnull ILabel label)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Delete a named Perforce label from the Perforce server.
     *
     * @param labelName non-null label name
     * @param force     if true, forces the deletion of any label; normally labels can
     *                  only be deleted by their owner
     * @return non-null result message string from the Perforce server; this may
     * include form trigger output pre-pended and / or appended to the
     * "normal" message
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    String deleteLabel(String labelName, boolean force)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Delete a named Perforce label from the Perforce server.
     *
     * @param labelName non-null label name
     * @param opts      DeleteLabelOptions object describing optional parameters; if
     *                  null, no options are set.
     * @return non-null result message string from the Perforce server; this may
     * include form trigger output pre-pended and / or appended to the
     * "normal" message
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    String deleteLabel(String labelName, DeleteLabelOptions opts)
            throws P4JavaException;
}
