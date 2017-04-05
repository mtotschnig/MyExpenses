package org.totschnig.myexpenses.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import org.totschnig.myexpenses.task.TaskExecutionFragment;

public class SplashActivity extends ProtectedFragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    startTaskExecution(TaskExecutionFragment.TASK_INIT, null, null, 0);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //No menu needed here. Otherwise, some unusual input might call Help
    return false;
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Intent intent = new Intent(this, MyExpenses.class);
    startActivity(intent);
    finish();
  }
}