package com.perforce.p4java;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetFileSizesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.UnitTestGivenThatWillThrowException;
import org.junit.jupiter.api.Executable;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.perforce.p4java.core.ChangelistStatus.NEW;
import static com.perforce.p4java.core.ChangelistStatus.PENDING;
import static com.perforce.p4java.core.ChangelistStatus.SUBMITTED;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.expectThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Sean Shou
 * @since 25/08/2016
 */
public abstract class AbstractP4JavaUnitTest {
	protected static IOptionsServer server = null;
	protected IClient client = null;

	protected File loadFileFromClassPath(String fileName) {
		return new File(this.getClass().getClassLoader().getResource(fileName).getFile());
	}

	protected <T> Method getPrivateMethod(Class<T> clazzType, String methodName, Class<?>... parameterTypes) {
		try {
			Method declaredMethod = clazzType.getDeclaredMethod(methodName, parameterTypes);
			declaredMethod.setAccessible(true);
			return declaredMethod;
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	protected void testIfGivenExceptionWasThrown(Class<? extends P4JavaException> firstThrownException, Class<? extends P4JavaException> expectedThrows, Executable executable, UnitTestGivenThatWillThrowException unitTestGiven) throws P4JavaException {
		unitTestGiven.given(firstThrownException);

		expectThrows(expectedThrows, executable);
	}

	protected void testIfGivenExceptionWasThrown(Class<? extends P4JavaException> firstThrownException, Executable executable, UnitTestGivenThatWillThrowException unitTestGiven) throws P4JavaException {
		testIfGivenExceptionWasThrown(firstThrownException, firstThrownException, executable, unitTestGiven);
	}

	protected void revertChangelist(
			final IChangelist changelist,
			final List<IFileSpec> revertFileSpecs) {

		if (nonNull(client) && nonNull(changelist)) {
			try {
				ChangelistStatus status = changelist.getStatus();
				if (status == PENDING) {
					deletePendingChangelist(changelist);
				} else if (status == SUBMITTED) {
					deleteDepotFileFromServerSideIfExist(revertFileSpecs);
				}
			} catch (P4JavaException ignore) {
				Log.info(ignore.getLocalizedMessage());
			}
		}
	}

	protected void deletePendingChangelist(IChangelist changelist) throws P4JavaException {
		RevertFilesOptions revertFilesOptions = new RevertFilesOptions()
				.setChangelistId(changelist.getId());

		client.revertFiles(changelist.getFiles(true), revertFilesOptions);
		if (nonNull(server)) {
			server.deletePendingChangelist(changelist.getId());
		}
	}

	protected void deleteDepotFileFromServerSideIfExist(final List<IFileSpec> toDeleteFileSpecs)
			throws P4JavaException {

		IChangelist toDeleteChangeList = createChangeList("delete submitted file for next test");
		List<IFileSpec> files = client.deleteFiles(toDeleteFileSpecs,
				new DeleteFilesOptions().setChangelistId(toDeleteChangeList.getId()));
		if (files.size() > 0) {
			toDeleteChangeList.submit(new SubmitOptions());
		}
	}

	protected IChangelist createChangeList(final String changelistDescription)
			throws ConnectionException, AccessException, RequestException {

		return client.createChangelist(new Changelist(
				IChangelist.UNKNOWN,
				client.getName(),
				client.getOwnerName(),
				NEW,
				null,
				changelistDescription,
				false,
				(Server) server
		));
	}

	protected void connectToServer(final String clientName) throws Exception {
		client = server.getClient(clientName);
		assertThat(client, notNullValue());
		server.setCurrentClient(client);
	}

	protected Path copyTestToClientRoot(
			final String relativeDepotPath,
			final File testResourceFile) throws Exception {

		Path clientFileParentPath = Paths.get(client.getRoot(), relativeDepotPath);
		if (Files.notExists(clientFileParentPath)) {
			Files.createDirectories(clientFileParentPath);
		}

		String fileName = testResourceFile.getName();
		Path targetFilePath = clientFileParentPath.resolve(fileName);
		Files.deleteIfExists(targetFilePath);
		Files.copy(testResourceFile.toPath(), targetFilePath);

		return targetFilePath;
	}

	protected IChangelist submitFileToDepot(final List<IFileSpec> submittingFileSpecs) throws P4JavaException {
		IChangelist changelist = addFilesToDepot(submittingFileSpecs);
		List<IFileSpec> out = changelist.submit(new SubmitOptions());

		String errors = "";
		for (IFileSpec file : out) {
			if (file.getOpStatus() == FileSpecOpStatus.ERROR) {
				errors += file.getStatusMessage() + "\n";
			}
		}
		if (errors.length() > 0) {
			throw new P4JavaException(errors);
		}

		return changelist;
	}

	protected IChangelist addFilesToDepot(final List<IFileSpec> addFileDepotFileSpecs) throws P4JavaException {
		IChangelist changeList = createChangeList("test submit files to p4d");
		List<IFileSpec> out = client.addFiles(addFileDepotFileSpecs,
				new AddFilesOptions().setChangelistId(changeList.getId()));

		String errors = "";
		for (IFileSpec file : out) {
			if (file.getOpStatus() == FileSpecOpStatus.ERROR) {
				errors += file.getStatusMessage() + "\n";
			}
		}
		if (errors.length() > 0) {
			throw new P4JavaException(errors);
		}
		return changeList;
	}

	protected void verifyServerSideFileType(
			final String fileDepotPath,
			final String expectedPerforceFileType) throws Exception {

		List<IFileSpec> depotFileSpecs = server.getDepotFiles(
				FileSpecBuilder.makeFileSpecList(fileDepotPath),
				false);

		assertThat(fileDepotPath, depotFileSpecs, notNullValue());
		assertThat(fileDepotPath, depotFileSpecs.size(), is(1));
		assertThat(fileDepotPath, depotFileSpecs.get(0).getFileType(), is(expectedPerforceFileType));
	}

	protected void verifyServerSideFileSize(
			final String fileDepotPath,
			final long expectedFileSize) throws Exception {

		List<IFileSize> depotFileSizes = server.getFileSizes(
				FileSpecBuilder.makeFileSpecList(fileDepotPath),
				new GetFileSizesOptions().setMaxFiles(1));

		assertThat(fileDepotPath, depotFileSizes, notNullValue());
		assertThat(fileDepotPath, depotFileSizes.size(), is(1));
		assertThat(fileDepotPath, depotFileSizes.get(0).getFileSize(), is(expectedFileSize));
	}

	protected void verifyFileSizeAfterSyncFromDepot(
			final String fileDepotPath,
			final Path fileLocalPath,
			final long expectedFileSize) throws Exception {

		List<IFileSpec> files = client.sync(
				FileSpecBuilder.makeFileSpecList(fileDepotPath),
				new SyncOptions().setForceUpdate(true));
		if (files.size() < 1) {
			fail("Sync test file: " + fileDepotPath + "failed");
		}
		IFileSpec fileSpec = files.get(0);
		assertThat(fileDepotPath, fileSpec.getDepotPathString(), containsString(fileDepotPath));
		assertThat(fileDepotPath, Files.size(fileLocalPath), is(expectedFileSize));
	}

	protected SubmittingSupplier submitFileThatLoadFromClassPath(
			final String clientName,
			final String fileDepotPath,
			final String relativeDepotPath,
			final File testResourceFile) throws Exception {

		connectToServer(clientName);
		List<IFileSpec> submittingFileSpecs = FileSpecBuilder.makeFileSpecList(fileDepotPath);
		Path targetLocalFile = copyTestToClientRoot(relativeDepotPath, testResourceFile);

		deleteDepotFileFromServerSideIfExist(submittingFileSpecs);
		IChangelist changelist = submitFileToDepot(submittingFileSpecs);
		assertThat(changelist, notNullValue());
		List<IFileSpec> submittedFiles = changelist.getFiles(true);
		assertThat(submittedFiles.size(), is(1));
		IFileSpec file = submittedFiles.get(0);
		assertThat(file.getDepotPathString(), containsString(fileDepotPath));

		return new SubmittingSupplier(submittingFileSpecs.get(0), changelist, targetLocalFile);
	}

	protected class SubmittingSupplier implements Serializable {
		private static final long serialVersionUID = 3905413855583618193L;
		private IFileSpec submittedFileSpec;
		private IChangelist changelist;
		private Path targetLocalFile;

		public SubmittingSupplier(
				final IFileSpec submittedFileSpec,
				final IChangelist changelist,
				final Path targetLocalFile) {

			this.submittedFileSpec = submittedFileSpec;
			this.changelist = changelist;
			this.targetLocalFile = targetLocalFile;
		}

		public IFileSpec submittedFileSpec() {
			return submittedFileSpec;
		}

		public IChangelist changelist() {
			return changelist;
		}

		public Path targetLocalFile() {
			return targetLocalFile;
		}
	}
}
