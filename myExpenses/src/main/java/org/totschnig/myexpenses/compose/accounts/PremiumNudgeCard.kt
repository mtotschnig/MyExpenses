package org.totschnig.myexpenses.compose.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R

@Composable
fun PremiumNudgeCard(
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val starId = "star"
    val contribId = "contrib"
    val annotatedString = buildAnnotatedString {
        appendInlineContent(starId, "[star]")
        append("  ")
        append(stringResource(R.string.premium_nudge_portfolio_message))
        append("  ")
        appendInlineContent(contribId, "[contrib]")
    }

    val inlineContent = mapOf(
        starId to InlineTextContent(
            Placeholder(
                width = 20.sp,
                height = 20.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        },
        contribId to InlineTextContent(
            Placeholder(
                width = 100.sp,
                height = 20.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Text(
                text = stringResource(R.string.menu_contrib).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.clickable { onUpgrade() }
            )
        }
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = annotatedString,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Preview
@Composable
fun PremiumNudgeCardPreview() {
    PremiumNudgeCard(onUpgrade = {})
}
