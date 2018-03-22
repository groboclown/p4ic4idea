/**
 * 
 */
package com.perforce.p4java.tests.dev.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate a JUnit test class or other type as being associated
 * with one or more Perforce Job IDs. Defaults to "Unassigned".
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Jobs {
	String[] value() default {"unassigned"};
}
