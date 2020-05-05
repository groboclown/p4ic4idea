/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core.file;

import com.perforce.p4java.impl.generic.core.file.FileSpec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * A class used to provide generally-useful Perforce filespec-related
 * static methods.
 */

public class FileSpecBuilder {
	private FileSpecBuilder() { /* util */ }

	/**
	 * Given a list of file paths (which might include revision or label specs, etc.),
	 * return a corresponding list of file specs. Returns null if filePaths is null; skips
	 * any null element of the list.
	 *
	 * @param filePaths list of path strings
	 * @return non-null list of filespecs
	 */
	public static List<IFileSpec> makeFileSpecList(@Nullable List<String> filePaths) {
		List<IFileSpec> fileSpecs = new ArrayList<>();
		if (nonNull(filePaths)) {
			for (String filePath : filePaths) {
				if (isNotBlank(filePath)) {
					IFileSpec fileSpec = new FileSpec(filePath);
					fileSpecs.add(fileSpec);
				}
			}
		}

		return fileSpecs;
	}

	/**
	 * Given an array of file paths (which might include revision or label specs, etc.),
	 * return a corresponding list of file specs. Returns null if filePaths is null; skips
	 * any null element of the array.<p>
	 * <p>
	 * NOTE: use the 'FileSpecBuilder.makeFileSpecList(List<String> pathList)' method if
	 * you have a very large amount of file paths. The method with the 'List' parameter
	 * is more memory efficient, since an array keeps data in a contiguous chunk of memory.
	 *
	 * @param filePaths array of path strings
	 * @return non-null list of filespecs
	 */
	public static List<IFileSpec> makeFileSpecList(String... filePaths) {
		List<IFileSpec> fileSpecs = new ArrayList<>();
		if (filePaths != null) {
			for (String path : filePaths) {
				if (isNotBlank(path)) {
					fileSpecs.add(new FileSpec(path));
				}
			}
		}
		return fileSpecs;
	}

	/**
	 * Given a list of file specs, return a list of the valid file specs in that list.
	 * "Valid" here means a) non-null, and b) getOpStatus() returns VALID.
	 *
	 * @param fileSpecs candidate file specs
	 * @return non-null but possibly-empty list of valid file specs
	 */
	public static List<IFileSpec> getValidFileSpecs(@Nullable List<IFileSpec> fileSpecs) {
		List<IFileSpec> validFileSpecs = new ArrayList<>();

		if (nonNull(fileSpecs)) {
			for (IFileSpec fileSpec : fileSpecs) {
				if (nonNull(fileSpec) && (fileSpec.getOpStatus() == FileSpecOpStatus.VALID)) {
					validFileSpecs.add(fileSpec);
				}
			}
		}

		return validFileSpecs;
	}

	/**
	 * Given a list of file specs, return a list of the invalid file specs in that list.
	 * "Invalid" here means a) non-null, and b) getOpStatus() returns anything but VALID.
	 *
	 * @param fileSpecs candidate file specs
	 * @return non-null but possibly-empty list of invalid file specs
	 */

	public static List<IFileSpec> getInvalidFileSpecs(@Nullable List<IFileSpec> fileSpecs) {
		List<IFileSpec> invalidFileSpecs = new ArrayList<>();

		if (nonNull(fileSpecs)) {
			for (IFileSpec fileSpec : fileSpecs) {
				if (nonNull(fileSpec)
						&& fileSpec.getOpStatus() != FileSpecOpStatus.VALID
						&& fileSpec.getOpStatus() != FileSpecOpStatus.INFO) {
					invalidFileSpecs.add(fileSpec);
				}
			}
		}

		return invalidFileSpecs;
	}
}
