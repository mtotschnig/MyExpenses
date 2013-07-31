package org.totschnig.myexpenses.activity;

import org.totschnig.myexpenses.dialog.HelpDialogFragment;

import android.os.Bundle;

public class Help extends ProtectedFragmentActivityNoSherlock {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String activityName = getCallingActivity().getShortClassName();
    //trim leading .
    activityName = activityName.substring(activityName.lastIndexOf(".")+1);
    String variant = getIntent().getStringExtra("variant");
    HelpDialogFragment.newInstance(activityName,variant).show(getSupportFragmentManager(),"HELP");
  }
}
