package naarad.client.tabhost

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import android.view.View
import android.widget.TabHost
import android.widget.TabHost.OnTabChangeListener
import android.widget.TabHost.TabSpec
import android.widget.TextView
import com.sanbee.anaarad.R

/**
 * ---------------------------------------------------------------------------------------Freely inspired by:
 * --------------- http://thepseudocoder.wordpress.com/2011/10/13/android-tabs-viewpager-swipe-able-tabs-ftw/
 */
class MainActivity : FragmentActivity(), OnTabChangeListener, OnPageChangeListener,
    NaaradPlotFragment.nPlotDataArrivalListener {
    var pageAdapter: MyPageAdapter? = null
    private var mViewPager: MyViewPager? = null
    private var mTabHost: TabHost? = null
    private var gControlFragment: NaaradControlFragment? = null
    private var b1: TextView? = null
    private var tab1Label: TextView? = null
    private var gUIHandler: Handler? = null
    private var gRunnableBlinker: mRunnable? = null

    // This class depends on private variables b1 nd tab1Label.
    internal inner class mRunnable : Runnable {
        var theText: String? = null
        var theColor: Int = 0

        fun setText(thisText: String?, thisColor: Int) {
            theText = thisText
            theColor = thisColor
        }

        override fun run() {
            b1!!.setTextColor(theColor)
            tab1Label!!.setText(theText)
        }
    }

    override fun onDataArrival(json: String?) {
        if (json != null) {
            b1 = gControlFragment!!.setSensorValues(json)
        }

        gRunnableBlinker!!.setText("Sensors .", Color.GREEN)
        gUIHandler!!.post(gRunnableBlinker!!)
        gRunnableBlinker!!.setText("Sensors  ", Color.WHITE)
        gUIHandler!!.postDelayed(gRunnableBlinker!!, 250)
    }

    override fun onResume() {
        super.onResume()
        if (this.mViewPager == null) this.mViewPager = findViewById<MyViewPager>(R.id.viewpager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (this.mViewPager == null) this.mViewPager = findViewById<MyViewPager>(R.id.viewpager)


        // Tab Initialization
        initialiseTabHost()


        // Fragments and ViewPager Initialization
        val fragments: MutableList<Fragment> = this.fragments
        pageAdapter = MyPageAdapter(supportFragmentManager, fragments)
        this.mViewPager!!.adapter = pageAdapter
        this.mViewPager!!.setOnPageChangeListener(this@MainActivity)

        gUIHandler = Handler(Looper.getMainLooper())
        gRunnableBlinker = mRunnable()
    }

    // Manages the Tab changes, synchronizing it with Pages
    override fun onTabChanged(tag: String?) {
        val pos = this.mTabHost!!.currentTab
        this.mViewPager!!.currentItem = pos
    }

    // Manages the Page changes, synchronizing it with Tabs
    override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
        val pos: Int = this.mViewPager!!.currentItem
        this.mTabHost!!.currentTab = pos
    }


    private val fragments: MutableList<Fragment>
        get() {
            val fList: MutableList<Fragment> = ArrayList()

            val f1 = NaaradControlFragment.newInstance("Naarad Control Fragment")
            gControlFragment = f1
            tab1Label = mTabHost!!.tabWidget.getChildAt(1)
                .findViewById<View?>(android.R.id.title) as TextView

            val f2 = NaaradPlotFragment.newInstanceNPF("Naarad Plot Fragment")
            val f3 = NaaradSettingFragment.newInstanceNSF("Naarad Setting Fragment")

            fList.add(f1)
            fList.add(f2)
            fList.add(f3)

            return fList
        }

    // Tabs Creation
    private fun initialiseTabHost() {
        mTabHost = findViewById<TabHost>(android.R.id.tabhost)!!
        mTabHost!!.setup()

        AddTab(
            this, this.mTabHost!!,
            this.mTabHost!!.newTabSpec("Controls").setIndicator("Controls")
        )
        AddTab(
            this, this.mTabHost!!,
            this.mTabHost!!.newTabSpec("Sensors  ").setIndicator("Sensors  ")
        )
        AddTab(
            this, this.mTabHost!!,
            this.mTabHost!!.newTabSpec("Settings").setIndicator("Settings")
        )
        mTabHost!!.tabWidget.getChildAt(0).layoutParams.height = 40
        mTabHost!!.tabWidget.getChildAt(1).layoutParams.height = 40
        mTabHost!!.tabWidget.getChildAt(2).layoutParams.height = 40
        mTabHost!!.setOnTabChangedListener(this)
    }

    override fun onPageScrollStateChanged(arg0: Int) {}
    override fun onPageSelected(arg0: Int) {}

    companion object {
        // Method to add a TabHost
        private fun AddTab(
            activity: MainActivity?, tabHost: TabHost,
            tabSpec: TabSpec
        ) {
            tabSpec.setContent(MyTabFactory(activity))
            tabHost.addTab(tabSpec)
        }
    }
}
