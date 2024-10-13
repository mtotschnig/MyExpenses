package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutConfig
import app.futured.donut.compose.data.DonutModel
import kotlinx.coroutines.delay
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.ANIMATION_DURATION
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.ANIMATION_DURATION_L
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.getLocale
import org.totschnig.myexpenses.util.ui.calcProgressVisualRepresentation
import org.totschnig.myexpenses.util.ui.forCompose
import timber.log.Timber
import java.text.NumberFormat
import kotlin.math.absoluteValue
import kotlin.math.sign

@Parcelize
data class CriterionInfo(
    val startBalance: Long,
    val criterion: Long,
    val transactionAmount: Long,
    val accountColor: Int,
    val currency: CurrencyUnit,
) : Parcelable {

    @IgnoredOnParcel
    val newBalance = startBalance + transactionAmount

    @IgnoredOnParcel
    val startProgress = startBalance * 100F / criterion

    @IgnoredOnParcel
    val endProgress = newBalance * 100f / criterion

    @IgnoredOnParcel
    val overage = newBalance - criterion

    @IgnoredOnParcel
    val overagePercent = overage.toFloat() / criterion

    @IgnoredOnParcel
    val hasReached =
        criterion.sign == newBalance.sign &&
                (startBalance.absoluteValue < criterion.absoluteValue || BuildConfig.DEBUG) &&
                newBalance.absoluteValue >= criterion.absoluteValue

    @IgnoredOnParcel
    @StringRes
    val title = if (criterion.sign > 0) {
        if (newBalance == criterion) R.string.saving_goal_reached else R.string.saving_goal_exceeded
    } else {
        if (newBalance == criterion) R.string.credit_limit_reached else R.string.credit_limit_exceeded
    }
}

class CriterionReachedDialogFragment() : ComposeBaseDialogFragment3() {
    val info: CriterionInfo by lazy {
        requireNotNull(
            BundleCompat.getParcelable(
                requireArguments(), KEY_INFO, CriterionInfo::class.java
            )
        )
    }

    val progressColor
        get() = Color(info.accountColor)

    @get:Composable
    val overageColor: Color
        get() = with(LocalColors.current) {
            if (info.criterion > 0) income else expense
        }

    @Composable
    override fun ColumnScope.MainContent() {
        CriterionReachedGraph(
            info,
            progressColor = progressColor,
            overageColor = overageColor
        ) {
            dismiss()
            requireActivity().finish()
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

    override fun onCancel(dialog: DialogInterface) {
        requireActivity().finish()
    }
}

@Composable
fun GraphInternal(
    modifier: Modifier = Modifier,
    progress: Float,
    progressColor: Color = Color.Green,
    overageColor: Color = Color.Red,
) {
    DonutProgress(
        modifier = modifier,
        model = DonutModel(
            cap = 100f,
            sections = calcProgressVisualRepresentation(progress).forCompose(
                progressColor,
                overageColor
            ).also { Timber.d("sections: $it") },
            gapWidthDegrees = 0f,
            gapAngleDegrees = 0f,
            strokeCap = StrokeCap.Butt,
            backgroundLineColor = Color(0xFFE7E8E9)
        ),
        config = DonutConfig.create(
            layoutAnimationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = CubicBezierEasing(0.18f, 0.7f, 0.16f, 1f)
            ),
            colorAnimationSpec = tween(durationMillis = ANIMATION_DURATION)
        )
    )
}


@Composable
fun ColumnScope.CriterionReachedGraph(
    info: CriterionInfo,
    showDataInitially: Boolean = false,
    progressColor: Color = Color.Green,
    overageColor: Color = Color.Red,
    withAnimation: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    var progress by remember { mutableFloatStateOf(info.startProgress) }
    var showData by remember { mutableStateOf(showDataInitially) }
    val animation = if (withAnimation) remember { mutableStateOf(false) } else null
    val isLarge = booleanResource(R.bool.isLarge)
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isLarge) 16.dp else 8.dp)
        ) {
            GraphInternal(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                progress = progress,
                progressColor = progressColor,
                overageColor = overageColor
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                DataOverview(
                    info,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    showData = showData,
                    progressColor = progressColor,
                    overageColor = overageColor,
                    animation = animation?.value
                )
                OkButton(onDismiss)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isLarge) 0.75f else 1f)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
        ) {
            GraphInternal(
                modifier = Modifier.fillMaxSize(),
                progress = progress,
                progressColor = progressColor,
                overageColor = overageColor
            )
            DataOverview(
                info = info,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .align(Alignment.TopCenter),
                showData = showData,
                progressColor = progressColor,
                overageColor = overageColor,
                animation = animation?.value
            )
        }
        OkButton(onDismiss)
    }
    LaunchedEffect(Unit) {
        delay(ANIMATION_DURATION_L / 2)
        if (progress < 100F) {
            progress = 100F
            delay(ANIMATION_DURATION_L + 50) // extra time to make sure Donut's internal animation has finished
        }
        progress = info.endProgress
        showData = true
        delay(ANIMATION_DURATION_L)
        animation?.value = true
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun DataOverview(
    info: CriterionInfo,
    modifier: Modifier = Modifier,
    showData: Boolean,
    progressColor: Color,
    overageColor: Color,
    animation: Boolean?
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = showData,
        enter = fadeIn(animationSpec = tween(durationMillis = ANIMATION_DURATION))
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
            Column {
                Spacer(Modifier.weight(0.7f))
                val isSavingGoal = info.criterion > 0
                if (animation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val image = AnimatedImageVector.animatedVectorResource(
                        if (isSavingGoal) R.drawable.heart else R.drawable.notification_v4
                    )
                    Icon(
                        modifier = Modifier.size(72.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp),
                        painter = rememberAnimatedVectorPainter(image, animation),
                        tint = colorResource(if (isSavingGoal) R.color.colorIncome else R.color.colorExpense),
                        contentDescription = null // decorative element
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(72.dp).align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
                        imageVector = if (isSavingGoal) Icons.Filled.Celebration else Icons.Filled.Alarm,
                        tint = if (isSavingGoal) LocalColors.current.income else LocalColors.current.expense,
                        contentDescription = null
                    )
                }

                ValueRow(
                    progressColor,
                    stringResource(if (isSavingGoal) R.string.saving_goal else R.string.credit_limit),
                    info.criterion,
                    info.currency
                )
                ValueRow(
                    overageColor,
                    "Overage",
                    info.overage,
                    info.currency,
                    info.overagePercent
                )
                ValueRow(
                    null,
                    stringResource(R.string.new_balance),
                    info.newBalance,
                    info.currency
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ValueRow(
    color: Color?,
    label: String,
    amount: Long,
    currency: CurrencyUnit,
    percent: Float? = null,
) {
    val percentFormat = NumberFormat.getPercentInstance(LocalContext.current.getLocale()).also {
        it.setMinimumFractionDigits(2)
    }
    val currencyFormatter = LocalCurrencyFormatter.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        color?.let { Circle(radius = 4.dp, color = it) }
            ?: Spacer(modifier = Modifier.width(width = 4.dp))
        Spacer(modifier = Modifier.width(width = 12.dp))
        Text(
            modifier = Modifier.weight(1f).basicMarquee(iterations = 1),
            text = label,
            maxLines = 1
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(text = currencyFormatter.formatMoney(Money(currency, amount)))
            percent?.let { Text(text = percentFormat.format(it)) }
        }
    }
}

@Composable
fun Circle(radius: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier then Modifier
        .size(radius, radius)
        .drawBehind {
            drawCircle(color, radius.toPx(), size.center)
        })
}

@Composable
fun ColumnScope.OkButton(onDismiss: () -> Unit) {
    TextButton(
        onClick = onDismiss, modifier = Modifier.align(Alignment.End)
    ) {
        Text(stringResource(id = android.R.string.ok))
    }
}

@Preview(name = "landscape", device = "spec:width=640dp,height=360dp,dpi=480")
@Preview(name = "portrait", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun Demo() {

    Column {
        CriterionReachedGraph(
            CriterionInfo(
                startBalance = 95,
                criterion = 100,
                transactionAmount = 45,
                android.graphics.Color.GREEN,
                CurrencyUnit.DebugInstance,
            ),
            showDataInitially = true,
            withAnimation = false
        )
    }
}