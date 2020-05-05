package com.perforce.p4java.option.client;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.option.Options;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.DeleteBranchSpecOptions;
import com.perforce.p4java.option.server.DeleteClientOptions;
import com.perforce.p4java.option.server.DeleteLabelOptions;
import com.perforce.p4java.option.server.DescribeOptions;
import com.perforce.p4java.option.server.DuplicateRevisionsOptions;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.option.server.FixJobsOptions;
import com.perforce.p4java.option.server.GetBranchSpecOptions;
import com.perforce.p4java.option.server.GetBranchSpecsOptions;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.GetClientTemplateOptions;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.option.server.GetCountersOptions;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.GetFileDiffsOptions;
import com.perforce.p4java.option.server.GetFileSizesOptions;
import com.perforce.p4java.option.server.GetFixesOptions;
import com.perforce.p4java.option.server.GetInterchangesOptions;
import com.perforce.p4java.option.server.GetJobsOptions;
import com.perforce.p4java.option.server.GetKeysOptions;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.option.server.GetPropertyOptions;
import com.perforce.p4java.option.server.GetProtectionEntriesOptions;
import com.perforce.p4java.option.server.GetReviewChangelistsOptions;
import com.perforce.p4java.option.server.GetReviewsOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.option.server.GetServerProcessesOptions;
import com.perforce.p4java.option.server.GetStreamOptions;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.option.server.GetSubmittedIntegrationsOptions;
import com.perforce.p4java.option.server.GetUserGroupsOptions;
import com.perforce.p4java.option.server.GetUsersOptions;
import com.perforce.p4java.option.server.JournalWaitOptions;
import com.perforce.p4java.option.server.KeyOptions;
import com.perforce.p4java.option.server.LogTailOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.MatchingLinesOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.option.server.PropertyOptions;
import com.perforce.p4java.option.server.ReloadOptions;
import com.perforce.p4java.option.server.SearchJobsOptions;
import com.perforce.p4java.option.server.SetFileAttributesOptions;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.option.server.TagFilesOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.option.server.UnloadOptions;
import com.perforce.p4java.option.server.UpdateClientOptions;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.option.server.VerifyFilesOptions;

/**
 * Class to test a default options constructor and then setting options..
 */
public class OptionsDefaultConstructorsTest {

    /**
     * Test default constructors and set options.
     * 
     * @throws InstantiationException
     *             the instantiation exception
     * @throws IllegalAccessException
     *             the illegal access exception
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws ClassNotFoundException
     */
    @Test
    public void testDefaultConstructorsAndSetOptions() throws InstantiationException, IllegalAccessException {

        List<Class<? extends Options>> subTypes = new ArrayList<Class<? extends Options>>();
        subTypes.add(SubmitOptions.class);
        subTypes.add(AddFilesOptions.class);
        subTypes.add(CopyFilesOptions.class);
        subTypes.add(DeleteFilesOptions.class);
        subTypes.add(EditFilesOptions.class);
        subTypes.add(GetDiffFilesOptions.class);
        subTypes.add(IntegrateFilesOptions.class);
        subTypes.add(LabelSyncOptions.class);
        subTypes.add(LockFilesOptions.class);
        subTypes.add(MergeFilesOptions.class);
        subTypes.add(PopulateFilesOptions.class);
        subTypes.add(ReconcileFilesOptions.class);
        subTypes.add(ReopenFilesOptions.class);
        subTypes.add(ResolvedFilesOptions.class);
        subTypes.add(ResolveFilesAutoOptions.class);
        subTypes.add(RevertFilesOptions.class);
        subTypes.add(ShelveFilesOptions.class);
        subTypes.add(SyncOptions.class);
        subTypes.add(UnlockFilesOptions.class);
        subTypes.add(UnshelveFilesOptions.class);
        subTypes.add(ChangelistOptions.class);
        subTypes.add(CounterOptions.class);
        subTypes.add(DeleteBranchSpecOptions.class);
        subTypes.add(DeleteClientOptions.class);
        subTypes.add(DeleteLabelOptions.class);
        subTypes.add(DescribeOptions.class);
        subTypes.add(DuplicateRevisionsOptions.class);
        subTypes.add(ExportRecordsOptions.class);
        subTypes.add(FixJobsOptions.class);
        subTypes.add(GetBranchSpecOptions.class);
        subTypes.add(GetBranchSpecsOptions.class);
        subTypes.add(GetChangelistDiffsOptions.class);
        subTypes.add(GetChangelistsOptions.class);
        subTypes.add(GetClientsOptions.class);
        subTypes.add(GetClientTemplateOptions.class);
        subTypes.add(GetCountersOptions.class);
        subTypes.add(GetDepotFilesOptions.class);
        subTypes.add(GetDirectoriesOptions.class);
        subTypes.add(GetExtendedFilesOptions.class);
        subTypes.add(GetFileAnnotationsOptions.class);
        subTypes.add(GetFileContentsOptions.class);
        subTypes.add(GetFileDiffsOptions.class);
        subTypes.add(GetFileSizesOptions.class);
        subTypes.add(GetFixesOptions.class);
        subTypes.add(GetInterchangesOptions.class);
        subTypes.add(GetJobsOptions.class);
        subTypes.add(GetKeysOptions.class);
        subTypes.add(GetLabelsOptions.class);
        subTypes.add(GetPropertyOptions.class);
        subTypes.add(GetProtectionEntriesOptions.class);
        subTypes.add(GetReviewChangelistsOptions.class);
        subTypes.add(GetReviewsOptions.class);
        subTypes.add(GetRevisionHistoryOptions.class);
        subTypes.add(GetServerProcessesOptions.class);
        subTypes.add(GetStreamOptions.class);
        subTypes.add(GetStreamsOptions.class);
        subTypes.add(GetSubmittedIntegrationsOptions.class);
        subTypes.add(GetUserGroupsOptions.class);
        subTypes.add(GetUsersOptions.class);
        subTypes.add(JournalWaitOptions.class);
        subTypes.add(KeyOptions.class);
        subTypes.add(LoginOptions.class);
        subTypes.add(LogTailOptions.class);
        subTypes.add(MatchingLinesOptions.class);
        subTypes.add(MoveFileOptions.class);
        subTypes.add(ObliterateFilesOptions.class);
        subTypes.add(OpenedFilesOptions.class);
        subTypes.add(PropertyOptions.class);
        subTypes.add(ReloadOptions.class);
        subTypes.add(SearchJobsOptions.class);
        subTypes.add(SetFileAttributesOptions.class);
        subTypes.add(StreamIntegrationStatusOptions.class);
        subTypes.add(StreamOptions.class);
        subTypes.add(SwitchClientViewOptions.class);
        subTypes.add(TagFilesOptions.class);
        subTypes.add(TrustOptions.class);
        subTypes.add(UnloadOptions.class);
        subTypes.add(UpdateClientOptions.class);
        subTypes.add(UpdateUserGroupOptions.class);
        subTypes.add(UpdateUserOptions.class);
        subTypes.add(VerifyFilesOptions.class);

        for (Class<? extends Options> subtype : subTypes) {
            if (!subtype.getName().contains("Test")) {
                Options options = subtype.newInstance();
                options.setOptions("one");
                assertTrue(options.getOptions() != null);
                assertTrue(options.getOptions().size() == 1);
                assertTrue(options.getOptions().contains("one"));
            }
        }

    }
}
