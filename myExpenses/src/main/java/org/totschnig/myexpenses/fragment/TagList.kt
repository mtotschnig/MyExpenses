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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.input.SimpleInputDialog
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.Action
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.asAction
import org.totschnig.myexpenses.databinding.TagListBinding
import org.totschnig.myexpenses.viewmodel.TagBaseViewModel.Companion.KEY_DELETED_IDS
import org.totschnig.myexpenses.viewmodel.TagListViewModel
import org.totschnig.myexpenses.viewmodel.TagListViewModel.Companion.KEY_SELECTED_IDS
import org.totschnig.myexpenses.viewmodel.data.Tag

class TagList : Fragment(), OnDialogResultListener {
    private var _binding: TagListBinding? = null
    private val viewModel: TagListViewModel by activityViewModels()
    private lateinit var adapter: Adapter

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TagListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val action
        get() = requireActivity().intent.asAction

    private val shouldManage: Boolean
        get() = action == Action.MANAGE

    private val allowModifications: Boolean
        get() = action != Action.SELECT_FILTER

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selected = activity?.intent?.getLongArrayExtra(KEY_SELECTED_IDS)

        val closeFunction: (Tag) -> Unit = { tag ->
            SimpleDialog.build()
                .title(R.string.dialog_title_warning_delete_tag)
                .extra(Bundle().apply {
                    putParcelable(KEY_TAG, tag)
                })
                .msg(
                    resources.getQuantityString(
                        R.plurals.warning_delete_tag,
                        tag.count,
                        tag.label,
                        tag.count
                    )
                )
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
        adapter = Adapter(
            itemLayoutResId, if (allowModifications) closeFunction else null,
            if (allowModifications) longClickFunction else null
        )
        binding.recyclerView.adapter = adapter
        viewModel.tags.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        selected?.let { viewModel.selectedTagIds = it.toHashSet() }

        if (savedInstanceState == null) viewModel.loadTags()

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
        viewModel.removeTagAndPersist(tag)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addTag(commit: Boolean = false) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        binding.tagEdit.text.toString().trim().takeIf { !TextUtils.isEmpty(it) }?.let { label ->
            val position = adapter.getPosition(label)
            if (position > -1) {
                (activity as? ProtectedFragmentActivity)?.showSnackBar(
                    getString(
                        R.string.already_defined,
                        label
                    )
                )
            } else {
                viewModel.addTagAndPersist(label).observe(viewLifecycleOwner) {
                    if (commit) {
                        commit(it)
                    }
                }
            }
            binding.tagEdit.text = null
        } ?: kotlin.run { if (commit) commit(null) }
    }

    private fun commit(tag: Tag?) {
        activity?.run {
            setResult(Activity.RESULT_OK, resultIntent(tag))
            finish()
        }
    }

    fun confirm() {
        if (::adapter.isInitialized) {
            addTag(true)
        }
    }

    /**
     * @param newTag if User confirms with a new tag pending in the editor, after the tag has been
     * inserted into the db, it won't be visible in the adapter when we build the result, that's
     * why we add it explicitly here
     */
    private fun resultIntent(newTag: Tag?) = Intent().apply {
        putParcelableArrayListExtra(
            KEY_TAG_LIST,
            ArrayList(
                buildList {
                    addAll(adapter.currentList)
                    newTag?.let { add(it) }
                }
                    .distinct()
                    .filter { viewModel.selectedTagIds.contains(it.id) }
            )
        )
        backwardCanceledTags()
    }

    private fun Intent.backwardCanceledTags() {
        viewModel.deletedTagIds.takeIf { it.isNotEmpty() }?.let {
            putExtra(KEY_DELETED_IDS, it)
        }
    }

    fun cancelIntent() = Intent().apply {
        backwardCanceledTags()
    }

    private inner class Adapter(
        val itemLayoutResId: Int,
        val closeFunction: ((Tag) -> Unit)?,
        val longClickFunction: ((Tag) -> Unit)?
    ) : ListAdapter<Tag, ViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(itemLayoutResId, parent, false))

        fun getPosition(label: String) = currentList.indexOfFirst { it.label == label }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as Chip).apply {
                val tag = getItem(position)
                text = tag.label
                isChecked = viewModel.selectedTagIds.contains(tag.id)
                setOnClickListener {
                    viewModel.toggleSelectedTagId(tag.id)
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
    }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onResult(dialogTag: String, which: Int, extras: Bundle) =
        if (which == BUTTON_POSITIVE) {
            val tag: Tag = extras.getParcelable(KEY_TAG)!!
            when (dialogTag) {
                DELETE_TAG_DIALOG -> {
                    removeTag(tag)
                }
                EDIT_TAG_DIALOG -> {
                    val newLabel = extras.getString(SimpleInputDialog.TEXT)!!
                    viewModel.updateTag(tag, newLabel).observe(viewLifecycleOwner) {
                        if (!it) {
                            (context as? ProtectedFragmentActivity)?.showSnackBar(
                                getString(
                                    R.string.already_defined,
                                    newLabel
                                )
                            )
                        }
                    }
                }
            }
            true
        } else false

    companion object {
        const val KEY_TAG_LIST = "tagList"
        const val KEY_TAG = "tag"
        const val DELETE_TAG_DIALOG = "DELETE_TAG"
        const val EDIT_TAG_DIALOG = "EDIT_TAG"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Tag>() {
            override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
                return oldItem == newItem
            }
        }
    }
}