package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.RoadmapBinding
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.retrofit.Issue
import org.totschnig.myexpenses.retrofit.Vote
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView.RecyclerContextMenuInfo
import org.totschnig.myexpenses.ui.SimpleSeekBarDialog
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel
import org.totschnig.myexpenses.viewmodel.repository.RoadmapRepository.Companion.ROADMAP_URL
import java.util.*

class RoadmapVoteActivity : ProtectedFragmentActivity(), OnDialogResultListener {
    private lateinit var binding: RoadmapBinding
    private var dataSet: List<Issue>? = null
    private var dataSetFiltered: List<Issue>? = null
    private var voteMenuItem: MenuItem? = null
    private lateinit var voteWeights: MutableMap<Int, Int>
    private var lastVote: Vote? = null
    private lateinit var roadmapAdapter: RoadmapAdapter
    private lateinit var roadmapViewModel: RoadmapViewModel
    private var isPro = false
    private var query: String? = null
    private var isLoading = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RoadmapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.myRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        roadmapAdapter = RoadmapAdapter()
        binding.myRecyclerView.adapter = roadmapAdapter
        registerForContextMenu(binding.myRecyclerView)
        setupToolbar(true)
        supportActionBar?.setTitle(R.string.roadmap_vote)
        isPro = licenceHandler.hasAccessTo(ContribFeature.ROADMAP_VOTING)
        showIsLoading()
        roadmapViewModel = ViewModelProvider(this).get(RoadmapViewModel::class.java)
        voteWeights = roadmapViewModel.restoreWeights()
        roadmapViewModel.getData().observe(this, { data: List<Issue>? ->
            dataSet = data?.also {
                publishResult(String.format(Locale.getDefault(), "%d issues found", it.size))
            }
            if (dataSet == null) {
                publishResult("Failure loading data")
            } else {
                validateAndUpdateUi()
            }
        })
        roadmapViewModel.getLastVote().observe(this, { result: Vote? ->
            if (result != null && result.isPro == isPro) {
                lastVote = result
                if (voteWeights.isEmpty()) {
                    voteWeights.putAll(result.vote)
                    validateAndUpdateUi()
                }
            }
            roadmapViewModel.loadData(RoadmapViewModel.EXPECTED_MINIMAL_VERSION > versionFromPref)
        })
    }

    override fun onPause() {
        super.onPause()
        roadmapViewModel.cacheWeights(voteWeights)
    }

    private val versionFromPref: Int
        get() = prefHandler.getInt(PrefKey.ROADMAP_VERSION, 0)

    private fun validateAndUpdateUi() {
        validateWeights()
        dataSet = dataSet?.sortedWith { issue1: Issue, issue2: Issue ->
            val weight1 = voteWeights[issue1.number]
            val weight2 = voteWeights[issue2.number]
            if (weight1 != null) {
                return@sortedWith weight2?.compareTo(weight1) ?: -1
            }
            if (weight2 != null) {
                return@sortedWith 1
            }
            Utils.compare(issue2.number, issue1.number)
        }
        filterData()
        updateVoteMenuItem()
    }

    private fun validateWeights() {
        dataSet?.takeIf { voteWeights.isNotEmpty() }?.let { dataSet ->
            voteWeights =  voteWeights.filter { entry -> dataSet.any { it.number == entry.key } }.toMutableMap()
        }
    }

    private fun showIsLoading() {
        isLoading = true
        showSnackbar("Loading issues ...", Snackbar.LENGTH_INDEFINITE)
    }

    private fun publishResult(message: String) {
        isLoading = false
        dismissSnackbar()
        showSnackbar(message)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.search, menu)
        configureSearch(this, menu) { newText: String? -> onQueryTextChange(newText) }
        voteMenuItem = menu.add(Menu.NONE, R.id.ROADMAP_SUBMIT_VOTE, 0, "").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        inflater.inflate(R.menu.vote, menu)
        inflater.inflate(R.menu.help_with_icon, menu)
        return true
    }

    private fun onQueryTextChange(newText: String?): Boolean {
        query = newText
        filterData()
        return true
    }

    private fun filterData() {
        dataSetFiltered = dataSet?.let { data ->
            query.takeIf { !it.isNullOrEmpty() }?.let {
                data.filter { issue: Issue -> issue.title.toLowerCase(Locale.ROOT).contains(it.toLowerCase(Locale.ROOT)) }
            } ?: data
        }
        roadmapAdapter.notifyDataSetChanged()
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        when (command) {
            R.id.ROADMAP_RESULT_COMMAND -> {
                startActionView(ROADMAP_URL + "issues.html")
                return true
            }
            R.id.SYNC_COMMAND -> {
                showIsLoading()
                roadmapViewModel.loadData(true)
                return true
            }
            R.id.ROADMAP_SUBMIT_VOTE -> {
                if (lastVote?.let { it.vote == voteWeights && it.version == versionFromPref } == true) {
                    showSnackbar("Modify your vote, before submitting it again.")
                } else {
                    val emailIsKnown = email != null
                    val msg = if (emailIsKnown) R.string.roadmap_update_confirmation else R.string.roadmap_email_rationale
                    val simpleFormDialog = SimpleFormDialog.build().msg(msg)
                    if (!emailIsKnown) {
                        simpleFormDialog.fields(Input.email(KEY_EMAIL).required())
                    }
                    simpleFormDialog.show(this, DIALOG_TAG_SUBMIT_VOTE)
                }
                return true
            }
            else -> return false
        }
    }

    private val email: String?
        get() = lastVote?.email

    private fun updateVoteMenuItem() {
        voteMenuItem?.let {
            val currentTotalWeight = currentTotalWeight
            val enabled = currentTotalWeight == totalAvailableWeight
            it.title = if (enabled) "Submit" else String.format(Locale.ROOT, "%d/%d", currentTotalWeight, totalAvailableWeight)
            it.isEnabled = enabled
        }
    }

    private val currentTotalWeight: Int
        get() {
            var currentTotalWeight = 0
            for ((_, value) in voteWeights) {
                currentTotalWeight += +value
            }
            return currentTotalWeight
        }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        val inflater = menuInflater
        inflater.inflate(R.menu.roadmap_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as RecyclerContextMenuInfo
        val itemId = item.itemId
        if (itemId == R.id.ROADMAP_DETAILS_COMMAND) {
            startActionView("https://github.com/mtotschnig/MyExpenses/issues/" + info.id)
            return true
        } else if (itemId == R.id.ROADMAP_ISSUE_VOTE_COMMAND) {
            val extra = Bundle(1)
            extra.putInt(DatabaseConstants.KEY_ROWID, info.id.toInt())
            extra.putInt(KEY_POSITION, info.position)
            val value = voteWeights[info.id.toInt()]
            var available = totalAvailableWeight - currentTotalWeight
            if (value != null) {
                available += value
            }
            if (available > 0) {
                val dialog = SimpleSeekBarDialog.build()
                        .title(dataSetFiltered!![info.position].title)
                        .max(available)
                        .extra(extra)
                if (value != null) {
                    dialog.value(value)
                }
                dialog.show(this, DIALOG_TAG_ISSUE_VOTE)
            } else {
                showSnackbar("You spent all your points on other issues.", Snackbar.LENGTH_SHORT)
            }
            return true
        }
        return false
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == OnDialogResultListener.BUTTON_POSITIVE) {
            when (dialogTag) {
                DIALOG_TAG_ISSUE_VOTE -> {
                    val value = extras.getInt(SimpleSeekBarDialog.SEEKBAR_VALUE)
                    val issueId = extras.getInt(DatabaseConstants.KEY_ROWID)
                    if (value > 0) {
                        voteWeights[issueId] = value
                    } else {
                        voteWeights.remove(issueId)
                    }
                    validateAndUpdateUi()
                    updateVoteMenuItem()
                    return true
                }
                DIALOG_TAG_SUBMIT_VOTE -> {
                    showSnackbar("Submitting vote ...", Snackbar.LENGTH_INDEFINITE)
                    isLoading = true
                    roadmapViewModel.submitVote(Vote(lastVote?.key ?: licenceHandler.buildRoadmapVoteKey(),
                            HashMap(voteWeights),
                            isPro,
                            email ?: extras.getString(KEY_EMAIL)!!, versionFromPref)).observe(this,
                            { result -> publishResult(getString(result)) })
                    return true
                }
            }
        }
        return false
    }

    private val totalAvailableWeight: Int
        get() = if (isPro) 50 else 10

    private inner class RoadmapAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(this@RoadmapVoteActivity)
            val row = inflater.inflate(R.layout.roadmap_list_item, parent, false)
            row.setOnClickListener { view: View? -> openContextMenu(view) }
            return ViewHolder(row)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val issue = dataSetFiltered!![position]
            holder.textView.text = issue.title
            val weight = voteWeights[issue.number]
            holder.weightView.text = weight?.toString() ?: "0"
        }

        override fun getItemId(position: Int): Long {
            return dataSetFiltered!![position].number.toLong()
        }

        override fun getItemCount(): Int {
            return dataSetFiltered?.size ?: 0
        }
    }

    private inner class ViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.text)
        val weightView: TextView = itemView.findViewById(R.id.weight)

    }

    override fun getSnackbarContainerId(): Int {
        return R.id.container
    }

    companion object {
        private const val DIALOG_TAG_ISSUE_VOTE = "issueVote"
        private const val DIALOG_TAG_SUBMIT_VOTE = "ROADMAP_VOTE"
        private const val KEY_POSITION = "position"
        private const val KEY_EMAIL = "EMAIL"
    }
}