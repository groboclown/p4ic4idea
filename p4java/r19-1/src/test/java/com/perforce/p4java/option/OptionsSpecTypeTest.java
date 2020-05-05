package com.perforce.p4java.option;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.p4java.exception.OptionsException;

public class OptionsSpecTypeTest {

    @Test
    public void of() throws Exception {
        OptionsSpecType optionsSpecType = OptionsSpecType.of("i");
        Assert.assertEquals(OptionsSpecType.IntOption, optionsSpecType);

        optionsSpecType = OptionsSpecType.of("l");
        Assert.assertEquals(OptionsSpecType.LongOption, optionsSpecType);

        optionsSpecType = OptionsSpecType.of("b");
        Assert.assertEquals(OptionsSpecType.BoolOption, optionsSpecType);

        optionsSpecType = OptionsSpecType.of("s");
        Assert.assertEquals(OptionsSpecType.StringOption, optionsSpecType);

        optionsSpecType = OptionsSpecType.of("s[]");
        Assert.assertEquals(OptionsSpecType.StringArrayOption, optionsSpecType);

        try {
            OptionsSpecType.of("unknown");
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }
    }

    @Test
    public void getP4CommandOptionFields() throws Exception {
        List<String> p4CommandOptionFields = OptionsSpecType.StringOption
                .getP4CommandOptionFields(null, "p", "f");
        Assert.assertEquals(1, p4CommandOptionFields.size());
        Assert.assertEquals("-pf", p4CommandOptionFields.get(0));
    }

    @Test
    public void getP4CommandOptionFiled() throws Exception {
        try {
            OptionsSpecType.IntOption.getP4CommandOptionField(null, "p", "12L");
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String p4CommandOptionFiled = OptionsSpecType.IntOption.getP4CommandOptionField(null, "p", 12);
        Assert.assertEquals("-p12", p4CommandOptionFiled);

        try {
            OptionsSpecType.LongOption.getP4CommandOptionField(null, "p", "12L");
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        p4CommandOptionFiled = OptionsSpecType.LongOption.getP4CommandOptionField(null, "p", 12);
        Assert.assertEquals("-p12", p4CommandOptionFiled);

        p4CommandOptionFiled = OptionsSpecType.LongOption.getP4CommandOptionField(null, "p", 12L);
        Assert.assertEquals("-p12", p4CommandOptionFiled);

        try {
            OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "12L");
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        Assert.assertEquals("",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "false"));
        Assert.assertEquals("",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "False"));
        Assert.assertEquals("",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "FALSE"));
        Assert.assertEquals("",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "FAlSE"));
        Assert.assertEquals("",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", false));

        try {
            OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", 0);
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        try {
            OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", 1);
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        try {
            OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p",
                    "not valid boolean value");
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        Assert.assertEquals("-p",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "true"));
        Assert.assertEquals("-p",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "True"));
        Assert.assertEquals("-p",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "TrUE"));
        Assert.assertEquals("-p",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "TRUE"));
        Assert.assertEquals("-p",
                OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", true));

        String empty = OptionsSpecType.StringOption.getP4CommandOptionField(null, "p", null);
        Assert.assertEquals("", empty);

        try {
            OptionsSpecType.StringOption.getP4CommandOptionField("haha", "p", "f");
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        String value1 = OptionsSpecType.StringOption.getP4CommandOptionField(null, "p", "f");
        Assert.assertEquals("-pf", value1);

        List<String> p4CommandOptionFields = OptionsSpecType.StringOption
                .getP4CommandOptionFields(null, "p", "f");
        Assert.assertEquals(1, p4CommandOptionFields.size());
        Assert.assertEquals("-pf", p4CommandOptionFields.get(0));

        try {
            OptionsSpecType.StringArrayOption.getP4CommandOptionFields(null, "f",
                    new Object[] { "a", 3 });
            Assert.fail("No exception thrown!");
        } catch (Throwable e) {
            Assert.assertEquals(OptionsException.class, e.getClass());
        }

        List<String> optionsFields = OptionsSpecType.StringArrayOption
                .getP4CommandOptionFields(null, "e", new String[] { "mykey-*", "sshKey-*" });
        Assert.assertEquals(2, optionsFields.size());
        Assert.assertEquals("-emykey-*", optionsFields.get(0));
        Assert.assertEquals("-esshKey-*", optionsFields.get(1));

        optionsFields = OptionsSpecType.StringArrayOption.getP4CommandOptionFields(null, "e",
                new String[] {});
        Assert.assertEquals(0, optionsFields.size());
    }
}