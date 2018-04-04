package com.perforce.p4java.option;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.expectThrows;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.exception.OptionsException;

/**
 * @author Sean Shou
 * @since 8/09/2016
 */
public class OptionsSpecTypeTest {
  @Test
  public void of() throws Exception {
    OptionsSpecType optionsSpecType = OptionsSpecType.of("i");
    assertThat(optionsSpecType, is(OptionsSpecType.IntOption));

    optionsSpecType = OptionsSpecType.of("l");
    assertThat(optionsSpecType, is(OptionsSpecType.LongOption));

    optionsSpecType = OptionsSpecType.of("b");
    assertThat(optionsSpecType, is(OptionsSpecType.BoolOption));

    optionsSpecType = OptionsSpecType.of("s");
    assertThat(optionsSpecType, is(OptionsSpecType.StringOption));

    optionsSpecType = OptionsSpecType.of("s[]");
    assertThat(optionsSpecType, is(OptionsSpecType.StringArrayOption));

    expectThrows(OptionsException.class, () -> OptionsSpecType.of("unknown"));
  }

  @Test
  public void getP4CommandOptionFields() throws Exception {
    List<String> p4CommandOptionFields = OptionsSpecType.StringOption.getP4CommandOptionFields(null, "p", "f");
    assertThat(p4CommandOptionFields.size(), is(1));
    assertThat(p4CommandOptionFields.get(0), is("-pf"));
  }

  @Test
  public void getP4CommandOptionFiled() throws Exception {
    expectThrows(OptionsException.class, () -> OptionsSpecType.IntOption.getP4CommandOptionField(null, "p", "12L"));
    String p4CommandOptionFiled = OptionsSpecType.IntOption.getP4CommandOptionField(null, "p", 12);
    assertThat(p4CommandOptionFiled, is("-p12"));

    expectThrows(OptionsException.class, () -> OptionsSpecType.LongOption.getP4CommandOptionField(null, "p", "12L"));

    p4CommandOptionFiled = OptionsSpecType.LongOption.getP4CommandOptionField(null, "p", 12);
    assertThat(p4CommandOptionFiled, is("-p12"));

    p4CommandOptionFiled = OptionsSpecType.LongOption.getP4CommandOptionField(null, "p", 12L);
    assertThat(p4CommandOptionFiled, is("-p12"));


    expectThrows(OptionsException.class, () -> OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "12L"));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "false"), is(EMPTY));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "False"), is(EMPTY));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "FALSE"), is(EMPTY));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "FAlSE"), is(EMPTY));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", false), is(EMPTY));

    expectThrows(OptionsException.class, () -> OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", 0));
    expectThrows(OptionsException.class, () -> OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", 1));
    expectThrows(OptionsException.class, () -> OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "not valid boolean value"));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "true"), is("-p"));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "True"), is("-p"));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "TrUE"), is("-p"));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", "TRUE"), is("-p"));
    assertThat(OptionsSpecType.BoolOption.getP4CommandOptionField(null, "p", true), is("-p"));

    String empty = OptionsSpecType.StringOption.getP4CommandOptionField(null, "p", null);
    assertThat(empty, is(""));
    expectThrows(OptionsException.class, () -> OptionsSpecType.StringOption.getP4CommandOptionField("haha", "p", "f"));

    String value1 = OptionsSpecType.StringOption.getP4CommandOptionField(null, "p", "f");
    assertThat(value1, is("-pf"));

    List<String> p4CommandOptionFields = OptionsSpecType.StringOption.getP4CommandOptionFields(null, "p", "f");
    assertThat(p4CommandOptionFields.size(), is(1));
    assertThat(p4CommandOptionFields.get(0), is("-pf"));

    expectThrows(OptionsException.class, () -> OptionsSpecType.StringArrayOption.getP4CommandOptionFields(null, "f", new Object[]{"a", 3}));

    List<String> optionsFields = OptionsSpecType.StringArrayOption.getP4CommandOptionFields(null, "e", new String[]{"mykey-*", "sshKey-*"});
    assertThat(optionsFields.size(), is(2));
    assertThat(optionsFields.get(0), is("-emykey-*"));
    assertThat(optionsFields.get(1), is("-esshKey-*"));

    optionsFields = OptionsSpecType.StringArrayOption.getP4CommandOptionFields(null, "e", new String[]{});
    assertThat(optionsFields.size(), is(0));
  }
}