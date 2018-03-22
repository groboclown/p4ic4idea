/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core.file;

import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.STATUS;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TYPE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TYPE2;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Map;

import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.impl.generic.core.file.FileSpec;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
public class FileDiff implements IFileDiff {

  private Status status = null;
  private String file1 = null;
  private String file2 = null;
  private int revision1 = -1;
  private int revision2 = -1;
  private String type1 = null;
  private String type2 = null;

  /**
   * Create a new file diff with the values from the specified map
   */
  public FileDiff(final Map<String, Object> map) {
    String depotFile1 = parseString(map, DEPOT_FILE);
    if (isNotBlank(depotFile1)) {
      file1 = depotFile1;
      revision1 = FileSpec.getRevFromString(parseString(map, REV));
      type1 = parseString(map, TYPE);
    }

    String depotFile2 = parseString(map, DEPOT_FILE + "2");
    if (isNotBlank(depotFile2)) {
      file2 = depotFile2;
      revision2 = FileSpec.getRevFromString(parseString(map, REV + "2"));
      type2 = parseString(map, TYPE2);
    }
    status = Status.fromString(parseString(map, STATUS));
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override

  public String getDepotFile1() {
    return file1;
  }

  @Override

  public String getDepotFile2() {
    return file2;
  }

  @Override

  public int getRevision1() {
    return revision1;
  }

  @Override

  public int getRevision2() {
    return revision2;
  }

  @Override

  public String getFileType1() {
    return type1;
  }

  @Override
  public String getFileType2() {
    return type2;
  }
}
