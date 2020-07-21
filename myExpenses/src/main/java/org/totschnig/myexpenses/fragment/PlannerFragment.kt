package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.PlanInstanceBinding
import org.totschnig.myexpenses.databinding.PlannerFragmentBinding
import org.totschnig.myexpenses.dialog.CommitSafeDialogFragment
import org.totschnig.myexpenses.viewmodel.PlannerViewModell
import org.totschnig.myexpenses.viewmodel.data.PlanInstance


class PlannerFragment: CommitSafeDialogFragment() {

    private var _binding: PlannerFragmentBinding? = null
    // This property is only valid between onCreateDialog and onDestroyView.
    private val binding get() = _binding!!

    val model: PlannerViewModell by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = PlannerFragmentBinding.inflate(LayoutInflater.from(activity), null, false)
        binding.recyclerView.adapter = PlannerAdapter()
        registerForContextMenu(binding.recyclerView)
        model.getInstances().observe(this, Observer { list ->
            (binding.recyclerView.adapter as PlannerAdapter).addData(list)
        })
        if (savedInstanceState == null) {
            model.loadInstances()
        }
        return AlertDialog.Builder(requireContext())
                .setView(binding.root)
                .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete)
    }
}

class PlannerAdapter: RecyclerView.Adapter<PlanInstanceViewHolder>() {
    val data = mutableListOf<PlanInstance>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanInstanceViewHolder {
        val itemBinding = PlanInstanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        itemBinding.root.setOnClickListener({ view: View? -> (parent.context as Activity).openContextMenu(view) })
        return PlanInstanceViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun addData(data: List<PlanInstance>) {
        val previousCount = data.size
        this.data.addAll(data)
        notifyItemRangeInserted(previousCount, data.size)
    }

    override fun onBindViewHolder(holder: PlanInstanceViewHolder, position: Int) {
        holder.bind(data[position])
    }
}

class PlanInstanceViewHolder(private val itemBinding: PlanInstanceBinding): RecyclerView.ViewHolder(itemBinding.root) {
    fun bind(planInstance: PlanInstance) {
        itemBinding.date.text = planInstance.date.toString()
        itemBinding.label.text = planInstance.title + " " + planInstance.state
    }
}