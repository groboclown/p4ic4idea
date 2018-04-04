package com.perforce.p4java.option;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.expectThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.server.IServer;

/**
 * @author Sean Shou
 * @since 8/09/2016
 */
@RunWith(JUnitPlatform.class)
public class OptionsTest {
  @Test
  public void testDefaultConstruct() {
    Options options = new Options() {
      @Override
      public List<String> processOptions(IServer server) throws OptionsException {
        return null;
      }
    };

    assertNull(options.getOptions());
    assertFalse(options.isImmutable());
  }

  @Test
  public void testDefaultConstructWithOptions() {
    Options options = new MockOptions("-m10", "-uhreid");
    assertThat(options.getOptions().size(), is(2));
    assertTrue(options.isImmutable());
  }

  @Test
  public void testProcessFields() throws OptionsException {
    Options options = new MockOptions();
    String optionSpecsString = "b:f s:r i:d:gtz s[]:e";
    String[] stringArrayOptions = new String[]{"myKey-*", "sshKey-*"};
    List<String> processFields = options.processFields(optionSpecsString, false, "@2016/09/08", 15, stringArrayOptions);
    assertThat(processFields.size(), is(4));
    assertThat(processFields.get(0), is("-r@2016/09/08"));
    assertThat(processFields.get(1), is("-d15"));
    assertThat(processFields.get(2), is("-emyKey-*"));
    assertThat(processFields.get(3), is("-esshKey-*"));
  }

  @Test
  public void testProcessFields_withExceptions() {
    Options options = new MockOptions();
    expectThrows(OptionsException.class, () -> options.processFields(null, new Object[]{true, true}));

    String optionSpecsString = "b:f b:r b:d b:p";
    expectThrows(OptionsException.class, () -> options.processFields(optionSpecsString, new Object[]{true, true}));

    String optionSpecsStringHasSpace = "b:f   b:r";
    expectThrows(OptionsException.class, () -> options.processFields(optionSpecsStringHasSpace, new Object[]{true, true}));

    String optionSpecsStringHasNull = "b:f null";
    expectThrows(OptionsException.class, () -> options.processFields(optionSpecsStringHasNull, new Object[]{true, true}));

    String optionSpecsStringWithSpaceValue = "b: b:r";
    expectThrows(OptionsException.class, () -> options.processFields(optionSpecsStringWithSpaceValue, new Object[]{true, true}));

    String optionSpecsStringHasMoreThan3Parts = "b:f:a:d b:r";
    expectThrows(OptionsException.class, () -> options.processFields(optionSpecsStringHasMoreThan3Parts, new Object[]{true, true}));

    String unknownOptionType = "b:f:a:d d:r";
    expectThrows(OptionsException.class, () -> options.processFields(unknownOptionType, new Object[]{true, true}));

    expectThrows(OptionsException.class, () -> options.processFields(unknownOptionType, new Object[]{true, true}));

    String optionSpecString = "b:f s:p l:g";
    Object[] wrongValue = new Object[]{false, "test", "wrong long number"};
    expectThrows(OptionsException.class, () -> options.processFields(optionSpecString, wrongValue));

    String optionSpecString2 = "b:f s:p s[]:g";
    Object[] isNotStringArray = new Object[]{"a", 1, "d"};
    Object[] wrongValue2 = new Object[]{false, "test", isNotStringArray};
    expectThrows(OptionsException.class, () -> options.processFields(optionSpecString2, wrongValue2));
  }

  @Test
  public void testSetOption() {
    Options options = new MockOptions();
    options.setOptions();

    assertThat(options.getOptions().size(), is(0));

    options.setOptions("-p", "-r");
    assertThat(options.getOptions().size(), is(2));
    assertThat(options.getOptions().get(0), is("-p"));
    assertThat(options.getOptions().get(1), is("-r"));

    options.setOptions(null);
    assertNull(options.getOptions());
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