package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.server.CmdSpec.DUPLICATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.DuplicateRevisionsOptions;

/**
 * @author Sean Shou
 * @since 27/09/2016
 */
public class DuplicateDelegatorTest extends AbstractP4JavaUnitTest {
  private DuplicateDelegator duplicateDelegator;
  private Map<String, Object> resultMap;
  private List<Map<String, Object>> resultMaps;

  private List<IFileSpec> mockFileSpecs;
  private IFileSpec mockFileSpec;

  /**
   * Runs before every test.
   */
  @SuppressWarnings("unchecked")
  @Before
  public void beforeEach() {
    server = mock(Server.class);
    duplicateDelegator = new DuplicateDelegator(server);
    resultMap = mock(Map.class);
    resultMaps = newArrayList(resultMap);

    mockFileSpecs = newArrayList();
    mockFileSpec = mock(IFileSpec.class);
    mockFileSpecs.add(mockFileSpec);
  }

  /**
   * Test duplicate revisions
   *
   * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
   */
  @Test
  public void testDuplicateRevisions() throws Exception {
    //given
      when(server.execMapCmdList(
              eq(DUPLICATE.toString()),
              any(String[].class), eq(null))).thenReturn(resultMaps);
    //when
      List<IFileSpec> fileSpecs = duplicateDelegator.duplicateRevisions(
              mock(IFileSpec.class),
              mock(IFileSpec.class),
              mock(DuplicateRevisionsOptions.class));
    //then
    assertThat(fileSpecs.size(), is(1));
  }
}