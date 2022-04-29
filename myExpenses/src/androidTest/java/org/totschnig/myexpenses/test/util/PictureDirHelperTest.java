package org.totschnig.myexpenses.test.util;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.PictureDirHelper;

import java.io.File;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PictureDirHelperTest {

  @Test
  public void unProtectedPictureIsBuildAndHasUriScheme() {
    assertEquals("content", PictureDirHelper.getOutputMediaUri(false, false).getScheme());
  }

  @Test
  public void protectedPictureIsBuildAndHasUriScheme() {
    assertEquals("content", PictureDirHelper.getOutputMediaUri(false, true).getScheme());
  }

  @Test
  public void getFileForUnProtectedUriIsRetrievedAndMapsToOriginalUri() {
    //given
    Uri uri = PictureDirHelper.getOutputMediaUri(false, false);
    //then
    File file = PictureDirHelper.getFileForUri(uri);
    assertEquals(AppDirHelper.getContentUriForFile(InstrumentationRegistry.getInstrumentation().getTargetContext(), file), uri);
  }

  @Test
  public void getFileForProtectedUriIsRetrievedAndMapsToOriginalUri() {
    //given
    Uri uri = PictureDirHelper.getOutputMediaUri(false, true);
    //then
    File file = PictureDirHelper.getFileForUri(uri);
    assertEquals(AppDirHelper.getContentUriForFile(InstrumentationRegistry.getInstrumentation().getTargetContext(), file), uri);
  }
}
