package naarad.client.tabhost;
import android.view.View;
import android.widget.RelativeLayout;
import android.view.ViewGroup;

public class NaaradDnDParameters
{
    // The item which was clicked.  
    public View selected_item; 

    // The location at which longPress event happened.
    public int longPress_x, longPress_y;

    // Set in GestureDetector on LongPress and used in OnTouchListener
    // ACTION_MOVE to call containerOnTouch() to do the actual move of
    // the selected_item.
    public boolean touchFlag_p=false; 

    public void moveView(View v, int x, int y, int w, int h,
			 float wFudge, float hFudge)
    {
	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams
	    (new ViewGroup.MarginLayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
					      RelativeLayout.LayoutParams.WRAP_CONTENT));
	lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	lp.setMargins(x, y, 0, 0);  // left, top, right, bottom
	//lp.rightMargin = x; lp.topMargin = y;
	if (h > 0) lp.height = (int)(float)(h*hFudge);
	if (w > 0) lp.width  = (int)(float)(w*wFudge);
	//selected_item.setLayoutParams(lp);
	v.setLayoutParams(lp);
    }

}