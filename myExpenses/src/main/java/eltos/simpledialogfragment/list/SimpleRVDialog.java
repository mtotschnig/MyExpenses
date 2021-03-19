/*
 *  Copyright 2018 Philipp Niedermayer (github.com/eltos)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eltos.simpledialogfragment.list;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import org.totschnig.myexpenses.R;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eltos.simpledialogfragment.SimpleDialog;

/**
 * A dialog that displays a list of items in a {@link RecyclerView}.
 * MULTI_CHOICE and SINGLE_CHOICE modes are supported.
 * Specify your custom adapter
 *
 * Result:
 *      SELECTED_SINGLE_POSITION    int                 selected item position
 *      SELECTED_SINGLE_ID          long                selected item id
 *
 * Created by Michael Totschnig on 17.05.2019.
 */

public abstract class SimpleRVDialog<This extends SimpleRVDialog<This>>
        extends SimpleDialog<This> {

    protected static final String TAG = "SimpleRVDialog";

    private static final String GRID_N = TAG + "gridN";
    private static final String GRID_W = TAG + "gridW";

    public SimpleRVDialog() {
        pos(null);
    }

    /**
     * Overwrite this method to provide a custom adapter
     *
     * @return the ListAdapter to use
     */
    protected abstract ClickableAdapter onCreateAdapter();

    private RecyclerView recyclerView;

    /**
     * Specifies the number of columns of this grid view (only if in grid mode)
     *
     * @param numColumns the number of columns
     */
    public This gridNumColumn(int numColumns){
        return setArg(GRID_N, numColumns);
    }

    /**
     * Specifies the column with of this grid view (only if in grid mode)
     *
     * @param columnWidthDimenResId the with as an android dimension resource identifier
     */
    public This gridColumnWidth(@DimenRes int columnWidthDimenResId){
        return setArg(GRID_W, columnWidthDimenResId);
    }

    public RecyclerView.Adapter<?> getAdapter() {
        return adapter;
    }

    private ClickableAdapter adapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog dialog = ((AlertDialog) super.onCreateDialog(savedInstanceState));
        recyclerView = (RecyclerView) dialog.getLayoutInflater().inflate(R.layout.simpledialogfragment_rv, null, false);
        adapter = onCreateAdapter();
        recyclerView.setAdapter(adapter);
        dialog.setView(recyclerView);
        dialog.setOnShowListener(dialog1 -> {
            int containerWidthPixels = ((Dialog) dialog1).getWindow().getDecorView().getWidth() -
                2 * getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
            int spanCount;
            if (getArguments().containsKey(GRID_W)){
                spanCount = containerWidthPixels / getResources().getDimensionPixelSize(getArguments().getInt(GRID_W));
            } else {
                spanCount = getArguments().getInt(GRID_N);
            }
            RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), Math.max(spanCount, 5));
            recyclerView.setLayoutManager(layoutManager);
        });
        return dialog;
    }

    protected void notifyDataSetChanged(){
        adapter.notifyDataSetChanged();
    }

    public abstract class ClickableAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements View.OnClickListener  {
        @Override
        public void onBindViewHolder(@NonNull VH vh, int i) {
            vh.itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            getDialog().dismiss();
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(v);
            callResultListener(DialogInterface.BUTTON_POSITIVE, onResult(holder.getAdapterPosition()));
        }
    }

    protected abstract Bundle onResult(int selectedPosition);
}
