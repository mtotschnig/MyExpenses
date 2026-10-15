package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.transactions.InlineChip

class TagStylePreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    init {
        layoutResource = R.layout.preference_tag_style
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val composeView = holder.findViewById(R.id.compose_view) as? ComposeView
        composeView?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    val currentTagStyle = runCatching { value?.let { TagStyle.valueOf(it) } }.getOrNull()
                        ?: TagStyle.OUTLINE
                    TagStylePreviewSelector(
                        selectedStyle = currentTagStyle,
                        onStyleSelected = { newStyle ->
                            if (callChangeListener(newStyle.name)) {
                                value = newStyle.name
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TagStylePreviewSelector(
    selectedStyle: TagStyle,
    onStyleSelected: (TagStyle) -> Unit
) {
    val sampleColor = MaterialTheme.colorScheme.primary

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TagStyleCompactOption(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.pref_tag_style_outline),
            isSelected = selectedStyle == TagStyle.OUTLINE,
            onClick = { onStyleSelected(TagStyle.OUTLINE) },
            style = TagStyle.OUTLINE,
            color = sampleColor
        )

        TagStyleCompactOption(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.pref_tag_style_filled),
            isSelected = selectedStyle == TagStyle.FILLED,
            onClick = { onStyleSelected(TagStyle.FILLED) },
            style = TagStyle.FILLED,
            color = sampleColor
        )
    }
}

@Composable
private fun TagStyleCompactOption(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    style: TagStyle,
    color: Color
) {
    Surface(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            InlineChip(
                text = label,
                color = color,
                tagStyle = style
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun TagStylePreviewSelectorPreview() {
    AppTheme {
        TagStylePreviewSelector(
            selectedStyle = TagStyle.OUTLINE,
            onStyleSelected = {}
        )
    }
}
