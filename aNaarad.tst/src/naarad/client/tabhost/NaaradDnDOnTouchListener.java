package naarad.client.tabhost;

import android.view.View;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.widget.RelativeLayout;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
// import android.preference.PreferenceManager;
// import android.content.SharedPreferences;
// import android.content.Context;

//private View.OnTouchListener gOnTouchListener;

public class NaaradDnDOnTouchListener implements View.OnTouchListener
{
    public NaaradApp myApp;
    //private View selected_item=null;
    private GestureDetector gGestureDetector;

    //public boolean touchFlag_p=false;
    //private int longPress_x, longPress_y;
    private NaaradDnDParameters gDnDParams;
    private float myWidthFudgeFactor, myHeightFudgeFactor;
    private int mySizeXDP, mySizeYDP;
    // Construct with GestureDetector, the global DnDParameters object
    // and the global app object.
    public NaaradDnDOnTouchListener(GestureDetector thisGestureDetector,
				    NaaradDnDParameters thisDnDParams,
				    NaaradApp thisApp,
				    Float thisWidthFudgeFactor,
				    Float thisHeightFudgeFactor,
				    int sizeXDP, int sizeYDP)
    {
	gGestureDetector = thisGestureDetector;
	gDnDParams = thisDnDParams;
	myApp = thisApp;
	myWidthFudgeFactor=thisWidthFudgeFactor;
	myHeightFudgeFactor=thisHeightFudgeFactor;
	mySizeXDP=sizeXDP; mySizeYDP = sizeYDP;
	//gGestureDetector = new GestureDetector(mActivity0, myGestureListener);
    }

    public boolean containerOnTouch(View v, MotionEvent event, boolean touchFlag_l) 
    {
	int w, h;
	// if ((ImageView)v != null) 
	//     {
	// 	Drawable drawable = ((ImageView)v).getDrawable();

	// 	Rect imageBounds = drawable.getBounds();
		
	// 	//original height and width of the bitmap
	// 	int intrinsicHeight = drawable.getIntrinsicHeight();
	// 	int intrinsicWidth = drawable.getIntrinsicWidth();

	// 	//height and width of the visible (scaled) image
	// 	int scaledHeight = imageBounds.height();
	// 	int scaledWidth = imageBounds.width();
		
	// 	//Find the ratio of the original image to the scaled image
	// 	//Should normally be equal unless a disproportionate scaling
	// 	//(e.g. fitXY) is used.
	// 	float heightRatio = intrinsicHeight / scaledHeight;
	// 	float widthRatio = intrinsicWidth / scaledWidth;

	// 	System.err.println("hr,wr:"+heightRatio+" "+widthRatio);
	//     }


	//	if ((touchFlag_l==true) && (selected_item != null))
	if ((touchFlag_l==true) && (v != null))
	    {
		//System.err.println("Display If  Part ::->"+touchFlag_l);
		switch (event.getActionMasked()) 
		    {
		    case MotionEvent.ACTION_MOVE:
			int x, y,pX,pY, oX=0, oY=0, iX,iY,dx,dy;
			pX=(int) event.getX();
			pY=(int) event.getY();

			// w=selected_item.getHeight();
			// h=selected_item.getWidth();
			w=v.getHeight();
			h=v.getWidth();
			oX=myApp.dpToPixel(mySizeXDP);//(int)(mySizeXDP*myApp.densityDpi);//160.0);
			oY=myApp.dpToPixel(mySizeYDP);//(int)(mySizeYDP*myApp.densityDpi);//160.0);

			// iX=selected_item.getRight();
			// iY=selected_item.getTop();
			iX=v.getRight();
			iY=v.getTop();

			x=pX - oX + iX;
			y=pY - oY + iY;

			dx = oX*2 - pX;  dy = oY - pY;
			//dx = -pX;      dy = -pY;
			dx = -(pX - gDnDParams.longPress_x) ;
			dy = -(pY - gDnDParams.longPress_y);
			float scalex=800.0f/1024.0f, scaley=480.0f/768.0f;
			x  = (int)(iX - dx -54*scalex) ;  y  = (int)(iY - dy -24*scaley);
			
			// System.err.println("M Display Here X Value-->"+(x)
			// 		   +" Px:"+pX+" deX:"+(pX - gDnDParams.longPress_x)
			// 		   +" R:"+iX+" dx:"+dx+" X:"+(iX - dx));
			// System.err.println("M Display Here Y Value-->"+(y)
			// 		   +" Py:"+pY+" deY:"+(pY - gDnDParams.longPress_y)
			// 		   +" T:"+iY+" dy:"+dy+" Y:"+(iY - dy));

			gDnDParams.moveView(v,x,y,
					    oX,oY,1.0F,1.0F);
					    // myWidthFudgeFactor,
					    // myHeightFudgeFactor);

			// Move the view
			// RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams
			//     (new ViewGroup.MarginLayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
			// 				      RelativeLayout.LayoutParams.WRAP_CONTENT));
			// lp.setMargins(x, y, 0, 0);  // top, left, right, bottom
			// //lp.rightMargin = x; lp.topMargin = y;
			// //lp.height = (int)(float)(oX*myWidthFudgeFactor*1.5)/2;
			// lp.height = (int)(float)(oX*myHeightFudgeFactor);
			// lp.width  = (int)((float)oY*myWidthFudgeFactor);
			// //selected_item.setLayoutParams(lp);
			// v.setLayoutParams(lp);

			//resizeView(selected_item, event, 50,50);
			break;  
		    // case MotionEvent.ACTION_UP:
		    // 	Log.i(null,"COT Up");
		    // 	break;
		    default:
			break;
		    }
	    }
	// else
	//     {
	// 	System.err.println("Display Else Part ::->"+touchFlag);
	//     }               
	return true;
    };

    private boolean myOnTouchListener_p(View v, MotionEvent event)
    {
	    boolean ret=true;
	    int act = event.getActionMasked();
	    
	    if (act != MotionEvent.ACTION_MOVE)	
		gGestureDetector.onTouchEvent(event);

	    // This handler sets up the global variable selected_item
	    // to point to the view on which the events were detected.
	    //
	    // The MOVE event transfers control to containerOnTouch
	    // which has the code to actually moving the icons.  The
	    // UP event essentially finishes the MOVE event, setting
	    // the global variables (toughFlag_p and selected_item),
	    // resetting the background of the icon on the screen and
	    // re-enabling the swiping of the TabHost Fragments).
	    //selected_item = v;
	    gDnDParams.selected_item = v;

	    switch(act)
		{
		case MotionEvent.ACTION_MOVE:
		    if (gDnDParams.touchFlag_p)
			//ret=containerOnTouch(selected_item, event,true);
			ret=containerOnTouch(v, event,true);
		    break;
		case MotionEvent.ACTION_UP:
		    gDnDParams.touchFlag_p=false;

		    if (v instanceof ImageView)
			{
			    ((ImageView)v).setAlpha(255);
			    v.setBackgroundColor(Color.TRANSPARENT);
			}
		    else if (v instanceof TextView)
			{
			    //System.err.println("setting alpha");
			    ((TextView)v).getBackground().setAlpha(100);
			}

		    // selected_item.setBackgroundColor(Color.TRANSPARENT);
		    // selected_item=null;
		    System.err.println("Drop pos.: "+v.getRight()+" "+v.getTop());
		    {
			
		    }
		    //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context)
		    //setPreference("posx",v.getRight());
		    myApp.setSwipeState(true);
		    break;
		};
	    return ret;
    }

    public boolean onTouch(View v, MotionEvent event) 
    {
	gDnDParams.selected_item = v;
	return myOnTouchListener_p(v,event);
    }
}