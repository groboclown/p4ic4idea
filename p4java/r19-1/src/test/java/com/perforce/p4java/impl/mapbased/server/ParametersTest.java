package com.perforce.p4java.impl.mapbased.server;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

public class ParametersTest extends P4JavaTestCase {

  @Test
  public void addOpts() throws Exception {
    Parameters parameters = new Parameters();
    Method addOpts = getPrivateMethod(Parameters.class, "addOpts", List.class, Options.class, IServer.class);
    List<String> args = new ArrayList<String>();
    addOpts.invoke(parameters, args, null, null);
    assertThat(args.size(), is(0));


    Options opts = mock(Options.class);
    when(opts.isImmutable()).thenReturn(true);
    ArrayList<String> optionsStrings = new ArrayList<String>();
    optionsStrings.add("-a");
    optionsStrings.add("-f");
    optionsStrings.add("-s");
    when(opts.getOptions()).thenReturn(optionsStrings);
    args = new ArrayList<String>();
    addOpts.invoke(parameters, args, opts, mock(IServer.class));
    assertEquals(optionsStrings, args);

    reset(opts);
    IServer server = mock(IServer.class);
    when(opts.isImmutable()).thenReturn(false);
    when(opts.processOptions(server)).thenReturn(optionsStrings);
    args = new ArrayList<String>();
    addOpts.invoke(parameters, args, opts, server);
    assertEquals(optionsStrings, args);
  }

  @Test
  public void addFileSpecs() throws Exception {
    Parameters parameters = new Parameters();
    Method addFileSpecs = getPrivateMethod(Parameters.class, "addFileSpecs", List.class, List.class);
    try {
        addFileSpecs.invoke(parameters, null, null);
        fail("should have thown InvocationTargetException.class");
    } catch (Throwable e) {
        assertEquals(InvocationTargetException.class, e.getClass());
    }

    List<String> args = new ArrayList<String>();
    addFileSpecs.invoke(parameters, args, null);
    assertThat(args.size(), is(0));

    List<IFileSpec> fileSpecs = new ArrayList<IFileSpec>();
    IFileSpec fileSpec1 = mock(IFileSpec.class);
    when(fileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec1.getAnnotatedPreferredPathString()).thenReturn("//depot/test");
    fileSpecs.add(fileSpec1);

    IFileSpec fileSpec2 = mock(IFileSpec.class);
    when(fileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.UNKNOWN);
    fileSpecs.add(fileSpec2);
    addFileSpecs.invoke(parameters, args, fileSpecs);
    assertThat(args.size(), is(1));
    assertThat(args.get(0), is("//depot/test"));
  }

  @Test
  public void addUnannotatedFileSpecs() throws Exception {
    Parameters parameters = new Parameters();
    Method addUnannotatedFileSpecs = getPrivateMethod(Parameters.class, "addUnannotatedFileSpecs", List.class, List.class);
    try {
        addUnannotatedFileSpecs.invoke(parameters, null, null);
        fail("should have thown InvocationTargetException.class");
    } catch (Throwable e) {
        assertEquals(InvocationTargetException.class, e.getClass());
    }

    List<String> args = new ArrayList<String>();
    addUnannotatedFileSpecs.invoke(parameters, args, null);
    assertThat(args.size(), is(0));

    args = new ArrayList<String>();
    List<IFileSpec> fileSpecs = new ArrayList<IFileSpec>();
    IFileSpec fileSpec1 = mock(IFileSpec.class);
    when(fileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec1.getPreferredPathString()).thenReturn("//depot/test1");
    fileSpecs.add(fileSpec1);

    IFileSpec fileSpec2 = mock(IFileSpec.class);
    when(fileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.UNKNOWN);
    fileSpecs.add(fileSpec2);

    fileSpecs.add(null);
    addUnannotatedFileSpecs.invoke(parameters, args, fileSpecs);
    assertThat(args.size(), is(1));
    assertThat(args.get(0), is("//depot/test1"));
  }

  @Test
  public void addFileSpec() throws Exception {
    Parameters parameters = new Parameters();
    Method addFileSpec = getPrivateMethod(Parameters.class, "addFileSpec", List.class, IFileSpec.class);
    try {
        addFileSpec.invoke(parameters, null, null);
        fail("should have thown InvocationTargetException.class");
    } catch (Throwable e) {
        assertEquals(InvocationTargetException.class, e.getClass());
    }

    List<String> args = new ArrayList<String>();
    IFileSpec fileSpec1 = mock(IFileSpec.class);
    when(fileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec1.getAnnotatedPreferredPathString()).thenReturn("//depot/test1");

    addFileSpec.invoke(parameters, args, fileSpec1);
    assertThat(args.size(), is(1));
    assertThat(args.get(0), is("//depot/test1"));

    args = new ArrayList<String>();
    IFileSpec fileSpec2 = mock(IFileSpec.class);
    when(fileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.ERROR);
    addFileSpec.invoke(parameters, args, fileSpec2);
    assertThat(args.size(), is(0));
  }

  @Test
  public void processParameters_opts_fileSpecs_stringParams_server() throws Exception {
    Options opts = createMockOptions();
    List<IFileSpec> fileSpecs = null;
    String[] stringParams = null;

    String[] processParameters = Parameters.processParameters(opts, fileSpecs, stringParams, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s"}));

    fileSpecs = new ArrayList<IFileSpec>();
    IFileSpec fileSpec1 = mock(IFileSpec.class);
    when(fileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec1.getAnnotatedPreferredPathString()).thenReturn("//depot/test1");
    fileSpecs.add(fileSpec1);

    IFileSpec fileSpec2 = mock(IFileSpec.class);
    when(fileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.UNKNOWN);
    fileSpecs.add(fileSpec2);
    processParameters = Parameters.processParameters(opts, fileSpecs, stringParams, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "//depot/test1"}));

    stringParams = new String[] {"-o"};
    processParameters = Parameters.processParameters(opts, fileSpecs, stringParams, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "-o", "//depot/test1"}));

    processParameters = Parameters.processParameters(opts, fileSpecs, "-o", null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "-o", "//depot/test1"}));

    processParameters = Parameters.processParameters(opts, fileSpecs, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "//depot/test1"}));

    processParameters = Parameters.processParameters(opts, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s"}));
  }

  @Test
  public void processParameters_opts_fileSpecs_stringParams_annotateFiles_server() throws Exception {
    Options opts = createMockOptions();
    List<IFileSpec> fileSpecs = null;
    String[] stringParams = null;

    String[] processParameters = Parameters.processParameters(opts, fileSpecs, stringParams, true, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s"}));

    fileSpecs = new ArrayList<IFileSpec>();
    IFileSpec fileSpec1 = mock(IFileSpec.class);
    when(fileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec1.getAnnotatedPreferredPathString()).thenReturn("//depot/test1");
    when(fileSpec1.getPreferredPathString()).thenReturn("//depot/test1.txt");
    fileSpecs.add(fileSpec1);

    IFileSpec fileSpec2 = mock(IFileSpec.class);
    when(fileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.UNKNOWN);
    fileSpecs.add(fileSpec2);
    processParameters = Parameters.processParameters(opts, fileSpecs, stringParams, true, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "//depot/test1"}));

    processParameters = Parameters.processParameters(opts, fileSpecs, stringParams, false, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "//depot/test1.txt"}));

    stringParams = new String[] {"-o"};
    processParameters = Parameters.processParameters(opts, fileSpecs, stringParams, false, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "-o", "//depot/test1.txt"}));

    processParameters = Parameters.processParameters(opts, fileSpecs, stringParams, true, null);
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "-o", "//depot/test1"}));
  }

  @Test
  public void processParameters_opts_fromFileSpec_toFileSpec_stringParams_server() throws P4JavaException {
    Options opts = createMockOptions();

    List<IFileSpec> toFileSpecs = null;
    String[] stringParams = null;
    String[] processParameters = Parameters.processParameters(opts, mock(IFileSpec.class), toFileSpecs, stringParams, mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s"}));

    toFileSpecs = new ArrayList<IFileSpec>();
    IFileSpec fileSpec1 = mock(IFileSpec.class);
    when(fileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec1.getAnnotatedPreferredPathString()).thenReturn("//depot/test1");
    toFileSpecs.add(fileSpec1);

    IFileSpec fileSpec2 = mock(IFileSpec.class);
    when(fileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.UNKNOWN);
    toFileSpecs.add(fileSpec2);
    processParameters = Parameters.processParameters(opts, mock(IFileSpec.class), toFileSpecs, stringParams, mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "//depot/test1"}));

    stringParams = new String[]{"//depot/test2", "//depot/test3"};
    processParameters = Parameters.processParameters(opts, mock(IFileSpec.class), toFileSpecs, stringParams, mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s", "//depot/test2", "//depot/test3", "//depot/test1"}));
  }

  private Options createMockOptions() {
    Options opts = mock(Options.class);
    ArrayList<String> optionsStrings = new ArrayList<String>();
    optionsStrings.add("-a");
    optionsStrings.add("-f");
    optionsStrings.add("-s");
    when(opts.isImmutable()).thenReturn(true);
    when(opts.getOptions()).thenReturn(optionsStrings);
    return opts;
  }

  @Test
  public void processParameters_opts_fromFileSpec_toFileSpecs_branchSpec_server() throws P4JavaException {
    Options opts = mock(Options.class);
    ArrayList<String> optionsStrings = new ArrayList<String>();
    optionsStrings.add("-a");
    optionsStrings.add("-f");
    when(opts.isImmutable()).thenReturn(true);
    when(opts.getOptions()).thenReturn(optionsStrings);

    IFileSpec fromFile = mock(IFileSpec.class);
    when(fromFile.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fromFile.getAnnotatedPreferredPathString()).thenReturn("//depot/test1");

    IFileSpec toFile = mock(IFileSpec.class);
    when(toFile.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(toFile.getAnnotatedPreferredPathString()).thenReturn("//depot/test2");

    String[] processParameters = Parameters.processParameters(opts, fromFile, toFile, "myBranch", mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-b", "myBranch", "//depot/test1", "//depot/test2"}));

    reset(toFile);
    when(toFile.getOpStatus()).thenReturn(FileSpecOpStatus.ERROR);
    processParameters = Parameters.processParameters(opts, fromFile, toFile, "myBranch", mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-b", "myBranch", "//depot/test1"}));

    reset(fromFile);
    when(fromFile.getOpStatus()).thenReturn(FileSpecOpStatus.ERROR);
    processParameters = Parameters.processParameters(opts, fromFile, toFile, "myBranch", mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-b", "myBranch"}));
  }

  @Test
  public void processParameters_opts_formFile_toFile_branchSpec_server() throws Exception {
    Options opts = createMockOptions();

    List<IFileSpec> fromFiles = null;
    List<IFileSpec> toFiles = null;

    String[] processParameters = Parameters.processParameters(opts, fromFiles, toFiles, "myBranch", mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-b", "myBranch", "-s"}));

    processParameters = Parameters.processParameters(opts, fromFiles, toFiles, null, mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s"}));

    processParameters = Parameters.processParameters(opts, fromFiles, toFiles, EMPTY, mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-s"}));

    reset(opts);
    ArrayList<String> optionsStrings = new ArrayList<String>();
    optionsStrings.add("-a");
    optionsStrings.add("-f");
    when(opts.isImmutable()).thenReturn(true);
    when(opts.getOptions()).thenReturn(optionsStrings);
    processParameters = Parameters.processParameters(opts, fromFiles, toFiles, "myBranch", mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-b", "myBranch"}));


    fromFiles = new ArrayList<IFileSpec>();
    IFileSpec fileSpec1 = mock(IFileSpec.class);
    when(fileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec1.getAnnotatedPreferredPathString()).thenReturn("//depot/test1");
    fromFiles.add(fileSpec1);

    IFileSpec fileSpec2 = mock(IFileSpec.class);
    when(fileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.ERROR);
    when(fileSpec2.getAnnotatedPreferredPathString()).thenReturn("//depot/test2");
    fromFiles.add(fileSpec2);

    toFiles = new ArrayList<IFileSpec>();
    IFileSpec fileSpec3 = mock(IFileSpec.class);
    when(fileSpec3.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec3.getAnnotatedPreferredPathString()).thenReturn("//depot/test3");
    toFiles.add(fileSpec3);

    IFileSpec fileSpec4 = mock(IFileSpec.class);
    when(fileSpec4.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec4.getAnnotatedPreferredPathString()).thenReturn("//depot/test4");
    toFiles.add(fileSpec4);

    processParameters = Parameters.processParameters(opts, fromFiles, toFiles, "myBranch", mock(IServer.class));
    assertThat(processParameters, is(new String[]{"-a", "-f", "-b", "myBranch", "//depot/test1", "//depot/test3", "//depot/test4"}));
  }

  @Test
  public void addFileSpecIfValidFileSpec() throws InvocationTargetException, IllegalAccessException {
    Parameters parameters = new Parameters();
    Method addFileSpecIfValidFileSpec = getPrivateMethod(Parameters.class, "addFileSpecIfValidFileSpec", List.class, List.class);
    try {
        addFileSpecIfValidFileSpec.invoke(parameters, null, null);
        fail("should have thown InvocationTargetException.class");
    } catch (Throwable e) {
        assertEquals(InvocationTargetException.class, e.getClass());
    }
    
    List<String> args = new ArrayList<String>();
    args.add("a");
    args.add("b");

    addFileSpecIfValidFileSpec.invoke(parameters, args, null);
    assertThat(args.size(), is(2));

    List<IFileSpec> fileSpecs = new ArrayList<IFileSpec>();
    IFileSpec fileSpec1 = mock(IFileSpec.class);
    when(fileSpec1.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec1.getAnnotatedPreferredPathString()).thenReturn("test1");
    fileSpecs.add(fileSpec1);

    IFileSpec fileSpec2 = mock(IFileSpec.class);
    when(fileSpec2.getOpStatus()).thenReturn(FileSpecOpStatus.ERROR);
    when(fileSpec2.getAnnotatedPreferredPathString()).thenReturn("test2");
    fileSpecs.add(fileSpec2);

    IFileSpec fileSpec3 = mock(IFileSpec.class);
    when(fileSpec3.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    when(fileSpec3.getAnnotatedPreferredPathString()).thenReturn("test3");
    fileSpecs.add(fileSpec3);


    addFileSpecIfValidFileSpec.invoke(parameters, args, fileSpecs);
    assertThat(args.size(), is(4));
    verify(fileSpec2).getOpStatus();
    verify(fileSpec2, times(0)).getAnnotatedPreferredPathString();
    verify(fileSpec1).getAnnotatedPreferredPathString();
  }



}