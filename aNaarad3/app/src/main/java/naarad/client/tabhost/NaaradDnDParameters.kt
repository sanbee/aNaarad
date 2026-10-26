package naarad.client.tabhost

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.RelativeLayout

class NaaradDnDParameters {
    // The item which was clicked.  
    var selected_item: View? = null

    // The location at which longPress event happened.
    var longPress_x: Int = 0
    var longPress_y: Int = 0

    // Set in GestureDetector on LongPress and used in OnTouchListener
    // ACTION_MOVE to call containerOnTouch() to do the actual move of
    // the selected_item.
    var touchFlag_p: Boolean = false

    fun moveView(
        v: View, x: Int, y: Int, w: Int, h: Int,
        wFudge: Float, hFudge: Float
    ) {
        val lp = RelativeLayout.LayoutParams(
            MarginLayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
        )
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        lp.setMargins(x, y, 0, 0) // left, top, right, bottom
        //lp.rightMargin = x; lp.topMargin = y;
        if (h > 0) lp.height = (h * hFudge).toInt()
        if (w > 0) lp.width = (w * wFudge).toInt()
        //selected_item.setLayoutParams(lp);
        v.layoutParams = lp
    }
}