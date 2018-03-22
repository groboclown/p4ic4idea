/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TYPE;

import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.impl.generic.core.file.FileSpec;

/**
 * Implementation class of the {@link IFileLineMatch} interface
 */
public class FileLineMatch implements IFileLineMatch {
  protected String file = null;
  protected int revision = -1;
  protected int lineNumber = -1;
  protected String line = null;
  protected MatchType type = MatchType.MATCH;

  /**
   * Explicit-value constructor; sets all fields to null or -1, type to MatchType.MATCH.
   */
  public FileLineMatch(final String file,
                       final int revision,
                       final String line,
                       final MatchType type) {
    this.file = file;
    this.revision = revision;
    this.line = line;
    this.type = type;
  }

  public FileLineMatch(Map<String, Object> map) {
    if (nonNull(map)) {
      file = parseString(map, DEPOT_FILE);
      revision = FileSpec.getRevFromString(parseString(map, REV));
      if (nonNull(map.get("line"))) {
        try {
          lineNumber = parseInt(map, "line");
        } catch (NumberFormatException exc) {
          Log.warn("NumberFormatException in FileLineMatch map-based constructor: %s",
              exc.getLocalizedMessage());
        }
      }
      line = parseString(map, "matchedLine");
      type = MatchType.fromServerString(parseString(map, TYPE));
    }
  }

  @Override
  public String getDepotFile() {
    return file;
  }

  @Override
  public String getLine() {
    return line;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  public MatchType getType() {
    return type;
  }
}
