package com.perforce.p4java.core.file;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;


/**
 * @author Sean Shou
 * @since 31/08/2016
 */
@RunWith(JUnitPlatform.class)
public class FileSpecBuilderTest {
  @Test
  public void makeFileSpecList_non_empty_path_string() throws Exception {
    String depotFileSpec = "//depot/p4java/test/test.txt";
    List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(depotFileSpec);
    assertThat(fileSpecs.size(), is(1));
    assertThat(fileSpecs.get(0).getOriginalPathString(), is(depotFileSpec));
  }

  @Test
  public void makeFileSpecList_non_empty_path_string_and_has_space() throws Exception {
    String depotFileSpec = "//depot/p4java/test/test copy.txt";
    List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(depotFileSpec);
    assertThat(fileSpecs.size(), is(1));
    assertThat(fileSpecs.get(0).getOriginalPathString(), is(depotFileSpec));
  }

  @Test
  public void makeFileSpecList_blank_path_string() throws Exception {
    String depotFileSpec = "";
    List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(depotFileSpec);
    assertThat(fileSpecs.size(), is(0));

    depotFileSpec = StringUtils.SPACE;
    fileSpecs = FileSpecBuilder.makeFileSpecList(depotFileSpec);
    assertThat(fileSpecs.size(), is(0));
  }

  @Test
  public void makeFileSpecList_null_path_string() throws Exception {
    List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList();
    assertThat(fileSpecs.size(), is(0));
  }

  @Test
  public void makeFileSpecList_hasRevisionAnnotations() throws Exception {
    String depotFileSpec = "//depot/p4java/test/test.txt";
    String revision = "#2";
    List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(depotFileSpec + revision);
    assertThat(fileSpecs.size(), is(1));
    IFileSpec fileSpec = fileSpecs.get(0);
    assertThat(fileSpec.getOriginalPathString(), is(depotFileSpec));
    assertThat(fileSpec.getStartRevision(), is(-1));
    assertThat(fileSpec.getEndRevision(), is(2));
  }

  @Test
  public void makeFileSpecList_valid_list() throws Exception {
    String depotFileSpec = "//depot/p4java/test/test.txt";
    String revision = "#2";
    List<String> filePaths = ImmutableList.of(depotFileSpec + revision, depotFileSpec + "#3,#4");
    List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
    assertThat(fileSpecs.size(), is(2));
    IFileSpec fileSpec1 = fileSpecs.get(0);
    assertThat(fileSpec1.getOriginalPathString(), is(depotFileSpec));
    assertThat(fileSpec1.getStartRevision(), is(-1));
    assertThat(fileSpec1.getEndRevision(), is(2));

    IFileSpec fileSpec2 = fileSpecs.get(1);
    assertThat(fileSpec2.getOriginalPathString(), is(depotFileSpec));
    assertThat(fileSpec2.getStartRevision(), is(3));
    assertThat(fileSpec2.getEndRevision(), is(4));

  }

  @Test
  public void getValidFileSpecs_and_InvalidFileSpecs() throws Exception {
    IFileSpec validFileSpec1 = mock(IFileSpec.class);
    when(validFileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);

    IFileSpec validFileSpec2 = mock(IFileSpec.class);
    when(validFileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);

    IFileSpec invalidFileSpec = mock(IFileSpec.class);
    when(invalidFileSpec.getOpStatus()).thenReturn(FileSpecOpStatus.ERROR);

    List<IFileSpec> fileSpecs = ImmutableList.of(validFileSpec1, validFileSpec2, invalidFileSpec);
    List<IFileSpec> validFileSpecs = FileSpecBuilder.getValidFileSpecs(fileSpecs);
    assertThat(validFileSpecs.size(), is(2));

    List<IFileSpec> invalidFileSpecs = FileSpecBuilder.getInvalidFileSpecs(fileSpecs);
    assertThat(invalidFileSpecs.size(), is(1));
  }

  @Test
  public void getValidFileSpecs_and_InvalidFileSpecs_with_null() throws Exception {
    List<IFileSpec> validFileSpecs = FileSpecBuilder.getValidFileSpecs(null);
    assertThat(validFileSpecs.size(), is(0));

    List<IFileSpec> invalidFileSpecs = FileSpecBuilder.getInvalidFileSpecs(null);
    assertThat(invalidFileSpecs.size(), is(0));
  }

  @Test
  public void testFileSpecWithAtAnnotation() throws Exception {
    String annotatedLabel = "@15";
    List<IFileSpec> spec = FileSpecBuilder.makeFileSpecList(annotatedLabel);
    assertThat(spec.size(), is(1));
    assertThat(spec.get(0).getAnnotatedPreferredPathString(), is(annotatedLabel));
  }

  @Test
  public void testFileSpecWithSharpAnnotation() throws Exception {
    String annotatedLabel = "#15";
    List<IFileSpec> spec = FileSpecBuilder.makeFileSpecList(annotatedLabel);
    assertThat(spec.size(), is(1));
    assertThat(spec.get(0).getAnnotatedPreferredPathString(), is(annotatedLabel));
  }
}