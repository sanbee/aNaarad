package naarad.client.tabhost;

import android.util.Log;
import android.content.Context;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.support.v4.view.ViewPager;
import android.app.Application;

public class MyViewPager extends ViewPager 
{
    private boolean enabled;
    private NaaradApp myApp;

    public MyViewPager(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
	myApp = (NaaradApp) context.getApplicationContext();
        this.enabled = true;

	// Keep all the three fragments in memory.  
	setOffscreenPageLimit(2);
    }

    public MyViewPager(Context context) 
    {
        super(context);

	myApp = (NaaradApp) context.getApplicationContext();
        this.enabled = false;
    }

    public void enableSwipe(boolean enable) 
    {
	this.enabled=enable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) 
    {
        //if (this.enabled) 
	if (myApp.getSwipeState())
            return super.onTouchEvent(event);

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
	// if (myApp == null) Log.i("intercept: ", "app is null");
	// else
	//     if (myApp.getSwipeState()) Log.i("intercept:"," on");
	//     else             Log.i("intercept:"," off");

        //if (this.enabled) 
	if (myApp.getSwipeState()) 
	    return super.onInterceptTouchEvent(event);

        return false;
    }

    public void setPagingEnabled(boolean enabled) 
    {
        this.enabled = enabled;
    }
}