package org.totschnig.myexpenses.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.retrofit.Issue;
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView;
import org.totschnig.myexpenses.ui.SimpleSeekBarDialog;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class RoadmapVoteActivity extends ProtectedFragmentActivity implements
    SimpleDialog.OnDialogResultListener {
  private static final String DIALOG_TAG_ISSUE_VOTE = "issueVote";
  @BindView(R.id.my_recycler_view)
  ContextAwareRecyclerView recyclerView;
  private List<Issue> dataSet;
  private MenuItem voteMenuItem;
  private int totalAvailableWeight = 10;
  SparseIntArray voteWeights = new SparseIntArray();

  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.roadmap);
    ButterKnife.bind(this);
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));
    RoadmapAdapter roadmapAdapter = new RoadmapAdapter();
    recyclerView.setAdapter(roadmapAdapter);
    registerForContextMenu(recyclerView);
    setupToolbar(true);
    getSupportActionBar().setTitle(R.string.roadmap);

    RoadmapViewModel model =
        ViewModelProviders.of(this).get(RoadmapViewModel.class);
    model.getData().observe(this, data -> {
      this.dataSet = data;
      roadmapAdapter.notifyDataSetChanged();
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    voteMenuItem = menu.add(Menu.NONE, R.id.ROADMAP_SUBMIT_VOTE, 0,"");
    voteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    updateVoteMenuItem();
    return true;
  }

  private void updateVoteMenuItem() {
    int currentTotalWeight = getCurrentTotalWeight();
    voteMenuItem.setTitle(String.format(Locale.ROOT, "%d/%d", currentTotalWeight, totalAvailableWeight));
    voteMenuItem.setEnabled(currentTotalWeight == totalAvailableWeight);
  }

  private int getCurrentTotalWeight() {
    int currentTotalWeight = 0;
    for (int i = 0; i < voteWeights.size(); i++) {
      currentTotalWeight += voteWeights.valueAt(i);
    }
    return currentTotalWeight;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.roadmap_context, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    ContextAwareRecyclerView.RecyclerContextMenuInfo info = (ContextAwareRecyclerView.RecyclerContextMenuInfo) item.getMenuInfo();
    switch (item.getItemId()) {
      case R.id.ROADMAP_DETAILS_COMMAND: {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://github.com/mtotschnig/MyExpenses/issues/" + info.id));
        startActivity(i);
        return true;
      }
      case R.id.ROADMAP_ISSUE_VOTE_COMMAND: {
        Bundle extra = new Bundle(1);
        extra.putInt(KEY_ROWID, (int) info.id);
        int available = totalAvailableWeight - getCurrentTotalWeight();
        if (available > 0) {
          SimpleSeekBarDialog.build()
              .title(dataSet.get(info.position).getTitle())
              .max(available)
              .extra(extra)
              .show(this, DIALOG_TAG_ISSUE_VOTE);
        } else {
          Snackbar snackbar = Snackbar.make(
              findViewById(R.id.container), "You spent all your points on other issues.", Snackbar.LENGTH_SHORT);
          UiUtils.configureSnackbarForDarkTheme(snackbar);
          snackbar.show();
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    switch (dialogTag) {
      case DIALOG_TAG_ISSUE_VOTE: {
        voteWeights.put(extras.getInt(KEY_ROWID), extras.getInt(SimpleSeekBarDialog.SEEKBAR_VALUE));
        updateVoteMenuItem();
        supportInvalidateOptionsMenu();
        return true;
      }
    }
    return false;
  }

  private class RoadmapAdapter extends RecyclerView.Adapter<RoadmapAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(RoadmapVoteActivity.this);
      View row = inflater.inflate(R.layout.roadmap_list_item, parent, false);
      return new ViewHolder(row);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      holder.textView.setText(dataSet.get(position).getTitle());
    }

    @Override
    public long getItemId(int position) {
      return dataSet.get(position).getNumber();
    }

    @Override
    public int getItemCount() {
      return dataSet == null ? 0 : dataSet.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
      private TextView textView;
      private ViewHolder(View itemView) {
        super(itemView);
        textView = itemView.findViewById(R.id.text);
      }
    }
  }
}
