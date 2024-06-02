package org.totschnig.myexpenses.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//https://gist.github.com/ntoskrnl/14622d88cad3387d14786aa259e4653f
@Composable
fun Initials(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val parts = name.split(' ')
        val firstName = parts[0]
        val lastName = if (parts.size > 1) parts.last() else null
        val initials = (firstName.take(1) + (lastName?.take(1) ?: "")).uppercase()
        val color = MaterialTheme.colorScheme.surface
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(SolidColor(color))
        }
        Text(text = initials, style = textStyle)
    }
}

@Preview
@Composable
private fun InitialsPreview() {
    Row {
        Initials("Jane Foe")
        Initials("Random")
        Initials("Primus Tertius Doctor")
    }
}