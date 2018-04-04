package com.perforce.p4java.common.base;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;

import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.option.server.DiffsOptions;

/**
 * @author Sean Shou
 * @since 6/10/2016
 */
public class FileDiffUtils {
  private FileDiffUtils() { /* util */ }

  public static void setFileDiffsOptionsByDiffType(DiffType diffType, DiffsOptions opts) {
    if (nonNull(diffType)) {
      switch (diffType) {
        case RCS_DIFF:
          opts.setRcsDiffs(true);
          break;
        case CONTEXT_DIFF:
          opts.setDiffContext(0);
          break;
        case SUMMARY_DIFF:
          opts.setSummaryDiff(true);
          break;
        case UNIFIED_DIFF:
          opts.setUnifiedDiff(0);
          break;
        case IGNORE_WS_CHANGES:
          opts.setIgnoreWhitespaceChanges(true);
          break;
        case IGNORE_WS:
          opts.setIgnoreWhitespace(true);
          break;
        case IGNORE_LINE_ENDINGS:
          opts.setIgnoreLineEndings(true);
          break;
      }
    }
  }
}
