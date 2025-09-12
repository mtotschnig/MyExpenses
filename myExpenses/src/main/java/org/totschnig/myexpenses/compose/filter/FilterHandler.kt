package org.totschnig.myexpenses.compose.filter

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PickCategoryContract
import org.totschnig.myexpenses.activity.PickPayeeContract
import org.totschnig.myexpenses.activity.PickTagContract
import org.totschnig.myexpenses.dialog.AmountFilterDialog
import org.totschnig.myexpenses.dialog.DateFilterDialog
import org.totschnig.myexpenses.dialog.KEY_RESULT_FILTER
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMethodDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMultipleAccountDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransferAccountDialogFragment
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.AmountCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.NotCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.provider.filter.TransferCriterion
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import kotlin.reflect.KClass

interface FilterHandlerScope {
    fun handleAmountEdit(amountCriterion: AmountCriterion?)
    fun handleCrStatusEdit(crStCriterion: CrStatusCriterion?)
    fun handleDateEdit(dateCriterion: DateCriterion?)
    fun handleAccountEdit(accountCriterion: AccountCriterion?)
    fun handleCategoryEdit(categoryCriterion: CategoryCriterion?)
    fun handleMethodEdit(methodCriterion: MethodCriterion?)
    fun handlePayeeEdit(payeeCriterion: PayeeCriterion?)
    fun handleTagEdit(tagCriterion: TagCriterion?)
    fun handleTransferEdit(transferCriterion: TransferCriterion?)
    fun handleCommentEdit(commentCriterion: CommentCriterion?)

    fun handleEdit(criterion: Criterion) {
        when (criterion) {
            is NotCriterion -> handleEdit(criterion.criterion)
            is AccountCriterion -> handleAccountEdit(criterion)
            is AmountCriterion -> handleAmountEdit(criterion)
            is CategoryCriterion -> handleCategoryEdit(criterion)
            is CommentCriterion -> handleCommentEdit(criterion)
            is CrStatusCriterion -> handleCrStatusEdit(criterion)
            is DateCriterion -> handleDateEdit(criterion)
            is MethodCriterion -> handleMethodEdit(criterion)
            is PayeeCriterion -> handlePayeeEdit(criterion)
            is TagCriterion -> handleTagEdit(criterion)
            is TransferCriterion -> handleTransferEdit(criterion)
            else -> throw IllegalStateException("Nested complex not supported")
        }
    }

    fun handleEdit(clazz: KClass<out SimpleCriterion<*>>) {
        when(clazz) {
            AccountCriterion::class -> handleAccountEdit(null)
            AmountCriterion::class -> handleAmountEdit(null)
            CategoryCriterion::class -> handleCategoryEdit(null)
            CommentCriterion::class -> handleCommentEdit(null)
            CrStatusCriterion::class -> handleCrStatusEdit(null)
            DateCriterion::class -> handleDateEdit(null)
            MethodCriterion::class -> handleMethodEdit(null)
            PayeeCriterion::class -> handlePayeeEdit(null)
            TagCriterion::class -> handleTagEdit(null)
            TransferCriterion::class -> handleTransferEdit(null)
        }
    }
}

@Composable
fun FilterHandler(
    account: BaseAccount,
    requestKey: String,
    onResult: (SimpleCriterion<*>?, SimpleCriterion<*>?) -> Unit,
    content: @Composable FilterHandlerScope.() -> Unit
) {
    val currentEdit: MutableState<SimpleCriterion<*>?> = rememberSaveable {
        mutableStateOf(null)
    }

    val getCategory = rememberLauncherForActivityResult(PickCategoryContract()) {
        onResult(currentEdit.value, it)
    }
    val getPayee = rememberLauncherForActivityResult(PickPayeeContract()) {
        onResult(currentEdit.value, it)
    }
    val getTags = rememberLauncherForActivityResult(PickTagContract()) {
        onResult(currentEdit.value, it)
    }

    var showCommentFilterPrompt by rememberSaveable { mutableStateOf<CommentCriterion?>(null) }

    val activity = LocalActivity.current as FragmentActivity

    val supportFragmentManager = activity.supportFragmentManager
    DisposableEffect(onResult) {
        supportFragmentManager.setFragmentResultListener(
            requestKey, activity
        ) { _, result ->
            onResult(
                currentEdit.value,
                BundleCompat.getParcelable(
                    result,
                    KEY_RESULT_FILTER,
                    SimpleCriterion::class.java
                )
            )
        }
        onDispose {
            supportFragmentManager.clearFragmentResultListener(requestKey)
        }
    }
    val handler = object: FilterHandlerScope {

        override fun handleEdit(criterion: Criterion) {
            currentEdit.value = criterion as? SimpleCriterion<*>
            super.handleEdit(criterion)
        }

        override fun handleAmountEdit(amountCriterion: AmountCriterion?) {
            AmountFilterDialog.newInstance(requestKey,
                account.currencyUnit, amountCriterion
            ).show(activity.supportFragmentManager, "AMOUNT_FILTER")
        }

        override fun handleCommentEdit(commentCriterion: CommentCriterion?) {
            showCommentFilterPrompt = commentCriterion ?: CommentCriterion("")
        }

        override fun handleCrStatusEdit(crStCriterion: CrStatusCriterion?) {
            SelectCrStatusDialogFragment.newInstance(requestKey, crStCriterion)
                .show(activity.supportFragmentManager, "STATUS_FILTER")
        }

        override fun handleDateEdit(dateCriterion: DateCriterion?) {
            DateFilterDialog.newInstance(requestKey, dateCriterion)
                .show(activity.supportFragmentManager, "DATE_FILTER")
        }

        override fun handleAccountEdit(accountCriterion: AccountCriterion?) {
            SelectMultipleAccountDialogFragment.newInstance(
                requestKey,
                if (account.isHomeAggregate) null else account.currency,
                accountCriterion
            )
                .show(activity.supportFragmentManager, "ACCOUNT_FILTER")
        }

        override fun handleCategoryEdit(categoryCriterion: CategoryCriterion?) {
            getCategory.launch(account.id to categoryCriterion)
        }

        override fun handleMethodEdit(methodCriterion: MethodCriterion?) {
            SelectMethodDialogFragment.newInstance(
                requestKey,
                account.id, methodCriterion
            ).show(activity.supportFragmentManager, "METHOD_FILTER")
        }

        override fun handlePayeeEdit(payeeCriterion: PayeeCriterion?) {
            getPayee.launch(account.id to payeeCriterion)
        }

        override fun handleTagEdit(tagCriterion: TagCriterion?) {
            getTags.launch(account.id to tagCriterion)
        }

        override fun handleTransferEdit(transferCriterion: TransferCriterion?) {
            SelectTransferAccountDialogFragment.newInstance(requestKey,account.id, transferCriterion)
                .show(activity.supportFragmentManager, "TRANSFER_FILTER")
        }
    }

    handler.content()

    showCommentFilterPrompt?.let { prompt ->
        var search by rememberSaveable { mutableStateOf(prompt.searchString) }

        AlertDialog(
            onDismissRequest = {
                showCommentFilterPrompt = null
                onResult(currentEdit.value, null)
            },
            confirmButton = {
                Button(onClick = {
                    onResult(currentEdit.value, CommentCriterion(search?.takeIf { it.isNotBlank() }))
                    showCommentFilterPrompt = null
                }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            text = {
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                OutlinedTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                keyboardController?.show()
                            }
                        },
                    value = search ?: "",
                    onValueChange = {
                        search = it
                    },
                    label = { Text(text = stringResource(R.string.search_comment)) },
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        )
    }
}





