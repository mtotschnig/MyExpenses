package org.totschnig.myexpenses.test.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.annimon.stream.Stream;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.ShareUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ShareUtilsTest  {

  @Before
  public void setup() {
    Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
  }

  @Test
  public void shouldConvertSingleFileUri() {
    String mimeType = "text/plain";
    Uri testFileUri = AppDirHelper.getAppDir(InstrumentationRegistry.getTargetContext()).createFile(mimeType,"testFile").getUri();
    assertFileScheme(testFileUri);
    List<Uri> fileUris = Collections.singletonList(testFileUri);
    Intent intent = ShareUtils.buildIntent(fileUris, mimeType, null);
    Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    assertContentScheme(sharedUri);
  }

  @Test
  public void shouldConvertMultipleFileUris() {
    String mimeType = "text/plain";
    Context context = InstrumentationRegistry.getTargetContext();
    Uri testFile1Uri = AppDirHelper.getAppDir(context).createFile(mimeType,"testFile1").getUri();
    Uri testFile2Uri = AppDirHelper.getAppDir(context).createFile(mimeType,"testFile1").getUri();
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
