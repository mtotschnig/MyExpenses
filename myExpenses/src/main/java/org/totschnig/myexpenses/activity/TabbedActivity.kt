package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import org.totschnig.myexpenses.databinding.ActivityWithTabsBinding
import org.totschnig.myexpenses.ui.FragmentPagerAdapter

abstract class TabbedActivity : ProtectedFragmentActivity() {
    lateinit var binding: ActivityWithTabsBinding

    lateinit var mSectionsPagerAdapter: SectionsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWithTabsBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        setupToolbar()
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = mSectionsPagerAdapter
        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = getTitle(position)
        }.attach()
    }

    abstract fun getTitle(position: Int): CharSequence

    abstract fun createFragment(position: Int): Fragment

    abstract fun getItemCount(): Int

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
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
