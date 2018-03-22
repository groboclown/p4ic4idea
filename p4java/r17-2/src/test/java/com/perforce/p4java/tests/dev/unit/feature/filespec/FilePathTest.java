/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.filespec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.impl.generic.core.file.FilePath.PathType;
import com.perforce.p4java.impl.generic.core.file.PathAnnotations;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple self-contained FilePathTest tests.
 */
@TestId("FilePathTest01")
@Standalone
public class FilePathTest extends P4JavaTestCase {

	public FilePathTest() {
	}
	
	@Test
	public void testPathConstructors() {
		FilePath filePath = new FilePath();
		
		assertNotNull("Null path type in fresh PathType",
				filePath.getPathType());
		assertEquals("Path type not UNKNOWN in fresh FilePath",
				filePath.getPathType(), PathType.UNKNOWN);
		assertEquals("Path string not null in fresh FilePath",
				filePath.getPathString(), null);
		
		filePath = new FilePath(PathType.DEPOT, "//depot/dev/hreid/test.txt");
		assertNotNull("Null path type in fresh PathType",
				filePath.getPathType());
		assertNotNull("Null path string in fresh PathType",
				filePath.getPathString());
		assertEquals("Path type not DEPOT in fresh FilePath",
				filePath.getPathType(), PathType.DEPOT);
		assertEquals("Path string not initialized in fresh FilePath",
				filePath.getPathString(), "//depot/dev/hreid/test.txt");
		
		filePath = new FilePath(PathType.CLIENT, "//xyz/dev/hreid/test.txt#12,#15");
		assertNotNull("Null path type in fresh PathType",
				filePath.getPathType());
		assertNotNull("Null path string in fresh PathType",
				filePath.getPathString());
		assertEquals("Path type not CLIENT in fresh FilePath",
				filePath.getPathType(), PathType.CLIENT);
		assertEquals("Path string not correctly initialized in fresh FilePath",
				filePath.getPathString(), "//xyz/dev/hreid/test.txt");
		
		filePath = new FilePath(PathType.CLIENT, "//xyz/dev/hreid/#test.txt");
		assertNotNull("Null path type in fresh PathType",
				filePath.getPathType());
		assertNotNull("Null path string in fresh PathType",
				filePath.getPathString());
		assertEquals("Path type not CLIENT in fresh FilePath",
				filePath.getPathType(), PathType.CLIENT);
		assertEquals("Path string not correctly initialized in fresh FilePath",
				filePath.getPathString(), "//xyz/dev/hreid/#test.txt");
		
		filePath = new FilePath(PathType.CLIENT, "//xyz/dev/hreid/#test.txt#12,#head");
		assertNotNull("Null path type in fresh PathType",
				filePath.getPathType());
		assertNotNull("Null path string in fresh PathType",
				filePath.getPathString());
		assertEquals("Path type not CLIENT in fresh FilePath",
				filePath.getPathType(), PathType.CLIENT);
		assertEquals("Path string not correctly initialized in fresh FilePath",
				filePath.getPathString(), "//xyz/dev/hreid/#test.txt");
		
		filePath = new FilePath(PathType.CLIENT, "#12,#head");
		assertNotNull("Null path type in fresh PathType",
				filePath.getPathType());
		assertNotNull("Null path string in fresh PathType",
				filePath.getPathString());
		assertEquals("Path type not CLIENT in fresh FilePath",
				filePath.getPathType(), PathType.CLIENT);
		assertEquals("Path string not correctly initialized in fresh FilePath",
				filePath.getPathString(), "");
	}
	
	@Test
	public void testAnnotations() {
		String testString1 = "//depot/dev/hreid/test.txt#12,#15";
		FilePath filePath = new FilePath(PathType.DEPOT, testString1);
		
		assertNotNull(filePath.getPathString());
		assertEquals("//depot/dev/hreid/test.txt", filePath.getPathString()); 
		assertEquals(testString1, filePath.annotate(new PathAnnotations(testString1)));
	}
}