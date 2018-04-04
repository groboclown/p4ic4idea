/**
 * 
 */
package com.perforce.p4java.tests.dev.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime annotation to indicate a JUnit test is "standalone", i.e. it
 * does not require a connection to a Perforce server. Defaults to true
 * if used as a simple marker, but can also be use to set standaloneness
 * to false explicitly.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Standalone {
	boolean value() default true;
}
