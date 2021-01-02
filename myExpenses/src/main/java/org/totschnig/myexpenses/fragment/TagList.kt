package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.input.SimpleInputDialog
import org.totschnig.myexpenses.ACTION_MANAGE
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageTags
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.TagListBinding
import org.totschnig.myexpenses.viewmodel.TagListViewModel
import org.totschnig.myexpenses.viewmodel.data.Tag

const val KEY_TAG_LIST = "tagList"
const val KEY_DELETED_IDS = "deletedIds"
const val KEY_TAG = "tag"
const val DELETE_TAG_DIALOG = "DELETE_TAG"
const val EDIT_TAG_DIALOG = "EDIT_TAG"

class TagList : Fragment(), OnDialogResultListener {
    private var _binding: TagListBinding? = null
    private lateinit var viewModel: TagListViewModel
    private lateinit var adapter: Adapter
    private lateinit var action: String

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = SavedStateViewModelFactory(requireActivity().application, this, null)
        viewModel = ViewModelProvider(this, factory)[TagListViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TagListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        action = (context as? ManageTags)?.intent?.action ?: ACTION_SELECT_MAPPING
    }

    private val shouldManage: Boolean
        get() = action == ACTION_MANAGE

    private val allowModifications: Boolean
        get() = action != ACTION_SELECT_FILTER

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selected = activity?.intent?.getParcelableArrayListExtra<Tag>(KEY_TAG_LIST)
        viewModel.loadTags(selected).observe(viewLifecycleOwner, {
            val closeFunction: (Tag) -> Unit = { tag ->
                SimpleDialog.build()
                        .title(R.string.dialog_title_warning_delete_tag)
                        .extra(Bundle().apply {
                            putParcelable(KEY_TAG, tag)
                        })
                        .msg(resources.getQuantityString(R.plurals.warning_delete_tag, tag.count, tag.label, tag.count))
                        .pos(R.string.menu_delete)
                        .neg(android.R.string.cancel)
                        .show(this, DELETE_TAG_DIALOG)
            }
            val longClickFunction: (Tag) -> Unit = { tag ->
                SimpleInputDialog.build()
                        .title(R.string.menu_edit_tag)
                        .cancelable(false)
                        .text(tag.label)
                        .pos(R.string.menu_save)
                        .neut()
                        .extra(Bundle().apply { putParcelable(KEY_TAG, tag) })
                        .show(this, EDIT_TAG_DIALOG)
            }
            val itemLayoutResId = if (shouldManage) R.layout.tag_manage else R.layout.tag_select
            adapter = Adapter(it, itemLayoutResId, if (allowModifications) closeFunction else null,
                    if (allowModifications) longClickFunction else null)
            binding.recyclerView.adapter = adapter
        })
        binding.tagEdit.apply {
            if (allowModifications) {
                setOnEditorActionListener { _, actionId, event: KeyEvent? ->
                    return@setOnEditorActionListener when (actionId) {
                        EditorInfo.IME_ACTION_DONE -> {
                            addTag()
                            true
                        }
                        EditorInfo.IME_NULL -> {
                            if (event == null || event.action == KeyEvent.ACTION_UP) {
                                addTag()
                            }
                            true
                        }
                        else -> false
                    }
                }
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun removeTag(tag: Tag) {
        val position = adapter.getPosition(tag.label)
        viewModel.removeTagAndPersist(tag).observe(viewLifecycleOwner, {
            if (it) {
                adapter.notifyItemRemoved(position)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addTag(runnable: Runnable? = null) {
        binding.tagEdit.text.toString().trim().takeIf { !TextUtils.isEmpty(it) }?.let { label ->
            val position = adapter.getPosition(label)
            if (position > -1) {
                (activity as? ProtectedFragmentActivity)?.showSnackbar(getString(R.string.already_defined, label))
            } else {
                viewModel.addTagAndPersist(label).observe(viewLifecycleOwner, {
                    if (it) {
                        adapter.notifyItemInserted(0)
                        runnable?.run()
                    }
                })
            }
            binding.tagEdit.text = null
        } ?: kotlin.run { runnable?.run() }
    }

    fun confirm() {
        if (::adapter.isInitialized ) {
            addTag {
                activity?.run {
                    setResult(Activity.RESULT_OK, resultIntent())
                    finish()
                }
            }
        }
    }

    private fun resultIntent() = Intent().apply {
        putParcelableArrayListExtra(KEY_TAG_LIST, ArrayList(adapter.tagList.filter { tag -> tag.selected }))
    }

    fun cancelIntent() = Intent().apply {
        viewModel.getDeletedTagIds().takeIf { it.isNotEmpty() }?.let {
            putExtra(KEY_DELETED_IDS, it)
        }
    }

    private class Adapter(val tagList: MutableList<Tag>, val itemLayoutResId: Int,
                          val closeFunction: ((Tag) -> Unit)?,
                          val longClickFunction: ((Tag) -> Unit)?) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(LayoutInflater.from(parent.context).inflate(itemLayoutResId, parent, false))

        override fun getItemCount(): Int = tagList.size

        fun getPosition(label: String) = tagList.indexOfFirst { it.label == label }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as Chip).apply {
                val tag = tagList[position]
                text = tag.label
                isChecked = tag.selected
                setOnClickListener {
                    tag.selected = !tag.selected
                }
                closeFunction?.let {
                    setOnCloseIconClickListener {
                        it(tag)
                    }
                } ?: kotlin.run { isCloseIconVisible = false }
                longClickFunction?.let {
                    setOnLongClickListener {
                        it(tag)
                        true
                    }
                }
            }
        }

        private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle) =
            if (which == BUTTON_POSITIVE) {
                val tag: Tag = extras.getParcelable(KEY_TAG)!!
                when (dialogTag) {
                    DELETE_TAG_DIALOG -> {
                        removeTag(tag)
                    }
                    EDIT_TAG_DIALOG -> {
                        val activePosition = adapter.getPosition(tag.label)
                        val newLabel = extras.getString(SimpleInputDialog.TEXT)!!
                        viewModel.updateTag(tag, newLabel).observe(viewLifecycleOwner, {
                            if (it) {
                                adapter.notifyItemChanged(activePosition)
                            } else {
                                (context as? ProtectedFragmentActivity)?.showSnackbar(getString(R.string.already_defined, newLabel))
                            }
                        })
                    }
                }
                true
            } else false
}