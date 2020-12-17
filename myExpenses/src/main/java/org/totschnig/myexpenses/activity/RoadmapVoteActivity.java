package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.retrofit.Issue;
import org.totschnig.myexpenses.retrofit.Vote;
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView;
import org.totschnig.myexpenses.ui.SimpleSeekBarDialog;
import org.totschnig.myexpenses.util.MenuUtilsKt;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.viewmodel.RoadmapViewModel.EXPECTED_MINIMAL_VERSION;
import static org.totschnig.myexpenses.viewmodel.RoadmapViewModel.ROADMAP_URL;

public class RoadmapVoteActivity extends ProtectedFragmentActivity implements
    SimpleDialog.OnDialogResultListener {
  private static final String DIALOG_TAG_ISSUE_VOTE = "issueVote";
  private static final String DIALOG_TAG_SUBMIT_VOTE = "ROADMAP_VOTE";
  private static final String KEY_POSITION = "position";
  private static final String KEY_EMAIL = "EMAIL";
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

    roadmapViewModel = new ViewModelProvider(this).get(RoadmapViewModel.class);
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
            if (result.isSuccess()) {
              lastVote = result.getExtra();
            }
            publishResult(result.print(this));
          }
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
          roadmapViewModel.loadData(EXPECTED_MINIMAL_VERSION <= getVersionFromPref());
        });
  }

  @Override
  protected void onPause() {
    super.onPause();
    roadmapViewModel.cacheWeights(voteWeights);
  }


  private int getVersionFromPref() {
    return prefHandler.getInt(PrefKey.ROADMAP_VERSION, 0 );
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
    showSnackbar(message);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.search, menu);
    MenuUtilsKt.configureSearch(this, menu, this::onQueryTextChange);
    voteMenuItem = menu.add(Menu.NONE, R.id.ROADMAP_SUBMIT_VOTE, 0, "");
    voteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    updateVoteMenuItem();

    inflater.inflate(R.menu.vote, menu);
    inflater.inflate(R.menu.help_with_icon, menu);
    return true;
  }

  private Boolean onQueryTextChange(String newText) {
    query = newText;
    filterData();
    return true;
  }

  private void filterData() {
    if (TextUtils.isEmpty(query) || dataSet == null) {
      dataSetFiltered = dataSet;
    } else {
      dataSetFiltered = Stream.of(dataSet)
          .filter(issue -> issue.getTitle().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
          .collect(Collectors.toList());
    }
    roadmapAdapter.notifyDataSetChanged();
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    switch (command) {
      case R.id.ROADMAP_RESULT_COMMAND: {
        startActionView(ROADMAP_URL + "issues.html");
        return true;
      }
      case R.id.SYNC_COMMAND: {
        showIsLoading();
        roadmapViewModel.loadData(false);
        return true;
      }
      case R.id.ROADMAP_SUBMIT_VOTE: {
        if (lastVote != null && lastVote.getVote().equals(voteWeights) && lastVote.getVersion() == getVersionFromPref()) {
          showSnackbar("Modify your vote, before submitting it again.");
        } else {
          final boolean emailIsKnown = getEmail() != null;
          int msg = emailIsKnown ? R.string.roadmap_update_confirmation : R.string.roadmap_email_rationale;
          final SimpleFormDialog simpleFormDialog = SimpleFormDialog.build().msg(msg);
          if (!emailIsKnown) {
            simpleFormDialog.fields(Input.email(KEY_EMAIL).required());
          }
          simpleFormDialog.show(this, DIALOG_TAG_SUBMIT_VOTE);
        }
        return true;
      }
    }
    return false;
  }

  @Nullable
  protected String getEmail() {
    return lastVote != null? lastVote.getEmail() : null;
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
        startActionView("https://github.com/mtotschnig/MyExpenses/issues/" + info.id);
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
    if (which == BUTTON_POSITIVE) {
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
        case DIALOG_TAG_SUBMIT_VOTE: {
          showSnackbar("Submitting vote ...", Snackbar.LENGTH_INDEFINITE);
          isLoading = true;
          boolean isPro = ContribFeature.ROADMAP_VOTING.hasAccess();
          String email =  getEmail();
          if (email == null) {
            email = extras.getString(KEY_EMAIL);
          }
          String key = lastVote != null ? lastVote.getKey() : licenceHandler.buildRoadmapVoteKey();
          Vote vote = new Vote(key, new HashMap<>(voteWeights), isPro, email, getVersionFromPref());
          roadmapViewModel.submitVote(vote);
          return true;
        }
      }
    }
    return false;
  }

  private int getTotalAvailableWeight() {
    return isPro ? 50 : 10;
  }

  private class RoadmapAdapter extends RecyclerView.Adapter<RoadmapAdapter.ViewHolder> {

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(RoadmapVoteActivity.this);
      View row = inflater.inflate(R.layout.roadmap_list_item, parent, false);
      row.setOnClickListener(RoadmapVoteActivity.this::openContextMenu);
      return new ViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
