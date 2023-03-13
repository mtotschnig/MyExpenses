package org.totschnig.myexpenses.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.OnboardingActivity
import org.totschnig.myexpenses.databinding.OnboardingWizzardBinding
import org.totschnig.myexpenses.preference.PrefHandler
import javax.inject.Inject

abstract class OnboardingFragment : Fragment() {
    private var _binding: OnboardingWizzardBinding? = null
    private val binding get() = _binding!!
    lateinit var nextButton: View
    lateinit var toolbar: Toolbar

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OnboardingWizzardBinding.inflate(inflater, container, false)
        configureNavigation(binding.root, inflater)
        binding.onboardingContent.layoutResource = layoutResId
        bindView(binding.onboardingContent.inflate())
        binding.setupWizardLayout.headerText = title
        binding.setupWizardLayout.setIllustration(
            R.drawable.bg_setup_header,
            R.drawable.bg_header_horizontal_tile
        )
        return binding.root
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        configureView(savedInstanceState)
    }

    protected abstract val title: CharSequence

    private fun configureNavigation(content: View, inflater: LayoutInflater) {
        val navParent =
            content.findViewById<View>(R.id.suw_layout_navigation_bar).parent as ViewGroup
        val customNav = inflater.inflate(R.layout.onboarding_navigation, navParent, false)
        toolbar = ViewCompat.requireViewById(customNav, R.id.onboarding_menu)

        if (menuResId != 0) {
            toolbar.inflateMenu(menuResId)
            setupMenu()
        }
        nextButton = ViewCompat.requireViewById(customNav, navigationButtonId)
        nextButton.setOnClickListener { onNextButtonClicked() }

        // Swap our custom navigation bar into place
        for (i in 0 until navParent.childCount) {
            if (navParent.getChildAt(i).id == R.id.suw_layout_navigation_bar) {
                navParent.removeViewAt(i)
                navParent.addView(customNav, i)
                break
            }
        }
    }

    protected open val navigationButtonId: Int
        @IdRes get() = R.id.suw_navbar_next

    protected abstract fun configureView(savedInstanceState: Bundle?)

    @get:LayoutRes
    protected abstract val layoutResId: Int
    abstract fun bindView(view: View)

    protected open val menuResId: Int
        @MenuRes get() = 0

    protected open fun onNextButtonClicked() {
        hostActivity.navigateNext()
    }

    open fun setupMenu() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    val hostActivity
        get() = requireActivity() as OnboardingActivity
}