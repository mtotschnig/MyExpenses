package org.totschnig.myexpenses.dialog

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutConfig
import app.futured.donut.compose.data.DonutModel
import kotlinx.coroutines.delay
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.ANIMATION_DURATION
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.ANIMATION_DURATION_L
import org.totschnig.myexpenses.util.ui.calcProgressVisualRepresentation
import org.totschnig.myexpenses.util.ui.forCompose
import kotlin.math.absoluteValue

data class CriterionReachedEventInfo(
    val startBalance: Long,
    val criterion: Long,
    val transactionAmount: Long
) {
    init {
        require((startBalance + transactionAmount).absoluteValue >= criterion.absoluteValue)
    }

    val newBalance = startBalance + transactionAmount

    val startProgress = startBalance * 100F / criterion
    val endProgress = newBalance * 100f / criterion
}

class CriterionReachedDialogFragment: ComposeBaseDialogFragment3() {
    val info = CriterionReachedEventInfo(
        startBalance = 65,
        criterion = 100,
        transactionAmount = 75
    )

    @Composable
    override fun ColumnScope.MainContent() {

        Column {
            CriterionReachedGraph(info)
            Text("You have reached your credit limit.")
        }
    }

    override val title: CharSequence
        get() = getString(R.string.credit_limit)

    companion object {
        const val ANIMATION_DURATION = 1000
        const val ANIMATION_DURATION_L = ANIMATION_DURATION.toLong()
    }
}

@Composable
fun CriterionReachedGraph(info: CriterionReachedEventInfo) {
    val progress = remember { mutableFloatStateOf(info.startProgress) }
    DonutProgress(
        modifier = Modifier.height(240.dp).width(240.dp),
        model = DonutModel(
            cap = 100f,
            sections = calcProgressVisualRepresentation(progress.floatValue).forCompose(Color.Green, Color.Red),
            gapWidthDegrees = 0f,
            gapAngleDegrees = 0f,
            strokeCap = StrokeCap.Butt,
            backgroundLineColor = Color(0xFFE7E8E9)
        ),
        config = DonutConfig.create(
            layoutAnimationSpec = tween(
                durationMillis = 1000,
                easing = CubicBezierEasing(0.18f, 0.7f, 0.16f, 1f)
            ),
            colorAnimationSpec = tween(durationMillis = ANIMATION_DURATION)
        )
    )
    LaunchedEffect(Unit) {
        delay(ANIMATION_DURATION_L)
        progress.floatValue = 100F
        delay(ANIMATION_DURATION_L)
        progress.floatValue = info.endProgress
    }
}

@Preview
@Composable
fun Demo() {
    CriterionReachedGraph(CriterionReachedEventInfo(
        startBalance = 95,
        criterion = 100,
        transactionAmount = 10
    ))
}