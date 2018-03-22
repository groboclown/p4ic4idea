package com.perforce.p4java.option.client;

import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.reflections.Reflections;

import com.perforce.p4java.option.Options;

/**
 * Class to test a default options constructor and then
 * setting options..
 */
public class OptionsDefaultConstructorsTest {

	/**
	 * Test default constructors and set options.
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 */
	@Test
	public void testDefaultConstructorsAndSetOptions()
			throws InstantiationException, IllegalAccessException {
		Reflections reflections = new Reflections(
			"com.perforce.p4java.option");
		Set<Class<? extends Options>> subTypes = 
		           reflections.getSubTypesOf(Options.class);
		for (Class<? extends Options> subtype : subTypes) {
			if (!subtype.getName().contains("Test")) {
				Options options = subtype.newInstance();
				options.setOptions("one");
				assertTrue(options.getOptions() != null);
				assertTrue(options.getOptions().size() == 1);
				assertTrue(options.getOptions().contains("one"));
			}
		}
	}
}
