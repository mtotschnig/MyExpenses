package org.totschnig.myexpenses.next.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import org.totschnig.myexpenses.next.core.ui.DetailPage
import org.totschnig.myexpenses.next.core.ui.ExtraPage
import org.totschnig.myexpenses.next.core.ui.ListPage
import org.totschnig.myexpenses.next.core.ui.rememberListDetailPages
import org.totschnig.myexpenses.designsystem.DetailCard
import org.totschnig.myexpenses.designsystem.DialogActionRow
import org.totschnig.myexpenses.designsystem.DetailScaffold
import org.totschnig.myexpenses.designsystem.EmptyState
import org.totschnig.myexpenses.designsystem.Highlighted
import org.totschnig.myexpenses.designsystem.HintRow
import org.totschnig.myexpenses.designsystem.IconNavigationRow
import org.totschnig.myexpenses.designsystem.MultiChoiceSheet
import org.totschnig.myexpenses.designsystem.PageTopBar
import org.totschnig.myexpenses.designsystem.RowDivider
import org.totschnig.myexpenses.designsystem.SearchField
import org.totschnig.myexpenses.designsystem.SectionTitle
import org.totschnig.myexpenses.designsystem.SingleChoiceDialog
import org.totschnig.myexpenses.designsystem.SliderRow
import org.totschnig.myexpenses.designsystem.SwitchRow
import org.totschnig.myexpenses.designsystem.TextDialog
import org.totschnig.myexpenses.designsystem.TimeDialog
import org.totschnig.myexpenses.designsystem.Spacing
import org.totschnig.myexpenses.designsystem.ValueRow
import org.totschnig.myexpenses.next.core.common.R
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** What the settings screens do with an entry */
interface SettingsActions {
    /** The title of [entry] as shown in a group and in the results of the search */
    fun title(entry: SettingEntry): String

    /** What [entry] is for in everyday words, see [SettingTexts.explanations] */
    fun explanation(entry: SettingEntry): String?

    /** What else the search finds [entry] by, e.g. its summary and the options of a list */
    fun details(entry: SettingEntry): String

    /** False for entries that do not apply, e.g. those of the old main screen */
    fun isVisible(entry: SettingEntry): Boolean

    /** False for entries that cannot be opened yet */
    fun canOpen(entry: SettingEntry): Boolean

    /** Opens the old screen or the dialog of [entry], for entries without [control] */
    fun open(entry: SettingEntry)

    /** How [entry] is changed right on the group page, null if it is opened instead */
    @Composable
    fun control(entry: SettingEntry): SettingControl?

    /** The page a [SettingControl.Page] opens, on the back stack of the settings */
    @Composable
    fun page(setting: InsightSetting, onBack: () -> Unit)
}

/** The pages of the settings, the start page or the group opened right away at the bottom */
@Serializable
sealed interface SettingsKey : NavKey {
    @Serializable
    data object Start : SettingsKey

    /** @param highlighted key of the entry found by the search, until it has been shown */
    @Serializable
    data class Group(val group: SettingsGroup, val highlighted: String? = null) : SettingsKey

    /** A page of the new UI behind an entry, e.g. the rules of categories, by the [InsightSetting.id] */
    @Serializable
    data class Page(val setting: String) : SettingsKey
}

/** A setting changed right on the group page */
sealed interface SettingControl {
    val isEnabled: Boolean

    data class Switch(
        val checked: Boolean,
        val summary: String?,
        override val isEnabled: Boolean,
        val onChange: (Boolean) -> Unit,
    ) : SettingControl

    /**
     * @param options value to label
     * @param explanation above the options
     * @param custom a value of its own besides the options, e.g. the pattern of a date format
     */
    data class Choice(
        val selected: String?,
        val options: List<Pair<String, String>>,
        override val isEnabled: Boolean,
        val onSelect: (String) -> Unit,
        val explanation: String? = null,
        val custom: CustomValue? = null,
    ) : SettingControl {
        /** The label of the option selected, or else how the value of its own looks */
        val selectedLabel: String?
            get() = options.firstOrNull { it.first == selected }?.second
                ?: selected?.let { custom?.preview?.invoke(it) ?: it }
    }

    /**
     * @param legend what the letters of a pattern stand for
     * @param preview how a value looks, e.g. today in a date pattern
     * @param error of a value, null if it is fine
     */
    data class CustomValue(
        val legend: String,
        val preview: (String) -> String?,
        val error: (String) -> String?,
    )

    /**
     * @param summary below the value, e.g. what the text is used for
     * @param isPassword hides the value
     */
    data class Text(
        val value: String,
        val isNumber: Boolean,
        override val isEnabled: Boolean,
        val onChange: (String) -> Unit,
        val summary: String? = null,
        val isPassword: Boolean = false,
    ) : SettingControl

    /**
     * @param isStepped stops at every whole number of [range], e.g. the steps of the font size
     * @param valueText the value as shown next to the title
     * @param sampleScale of the font of a sample text below, to show a font size while dragging
     */
    data class Slider(
        val value: Int,
        val range: IntRange,
        override val isEnabled: Boolean,
        val onChange: (Int) -> Unit,
        val isStepped: Boolean = false,
        val valueText: (Int) -> String = { it.toString() },
        val sampleScale: ((Int) -> Float)? = null,
    ) : SettingControl

    /** @param options value to label */
    data class MultiChoice(
        val selected: Set<String>,
        val options: List<Pair<String, String>>,
        override val isEnabled: Boolean,
        val onChange: (Set<String>) -> Unit,
    ) : SettingControl

    data class Time(
        val hour: Int,
        val minute: Int,
        val text: String,
        override val isEnabled: Boolean,
        val onChange: (hour: Int, minute: Int) -> Unit,
    ) : SettingControl

    /** Opens a dialog or does something right away, e.g. resetting hints; [value] tells what is there */
    data class Action(val value: String?, override val isEnabled: Boolean, val onClick: () -> Unit) : SettingControl

    /** Opens the page of [setting], see [SettingsActions.page]; [value] tells what is there */
    data class Page(val value: String?, override val isEnabled: Boolean, val setting: InsightSetting) : SettingControl

    /** Only explains something, e.g. that the database is encrypted; without [text] the title */
    data class Info(val text: String?) : SettingControl {
        override val isEnabled = true
    }
}

/**
 * The settings: the start page, the page of a group and the pages behind entries, on a back stack
 * of their own ([SettingsKey]). Back on the page at the bottom leaves the settings.
 *
 * @param initialGroup shown instead of the start page, e.g. from the tab of an analysis
 */
@Composable
fun NextSettingsScreen(actions: SettingsActions, onBack: () -> Unit, initialGroup: SettingsGroup? = null) {
    val backStack = rememberNavBackStack(initialGroup?.let { SettingsKey.Group(it) } ?: SettingsKey.Start)
    /** Leaves [key], and the settings if it is the page at the bottom */
    fun leave(key: SettingsKey) {
        if (backStack.size == 1) onBack() else backStack.remove(key)
    }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(rememberListDetailPages()),
        entryProvider = entryProvider {
            entry<SettingsKey.Start>(metadata = ListPage(R.string.next_pick_settings)) {
                SettingsStart(actions, onBack) { group, highlighted -> backStack.add(SettingsKey.Group(group, highlighted)) }
            }
            entry<SettingsKey.Group>(metadata = DetailPage()) { key ->
                var highlighted by rememberSaveable { mutableStateOf(key.highlighted) }
                SettingsGroupPage(
                    key.group, actions, onBack = { leave(key) }, highlighted,
                    onHighlightShown = { highlighted = null },
                    onOpenPage = { backStack.add(SettingsKey.Page(it.id)) }
                )
            }
            entry<SettingsKey.Page>(metadata = ExtraPage()) { key ->
                val setting = InsightSetting.all.firstOrNull { it.id == key.setting }
                if (setting != null) actions.page(setting) { leave(key) } else LaunchedEffect(key) { leave(key) }
            }
        }
    )
}

/**
 * Start page of the settings: search and the groups, below them the project. A group with a
 * single entry, like "About", opens the entry right away.
 *
 * @param onOpenGroup opens a group, with the key of an entry found by the search to light up
 */
@Composable
private fun SettingsStart(actions: SettingsActions, onBack: () -> Unit, onOpenGroup: (SettingsGroup, String?) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val onGroup: (SettingsGroup) -> Unit = { group ->
        SettingsCatalog.of(group).singleOrNull()?.let(actions::open) ?: onOpenGroup(group, null)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        PageTopBar(onBack = onBack, title = stringResource(org.totschnig.myexpenses.R.string.settings_label))
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.next_settings_search),
            clearLabel = stringResource(R.string.next_settings_clear_search),
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp)
        )
        if (query.isBlank()) SettingsGroups(onGroup) else SearchResults(query, actions) { entry ->
            // Settings changed on the page of their group are shown there
            if (entry.isNative) {
                onOpenGroup(entry.group, entry.key.toString())
            } else actions.open(entry)
        }
    }
}

private val SettingEntry.isNative
    get() = key is SettingKey.Next || (key as? SettingKey.Pref)?.prefKey in NativeSettings.all

@Composable
private fun SettingsGroups(onGroup: (SettingsGroup) -> Unit) {
    LazyColumn {
        SettingsGroup.Section.entries.forEach { section ->
            if (section == SettingsGroup.Section.PROJECT) {
                item(key = section) {
                    RowDivider(Modifier.padding(top = 8.dp))
                    SectionTitle(
                        stringResource(R.string.next_settings_project),
                        Modifier.padding(horizontal = Spacing.screen - 4.dp)
                    )
                }
            }
            items(SettingsGroup.entries.filter { it.section == section }, key = { it }) { group ->
                IconNavigationRow(
                    icon = group.icon,
                    title = stringResource(group.title),
                    onClick = { onGroup(group) },
                    supportingText = group.summary?.let { stringResource(it) }
                )
            }
        }
    }
}

@Composable
private fun SearchResults(query: String, actions: SettingsActions, onResult: (SettingEntry) -> Unit) {
    val groupTitles = SettingsGroup.entries.associateWith { stringResource(it.title) }
    val sectionTitles = SettingsSection.entries.associateWith { stringResource(it.title) }
    val place: (SettingEntry) -> String = { entry ->
        listOfNotNull(groupTitles.getValue(entry.group), entry.section?.let(sectionTitles::getValue)).joinToString(" › ")
    }
    val details = remember(actions) { SettingsCatalog.entries.associateWith(actions::details) }
    val results = searchSettings(query, SettingsCatalog.entries, actions::title, place, details::getValue)
        .filter { actions.isVisible(it) && actions.canOpen(it) }
    if (results.isEmpty()) {
        EmptyState(stringResource(R.string.next_settings_no_results), modifier = Modifier.fillMaxSize())
        return
    }
    LazyColumn {
        items(results, key = { it.key.toString() }) { entry ->
            IconNavigationRow(
                icon = entry.group.icon,
                title = actions.title(entry),
                onClick = { onResult(entry) },
                supportingText = place(entry)
            )
        }
    }
}

/**
 * The settings of one group: changed right here, or opening the old screen that holds them. A
 * group with sections shows each in a card of its own below its title.
 *
 * @param highlighted key of the entry found by the search, scrolled to and lit up
 */
@Composable
private fun SettingsGroupPage(
    group: SettingsGroup,
    actions: SettingsActions,
    onBack: () -> Unit,
    highlighted: String?,
    onHighlightShown: () -> Unit,
    onOpenPage: (InsightSetting) -> Unit,
) {
    var editing by remember { mutableStateOf<Pair<String, SettingControl>?>(null) }
    // The value of its own of a choice, typed instead of picking an option
    var editingCustom by remember { mutableStateOf<Pair<String, SettingControl.Choice>?>(null) }
    val onEdit: (String, SettingControl) -> Unit = { title, control ->
        if (control is SettingControl.Page) onOpenPage(control.setting) else editing = title to control
    }
    DetailScaffold(onBack = onBack, title = stringResource(group.title)) {
        SettingsCatalog.of(group).filter(actions::isVisible).groupBy { it.section }.forEach { (section, entries) ->
            if (section == null) {
                DetailCard(Modifier.padding(top = 8.dp)) { SettingRows(entries, actions, onEdit, highlighted, onHighlightShown) }
            } else {
                SectionTitle(stringResource(section.title))
                DetailCard { SettingRows(entries, actions, onEdit, highlighted, onHighlightShown) }
            }
        }
    }
    editing?.let { (title, control) ->
        val close = { editing = null }
        when (control) {
            is SettingControl.Choice -> SingleChoiceDialog(
                title = title,
                options = control.options,
                selected = control.options.firstOrNull { it.first == control.selected },
                label = { it.second },
                onSelect = { close(); control.onSelect(it.first) },
                onDismiss = close,
                intro = control.explanation,
                footer = control.custom?.let {
                    {
                        DialogActionRow(Icons.Default.Edit, stringResource(R.string.next_settings_custom_format)) {
                            close()
                            editingCustom = title to control
                        }
                    }
                }
            )
            is SettingControl.Text -> TextDialog(
                title = title,
                initialText = control.value,
                onConfirm = { close(); control.onChange(it) },
                onDismiss = close,
                keyboardType = if (control.isNumber) KeyboardType.Number else KeyboardType.Text,
                allowBlank = !control.isNumber,
                isPassword = control.isPassword,
                supportingText = control.summary
            )
            is SettingControl.MultiChoice -> {
                var picked by remember { mutableStateOf(control.selected) }
                MultiChoiceSheet(
                    title = title,
                    options = control.options,
                    selected = control.options.filter { it.first in picked }.toSet(),
                    onToggle = { (value) -> picked = if (value in picked) picked - value else picked + value },
                    label = { it.second },
                    onConfirm = { close(); control.onChange(picked) },
                    onDismiss = close,
                    confirmEnabled = picked.isNotEmpty()
                )
            }
            is SettingControl.Time -> TimeDialog(
                title = title,
                hour = control.hour,
                minute = control.minute,
                onConfirm = { hour, minute -> close(); control.onChange(hour, minute) },
                onDismiss = close
            )
            else -> close()
        }
    }
    editingCustom?.let { (title, control) ->
        val custom = control.custom ?: return@let
        val close = { editingCustom = null }
        val resources = LocalResources.current
        TextDialog(
            title = title,
            initialText = control.selected.orEmpty(),
            onConfirm = { close(); control.onSelect(it) },
            onDismiss = close,
            intro = custom.legend,
            preview = { text -> custom.preview(text)?.let { resources.getString(R.string.next_settings_example, it) } },
            error = custom.error
        )
    }
}

@Composable
private fun SettingRows(
    entries: List<SettingEntry>,
    actions: SettingsActions,
    onEdit: (String, SettingControl) -> Unit,
    highlighted: String?,
    onHighlightShown: () -> Unit,
) {
    entries.forEachIndexed { index, entry ->
        Highlighted(isActive = entry.key.toString() == highlighted, onShown = onHighlightShown) {
            SettingRow(entry, actions, onEdit, isFirst = index == 0)
        }
    }
}

@Composable
private fun SettingRow(entry: SettingEntry, actions: SettingsActions, onEdit: (String, SettingControl) -> Unit, isFirst: Boolean) {
    val title = actions.title(entry)
    when (val control = actions.control(entry)) {
        is SettingControl.Switch -> SwitchRow(
            title, control.checked, control.onChange,
            supportingText = control.summary, isFirst = isFirst, enabled = control.isEnabled
        )
        is SettingControl.Slider -> SliderRow(
            title, control.value.toFloat(), { control.onChange(it.roundToInt()) },
            valueRange = control.range.first.toFloat()..control.range.last.toFloat(),
            valueText = { control.valueText(it.roundToInt()) },
            isFirst = isFirst,
            steps = if (control.isStepped) control.range.last - control.range.first - 1 else 0,
            enabled = control.isEnabled,
            preview = control.sampleScale?.let { scale -> { value -> FontSample(scale(value.roundToInt())) } }
        )
        is SettingControl.MultiChoice -> ValueRow(
            title, control.options.filter { it.first in control.selected }.joinToString { it.second },
            onClick = { onEdit(title, control) }, isFirst = isFirst, enabled = control.isEnabled
        )
        is SettingControl.Choice -> ValueRow(
            title, control.selectedLabel,
            onClick = { onEdit(title, control) }, isFirst = isFirst, enabled = control.isEnabled
        )
        is SettingControl.Text -> ValueRow(
            title,
            if (control.isPassword) control.value.replace(Regex("."), "•") else control.value,
            onClick = { onEdit(title, control) },
            isFirst = isFirst, enabled = control.isEnabled
        )
        is SettingControl.Time -> ValueRow(
            title, control.text, onClick = { onEdit(title, control) },
            isFirst = isFirst, enabled = control.isEnabled
        )
        is SettingControl.Info -> HintRow(control.text ?: title, isFirst = isFirst)
        is SettingControl.Action -> ValueRow(
            title, control.value ?: actions.explanation(entry), onClick = control.onClick, isFirst = isFirst, enabled = control.isEnabled
        )
        is SettingControl.Page -> ValueRow(
            title, control.value ?: actions.explanation(entry), onClick = { onEdit(title, control) },
            isFirst = isFirst, enabled = control.isEnabled
        )
        null -> {
            val canOpen = actions.canOpen(entry)
            ValueRow(
                label = title,
                value = if (canOpen) actions.explanation(entry) else stringResource(R.string.next_coming_soon),
                onClick = { actions.open(entry) },
                isFirst = isFirst,
                enabled = canOpen
            )
        }
    }
}

/** A line of text in a font size, scaled from the size the app uses now */
@Composable
private fun FontSample(scale: Float) {
    val style = MaterialTheme.typography.bodyLarge
    Text(stringResource(R.string.next_settings_font_sample), style = style.copy(fontSize = style.fontSize * scale))
}
