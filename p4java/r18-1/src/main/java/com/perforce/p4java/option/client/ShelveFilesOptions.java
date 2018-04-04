package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IClient.shelveFiles method and associated
 * convenience methods.
 *
 * @see com.perforce.p4java.client.IClient#shelveFiles(java.util.List, int,
 * com.perforce.p4java.option.client.ShelveFilesOptions)
 */
public class ShelveFilesOptions extends Options {

  /**
   * Options: -f, -r, -d, -p
   */
  public static final String OPTIONS_SPECS = "b:f b:r b:d b:p";

  /**
   * If true, force the shelve operation; corresponds to the -f flag
   */
  protected boolean forceShelve = false;

  /**
   * If true, allow the incoming files to replace the shelved files;
   * corresponds to the -r flag.
   */
  protected boolean replaceFiles = false;

  /**
   * If true, promotes a shelved change from an edge server to a commit
   * server where it can be accessed by other edge servers participating
   * in the distributed configuration.
   * corresponds to the -p flag.
   */
  protected boolean promotesShelvedChangeIfDistributedConfigured = false;

  /**
   * If true, delete incoming files from the shelf; corresponds to the -d flag.
   */
  protected boolean deleteFiles = false;

  /**
   * Default constructor..
   */
  public ShelveFilesOptions() {
    super();
  }

  /**
   * Strings-based constructor; see 'p4 help [command]' for possible options.
   * <p>
   *
   * <b>WARNING: you should not pass more than one option or argument in each
   * string parameter. Each option or argument should be passed-in as its own
   * separate string parameter, without any spaces between the option and the
   * option value (if any).<b>
   * <p>
   *
   * <b>NOTE: setting options this way always bypasses the internal options
   * values, and getter methods against the individual values corresponding to
   * the strings passed in to this constructor will not normally reflect the
   * string's setting. Do not use this constructor unless you know what you're
   * doing and / or you do not also use the field getters and setters.</b>
   *
   * @see com.perforce.p4java.option.Options#Options(java.lang.String...)
   */
  public ShelveFilesOptions(String... options) {
    super(options);
  }

  /**
   * Explicit-value constructor.
   */
  public ShelveFilesOptions(final boolean forceShelve,
                            final boolean replaceFiles,
                            final boolean deleteFiles,
                            final boolean promotesShelvedChangeIfDistributedConfigured) {
    super();
    this.forceShelve = forceShelve;
    this.replaceFiles = replaceFiles;
    this.deleteFiles = deleteFiles;
    this.promotesShelvedChangeIfDistributedConfigured = promotesShelvedChangeIfDistributedConfigured;
  }

  /**
   * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
   */
  public List<String> processOptions(IServer server) throws OptionsException {
    optionList = processFields(OPTIONS_SPECS,
        isForceShelve(),
        isReplaceFiles(),
        isDeleteFiles(),
        isPromotesShelvedChangeIfDistributedConfigured());
    return optionList;
  }

  public boolean isForceShelve() {
    return forceShelve;
  }

  public ShelveFilesOptions setForceShelve(boolean forceShelve) {
    this.forceShelve = forceShelve;
    return this;
  }

  public boolean isReplaceFiles() {
    return replaceFiles;
  }

  public ShelveFilesOptions setReplaceFiles(boolean replaceFiles) {
    this.replaceFiles = replaceFiles;
    return this;
  }

  public boolean isPromotesShelvedChangeIfDistributedConfigured() {
    return promotesShelvedChangeIfDistributedConfigured;
  }

  public ShelveFilesOptions setPromotesShelvedChangeIfDistributedConfigured(boolean promotesShelvedChangeIfDistributedConfigured) {
    this.promotesShelvedChangeIfDistributedConfigured = promotesShelvedChangeIfDistributedConfigured;
    return this;
  }

  public boolean isDeleteFiles() {
    return deleteFiles;
  }

  public ShelveFilesOptions setDeleteFiles(boolean deleteFiles) {
    this.deleteFiles = deleteFiles;
    return this;
  }
}
