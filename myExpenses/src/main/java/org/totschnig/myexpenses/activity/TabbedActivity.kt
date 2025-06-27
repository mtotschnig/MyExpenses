package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import org.totschnig.myexpenses.databinding.ActivityWithTabsBinding

abstract class TabbedActivity : ProtectedFragmentActivity() {
    lateinit var binding: ActivityWithTabsBinding

    lateinit var mSectionsPagerAdapter: SectionsPagerAdapter

    override val scrollsHorizontally = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWithTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.adapter = mSectionsPagerAdapter
        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = getTitle(position)
        }.attach()
        ViewCompat.setOnApplyWindowInsetsListener(binding.appbar) { v, insets ->
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).left
                rightMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).right
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    abstract fun getTitle(position: Int): CharSequence

    abstract fun createFragment(position: Int): Fragment

    abstract fun getItemCount(): Int

    /**
     * A [FragmentStateAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {

        fun getFragmentName(currentPosition: Int): String {
            //https://stackoverflow.com/a/61178226/1199911
            return "f" + getItemId(currentPosition)
        }

        override fun getItemCount() = this@TabbedActivity.getItemCount()

        override fun createFragment(position: Int) = this@TabbedActivity.createFragment(position)
    }

    override val snackBarContainerId: Int
        get() = binding.viewPager.id
}
