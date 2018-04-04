package com.perforce.p4java.tests.dev.unit.feature.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.impl.generic.client.ClientSubmitOptions;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 * The ClientSubmitOptionsTest class exercises the ClientSubmitOptions class. 
 * Constructors -
 * The test verifies the ClientSubmitOptions(), ClientSubmitOptions(String) and
 * ClientSubmitOptions(boolean,boolean,boolean,boolean,boolean,boolean) default behavior as
 * well as behavior with various good and bad inputs.
 * Setters and Getters -
 * Each flag is verified by setting the option through one of the constructors or setters
 * and then calling its "getter" isXXX() and/or toString() to see if the setting registered.
 * toString() -
 * toString is utilized in most of the tests. It is used in verification of the setters
 * and getters, but its return values are also tested with erroneous or oddly formatted input.
 *
 * @throws java.lang.Exception
 */


@TestId("ClientSubmitOptionsTest01")
@Standalone
public class ClientSubmitOptionsTest extends P4JavaTestCase {

	/**
	 * The testClientSubmitOptionsDefaultConstructor verifies the assertion that the default
	 * constructor sets all fields to false. Verified by calling the "getters" separately
	 * for each method.
	 */
	@Test
	public void testClientSubmitOptionsDefaultConstructor() {
		try {
			
			debugPrintTestName();
			ClientSubmitOptions submitOpts = new ClientSubmitOptions();

			//all fields of default constructor should be set to false
			assertFalse("ClientOption should be False", submitOpts.isLeaveunchanged());
			assertFalse("ClientOption should be False", submitOpts.isLeaveunchangedReopen());
			assertFalse("ClientOption should be False", submitOpts.isRevertunchanged());
			assertFalse("ClientOption should be False", submitOpts.isRevertunchangedReopen());
			assertFalse("ClientOption should be False", submitOpts.isSubmitunchanged());
			assertFalse("ClientOption should be False", submitOpts.isSubmitunchangedReopen());

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsToString verifies the assertion that the toString() method will return a P4 string
	 * that communicates the status of the options. The string is in the form of:
	 * noleaveunchanged noleaveunchangedreopen norevertunchanged norevertunchangedreopen nosubmitunchanged nosubmitunchangedreopen. Verification is via a NotNull
	 * assertion and string comparison with expected value.
	 */
	@Test
	public void testClientSubmitOptionsToString() {
		try {
			
			debugPrintTestName();
			String verOptions = getVerificationString(false, false, false, false, false, false);

			ClientSubmitOptions submitOpts = new ClientSubmitOptions();
 
			String cOptions = submitOpts.toString();
			debugPrint(verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match", verOptions, cOptions);

			//now change one option to see if string changes
			submitOpts.setLeaveunchanged(true);
			String newOptions = submitOpts.toString();
			verOptions = getVerificationString(true, false, false, false, false, false);
			debugPrint("Leaveunchanged=true", verOptions, newOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", newOptions);
			assertEquals("ClientOption strings should match", verOptions, newOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsExplicitValueConstructor verifies that the options are correctly set
	 * when we use the ClientSubmitOptions Explicit Value Constructor
	 * ClientSubmitOptions(boolean,boolean,boolean,boolean,boolean,boolean) to set one option to "true".
	 * Verification is via the a string comparison with expected value returned by the toString() method.
	 */
	@Test
	public void testClientSubmitOptionsExplicitValueConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			ClientSubmitOptions submitOpts = new ClientSubmitOptions(true, false, false, false, false, false);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(false, false, false, false, true, false);
			debugPrint(verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsExplicitValueConstructorSetAllTrue verifies that the options are correctly
	 * set when we use the ClientSubmitOptions Explicit Value Constructor
	 * ClientSubmitOptions(boolean,boolean,boolean,boolean,boolean,boolean) to set all options to "true".
	 * Verification is via getters and via a string comparison with expected value returned
	 * by the toString() method.
	 */
	@Test
	public void testClientSubmitOptionsExplicitValueConstructorSetAllTrue() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			ClientSubmitOptions submitOpts = new ClientSubmitOptions(true, true, true, true, true, true);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(true, true, true, true, true, true);
			debugPrint(verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			//be diligent and test each option at the getter level, too.
			assertTrue("ClientOption should be True.", submitOpts.isLeaveunchanged());
			assertTrue("ClientOption should be True.", submitOpts.isLeaveunchangedReopen());
			assertTrue("ClientOption should be True.", submitOpts.isRevertunchanged());
			assertTrue("ClientOption should be True.", submitOpts.isRevertunchangedReopen());
			assertTrue("ClientOption should be True.", submitOpts.isSubmitunchanged());
			assertTrue("ClientOption should be True.", submitOpts.isSubmitunchangedReopen());

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}


	/**
	 * The testClientSubmitOptionsMixedExplicitValueConstructor verifies that the options are correctly
	 * set when we use the ClientSubmitOptions Explicit Value Constructor
	 * ClientSubmitOptions(boolean,boolean,boolean,boolean,boolean,boolean) to set some options to true or false.
	 * Verification is via getters and via a string comparison with expected value returned
	 * by the toString() method.
	 */
	@Test
	public void testClientSubmitOptionsMixedExplicitValueConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			ClientSubmitOptions submitOpts = new ClientSubmitOptions(true, false, true, false, true, false);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(true, false, true, false, true, false);
			debugPrint(verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			//be diligent and test each option at the getter level, too.
			assertTrue("ClientOption should be True.", submitOpts.isLeaveunchanged());
			assertFalse("ClientOption should be False.", submitOpts.isLeaveunchangedReopen());
			assertTrue("ClientOption should be True.", submitOpts.isRevertunchanged());
			assertFalse("ClientOption should be False.", submitOpts.isRevertunchangedReopen());
			assertTrue("ClientOption should be True.", submitOpts.isSubmitunchanged());
			assertFalse("ClientOption should be False.", submitOpts.isSubmitunchangedReopen());

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsStringConstructor verifies that the options are correctly
	 * set when we use the ClientSubmitOptions String constructor ClientSubmitOptions(String)
	 * to set some options to true or false in form of
	 * ClientSubmitOptions("leaveunchanged leaveunchangedreopen revertunchanged norevertunchangedreopen
	 * nosubmitunchanged nosubmitunchangedreopen.")
	 * Verification is via getters and via a string comparison with expected value returned
	 * by the toString() method.
	 */
	@Test
	public void testClientSubmitOptionsStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String verOptions = getVerificationString(true, true, true, false, false, false);

			ClientSubmitOptions submitOpts = new ClientSubmitOptions(verOptions);
			cOptions = submitOpts.toString();
			debugPrint("ClientOption strings should match.", verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

			//be diligent and test each option at the getter level, too.
			assertTrue("ClientOption should be True.", submitOpts.isLeaveunchanged());
			assertTrue("ClientOption should be True.", submitOpts.isLeaveunchangedReopen());
			assertTrue("ClientOption should be True.", submitOpts.isRevertunchanged());
			assertFalse("ClientOption should be False.", submitOpts.isRevertunchangedReopen());
			assertFalse("ClientOption should be False.", submitOpts.isSubmitunchanged());
			assertFalse("ClientOption should be False.", submitOpts.isSubmitunchangedReopen());

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsNullStringConstructor verifies the assertion that if you pass a
	 * null to the ClientSubmitOptions constructor, it acts as the default constructor and sets all
	 * options to false.
	 * Verification is via string comparison with expected value returned
	 * by the toString() method.
	 */
	@Test
	public void testClientSubmitOptionsNullStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;
			String verOptions = getVerificationString(false, false, false, false, false, false);

			ClientSubmitOptions submitOpts = new ClientSubmitOptions(null);
			cOptions = submitOpts.toString();
			debugPrint(verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsEmptyStringConstructor verifies that passing an empty
	 * string to the ClientSubmitOptions constructor should set all options to false.
	 * Verification is via string comparison with expected value returned
	 * by the toString() method.
	 */
	@Test
	public void testClientSubmitOptionsEmptyStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;
			String verOptions = getVerificationString(false, false, false, false, false, false);

			ClientSubmitOptions submitOpts = new ClientSubmitOptions("");
			cOptions = submitOpts.toString();
			debugPrint(verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsOneMissingStringConstructor verifies that passing an
	 * incomplete string to the ClientSubmitOptions constructor should set named options to
	 * the expected values.
	 * Verification is via string comparison with expected value returned
	 * by the toString() method.
	 */
	@Test
	public void testClientSubmitOptionsOneMissingStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString =
					"leaveunchanged+reopen revertunchanged revertunchanged+reopen submitunchanged submitunchanged+reopen";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(true, true, true, true, true, true);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}


	/**
	 * The testClientSubmitOptionsOnlyOneStringConstructor verifies that passing an
	 * incomplete string to the ClientSubmitOptions constructor should set named options to
	 * the expected values and unnamed ones to false. One of the passed-in options is intentionally left "false"
	 * Verification is via string comparison with expected value returned
	 * noleaveunchanged noleaveunchangedreopen norevertunchanged norevertunchangedreopen
	 * nosubmitunchanged nosubmitunchangedreopen
	 * by the toString() method.
	 */
	@Test
	public void testClientSubmitOptionsOnlyOneStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString = "revertunchangedreopen";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(false, false, true, true, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsRetainedValuesStringConstructor verifies that passing an
	 * incomplete string to the ClientSubmitOptions constructor should set named options to
	 * the expected values and unnamed ones should remain untouched.
	 * Verification is via string comparison with expected value returned
	 * by the toString() method and via getter for updated option.
	 */
	@Test
	public void testClientSubmitOptionsRetainedValuesStringConstructor() {
		try {
			
			String cOptions = null;

			String defaultOptionString = "revertunchangedreopen";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(false, false, true, true, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

			//now set another options to see if we clear options already set.
			submitOpts.setLeaveunchanged(true);
			verOptions = getVerificationString(true, false, true, true, false, false);
			cOptions = submitOpts.toString();

			assertTrue("ClientOption should be True.", submitOpts.isLeaveunchanged());
			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertTrue("ClientOption strings should not match.",
								!verOptions.equalsIgnoreCase(cOptions));

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}


	/**
	 * The testClientSubmitOptionsMisspelledStringConstructor verifies that passing a
	 * misspelled option string to the ClientSubmitOptions constructor should not set any options
	 * and hence all options should be "false".
	 * Verification is via string comparison with expected value returned
	 * by the toString() method and via getter for updated option.
	 */
	@Test
	public void testClientSubmitOptionsMisspelledStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString = "loccked";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(false, false, false, false, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsSpaceInStringConstructor verifies that passing a
	 * misspelled option string (i.e. a space in option name) to the constructor should not set
	 * any options and hence all options should be "false".
	 * Verification is via string comparison with expected value returned
	 * by the toString() method and via getter for specified option.
	 */
	@Test
	public void testClientSubmitOptionsSpaceInStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString = "lo cked";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(false, false, false, false, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertFalse("ClientOption should be False.", submitOpts.isRevertunchangedReopen());
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}


	/**
	 * The testClientSubmitOptionsSpaceAtEndStringConstructor verifies that passing an
	 * option string that is not trimmed to the constructor should set the option.
	 * Verification is via string comparison with expected value returned
	 * by the toString() method and via getter for specified option.
	 */
	@Test
	public void testClientSubmitOptionsSpaceAtEndStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString = "revertunchangedreopen   ";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(false, false, true, true, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertTrue("ClientOption should be True.", submitOpts.isRevertunchangedReopen());
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}


	/**
	 * The testClientSubmitOptionsBadStringGoodStringConstructor verifies that passing an
	 * option string that has a misspelled option and a good option should indeed set
	 * the good option but not the bad one.
	 * Verification is via string comparison with expected value returned
	 * by the toString() method and via getter for specified option.
	 */

	@Test
	public void testClientSubmitOptionsBadStringGoodStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString = "prevertunchangedreopen submitunchangedreopen";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);

			cOptions = submitOpts.toString();
			String verOptions = getVerificationString(false, false, false, false, true, true);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertFalse("ClientOption should be False.", submitOpts.isRevertunchangedReopen());
			assertTrue("ClientOption should be True.", submitOpts.isSubmitunchangedReopen());
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsOutOfOrderStringConstructor verifies that passing an
	 * option string that has options in random order should set appropriate options.
	 * Verification is via string comparison with expected value returned
	 * by the toString() method and via getter for all options.
	 */
	@Test
	public void testClientSubmitOptionsOutOfOrderStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString = "submitunchanged LeaveunchangedReopen Leaveunchanged";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(true, true, false, false, true, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);
			
			//now test each option at the getter level, too.
			assertTrue("ClientOption should be True.", submitOpts.isLeaveunchanged());
			assertTrue("ClientOption should be True.", submitOpts.isLeaveunchangedReopen());
			assertFalse("ClientOption should be False.", submitOpts.isRevertunchanged());
			assertFalse("ClientOption should be False.", submitOpts.isRevertunchangedReopen());
			assertTrue("ClientOption should be True.", submitOpts.isSubmitunchanged());
			assertFalse("ClientOption should be True.", submitOpts.isSubmitunchangedReopen());

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsMixedCaseStringConstructor verifies that passing a
	 * mixed case option string sets the appropriate options.
	 * Verification is via string comparison with expected value returned
	 * by the toString().
	 */
	@Test
	public void testClientSubmitOptionsMixedCaseStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString = "rmdir LeaveunchangedReopen Leaveunchanged";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(true, true, false, false, false, true);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * The testClientSubmitOptionsBadCharsStringConstructor verifies that passing an
	 * option string with bad chars will not set that options.
	 * Verification is via string comparison with expected value returned
	 * by the toString().
	 */
	@Test
	public void testClientSubmitOptionsBadCharsStringConstructor() {
		try {
			
			debugPrintTestName();
			String cOptions = null;

			String defaultOptionString = "/\\@:Leaveunchanged";
			ClientSubmitOptions submitOpts = new ClientSubmitOptions(defaultOptionString);
			cOptions = submitOpts.toString();

			String verOptions = getVerificationString(false, false, false, false, false, false);
			debugPrint(defaultOptionString, verOptions, cOptions);

			assertNotNull("ClientSubmitOptions.toString() returned Null Result.", cOptions);
			assertEquals("ClientOption strings should match.", verOptions, cOptions);

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * This helper function takes the boolean values passed in and converts them to Perforce-standard
	 * representation of these options for ClientSubmitOptions. The string that is returned is useful for
	 * comparison against the return value of the toString() method of the ClientSubmitOptions class.
	 */
	private String getVerificationString(boolean leaveunchangedVal, boolean leaveunchangedReopenVal,
			boolean revertunchangedVal, boolean revertunchangedReopenVal, boolean submitunchangedVal, boolean submitunchangedReopenVal) {

		String vString = 
				(submitunchangedVal ? ("submitunchanged" + (submitunchangedReopenVal ? "+reopen" : "") + " ") : "")
				+ (revertunchangedVal ? ("revertunchanged" + (revertunchangedReopenVal ? "+reopen" : "") + " ") : "")
				+ (leaveunchangedVal ? ("leaveunchanged" + (leaveunchangedReopenVal ? "+reopen" : "") + " ") : "");

		return vString.trim();
	}


}
