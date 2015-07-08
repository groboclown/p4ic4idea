/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core.file;

import java.util.ArrayList;
import java.util.List;

import com.perforce.p4java.impl.generic.core.file.FileSpec;

/**
 * A class used to provide generally-useful Perforce filespec-related
 * static methods.
 *
 *
 */

public class FileSpecBuilder {
	
	/**
	 * Given a list of file paths (which might include revision or label specs, etc.),
	 * return a corresponding list of file specs. Returns null if pathList is null; skips
	 * any null element of the list.
	 * 
	 * @param pathList list of path strings
	 * @return possibly-null (or empty) list of filespecs
	 */
	
	public static List<IFileSpec> makeFileSpecList(List<String> pathList) {
		List<IFileSpec> specList = null;
		if (pathList != null) {
			specList = new ArrayList<IFileSpec>();
			
			for (String path : pathList) {
				if (path != null) {
					specList.add(new FileSpec(path));
				}
			}
		}
		
		return specList;
	}

	/**
	 * Given an array of file paths (which might include revision or label specs, etc.),
	 * return a corresponding list of file specs. Returns null if pathArray is null; skips
	 * any null element of the array.<p>
	 * 
	 * NOTE: use the 'FileSpecBuilder.makeFileSpecList(List<String> pathList)' method if
	 * you have a very large amount of file paths. The method with the 'List' parameter
	 * is more memory efficient, since an array keeps data in a contiguous chunk of memory.
	 * 
	 * @param pathArray array of path strings
	 * @return possibly-null (or empty) list of filespecs
	 */
	
	public static List<IFileSpec> makeFileSpecList(String[] pathArray) {
		List<IFileSpec> specList = null;
		if (pathArray != null) {
			specList = new ArrayList<IFileSpec>();
			
			for (String path : pathArray) {
				if (path != null) {
					specList.add(new FileSpec(path));
				}
			}
		}
		
		return specList;
	}
	
	/**
	 * Create a list containing a single file spec created from the specified
	 * path.
	 * 
	 * @param path
	 * @return non-null but possibly empty list of filespecs
	 */
	public static List<IFileSpec> makeFileSpecList(String path) {
		return makeFileSpecList(new String[] { path });
	}
	
	/**
	 * Given a list of file specs, return a list of the valid file specs in that list.
	 * "Valid" here means a) non-null, and b) getOpStatus() returns VALID.
	 * 
	 * @param fileSpecs candidate file specs
	 * @return non-null but possibly-empty list of valid file specs
	 */
	
	public static List<IFileSpec> getValidFileSpecs(List<IFileSpec> fileSpecs) {
		List <IFileSpec> validList = new ArrayList<IFileSpec>();
		
		if (fileSpecs != null) {
			for (IFileSpec fSpec : fileSpecs) {
				if ((fSpec != null) && (fSpec.getOpStatus() == FileSpecOpStatus.VALID)) {
					validList.add(fSpec);
				}
			}
		}
		
		return validList;
	}
	
	/**
	 * Given a list of file specs, return a list of the invalid file specs in that list.
	 * "Invalid" here means a) non-null, and b) getOpStatus() returns anything but VALID.
	 * 
	 * @param fileSpecs candidate file specs
	 * @return non-null but possibly-empty list of invalid file specs
	 */
	
	public static List<IFileSpec> getInvalidFileSpecs(List<IFileSpec> fileSpecs) {
		List <IFileSpec> invalidList = new ArrayList<IFileSpec>();
		
		if (fileSpecs != null) {
			for (IFileSpec fSpec : fileSpecs) {
				if ((fSpec != null) && (fSpec.getOpStatus() != FileSpecOpStatus.VALID)) {
					invalidList.add(fSpec);
				}
			}
		}
		
		return invalidList;
	}
}
