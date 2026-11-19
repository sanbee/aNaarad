/*
 * Copyright (C) 2026 Sanjay Bhatnagar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should obtain a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package naarad.client.tabhost

import android.util.Log
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
import android.os.Build
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import org.json.JSONException
import org.json.JSONObject
import com.sanbee.anaarad.R
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SENSOR_VALUE_COLOR = "black"
const val SENSOR_VALUE_FONT_SIZE = 11f

//public class NaaradControlFragment extends Fragment implements View.OnLongClickListener 
class NaaradControlFragment : NaaradAbstractFragment() //implements OnTouchListener
{
    private val controlViewModel: ControlViewModel by lazy {
        ViewModelProvider(this)[ControlViewModel::class.java]
    }
//    private var client: Socket? = null
//    private var printwriter: PrintWriter? = null
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
        val densityDpi = myApp?.densityDpi ?: 160
        xsdp = (xSizeDP * densityDpi.toDouble() / 160.0).toInt()
        ysdp = (ySizeDP * densityDpi.toDouble() / 160.0).toInt()

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

        // Log.d("NCF::resizeView()","M Display Here X Value-->"+(x)+" "+pX+" "+oX+" "+iX);
        // Log.d("NCF::resizeView()","M Display Here Y Value-->"+(y)+" "+pY+" "+oY+" "+iY);
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
        controlViewModel.lastJsonData = jsonStr
        try {
            val json = JSONObject(jsonStr)
            val temp = json.getDouble("degc").toFloat()
            val nodeid = json.getInt("node_id")
            val bubbleID = mapNodeID2Ndx(nodeid)
            if (bubbleID < 0 || bubbleID >= tempBubbleArr.size) return null
            val tv = tempBubbleArr[bubbleID] ?: return null
            val unit = getPreference("bubbleUnit$nodeid", "C") ?: "C"
            tv.setTag(R.integer.key1, unit)
            var displayTemp = temp
            if (unit == "F") displayTemp = displayTemp * 9.0f / 5.0f + 32.0f

            setBubbleValue(nodeid, displayTemp)
            return tv
        } catch (e: Exception) {
            Log.d("NCF::setSensorValues","setSensorValues error: " + e.message)
        }
        return null
    }

    fun setBubbleValue(nodeid: Int, temp: Float) {
        class myRunnable : Runnable {
            var thisTV: TextView
            var tmp: Spanned?

            constructor(tv: TextView, text: String?) {
                thisTV = tv
                @Suppress("DEPRECATION")
                tmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml("<p><b>$text</b></p>", Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml("<p><b>$text</b></p>")
                }
            }

            override fun run() {
                thisTV.text = tmp
                thisTV.gravity = Gravity.CENTER
                thisTV.setPadding(0, 0, 0, 6)
                thisTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, SENSOR_VALUE_FONT_SIZE)
                thisTV.setTextColor(Color.parseColor(SENSOR_VALUE_COLOR))
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
            val res = mActivity0?.resources ?: activity?.resources
            if (res != null) {
                val drawableId = if (on) R.drawable.lamp_off else R.drawable.lamp_on
                val drawable = ResourcesCompat.getDrawable(res, drawableId, null)
                img.setImageDrawable(drawable)
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
        currentToggleButton = v as? ToggleButton

        val tag = (v.tag as? String)?.toIntOrNull() ?: 0
        val on = setLampBG(v)
        bulbArr.getOrNull(tag)?.let { bulb ->
            setBulbBG(bulb, !on)
        }
        val tagStr = v.tag?.toString() ?: ""
        messsage = "tell $tagStr " + (if (on) "1" else "0")

        messsage?.let { msg ->
            sendMessageAsync(msg)
        }
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
            val selItem = (gDnDParams?.selected_item as? TextView) ?: return false

            val textStr = selItem.text?.toString() ?: ""
            val nodeid = (selItem.tag as? Number)?.toInt()
                ?: (selItem.tag?.toString()?.toIntOrNull() ?: 1)

            var unit = (selItem.getTag(R.integer.key1) as? String)
                ?: getPreference("bubbleUnit$nodeid", "C") ?: "C"

            val numStr = textStr.replace(Regex("[^0-9.-]"), "")
            var tt = numStr.toFloatOrNull()
                ?: ((selItem.getTag(R.integer.key0) as? Number)?.toFloat() ?: 20.0f)

            if (unit == "C") {
                tt = tt * 9.0f / 5.0f + 32.0f
                unit = "F"
            } else {
                tt = (tt - 32.0f) * 5.0f / 9.0f
                unit = "C"
            }
            selItem.setTag(R.integer.key1, unit)
            setPreference("bubbleUnit$nodeid", unit)
            setBubbleValue(nodeid, tt)

            return false
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            toast("DnD activated", Gravity.TOP or Gravity.RIGHT)

            val params = gDnDParams ?: return
            val selItem = params.selected_item ?: return
            params.touchFlag_p = true
            selItem.background?.alpha = 80

            val lp = selItem.layoutParams as? MarginLayoutParams
            params.initial_left = if (lp != null && lp.leftMargin > 0) lp.leftMargin else selItem.left
            params.initial_top = if (lp != null && lp.topMargin > 0) lp.topMargin else selItem.top
            params.raw_touch_x0 = e.rawX
            params.raw_touch_y0 = e.rawY

            selItem.parent?.requestDisallowInterceptTouchEvent(true)
            myApp?.swipeState = false

            params.longPress_x = e.x.toInt()
            params.longPress_y = e.y.toInt()
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
            val lampBtn = lampArr.getOrNull(tag)
            if (lampBtn != null) {
                val on = lampBtn.isChecked
                val bulbView = bulbArr.getOrNull(tag) ?: selItem
                setBulbBG(bulbView, on)
                lampBtn.isChecked = !on
                lampHandler0(lampBtn)
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

            val lp = selItem.layoutParams as? MarginLayoutParams
            gDnDParams?.initial_left = if (lp != null && lp.leftMargin > 0) lp.leftMargin else selItem.left
            gDnDParams?.initial_top = if (lp != null && lp.topMargin > 0) lp.topMargin else selItem.top
            gDnDParams?.raw_touch_x0 = e.rawX
            gDnDParams?.raw_touch_y0 = e.rawY

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
    private fun makeHandlers(activity: Activity?) {
        gLampHandler = View.OnClickListener { v ->
            lampHandler0(v)
        }

        val act = activity ?: mActivity0 ?: this.activity
        val dnd = gDnDParams ?: NaaradDnDParameters().also { gDnDParams = it }
        val app = myApp ?: (act?.application as? NaaradApp)

        if (act != null && app != null) {
            val gd = GestureDetector(act, myGestureListener).also { gGestureDetector = it }
            gOnTouchListener = NaaradDnDOnTouchListener(gd, dnd, app, 1.0f, 1.0f, 20, 20)

            val bgd = GestureDetector(act, bubbleGestureListener).also { gBubbleGestureListener = it }
            gBubbleOnTouchListener = NaaradDnDOnTouchListener(bgd, dnd, app, 1.0f, 1.0f, 45, 25)
        }
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

        val map = nodeID2Ndx ?: NaaradUtils.nodeID2NdxMap
        val keys = getKeysAsArray(map)

        val nBubbles = map.size

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
            val tv = tempBubbleArr[i] ?: continue
            val nodeid = keys[i]
            val savedUnit = getPreference("bubbleUnit$nodeid", "C") ?: "C"
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, SENSOR_VALUE_FONT_SIZE)
            tv.setTag(R.integer.key1, savedUnit)
            tv.setTag(R.integer.key0, 20.0f)
            tv.tag = nodeid
            tv.setTextColor(Color.parseColor(SENSOR_VALUE_COLOR))
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
        val oY = myApp?.dpToPixel(20) ?: 20
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
            val width = myApp?.dpToPixel(45) ?: 45
            val height = myApp?.dpToPixel(25) ?: 25
            val x = getPreference("bubbleX$i", -1)
            val y = getPreference("bubbleY$i", -1)
            if (x >= 0 && y >= 0) {
                gDnDParams?.moveView(tv, x, y, width, height, 1.0f, 1.0f)
            }
        }

        makeHandlers(mActivity0)
        installHandlers(gLampHandler, gOnTouchListener, lampArr, bulbArr)
        tempBubbleArr.getOrNull(0)?.setOnTouchListener(gBubbleOnTouchListener)
        tempBubbleArr.getOrNull(1)?.setOnTouchListener(gBubbleOnTouchListener)
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        myApp = requireActivity().application as? NaaradApp

        mActivity0 = activity
        gDnDParams = NaaradDnDParameters()

        val view = if (recreateView(mView)) mView else null
        val rootView = view ?: inflater.inflate(R.layout.activity_naarad_control, container, false).also { mView = it }

        makeSensorIcons(rootView)
        rootView.background?.alpha = 100

        return rootView
    }

    override fun onResume() {
        super.onResume()
        mView?.let { view -> makeSensorIcons(view) }
        controlViewModel.lastJsonData?.let { json -> setSensorValues(json) }
        Log.d("NCF::onResume","From NCF::onResume")
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

    fun saveSingleViewPosition(v: View, rawX: Int, rawY: Int) {
        val oY = myApp?.dpToPixel(20) ?: 20
        val oX = oY
        val textOffset = myApp?.dpToPixel(5) ?: 5
        for (i in bulbArr.indices) {
            if (bulbArr[i] == v) {
                setPreference("bulbX$i", rawX)
                setPreference("bulbY$i", rawY)
                bulbArr[i]?.let { gDnDParams?.moveView(it, rawX, rawY, oX, oY, 1.0f, 1.0f) }
                bulbTextArr[i]?.let { gDnDParams?.moveView(it, rawX + textOffset, rawY + textOffset, oX, oY, 1.0f, 1.0f) }
                return
            } else if (bulbTextArr[i] == v) {
                val bulbX = rawX - textOffset
                val bulbY = rawY - textOffset
                setPreference("bulbX$i", bulbX)
                setPreference("bulbY$i", bulbY)
                bulbArr[i]?.let { gDnDParams?.moveView(it, bulbX, bulbY, oX, oY, 1.0f, 1.0f) }
                bulbTextArr[i]?.let { gDnDParams?.moveView(it, rawX, rawY, oX, oY, 1.0f, 1.0f) }
                return
            }
        }
        for (i in tempBubbleArr.indices) {
            if (tempBubbleArr[i] == v) {
                setPreference("bubbleX$i", rawX)
                setPreference("bubbleY$i", rawY)
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
        Log.d("NCF::onPause","From NCF::onPause")
        savePositions()
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun sendCommand(cmd: String): String? {
        try {
            if (cmd.isEmpty()) return null

            val host = NaaradUtils.serverName
            val port = NaaradUtils.serverPort

            val soc = NaaradUtils.naaradSocket(host, port)
            val pw = PrintWriter(soc.getOutputStream(), true)
//            client = soc
//            printwriter = pw

            NaaradUtils.naaradWriter(pw, "open")
            SystemClock.sleep(500)

            NaaradUtils.naaradWriter(pw, cmd)
            SystemClock.sleep(500)

            NaaradUtils.naaradWriter(pw, "done")

            pw.close()
            soc.close()
//            printwriter = null
//            client = null
        } catch (e: UnknownHostException) {
            var msg = ""
            if (!isWifiConnected) msg = "Wifi is not connected.\n"
            return msg + "Unknown host: " + NaaradUtils.serverName + ":" + NaaradUtils.serverPort.toString() + "\nCheck settings"
        } catch (e: IOException) {
            return "Error connecting to " + NaaradUtils.serverName + ":" + NaaradUtils.serverPort.toString() + "\nCheck settings"
        }
        return ALL_WELL
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    private fun sendMessageAsync(cmd: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = sendCommand(cmd)
            if (result != ALL_WELL) {
                withContext(Dispatchers.Main) {
                    toast(result, Gravity.TOP or Gravity.RIGHT)
                    currentToggleButton?.let { btn ->
                        val on = btn.isChecked
                        val tag = (btn.tag as? String)?.toIntOrNull() ?: 0

                        btn.isChecked = !on
                        bulbArr.getOrNull(tag)?.let { bulb ->
                            setBulbBG(bulb, on)
                        }
                    }
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


