/*
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Handle the checking of patterns in ignore files.
 */
public class ClientIgnoreChecker {

	/** The client root. */
	private String clientRoot = null;
	
	/** The ignore file name. */
	private String ignoreFileName = null;

	/** The charset. */
	private Charset charset = null;
	
	/**
	 * Instantiates a new ignore file checker.
	 * 
	 * @param clientRoot
	 *            the client root
	 * @param ignoreFileName
	 *            the ignore file name
	 * @param charset
	 *            the charset
	 */
	public ClientIgnoreChecker(String clientRoot, String ignoreFileName, Charset charset) {
		if (clientRoot == null) {
			throw new IllegalArgumentException(
					"Null client root directory passed to IgnoreFileChecker constructor.");
		}
		if (ignoreFileName == null) {
			throw new IllegalArgumentException(
					"Null ignore file passed to IgnoreFileChecker constructor.");
		}
		if (charset == null) {
			throw new IllegalArgumentException(
					"Null charset passed to IgnoreFileChecker constructor.");
		}
		this.clientRoot = clientRoot;
		this.ignoreFileName = ignoreFileName;
		this.charset = charset;
	}

	/**
	 * Check for an ignore match of the file.
	 * 
	 * @param file
	 *            the file
	 * @return true, if successful
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public boolean match(File file) throws FileNotFoundException, IOException {
		if (file != null) {
			if (checkIgnoreFiles(file)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check all ignore files up to the client root directory.
	 * 
	 * @param file
	 *            the file
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean checkIgnoreFiles(File file) throws IOException {
		if (file != null) {
			// Signal for inverse match
			Negate negate = this.new Negate();
	
			File clientRootDir = new File(clientRoot);
			File fileDir = file;
			do {
				fileDir = fileDir.getParentFile();
				if (fileDir != null) {
					File ignoreFile = new File(fileDir, ignoreFileName);
					if (ignoreFile.exists()) {
						if (checkIgnoreFile(ignoreFile, fileDir, file, negate)) {
							// Inverse match
							if (negate.isMatch()) {
								return false;
							}
							return true;
						}
					}
				}
			} while (fileDir != null && !fileDir.getAbsoluteFile().equals(clientRootDir));
		}

		return false;
	}

	/**
	 * Inversely loop through patterns in an ignore file and check for a match.
	 * 
	 * @param ignoreFile
	 *            the ignore file
	 * @param currentDir
	 *            the current directory
	 * @param file
	 *            the file
	 * @param negate
	 *            the negate
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean checkIgnoreFile(File ignoreFile, File currentDir, File file, Negate negate)
			throws IOException {

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					ignoreFile), this.charset));
			ArrayList<String> list = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				list.add(line);
			}

			// Reverse the lines
			Collections.reverse(list);

			for (String entry : list) {
				if (checkIgnorePattern(entry, currentDir, file, negate)) {
					return true;
				}
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}

		return false;
	}

	/**
	 * Check for a pattern match.
	 * 
	 * @param pattern
	 *            the pattern
	 * @param currentDir
	 *            the current directory
	 * @param file
	 *            the file
	 * @param negate
	 *            the negate
	 * @return true, if successful
	 */
	private boolean checkIgnorePattern(String pattern, File currentDir, File file, Negate negate) {

		boolean wildcard = false;
		boolean negation = false;

		if (file == null) {
			return false;
		}

		if (pattern == null) {
			return false;
		}

		pattern = pattern.trim();

		if (pattern.startsWith("#")) {
			return false;
		}

		// Check for negation
		if (pattern.startsWith("!")) {
			negation = true;
			pattern = pattern.substring(1);
		}

		if (pattern.length() == 0) {
			return false;
		}

		// Check for wildcard
		if (pattern.contains("*")) {
			wildcard = true;
		}
		
		// Match file name only
		String path = file.getName();
		
		// Match file name or path
		if (!wildcard) {
			path = file.getAbsolutePath().substring(currentDir.getAbsolutePath().length());
			path += File.separator;
			pattern = "*" + File.separator + pattern;
			pattern += File.separator + "*";
		}
		
		// Escape '\', '.' and '*'
		pattern = pattern.replace("\\", "\\\\").replace(".", "\\.").replace("*", ".*");

		// Match pattern
		if (path.matches(pattern)) {
			if (negation) {
				negate.setMatch(true);
			}

			return true;
		}

		return false;
	}

	/**
	 * Signal for negate pattern.
	 */
	private class Negate {
		
		/** The match. */
		private boolean match = false;

		/**
		 * Checks if is match.
		 * 
		 * @return true, if is match
		 */
		public boolean isMatch() {
			return match;
		}

		/**
		 * Sets the match.
		 * 
		 * @param match
		 *            the new match
		 */
		public void setMatch(boolean match) {
			this.match = match;
		}
	}
}