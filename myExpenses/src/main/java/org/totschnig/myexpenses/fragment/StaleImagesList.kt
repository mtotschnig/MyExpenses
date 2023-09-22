/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.fragment

import android.annotation.SuppressLint
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageView
import android.widget.SimpleCursorAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.ViewIntentProvider
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.attachmentInfoMap
import org.totschnig.myexpenses.util.setAttachmentInfo
import org.totschnig.myexpenses.viewmodel.StaleImagesViewModel
import org.totschnig.myexpenses.viewmodel.data.AttachmentInfo
import javax.inject.Inject

class StaleImagesList : ContextualActionBarFragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private lateinit var adapter: SimpleCursorAdapter
    private var imagesCursor: Cursor? = null
    private val viewModel: StaleImagesViewModel by viewModels()

    @Inject
    lateinit var viewIntentProvider: ViewIntentProvider

    private var attachmentInfoMap: Map<Uri, AttachmentInfo>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appComponent = (requireActivity().application as MyApplication).appComponent
        appComponent.inject(this)
        appComponent.inject(viewModel)
        attachmentInfoMap = attachmentInfoMap(requireContext(), true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        attachmentInfoMap = null
    }

    override fun dispatchCommandMultiple(
        command: Int,
        positions: SparseBooleanArray, itemIds: LongArray
    ): Boolean {
        if (super.dispatchCommandMultiple(command, positions, itemIds)) {
            return true
        }
        val activity = requireActivity() as BaseActivity
        if (command == R.id.SAVE_COMMAND) {
            activity.showSnackBar(R.string.saving)
            viewModel.saveImages(itemIds)
        } else if (command == R.id.DELETE_COMMAND) {
            activity.showSnackBar(R.string.progress_dialog_deleting)
            viewModel.deleteImages(itemIds)
        }
        finishActionMode()
        return true
    }

    override fun dispatchCommandSingle(command: Int, info: ContextMenu.ContextMenuInfo?): Boolean {
        if (super.dispatchCommandSingle(command, info)) {
            return true
        }
        if (command == R.id.VIEW_COMMAND) {
            viewIntentProvider.startViewAction(
                requireActivity(),
                uriAtPosition((info as AdapterView.AdapterContextMenuInfo?)!!.position)
            )
        }
        return false
    }

    private fun uriAtPosition(position: Int): Uri {
        imagesCursor!!.moveToPosition(position)
        return Uri.parse(
            imagesCursor!!.getString(
                imagesCursor!!.getColumnIndexOrThrow(DatabaseConstants.KEY_URI)
            )
        )
    }

    @SuppressLint("InlinedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.images_list, container, false)
        val lv = v.findViewById<GridView>(R.id.grid)

        // Create an array to specify the fields we want to display in the list
        val from = arrayOf(DatabaseConstants.KEY_URI)

        // and an array of the fields we want to bind those fields to 
        val to = intArrayOf(R.id.image)

        // Now create a simple cursor adapter and set it to display
        adapter = object : SimpleCursorAdapter(
            activity,
            R.layout.attachment_item_multiple_choice,
            null,
            from,
            to,
            0
        ) {
            override fun setViewImage(v: ImageView, value: String) {
                if (v.tag != null && v.tag == value) {
                    //already dealing with value; nothing to do
                    return
                }
                lifecycleScope.launch {
                    v.setAttachmentInfo(withContext(Dispatchers.IO) { attachmentInfoMap!!.getValue(Uri.parse(value)) })
                }
                v.tag = value
                v.contentDescription = value
            }
        }
        lv.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                (requireActivity() as ProtectedFragmentActivity).showSnackBar(
                    uriAtPosition(position).let {
                        attachmentInfoMap!!.getValue(it).file?.path ?: it.toString()
                    }
                )
            }
        LoaderManager.getInstance(this).initLoader(0, null, this)
        lv.adapter = adapter
        registerForContextualActionBar(lv)
        return v
    }

    override fun onCreateLoader(arg0: Int, arg1: Bundle?): Loader<Cursor> {
        return CursorLoader(
            requireActivity(),
            TransactionProvider.STALE_IMAGES_URI, null, null, null, null
        )
    }

    override fun onLoadFinished(arg0: Loader<Cursor>, c: Cursor) {
        imagesCursor = c
        adapter.swapCursor(c)
    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
        imagesCursor = null
        adapter.swapCursor(null)
    }

    override fun inflateContextualActionBar(menu: Menu, listId: Int) {
        val inflater = requireActivity().menuInflater
        inflater.inflate(R.menu.stale_images_context, menu)
    }
}
