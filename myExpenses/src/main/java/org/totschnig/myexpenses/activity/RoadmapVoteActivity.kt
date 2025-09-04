package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.Check
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.RoadmapBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.retrofit.Issue
import org.totschnig.myexpenses.retrofit.Vote
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView.RecyclerContextMenuInfo
import org.totschnig.myexpenses.ui.SimpleSeekBarDialog
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel
import org.totschnig.myexpenses.viewmodel.repository.RoadmapRepository
import org.totschnig.myexpenses.viewmodel.repository.RoadmapRepository.Companion.ROADMAP_URL
import java.util.Locale

class RoadmapVoteActivity : ProtectedFragmentActivity(), OnDialogResultListener {
    private lateinit var binding: RoadmapBinding
    private var dataSet: List<Issue>? = null
    private var dataSetFiltered: List<Issue>? = null
    private lateinit var voteWeights: MutableMap<Int, Int>
    private var lastVote: Vote? = null
    private var voteKey: Pair<String, String>? = null
    private lateinit var roadmapAdapter: RoadmapAdapter
    private lateinit var roadmapViewModel: RoadmapViewModel
    private var isPro = false
    private var query: String? = null
    private var isLoading = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RoadmapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.myRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        roadmapAdapter = RoadmapAdapter()
        binding.myRecyclerView.adapter = roadmapAdapter
        registerForContextMenu(binding.myRecyclerView)
        setupToolbar()
        supportActionBar?.setTitle(R.string.roadmap_vote)
        isPro = licenceHandler.hasAccessTo(ContribFeature.ROADMAP_VOTING)
        showIsLoading()
        roadmapViewModel = ViewModelProvider(this)[RoadmapViewModel::class.java]
        injector.inject(roadmapViewModel)
        voteWeights = roadmapViewModel.restoreWeights()
        roadmapViewModel.getData().observe(this) { data: List<Issue>? ->
            dataSet = data?.also {
                publishResult("${it.size} issues found")
            }
            if (dataSet == null) {
                publishResult("Failure loading data")
            } else {
                validateAndUpdateUi()
            }
        }
        voteKey = licenceHandler.roadmapVoteKey
        roadmapViewModel.getLastVote().observe(this) { result: Vote? ->
            if (result != null && result.key == voteKey) {
                lastVote = result
                if (voteWeights.isEmpty()) {
                    voteWeights.putAll(result.vote)
                    validateAndUpdateUi()
                }
            }
            roadmapViewModel.loadData(RoadmapRepository.VERSION > versionFromPref)
        }
    }

    override val drawToBottomEdge = false

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
            issue2.number.compareTo(issue1.number)
        }
        filterData()
        invalidateOptionsMenu()
    }

    private fun validateWeights() {
        dataSet?.takeIf { voteWeights.isNotEmpty() }?.let { dataSet ->
            voteWeights = voteWeights.filter { entry -> dataSet.any { it.number == entry.key } }
                .toMutableMap()
        }
    }

    private fun showIsLoading() {
        isLoading = true
        showSnackBarIndefinite(R.string.roadmap_loading)
    }

    private fun publishResult(message: String) {
        isLoading = false
        dismissSnackBar()
        showSnackBar(message)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.search, menu)
        configureSearch(this, menu,  callback = ::onQueryTextChange)
        menu.add(Menu.NONE, R.id.ROADMAP_SUBMIT_VOTE, 0, "").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        inflater.inflate(R.menu.vote, menu)
        inflater.inflate(R.menu.help_with_icon, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.ROADMAP_SUBMIT_VOTE)?.let {
            val currentTotalWeight = currentTotalWeight
            val enabled = currentTotalWeight == totalAvailableWeight
            it.title = if (enabled) "Submit" else "$currentTotalWeight/$totalAvailableWeight"
            it.isEnabled = enabled
        }
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
                data.filter { issue: Issue ->
                    issue.title.lowercase(Locale.ROOT).contains(
                        it.lowercase(
                            Locale.ROOT
                        )
                    )
                }
            } ?: data
        }
        roadmapAdapter.notifyDataSetChanged()
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        return when (command) {
            R.id.ROADMAP_RESULT_COMMAND -> {
                startActionView(ROADMAP_URL + "issues.html")
                true
            }

            R.id.ROADMAP_CLEAR_COMMAND_DO -> {
                voteWeights.clear()
                validateAndUpdateUi()
                true
            }

            R.id.ROADMAP_CLEAR_COMMAND -> {
                ConfirmationDialogFragment.newInstance(Bundle().apply {
                    putString(
                        ConfirmationDialogFragment.KEY_TITLE_STRING,
                        "Clear Vote"
                    )
                    putString(
                        ConfirmationDialogFragment.KEY_MESSAGE,
                        getString(R.string.menu_RoadmapVoteActivity_clear_help_text)
                    )
                    putInt(
                        ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                        R.id.ROADMAP_CLEAR_COMMAND_DO
                    )
                    putInt(
                        ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                        R.string.menu_clear
                    )
                })
                    .show(supportFragmentManager, "Clear")
                true
            }

            R.id.SYNC_COMMAND -> {
                showIsLoading()
                roadmapViewModel.loadData(true)
                true
            }

            R.id.ROADMAP_SUBMIT_VOTE -> {
                if (lastVote?.let { it.vote == voteWeights && it.version == versionFromPref } == true) {
                    showSnackBar("Modify your vote, before submitting it again.")
                } else {
                    val msg = if (lastVote != null) {
                        getString(R.string.roadmap_update_confirmation)
                    } else {
                        getString(R.string.roadmap_email_rationale)
                    }
                    val simpleFormDialog = SimpleFormDialog.build().msg(msg)
                    val fields = buildList {
                        if (lastVote == null) {
                            add(Input.email(KEY_EMAIL).required().apply {
                                if (isPro && DistributionHelper.isGithub) {
                                    text(prefHandler.getString(PrefKey.LICENCE_EMAIL, null))
                                }
                            })
                        }
                        add(Check.box(KEY_CONTACT_CONSENT).label(getString(R.string.roadmap_consent_checkbox)))
                    }.toTypedArray()
                    simpleFormDialog.fields(*fields)
                    simpleFormDialog.show(this, DIALOG_TAG_SUBMIT_VOTE)
                }
                true
            }

            else -> false
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
        val info = item.menuInfo as? RecyclerContextMenuInfo
            ?: return super.onContextItemSelected(item)
        val itemId = item.itemId
        if (itemId == R.id.ROADMAP_DETAILS_COMMAND) {
            startActionView("https://github.com/mtotschnig/MyExpenses/issues/" + info.id)
            return true
        } else if (itemId == R.id.ROADMAP_ISSUE_VOTE_COMMAND) {
            val value = voteWeights[info.id.toInt()]
            var available = totalAvailableWeight - currentTotalWeight
            if (value != null) {
                available += value
            }
            if (available > 0) {
                val dialog = SimpleSeekBarDialog.build()
                    .title(dataSetFiltered!![info.position].title)
                    .max(available)
                    .extra(Bundle(2).apply {
                        putInt(DatabaseConstants.KEY_ROWID, info.id.toInt())
                        putInt(KEY_POSITION, info.position)
                    })
                if (value != null) {
                    dialog.value(value.coerceAtMost(available))
                }
                dialog.show(this, DIALOG_TAG_ISSUE_VOTE)
            } else {
                showSnackBar("You spent all your points on other issues.", Snackbar.LENGTH_SHORT)
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
                    invalidateOptionsMenu()
                    return true
                }

                DIALOG_TAG_SUBMIT_VOTE -> {
                    showSnackBarIndefinite(R.string.roadmap_submitting)
                    isLoading = true
                    val vote = Vote(
                        voteKey,
                        HashMap(voteWeights),
                        lastVote?.email ?: extras.getString(KEY_EMAIL)!!,
                        versionFromPref,
                        extras.getBoolean(KEY_CONTACT_CONSENT)
                    )
                    roadmapViewModel.submitVote(vote).observe(this) { result ->
                        result.onSuccess {
                            lastVote = vote
                            publishResult(getString(R.string.roadmap_vote_success))
                        }.onFailure {
                            publishResult(it.safeMessage)
                        }
                    }
                    return true
                }
            }
        }
        return false
    }

    private val totalAvailableWeight: Int
        get() = if (isPro) 50 else 10

    private inner class RoadmapAdapter : RecyclerView.Adapter<ViewHolder>() {
        init {
            setHasStableIds(true)
        }

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

    private inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.text)
        val weightView: TextView = itemView.findViewById(R.id.weight)
    }

    override val snackBarContainerId: Int = R.id.main_content

    companion object {
        private const val DIALOG_TAG_ISSUE_VOTE = "issueVote"
        private const val DIALOG_TAG_SUBMIT_VOTE = "ROADMAP_VOTE"
        private const val KEY_POSITION = "position"
        private const val KEY_EMAIL = "EMAIL"
        private const val KEY_CONTACT_CONSENT = "CONTACT_CONSENT"
    }
}