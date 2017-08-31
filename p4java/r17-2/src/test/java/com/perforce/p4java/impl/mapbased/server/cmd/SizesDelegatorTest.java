package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.server.CmdSpec.SIZES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetFileSizesOptions;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 5/10/2016
 */
public class SizesDelegatorTest extends AbstractP4JavaUnitTest {
  private static final String[] CMD_OPTIONS = {"-a", "-m 20"};
  private static final String TEST_FILE_DEPOT_PATH = "//depot/dev/test.txt";
  private static final String[] CMD_ARGUMENTS = ArrayUtils.add(CMD_OPTIONS, TEST_FILE_DEPOT_PATH);

  private SizesDelegator sizesDelegator;
  private Map<String, Object> resultMap;
  private List<Map<String, Object>> resultMaps;

  private List<IFileSpec> fileSpecs;
  private GetFileSizesOptions opts;


  /**
   * Runs before every test.
   */
  @SuppressWarnings("unchecked")
  @Before
  public void beforeEach() throws ConnectionException, AccessException, RequestException {
    server = mock(Server.class);
    sizesDelegator = new SizesDelegator(server);

    resultMap = mock(Map.class);
    when(resultMap.get(DEPOT_FILE)).thenReturn(TEST_FILE_DEPOT_PATH);
    resultMaps = newArrayList(resultMap);

    fileSpecs = FileSpecBuilder.makeFileSpecList(TEST_FILE_DEPOT_PATH);
    opts = new GetFileSizesOptions(CMD_OPTIONS);

    when(server.execMapCmdList(eq(SIZES.toString()), eq(CMD_ARGUMENTS), eq(null)))
            .thenReturn(resultMaps);
  }

  /**
   * Expected return non empty file sizes
   * @throws Exception
   */
  @Test
  public void shouldReturnNonEmptyFileSizes() throws Exception {
    //when
    List<IFileSize> fileSizes = sizesDelegator.getFileSizes(fileSpecs, opts);
    //then
    assertThat(fileSizes.size(), is(1));
    assertThat(fileSizes.get(0).getDepotFile(), is(TEST_FILE_DEPOT_PATH));
  }
}