/**
 * Copyright 2013 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core.file;


/**
 * Describes information about the size of the files in the depot. For specified file specification,
 * it shows the depot file name, revision, file count and file size. If you use client syntax for
 * the file specification,the view mapping is used to list the corresponding depot files. Full field
 * semantics and usage are given in the main Perforce documentation.
 */
public interface IFileSize {

  /**
   * Get the depot file.
   */
  String getDepotFile();

  /**
   * Set the depot file.
   */
  void setDepotFile(String depotFile);

  /**
   * Get the file revision ID.
   */
  long getRevisionId();

  /**
   * Set the file revision ID.
   */
  void setRevisionId(long revisionId);

  /**
   * Get the file size.
   */
  long getFileSize();

  /**
   * Set the file size.
   */
  void setFileSize(long fileSize);

  /**
   * Get the path.
   */
  String getPath();

  /**
   * Set the path.
   */
  void setPath(String path);

  /**
   * Get the file count.
   */
  long getFileCount();

  /**
   * Set the file count.
   */
  void setFileCount(long fileCount);

  /**
   * Get the shelved changelist ID.
   */
  long getChangelistId();

  /**
   * Set the shelved changelist ID.
   */
  void setChangelistId(long changeListId);
}
