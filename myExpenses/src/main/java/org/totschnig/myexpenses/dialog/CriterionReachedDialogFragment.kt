package org.totschnig.myexpenses.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutModel
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.ui.calcProgressVisualRepresentation
import org.totschnig.myexpenses.util.ui.forCompose

class CriterionReachedDialogFragment: ComposeBaseDialogFragment3() {
    @Composable
    override fun ColumnScope.MainContent() {
        Column {
            Text("Warning")
            DonutProgress(
                modifier = Modifier.fillMaxWidth().height(240.dp).width(240.dp),
                model = DonutModel(
                    cap = 100f,
                    sections =  calcProgressVisualRepresentation(120).forCompose(
                        valueColor = Color.Green, excessColor = Color.Red
                    )
                )
            )
            Text("You have reached your credit limit")
        }
    }

    override val title: CharSequence
        get() = getString(R.string.credit_limit)

}