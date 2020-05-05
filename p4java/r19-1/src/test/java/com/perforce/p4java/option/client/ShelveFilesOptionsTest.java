package com.perforce.p4java.option.client;

import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

/**
 * The Class ShelveFilesOptionsTest.
 */
public class ShelveFilesOptionsTest {
	
	/**
	 * Process no options.
	 * @throws Exception the exception
	 */
	@Test
	public void processNoOptions() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions();
		List<String> optsList = opts.processOptions(null);
		assertTrue(optsList != null);
		assertTrue(optsList.size() == 0);
	}
	
	/**
	 * Sets the options.
	 * @throws Exception the exception
	 */
	@Test
	public void setOptions() throws Exception {
		final int four  = 4;
		ShelveFilesOptions opts = new ShelveFilesOptions();
		opts.setOptions("-f", "-r", "-d", "-p");
		List<String> optsList = opts.getOptions();
		assertTrue(optsList != null);
		assertTrue(optsList.size() == four);
		assertTrue(optsList.containsAll(
				Arrays.asList(new String[] { "-f", "-r", "-d", "-p" })));
		opts = new ShelveFilesOptions();
		opts.setOptions("any");
		optsList = opts.getOptions();
		assertTrue(optsList != null);
		assertTrue(optsList.size() == 1);
		assertTrue(optsList.containsAll(
				Arrays.asList(new String[] { "any" })));
	}

	/**
	 * Process some options.
	 * @throws Exception the exception
	 */
	@Test
	public void processSomeOptions() throws Exception {
		final int four  = 4;
		final int three = 3;
		ShelveFilesOptions opts = new ShelveFilesOptions(true, true, true, true);
		List<String> optsList = opts.processOptions(null);
		assertTrue(optsList != null);
		assertTrue(optsList.size() == four);
		assertTrue(optsList.containsAll(
			Arrays.asList(new String[] { "-f", "-r", "-d", "-p" })));
		opts = new ShelveFilesOptions(false, true, true, true);
		optsList = opts.processOptions(null);
		assertTrue(optsList != null);
		assertTrue(optsList.size() == three);
		assertTrue(optsList.containsAll(
			Arrays.asList(new String[] { "-r", "-d", "-p" })));
		opts = new ShelveFilesOptions(false, false, true, true);
		optsList = opts.processOptions(null);
		assertTrue(optsList != null);
		assertTrue(optsList.size() == 2);
		assertTrue(optsList.containsAll(Arrays.asList(new String[] { "-d", "-p" })));
		opts = new ShelveFilesOptions(false, false, false, true);
		optsList = opts.processOptions(null);
		assertTrue(optsList != null);
		assertTrue(optsList.size() == 1);
		assertTrue(optsList.containsAll(Arrays.asList(new String[] { "-p" })));
		opts = new ShelveFilesOptions(true, false, false, true);
		optsList = opts.processOptions(null);
		assertTrue(optsList != null);
		assertTrue(optsList.size() == 2);
		assertTrue(optsList.containsAll(Arrays.asList(new String[] { "-f", "-p" })));
	}

	/**
	 * Checks if is force shelve.
	 * @throws Exception the exception
	 */
	@Test
	public void isForceShelve() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions(true, true, true, true);
		assertTrue(opts.forceShelve);
		assertTrue(opts.isForceShelve());
	}

	/**
	 * Sets the force shelve.
	 * @throws Exception the exception
	 */
	@Test
	public void setForceShelve() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions(false, true, true, true);
		assertFalse(opts.forceShelve);
		assertFalse(opts.isForceShelve());
		opts.setForceShelve(true);
		assertTrue(opts.forceShelve);
		assertTrue(opts.isForceShelve());
	}

	/**
	 * Checks if is replace files.
	 * @throws Exception the exception
	 */
	@Test
	public void isReplaceFiles() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions(true, true, true, true);
		assertTrue(opts.replaceFiles);
		assertTrue(opts.isReplaceFiles());
	}

	/**
	 * Sets the replace files.
	 * @throws Exception the exception
	 */
	@Test
	public void setReplaceFiles() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions(false, false, true, true);
		assertFalse(opts.replaceFiles);
		assertFalse(opts.isReplaceFiles());
		opts.setReplaceFiles(true);
		assertTrue(opts.replaceFiles);
		assertTrue(opts.isReplaceFiles());
	}

	/**
	 * Checks if is delete files.
	 * @throws Exception the exception
	 */
	@Test
	public void isDeleteFiles() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions(true, true, true, true);
		assertTrue(opts.deleteFiles);
		assertTrue(opts.isDeleteFiles());
	}

	/**
	 * Sets the delete files.
	 * @throws Exception the exception
	 */
	@Test
	public void setDeleteFiles() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions(false, false, false, true);
		assertFalse(opts.deleteFiles);
		assertFalse(opts.isDeleteFiles());
		opts.setDeleteFiles(true);
		assertTrue(opts.deleteFiles);
		assertTrue(opts.isDeleteFiles());
	}
	
	/**
	 * Checks if is promote files.
	 * @throws Exception the exception
	 */
	@Test
	public void isPromoteFiles() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions(true, true, true, true);
		assertTrue(opts.promotesShelvedChangeIfDistributedConfigured);
		assertTrue(opts.isPromotesShelvedChangeIfDistributedConfigured());
	}

	/**
	 * Sets the promote files.
	 * @throws Exception the exception
	 */
	@Test
	public void setPromoteFiles() throws Exception {
		ShelveFilesOptions opts = new ShelveFilesOptions(false, false, false, false);
		assertFalse(opts.promotesShelvedChangeIfDistributedConfigured);
		assertFalse(opts.isPromotesShelvedChangeIfDistributedConfigured());
		opts.setPromotesShelvedChangeIfDistributedConfigured(true);
		assertTrue(opts.promotesShelvedChangeIfDistributedConfigured);
		assertTrue(opts.isPromotesShelvedChangeIfDistributedConfigured());
	}
}