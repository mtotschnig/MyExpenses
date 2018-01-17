package org.totschnig.myexpenses.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.widget.Toast;

import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

public class RoadmapVoteActivity extends ProtectedFragmentActivity {
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    RoadmapViewModel model =
        ViewModelProviders.of(this).get(RoadmapViewModel.class);
    model.getData().observe(this, data -> {
      Toast.makeText(this, String.format("Got %d issues", data.size()), Toast.LENGTH_LONG).show();
    });
  }
}
