package org.totschnig.myexpenses.test.util;


import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.util.AppDirHelper;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AppDirHelperTest {
  @Test
  public void shouldValidateDefaultAppDir() {
    assertTrue(AppDirHelper.checkAppDir(InstrumentationRegistry.getTargetContext()).isSuccess());
  }
}
