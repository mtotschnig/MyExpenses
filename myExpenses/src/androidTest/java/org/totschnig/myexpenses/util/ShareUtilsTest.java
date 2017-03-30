package org.totschnig.myexpenses.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.annimon.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ShareUtilsTest  {

  private Context targetContext;

  @Before
  public void setUp() throws Exception {
    targetContext = InstrumentationRegistry.getTargetContext();
  }

  @Test
  public void shouldConvertSingleFileUri() {
    String mimeType = "text/plain";
    Uri testFileUri = AppDirHelper.getAppDir().createFile(mimeType,"testFile").getUri();
    assertFileScheme(testFileUri);
    List<Uri> fileUris = Collections.singletonList(testFileUri);
    Intent intent = ShareUtils.buildIntent(fileUris, mimeType, null);
    Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    assertContentScheme(sharedUri);
  }

  @Test
  public void shouldConvertMultipleFileUris() {
    String mimeType = "text/plain";
    Uri testFile1Uri = AppDirHelper.getAppDir().createFile(mimeType,"testFile1").getUri();
    Uri testFile2Uri = AppDirHelper.getAppDir().createFile(mimeType,"testFile1").getUri();
    List<Uri> fileUris = Arrays.asList(testFile1Uri, testFile2Uri);
    Stream.of(fileUris).forEach(this::assertFileScheme);
    Intent intent = ShareUtils.buildIntent(fileUris, mimeType, null);
    List<Uri> sharedUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    Stream.of(sharedUris).forEach(this::assertContentScheme);
  }

  private void assertFileScheme(Uri uri) {
    assertScheme(uri, "file");
  }

  private void assertContentScheme(Uri uri) {
    assertScheme(uri, "content");
  }

  private void assertScheme(Uri uri, String scheme) {
    assertEquals(scheme, uri.getScheme());
  }
}
