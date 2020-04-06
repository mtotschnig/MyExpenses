package org.totschnig.myexpenses.fragment

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageTags
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.TagListBinding
import org.totschnig.myexpenses.viewmodel.TagListViewModel
import org.totschnig.myexpenses.viewmodel.data.Tag

const val KEY_TAGLIST = "tagList"
const val KEY_TAG = "tag"
const val KEY_POSITION = "position"
const val ACTION_MANAGE = "MANAGE"
const val ACTION_SELECT_MAPPING = "SELECT_MAPPING"
const val DELETE_TAG_DIALOG = "DELETE_TAG"

class TagList : Fragment(), OnDialogResultListener {
    private var _binding: TagListBinding? = null
    private lateinit var viewModel: TagListViewModel
    private lateinit var adapter: Adapter
    private lateinit var action: String

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var activeTag: Tag? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[TagListViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = TagListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        action = (context as? ManageTags)?.intent?.action ?: ACTION_SELECT_MAPPING
    }

    val shouldManage: Boolean
        get() = action == ACTION_MANAGE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selected = activity?.intent?.getParcelableArrayListExtra<Tag>(KEY_TAGLIST)
        viewModel.loadTags(selected).observe(viewLifecycleOwner, Observer {
            val closeFunction: (Tag) -> Unit = { tag ->
                if (tag.count == 0) {
                    removeTag(tag)
                } else {
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
            }
            val longClickFunction: (Tag) -> Unit = { tag ->
                binding.newTag.setText(tag.label)
                activeTag = tag
            }
            val itemLayoutResId = if (shouldManage) R.layout.tag_manage else R.layout.tag_select
            adapter = Adapter(it, itemLayoutResId, closeFunction, longClickFunction)
            binding.recyclerView.adapter = adapter
        })
        binding.newTag.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    addOrChangeTag()
                    true
                }
                EditorInfo.IME_NULL -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        addOrChangeTag()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun removeTag(tag: Tag) {
        val position = adapter.getPosition(tag.label)
        if (tag.id == -1L) {
            viewModel.removeTag(tag)
            adapter.notifyItemRemoved(position)
        } else {
            viewModel.removeTagAndPersist(tag).observe(viewLifecycleOwner, Observer {
                if (it) {
                    adapter.notifyItemRemoved(position)
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addOrChangeTag() {
        binding.newTag.text.toString().takeIf { !TextUtils.isEmpty(it) }?.let { label ->
            val position = adapter.getPosition(label)
            if (position > -1) {
                (activity as? ProtectedFragmentActivity)?.showSnackbar(R.string.tag_already_defined, Snackbar.LENGTH_LONG)
            } else {
                activeTag?.let { activeTag ->
                    val activePosition = adapter.getPosition(activeTag.label)
                    if (activePosition > -1) {
                        viewModel.updateTag(activeTag, label).observe(viewLifecycleOwner, Observer {
                            if (it) {
                                adapter.notifyItemChanged(activePosition)
                            }
                        })
                    }
                } ?: kotlin.run {
                    viewModel.addTagAndPersist(label).observe(viewLifecycleOwner, Observer {
                        if (it) {
                            adapter.notifyItemInserted(0)
                        }
                    })
                }

            }
            binding.newTag.text = null
        }
        activeTag = null
    }

    fun resultIntent() = Intent().apply {
        putParcelableArrayListExtra(KEY_TAGLIST, ArrayList(adapter.tagList.filter { tag -> tag.selected }))
    }

    private class Adapter(val tagList: MutableList<Tag>, val itemLayoutResId: Int,
                          val closeFunction: (Tag) -> Unit,
                          val longClickFunction: (Tag) -> Unit) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(LayoutInflater.from(parent.context).inflate(itemLayoutResId, parent, false))

        override fun getItemCount(): Int = tagList.size

        fun getPosition(label: String) = tagList.indexOfFirst { it.label.equals(label) }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as Chip).apply {
                val tag = tagList[position]
                text = tag.label
                isChecked = tag.selected
                setOnClickListener {
                    tag.selected = !tag.selected
                }
                setOnCloseIconClickListener {
                    closeFunction(tag)
                }
                setOnLongClickListener {
                    longClickFunction(tag)
                    true
                }
            }
        }

        private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle) =
            if (which == BUTTON_POSITIVE && dialogTag == DELETE_TAG_DIALOG) {
                removeTag(extras.getParcelable(KEY_TAG)!!)
                true
            } else false
}