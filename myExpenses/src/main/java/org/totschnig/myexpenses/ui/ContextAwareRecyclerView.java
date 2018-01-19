package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;

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
    final int longPressPosition = getChildAdapterPosition(originalView);
    if (longPressPosition >= 0) {
      final long longPressId = getAdapter().getItemId(longPressPosition);
      contextMenuInfo = new RecyclerContextMenuInfo(longPressPosition, longPressId);
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
