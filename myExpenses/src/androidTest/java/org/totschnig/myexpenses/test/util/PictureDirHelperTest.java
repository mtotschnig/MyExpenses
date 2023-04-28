package org.totschnig.myexpenses.test.util;

import android.content.Context;
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
    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File file = PictureDirHelper.getFileForUri(targetContext, uri);
    assertEquals(AppDirHelper.getContentUriForFile(targetContext, file), uri);
  }

  @Test
  public void getFileForProtectedUriIsRetrievedAndMapsToOriginalUri() {
    //given
    Uri uri = PictureDirHelper.getOutputMediaUri(false, true);
    //then
    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File file = PictureDirHelper.getFileForUri(targetContext, uri);
    assertEquals(AppDirHelper.getContentUriForFile(targetContext, file), uri);
  }
}
