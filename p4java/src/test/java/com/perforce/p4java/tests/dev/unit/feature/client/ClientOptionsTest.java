package com.perforce.p4java.tests.dev.unit.feature.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 
 * The ClientOptionsTest class exercises the ClientOptions class. 
 * Constructors -
 * The test verifies the ClientOptions(), ClientOptions(String) and 
 * ClientOptions(boolean,boolean,boolean,boolean,boolean,boolean) default behavior as
 * well as behavior with various good and bad inputs. 
 * Setters and Getters -
 * Each flag is verified by setting the option through one of the constructors or setters
 * and then calling its "getter" isXXX() and/or toString() to see if the setting registered.
 * toString() -
 * toString is utilized in most of the tests. It is used in verification of the setters
 * and getters, but its return values are also tested with erroneous or oddly formatted input.
 */

@TestId("ClientOptionsTest01")
@Standalone
public class ClientOptionsTest extends P4JavaTestCase {
	
	String clientDir = defaultTestClientName + "_Dir" + File.separator + testId;
	
	/**
	 * The testClientOptionsDefaultConstructor verifies the assertion that the default
	 * constructor sets all fields to false. Verified by calling the "getters" separately
	 * for each method. 
	 */
	@Test
	public void testClientOptionsDefaultConstructor() {
		
		try {
			debugPrintTestName();

			ClientOptions clientOpts = new ClientOptions();

			//all fields of constructor should be set to false by default	
			assertFalse("ClientOption should be False.", clientOpts.isAllWrite());
			assertFalse("ClientOption should be False.", clientOpts.isClobber());
			assertFalse("ClientOption should be False.", clientOpts.isCompress());
			assertFalse("ClientOption should be False.", clientOpts.isLocked());
			assertFalse("ClientOption should be False.", clientOpts.isModtime());
			assertFalse("ClientOption should be False.", clientOpts.isRmdir());
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsToString verifies the assertion that the toString() method will return a P4 string
	 * that communicates the status of the options. The string is in the form of:
	 * noallwrite noclobber nocompress nolocked nomodtime normdir. Verification is via a NotNull 
	 * assertion and string comparison with expected value.
	 */
	@Test
	public void testClientOptionsToString() {
		try {
			
			debugPrintTestName();
			
			String verOptions = getVerificationString(false, false, false, false, false, false);
			
			ClientOptions clientOpts = new ClientOptions();

			String cOptions = clientOpts.toString();
			debugPrint(verOptions, cOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("When created, all options should be set to false.", verOptions, cOptions);
			
			//now change one option to see if string changes
			clientOpts.setAllWrite(true);
			String newOptions = clientOpts.toString();
			verOptions = getVerificationString(true, false, false, false, false, false);
			debugPrint("AllWrite=true", verOptions, newOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", newOptions);
			assertEquals("ClientOption should be True.", verOptions, newOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}
	

	/**
	 * The testClientOptionsSetOptionsToTrue verifies that the options are correctly set
	 * when we use the ClientOptions setters to set all options to "true".
	 * Verification is via the getters and via string comparison with expected value
	 * returned by the toString() method.
	 */
	@Test
	public void testClientOptionsSetOptionsToTrue() {
		try {
				
			debugPrintTestName();
			
			String verOptions = getVerificationString(true, true, true, true, true, true);
			
			ClientOptions clientOpts = new ClientOptions();
			
			clientOpts.setAllWrite(true);
			clientOpts.setClobber(true);
			clientOpts.setCompress(true);
			clientOpts.setLocked(true);
			clientOpts.setModtime(true);
			clientOpts.setRmdir(true);
			
			assertTrue("ClientOption should be True.", clientOpts.isAllWrite());
			assertTrue("ClientOption should be True.", clientOpts.isClobber());
			assertTrue("ClientOption should be True.", clientOpts.isCompress());
			assertTrue("ClientOption should be True.", clientOpts.isLocked());
			assertTrue("ClientOption should be True.", clientOpts.isModtime());
			assertTrue("ClientOption should be True.", clientOpts.isRmdir());
			
			String newOptions = clientOpts.toString();
			
			assertNotNull("ClientOptions.toString() returned Null Result.", newOptions);
			assertEquals("ClientOptions should all be True.", verOptions, newOptions);
			debugPrint(verOptions, newOptions);


		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * The testClientOptionsExplicitValueConstructor verifies that the options are correctly set
	 * when we use the ClientOptions Explicit Value Constructor 
	 * ClientOptions(boolean,boolean,boolean,boolean,boolean,boolean) to set one option to "true".
	 * Verification is via the a string comparison with expected value returned by the toString() method.
	 */
	@Test
	public void testClientOptionsExplicitValueConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			ClientOptions clientOpts = new ClientOptions(true, false, false, false, false, false);
			cOptions = clientOpts.toString();
			
			String verOptions = getVerificationString(true, false, false, false, false, false);			
			debugPrint(verOptions, cOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsExplicitValueConstructorSetAllTrue verifies that the options are correctly 
	 * set when we use the ClientOptions Explicit Value Constructor 
	 * ClientOptions(boolean,boolean,boolean,boolean,boolean,boolean) to set all options to "true".
	 * Verification is via getters and via a string comparison with expected value returned 
	 * by the toString() method.
	 */
	@Test
	public void testClientOptionsExplicitValueConstructorSetAllTrue() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			ClientOptions clientOpts = new ClientOptions(true, true, true, true, true, true);
			cOptions = clientOpts.toString();
			
			String verOptions = getVerificationString(true, true, true, true, true, true);
			debugPrint(verOptions, cOptions);
						
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption should all be True.", verOptions, cOptions);
			
			assertTrue("ClientOption should be True.", clientOpts.isAllWrite());
			assertTrue("ClientOption should be True.", clientOpts.isClobber());
			assertTrue("ClientOption should be True.", clientOpts.isCompress());
			assertTrue("ClientOption should be True.", clientOpts.isLocked());
			assertTrue("ClientOption should be True.", clientOpts.isModtime());
			assertTrue("ClientOption should be True.", clientOpts.isRmdir());
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	
	/**
	 * The testClientOptionsMixedExplicitValueConstructor verifies that the options are correctly 
	 * set when we use the ClientOptions Explicit Value Constructor 
	 * ClientOptions(boolean,boolean,boolean,boolean,boolean,boolean) to set some options to true or false.
	 * Verification is via getters and via a string comparison with expected value returned 
	 * by the toString() method.
	 */
	@Test
	public void testClientOptionsMixedExplicitValueConstructor() {
		try {

			debugPrintTestName();
			
			String cOptions = null;
			
			ClientOptions clientOpts = new ClientOptions(true, false, true, false, true, false);
			cOptions = clientOpts.toString();
			
			String verOptions = getVerificationString(true, false, true, false, true, false);		
			debugPrint(verOptions, cOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption settings should match.", verOptions, cOptions);
			
			assertTrue("ClientOption should be True.", clientOpts.isAllWrite());
			assertFalse("ClientOption should be False.", clientOpts.isClobber());
			assertTrue("ClientOption should be True.", clientOpts.isCompress());
			assertFalse("ClientOption should be False.", clientOpts.isLocked());
			assertTrue("ClientOption should be True.", clientOpts.isModtime());
			assertFalse("ClientOption should be False.", clientOpts.isRmdir());
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsStringConstructor verifies that the options are correctly 
	 * set when we use the ClientOptions String constructor ClientOptions(String) 
	 * to set some options to true or false in form of 
	 * ClientOptions("noallwrite noclobber compress nolocked nomodtime rmdir")
	 * Verification is via getters and via a string comparison with expected value returned 
	 * by the toString() method.
	 */
	@Test
	public void testClientOptionsStringConstructor() {
		try {

			debugPrintTestName();
			
			String cOptions = null;
			
			String verOptions = getVerificationString(true, true, true, false, false, false);

			ClientOptions clientOpts = new ClientOptions(verOptions);
			cOptions = clientOpts.toString();			
			debugPrint(verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
			//be diligent and test each option at the getter level, too.
			assertTrue("ClientOption should be True.", clientOpts.isAllWrite());
			assertTrue("ClientOption should be True.", clientOpts.isClobber());
			assertTrue("ClientOption should be True.", clientOpts.isCompress());
			assertFalse("ClientOption should be False.", clientOpts.isLocked());
			assertFalse("ClientOption should be False.", clientOpts.isModtime());
			assertFalse("ClientOption should be False.", clientOpts.isRmdir());
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsNullStringConstructor verifies the assertion that if you pass a  
	 * null to the ClientOptions constructor, it acts as the default constructor and sets all
	 * options to false.
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method. 
	 */
	@Test
	public void testClientOptionsNullStringConstructor() {
		try {

			debugPrintTestName();
			
			String cOptions = null;
			String verOptions = getVerificationString(false, false, false, false, false, false);

			ClientOptions clientOpts = new ClientOptions(null);
			cOptions = clientOpts.toString();			
			debugPrint("ClientOption strings should match.", verOptions, cOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsEmptyStringConstructor verifies that passing an empty  
	 * string to the ClientOptions constructor should set all options to false.
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method. 
	 */
	@Test
	public void testClientOptionsEmptyStringConstructor() {
		try {

			debugPrintTestName();
			
			String cOptions = null;			
			String verOptions = getVerificationString(false, false, false, false, false, false);

			ClientOptions clientOpts = new ClientOptions("");
			cOptions = clientOpts.toString();						
			debugPrint(verOptions, cOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsOneMissingStringConstructor verifies that passing an  
	 * incomplete string to the ClientOptions constructor should set named options to 
	 * the expected values.
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method. 
	 */
	@Test
	public void testClientOptionsOneMissingStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "clobber compress locked modtime rmdir";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, true, true, true, true, true);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsOneMissingOneFalseStringConstructor verifies that passing an  
	 * incomplete string to the ClientOptions constructor should set named options to 
	 * the expected values and unnamed ones to false. One of the passed-in options is 
	 * intentionally left "false".
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method. 
	 */
	@Test
	public void testClientOptionsOneMissingOneFalseStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "clobber nocompress locked modtime rmdir";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, true, false, true, true, true);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}
	
	
	/**
	 * The testClientOptionsOnlyOneStringConstructor verifies that passing an  
	 * incomplete string to the ClientOptions constructor should set named options to 
	 * the expected values and unnamed ones to false. One of the passed-in options is intentionally left "false"
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method. 
	 */
	@Test
	public void testClientOptionsOnlyOneStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "locked";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, false, false, true, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptions_RetainedValuesStringConstructor verifies that passing an  
	 * incomplete string to the ClientOptions constructor should set named options to 
	 * the expected values and unnamed ones should remain untouched. 
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method and via getter for updated option. 
	 */
	@Test
	public void testClientOptionsRetainedValuesStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "locked";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, false, false, true, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

			//now set another options to see if we retain options already set.
			clientOpts.setAllWrite(true);
			verOptions = getVerificationString(true, false, false, true, false, false);
			cOptions = clientOpts.toString();			
			debugPrint("AllWrite=true", verOptions, cOptions);
			
			assertTrue("ClientOption should be True.", clientOpts.isAllWrite());
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	
	/**
	 * The testClientOptionsMisspelledStringConstructor verifies that passing a  
	 * misspelled option string to the ClientOptions constructor should not set any options
	 * and hence all options should be "false". 
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method and via getter for updated option. 
	 */
	@Test
	public void testClientOptionsMisspelledStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "loccked";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, false, false, false, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);
			
			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
						
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsSpaceInStringConstructor verifies that passing a  
	 * misspelled option string (i.e. a space in option name) to the constructor should not set 
	 * any options and hence all options should be "false". 
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method and via getter for specified option. 
	 */
	@Test
	public void testClientOptionsSpaceInStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "lo cked";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, false, false, false, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertFalse("ClientOption should be False", clientOpts.isLocked());
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
						
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}


	/**
	 * The testClientOptionsSpaceAtEndStringConstructor verifies that passing an  
	 * option string that is not trimmed to the constructor should set the option. 
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method and via getter for specified option. 
	 */
	@Test
	public void testClientOptionsSpaceAtEndStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "locked   ";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, false, false, true, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertTrue("ClientOption should be True.", clientOpts.isLocked());
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
						
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}
	
	
	/**
	 * The testClientOptionsBadStringGoodStringConstructor verifies that passing an  
	 * option string that has a misspelled option and a good option should indeed set 
	 * the good option but not the bad one. 
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method and via getter for specified option. 
	 */
	
	@Test
	public void testClientOptionsBadStringGoodStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "plocked rmdir";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, false, false, false, false, true);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertFalse("ClientOption should be False.", clientOpts.isLocked());
			assertTrue("ClientOption should be True.", clientOpts.isRmdir());
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsOutOfOrderStringConstructor verifies that passing an  
	 * option string that has options in random order should set appropriate options. 
	 * Verification is via string comparison with expected value returned 
	 * by the toString() method and via getter for all options. 
	 */
	@Test
	public void testClientOptionsOutOfOrderStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "rmdir clobber allwrite";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(true, true, false, false, false, true);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

			assertTrue("ClientOption should be True.", clientOpts.isAllWrite());
			assertTrue("ClientOption should be True.", clientOpts.isClobber());
			assertFalse("ClientOption should be False.", clientOpts.isCompress());
			assertFalse("ClientOption should be False.", clientOpts.isLocked());
			assertFalse("ClientOption should be False.", clientOpts.isModtime());
			assertTrue("ClientOption should be True.", clientOpts.isRmdir());
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsMixedCaseStringConstructor verifies that passing a  
	 * mixed case option string sets the appropriate options. 
	 * Verification is via string comparison with expected value returned 
	 * by the toString(). 
	 */
	@Test
	public void testClientOptionsMixedCaseStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "rmdir CLObber allwRIte";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(true, true, false, false, false, true);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientOptionsBadCharsStringConstructor verifies that passing an  
	 * option string with bad chars will not set that options. 
	 * Verification is via string comparison with expected value returned 
	 * by the toString(). 
	 */
	@Test
	public void testClientOptionsBadCharsStringConstructor() {
		try {

			debugPrintTestName();
			String cOptions = null;
			
			String defaultOptionString = "/\\@:allwrite";
			ClientOptions clientOpts = new ClientOptions(defaultOptionString);
			cOptions = clientOpts.toString();			
			
			String verOptions = getVerificationString(false, false, false, false, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}


	/**
	 * This helper function takes the boolean values passed in and converts them to Perforce-standard 
	 * representation of these options for ClientOptions. The string that is returned is useful for 
	 * comparison against the return value of the toString() method of the ClientOptions class.
	 */
	private String getVerificationString(boolean allWriteVal, boolean clobberVal,
			boolean compressVal, boolean lockedVal, boolean modtimeVal, boolean rmdirVal) {
		
		String vString = null;
		
		vString = allWriteVal ? "allwrite" : "noallwrite";
		vString += clobberVal ? " clobber" : " noclobber";
		vString += compressVal ? " compress" : " nocompress";
		vString += lockedVal ? " locked" : " nolocked";
		vString += modtimeVal ? " modtime" : " nomodtime";
		vString += rmdirVal ? " rmdir" : " normdir";
		
		return vString;
	}
	

}
