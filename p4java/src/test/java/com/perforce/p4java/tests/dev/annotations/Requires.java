/**
 * 
 */
package com.perforce.p4java.tests.dev.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Omnibus annotation to spell out any specific platform, JDK,
 * server version, unicodeness, for a JUnit test class or method.
 * Will probably evolve over time or may end up being split into
 * more specific annotations. No checking for consistent, coherent,
 * or realistic values is promised anywhere in the associated
 * annotation processing.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Requires {
	final String NO_REQUIREMENTS_STRING = "none";
	final int NO_REQIREMENTS_INT = 0;
	
	/**
	 * Specific JDK version required.
	 */
	String jdk() default NO_REQUIREMENTS_STRING;
	
	/**
	 * Minimum JDK version required.
	 */
	String minJdk() default NO_REQUIREMENTS_STRING;
	
	/**
	 * Maximum JDK version required.
	 */
	String maxJdk() default NO_REQUIREMENTS_STRING;
	
	/**
	 * Specific server version required.<p>
	 * 
	 * In format 20092 or 20073, etc. 0 signals no requirement.
	 */
	int serverVersion() default NO_REQIREMENTS_INT;
	
	/**
	 * Minimum server version required.<p>
	 * 
	 * In format 20092 or 20073, etc. 0 signals no requirement.
	 */
	int minServerVersion() default NO_REQIREMENTS_INT;
	
	/**
	 * Max server version required.<p>
	 * 
	 * In format 20092 or 20073, etc. 0 signals no requirement.
	 */
	int maxServerVersion() default NO_REQIREMENTS_INT;
	
	/**
	 * Specific platform required.
	 */
	String platform() default NO_REQUIREMENTS_STRING;
	
	/**
	 * Test requires a Unicode server.
	 */
	boolean unicodeServer() default false;
}
