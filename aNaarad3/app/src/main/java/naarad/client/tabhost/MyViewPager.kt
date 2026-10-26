package naarad.client.tabhost

import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class MyViewPager : ViewPager {
    private var enabled: Boolean
    private val myApp: NaaradApp

    constructor(
        context: android.content.Context,
        attrs: android.util.AttributeSet?
    ) : super(context, attrs) {
        myApp = context.applicationContext as NaaradApp
        this.enabled = true

        // Keep all the three fragments in memory.  
        setOffscreenPageLimit(2)
    }

    constructor(context: android.content.Context) : super(context) {
        myApp = context.applicationContext as NaaradApp
        this.enabled = false
    }

    fun enableSwipe(enable: Boolean) {
        this.enabled = enable
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //if (this.enabled) 
        return myApp.swipeState && super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        // if (myApp == null) Log.i("intercept: ", "app is null");
        // else
        //     if (myApp.getSwipeState()) Log.i("intercept:"," on");
        //     else             Log.i("intercept:"," off");

        //if (this.enabled) 

        return myApp.swipeState && super.onInterceptTouchEvent(event)
    }

    fun setPagingEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}