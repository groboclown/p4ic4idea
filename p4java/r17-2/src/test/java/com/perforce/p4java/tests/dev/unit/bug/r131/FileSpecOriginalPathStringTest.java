package com.perforce.p4java.tests.dev.unit.bug.r131;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
/**
 * Test FileSpec.getOriginalPath().
 */
@RunWith(JUnitPlatform.class)
@Jobs({ "job061945" })
@TestId("Bug131_FileSpecOriginalPathStringTest")
public class FileSpecOriginalPathStringTest extends P4JavaTestCase {
	/**
	 * Test FileSpec.getOriginalPath().
	 */
	@Test
	public void testOriginalPath() {
		FileSpec fs = new FileSpec();
		fs.setOriginalPath(new FilePath(FilePath.PathType.ORIGINAL, "//depot/project/fix.txt"));
		assertThat("Testing fs.getOriginalPath():",
				fs.getOriginalPath().getPathString(),
				is("//depot/project/fix.txt"));

	}
}
