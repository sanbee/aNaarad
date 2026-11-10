/*
 * Copyright (C) 2026 aNaarad Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should obtain a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package naarad.client.tabhost

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sanbee.anaarad.R

class MainActivity : FragmentActivity(), NaaradPlotFragment.nPlotDataArrivalListener {
    var pageAdapter: NaaradPagerAdapter? = null
    private var mViewPager: ViewPager2? = null
    private var mTabLayout: TabLayout? = null
    private var gControlFragment: NaaradControlFragment? = null
    private var b1: TextView? = null
    private var gUIHandler: Handler? = null
    private var gRunnableBlinker: mRunnable? = null

    internal inner class mRunnable : Runnable {
        var theText: String? = null
        var theColor: Int = 0

        fun setText(thisText: String?, thisColor: Int) {
            theText = thisText
            theColor = thisColor
        }

        override fun run() {
            val tab = mTabLayout?.getTabAt(1) ?: return
            val label = theText ?: "Sensors"
            if (theColor == Color.GREEN) {
                val spannable = SpannableString(label)
                spannable.setSpan(ForegroundColorSpan(Color.GREEN), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                tab.text = spannable
            } else {
                tab.text = label.trim()
            }
        }
    }

    private var isBlinking = false

    override fun onDataArrival(json: String?) {
        if (json != null && gControlFragment != null) {
            b1 = gControlFragment!!.setSensorValues(json)
        }

        if (!isBlinking) {
            isBlinking = true
            gRunnableBlinker?.setText("Sensors", Color.GREEN)
            gUIHandler?.let { handler ->
                gRunnableBlinker?.let { runnable ->
                    handler.post(runnable)
                    handler.postDelayed({
                        runnable.setText("Sensors", Color.WHITE)
                        handler.post(runnable)
                        isBlinking = false
                    }, 250)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val myApp = application as? NaaradApp
        if (myApp != null && mViewPager != null) {
            mViewPager!!.isUserInputEnabled = myApp.swipeState
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mViewPager = findViewById(R.id.view_pager)
        mTabLayout = findViewById(R.id.tab_layout)

        val controlFragment = NaaradControlFragment.newInstance("Naarad Control Fragment")
        gControlFragment = controlFragment
        val plotFragment = NaaradPlotFragment.newInstanceNPF("Naarad Plot Fragment")
        val settingFragment = NaaradSettingFragment.newInstanceNSF("Naarad Setting Fragment")

        pageAdapter = NaaradPagerAdapter(this, controlFragment, plotFragment, settingFragment)
        mViewPager!!.adapter = pageAdapter
        mViewPager!!.offscreenPageLimit = 2

        val myApp = application as? NaaradApp
        if (myApp != null && mViewPager != null) {
            myApp.onSwipeStateChanged = { enabled ->
                mViewPager?.isUserInputEnabled = enabled
            }
            mViewPager!!.isUserInputEnabled = myApp.swipeState
        }

        TabLayoutMediator(mTabLayout!!, mViewPager!!) { tab, position ->
            tab.text = when (position) {
                0 -> "Controls"
                1 -> "Sensors"
                2 -> "Settings"
                else -> ""
            }
        }.attach()

        gUIHandler = Handler(Looper.getMainLooper())
        gRunnableBlinker = mRunnable()
    }
}
