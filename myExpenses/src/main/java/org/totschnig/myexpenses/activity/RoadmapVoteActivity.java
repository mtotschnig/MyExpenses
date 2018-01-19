package org.totschnig.myexpenses.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
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
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RoadmapVoteActivity extends ProtectedFragmentActivity {
  @BindView(R.id.my_recycler_view)
  ContextAwareRecyclerView recyclerView;
  private List<Issue> dataSet;

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
        SimpleSeekBarDialog.build().title(dataSet.get(info.position).getTitle()).max(10).show(this);
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
