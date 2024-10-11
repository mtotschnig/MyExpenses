package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutConfig
import app.futured.donut.compose.data.DonutModel
import kotlinx.coroutines.delay
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.ANIMATION_DURATION
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.ANIMATION_DURATION_L
import org.totschnig.myexpenses.util.ui.calcProgressVisualRepresentation
import org.totschnig.myexpenses.util.ui.forCompose
import kotlin.math.absoluteValue
import kotlin.math.sign

@Parcelize
data class CriterionInfo(
    val startBalance: Long,
    val criterion: Long,
    val transactionAmount: Long,
) : Parcelable {

    @IgnoredOnParcel
    val newBalance = startBalance + transactionAmount

    @IgnoredOnParcel
    val startProgress = startBalance * 100F / criterion

    @IgnoredOnParcel
    val endProgress = newBalance * 100f / criterion

    @IgnoredOnParcel
    val hasReached =
        criterion.sign == newBalance.sign && newBalance.absoluteValue >= criterion.absoluteValue

    @IgnoredOnParcel
    @StringRes
    val title = if(criterion.sign > 0) {
        if (newBalance == criterion) R.string.saving_goal_reached else R.string.saving_goal_exceeded
    } else {
        if (newBalance == criterion) R.string.credit_limit_reached else R.string.credit_limit_exceeded
    }
}

class CriterionReachedDialogFragment private constructor() : ComposeBaseDialogFragment3() {
    val info: CriterionInfo
        get() = BundleCompat.getParcelable(
            requireArguments(), KEY_INFO, CriterionInfo::class.java
        )!!

    @Composable
    override fun ColumnScope.MainContent() {
        CriterionReachedGraph(
            info,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        TextButton(onClick = { dismiss() }, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(id = android.R.string.ok))
        }
    }

    override val title: CharSequence
        get() = getString(info.title)

    companion object {
        const val ANIMATION_DURATION = 1000
        const val ANIMATION_DURATION_L = ANIMATION_DURATION.toLong()
        private const val KEY_INFO = "info"

        fun newInstance(criterionInfo: CriterionInfo) =
            CriterionReachedDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_INFO, criterionInfo)
                }
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finish()
    }
}

@Composable
fun CriterionReachedGraph(
    info: CriterionInfo,
    modifier: Modifier = Modifier
) {
    val progress = remember { mutableFloatStateOf(info.startProgress) }
    DonutProgress(
        modifier = modifier
            .height(240.dp)
            .width(240.dp),
        model = DonutModel(
            cap = 100f,
            sections = calcProgressVisualRepresentation(progress.floatValue).forCompose(
                Color.Green,
                Color.Red
            ),
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
        if (progress.floatValue < 100F) {
            progress.floatValue = 100F
            delay(ANIMATION_DURATION_L)
        }
        progress.floatValue = info.endProgress
    }
}

@Preview(widthDp = 300, heightDp = 300)
@Composable
fun Demo() {
    Box {
        CriterionReachedGraph(
            CriterionInfo(
                startBalance = 95,
                criterion = 100,
                transactionAmount = 10
            ),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}