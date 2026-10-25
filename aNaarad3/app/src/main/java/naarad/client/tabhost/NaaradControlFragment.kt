package naarad.client.tabhost

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.ToggleButton
import org.json.JSONException
import org.json.JSONObject
import com.sanbee.anaarad.R
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException
import android.os.AsyncTask

//public class NaaradControlFragment extends Fragment implements View.OnLongClickListener 
class NaaradControlFragment : NaaradAbstractFragment() //implements OnTouchListener
{
    private var client: Socket? = null
    private var printwriter: PrintWriter? = null
    private var lamp0: ToggleButton? = null
    private var lamp1: ToggleButton? = null
    private var lamp2: ToggleButton? = null
    private var currentToggleButton: ToggleButton? = null
    private var lampArr: Array<ToggleButton?> = arrayOfNulls(3)
    private var bulb0: ImageView? = null
    private var bulb1: ImageView? = null
    private var bulb2: ImageView? = null
    private var bulbArr: Array<ImageView?> = arrayOfNulls(3)
    private var tempBubbleArr: Array<TextView?> = arrayOfNulls(2)

    //    private TextView ttBulbArr;
    private var messsage: String? = null
    private var tempInDegC = true
    private val ALL_WELL = "All well"
    private val iv: ImageView? = null

    //public boolean touchFlag=false;
    //private View selected_item=null;
    //private int longPress_x, longPress_y;
    private var gGestureDetector: GestureDetector? = null
    private var gBubbleGestureListener: GestureDetector? = null
    private var gOnTouchListener: OnTouchListener? = null
    private var gBubbleOnTouchListener: OnTouchListener? = null
    private var gLampHandler: View.OnClickListener? = null

    //private View.OnClickListener gBulbOnClickListener;
    //private boolean touchFlag_p;
    private var mActivity0: Activity? = null

    //private MyViewPager mViewPager=null;
    var myApp: NaaradApp? = null
    var gDnDParams: NaaradDnDParameters? = null

    //
    //-----------------------------------------------------------------------------------------
    //
    private fun resizeView(v: View, event: MotionEvent, xSizeDP: Int, ySizeDP: Int) {
        val x: Int
        val y: Int
        val pX: Int
        val pY: Int
        var oX = 0
        var oY = 0
        val iX: Int
        val iY: Int
        val xsdp: Int
        val ysdp: Int
        xsdp = (xSizeDP * myApp!!.densityDpi / 160.0).toInt()
        ysdp = (ySizeDP * myApp!!.densityDpi / 160.0).toInt()

        pX = event.getX().toInt()
        pY = event.getY().toInt()

        oX = v.getHeight() * 2
        oY = v.getWidth()
        oX = xsdp * 2
        oY = ysdp
        iX = v.getRight()
        iY = v.getTop()
        x = pX - oX + iX
        y = pY - oY + iY

        // System.err.println("M Display Here X Value-->"+(x)+" "+pX+" "+oX+" "+iX);
        // System.err.println("M Display Here Y Value-->"+(y)+" "+pY+" "+oY+" "+iY);
        val lp: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
            MarginLayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
        )
        lp.setMargins(x, y, 0, 0)
        lp.height = xsdp
        lp.width = ysdp
        v.setLayoutParams(lp)
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun setSensorValues(jsonStr: String): TextView? {
        //System.err.println("From NCF: "+jsonStr);
        var temp: Float
        var svolt: Float
        var rssi: Float
        val nodeid: Int
        try {
            val json = JSONObject(jsonStr)
            temp = json.getDouble("degc").toFloat()

            // svolt  = (float)json.getDouble("node_v");
            // rssi   = (float)json.getDouble("node_p");
            nodeid = json.getInt("node_id")
            val bubbleID = mapNodeID2Ndx(nodeid)


            // if (nodeid == 1)      bubbleID = 0;
            // else if (nodeid == 3) bubbleID = 1;
            val unit = tempBubbleArr[bubbleID]!!.getTag(R.integer.key1) as String?
            if (unit === "F") temp = temp * 9.0f / 5.0f + 32.0f

            setBubbleValue(nodeid, temp)
            return tempBubbleArr[bubbleID]
        } catch (e: JSONException) {
            //throw new RuntimeException(e);
            System.err.println(e.message)
        }
        return null
    }

    fun setBubbleValue(nodeid: Int, temp: Float) {
        class myRunnable : Runnable {
            var thisID: Int = 0
            var thisText: String? = null
            var thisTV: TextView
            var tmp: Spanned?

            //tempBubbleArr[thisID].setText(thisText);
            //myRunnable(int id, String text) 
            internal constructor(tv: TextView, text: String?) {
                thisTV = tv
                thisText = text
                tmp =
                    Html.fromHtml("<p><b>" + thisText + "</b><font size =\"50\" color=\"#0066FF\"></font></p>")
            }

            internal constructor(tv: TextView, text: Spanned?) {
                thisTV = tv
                tmp = text
            }

            override fun run() {
                //tempBubbleArr[thisID].setText(thisText);
                // tempBubbleArr[thisID].setText(tmp);
                // tempBubbleArr[thisID].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                // tempBubbleArr[0].setTextColor(Color.parseColor("white"));

                thisTV.setText(tmp)
                thisTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                thisTV.setTextColor(Color.parseColor("white"))
            }
        }

        val bubbleID = mapNodeID2Ndx(nodeid)

        // int bubbleID=0;
        // if (nodeid == 1)      bubbleID = 0;
        // else if (nodeid == 3) bubbleID = 1;
        var text = String.format("%.2f", temp)
        val unit = tempBubbleArr[bubbleID]!!.getTag(R.integer.key1) as String
        text = text + unit

        tempBubbleArr[bubbleID]!!.setTag(R.integer.key0, temp)

        //tempBubbleArr[bubbleID].setTag(R.integer.key1,unit);

        //Spanned tmp=Html.fromHtml("<p>"+text+"<sup><small>"+unit+"</small></sup></p>");
        //myRunnable setTVText=new myRunnable(tempBubbleArr[bubbleID],tmp);
        val setTVText: myRunnable = myRunnable(tempBubbleArr[bubbleID]!!, text)

        tempBubbleArr[bubbleID]!!.post(setTVText)
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun setBulbBG(v: View, on: Boolean) {
        if (on) {
            ((v) as ImageView).setImageDrawable(
                mActivity0!!.getResources().getDrawable(R.drawable.lamp_off)
            )
        } else {
            ((v) as ImageView).setImageDrawable(
                mActivity0!!.getResources().getDrawable(R.drawable.lamp_on)
            )
            //v.setBackgroundColor(Color.YELLOW);
        }
        v.setBackgroundColor(Color.TRANSPARENT)


        // if (on) v.setBackgroundColor(Color.TRANSPARENT);
        // else    v.setBackgroundColor(Color.YELLOW);
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun setLampBG(v: View): Boolean {
        val on: Boolean = (v as ToggleButton).isChecked()
        if (on) {
            //v.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.lamp_on));
            v.getBackground().setAlpha(255)
        } else {
            //		v.setBackgroundDrawable(R.drawable.lamp_off);
            v.getBackground().setAlpha(128)
        }
        return on
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun lampHandler0(v: View) {
        serverName = serverName
        serverPort = serverPort
        currentToggleButton = (v) as ToggleButton

        //Log.i("Ctrl Server: ", serverName+":"+serverPort);
        val tag = (v.getTag() as String?)!!.toInt()
        val on = setLampBG(v)
        setBulbBG(bulbArr[tag]!!, !on)
        messsage = "tell " + v.getTag() + " "

        if (on) messsage += "1"
        else messsage += "0"


        //	Log.i("Cmd: ",messsage);
        val sendMessageTask = SendMessage()
        sendMessageTask.execute()
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    // @Override public void onSaveInstanceState(Bundle outState)
    // {
    // 	//Log.i("setInstance: ", "Setting 0");
    // 	super.onSaveInstanceState(outState);
    // 	ColorDrawable cd=(ColorDrawable)(bulb2.getBackground());
    // 	outState.putInt("bg0", cd.getColor());
    // }
    //
    //-----------------------------------------------------------------------------------------
    //
    var bubbleGestureListener
            : SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            super.onSingleTapUp(e)
            tempInDegC = !tempInDegC
            // if (tempInDegC) toast("Temp. in degC");
            // else toast("Temp. in F");
            var tt = (gDnDParams!!.selected_item.getTag(R.integer.key0) as Number).toFloat()
            var unit = gDnDParams!!.selected_item.getTag(R.integer.key1) as String?
            val nodeid = gDnDParams!!.selected_item.getTag() as Int
            //System.err.println("T: "+tt+unit+nodeid);
            if (unit === "C") {
                tt = tt * 9.0f / 5.0f + 32.0f
                unit = "F"
            } else {
                tt = (tt - 32.0f) * 5.0f / 9.0f
                unit = "C"
            }
            gDnDParams!!.selected_item.setTag(R.integer.key1, unit)
            // if (!tempInDegC) 
            // 	tt = tt * 9.0F/5.0F + 32.0F;
            //     //temp = temp * 9.0F/5.0F + 32.0F;
            // else
            // 	tt = (tt-32.0f)*5.0f/9.0f;
            setBubbleValue(nodeid, tt)

            return false
        }

        override fun onLongPress(e: MotionEvent) {
            // Log.i("Gesture: ","LongPress");
            super.onLongPress(e)
            toast("DnD activated", Gravity.TOP or Gravity.RIGHT)

            gDnDParams!!.touchFlag_p = true
            //((TextView)gDnDParams.selected_item).setAlpha(100);
            //gDnDParams.selected_item.setBackgroundColor(Color.GREEN);
            gDnDParams!!.selected_item.getBackground().setAlpha(80)

            myApp!!.swipeState = false

            gDnDParams!!.longPress_x = e.getX().toInt()
            gDnDParams!!.longPress_y = e.getY().toInt()

            // int[] loc = new int[2];
            // gDnDParams.selected_item.getLocationOnScreen(loc);
            // System.err.println("On screen (x,y): "
            // 		       +loc[0]+" "+loc[1]+" "
            // 		       +gDnDParams.selected_item.getRight()+" "+gDnDParams.selected_item.getTop()+" "
            // 		       +gDnDParams.longPress_x+" "+gDnDParams.longPress_y);
            return
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    // Gesture listener with onSingleTapUp() and onLongPress() overloaded.  
    //
    var myGestureListener
            : SimpleOnGestureListener = object : SimpleOnGestureListener() {
        //
        // onSingleTap() on the lamp icons recreates the same
        // operation as click on the associated ToggleButtons.
        //
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            super.onSingleTapUp(e)
            //		Log.i("Gesture: ","TapUp");
            val tag = (gDnDParams!!.selected_item.getTag(R.integer.key0)) as Int
            //Log.i("Tag: ",Integer.toString(tag));//R.integer.key0);
            val on = lampArr[tag]!!.isChecked()
            setBulbBG(gDnDParams!!.selected_item!!, on)
            lampArr[tag]!!.setChecked(!on)
            lampHandler0(lampArr[tag]!!)
            return false
        }

        //
        // onLongPress() sets up the global variables used for the
        // MOVE operation (touchFlag_p, selected_item and
        // longPress_{x,y}) and disables the swiping of the
        // Fragements.
        //
        override fun onLongPress(e: MotionEvent) {
            // Log.i("Gesture: ","LongPress");
            toast("DnD activated", Gravity.TOP or Gravity.RIGHT)
            super.onLongPress(e)
            gDnDParams!!.touchFlag_p = true
            // ttbulb -- uncomment the following line when not using ttbulb
            (gDnDParams!!.selected_item as ImageView).setAlpha(100)

            gDnDParams!!.selected_item.setBackgroundColor(Color.GREEN)
            gDnDParams!!.selected_item.getBackground().setAlpha(100)

            //resizeView(selected_item, e, 50, 50);
            //mViewPager = ((MyViewPager)mActivity0.findViewById(R.id.viewpager));
            //mViewPager.enableSwipe(false);
            //mViewPager.enableSwipe(false);
            myApp!!.swipeState = false
            // Location of the long press event on the screen (in
            // the co-ordinate system of the Fragment display area
            // -- i.e. minus any area of the screen occupied by
            // the Tabs etc.).
            //
            // These co-ordinates are relative to the co-ordinate
            // system of the View on which the LongPress was
            // detected.  Therefore these need not be reset in the
            // MOVE event handler.
            gDnDParams!!.longPress_x = e.getX().toInt()
            gDnDParams!!.longPress_y = e.getY().toInt()

            val loc = IntArray(2)
            gDnDParams!!.selected_item.getLocationOnScreen(loc)
            System.err.println(
                ("On screen (x,y): "
                        + loc[0] + " " + loc[1] + " "
                        + gDnDParams!!.selected_item.getRight() + " " + gDnDParams!!.selected_item.getTop() + " "
                        + gDnDParams!!.longPress_x + " " + gDnDParams!!.longPress_y)
            )

            //containerOnTouch(selected_item, e, touchFlag_p);
            return
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    private fun makeHandlers(activity: Activity?) // View.OnClickListener lampHandler, 
    // View.OnTouchListener onTouchListener, 
    // GestureDetector gestureDetector)
    {
        // The following handles events on the toggle buttons
        gLampHandler = object : View.OnClickListener {
            override fun onClick(v: View) {
                lampHandler0(v)
            }
        }


        // The following two handlers handle gestures (SingleTap,
        // LongPress) on lamp icons.
        // gOnTouchListener = new View.OnTouchListener() 
        //     {
        //         public boolean onTouch(View v, MotionEvent event) 
        //         {
        // 	    return myOnTouchListener_p(v,event);
        // 	}
        //     };

        // Gesture detection for the lamp icons
        gGestureDetector = GestureDetector(mActivity0, myGestureListener)
        gOnTouchListener = NaaradDnDOnTouchListener(
            gGestureDetector!!,
            gDnDParams!!,
            myApp!!,
            1.0f,
            1.0f,
            20,
            20
        ) //Test code


        // Gesture detection for the data-bubble icons.  Need for the
        // height,width fudge factors (2.0, 1.5) should fixed.
        gBubbleGestureListener = GestureDetector(mActivity0, bubbleGestureListener)
        gBubbleOnTouchListener = NaaradDnDOnTouchListener(
            gBubbleGestureListener!!,
            gDnDParams!!,
            myApp!!,
            1.0f,
            1.0f,
            45,
            25
        ) //2.3F,1.5F); //Test code
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    private fun installHandlers(
        lampHandler: View.OnClickListener?,
        onTouchListener: OnTouchListener?,
        lampArr: Array<ToggleButton?>,
        bulbArr: Array<ImageView?>
    ) {
        // Set up the handlers for events on the toggle buttons
        // (Single click).
        for (i in lampArr.indices) {
            lampArr[i].setTag(i.toString())
            lampArr[i].setOnClickListener(lampHandler)
            setLampBG(lampArr[i])
        }

        // Set up the handlers to handle gestures (SingleTap,
        // LongPress) on the icons for the lamps.
        for (i in bulbArr.indices) {
            // if (i==1) 
            //     {
            // 	ttBulbArr.setTag(R.integer.key0,i);
            // 	ttBulbArr.setTag(R.integer.key1,"0");
            // 	ttBulbArr.setOnTouchListener(onTouchListener);
            //     }
            // else 
            run {
                bulbArr[i]!!.setTag(R.integer.key0, i)
                bulbArr[i]!!.setTag(R.integer.key1, "0")
                bulbArr[i]!!.setOnTouchListener(onTouchListener)
            }
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun makeSensorIcons(thisView: View) {
        //
        // To add more icons/buttons, adding to the following arrays
        // would be sufficient.
        //
        mActivity0 = getActivity()
        lampArr = kotlin.arrayOfNulls<ToggleButton>(3)
        bulbArr = arrayOfNulls<ImageView>(3)

        val keys = getKeysAsArray(nodeID2Ndx!!)

        // for (int i=0;i<keys.length;i++)
        //     System.err.println("Keys: "+keys[i]);
        val nBubbles = nodeID2Ndx!!.size

        tempBubbleArr = arrayOfNulls<TextView>(nBubbles)

        lamp0 = thisView.findViewById<View?>(R.id.lamp0) as ToggleButton
        lampArr[0] = lamp0
        lamp1 = thisView.findViewById<View?>(R.id.lamp1) as ToggleButton
        lampArr[1] = lamp1
        lamp2 = thisView.findViewById<View?>(R.id.lamp2) as ToggleButton
        lampArr[2] = lamp2
        bulb0 = thisView.findViewById<View?>(R.id.iv1) as ImageView?
        bulbArr[0] = bulb0
        bulb1 = thisView.findViewById<View?>(R.id.iv2) as ImageView?
        bulbArr[1] = bulb1


        //ttBulbArr  =  (TextView) thisView.findViewById(R.id.iv2); 
        bulb2 = thisView.findViewById<View?>(R.id.iv3) as ImageView?
        bulbArr[2] = bulb2

        tempBubbleArr[0] = thisView.findViewById<View?>(R.id.tv1) as TextView?
        tempBubbleArr[1] = thisView.findViewById<View?>(R.id.tv2) as TextView?

        for (i in tempBubbleArr.indices) {
            tempBubbleArr[i]!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            tempBubbleArr[i]!!.setTag(R.integer.key1, "C")
            //tempBubbleArr[0].setTag(1); // Set the default tag value
            tempBubbleArr[i]!!.setTag(keys[i]) // Set the default tag value
            //setBubbleValue(1,20.63f);
            //tempBubbleArr[0].setText(Html.fromHtml("<p><b>21.63C</b><font size =\"50\" color=\"#0066FF\"></font></p>"));
            tempBubbleArr[i]!!.setTextColor(Color.parseColor("white"))
        }


        // tempBubbleArr[1].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        // //tempBubbleArr[1].setText(Html.fromHtml("<p><b>----C</b><font size =\"50\" color=\"#0066FF\"></font></p>"));
        // tempBubbleArr[1].setTag(R.integer.key1,"C");
        // //tempBubbleArr[1].setTag(3); // Set the default tag value
        // tempBubbleArr[1].setTag(keys[1]); // Set the default tag value
        // //setBubbleValue(3,21.45f);
        // tempBubbleArr[1].setTextColor(Color.parseColor("white"));


        //
        // If the position of icons was saved earlier, load the
        // position and re-position the icons.
        //
        val oX: Int
        val oY: Int
        var x = -2
        var y = -2
        var xMargin = (54 * 480.0 / 600.0).toInt()
        val yMargin = (24 * 800.0 / 1024.0).toInt()
        oY = myApp!!.dpToPixel(20)
        oX = oY //(int)(20*myApp.densityDpi);///160.0);

        for (i in bulbArr.indices) {
            //Drawable image = getResources().getDrawable(R.id.iv1);
            //float scale = myApp.metrics.widthPixels / (float) bulbArr[i].getIntrinsicWidth();

            System.err.println("Scale = " + myApp!!.metrics!!.widthPixels)

            x = getPreference("bulbX" + String.format("%d", i), -1)
            y = getPreference("bulbY" + String.format("%d", i), -1)
            System.err.println("bulb" + i + " " + x + " " + y)
            if ((x != -1) && (y != -1)) {
                // if (i == 1)
                //     gDnDParams.moveView(ttBulbArr,x-xMargin,y-yMargin,oX,oY,1.0F,1.0F);
                // else
                run {
                    System.err.println(
                        ("bulb" + i
                                + " " + (bulbArr[i]!!.getParent() as View).getLeft() // +" "+bulbArr[i].getTotalPaddingRight()
                                // +" "+bulbArr[i].getTotalPaddingLeft()
                                // +" "+bulbArr[i].getTotalPaddingTop()
                                // +" "+bulbArr[i].getTotalPaddingBottom()
                                // +" "+xPadding
                                + " " + myApp!!.densityDpi)
                    )
                    gDnDParams!!.moveView(bulbArr[i], x - xMargin, y - yMargin, oX, oY, 1.0f, 1.0f)
                }
            }
        }
        xMargin = (xMargin * myApp!!.densityDpi).toInt()
        for (i in tempBubbleArr.indices) {
            tempBubbleArr[i]!!.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            var width = tempBubbleArr[i]!!.getMeasuredWidth()
            var height = tempBubbleArr[i]!!.getMeasuredHeight()
            width = myApp!!.dpToPixel(45) //(int)(45*myApp.densityDpi);
            height = myApp!!.dpToPixel(25) //(int)(25*myApp.densityDpi);
            x = getPreference("bubbleX" + String.format("%d", i), -1)
            y = getPreference("bubbleY" + String.format("%d", i), -1)
            //int xPadding = 1+tempBubbleArr[i].getTotalPaddingRight()+tempBubbleArr[i].getTotalPaddingLeft();
            val xPadding =
                ((1 + tempBubbleArr[i]!!.getTotalPaddingRight() + tempBubbleArr[i]!!.getTotalPaddingLeft())) //*1.5F/2.0F),
            val yPadding =
                ((1 + tempBubbleArr[i]!!.getTotalPaddingTop() + tempBubbleArr[i]!!.getTotalPaddingBottom())) //*1.5F/2.0F);
            System.err.println(
                ("bubble" + i
                        + " " + tempBubbleArr[i]!!.getTotalPaddingRight()
                        + " " + tempBubbleArr[i]!!.getTotalPaddingLeft()
                        + " " + tempBubbleArr[i]!!.getTotalPaddingTop()
                        + " " + tempBubbleArr[i]!!.getTotalPaddingBottom()
                        + " " + (tempBubbleArr[i]!!.getParent() as View).getLeft()
                        + " " + xPadding + " " + yPadding
                        + " " + myApp!!.densityDpi)
            )
            if ((x != -1) && (y != -1)) gDnDParams!!.moveView(
                tempBubbleArr[i],  //x,y,
                x - xMargin - xPadding, y - yMargin,  //-xPadding,
                width, height, 1.0f, 1.0f
            )
        }

        // Makes the handles accessed via the global variables
        // gLampHandler, gOnTouchListener, gGestureDetector;
        makeHandlers(mActivity0)

        // Install the gLampHandler, gOnTouchListener,
        // gGestureDetector event/gesture handlers.
        installHandlers(gLampHandler, gOnTouchListener, lampArr, bulbArr)
        tempBubbleArr[0]!!.setOnTouchListener(gBubbleOnTouchListener)
        tempBubbleArr[1]!!.setOnTouchListener(gBubbleOnTouchListener)
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        myApp = getActivity().getApplication() as NaaradApp

        //Log.i("dpi: ",Integer.toString(myApp.densityDpi));
        setRetainInstance(true)
        mActivity0 = getActivity()
        gDnDParams = NaaradDnDParameters()

        if (recreateView(mView)) return mView!!

        mView = inflater.inflate(R.layout.activity_naarad_control, container, false)
        makeSensorIcons(mView!!)

        // //
        // // To add more icons/buttons, adding to the following arrays
        // // would be sufficient.
        // //
        // lampArr = new ToggleButton[3];
        // bulbArr = new ImageView[3];
        // tempBubbleArr = new TextView[2];

        // lampArr[0]  = lamp0 = (ToggleButton) mView.findViewById(R.id.lamp0);
        // lampArr[1]  = lamp1 = (ToggleButton) mView.findViewById(R.id.lamp1);
        // lampArr[2]  = lamp2 = (ToggleButton) mView.findViewById(R.id.lamp2);
        // bulbArr[0]  = bulb0 = (ImageView) mView.findViewById(R.id.iv1); 
        // bulbArr[1]  = bulb1 = (ImageView) mView.findViewById(R.id.iv2); 
        // bulbArr[2]  = bulb2 = (ImageView) mView.findViewById(R.id.iv3); 
        // tempBubbleArr[0] = (TextView) mView.findViewById(R.id.tv1);
        // tempBubbleArr[0].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        // tempBubbleArr[0].setText(Html.fromHtml("<p><b>----C</b><font size =\"50\" color=\"#0066FF\"></font></p>"));
        // tempBubbleArr[0].setTextColor(Color.parseColor("white"));
        // tempBubbleArr[1] = (TextView) mView.findViewById(R.id.tv2);
        // tempBubbleArr[1].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        // tempBubbleArr[1].setText(Html.fromHtml("<p><b>----C</b><font size =\"50\" color=\"#0066FF\"></font></p>"));
        // tempBubbleArr[1].setTextColor(Color.parseColor("white"));
        // // Makes the handles accessed via the global variables
        // // gLampHandler, gOnTouchListener, gGestureDetector;
        // makeHandlers(mActivity0);

        // // Install the gLampHandler, gOnTouchListener,
        // // gGestureDetector event/gesture handlers.
        // installHandlers(gLampHandler, gOnTouchListener, lampArr, bulbArr);
        // tempBubbleArr[0].setOnTouchListener(gBubbleOnTouchListener);
        // tempBubbleArr[1].setOnTouchListener(gBubbleOnTouchListener);
        mView!!.getBackground().setAlpha(100)

        return mView!!
    }

    public override fun onResume() {
        super.onResume()
        makeSensorIcons(mView!!)
        val lp: MarginLayoutParams = mView!!.getLayoutParams() as MarginLayoutParams
        System.err.println(
            "Offsets: " +
                    lp.leftMargin + " " +
                    lp.rightMargin
        )
        getRetainInstance()
        System.err.println("From NCF::onResume")
    }

    public override fun onPause() {
        super.onPause()
        setRetainInstance(true)
        System.err.println("From NCF::onPause")
        val xf = 1.0
        val yf = 1.0 //480.0/600.0, yf=800.0/1024.0;
        for (i in lampArr.indices) {
            setPreference("bulbX" + String.format("%d", i), (bulbArr[i]!!.getRight() * xf).toInt())
            setPreference("bulbY" + String.format("%d", i), (bulbArr[i]!!.getTop() * yf).toInt())
        }
        for (i in tempBubbleArr.indices) {
            setPreference("bubbleX" + String.format("%d", i), tempBubbleArr[i]!!.getRight())
            setPreference("bubbleY" + String.format("%d", i), tempBubbleArr[i]!!.getTop())
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun sendCommand(cmd: String): String? {
        try {
            if (cmd.length == 0) return null


            //Log.i("Thread: ",serverName+":"+Integer.toString(serverPort));
            client = Socket(serverName, serverPort)
            printwriter = PrintWriter(client!!.getOutputStream(), true)

            printwriter!!.write(mkMessage("open"))
            printwriter!!.flush()
            SystemClock.sleep(500)

            printwriter!!.write(mkMessage(cmd)) // write the message to output stream
            // printwriter.write((cmd)); // write the message to output stream
            printwriter!!.flush()
            SystemClock.sleep(500)

            printwriter!!.write(mkMessage("done"))
            printwriter!!.flush()

            //SystemClock.sleep(500);
            printwriter!!.close()
            client!!.close() // closing the connection
        } catch (e: UnknownHostException) {
            var msg = ""
            if (!isWifiConnected) msg = "Wifi is not connected.\n"
            msg =
                msg + "Unknown host: " + serverName + ":" + serverPort.toString() + "\nCheck settings"
            return msg
        } catch (e: IOException) {
            val msg =
                "Error connecting to " + serverName + ":" + serverPort.toString() + "\nCheck settings"
            return msg
        }
        return ALL_WELL
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    private inner class SendMessage : AsyncTask<Void?, Void?, String?>() {
        //
        //-----------------------------------------------------------------------------------------
        //
        override fun doInBackground(vararg params: Void?): String? {
            //Log.i("Thread: ",messsage);
            return sendCommand(messsage!!)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result !== ALL_WELL) {
                //Toast.makeText(mActivity0, result, Toast.LENGTH_SHORT).show();
                toast(result, Gravity.TOP or Gravity.RIGHT)
                val on = currentToggleButton.isChecked()
                val tag = (currentToggleButton.getTag() as String?)!!.toInt()

                currentToggleButton.setChecked(!on)
                setBulbBG(bulbArr[tag]!!, on)
            }
        }
    }

    companion object {
        private var mView: View? = null

        //
        //-----------------------------------------------------------------------------------------
        //
        @JvmStatic
        fun newInstance(sampleText: String?): NaaradControlFragment {
            val f = NaaradControlFragment()

            val b = Bundle()
            b.putString("bString", sampleText)
            f.setArguments(b)

            return f
        }
    }
} //
//=====================TEST CODE===================================
//
// RelativeLayout rl = new RelativeLayout(mView.getContext());
// ImageView iv;
// RelativeLayout.LayoutParams params;
// int yellow_iv_id = 123; // Some arbitrary ID value.
// iv = new ImageView(rl.getContext());
// iv.setId(yellow_iv_id);
// iv.setImageResource(R.drawable.ic_launcher);
// //	iv.setBackgroundColor(Color.YELLOW);
// params = new RelativeLayout.LayoutParams(30, 40);
// params.leftMargin = 50;
// params.topMargin = 60;
// iv.setLayoutParams(params);
// rl.addView(iv, params);
// iv.setVisibility(View.VISIBLE);
// iv = new ImageView(rl.getContext());
// iv.setImageResource(R.drawable.ourhouse2);
// //	iv.setBackgroundColor(Color.RED);
// params = new RelativeLayout.LayoutParams(30, 40);
// params.leftMargin = 80;
// params.topMargin = 90;
// // This line defines how params.leftMargin and params.topMargin are interpreted.
// // In this case, "<80,90>" means <80,90> to the right of the yellow ImageView.
// params.addRule(RelativeLayout.RIGHT_OF, yellow_iv_id);
// iv.setVisibility(View.VISIBLE);
// rl.addView(iv, params);
//getRetainInstance();
// final Resources res = getResources();
// final int k0 = res.getInteger(R.integer.key0);
// final int k1 = res.getInteger(R.integer.key1);
//
// This checks mView and recreates if it is null.  Otherwise
// returns the existing one.
//
// if(savedInstanceState != null)
//     {
// 	Log.i("BG: ",Integer.toString(savedInstanceState.getInt("bg0")));
// 	bulb2.setBackgroundColor(savedInstanceState.getInt("bg0"));//		mEditText.setText(savedInstanceState.getString("textKey"));
//     }
//
//=====================TEST CODE===================================
//


