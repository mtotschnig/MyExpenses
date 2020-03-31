package org.totschnig.myexpenses.fragment

import android.content.Intent
import android.os.Bundle
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
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.TagListBinding
import org.totschnig.myexpenses.viewmodel.TagListViewModel
import org.totschnig.myexpenses.viewmodel.data.Tag

const val KEY_TAGLIST = "tagList"

class TagList : Fragment() {
    private var _binding: TagListBinding? = null
    private lateinit var viewModel: TagListViewModel
    private lateinit var adapter: Adapter

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[TagListViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = TagListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = Adapter()
        binding.recyclerView.adapter = adapter
        viewModel.loadTags(-1).observe(viewLifecycleOwner, Observer {
            adapter.tagList.addAll(it)
            adapter.notifyDataSetChanged()
        })
        binding.newTag.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    addTag()
                    true
                }
                EditorInfo.IME_NULL -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        addTag()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addTag() {
        adapter.addTag(binding.newTag.text.toString())
        binding.newTag.text = null
    }

    fun resultIntent() = Intent().apply {
        putParcelableArrayListExtra(KEY_TAGLIST, ArrayList(adapter.tagList.filter { tag -> tag.selected }))
    }

}

class Adapter : RecyclerView.Adapter<ViewHolder>() {
    var tagList: MutableList<Tag> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.tag, parent, false))

    override fun getItemCount(): Int = tagList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView as Chip).apply {
            val tag = tagList[position]
            text = tag.label
            isChecked = tag.selected
            setOnClickListener({
                tag.selected = !tag.selected
            })
        }
    }

    fun addTag(label: String) {
        tagList.indexOfFirst { tag -> tag.label.equals(label) }.takeIf { it > -1 }?.let { position ->
            tagList[position].selected = true
            notifyItemChanged(position)
        } ?: kotlin.run {
            tagList.add(0, Tag(label, true))
            notifyItemInserted(0)
        }
    }
}

class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
