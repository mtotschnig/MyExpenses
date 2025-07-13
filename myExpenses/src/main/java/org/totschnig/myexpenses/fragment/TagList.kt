package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import eltos.simpledialogfragment.form.ColorField
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.Action
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.asAction
import org.totschnig.myexpenses.databinding.TagListBinding
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.filter.KEY_SELECTION
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView.RecyclerContextMenuInfo
import org.totschnig.myexpenses.util.ui.setColor
import org.totschnig.myexpenses.viewmodel.TagBaseViewModel.Companion.KEY_DELETED_IDS
import org.totschnig.myexpenses.viewmodel.TagListViewModel
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
        savedInstanceState: Bundle?,
    ): View {
        _binding = TagListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val action
        get() = requireActivity().intent.asAction

    private val allowSelection: Boolean
        get() = action != Action.MANAGE

    private val allowModifications: Boolean
        get() = action != Action.SELECT_FILTER

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?,
    ) {
        requireActivity().menuInflater.inflate(R.menu.tags, menu)
        menu.findItem(R.id.EDIT_COMMAND).title =
            "${getString(R.string.menu_edit)} / ${getString(R.string.color)}"
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? RecyclerContextMenuInfo
            ?: return super.onContextItemSelected(item)
        val tag = adapter.getItem(info.position)
        return when (item.itemId) {
            R.id.DELETE_COMMAND -> {
                onDelete(tag)
                true
            }

            R.id.EDIT_COMMAND -> {
                onEdit(tag)
                true
            }

            else -> false
        }
    }

    private fun onEdit(tag: Tag) {
        SimpleFormDialog.build()
            .title(R.string.menu_edit_tag)
            .cancelable(false)
            .fields(
                Input.plain(KEY_LABEL).text(tag.label),
                ColorField.picker(KEY_COLOR).color(tag.color ?: ColorField.NONE)
                    .label(R.string.color)
            )
            .pos(R.string.menu_save)
            .neut()
            .extra(Bundle().apply { putParcelable(KEY_TAG, tag) })
            .show(this, EDIT_TAG_DIALOG)
    }

    private fun onDelete(tag: Tag) {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selected = activity?.intent?.getLongArrayExtra(KEY_SELECTION)
        adapter = Adapter()
        registerForContextMenu(binding.recyclerView)
        binding.recyclerView.adapter = adapter
        viewModel.tags.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.emptyView.isVisible = it.isEmpty() && allowModifications
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView, { v, insets ->
            val navigationInsets = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars()
            )

            v.updatePadding(
                bottom = navigationInsets.bottom
            )

            WindowInsetsCompat.CONSUMED
        })
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
        putExtra(KEY_ACCOUNTID, requireActivity().intent.getLongExtra(KEY_ACCOUNTID, 0))
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

    private inner class Adapter : ListAdapter<Tag, ViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    if (allowSelection) R.layout.tag_select else R.layout.tag_manage,
                    parent,
                    false
                )
            )

        fun getPosition(label: String) = currentList.indexOfFirst { it.label == label }

        public override fun getItem(position: Int): Tag {
            return super.getItem(position)
        }

        override fun getItemViewType(position: Int) =
            if (getItem(position).color == null) 0 else 1

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as Chip).apply {
                val tag = getItem(position)
                text = tag.label
                tag.color?.let { setColor(it) }
                if (allowSelection) {
                    isChecked = viewModel.selectedTagIds.contains(tag.id)
                    setOnClickListener {
                        viewModel.toggleSelectedTagId(tag.id)
                    }
                }
                isCloseIconVisible = allowModifications
                if (allowModifications) {
                    setOnCloseIconClickListener(View::showContextMenu)
                    ViewCompat.addAccessibilityAction(
                        holder.itemView,
                        context.getString(R.string.menu_edit)
                    ) { _, _ ->
                        onEdit(tag)
                        true
                    }
                    ViewCompat.addAccessibilityAction(
                        holder.itemView,
                        context.getString(R.string.menu_delete)
                    ) { _, _ ->
                        onDelete(tag)
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
                    val newLabel = extras.getString(KEY_LABEL)!!
                    viewModel.updateTag(tag, newLabel, extras.getInt(KEY_COLOR))
                        .observe(viewLifecycleOwner) {
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