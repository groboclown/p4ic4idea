package com.perforce.p4java.option.client;

import java.util.Arrays;
import java.util.List;

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
import com.perforce.p4java.option.server.GraphCommitLogOptions;
import com.perforce.p4java.option.server.GraphReceivePackOptions;
import com.perforce.p4java.option.server.GraphRevListOptions;
import com.perforce.p4java.option.server.GraphShowRefOptions;
import com.perforce.p4java.option.server.JournalWaitOptions;
import com.perforce.p4java.option.server.KeyOptions;
import com.perforce.p4java.option.server.ListOptions;
import com.perforce.p4java.option.server.LogTailOptions;
import com.perforce.p4java.option.server.Login2Options;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.MatchingLinesOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.option.server.PropertyOptions;
import com.perforce.p4java.option.server.ReloadOptions;
import com.perforce.p4java.option.server.ReposOptions;
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
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.perforce.p4java.option.Options;

/**
 * Class to test a default options constructor and then
 * setting options..
 */
public class OptionsDefaultConstructorsTest {
	// The original class had a class dependency error.  Rather than hunt that down, we're just
	// using the list of known Option instances.  This also keeps the reflection tools from
	// finding test versions of the options.
	private static List<Class<? extends Options>> OPTION_CLASSES = Arrays.asList(
			AddFilesOptions.class,
			ChangelistOptions.class,
			CopyFilesOptions.class,
			CounterOptions.class,
			DeleteBranchSpecOptions.class,
			DeleteClientOptions.class,
			DeleteFilesOptions.class,
			DeleteLabelOptions.class,
			DescribeOptions.class,
			DuplicateRevisionsOptions.class,
			EditFilesOptions.class,
			ExportRecordsOptions.class,
			FixJobsOptions.class,
			GetBranchSpecsOptions.class,
			GetChangelistDiffsOptions.class,
			GetChangelistsOptions.class,
			GetClientTemplateOptions.class,
			GetClientsOptions.class,
			GetCountersOptions.class,
			GetDepotFilesOptions.class,
			GetDiffFilesOptions.class,
			GetDirectoriesOptions.class,
			GetExtendedFilesOptions.class,
			GetFileAnnotationsOptions.class,
			GetFileContentsOptions.class,
			GetFileDiffsOptions.class,
			GetFileSizesOptions.class,
			GetFixesOptions.class,
			GetInterchangesOptions.class,
			GetJobsOptions.class,
			GetKeysOptions.class,
			GetLabelsOptions.class,
			GetPropertyOptions.class,
			GetProtectionEntriesOptions.class,
			GetReviewChangelistsOptions.class,
			GetReviewsOptions.class,
			GetRevisionHistoryOptions.class,
			GetServerProcessesOptions.class,
			GetStreamOptions.class,
			GetStreamsOptions.class,
			GetSubmittedIntegrationsOptions.class,
			GetUserGroupsOptions.class,
			GetUsersOptions.class,
			GraphCommitLogOptions.class,
			GraphReceivePackOptions.class,
			GraphRevListOptions.class,
			GraphShowRefOptions.class,
			IntegrateFilesOptions.class,
			JournalWaitOptions.class,
			KeyOptions.class,
			LabelSyncOptions.class,
			ListOptions.class,
			LockFilesOptions.class,
			LogTailOptions.class,
			Login2Options.class,
			LoginOptions.class,
			MatchingLinesOptions.class,
			MergeFilesOptions.class,
			MoveFileOptions.class,
			ObliterateFilesOptions.class,
			OpenedFilesOptions.class,
			PopulateFilesOptions.class,
			PropertyOptions.class,
			ReconcileFilesOptions.class,
			ReloadOptions.class,
			ReposOptions.class,
			ResolveFilesAutoOptions.class,
			ResolvedFilesOptions.class,
			RevertFilesOptions.class,
			SearchJobsOptions.class,
			SetFileAttributesOptions.class,
			ShelveFilesOptions.class,
			StreamIntegrationStatusOptions.class,
			StreamOptions.class,
			SubmitOptions.class,
			SwitchClientViewOptions.class,
			SyncOptions.class,
			TagFilesOptions.class,
			TrustOptions.class,
			UnloadOptions.class,
			UnlockFilesOptions.class,
			UnshelveFilesOptions.class,
			UpdateClientOptions.class,
			UpdateUserGroupOptions.class,
			UpdateUserOptions.class,
			VerifyFilesOptions.class
	);


	/**
	 * Test default constructors and set options.
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 */
	@Test
	public void testDefaultConstructorsAndSetOptions()
			throws InstantiationException, IllegalAccessException {
		for (Class<? extends Options> subtype : OPTION_CLASSES) {
			if (!subtype.getName().contains("Test")) {
				Options options;
				try {
					options = subtype.newInstance();
				} catch (InstantiationException e) {
					fail("No default constructor for " + subtype);
					continue;
				}
				options.setOptions("one");
				assertTrue(options.getOptions() != null);
				assertTrue(options.getOptions().size() == 1);
				assertTrue(options.getOptions().contains("one"));
			}
		}
	}
}
