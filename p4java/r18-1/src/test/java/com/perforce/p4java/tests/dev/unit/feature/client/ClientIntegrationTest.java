package com.perforce.p4java.tests.dev.unit.feature.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.core.file.IntegrationOptions;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;


/**
 * 
 * This class lightly exercises the ClientIntegration tasks in p4Java starting with 
 * IntegrationOptions. This will be added to as we run across opportunities to add tests.
 */
@TestId("ClientIntegrationTest01")
public class ClientIntegrationTest extends P4JavaTestCase {

	/**
	 * IntegrationOptions default constructor. Ensures all values
	 * are initially false.
	 */
	@Test
	public void testDefaultIntegrationOptions() throws Exception {

						
		try {
			debugPrintTestName();
			
			IntegrationOptions intOptions = new IntegrationOptions();
			
			boolean baselessMergeVal = intOptions.isBaselessMerge();
			boolean bidirectionalIntegVal = intOptions.isBidirectionalInteg();
			boolean displayBaseDetailsVal = intOptions.isDisplayBaseDetails();
			boolean dontCopyToClientVal = intOptions.isDontCopyToClient();
			boolean forceVal = intOptions.isForce();
			boolean propagateTypeVal = intOptions.isPropagateType();
			boolean reverseMapping = intOptions.isReverseMapping();
			boolean useHaveRevVal = intOptions.isUseHaveRev();
						
			assertFalse("Default value expected to be false.", baselessMergeVal);
			assertFalse("Default value expected to be false.", bidirectionalIntegVal);
			assertFalse("Default value expected to be false.", displayBaseDetailsVal);
			assertFalse("Default value expected to be false.", dontCopyToClientVal);
			assertFalse("Default value expected to be false.", forceVal);
			assertFalse("Default value expected to be false.", propagateTypeVal);
			assertFalse("Default value expected to be false.", reverseMapping);
			assertFalse("Default value expected to be false.", useHaveRevVal);
						
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}

	/**
	 * IntegrationOptions Explicit Value constructor. Individually set each value to 
	 * true and verify that it returns true.
	 */
	@Test
	public void testIntegrationOptionsSettersGetters() throws Exception {

						
		try {
			debugPrintTestName();
			
			String [] deletedOptions = {
					"d"
			};

			IntegrationOptions intOptions = new IntegrationOptions();
			
			intOptions.setBaselessMerge(true);
			intOptions.setBidirectionalInteg(true);
			intOptions.setDisplayBaseDetails(true);
			intOptions.setDontCopyToClient(true);
			intOptions.setForce(true);
			intOptions.setPropagateType(true);
			intOptions.setReverseMapping(true);
			intOptions.setUseHaveRev(true);
			intOptions.setDeletedOptions(deletedOptions);

			
			assertTrue("Value expected to be true.", intOptions.isBaselessMerge());
			assertTrue("Value expected to be true.", intOptions.isBidirectionalInteg());
			assertTrue("Value expected to be true.", intOptions.isDisplayBaseDetails());
			assertTrue("Value expected to be true.", intOptions.isDontCopyToClient());
			assertTrue("Value expected to be true.", intOptions.isForce());
			assertTrue("Value expected to be true.", intOptions.isPropagateType());
			assertTrue("Value expected to be true.", intOptions.isReverseMapping());
			assertTrue("Value expected to be true.", intOptions.isUseHaveRev());
			assertEquals("Array lengths for deletedOptions should match.", deletedOptions.length, intOptions.getDeletedOptions().length);
			
			String [] actDeletedOptions = intOptions.getDeletedOptions();
			assertEquals("Array values for deletedOptions should match.", deletedOptions[0], actDeletedOptions[0]);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}


	/**
	 * IntegrationOptions Explicit Value constructor. Individually set each value to 
	 * true and verify that it returns true.
	 */
	@Test
	public void testIntegrationOptionsExplicitValueConstructor() throws Exception {

						
		try {
			debugPrintTestName();
			
			String [] deletedOptions = {
					"d"
			};

			IntegrationOptions intOptions = new IntegrationOptions(true, true, true, true,
					true, true, true, true, deletedOptions);
			
			assertTrue("Value expected to be true.", intOptions.isBaselessMerge());
			assertTrue("Value expected to be true.", intOptions.isBidirectionalInteg());
			assertTrue("Value expected to be true.", intOptions.isDisplayBaseDetails());
			assertTrue("Value expected to be true.", intOptions.isDontCopyToClient());
			assertTrue("Value expected to be true.", intOptions.isForce());
			assertTrue("Value expected to be true.", intOptions.isPropagateType());
			assertTrue("Value expected to be true.", intOptions.isReverseMapping());
			assertTrue("Value expected to be true.", intOptions.isUseHaveRev());
			assertEquals("Array lengths for deletedOptions should match.", deletedOptions.length, intOptions.getDeletedOptions().length);
			
			String [] actDeletedOptions = intOptions.getDeletedOptions();
			assertEquals("Array values for deletedOptions should match.", deletedOptions[0], actDeletedOptions[0]);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}

	
	/**
	 * FIXME Tests getDeletedOptions(). Need to verify something here.
	 */
	@Test
	public void testGetDeletedOptions() throws Exception {

		String [] deletedOptions;				
		try {
			debugPrintTestName();
			
			IntegrationOptions intOptions = new IntegrationOptions();
			
			deletedOptions = intOptions.getDeletedOptions();
			debugPrint("DeletedOptions: " + deletedOptions);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}

	/**
	 * Tests setDeletedOptions() to make sure values remain set.
	 * deletedOptions - if non-null, must contain zero or more non-null entries with 
	 * individual values "d", "Dt", "Ds", or "Di"; null, inconsistent, or conflicting 
	 * option values here will have unspecified and potentially incorrect effects.
	 */
	@Test
	public void testSetDeletedOptionsLittleD() throws Exception {

		final String [] newOptions = {
			"d"	
		};
		String [] returnedOptions;
		
		try {
			debugPrintTestName();
			
			IntegrationOptions intOptions = new IntegrationOptions();
			
			intOptions.setDeletedOptions(newOptions);
			
			returnedOptions = intOptions.getDeletedOptions();
			
			assertEquals("Wrong number of DeletedOptions returned.", newOptions.length, returnedOptions.length);
			debugPrint("Num DeletedOptions: " + newOptions.length, "Num returnedOptions: " + returnedOptions.length);
			assertNotNull("DeletedOptions should not be Null.", returnedOptions);
			debugPrint("DeletedOptions: ", newOptions[0], returnedOptions[0]);
			assertEquals("DeletedOptions should be " + newOptions[0], newOptions[0], returnedOptions[0]);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}

	/**
	 * Tests setDeletedOptions() to make sure values remain set.
	 * deletedOptions - if non-null, must contain zero or more non-null entries with 
	 * individual values "d", "Dt", "Ds", or "Di"; null, inconsistent, or conflicting 
	 * option values here will have unspecified and potentially incorrect effects.
	 */
	@Test
	public void testSetDeletedOptionsDt() throws Exception {

		final String [] newOptions = {
			"Dt"	
		};
		String [] returnedOptions;
		
		try {
			debugPrintTestName();
			
			IntegrationOptions intOptions = new IntegrationOptions();
			
			intOptions.setDeletedOptions(newOptions);
			
			returnedOptions = intOptions.getDeletedOptions();
			
			assertEquals("Wrong number of DeletedOptions returned.", newOptions.length, returnedOptions.length);
			debugPrint("Num DeletedOptions: " + newOptions.length, "Num returnedOptions: " + returnedOptions.length);
			assertNotNull("DeletedOptions should not be Null.", returnedOptions);
			debugPrint("DeletedOptions: ", newOptions[0], returnedOptions[0]);
			assertEquals("DeletedOptions should be " + newOptions[0], newOptions[0], returnedOptions[0]);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}


	/**
	 * Tests setDeletedOptions() to make sure values remain set.
	 * deletedOptions - if non-null, must contain zero or more non-null entries with 
	 * individual values "d", "Dt", "Ds", or "Di"; null, inconsistent, or conflicting 
	 * option values here will have unspecified and potentially incorrect effects.
	 */
	@Test
	public void testSetDeletedOptionsLittleDs() throws Exception {

		final String [] newOptions = {
			"Ds"	
		};
		String [] returnedOptions;
		
		try {
			debugPrintTestName();
			
			IntegrationOptions intOptions = new IntegrationOptions();
			
			intOptions.setDeletedOptions(newOptions);
			
			returnedOptions = intOptions.getDeletedOptions();
			
			assertEquals("Wrong number of DeletedOptions returned.", newOptions.length, returnedOptions.length);
			debugPrint("Num DeletedOptions: " + newOptions.length, "Num returnedOptions: " + returnedOptions.length);
			assertNotNull("DeletedOptions should not be Null.", returnedOptions);
			debugPrint("DeletedOptions: ", newOptions[0], returnedOptions[0]);
			assertEquals("DeletedOptions should be " + newOptions[0], newOptions[0], returnedOptions[0]);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}

	/**
	 * Tests setDeletedOptions() to make sure values remain set.
	 * deletedOptions - if non-null, must contain zero or more non-null entries with 
	 * individual values "d", "Dt", "Ds", or "Di"; null, inconsistent, or conflicting 
	 * option values here will have unspecified and potentially incorrect effects.
	 */
	@Test
	public void testSetDeletedOptionsDi() throws Exception {

		final String [] newOptions = {
			"Di"	
		};
		String [] returnedOptions;
		
		try {
			debugPrintTestName();
			
			IntegrationOptions intOptions = new IntegrationOptions();
			
			intOptions.setDeletedOptions(newOptions);
			
			returnedOptions = intOptions.getDeletedOptions();
			
			assertEquals("Wrong number of DeletedOptions returned.", newOptions.length, returnedOptions.length);
			debugPrint("Num DeletedOptions: " + newOptions.length, "Num returnedOptions: " + returnedOptions.length);
			assertNotNull("DeletedOptions should not be Null.", returnedOptions);
			debugPrint("DeletedOptions: ", newOptions[0], returnedOptions[0]);
			assertEquals("DeletedOptions should be " + newOptions[0], newOptions[0], returnedOptions[0]);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}

	/**
	 * Tests setDeletedOptions() to make sure values remain set.
	 * deletedOptions - if non-null, must contain zero or more non-null entries with 
	 * individual values "d", "Dt", "Ds", or "Di"; null, inconsistent, or conflicting 
	 * option values here will have unspecified and potentially incorrect effects.
	 */
	@Test
	public void testSetDeletedOptionsDsDtDi() throws Exception {

		final String [] newOptions = {
			"Ds Dt Di"	
		};
		String [] returnedOptions;
		
		try {
			debugPrintTestName();
			
			IntegrationOptions intOptions = new IntegrationOptions();
			
			intOptions.setDeletedOptions(newOptions);
			
			returnedOptions = intOptions.getDeletedOptions();
			
			assertEquals("Wrong number of DeletedOptions returned.", newOptions.length, returnedOptions.length);
			debugPrint("Num DeletedOptions: " + newOptions.length, "Num returnedOptions: " + returnedOptions.length);
			assertNotNull("DeletedOptions should not be Null.", returnedOptions);
			debugPrint("DeletedOptions: ", newOptions[0], returnedOptions[0]);
			assertEquals("DeletedOptions should be " + newOptions[0], newOptions[0], returnedOptions[0]);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} 
	}

}
