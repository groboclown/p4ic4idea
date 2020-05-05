package com.perforce.p4java.option;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.server.IServer;

public class OptionsTest {
    @Test
    public void testDefaultConstruct() {
        Options options = new Options() {
            @Override
            public List<String> processOptions(IServer server) throws OptionsException {
                return null;
            }
        };

        Assert.assertNull(options.getOptions());
        Assert.assertFalse(options.isImmutable());
    }

    @Test
    public void testDefaultConstructWithOptions() {
        Options options = new MockOptions("-m10", "-uhreid");
        Assert.assertEquals(2, options.getOptions().size());
        Assert.assertTrue(options.isImmutable());
    }

    @Test
    public void testProcessFields() throws OptionsException {
        Options options = new MockOptions();
        String optionSpecsString = "b:f s:r i:d:gtz s[]:e";
        String[] stringArrayOptions = new String[] { "myKey-*", "sshKey-*" };
        List<String> processFields = options.processFields(optionSpecsString, false, "@2016/09/08",
                15, stringArrayOptions);
        Assert.assertEquals(4, processFields.size());
        Assert.assertEquals("-r@2016/09/08", processFields.get(0));
        Assert.assertEquals("-d15", processFields.get(1));
        Assert.assertEquals("-emyKey-*", processFields.get(2));
        Assert.assertEquals("-esshKey-*", processFields.get(3));
    }

    @Test
    public void testProcessFields_withExceptions() {
        Options options = new MockOptions();
        try {
            options.processFields(null, new Object[] { true, true });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String optionSpecsString = "b:f b:r b:d b:p";
        try {
            options.processFields(optionSpecsString, new Object[] { true, true });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String optionSpecsStringHasSpace = "b:f   b:r";
        try {
            options.processFields(optionSpecsStringHasSpace, new Object[] { true, true });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String optionSpecsStringHasNull = "b:f null";
        try {
            options.processFields(optionSpecsStringHasNull, new Object[] { true, true });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String optionSpecsStringWithSpaceValue = "b: b:r";
        try {
            options.processFields(optionSpecsStringWithSpaceValue, new Object[] { true, true });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String optionSpecsStringHasMoreThan3Parts = "b:f:a:d b:r";
        try {
            options.processFields(optionSpecsStringHasMoreThan3Parts, new Object[] { true, true });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String unknownOptionType = "b:f:a:d d:r";
        try {
            options.processFields(unknownOptionType, new Object[] { true, true });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        try {
            options.processFields(unknownOptionType, new Object[] { true, true });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String optionSpecString = "b:f s:p l:g";
        Object[] wrongValue = new Object[] { false, "test", "wrong long number" };
        try {
            options.processFields(optionSpecString, wrongValue);
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String optionSpecString2 = "b:f s:p s[]:g";
        Object[] isNotStringArray = new Object[] { "a", 1, "d" };
        Object[] wrongValue2 = new Object[] { false, "test", isNotStringArray };
        try {
            options.processFields(optionSpecString2, wrongValue2);
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }
    }

    @Test
    public void testSetOption() {
        Options options = new MockOptions();
        options.setOptions();

        Assert.assertEquals(0, options.getOptions().size());

        options.setOptions("-p", "-r");
        Assert.assertEquals(2, options.getOptions().size());
        Assert.assertEquals("-p", options.getOptions().get(0));
        Assert.assertEquals("-r", options.getOptions().get(1));

        options.setOptions((String[])null);
        Assert.assertNull(options.getOptions());
    }

    private class MockOptions extends Options {
        public MockOptions(String... options) {
            super(options);
        }

        @Override
        public List<String> processOptions(IServer server) throws OptionsException {
            return null;
        }
    }
}