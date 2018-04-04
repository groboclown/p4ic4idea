/*
 * Copyright (c) ${year}, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the SymbolicLinkHelper.
 */
public class SymbolicLinkHelperTest {
	
	/** The temp file. */
	private Path tempFile;
	
	/**
	 * Runs before every test.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Before
	public void before() throws IOException {
		tempFile = Files.createTempFile("SymbolicLinkHelperTest", ".txt");
	}
	
	/**
	 * Runs after every test.
	 */
	@After
	public void after() {
		tempFile.toFile().delete();
	}

	/**
	 * Test symbolic link.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testSymbolicLink() throws IOException {
		Path tempLink = createTempLink();
		assertTrue(SymbolicLinkHelper.isSymbolicLink(tempLink.toString()));
	}
	
	/**
	 * Test not a symbolic link.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testNotASymbolicLink() throws IOException {
		assertFalse(SymbolicLinkHelper.isSymbolicLink(tempFile.toString()));
	}
	
	/**
	 * Test symbolic link null.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testSymbolicLinkNull() throws IOException {
		assertFalse(SymbolicLinkHelper.isSymbolicLink(null));
	}
	
	/**
	 * Test symbolic link empty.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testSymbolicLinkEmpty() throws IOException {
		assertFalse(SymbolicLinkHelper.isSymbolicLink(""));
	}
	
	/**
	 * Test last modified time null.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testLastModifiedTimeNull() throws IOException {
		assertEquals(0L, SymbolicLinkHelper.getLastModifiedTime(null));
	}
	
	/**
	 * Test last modified time invalid.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testLastModifiedTimeInvalid() throws IOException {
		assertEquals(0L, SymbolicLinkHelper.getLastModifiedTime(
				RandomStringUtils.randomNumeric(10)));
	}
	
	/**
	 * Test last modified time.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testLastModifiedTime() throws IOException {
		Path tempLink = createTempLink();
		// Modified should be the link time not the real
		// file and we do not follow the link
		assertTrue("There are more than 5 milliseconds between the last modified times of <"
				+ tempLink + "> and its target",
			Files.getLastModifiedTime(tempLink).toMillis()-10 <
			SymbolicLinkHelper.getLastModifiedTime(tempLink.toString()));
	}
	
	/**
	 * Test read symbolic link.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testReadSymbolicLink() throws IOException {
		Path tempLink = createTempLink();
		assertEquals(
			tempFile.toString(),
			SymbolicLinkHelper.readSymbolicLink(tempLink.toString()));
	}
	
	/**
	 * Test read symbolic link null.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testReadSymbolicLinkNull() throws IOException {
		assertNull(SymbolicLinkHelper.readSymbolicLink(null));
	}
	
	/**
	 * Test read symbolic link empty.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testReadSymbolicLinkEmpty() throws IOException {
		assertNull(SymbolicLinkHelper.readSymbolicLink(""));
	}
	
	/**
	 * Test read symbolic link invalid.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testReadSymbolicLinkInvalid() throws IOException {
		assertNull(SymbolicLinkHelper.readSymbolicLink(
				RandomStringUtils.randomNumeric(10)));
	}
	
	/**
	 * Test exists null.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testExistsNull() throws IOException {
		assertFalse(SymbolicLinkHelper.exists(null));
	}
	
	/**
	 * Test exists empty.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testExistsEmpty() throws IOException {
		assertTrue(SymbolicLinkHelper.exists(""));
	}
	
	/**
	 * Test exists invalid.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testExistsInvalid() throws IOException {
		assertFalse(SymbolicLinkHelper.exists(
				RandomStringUtils.randomNumeric(10)));
	}
	
	/**
	 * Test exists.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testExists() throws IOException {
		Path tempLink = createTempLink();
		assertTrue(SymbolicLinkHelper.exists(tempLink.toString()));
		tempFile.toFile().delete();
		// We don't follow the link so when the file is deleted 
		// it should still exist
		assertTrue(SymbolicLinkHelper.exists(tempLink.toString()));
		// By default the files method does follow links so this
		// should be false
		assertFalse(Files.exists(tempLink));
	}
	
	/**
	 * Test create null.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testCreateNull() throws IOException {
		assertNull(SymbolicLinkHelper.createSymbolicLink(null, null));
	}
	
	/**
	 * Test create empty.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testCreateEmpty() throws IOException {
		assertNull(SymbolicLinkHelper.createSymbolicLink("", ""));
	}
	
	/**
	 * Test create.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testCreate() throws IOException {
		Path path =
			Paths.get(
				tempFile.getParent().toString(), 
				RandomStringUtils.randomNumeric(10));
		String linkPath = SymbolicLinkHelper.createSymbolicLink(
				path.toString(),
				tempFile.toString());
		path.toFile().deleteOnExit();
		assertTrue(SymbolicLinkHelper.isSymbolicLink(linkPath));
	}
	
	/**
	 * Test move.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testMove() throws IOException {
		Path path =
			Paths.get(
				tempFile.getParent().toString(), 
				RandomStringUtils.randomNumeric(10));
		String linkPath = SymbolicLinkHelper.move(
				path.toString(),
				tempFile.toString());
		path.toFile().deleteOnExit();
		assertTrue(SymbolicLinkHelper.isSymbolicLink(linkPath));
	}
	
	/**
	 * Test is symbolic link capable.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Test
	public void testIsSymbolicLinkCapable() throws IOException {
		assertTrue(SymbolicLinkHelper.isSymbolicLinkCapable());
	}
	
	/**
	 * Creates the temp link.
	 *
	 * @return the path
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private Path createTempLink() throws IOException {
		Path tempLink =
			Files.createSymbolicLink(
				Paths.get(tempFile.getParent().toString(), 
						  RandomStringUtils.randomNumeric(10)),
				tempFile);
		tempLink.toFile().deleteOnExit();
		return tempLink;
	}
}