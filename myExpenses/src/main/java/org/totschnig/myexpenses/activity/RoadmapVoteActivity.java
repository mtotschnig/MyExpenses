package org.totschnig.myexpenses.activity;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.retrofit.Issue;
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView;
import org.totschnig.myexpenses.ui.SimpleSeekBarDialog;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class RoadmapVoteActivity extends ProtectedFragmentActivity implements
    SimpleDialog.OnDialogResultListener {
  private static final String DIALOG_TAG_ISSUE_VOTE = "issueVote";
  private static final String KEY_POSITION = "position";
  private static final String KEY_VOTE_WEIGHTS = "voteWeights";
  @BindView(R.id.my_recycler_view)
  ContextAwareRecyclerView recyclerView;
  private List<Issue> dataSet;
  private List<Issue> dataSetFiltered;
  private MenuItem voteMenuItem;
  private int totalAvailableWeight = 10;
  HashMap<Integer, Integer> voteWeights = new HashMap<>();
  private RoadmapAdapter roadmapAdapter;
  private RoadmapViewModel roadmapViewModel;
  private Snackbar snackbar;

  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.roadmap);
    ButterKnife.bind(this);
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));
    roadmapAdapter = new RoadmapAdapter();
    recyclerView.setAdapter(roadmapAdapter);
    registerForContextMenu(recyclerView);
    setupToolbar(true);
    getSupportActionBar().setTitle(R.string.roadmap);

    if (savedInstanceState != null) {
      voteWeights = (HashMap<Integer, Integer>) savedInstanceState.getSerializable(KEY_VOTE_WEIGHTS);
    }
    showIsLoading();

    roadmapViewModel = ViewModelProviders.of(this).get(RoadmapViewModel.class);
    roadmapViewModel.getData().observe(this, data -> {
      this.dataSet = data;
      dataSetFiltered = dataSet;
      publishResult(dataSet == null ? "Failure loading data" : String.format(Locale.getDefault(), "%d issues found", dataSet.size()));
      roadmapAdapter.notifyDataSetChanged();
    });
    roadmapViewModel.getVoteResult().observe(this,
        result -> publishResult(result ? "Your vote has been successfully recorded" : "Failure while submitting your vote"));
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_VOTE_WEIGHTS, voteWeights);
  }

  private void showIsLoading() {
    showSnackBar("Loading issues ...", Snackbar.LENGTH_INDEFINITE);
  }

  private void publishResult(String message) {
    if (snackbar != null) {
      snackbar.dismiss();
    }
    showSnackBar(message, Snackbar.LENGTH_SHORT);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.search, menu);
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    MenuItem searchMenuItem = menu.findItem(R.id.search);
    SearchView searchView = (SearchView) searchMenuItem.getActionView();

    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
          dataSetFiltered = dataSet;
        } else {
          dataSetFiltered = Stream.of(dataSet)
              .filter(issue -> issue.getTitle().toLowerCase().contains(newText.toLowerCase()))
              .collect(Collectors.toList());
        }
        roadmapAdapter.notifyDataSetChanged();
        return true;
      }
    });
    voteMenuItem = menu.add(Menu.NONE, R.id.ROADMAP_SUBMIT_VOTE, 0,"");
    voteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    updateVoteMenuItem();

    inflater.inflate(R.menu.refresh, menu);
    inflater.inflate(R.menu.help_with_icon, menu);
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch (command) {
      case R.id.SYNC_COMMAND: {
        showIsLoading();
        roadmapViewModel.loadData(false);
        return true;
      }
      case R.id.ROADMAP_SUBMIT_VOTE: {
        roadmapViewModel.submitVote("bogus", voteWeights);
        return true;
      }
    }
    return super.dispatchCommand(command, tag);
  }

  private void updateVoteMenuItem() {
    int currentTotalWeight = getCurrentTotalWeight();
    voteMenuItem.setTitle(String.format(Locale.ROOT, "%d/%d", currentTotalWeight, totalAvailableWeight));
    voteMenuItem.setEnabled(currentTotalWeight == totalAvailableWeight);
  }

  private int getCurrentTotalWeight() {
    int currentTotalWeight = 0;
    for (Map.Entry<Integer, Integer> entry : voteWeights.entrySet())
    {
      currentTotalWeight += + entry.getValue();
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
        extra.putInt(KEY_POSITION, info.position);
        Integer value = voteWeights.get((int) info.id);
        int available = totalAvailableWeight - getCurrentTotalWeight();
        if (value != null) {
          available += value;
        }
        if (available > 0) {
          SimpleSeekBarDialog dialog = SimpleSeekBarDialog.build()
              .title(dataSetFiltered.get(info.position).getTitle())
              .max(available)
              .extra(extra);
          if (value != null) {
            dialog.value(value);
          }
          dialog.show(this, DIALOG_TAG_ISSUE_VOTE);
        } else {
          showSnackBar("You spent all your points on other issues.", Snackbar.LENGTH_SHORT);
        }
        return true;
      }
    }
    return false;
  }

  private void showSnackBar(CharSequence message, int duration) {
    snackbar = Snackbar.make(
        findViewById(R.id.container), message, duration);
    UiUtils.configureSnackbarForDarkTheme(snackbar);
    snackbar.show();
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    switch (dialogTag) {
      case DIALOG_TAG_ISSUE_VOTE: {
        voteWeights.put(extras.getInt(KEY_ROWID), extras.getInt(SimpleSeekBarDialog.SEEKBAR_VALUE));
        roadmapAdapter.notifyItemChanged(extras.getInt(KEY_POSITION));
        updateVoteMenuItem();
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
      Issue issue = dataSetFiltered.get(position);
      holder.textView.setText(issue.getTitle());
      Integer weight = voteWeights.get(issue.getNumber());
      holder.weightView.setText(weight == null ? "0" : String.valueOf(weight));
    }

    @Override
    public long getItemId(int position) {
      return dataSetFiltered.get(position).getNumber();
    }

    @Override
    public int getItemCount() {
      return dataSetFiltered == null ? 0 : dataSetFiltered.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
      private TextView textView;
      private TextView weightView;
      private ViewHolder(View itemView) {
        super(itemView);
        textView = itemView.findViewById(R.id.text);
        weightView = itemView.findViewById(R.id.weight);
      }
    }
  }
}
