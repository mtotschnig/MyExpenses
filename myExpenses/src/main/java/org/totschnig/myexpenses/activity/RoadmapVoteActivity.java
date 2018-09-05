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
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.retrofit.Issue;
import org.totschnig.myexpenses.retrofit.Vote;
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView;
import org.totschnig.myexpenses.ui.SimpleSeekBarDialog;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.viewmodel.RoadmapViewModel.ROADMAP_URL;

public class RoadmapVoteActivity extends ProtectedFragmentActivity implements
    SimpleDialog.OnDialogResultListener {
  private static final String DIALOG_TAG_ISSUE_VOTE = "issueVote";
  private static final String KEY_POSITION = "position";
  @BindView(R.id.my_recycler_view)
  ContextAwareRecyclerView recyclerView;
  private List<Issue> dataSet;
  private List<Issue> dataSetFiltered;
  private MenuItem voteMenuItem;
  Map<Integer, Integer> voteWeights;
  Vote lastVote;
  private RoadmapAdapter roadmapAdapter;
  private RoadmapViewModel roadmapViewModel;
  private boolean isPro;
  private String query;
  private boolean isLoading;

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
    getSupportActionBar().setTitle(R.string.roadmap_vote);

    isPro = ContribFeature.ROADMAP_VOTING.hasAccess();

    showIsLoading();

    roadmapViewModel = ViewModelProviders.of(this).get(RoadmapViewModel.class);
    voteWeights = roadmapViewModel.restoreWeights();
    roadmapViewModel.loadLastVote();
    roadmapViewModel.getData().observe(this, data -> {
      this.dataSet = data;
      if (dataSet == null) {
        publishResult("Failure loading data");
      } else {
        publishResult(String.format(Locale.getDefault(), "%d issues found", dataSet.size()));
        validateAndUpdateUi();
      }
    });
    roadmapViewModel.getVoteResult().observe(this,
        result -> {
          if (result != null) {
            lastVote = result;
          }
          publishResult(result != null ? "Your vote has been recorded" : "Failure while submitting your vote");
        });
    roadmapViewModel.getLastVote().observe(this,
        result -> {
          if (result != null && result.isPro() == isPro) {
            lastVote = result;
            if (voteWeights.size() == 0) {
              voteWeights.putAll(result.getVote());
              validateAndUpdateUi();
            }
          }
          roadmapViewModel.loadData(true);
        });
  }

  @Override
  protected void onPause() {
    super.onPause();
    roadmapViewModel.cacheWeights(voteWeights);
  }

  private void validateAndUpdateUi() {
    validateWeights();
    if (dataSet != null) {
      Collections.sort(dataSet, (issue1, issue2) -> {
        final Integer weight1 = voteWeights.get(issue1.getNumber());
        final Integer weight2 = voteWeights.get(issue2.getNumber());
        if (weight1 != null) {
          return weight2 == null ? -1 : weight2.compareTo(weight1);
        }
        if (weight2 != null) {
          return 1;
        }
        return Utils.compare(issue2.getNumber(), issue1.getNumber());
      });
      filterData();
    }
    updateVoteMenuItem();
  }

  private void validateWeights() {
    if (dataSet != null && voteWeights.size() > 0) {
      Iterator<Map.Entry<Integer, Integer>> iter = voteWeights.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<Integer, Integer> entry = iter.next();
        if (Stream.of(dataSet).noneMatch(issue -> issue.getNumber() == entry.getKey())) {
          iter.remove();
        }
      }
    }
  }

  private void showIsLoading() {
    isLoading = true;
    showSnackbar("Loading issues ...", Snackbar.LENGTH_INDEFINITE);
  }

  private void publishResult(String message) {
    isLoading = false;
    dismissSnackbar();
    showSnackbar(message, Snackbar.LENGTH_SHORT);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.search, menu);
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    MenuItem searchMenuItem = menu.findItem(R.id.SEARCH_COMMAND);
    SearchView searchView = (SearchView) searchMenuItem.getActionView();

    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        query = newText;
        filterData();
        return true;
      }
    });
    voteMenuItem = menu.add(Menu.NONE, R.id.ROADMAP_SUBMIT_VOTE, 0, "");
    voteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    updateVoteMenuItem();

    inflater.inflate(R.menu.vote, menu);
    inflater.inflate(R.menu.help_with_icon, menu);
    return true;
  }

  private void filterData() {
    if (TextUtils.isEmpty(query) || dataSet == null) {
      dataSetFiltered = dataSet;
    } else {
      dataSetFiltered = Stream.of(dataSet)
          .filter(issue -> issue.getTitle().toLowerCase().contains(query.toLowerCase()))
          .collect(Collectors.toList());
    }
    roadmapAdapter.notifyDataSetChanged();
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch (command) {
      case R.id.WEB_COMMAND: {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(ROADMAP_URL + "issues.html"));
        startActivity(i);
        return true;
      }
      case R.id.SYNC_COMMAND: {
        showIsLoading();
        roadmapViewModel.loadData(false);
        return true;
      }
      case R.id.ROADMAP_SUBMIT_VOTE: {
        if (lastVote != null && lastVote.getVote().equals(voteWeights)) {
          showSnackbar("Modify your vote, before submitting it again.", Snackbar.LENGTH_LONG);
        } else {
          Bundle b = new Bundle();
          b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.roadmap_vote);
          b.putString(ConfirmationDialogFragment.KEY_MESSAGE, lastVote == null ? "Your vote will be submitted" : "Your vote will be updated");
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.ROADMAP_SUBMIT_VOTE_DO);
          ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(), "ROADMAP_VOTE");
        }
        return true;
      }
      case R.id.ROADMAP_SUBMIT_VOTE_DO: {
        showSnackbar("Submitting vote ...", Snackbar.LENGTH_INDEFINITE);
        isLoading = true;
        roadmapViewModel.submitVote(lastVote != null ? lastVote.getKey() : null, new HashMap<>(voteWeights));
        return true;
      }
    }
    return super.dispatchCommand(command, tag);
  }

  private void updateVoteMenuItem() {
    if (voteMenuItem != null) {
      int currentTotalWeight = getCurrentTotalWeight();
      final boolean enabled = currentTotalWeight == getTotalAvailableWeight();
      voteMenuItem.setTitle(enabled ? "Submit" : String.format(Locale.ROOT, "%d/%d", currentTotalWeight, getTotalAvailableWeight()));
      voteMenuItem.setEnabled(enabled);
    }
  }

  @Override
  public void finish() {
    if (isLoading) {
      roadmapViewModel.cancel();
    }
    super.finish();
  }

  private int getCurrentTotalWeight() {
    int currentTotalWeight = 0;
    for (Map.Entry<Integer, Integer> entry : voteWeights.entrySet()) {
      currentTotalWeight += +entry.getValue();
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
        int available = getTotalAvailableWeight() - getCurrentTotalWeight();
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
          showSnackbar("You spent all your points on other issues.", Snackbar.LENGTH_SHORT);
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
        int value = extras.getInt(SimpleSeekBarDialog.SEEKBAR_VALUE);
        int issueId = extras.getInt(KEY_ROWID);
        if (value > 0) {
          voteWeights.put(issueId, value);
        } else {
          voteWeights.remove(issueId);
        }
        validateAndUpdateUi();
        updateVoteMenuItem();
        return true;
      }
    }
    return false;
  }

  private int getTotalAvailableWeight() {
    return isPro ? 50 : 10;
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

  @Override
  protected int getSnackbarContainerId() {
    return R.id.container;
  }
}
