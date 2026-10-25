package naarad.client.tabhost

import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class MyViewPager : ViewPager {
    private var enabled: kotlin.Boolean
    private val myApp: NaaradApp

    constructor(
        context: android.content.Context,
        attrs: android.util.AttributeSet?
    ) : super(context, attrs) {
        myApp = context.getApplicationContext() as NaaradApp
        this.enabled = true

        // Keep all the three fragments in memory.  
        setOffscreenPageLimit(2)
    }

    constructor(context: android.content.Context) : super(context) {
        myApp = context.getApplicationContext() as NaaradApp
        this.enabled = false
    }

    fun enableSwipe(enable: kotlin.Boolean) {
        this.enabled = enable
    }

    public override fun onTouchEvent(event: MotionEvent?): kotlin.Boolean {
        //if (this.enabled) 
        if (myApp.swipeState) return super.onTouchEvent(event)

        return false
    }

    public override fun onInterceptTouchEvent(event: MotionEvent?): kotlin.Boolean {
        // if (myApp == null) Log.i("intercept: ", "app is null");
        // else
        //     if (myApp.getSwipeState()) Log.i("intercept:"," on");
        //     else             Log.i("intercept:"," off");

        //if (this.enabled) 

        if (myApp.swipeState) return super.onInterceptTouchEvent(event)

        return false
    }

    fun setPagingEnabled(enabled: kotlin.Boolean) {
        this.enabled = enabled
    }
}