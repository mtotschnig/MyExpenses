package org.totschnig.myexpenses.test.espresso

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.internal.matcher.HelperMatchers.menuIdMatcher
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageActivity
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.addDebugAttachment
import org.totschnig.shared_test.TransactionData

class ExpenseEditAttachmentTest : BaseExpenseEditTest() {

    @Before
    fun setupIntents() {
        // Initialize Espresso-Intents before each test runs
        Intents.init()
    }

    @After
    fun tearDownIntents() {
        // Release Espresso-Intents after each test finishes
        Intents.release()
    }

    @Test
    fun shouldSaveAttachments() {
        runTest {
            val resultUri =
                Uri.parse("file:///android_asset/screenshot.jpg")
            val fakeResult = Instrumentation.ActivityResult(Activity.RESULT_OK,
                Intent().apply {

                    putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT, CropImage.ActivityResult(
                        Uri.parse("content://org.totschnig.myexpenses.debug.fileprovider/cache/Scan.jpg"),
                        resultUri,
                        null,
                        null,
                        null,
                        0,
                        null,
                        0
                    ))
                }
            )

            Intents.intending(IntentMatchers.hasComponent(CropImageActivity::class.java.name)).respondWith(fakeResult)

            account1 = buildAccount(ACCOUNT_LABEL_1)
            launch()
            setAmount(101)
            onView(withId(R.id.newAttachment)).perform(click())
            onData(menuIdMatcher(R.id.PHOTO_COMMAND)).perform(click())
            clickFab() // save transaction
            assertTransaction(
                id = repository.loadTransactions(account1.id).first().id,
                TransactionData(
                    accountId = account1.id,
                    amount = -10100,
                    attachments = listOf(resultUri)
                )
            )
        }
    }

    @Test
    fun shouldLoadAttachments() {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        val transaction = repository.insertTransaction(
            accountId = account1.id,
            amount = 100,
            equivalentAmount = 13
        )
        repository.addDebugAttachment(transaction.id)
        launch(getIntentForEditTransaction(transaction.id))
        onView(
            allOf(
                isDescendantOfA(withId(R.id.AttachmentGroup)),
                withContentDescription("screenshot.jpg")
            )
        ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }
}