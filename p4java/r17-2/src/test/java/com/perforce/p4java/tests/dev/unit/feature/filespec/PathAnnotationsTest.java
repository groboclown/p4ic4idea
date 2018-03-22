/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.filespec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.PathAnnotations;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests the PathAnnotations class for sanity and general ability
 * to parse path strings and represent complex annotations, etc. Very
 * little negative testing is done here.
 * 
 * @testid ClassPathAnnotations01
 * @job job035926
 */

@Jobs({"job035926"})
@Standalone
@TestId("ClassPathAnnotations01")
public class PathAnnotationsTest extends P4JavaTestCase {

	public PathAnnotationsTest() {
	}
	
	@Test
	public void testDefaultConstructor() {
		PathAnnotations pathAnnotations = new PathAnnotations();
		
		assertEquals("Bad default constructor default field value",
				pathAnnotations.getStartRevision(), IFileSpec.NO_FILE_REVISION);
		assertEquals("Bad default constructor default field value",
				pathAnnotations.getEndRevision(), IFileSpec.NO_FILE_REVISION);
		assertEquals("Bad default constructor default field value",
				pathAnnotations.getLabel(), null);
		assertEquals("Bad default constructor default field value",
				pathAnnotations.getDate(), null);
		assertEquals("Bad default constructor default field value",
				pathAnnotations.getChangelistId(), IChangelist.UNKNOWN);
	}
	
	@Test
	public void testNullStringArgToConstructor() {
		PathAnnotations pathAnnotations = new PathAnnotations((String) null);
		
		assertEquals("Bad null string constructor default field value",
				pathAnnotations.getStartRevision(), IFileSpec.NO_FILE_REVISION);
		assertEquals("Bad null string constructor default field value",
				pathAnnotations.getEndRevision(), IFileSpec.NO_FILE_REVISION);
		assertEquals("Bad null string constructor default field value",
				pathAnnotations.getLabel(), null);
		assertEquals("Bad null string constructor default field value",
				pathAnnotations.getDate(), null);
		assertEquals("Bad null string constructor default field value",
				pathAnnotations.getChangelistId(), IChangelist.UNKNOWN);
	}
	
	@Test
	public void testNullFileSpecArgToConstructor() {
		PathAnnotations pathAnnotations = new PathAnnotations((IFileSpec) null);
		
		assertEquals("Bad null filespec constructor default field value",
				pathAnnotations.getStartRevision(), IFileSpec.NO_FILE_REVISION);
		assertEquals("Bad null filespec constructor default field value",
				pathAnnotations.getEndRevision(), IFileSpec.NO_FILE_REVISION);
		assertEquals("Bad null filespec constructor default field value",
				pathAnnotations.getLabel(), null);
		assertEquals("Bad null filespec constructor default field value",
				pathAnnotations.getDate(), null);
		assertEquals("Bad null filespec constructor default field value",
				pathAnnotations.getChangelistId(), IChangelist.UNKNOWN);
	}
	
	@Test
	public void testFromString() {
		String testString1 = "//depot/text/xyz.tmp#12";
		String testString2 = "//depot/text/xyz.tmp#10,#12";
		String testString3 = "//depot/text/xyz.tmp@testLabel123";
		String testString4 = "//depot/text/xyz.tmp@1234";
		String testString5 = "//depot/text/xyz.tmp@2009/09/10";
		String testString6 = "//depot/text/xyz.tmp#head";
		String testString7 = "//depot/text/xyz.tmp#none";
		String testString8 = "//depot/text/xyz.tmp#have";
		String testString9 = "//depot/text/xyz.tmp#1,#have";
		String testString10 = "//depot/text/xyz.tmp#1,#head";
		String testString11 = "//depot/text/xyz.tmp#have,#head";
		String testString12 = "@1234";
		String testString13 = "#12";
		String testString14 = "//depot/text/xyz.tmp#have,#3";
		String testString15 = "//depot/text/xyz.tmp@testLabel123xYZ";
		String testString16 = "//depot/text/xyz.tmp@2009/09/10_label";	// job035926
		String testString17 = "//depot/text/xyz.tmp@Test_label.17";		// job035926
		
		PathAnnotations pathAnnotations = new PathAnnotations(testString1);
		assertEquals(12, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString2);
		assertEquals(10, pathAnnotations.getStartRevision());
		assertEquals(12, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString3);
		assertNotNull(pathAnnotations.getLabel());
		assertEquals("testLabel123", pathAnnotations.getLabel());
		
		pathAnnotations = new PathAnnotations(testString4);
		assertEquals(1234, pathAnnotations.getChangelistId());
		
		// Next test may need tuning for locale, etc.:
		pathAnnotations = new PathAnnotations(testString5);
		assertNotNull(pathAnnotations.getDate());
		assertTrue(pathAnnotations.getDate().toString().startsWith("Thu Sep 10"));
		
		pathAnnotations = new PathAnnotations(testString6);
		assertEquals(IFileSpec.NO_FILE_REVISION, pathAnnotations.getStartRevision());
		assertEquals(IFileSpec.HEAD_REVISION, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString7);
		assertEquals(IFileSpec.NO_FILE_REVISION, pathAnnotations.getStartRevision());
		assertEquals(IFileSpec.NONE_REVISION, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString8);
		assertEquals(IFileSpec.NO_FILE_REVISION, pathAnnotations.getStartRevision());
		assertEquals(IFileSpec.HAVE_REVISION, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString9);
		assertEquals(1, pathAnnotations.getStartRevision());
		assertEquals(IFileSpec.HAVE_REVISION, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString10);
		assertEquals(1, pathAnnotations.getStartRevision());
		assertEquals(IFileSpec.HEAD_REVISION, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString11);
		assertEquals(IFileSpec.HAVE_REVISION, pathAnnotations.getStartRevision());
		assertEquals(IFileSpec.HEAD_REVISION, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString12);
		assertEquals(1234, pathAnnotations.getChangelistId());
		
		pathAnnotations = new PathAnnotations(testString13);
		assertEquals(12, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString14);
		assertEquals(IFileSpec.HAVE_REVISION, pathAnnotations.getStartRevision());
		assertEquals(3, pathAnnotations.getEndRevision());
		
		pathAnnotations = new PathAnnotations(testString15);
		assertNotNull(pathAnnotations.getLabel());
		assertEquals("testLabel123xYZ", pathAnnotations.getLabel());
		
		pathAnnotations = new PathAnnotations(testString16);
		assertNotNull(pathAnnotations.getLabel());
		assertEquals("2009/09/10_label", pathAnnotations.getLabel());
		
		pathAnnotations = new PathAnnotations(testString17);
		assertNotNull(pathAnnotations.getLabel());
		assertEquals("Test_label.17", pathAnnotations.getLabel());
	}
	
	@Test
	public void testStripAnnotations() {
		String testString0 = "//depot/text/xyz.tmp";
		String testString1 = "//depot/text/xyz.tmp#12";
		String testString2 = "//depot/text/xyz.tmp#10,#12";
		String testString3 = "//depot/text/xyz.tmp@testLabel123";
		String testString4 = "//depot/text/xyz.tmp@1234";
		String testString5 = "//depot/text/xyz.tmp@2009/09/10";
		String testString6 = "//depot/text/xyz.tmp#head";
		String testString7 = "//depot/text/xyz.tmp#none";
		String testString8 = "//depot/text/xyz.tmp#have";
		String testString9 = "//depot/text/xyz.tmp#1,#have";
		String testString10 = "//depot/text/xyz.tmp#1,#head";
		String testString11 = "//depot/text/xyz.tmp#have,#head";
		String testString12 = "@1234";
		String testString13 = "#12";
		String testString14 = "//depot/text/xyz.tmp#have,#3";
		
		assertNull(PathAnnotations.stripAnnotations(null));
		assertNotNull(PathAnnotations.stripAnnotations(testString0));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString1));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString2));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString3));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString4));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString5));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString6));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString7));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString8));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString9));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString10));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString11));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString12));
		assertEquals("", PathAnnotations.stripAnnotations(testString12));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString13));
		assertEquals("", PathAnnotations.stripAnnotations(testString13));
		
		assertNotNull(PathAnnotations.stripAnnotations(testString14));
		assertEquals(testString0, PathAnnotations.stripAnnotations(testString0));
	}
	
	@Test
	public void testToString() {
		PathAnnotations pathAnnotations = new PathAnnotations();
		
		pathAnnotations.setEndRevision(12);
		assertNotNull(pathAnnotations.toString());
		assertEquals("#12", pathAnnotations.toString());
		
		pathAnnotations.setStartRevision(10);
		assertNotNull(pathAnnotations.toString());
		assertEquals("#10,#12", pathAnnotations.toString());
		
		// Ensure revisions override other fields:
		pathAnnotations.setLabel("testLabel123");
		pathAnnotations.setChangelistId(123);
		pathAnnotations.setDate(new Date());
		assertNotNull(pathAnnotations.toString());
		assertEquals("#10,#12", pathAnnotations.toString());
		
		pathAnnotations = new PathAnnotations();
		pathAnnotations.setChangelistId(123);
		assertNotNull(pathAnnotations.toString());
		assertEquals("@123", pathAnnotations.toString());
		
		pathAnnotations = new PathAnnotations();
		pathAnnotations.setLabel("testLabel123");
		assertNotNull(pathAnnotations.toString());
		assertEquals("@testLabel123", pathAnnotations.toString());
		
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");
		StringBuffer sBuf = new StringBuffer();
		sdf.format(date, sBuf, new FieldPosition(0));
		pathAnnotations = new PathAnnotations();
		pathAnnotations.setDate(date);
		assertNotNull(pathAnnotations.toString());
		assertEquals("@" + sBuf, pathAnnotations.toString());
	}
	
	@Test
	public void testEmbeddedMetadaChars() {
		String testString1 = "//depot/#test.tmp";
		String testString3 = "//depot/#test.tmp#12,#head";
		String testString4 = "//depot/#test.tmp@1234";
		
		PathAnnotations pathAnnotations = new PathAnnotations(testString1);
		assertNotNull(pathAnnotations.toString());
		assertEquals("", pathAnnotations.toString());
		
		pathAnnotations = new PathAnnotations(testString3);
		assertNotNull(pathAnnotations.toString());
		assertEquals("#12,#head", pathAnnotations.toString());
		
		pathAnnotations = new PathAnnotations(testString4);
		assertNotNull(pathAnnotations.toString());
		assertEquals("@1234", pathAnnotations.toString());
	}
}
