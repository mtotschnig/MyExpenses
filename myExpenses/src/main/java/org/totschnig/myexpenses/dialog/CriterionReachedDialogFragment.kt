package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutConfig
import app.futured.donut.compose.data.DonutModel
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.AmountInput
import eltos.simpledialogfragment.form.AmountInputHostDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.ANIMATION_DURATION
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment.Companion.ANIMATION_DURATION_L
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.getLocale
import org.totschnig.myexpenses.util.ui.DisplayProgress
import org.totschnig.myexpenses.viewmodel.CriterionViewModel
import timber.log.Timber
import java.math.BigDecimal
import java.text.NumberFormat
import kotlin.math.absoluteValue
import kotlin.math.sign

interface OnCriterionDialogDismissedListener {
    fun onCriterionDialogDismissed()
}

@Parcelize
data class CriterionInfo(
    val accountId: Long,
    val startBalance: Long,
    val criterion: Long,
    val transaction: Long,
    val accountColor: Int,
    val currency: CurrencyUnit,
    val accountLabel: String,
    val isSealed: Boolean
) : Parcelable {

    @IgnoredOnParcel
    val newBalance = startBalance + transaction

    @IgnoredOnParcel
    val startProgress = (startBalance * 100F / criterion).coerceAtLeast(0f)

    @IgnoredOnParcel
    val endProgress = newBalance * 100f / criterion

    @IgnoredOnParcel
    val delta = newBalance - criterion

    @IgnoredOnParcel
    val deltaPercent = delta.toFloat() / criterion

    val Long.asMoney: Money
        get() = Money(currency, this)

    /**
     * returns true if we are above the criterion, but if [onlyWhenChanged] is true only if startBalance was still below criterion
     */
    fun hasReached(onlyWhenChanged: Boolean = true) =
        criterion.sign == newBalance.sign && newBalance.absoluteValue >= criterion.absoluteValue &&
                (!onlyWhenChanged || (startBalance * criterion.sign < criterion * criterion.sign))

    @IgnoredOnParcel
    val isSavingGoal = criterion.sign > 0

    @IgnoredOnParcel
    val label: Int = if (isSavingGoal) R.string.saving_goal else R.string.credit_limit

    @IgnoredOnParcel
    val dialogTitle: Int = if (isSavingGoal) when {
        newBalance < criterion -> R.string.saving_goal
        newBalance == criterion -> R.string.saving_goal_reached
        else -> R.string.saving_goal_exceeded
    } else when {
        newBalance > criterion -> R.string.credit_limit
        newBalance == criterion -> R.string.credit_limit_reached
        else -> R.string.credit_limit_exceeded
    }
}

class CriterionReachedDialogFragment() : ComposeBaseDialogFragment3(), OnDialogResultListener {

    val viewModel: CriterionViewModel by viewModels()

    val withProgressAnimation: MutableState<Boolean> by lazy {
        mutableStateOf(requireArguments().getBoolean(KEY_WITH_ANIMATION))
    }

    val progressColor
        get() = Color(viewModel.info.accountColor)

    @get:Composable
    val overageColor: Color
        get() = with(LocalColors.current) {
            if (viewModel.info.criterion > 0) income else expense
        }

    @Composable
    override fun ColumnScope.MainContent() {
        viewModel.infoLiveData.observeAsState().value?.let { info ->
            CriterionReachedGraph(
                info,
                progressColor = progressColor,
                overageColor = overageColor,
                withProgressAnimation = withProgressAnimation,
                onEdit = with(info) {
                    if (isSealed) null else {
                        {
                            AmountInputHostDialog.build().title(label)
                                .fields(
                                    AmountInput.plain(KEY_AMOUNT)
                                        .label(label)
                                        .fractionDigits(currency.fractionDigits)
                                        .amount(criterion.asMoney.amountMajor)
                                )
                                .neg()
                                .show(this@CriterionReachedDialogFragment, DIALOG_TAG_NEW_CRITERION)
                        }
                    }
                }
            ) {
                dismiss()
                onFinished()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
    }

    override fun onStart() {
        super.onStart()
        requireArguments().getString(KEY_ADDITIONAL_MESSAGE)?.let {
            showSnackBar(it)
        }
    }

    companion object {
        const val ANIMATION_DURATION = 1000
        const val ANIMATION_DURATION_L = ANIMATION_DURATION.toLong()
        const val KEY_INFO = "info"
        private const val KEY_ADDITIONAL_MESSAGE = "additionalMessage"
        private const val KEY_WITH_ANIMATION = "withAnimation"
        const val DIALOG_TAG_NEW_CRITERION = "NEW_CRITERION"

        fun newInstance(
            criterionInfo: CriterionInfo,
            additionalMessage: String? = null,
            withAnimation: Boolean = true,
        ) =
            CriterionReachedDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_INFO, criterionInfo)
                    additionalMessage?.let {
                        putString(KEY_ADDITIONAL_MESSAGE, it)
                    }
                    putBoolean(KEY_WITH_ANIMATION, withAnimation)
                }
            }
    }

    override fun onCancel(dialog: DialogInterface) {
        onFinished()
    }

    fun onFinished() {
        (requireActivity() as? OnCriterionDialogDismissedListener)?.onCriterionDialogDismissed()
    }

    override fun onResult(
        dialogTag: String,
        which: Int,
        extras: Bundle,
    ) = if (dialogTag == DIALOG_TAG_NEW_CRITERION) {
        BundleCompat.getSerializable(extras, KEY_AMOUNT, BigDecimal::class.java)?.let {
            with(viewModel.info) {
                if (it.compareTo(BigDecimal.ZERO) == 0) {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.required) + ": " + getString(label),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val newCriterion = Money(currency, it).amountMinor * criterion.sign
                    if (criterion.absoluteValue != newCriterion) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                viewModel.updateCriterion(accountId, newCriterion)
                            }
                            viewModel.info = copy(
                                startBalance = newBalance,
                                transaction = 0,
                                criterion = newCriterion
                            )
                        }
                    }
                }
            }
        }
        true
    } else false
}

@Composable
fun GraphInternal(
    modifier: Modifier = Modifier,
    progress: Float,
    progressColor: Color,
    overageColor: Color,
    remainderColor: Color,
) {
    Timber.i("progress: %f", progress)
    DonutProgress(
        modifier = modifier,
        model = DonutModel(
            cap = 100f,
            sections = DisplayProgress.calcProgressVisualRepresentation(progress).forCompose(
                progressColor,
                overageColor
            ).also { Timber.d("sections: $it") },
            gapWidthDegrees = 0f,
            gapAngleDegrees = 0f,
            strokeCap = StrokeCap.Butt,
            backgroundLineColor = remainderColor
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
fun CriterionReachedGraph(
    info: CriterionInfo,
    showDataInitially: Boolean = false,
    progressColor: Color = Color.Green,
    overageColor: Color = Color.Red,
    withProgressAnimation: MutableState<Boolean>,
    onEdit: (() -> Unit)? = {},
    onDismiss: () -> Unit = {},
) {
    var progress by remember(info) { mutableFloatStateOf(info.startProgress) }
    var showData by remember { mutableStateOf(showDataInitially) }
    val iconAnimation = remember { mutableStateOf(false) }
    val isLarge = booleanResource(R.bool.isLarge)
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val remainderColor = MaterialTheme.colorScheme.outline
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Text(
            text = stringResource(info.dialogTitle),
            style = MaterialTheme.typography.titleMedium
        )

        if (isLandscape) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isLarge) 16.dp else 8.dp)
            ) {
                GraphInternal(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    progress, progressColor, overageColor, remainderColor
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    DataOverview(
                        info,
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        showData,
                        progressColor,
                        overageColor,
                        remainderColor,
                        iconAnimation.value
                    )
                    Buttons(onEdit, onDismiss)
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
                    Modifier.fillMaxSize(), progress, progressColor, overageColor, remainderColor
                )
                DataOverview(
                    info,
                    Modifier
                        .fillMaxWidth(0.75f)
                        .align(Alignment.TopCenter),
                    showData,
                    progressColor,
                    overageColor,
                    remainderColor,
                    iconAnimation.value
                )
            }
            Buttons(onEdit, onDismiss)
        }
    }
    LaunchedEffect(Unit) {
        if (withProgressAnimation.value) {
            delay(ANIMATION_DURATION_L / 2)
            if (progress < 100F) {
                progress = 100F
                delay(ANIMATION_DURATION_L + 50) // extra time to make sure Donut's internal animation has finished
            }
            progress = info.endProgress
        }
        showData = true
        delay(ANIMATION_DURATION_L)
        iconAnimation.value = true
        withProgressAnimation.value = false
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
    remainderColor: Color,
    iconAnimation: Boolean,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = showData,
        enter = fadeIn(animationSpec = tween(durationMillis = ANIMATION_DURATION))
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
            Column {
                val hasReached = info.hasReached(false)
                Spacer(Modifier.weight(if (hasReached) 0.7f else 1f))
                val savingGoal = info.isSavingGoal
                if (hasReached) {
                    val image = AnimatedImageVector.animatedVectorResource(
                        if (savingGoal) R.drawable.heart else R.drawable.notification_v4
                    )
                    Icon(
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp),
                        painter = rememberAnimatedVectorPainter(image, iconAnimation),
                        tint = colorResource(if (savingGoal) R.color.colorIncome else R.color.colorExpense),
                        contentDescription = null // decorative element
                    )
                }

                if (hasReached) {
                    info.RenderCriterion(progressColor)
                } else {
                    info.RenderBalance(progressColor)
                }

                ValueRow(
                    if (hasReached) overageColor else remainderColor,
                    stringResource(
                        when {
                            hasReached -> R.string.overage
                            savingGoal -> R.string.saving_goal_short_fall
                            else -> R.string.credit_limit_available_credit
                        }
                    ),
                    with(info) { delta.asMoney },
                    info.deltaPercent.absoluteValue
                )

                if (hasReached) {
                    info.RenderBalance(progressColor)
                } else {
                    info.RenderCriterion(progressColor)
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CriterionInfo.RenderCriterion(progressColor: Color) {
    ValueRow(
        progressColor.takeIf { hasReached(false) },
        stringResource(label),
        criterion.asMoney
    )
}

@Composable
fun CriterionInfo.RenderBalance(progressColor: Color) {
    ValueRow(
        progressColor.takeIf { !hasReached(false) },
        stringResource(R.string.current_balance),
        newBalance.asMoney
    )
}

@Composable
fun ValueRow(
    color: Color?,
    label: String,
    amount: Money,
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
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = 1),
            text = label,
            maxLines = 1
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(text = currencyFormatter.formatMoney(amount))
            percent?.let { Text(text = percentFormat.format(it)) }
        }
    }
}

@Composable
fun Circle(radius: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier then Modifier
            .size(radius, radius)
            .drawBehind {
                drawCircle(color, radius.toPx(), size.center)
            })
}

@Composable
fun Buttons(
    onEdit: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    ButtonRow {
        if (onEdit != null) {
            TextButton(onClick = onEdit) {
                Text(stringResource(id = R.string.menu_edit))
            }
        }
        TextButton(onClick = onDismiss) {
            Text(stringResource(id = R.string.menu_close))
        }
    }
}

@Preview(name = "landscape", device = "spec:width=640dp,height=360dp,dpi=480")
@Preview(name = "portrait", device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun Demo() {

    Column {
        CriterionReachedGraph(
            CriterionInfo(
                accountId = 1L,
                startBalance = 95,
                criterion = 100,
                transaction = 45,
                android.graphics.Color.GREEN,
                CurrencyUnit.DebugInstance,
                "My savings account",
                false
            ),
            showDataInitially = true,
            withProgressAnimation = remember { mutableStateOf(true) }
        )
    }
}