package naarad.client.tabhost

import android.graphics.Color
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.TextView

// import android.preference.PreferenceManager;
// import android.content.SharedPreferences;
// import android.content.Context;
//private View.OnTouchListener gOnTouchListener;
class NaaradDnDOnTouchListener(//private View selected_item=null;
    private val gGestureDetector: GestureDetector,
    //public boolean touchFlag_p=false;
    //private int longPress_x, longPress_y;
    private val gDnDParams: NaaradDnDParameters,
    thisApp: NaaradApp,
    thisWidthFudgeFactor: Float,
    thisHeightFudgeFactor: Float,
    sizeXDP: Int, sizeYDP: Int
) : OnTouchListener {
    var myApp: NaaradApp

    private val myWidthFudgeFactor: Float
    private val myHeightFudgeFactor: Float
    private val mySizeXDP: Int
    private val mySizeYDP: Int

    // Construct with GestureDetector, the global DnDParameters object
    // and the global app object.
    init {
        myApp = thisApp
        myWidthFudgeFactor = thisWidthFudgeFactor
        myHeightFudgeFactor = thisHeightFudgeFactor
        mySizeXDP = sizeXDP
        mySizeYDP = sizeYDP
    }

    fun containerOnTouch(v: View?, event: MotionEvent, touchFlag_l: Boolean): Boolean {
        val w: Int
        val h: Int


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
        if ((touchFlag_l == true) && (v != null)) {
            //System.err.println("Display If  Part ::->"+touchFlag_l);
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    var x: Int
                    var y: Int
                    val pX: Int
                    val pY: Int
                    var oX = 0
                    var oY = 0
                    val iX: Int
                    val iY: Int
                    var dx: Int
                    var dy: Int
                    pX = event.x.toInt()
                    pY = event.y.toInt()

                    // w=selected_item.getHeight();
                    // h=selected_item.getWidth();
                    w = v.height
                    h = v.width
                    oX = myApp.dpToPixel(mySizeXDP) //(int)(mySizeXDP*myApp.densityDpi);//160.0);
                    oY = myApp.dpToPixel(mySizeYDP) //(int)(mySizeYDP*myApp.densityDpi);//160.0);

                    // iX=selected_item.getRight();
                    // iY=selected_item.getTop();
                    iX = v.right
                    iY = v.top

                    x = pX - oX + iX
                    y = pY - oY + iY

                    dx = oX * 2 - pX
                    dy = oY - pY
                    //dx = -pX;      dy = -pY;
                    dx = -(pX - gDnDParams.longPress_x)
                    dy = -(pY - gDnDParams.longPress_y)
                    val scalex = 800.0f / 1024.0f
                    val scaley = 480.0f / 768.0f
                    x = (iX - dx - 54 * scalex).toInt()
                    y = (iY - dy - 24 * scaley).toInt()


                    // System.err.println("M Display Here X Value-->"+(x)
                    // 		   +" Px:"+pX+" deX:"+(pX - gDnDParams.longPress_x)
                    // 		   +" R:"+iX+" dx:"+dx+" X:"+(iX - dx));
                    // System.err.println("M Display Here Y Value-->"+(y)
                    // 		   +" Py:"+pY+" deY:"+(pY - gDnDParams.longPress_y)
                    // 		   +" T:"+iY+" dy:"+dy+" Y:"+(iY - dy));
                    gDnDParams.moveView(
                        v, x, y,
                        oX, oY, 1.0f, 1.0f
                    )
                }

                else -> {}
            }
        }
        // else
        //     {
        // 	System.err.println("Display Else Part ::->"+touchFlag);
        //     }               
        return true
    }

    private fun myOnTouchListener_p(v: View, event: MotionEvent): Boolean {
        var ret = true
        val act = event.actionMasked

        if (act != MotionEvent.ACTION_MOVE) gGestureDetector.onTouchEvent(event)

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
        gDnDParams.selected_item = v

        when (act) {
            MotionEvent.ACTION_MOVE -> if (gDnDParams.touchFlag_p) {
                v.parent?.requestDisallowInterceptTouchEvent(true)
                ret = containerOnTouch(v, event, true)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                gDnDParams.touchFlag_p = false
                v.parent?.requestDisallowInterceptTouchEvent(false)

                if (v is ImageView) {
                    v.setAlpha(255)
                    v.setBackgroundColor(Color.TRANSPARENT)
                } else if (v is TextView) {
                    v.background?.alpha = 255
                }

                val lp = v.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                val left = lp?.leftMargin ?: v.left
                val top = lp?.topMargin ?: v.top
                NaaradAbstractFragment.mActivity?.let { activity ->
                    val fragment = (activity as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
                        ?.fragments?.firstOrNull { it is NaaradControlFragment } as? NaaradControlFragment
                    fragment?.saveSingleViewPosition(v, left, top)
                }

                //System.err.println("Drop pos.: $left $top")
                myApp.swipeState = true
            }
        }

        return ret
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gDnDParams.selected_item = v
        return myOnTouchListener_p(v, event)
    }
}
