/**
 *
 */
package com.perforce.p4java.impl.generic.core.file;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CHANGE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FILESIZE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PATH;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REV;

import java.util.Map;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSize;

/**
 * Default implementation for the IFileSize interface.
 */
public class FileSize implements IFileSize {
  private String depotFile = null;
  private long revisionId = 0;
  private long fileSize = 0;
  private String path = null;
  private long fileCount = 0;
  private long changeListId = IChangelist.UNKNOWN;

  public FileSize(Map<String, Object> map) {
    if (nonNull(map)) {
      if (nonNull(map.get(DEPOT_FILE))) {
        setDepotFile(parseString(map, DEPOT_FILE));
      }
      if (nonNull(map.get(REV))) {
        setRevisionId(parseLong(map, REV));
      }
      if (nonNull(map.get(FILESIZE))) {
        setFileSize(parseLong(map, FILESIZE));
      }
      if (nonNull(map.get(PATH))) {
        setPath(parseString(map, PATH));
      }
      if (nonNull(map.get("fileCount"))) {
        setFileCount((parseLong(map, "fileCount")));
      }
      if (nonNull(map.get(CHANGE))) {
        if ("default".equalsIgnoreCase(parseString(map, CHANGE))) {
          setChangelistId(IChangelist.DEFAULT);
        } else {
          setChangelistId((parseLong(map, CHANGE)));
        }
      }
    }
  }

  @Override
  public void setChangelistId(long changeListId) {
    this.changeListId = changeListId;
  }

  public FileSize(
      String depotFile,
      long revisionId,
      long fileSize,
      String path,
      long fileCount,
      long changeListId) {

    this.depotFile = depotFile;
    this.revisionId = revisionId;
    this.fileSize = fileSize;
    this.path = path;
    this.fileCount = fileCount;
    this.changeListId = changeListId;
  }

  @Override
  public String getDepotFile() {
    return depotFile;
  }

  @Override
  public void setDepotFile(String depotFile) {
    this.depotFile = depotFile;
  }

  @Override
  public long getFileCount() {
    return fileCount;
  }

  @Override
  public void setFileCount(long fileCount) {
    this.fileCount = fileCount;
  }

  @Override
  public long getFileSize() {
    return fileSize;
  }

  @Override
  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public long getRevisionId() {
    return revisionId;
  }

  @Override
  public void setRevisionId(long revisionId) {
    this.revisionId = revisionId;
  }

  @Override
  public long getChangelistId() {
    return changeListId;
  }
}
