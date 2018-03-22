/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.perforce.p4java.Log;


/**
 * A simple extension to the normal FileInputStream to allow us to
 * subvert a few things like close() for our own nefarious purposes.
 * 
 *
 */

public class TempFileInputStream extends FileInputStream {
	
	private File tmpFile = null;
	
	public TempFileInputStream(File file)
			throws FileNotFoundException {
		// p4ic4idea: bug #114: there's the rare occasion when the
		// directory has been removed underneath the file creation.
		super(ensureDirExists(file));
		this.tmpFile = file;
	}

	// p4ic4idea: patch for #114
	private static File ensureDirExists(final File file) throws FileNotFoundException {
		final File parent = file.getParentFile();
		if (! parent.exists()) {
			boolean created = parent.mkdirs();
			if (!created) {
				throw new FileNotFoundException("Parent directory was not created for temp file " + file);
			}
		}
		return file;
	}

	public void close() throws IOException {
		super.close();
		
		if ((tmpFile != null) && tmpFile.exists()) {
			if (!tmpFile.delete()) {
				Log.warn("delete failed in TempFileInputStream.close( ) for file: "
						+ tmpFile.getPath() + "(unknown cause)");
			}
		}
	}

	public File getTmpFile() {
		return tmpFile;
	}

	public void setTmpFile(File tmpFile) {
		this.tmpFile = tmpFile;
	}
}
