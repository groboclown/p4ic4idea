/**
 * 
 */
package com.perforce.p4java.tests.dev.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime annotation used to annotate a particular
 * JUnit test class with a P4Java test ID.<p>
 * 
 * Test ID's are not formally specified anywhere, but in general
 * they're used to uniquely identify specific JUnit test classes
 * in tools, jobs, changelists, etc., and are used within tests to
 * generate unique paths, names, etc. They are typically something
 * similar to "Job034567G" (i.e. a job ID with a serial alpha suffix)
 * or "ClassAnnotatePaths02" (i.e. a class under test with a serial
 * suffix).<p>
 * 
 * One note: don't use "=" in a test ID; it may cause (minor) odd
 * errors in the test superclass test ID processing.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestId {
	/**
	 * The value used to signal that no valid test ID has been
	 * assigned yet.
	 */
	final String NO_TESTID = "unassigned";
	
	/**
	 * Get the P4Java test id associated with this element.
	 */
	String value() default NO_TESTID;
}
