package naarad.client.tabhost

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class NaaradPagerAdapter(
    activity: FragmentActivity,
    val controlFragment: NaaradControlFragment,
    val plotFragment: NaaradPlotFragment,
    val settingFragment: NaaradSettingFragment
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> controlFragment
            1 -> plotFragment
            2 -> settingFragment
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
