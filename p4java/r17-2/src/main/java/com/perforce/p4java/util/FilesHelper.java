package com.perforce.p4java.util;

import com.perforce.p4java.Log;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.server.ServerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Files helper class with generally useful methods.
 */
public class FilesHelper {

	/**
	 * Copy source file to destination file.
	 */
	public static boolean copy(File source, File destination) throws IOException {
		boolean copied = false;
		if (source != null && destination != null) {
		    FileInputStream reader = null;
		    FileOutputStream writer = null;
			try {
				ISystemFileCommandsHelper helper = ServerFactory.getRpcFileSystemHelper();
				if (helper == null) {
					helper = SysFileHelperBridge.getSysFileCommands();
				}
				if (helper != null) {
					helper.setWritable(destination.getAbsolutePath(), true);
				}
				reader = new FileInputStream(source);
				writer = new FileOutputStream(destination, false);
				long targetCount = reader.getChannel().size();
				long transferCount = writer.getChannel().transferFrom(reader.getChannel(), 0, targetCount);
				copied = transferCount == targetCount;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.warn("reader close error: " + e.getLocalizedMessage());
						Log.exception(e);
					}
				}
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						Log.warn("writer close error: " + e.getLocalizedMessage());
						Log.exception(e);
					}
				}
			}
		}
		return copied;
	}

	/**
	 * Create all directories, including nonexistent parent directories.
	 */
	public static boolean mkdirs(File file) {
		if (file != null) {
			String parent = file.getParent();
			if (parent != null) {
				File parentDir = new File(parent);
				if (!parentDir.exists()) {
					if(parentDir.mkdirs()) {
						return true;
					} else {
						return parentDir.exists();
					}
				}
			}
			return true;
		}
		
		return false;
	}
}
