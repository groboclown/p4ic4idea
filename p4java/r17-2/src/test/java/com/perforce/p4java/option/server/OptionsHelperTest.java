package com.perforce.p4java.option.server;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.expectThrows;

import org.junit.Test;

import com.perforce.p4java.exception.OptionsException;

/**
 * @author Sean Shou
 * @since 9/09/2016
 */
public class OptionsHelperTest {
  @Test
  public void applyRule_boolean() throws Exception {
    expectThrows(OptionsException.class, () -> OptionsHelper.applyRule("nonnull", "e", true));
    expectThrows(OptionsException.class, () -> OptionsHelper.applyRule(null, null, true));

    String optionsField = OptionsHelper.applyRule(null, "f", true);
    assertThat(optionsField, is("-f"));

    optionsField = OptionsHelper.applyRule(null, "f", false);
    assertThat(optionsField, is(EMPTY));
  }

  @Test
  public void applyRule_integer() throws Exception {
    expectThrows(OptionsException.class, () -> OptionsHelper.applyRule(null, null, 1));
    expectThrows(OptionsException.class, () -> OptionsHelper.applyRule("nonnull", "not-exist", 1));

    String optionsField = OptionsHelper.applyRule(null, "d", 12);
    assertThat(optionsField, is("-d12"));

    optionsField = OptionsHelper.applyRule("gtz", "d", 12);
    assertThat(optionsField, is("-d12"));

    optionsField = OptionsHelper.applyRule("gtz", "d", 0);
    assertThat(optionsField, is(EMPTY));

    optionsField = OptionsHelper.applyRule("cl", "d", 0);
    assertThat(optionsField, is("-ddefault"));

    optionsField = OptionsHelper.applyRule("cl", "d", 12);
    assertThat(optionsField, is("-d12"));

    optionsField = OptionsHelper.applyRule("cl", "d", -1);
    assertThat(optionsField, is(EMPTY));

    optionsField = OptionsHelper.applyRule("clz", "d", 12);
    assertThat(optionsField, is("-d12"));

    optionsField = OptionsHelper.applyRule("clz", "d", 0);
    assertThat(optionsField, is(EMPTY));

    optionsField = OptionsHelper.applyRule("dcn", "d", 12);
    assertThat(optionsField, is("-d12"));

    optionsField = OptionsHelper.applyRule("dcn", "d", 0);
    assertThat(optionsField, is("-d"));

    optionsField = OptionsHelper.applyRule("dcn", "d", -1);
    assertThat(optionsField, is(EMPTY));

  }

  @Test
  public void applyRule_long() throws Exception {
    expectThrows(OptionsException.class, () -> OptionsHelper.applyRule(null, null, 1L));
    expectThrows(OptionsException.class, () -> OptionsHelper.applyRule("nonnull", "not-exist", 1L));

    String optionsField = OptionsHelper.applyRule(null, "d", 2L);
    assertThat(optionsField, is("-d2"));

    optionsField = OptionsHelper.applyRule("gtz", "d", 2L);
    assertThat(optionsField, is("-d2"));

    optionsField = OptionsHelper.applyRule("gtz", "d", 0L);
    assertThat(optionsField, is(EMPTY));

    optionsField = OptionsHelper.applyRule("gez", "d", 0L);
    assertThat(optionsField, is("-d0"));

    optionsField = OptionsHelper.applyRule("gez", "d", 2L);
    assertThat(optionsField, is("-d2"));

    optionsField = OptionsHelper.applyRule("gez", "d", -2L);
    assertThat(optionsField, is(EMPTY));

  }

  @Test
  public void applyRule_String() throws Exception {
    expectThrows(OptionsException.class, () -> OptionsHelper.applyRule(null, null, "string"));
    expectThrows(OptionsException.class, () -> OptionsHelper.applyRule("notnull", null, "string"));

    String optionsField = OptionsHelper.applyRule(null, "d", null);
    assertThat(optionsField, is(EMPTY));

    optionsField = OptionsHelper.applyRule(null, "d", EMPTY);
    assertThat(optionsField, is(EMPTY));

    optionsField = OptionsHelper.applyRule(null, "d", SPACE);
    assertThat(optionsField, is(EMPTY));

    optionsField = OptionsHelper.applyRule(null, "o", "myClient");
    assertThat(optionsField, is("-omyClient"));
  }

  @Test
  public void objectToBoolean() throws Exception {
    expectThrows(OptionsException.class, () -> OptionsHelper.objectToBoolean(null));
    expectThrows(OptionsException.class, () -> OptionsHelper.objectToBoolean(EMPTY));
    expectThrows(OptionsException.class, () -> OptionsHelper.objectToBoolean(SPACE));
    expectThrows(OptionsException.class, () -> OptionsHelper.objectToBoolean(1L));
    expectThrows(OptionsException.class, () -> OptionsHelper.objectToBoolean("not boolean"));

    Object optValue = true;
    boolean actual = OptionsHelper.objectToBoolean(optValue);
    assertThat(actual, is(true));

    optValue = false;
    actual = OptionsHelper.objectToBoolean(optValue);
    assertThat(actual, is(false));

    optValue = "true";
    actual = OptionsHelper.objectToBoolean(optValue);
    assertThat(actual, is(true));

    optValue = "TruE";
    actual = OptionsHelper.objectToBoolean(optValue);
    assertThat(actual, is(true));

    optValue = "false";
    actual = OptionsHelper.objectToBoolean(optValue);
    assertThat(actual, is(false));

    optValue = "fALse";
    actual = OptionsHelper.objectToBoolean(optValue);
    assertThat(actual, is(false));
  }
}