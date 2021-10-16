package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class ContextAwareRecyclerView extends RecyclerView {
  private RecyclerContextMenuInfo contextMenuInfo;

  public ContextAwareRecyclerView(Context context) {
    super(context);
  }

  public ContextAwareRecyclerView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public ContextAwareRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected RecyclerContextMenuInfo getContextMenuInfo() {
    return contextMenuInfo;
  }

  @Override
  public boolean showContextMenuForChild(View originalView) {
    final int position = getChildAdapterPosition(originalView);
    if (position >= 0) {
      contextMenuInfo = new RecyclerContextMenuInfo(position, getChildViewHolder(originalView).getItemId());
      return super.showContextMenuForChild(originalView);
    }
    return false;
  }

  public class RecyclerContextMenuInfo implements ContextMenu.ContextMenuInfo {

    public RecyclerContextMenuInfo(int position, long id) {
      this.position = position;
      this.id = id;
    }

    final public int position;
    final public long id;
  }
}
