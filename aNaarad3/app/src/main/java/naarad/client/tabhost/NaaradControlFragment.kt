package naarad.client.tabhost

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
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
import android.view.View.MeasureSpec
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
    private var bulbText0: TextView? = null
    private var bulbText1: TextView? = null
    private var bulbText2: TextView? = null
    private var bulbTextArr: Array<TextView?> = arrayOfNulls(3)
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

        pX = event.x.toInt()
        pY = event.y.toInt()

        oX = v.height * 2
        oY = v.width
        oX = xsdp * 2
        oY = ysdp
        iX = v.right
        iY = v.top
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
        v.layoutParams = lp
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun setSensorValues(jsonStr: String): TextView? {
        try {
            val json = JSONObject(jsonStr)
            val temp = json.getDouble("degc").toFloat()
            val nodeid = json.getInt("node_id")
            val bubbleID = mapNodeID2Ndx(nodeid)
            if (bubbleID < 0 || bubbleID >= tempBubbleArr.size) return null
            val tv = tempBubbleArr[bubbleID] ?: return null
            val unit = (tv.getTag(R.integer.key1) as? String) ?: "C"
            var displayTemp = temp
            if (unit == "F") displayTemp = displayTemp * 9.0f / 5.0f + 32.0f

            setBubbleValue(nodeid, displayTemp)
            return tv
        } catch (e: Exception) {
            System.err.println("setSensorValues error: " + e.message)
        }
        return null
    }

    fun setBubbleValue(nodeid: Int, temp: Float) {
        class myRunnable : Runnable {
            var thisTV: TextView
            var tmp: Spanned?

            constructor(tv: TextView, text: String?) {
                thisTV = tv
                tmp = Html.fromHtml("<p><b>" + text + "</b></p>")
            }

            override fun run() {
                thisTV.text = tmp
                thisTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                thisTV.setTextColor(Color.parseColor("white"))
                thisTV.background?.alpha = 255
            }
        }

        val bubbleID = mapNodeID2Ndx(nodeid)
        if (bubbleID < 0 || bubbleID >= tempBubbleArr.size) return
        val tv = tempBubbleArr[bubbleID] ?: return

        var text = String.format("%.2f", temp)
        val unit = (tv.getTag(R.integer.key1) as? String) ?: "C"
        text = text + unit

        tv.setTag(R.integer.key0, temp)
        val setTVText = myRunnable(tv, text)
        tv.post(setTVText)
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun setBulbBG(v: View, on: Boolean) {
        val imageView = (v as? ImageView) ?: (v.getTag(R.integer.key0)?.toString()?.toIntOrNull()?.let { bulbArr.getOrNull(it) })
        imageView?.let { img ->
            if (on) {
                img.setImageDrawable(mActivity0!!.resources.getDrawable(R.drawable.lamp_off))
            } else {
                img.setImageDrawable(mActivity0!!.resources.getDrawable(R.drawable.lamp_on))
            }
            img.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun setLampBG(v: View): Boolean {
        val on: Boolean = (v as ToggleButton).isChecked
        if (on) {
            //v.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.lamp_on));
            v.background.alpha = 255
        } else {
            //		v.setBackgroundDrawable(R.drawable.lamp_off);
            v.background.alpha = 128
        }
        return on
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun lampHandler0(v: View) {
        currentToggleButton = (v) as ToggleButton

        //Log.i("Ctrl Server: ", serverName+":"+serverPort);
        val tag = (v.tag as String?)!!.toInt()
        val on = setLampBG(v)
        setBulbBG(bulbArr[tag]!!, !on)
        messsage = "tell " + v.tag + " "

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
            val selItem = gDnDParams!!.selected_item!!
            var tt = (selItem.getTag(R.integer.key0) as Number).toFloat()
            var unit = selItem.getTag(R.integer.key1) as String?
            val nodeid = selItem.tag as Int
            if (unit === "C") {
                tt = tt * 9.0f / 5.0f + 32.0f
                unit = "F"
            } else {
                tt = (tt - 32.0f) * 5.0f / 9.0f
                unit = "C"
            }
            selItem.setTag(R.integer.key1, unit)
            setBubbleValue(nodeid, tt)

            return false
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            toast("DnD activated", Gravity.TOP or Gravity.RIGHT)

            val selItem = gDnDParams!!.selected_item!!
            gDnDParams!!.touchFlag_p = true
            selItem.background?.alpha = 80

            selItem.parent?.requestDisallowInterceptTouchEvent(true)
            myApp!!.swipeState = false

            gDnDParams!!.longPress_x = e.x.toInt()
            gDnDParams!!.longPress_y = e.y.toInt()
            return
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    // Gesture listener with onSingleTapUp() and onLongPress() overloaded.  
    //
    var myGestureListener
            : SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            super.onSingleTapUp(e)
            val selItem = gDnDParams?.selected_item ?: return false
            val tag = (selItem.getTag(R.integer.key0) as? Int) ?: 0
            if (tag >= 0 && tag < lampArr.size && lampArr[tag] != null) {
                val on = lampArr[tag]!!.isChecked
                val bulbView = bulbArr[tag] ?: selItem
                setBulbBG(bulbView, on)
                lampArr[tag]!!.isChecked = !on
                lampHandler0(lampArr[tag]!!)
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            toast("DnD activated", Gravity.TOP or Gravity.RIGHT)
            super.onLongPress(e)
            val selItem = gDnDParams?.selected_item ?: return
            gDnDParams?.touchFlag_p = true

            val tag = (selItem.getTag(R.integer.key0) as? Int) ?: 0
            val bulbView = bulbArr.getOrNull(tag)

            bulbView?.setBackgroundColor(Color.GREEN)
            bulbView?.background?.alpha = 100

            selItem.parent?.requestDisallowInterceptTouchEvent(true)
            myApp?.swipeState = false
            gDnDParams?.longPress_x = e.x.toInt()
            gDnDParams?.longPress_y = e.y.toInt()
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
            lampArr[i]?.tag = i.toString()
            lampArr[i]?.setOnClickListener(lampHandler)
            lampArr[i]?.let { setLampBG(it) }
        }

        // Set up the handlers to handle gestures (SingleTap,
        // LongPress) on the icons for the lamps.
        for (i in bulbArr.indices) {
            bulbArr[i]?.setTag(R.integer.key0, i)
            bulbArr[i]?.setTag(R.integer.key1, "0")
            bulbArr[i]?.setOnTouchListener(onTouchListener)
            bulbTextArr[i]?.setTag(R.integer.key0, i)
            bulbTextArr[i]?.setTag(R.integer.key1, "0")
            bulbTextArr[i]?.setOnTouchListener(onTouchListener)
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
        mActivity0 = activity
        lampArr = arrayOfNulls<ToggleButton>(3)
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


        bulb2 = thisView.findViewById<View?>(R.id.iv3) as ImageView?
        bulbArr[2] = bulb2

        bulbText0 = thisView.findViewById<View?>(R.id.myImageViewText1) as? TextView
        bulbTextArr[0] = bulbText0
        bulbText1 = thisView.findViewById<View?>(R.id.myImageViewText2) as? TextView
        bulbTextArr[1] = bulbText1
        bulbText2 = thisView.findViewById<View?>(R.id.myImageViewText3) as? TextView
        bulbTextArr[2] = bulbText2

        for (i in bulbTextArr.indices) {
            bulbTextArr[i]?.text = "${i + 1}"
            bulbTextArr[i]?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            bulbTextArr[i]?.setTextColor(Color.parseColor("#795548"))
            bulbTextArr[i]?.setTypeface(Typeface.DEFAULT_BOLD)
        }

        tempBubbleArr[0] = thisView.findViewById<View?>(R.id.tv1) as TextView?
        tempBubbleArr[1] = thisView.findViewById<View?>(R.id.tv2) as TextView?

        for (i in tempBubbleArr.indices) {
            tempBubbleArr[i]!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            tempBubbleArr[i]!!.setTag(R.integer.key1, "C")
            //tempBubbleArr[0].setTag(1); // Set the default tag value
            tempBubbleArr[i]!!.tag = keys[i] // Set the default tag value
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
        val oY = myApp!!.dpToPixel(20)
        val oX = oY

        val textOffset = myApp?.dpToPixel(5) ?: 5
        for (i in bulbArr.indices) {
            val bulb = bulbArr[i] ?: continue
            val x = getPreference("bulbX$i", -1)
            val y = getPreference("bulbY$i", -1)
            if (x >= 0 && y >= 0) {
                gDnDParams?.moveView(bulb, x, y, oX, oY, 1.0f, 1.0f)
                bulbTextArr[i]?.let { txt ->
                    gDnDParams?.moveView(txt, x + textOffset, y + textOffset, oX, oY, 1.0f, 1.0f)
                }
            }
        }

        for (i in tempBubbleArr.indices) {
            val tv = tempBubbleArr[i] ?: continue
            tv.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            val width = myApp!!.dpToPixel(45)
            val height = myApp!!.dpToPixel(25)
            val x = getPreference("bubbleX$i", -1)
            val y = getPreference("bubbleY$i", -1)
            if (x >= 0 && y >= 0) {
                gDnDParams?.moveView(tv, x, y, width, height, 1.0f, 1.0f)
            }
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        myApp = requireActivity().application as NaaradApp

        //Log.i("dpi: ",Integer.toString(myApp.densityDpi));
        setRetainInstance(true)
        mActivity0 = activity
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
        mView!!.background.alpha = 100

        return mView!!
    }

    override fun onResume() {
        super.onResume()
        makeSensorIcons(mView!!)
        (mView?.layoutParams as? MarginLayoutParams)?.let { lp ->
            // System.err.println(
            //     "Offsets: " +
            //             lp.leftMargin + " " +
            //             lp.rightMargin
            // )
        }
        getRetainInstance()
        System.err.println("From NCF::onResume")
    }

    fun resetBulbHighlight(tag: Int) {
        if (tag >= 0 && tag < bulbArr.size) {
            bulbArr[tag]?.setBackgroundColor(Color.TRANSPARENT)
            bulbArr[tag]?.background?.alpha = 255
            bulbTextArr.getOrNull(tag)?.setBackgroundColor(Color.TRANSPARENT)
            bulbTextArr.getOrNull(tag)?.background?.alpha = 255
        }
    }

    fun moveIconAndTextTogether(v: View, x: Int, y: Int, w: Int, h: Int) {
        val textOffset = myApp?.dpToPixel(5) ?: 5
        for (i in bulbArr.indices) {
            if (bulbArr[i] == v || bulbTextArr[i] == v) {
                bulbArr[i]?.let { gDnDParams?.moveView(it, x, y, w, h, 1.0f, 1.0f) }
                bulbTextArr[i]?.let { gDnDParams?.moveView(it, x + textOffset, y + textOffset, w, h, 1.0f, 1.0f) }
                return
            }
        }
        gDnDParams?.moveView(v, x, y, w, h, 1.0f, 1.0f)
    }

    fun saveSingleViewPosition(v: View, x: Int, y: Int) {
        val oY = myApp?.dpToPixel(20) ?: 20
        val oX = oY
        val textOffset = myApp?.dpToPixel(5) ?: 5
        for (i in bulbArr.indices) {
            if (bulbArr[i] == v || bulbTextArr[i] == v) {
                setPreference("bulbX$i", x)
                setPreference("bulbY$i", y)
                bulbArr[i]?.let { gDnDParams?.moveView(it, x, y, oX, oY, 1.0f, 1.0f) }
                bulbTextArr[i]?.let { gDnDParams?.moveView(it, x + textOffset, y + textOffset, oX, oY, 1.0f, 1.0f) }
                return
            }
        }
        for (i in tempBubbleArr.indices) {
            if (tempBubbleArr[i] == v) {
                setPreference("bubbleX$i", x)
                setPreference("bubbleY$i", y)
                return
            }
        }
    }

    fun savePositions() {
        for (i in bulbArr.indices) {
            val bulb = bulbArr[i] ?: continue
            val lp = bulb.layoutParams as? MarginLayoutParams
            val x = lp?.leftMargin ?: bulb.left
            val y = lp?.topMargin ?: bulb.top
            if (x >= 0 && y >= 0) {
                setPreference("bulbX$i", x)
                setPreference("bulbY$i", y)
            }
        }
        for (i in tempBubbleArr.indices) {
            val tv = tempBubbleArr[i] ?: continue
            val lp = tv.layoutParams as? MarginLayoutParams
            val x = lp?.leftMargin ?: tv.left
            val y = lp?.topMargin ?: tv.top
            if (x >= 0 && y >= 0) {
                setPreference("bubbleX$i", x)
                setPreference("bubbleY$i", y)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        setRetainInstance(true)
        System.err.println("From NCF::onPause")
        savePositions()
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun sendCommand(cmd: String): String? {
        try {
            if (cmd.length == 0) return null

            val host = NaaradUtils.serverName
            val port = NaaradUtils.serverPort

            client = Socket(host, port)
            printwriter = PrintWriter(client!!.getOutputStream(), true)

            printwriter!!.write(mkMessage("open"))
            printwriter!!.flush()
            SystemClock.sleep(500)

            printwriter!!.write(mkMessage(cmd)) // write the message to output stream
            printwriter!!.flush()
            SystemClock.sleep(500)

            printwriter!!.write(mkMessage("done"))
            printwriter!!.flush()

            printwriter!!.close()
            client!!.close() // closing the connection
        } catch (e: UnknownHostException) {
            var msg = ""
            if (!isWifiConnected) msg = "Wifi is not connected.\n"
            msg =
                msg + "Unknown host: " + NaaradUtils.serverName + ":" + NaaradUtils.serverPort.toString() + "\nCheck settings"
            return msg
        } catch (e: IOException) {
            val msg =
                "Error connecting to " + NaaradUtils.serverName + ":" + NaaradUtils.serverPort.toString() + "\nCheck settings"
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
                currentToggleButton?.let { btn ->
                    val on = btn.isChecked
                    val tag = (btn.tag as String?)!!.toInt()

                    btn.isChecked = !on
                    setBulbBG(bulbArr[tag]!!, on)
                }
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


