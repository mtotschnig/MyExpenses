/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.helper.ItemTouchHelperAdapter;
import org.totschnig.myexpenses.adapter.helper.ItemTouchHelperViewHolder;
import org.totschnig.myexpenses.adapter.helper.OnStartDragListener;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Simple RecyclerView.Adapter that implements {@link ItemTouchHelperAdapter} to respond to move and
 * dismiss events from a {@link ItemTouchHelper}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder>
    implements ItemTouchHelperAdapter {

  private final ArrayList<AbstractMap.SimpleEntry<Long, String>> mItems = new ArrayList<>();

  private final OnStartDragListener mDragStartListener;

  public RecyclerListAdapter(OnStartDragListener dragStartListener, List<AbstractMap.SimpleEntry<Long, String>> items) {
    mDragStartListener = dragStartListener;
    mItems.addAll(items);
  }

  @NonNull
  @Override
  public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_with_drag_handle, parent, false);
    return new ItemViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {
    holder.textView.setText(String.format(Locale.getDefault(), "%d. %s", position + 1, mItems.get(position).getValue()));

    // Start a drag whenever the handle view it touched
    holder.handleView.setOnTouchListener((v, event) -> {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        mDragStartListener.onStartDrag(holder);
      }
      return false;
    });
  }

  @Override
  public void onItemDismiss(int position) {
    mItems.remove(position);
    notifyItemRemoved(position);
  }

  @Override
  public void onDrop() {
    notifyDataSetChanged();
  }

  @Override
  public boolean onItemMove(int fromPosition, int toPosition) {
    Collections.swap(mItems, fromPosition, toPosition);
    notifyItemMoved(fromPosition, toPosition);
    return true;
  }

  @Override
  public int getItemCount() {
    return mItems.size();
  }


  public ArrayList<AbstractMap.SimpleEntry<Long, String>> getItems() {
    return mItems;
  }

  /**
   * Simple example of a view holder that implements {@link ItemTouchHelperViewHolder} and has a
   * "handle" view that initiates a drag event when touched.
   */
  public static class ItemViewHolder extends RecyclerView.ViewHolder implements
      ItemTouchHelperViewHolder {

    final TextView textView;
    final ImageView handleView;

    ItemViewHolder(View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.text);
      handleView = itemView.findViewById(R.id.handle);
    }

    @Override
    public void onItemSelected() {
      itemView.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onItemClear() {
      itemView.setBackgroundColor(0);
    }
  }
}
