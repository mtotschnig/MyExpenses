package org.totschnig.myexpenses.test.util;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.util.AppDirHelper;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AppDirHelperTest {
  @Test
  public void shouldValidateDefaultAppDir() {
    assertTrue(AppDirHelper.checkAppDir(InstrumentationRegistry.getTargetContext()).isSuccess());
  }
}
