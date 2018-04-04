package com.perforce.p4java.option.server;

import com.perforce.p4java.option.Options;

/**
 * @author Sean Shou
 * @since 6/10/2016
 */
public interface DiffsOptions<T extends Options> {
  T setRcsDiffs(boolean rcsDiffs);

  T setDiffContext(int diffContext);

  T setSummaryDiff(boolean summaryDiff);

  T setUnifiedDiff(int unifiedDiff);

  T setIgnoreWhitespaceChanges(boolean ignoreWhitespaceChanges);

  T setIgnoreWhitespace(boolean ignoreWhitespace);

  T setIgnoreLineEndings(boolean ignoreLineEndings);
}
