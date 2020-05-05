package com.perforce.p4java.server.delegator;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.server.callback.IStreamingCallback;

/**
 * Interface to handle the Export command.
 */
public interface IExportDelegator {
    /**
     * Get a list of exported journal or checkpoint records (admin / superuser
     * command).
     * <p>
     * <p>
     * See the main p4 export command documentation for full semantics and usage
     * details.
     * <p>
     * <p>
     * Note that the 'skip*' options in ExportRecordsOptions are specific to
     * P4Java only; they are not Perforce command options. These options are for
     * field handling rules in the lower layers of P4Java. The field rules are
     * for identifying the fields that should skip charset translation of their
     * values; leaving their values as bytes instead of converting them to
     * strings. Please see ExportRecordsOptions for usage details.
     *
     * @param useJournal    *If true, specifies a journal number and optional offset position (journal
     *                      number/offset) from which to start exporting. Corresponds to the '-j
     *                      token' flag.<p>
     *                      <p>
     *                      If false, specifies a checkpoint number and optional offset position
     *                      (checkpoint number#offset) from which to start exporting. Corresponds to
     *                      the '-c token' flag.
     * @param maxRecs       If greater than zero, limits the number of lines (records) exported.
     *                      Corresponds to the '-l lines' flag.
     * @param sourceNum     If positive, specifies a journal or checkpoint number. Corresponds to the
     *                      'token' part of the '-j token' and '-c token' flags.<p>
     *                      <p>
     *                      The '-j token' flag specifies a journal number and optional position (in
     *                      the form: journal number/offset) from which to start exporting. The -c
     *                      token flag specifies a checkpoint number and optional position (in the
     *                      form: checkpoint number#offset) from which to start exporting.
     * @param offset        If positive, specifies a journal or checkpoint optional offset position
     *                      (journal number/offset or checkpoint number#offset) from which to start
     *                      exporting.
     * @param format        If true, formats non-textual datatypes appropriately. Corresponds to the
     *                      '-f' flag.
     * @param journalPrefix If non-null, specifies a file name prefix to match the one used with 'p4d
     *                      -jc <prefix>'. Corresponds to the '-J' flag.
     * @param filter        If non-null, limits output to records that match the filter pattern.
     *                      Corresponds to the '-F' flag.
     * @return export records list
     * @throws ConnectionException
     * @throws RequestException
     * @throws AccessException
     */
    List<Map<String, Object>> getExportRecords(
            boolean useJournal,
            long maxRecs,
            int sourceNum,
            long offset,
            boolean format,
            String journalPrefix,
            String filter) throws ConnectionException, RequestException, AccessException;

    /**
     * Get a list of exported journal or checkpoint records (admin / superuser
     * command).
     * <p>
     * <p>
     * See the main p4 export command documentation for full semantics and usage
     * details.
     * <p>
     * <p>
     * Note that the 'skip*' options in ExportRecordsOptions are specific to
     * P4Java only; they are not Perforce command options. These options are for
     * field handling rules in the lower layers of P4Java. The field rules are
     * for identifying the fields that should skip charset translation of their
     * values; leaving their values as bytes instead of converting them to
     * strings. Please see ExportRecordsOptions for usage details.
     *
     * @param opts ExportRecordsOptions object describing optional parameters; if null, no options
     *             are set.
     * @return non-null but possibly empty list of maps representing exported journal or checkpoint
     * records.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     */
    List<Map<String, Object>> getExportRecords(ExportRecordsOptions opts) throws P4JavaException;

    /**
     * Get each exported journal or checkpoint record (admin / superuser
     * command) as it comes in from the server, rather than waiting for the
     * entire command to complete.
     * <p>
     * <p>
     * The results are sent to the user using the IStreamingCallback
     * handleResult method; see the IStreamingCallback Javadoc for details. The
     * payload passed to handleResult is usually the raw map gathered together
     * deep in the RPC protocol layer, and the user is assumed to have the
     * knowledge and technology to be able to parse it and use it suitably in
     * much the same way as a user unpacks or processes the results from the
     * other low-level exec methods like execMapCommand.
     * <p>
     * <p>
     * See the main p4 export command documentation for full semantics and usage
     * details.
     * <p>
     * <p>
     * Note that the 'skip*' options in ExportRecordsOptions are specific to
     * P4Java only; they are not Perforce command options. These options are for
     * field handling rules in the lower layers of P4Java. The field rules are
     * for identifying the fields that should skip charset translation of their
     * values; leaving their values as bytes instead of converting them to
     * strings. Please see ExportRecordsOptions for usage details.
     *
     * @param opts     ExportRecordsOptions object describing optional parameters; if null, no
     *                 options are set.
     * @param callback a non-null IStreamingCallback to be used to process the incoming results.
     * @param key      an opaque integer key that is passed to the IStreamingCallback callback
     *                 methods to identify the action as being associated with this specific call.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2012.3
     */
    void getStreamingExportRecords(
            ExportRecordsOptions opts,
            @Nonnull IStreamingCallback callback,
            int key) throws P4JavaException;
}
