package com.perforce.p4java.option.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.exception.OptionsException;

public class OptionsHelperTest {
    @Test
    public void applyRule_boolean() throws Exception {
        try {
            OptionsHelper.applyRule("nonnull", "e", true);
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }
        try {
            OptionsHelper.applyRule(null, null, true);
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }

        String optionsField = OptionsHelper.applyRule(null, "f", true);
        assertEquals("-f", optionsField);

        optionsField = OptionsHelper.applyRule(null, "f", false);
        assertEquals("", optionsField);
    }

    @Test
    public void applyRule_integer() throws Exception {
        try {
            OptionsHelper.applyRule(null, null, 1);
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }
        try {
            OptionsHelper.applyRule("nonnull", "not-exist", 1);
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }

        String optionsField = OptionsHelper.applyRule(null, "d", 12);
        assertEquals("-d12", optionsField);

        optionsField = OptionsHelper.applyRule("gtz", "d", 12);
        assertEquals("-d12", optionsField);

        optionsField = OptionsHelper.applyRule("gtz", "d", 0);
        assertEquals("", optionsField);

        optionsField = OptionsHelper.applyRule("cl", "d", 0);
        assertEquals("-ddefault", optionsField);

        optionsField = OptionsHelper.applyRule("cl", "d", 12);
        assertEquals("-d12", optionsField);

        optionsField = OptionsHelper.applyRule("cl", "d", -1);
        assertEquals("", optionsField);

        optionsField = OptionsHelper.applyRule("clz", "d", 12);
        assertEquals("-d12", optionsField);

        optionsField = OptionsHelper.applyRule("clz", "d", 0);
        assertEquals("", optionsField);

        optionsField = OptionsHelper.applyRule("dcn", "d", 12);
        assertEquals("-d12", optionsField);

        optionsField = OptionsHelper.applyRule("dcn", "d", 0);
        assertEquals("-d", optionsField);

        optionsField = OptionsHelper.applyRule("dcn", "d", -1);
        assertEquals("", optionsField);

    }

    @Test
    public void applyRule_long() throws Exception {
        try {
            OptionsHelper.applyRule(null, null, 1L);
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }
        try {
            OptionsHelper.applyRule("nonnull", "not-exist", 1L);
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }

        String optionsField = OptionsHelper.applyRule(null, "d", 2L);
        assertEquals("-d2", optionsField);

        optionsField = OptionsHelper.applyRule("gtz", "d", 2L);
        assertEquals("-d2", optionsField);

        optionsField = OptionsHelper.applyRule("gtz", "d", 0L);
        assertEquals("", optionsField);

        optionsField = OptionsHelper.applyRule("gez", "d", 0L);
        assertEquals("-d0", optionsField);

        optionsField = OptionsHelper.applyRule("gez", "d", 2L);
        assertEquals("-d2", optionsField);

        optionsField = OptionsHelper.applyRule("gez", "d", -2L);
        assertEquals("", optionsField);

    }

    @Test
    public void applyRule_String() throws Exception {
        try {
            OptionsHelper.applyRule(null, null, "string");
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }
        try {
            OptionsHelper.applyRule("notnull", null, "string");
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }

        String optionsField = OptionsHelper.applyRule(null, "d", null);
        assertEquals("", optionsField);

        optionsField = OptionsHelper.applyRule(null, "d", " ");
        assertEquals("", optionsField);

        optionsField = OptionsHelper.applyRule(null, "d", " ");
        assertEquals("", optionsField);

        optionsField = OptionsHelper.applyRule(null, "o", "myClient");
        assertEquals("-omyClient", optionsField);
    }

    @Test
    public void objectToBoolean() throws Exception {
        try {
            OptionsHelper.objectToBoolean(null);
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }
        try {
            OptionsHelper.objectToBoolean("");
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }
        try {
            OptionsHelper.objectToBoolean(" ");
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }
        try {
            OptionsHelper.objectToBoolean(1L);
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }
        try {
            OptionsHelper.objectToBoolean("not boolean");
            fail("Expected Exception!");
        } catch (Throwable e) {
            assertEquals(OptionsException.class, e.getClass());
        }

        Object optValue = true;
        boolean actual = OptionsHelper.objectToBoolean(optValue);
        assertEquals(actual, true);

        optValue = false;
        actual = OptionsHelper.objectToBoolean(optValue);
        assertEquals(false, actual);

        optValue = "true";
        actual = OptionsHelper.objectToBoolean(optValue);
        assertEquals(true, actual);

        optValue = "TruE";
        actual = OptionsHelper.objectToBoolean(optValue);
        assertEquals(true, actual);

        optValue = "false";
        actual = OptionsHelper.objectToBoolean(optValue);
        assertEquals(false, actual);

        optValue = "fALse";
        actual = OptionsHelper.objectToBoolean(optValue);
        assertEquals(false, actual);
    }
}