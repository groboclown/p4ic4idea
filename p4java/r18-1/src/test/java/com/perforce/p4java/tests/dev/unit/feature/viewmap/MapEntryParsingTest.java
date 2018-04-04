/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.viewmap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.perforce.p4java.core.IMapEntry.EntryType;
import com.perforce.p4java.impl.generic.core.MapEntry;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests the MapEntry parseViewMappingString and toString methods.
 * mapStr12 and associated gubbins added to test Job035374A.
 * 
 * @testid MapEntryParsing01
 * @job job035374A
 */

@Jobs({"Job035374A"})
@TestId("MapEntryParsing01")
@Standalone
public class MapEntryParsingTest extends P4JavaTestCase {
	
	private static final String mapStr01 = "//depot/dev/abc.txt //depot/dev/def.txt";
	private static final String mapStr02 = "-//depot/dev/abc/... //depot/dev/def/...";
	private static final String mapStr03 = "+//depot/dev/abc.txt //depot/dev/def.txt";
	private static final String mapStr04 = "\"//depot/dev/abc txt/...\" //depot/dev/def.txt";
	private static final String mapStr05 = "//depot/dev/abc.txt \"//depot/dev/def.txt\"";
	private static final String mapStr06 = "//depot/dev/abc.txt \"//depot/dev def.txt\"";
	private static final String mapStr07 = "\"-//depot/dev/abc.txt\" //depot/dev/def.txt";
	private static final String mapStr08 = "\"-//depot/dev abc.txt\" //depot/dev/def.txt";
	private static final String mapStr09 = "//depot/dev/abc.txt";
	private static final String mapStr10 = "\"//depot/dev abc.txt\"";
	private static final String mapStr11 = "\"-//depot/dev abc.txt\"";
	private static final String mapStr12 = "\"//depot/ratna/Java9.2 TimeLapse View test/...\""
									+ " \"//depot/ratna/Java9.2 TimeLapse View test2/...\"";
	private static final String mapStr13 = "//depot/...          //liz_play_win7/depot/...";
	private static final String mapStr14 = "\"//depot/...         \" //liz_play_win7/depot/...";

	public MapEntryParsingTest() {
	}

	@Test
	public void testBasics() throws Exception {

		MapEntry entry = new MapEntry(1, mapStr01);
		assertEquals(1, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/dev/abc.txt", entry.getLeft());
		assertEquals("//depot/dev/def.txt", entry.getRight());
		assertEquals(mapStr01, entry.toString());
		
		entry = new MapEntry(9, mapStr02);
		assertEquals(9, entry.getOrder());
		assertEquals(EntryType.EXCLUDE, entry.getType());
		assertEquals("//depot/dev/abc/...", entry.getLeft());
		assertEquals("//depot/dev/def/...", entry.getRight());
		assertEquals(mapStr02, entry.toString());
		
		entry = new MapEntry(2, mapStr03);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.OVERLAY, entry.getType());
		assertEquals("//depot/dev/abc.txt", entry.getLeft());
		assertEquals("//depot/dev/def.txt", entry.getRight());
		assertEquals(mapStr03, entry.toString());
		
		entry = new MapEntry(2, mapStr04);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/dev/abc txt/...", entry.getLeft());
		assertEquals("//depot/dev/def.txt", entry.getRight());
		assertEquals(mapStr04, entry.toString());
		
		entry = new MapEntry(2, mapStr05);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/dev/abc.txt", entry.getLeft());
		assertEquals("//depot/dev/def.txt", entry.getRight());
		assertEquals("//depot/dev/abc.txt //depot/dev/def.txt", entry.toString());
		
		entry = new MapEntry(2, mapStr06);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/dev/abc.txt", entry.getLeft());
		assertEquals("//depot/dev def.txt", entry.getRight());
		assertEquals(mapStr06, entry.toString());
		
		entry = new MapEntry(2, mapStr07);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.EXCLUDE, entry.getType());
		assertEquals("//depot/dev/abc.txt", entry.getLeft());
		assertEquals("//depot/dev/def.txt", entry.getRight());
		assertEquals("-//depot/dev/abc.txt //depot/dev/def.txt", entry.toString());
		
		entry = new MapEntry(2, mapStr08);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.EXCLUDE, entry.getType());
		assertEquals("//depot/dev abc.txt", entry.getLeft());
		assertEquals("//depot/dev/def.txt", entry.getRight());
		assertEquals(mapStr08, entry.toString());
		
		entry = new MapEntry(2, mapStr09);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/dev/abc.txt", entry.getLeft());
		assertEquals(null, entry.getRight());
		assertEquals(mapStr09, entry.toString());
		
		entry = new MapEntry(2, mapStr10);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/dev abc.txt", entry.getLeft());
		assertEquals(null, entry.getRight());
		assertEquals(mapStr10, entry.toString());
		
		entry = new MapEntry(2, mapStr11);
		assertEquals(2, entry.getOrder());
		assertEquals(EntryType.EXCLUDE, entry.getType());
		assertEquals("//depot/dev abc.txt", entry.getLeft());
		assertEquals(null, entry.getRight());
		assertEquals(mapStr11, entry.toString());
		
		entry = new MapEntry(0, mapStr12);
		assertEquals(0, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/ratna/Java9.2 TimeLapse View test/...", entry.getLeft());
		assertEquals("//depot/ratna/Java9.2 TimeLapse View test2/...", entry.getRight());
		assertEquals(mapStr12, entry.toString());
		
		entry = new MapEntry(0, mapStr13);
		assertEquals(0, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/...", entry.getLeft());
		assertEquals("//liz_play_win7/depot/...", entry.getRight());
		assertEquals("//depot/..." + " " + "//liz_play_win7/depot/...", entry.toString());
		
		entry = new MapEntry(0, mapStr14);
		assertEquals(0, entry.getOrder());
		assertEquals(EntryType.INCLUDE, entry.getType());
		assertEquals("//depot/...         ", entry.getLeft());
		assertEquals("//liz_play_win7/depot/...", entry.getRight(true));
		assertEquals("\"//depot/...         \"", entry.getLeft(true));
	}
}
