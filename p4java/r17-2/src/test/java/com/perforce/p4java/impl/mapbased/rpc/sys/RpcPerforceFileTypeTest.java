package com.perforce.p4java.impl.mapbased.rpc.sys;

import java.io.File;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.server.PerforceCharsets;


public class RpcPerforceFileTypeTest extends AbstractP4JavaUnitTest {
  @Test
  public void testInferFileTypeFromContents_hasUtf8Bom_but_its_a_text() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/has_utf8_bom_but_its_text.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_TEXT, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_hasUtf8Bom_but_its_a_binary() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/has_utf8_bom_but_its_a_binary.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_BINARY, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_valid_utf8_without_bom_detector_as_Text_match_p4_command() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/utf_8-jp_without_bom.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_TEXT, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_valid_utf8_with_bom_and_win_line_ending() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/utf8_with_bom_win_line_ending.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_UTF8, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_valid_utf8_with_bom_and_unix_line_ending() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/utf8_with_bom_unix_lineending.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_UTF8, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_valid_utf_16LE_with_bom_and_unix_line_ending() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/ja_utf16LE_with_bom_and_unix_line_ending.xml");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_UTF16, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_valid_utf_16LE_without_bom() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/utf_16LE_without_bom.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_BINARY, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_valid_utf_16LE_with_bom_and_win_line_ending() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/utf_16LE_with_bom_WIN_line_end.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_UTF16, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_file_has_utf_16LE_bom_but_its_actually_a_audio_file() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/file_has_utf_16LE_bom_but_its_actually_a_audio_file.mp1");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_BINARY, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_valid_utf_16BE_without_bom_and_unix_line_ending() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/utf-16be_without_bom.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_BINARY, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_valid_utf_16BE_with_bom_and_unix_line_ending() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/utf_16BE_with_bom_and_win_line_ending.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_UTF16, fileType);
  }

  //@Test
  public void testInferFileTypeFromContents_GIF() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/sample_gif.gif");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, Charset.defaultCharset());
    Assert.assertEquals(RpcPerforceFileType.FST_BINARY, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_jp_shift_jis_expect_trade_as_FST_TEXT_as_can_convert_to_wellFormed_utf8() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/shift_jis.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, null);
    Assert.assertEquals(RpcPerforceFileType.FST_TEXT, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_jp_shift_jis_server_unicode_enabled() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/shift_jis.txt");
    Charset matchCharset = PerforceCharsets.getP4Charset("shiftjis");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, true, matchCharset);
    Assert.assertEquals(RpcPerforceFileType.FST_UNICODE, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_enc_jp_expect_trade_as_FST_TEXT_as_can_convert_to_wellFormed_utf8() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/euc-jp.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, null);
    Assert.assertEquals(RpcPerforceFileType.FST_TEXT, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_enc_jp_expect_detect_as_FST_TEXT_as_unicode_server_not_enabled_and_can_convertToUtf8() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/euc-jp.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, false, null);
    Assert.assertEquals(RpcPerforceFileType.FST_TEXT, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_enc_jp_expect_detect_as_FST_TEXT_as_clientCharset_isNull_and_unicode_server_enabled() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/euc-jp.txt");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, true, null);
    Assert.assertEquals(RpcPerforceFileType.FST_TEXT, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_enc_jp_expect_detect_as_FST_TEXT_as_clientCharset_isNotMatch_and_unicode_server_enabled() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/euc-jp.txt");
    Charset wrongCharset = PerforceCharsets.getP4Charset("shiftjis");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, true, wrongCharset);
    Assert.assertEquals(RpcPerforceFileType.FST_TEXT, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_gb18030_expect_detect_as_Binary_as_clientCharset_isNotMatch_real_file_charset_but_unicode_server_enabled() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/gb18030.txt");
    Charset wrongCharset = PerforceCharsets.getP4Charset("shiftjis");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, true, wrongCharset);
    Assert.assertEquals(RpcPerforceFileType.FST_TEXT, fileType);
  }

  @Test
  public void testInferFileTypeFromContents_gb18030_expect_detect_as_Unicode_as_clientCharset_isMatch_real_file_charset_but_unicode_server_enabled() {
    File file = loadFileFromClassPath("com/perforce/p4java/impl/mapbased/rpc/sys/gb18030.txt");
    Charset matchCharset = PerforceCharsets.getP4Charset("cp936");
    RpcPerforceFileType fileType = RpcPerforceFileType.inferFileType(file, -1, true, matchCharset);
    Assert.assertEquals(RpcPerforceFileType.FST_UNICODE, fileType);
  }
}