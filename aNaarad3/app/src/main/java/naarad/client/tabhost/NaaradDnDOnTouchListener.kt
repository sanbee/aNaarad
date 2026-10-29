package naarad.client.tabhost

import android.graphics.Color
import android.view.ViewGroup.MarginLayoutParams
import com.sanbee.anaarad.R
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
        if (v == null || !touchFlag_l) return false

        val deltaX = (event.rawX - gDnDParams.raw_touch_x0).toInt()
        val deltaY = (event.rawY - gDnDParams.raw_touch_y0).toInt()

        val x = gDnDParams.initial_left + deltaX
        val y = gDnDParams.initial_top + deltaY

        val oX = myApp.dpToPixel(mySizeXDP)
        val oY = myApp.dpToPixel(mySizeYDP)

        val fragment = (NaaradAbstractFragment.mActivity as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
            ?.fragments?.firstOrNull { it is NaaradControlFragment } as? NaaradControlFragment
        if (fragment != null) {
            fragment.moveIconAndTextTogether(v, x, y, oX, oY)
        } else {
            gDnDParams.moveView(v, x, y, oX, oY, 1.0f, 1.0f)
        }

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
                val wasDragging = gDnDParams.touchFlag_p
                gDnDParams.touchFlag_p = false
                v.parent?.requestDisallowInterceptTouchEvent(false)

                if (v is ImageView) {
                    v.setBackgroundColor(Color.TRANSPARENT)
                    v.setAlpha(255)
                } else if (v is TextView) {
                    v.background?.alpha = 255
                }

                val tag = (v.getTag(R.integer.key0) as? Int) ?: -1
                NaaradAbstractFragment.mActivity?.let { activity ->
                    val fragment = (activity as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
                        ?.fragments?.firstOrNull { it is NaaradControlFragment } as? NaaradControlFragment
                    if (tag >= 0) fragment?.resetBulbHighlight(tag)
                    if (wasDragging) {
                        val lp = v.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                        val left = lp?.leftMargin ?: v.left
                        val top = lp?.topMargin ?: v.top
                        fragment?.saveSingleViewPosition(v, left, top)
                    }
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
