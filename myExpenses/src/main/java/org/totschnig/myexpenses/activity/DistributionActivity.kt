package org.totschnig.myexpenses.activity

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.viewmodel.CategoryViewModel
import org.totschnig.myexpenses.viewmodel.data.Category2

class DistributionActivity: ProtectedFragmentActivity() {
    private lateinit var binding: ActivityComposeBinding
    val viewModel: CategoryViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        binding.composeView.setContent {
            AppTheme(this) {
                val configuration = LocalConfiguration.current
                when (configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        Row {
                            RenderTree(modifier = Modifier.weight(0.5f))
                            RenderChart(modifier = Modifier.weight(0.5f))
                        }
                    }
                    else -> {
                        Column {
                            RenderTree(modifier = Modifier.weight(0.5f))
                            RenderChart(modifier = Modifier.weight(0.5f))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RenderTree(modifier: Modifier) {
        Category(
            modifier = modifier,
            category = viewModel.categoryTreeWithSum.collectAsState(initial = Category2.EMPTY).value,
            choiceMode = ChoiceMode.NoChoice,
            expansionMode = ExpansionMode.DefaultCollapsed(rememberMutableStateListOf())
        )
    }

    @Composable
    fun RenderChart(modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
            PieChart(ctx).apply {
            }
        })
    }


}