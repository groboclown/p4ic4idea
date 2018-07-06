package com.perforce.p4java.impl.generic.core.file;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IRevisionIntegrationData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ACTION;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CHANGE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TIME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TYPE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.USER;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Simple default generic implementation calls for the IFileRevisionData
 * interface.
 */
public class FileRevisionData implements IFileRevisionData {
  private int revision = 0;
  private int changeListId = IChangelist.UNKNOWN;
  private FileAction action = null;
  private Date date = null;
  private String userName = null;
  private String fileType = null;
  private String description = null;
  private String depotFileName = null;
  private String clientName = null;
  private List<IRevisionIntegrationData> revisionIntegrationDataList = null;

  public FileRevisionData() {
  }

  public FileRevisionData(final Map<String, Object> map, final int revNo) {
    if (nonNull(map)) {
      String revStr = EMPTY;
      if (revNo >= 0) {
        revStr += revNo;
      }

      setRevision(parseInt(map, REV + revNo));
      String changeId = parseString(map, CHANGE + revStr);
      if (isNotBlank(changeId)) {
        if ("default".equalsIgnoreCase(changeId)) {
          setChangelistId(IChangelist.DEFAULT);
        } else {
          setChangelistId(Integer.parseInt(changeId));
        }
      }
      setAction(FileAction.fromString(parseString(map, ACTION + revStr)));
      setDate(new Date(parseLong(map, TIME + revStr) * 1000));
      setUserName(parseString(map, USER + revStr));
      setFileType(parseString(map, TYPE + revStr));
      setDescription(parseString(map, "desc" + revStr));
      setDepotFileName(parseString(map, DEPOT_FILE));
      setClientName(parseString(map, "client" + revStr));

      int revRev = 0;

      String fromFile = parseString(map, FILE + revNo + "," + revRev);

      while (isNotBlank(fromFile)) {
        if (isNull(revisionIntegrationDataList)) {
          revisionIntegrationDataList = new ArrayList<>();
        }

        RevisionIntegrationData revisionIntegrationData = new RevisionIntegrationData(
            parseString(map, "srev" + revNo + "," + revRev),
            parseString(map, "erev" + revNo + "," + revRev),
            fromFile,
            parseString(map, "how" + revNo + "," + revRev)
        );

        revisionIntegrationDataList.add(revisionIntegrationData);
        revRev++;
        fromFile = parseString(map, FILE + revNo + "," + revRev);
      }
    }
  }

  public void setChangelistId(int changeListId) {
    this.changeListId = changeListId;
  }

  public FileRevisionData(final int revision,
                          final int changeListId,
                          final FileAction action,
                          final Date date,
                          final String userName,
                          final String fileType,
                          final String description,
                          final String depotFileName,
                          final String clientName) {
    this.revision = revision;
    this.changeListId = changeListId;
    this.action = action;
    this.date = date;
    this.userName = userName;
    this.fileType = fileType;
    this.description = description;
    this.depotFileName = depotFileName;
    this.clientName = clientName;
  }

  public FileAction getAction() {
    return action;
  }

  public void setAction(FileAction action) {
    this.action = action;
  }

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getDepotFileName() {
    return depotFileName;
  }

  public void setDepotFileName(String depotFileName) {
    this.depotFileName = depotFileName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public int getRevision() {
    return revision;
  }

  public void setRevision(int revision) {
    this.revision = revision;
  }

  public List<IRevisionIntegrationData> getRevisionIntegrationDataList() {
    return revisionIntegrationDataList;
  }

  @Deprecated
  public List<IRevisionIntegrationData> getRevisionIntegrationData() {
    return getRevisionIntegrationDataList();
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }


  public int getChangelistId() {
    return changeListId;
  }

  // p4ic4idea: better debugging support
  @Override
  public String toString() {
    return depotFileName + '#' + revision + "/@" + changeListId;
  }

  private class RevisionIntegrationData implements IRevisionIntegrationData {
    private int startFromRev = 0;
    private int endFromRev = 0;
    private String fromFile = null;
    private String howFrom = null;

    RevisionIntegrationData(String startFromRev,
                            String endFromRev,
                            String fromFile,
                            String howFrom) {
      this.startFromRev = FileSpec.getRevFromString(startFromRev);
      this.endFromRev = FileSpec.getRevFromString(endFromRev);
      this.fromFile = fromFile;
      this.howFrom = howFrom;
    }

    public int getEndFromRev() {
      return endFromRev;
    }

    public String getFromFile() {
      return fromFile;
    }

    public String getHowFrom() {
      return howFrom;
    }

    public int getStartFromRev() {
      return startFromRev;
    }
  }
}
