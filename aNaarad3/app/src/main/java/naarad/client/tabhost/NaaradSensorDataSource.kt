package naarad.client.tabhost

import android.os.SystemClock
import android.util.Log
import org.achartengine.model.XYMultipleSeriesDataset
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException

//public class NaaradSensorDataSource extends AsyncTask<XYMultipleSeriesDataset, XYSeries, String>
class NaaradSensorDataSource
    (
    thisDataset: XYMultipleSeriesDataset?,
    thisRenderer: XYMultipleSeriesRenderer?,
    thisTimeChartView: GraphicalView?
) {
    protected var nodeid: Int = 0
    private var allDone = false
    private var doReDraw = true
    protected var x0: Double = 0.0
    protected var y0: Double = 0.0
    protected var xMax: Double = 0.0
    protected var xMin: Double = 0.0
    protected var yMax: Double = 0.0
    protected var yMin: Double = 100.0
    protected var stopRecording: Boolean = false
    private val nU: NaaradUtils? = null

    private var mDataset: XYMultipleSeriesDataset? = null
    private var mRenderer: XYMultipleSeriesRenderer? = null
    private var mTimeChartView: GraphicalView? = null

    //protected XYSeries thisSeries;
    init {
        mDataset = thisDataset
        mRenderer = thisRenderer
        mTimeChartView = thisTimeChartView
    }

    //
    //----------------------------------------------------------------------------------------------
    // Given the various tokens that together form the JSON
    // string, this assembles, parse and publish the data.
    @Throws(JSONException::class)
    private fun publishJSON(jsonTokens: Array<String?>, params: Array<XYMultipleSeriesDataset?>) {
        var jsonStr: String? = ""
        for (j in 1..2) jsonStr += jsonTokens[j]


        //...publish the data...
        val json = JSONObject(jsonStr)
        if (json.getInt("rf_fail") == 0)  // Indicates that the packet is valid
        {
            nodeid = json.getInt("node_id")
            x0 = json.getDouble("time")
            y0 = json.getDouble("degc").toFloat().toDouble()
            publishProgress(params[0].getSeriesAt(NaaradUtils.nodeID2Ndx.get(nodeid)))
        } else {
            NaaradUtils.uiToast("Packet invalid (rf_fail=1)", Gravity.BOTTOM)
            return
        }
        //mMainActivityCallback.onDataArrival(jsonStr);
    }

    //
    //----------------------------------------------------------------------------------------------
    //
    @Throws(IOException::class, JSONException::class)
    private fun naaradGetDataHistoryEngine(
        keys: IntArray,
        params: Array<XYMultipleSeriesDataset?>,
        mySoc: Socket
    ) {
        val mySocWriter = PrintWriter(mySoc.getOutputStream(), true)
        val mySocReader = BufferedReader(InputStreamReader(mySoc.getInputStream()), 100)
        NaaradUtils.naaradWriter(mySocWriter, "SensorDataSync")

        for (i in keys.indices) {
            val msg = ("gethpkt " + keys[i].toString())
            NaaradUtils.naaradWriter(mySocWriter, msg)
            SystemClock.sleep(100)
            var message: String? = ""
            var msgData: String? = ""
            doReDraw = false
            while (msgData != "PHINISHED") {
                if ((NaaradUtils.naaradReader(mySocReader)
                        .also { message = it }) == null
                ) throw (JSONException("JSON message is null"))
                //SystemClock.sleep(100);
                val tokens: Array<String?> =
                    message!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                msgData = tokens[1]!!.trim { it <= ' ' }
                //System.err.println("Got: "+message+" \""+msgData+"\""+(msgData.equals("PHINISHED")));
                if (msgData != "PHINISHED") publishJSON(tokens, params)
            }
            doReDraw = true
            reDraw()
            //System.err.println("Got all");
        }
        NaaradUtils.naaradWriter(mySocWriter, "done")
        mySoc.close()
        doReDraw = true
    }

    //
    //----------------------------------------------------------------------------------------------
    //
    @Throws(IOException::class, JSONException::class)
    private fun naaradGetDataOnlyEngine(
        keys: IntArray,
        params: Array<XYMultipleSeriesDataset?>,
        mySoc: Socket
    ) {
        val mySocWriter = PrintWriter(mySoc.getOutputStream(), true)
        val mySocReader = BufferedReader(InputStreamReader(mySoc.getInputStream()), 100)
        NaaradUtils.naaradWriter(mySocWriter, "SensorDataSync")

        for (i in keys.indices) {
            val msg = ("getcpkt " + keys[i].toString())
            var message = ""

            NaaradUtils.naaradWriter(mySocWriter, msg)
            SystemClock.sleep(100)
            if ((NaaradUtils.naaradReader(mySocReader)
                    .also { message = it }) == null
            ) throw (JSONException("JSON message is null"))
            //======================================================
            val tokens: Array<String?> =
                message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var jsonStr: String? = ""
            for (j in 1..2) jsonStr += tokens[j]
            publishJSON(tokens, params)
        }


        //...close the socket.
        NaaradUtils.naaradWriter(mySocWriter, "done")
        mySoc.close()
    }

    //
    //----------------------------------------------------------------------------------------------
    //
    private fun naaradGetData(
        keys: IntArray, params: Array<XYMultipleSeriesDataset?>,
        getHistory: Boolean
    ) {
        try {
            // Open the socket connection....
            var mySoc: Socket?
            var tries = 0
            while ((NaaradUtils.naaradSocket(
                    NaaradUtils.getServerName(),
                    NaaradUtils.getServerPort()
                ).also { mySoc = it }) == null &&
                tries < 10
            ) {
                tries++
                val msg = "Wifi not yet connected..." + tries.toString()
                NaaradUtils.uiToast(msg, Gravity.BOTTOM)
                System.err.println(msg)
                SystemClock.sleep(100)
            }
            if (mySoc == null) NaaradUtils.uiToast(
                "Wifi not connected after 10 tries" + tries.toString(),
                Gravity.BOTTOM
            )
            else {
                if (getHistory) naaradGetDataHistoryEngine(keys, params, mySoc)
                else naaradGetDataOnlyEngine(keys, params, mySoc)
            }
        } catch (e: JSONException) {
            //throw new RuntimeException(e);
            System.err.println(e.message)
            NaaradUtils.uiToast(e.message, Gravity.BOTTOM)
            //cancel(true);
        } catch (e: UnknownHostException) {
            val msg =
                "Unknown host: " + NaaradUtils.getServerName() + ":" + NaaradUtils.getServerPort()
                    .toString() + "\nCheck settings"
            NaaradUtils.uiToast(msg, Gravity.BOTTOM)
        } catch (e: IOException) {
            val msg =
                "Error connecting to " + NaaradUtils.getServerName() + ":" + NaaradUtils.getServerPort()
                    .toString() + "\nCheck settings"
            NaaradUtils.uiToast(msg, Gravity.BOTTOM)
        }
    }

    //
    //----------------------------------------------------------------------------------------------
    //
    private fun stopRecording(status: Boolean) {
        stopRecording = status
    }

    //
    //----------------------------------------------------------------------------------------------
    //
    fun finish() {
        allDone = true
    } //
    //----------------------------------------------------------------------------------------------
    //
    //	@Override protected String doInBackground(XYSeries... params) 
    // public String doInBackground(XYMultipleSeriesDataset... params) 
    // 	{
    // 	    String retStr="AllOK";
    // 	    {
    // 		allDone=false;
    // 		int keys[] = nU.getKeysAsArray(nU.nodeID2Ndx);
    // 		naaradGetData(keys,params,true);
    // 		while(true && !allDone)
    // 		    {
    // 			//naaradGetDataOnly(keys,params);
    // 			naaradGetData(keys,params,false);
    // 			SystemClock.sleep(60000);
    // 		    }
    // 	    }

    // 	    return retStr;
    // 	}
    //
    //--------------------------------------------------------------------------
    //    
    fun addNewData(thisSeries: XYSeries, x: Double, y: Double) {
        val xMax: Double
        val xMin: Double
        var dX = 0.0
        val dT = 1000.0 * 60 * 60 * 12
        val n: Int
        n = thisSeries.getItemCount()
        if ((n > 0)) {
            if ((thisSeries.getX(n - 1) - x) >= 300000) thisSeries.add(x, y)
            else {
                if (y != thisSeries.getY(n - 1)) {
                    if (y != MathHelper.NULL_VALUE) {
                        if (y > yMax) yMax = y
                        if (y < yMin) yMin = y
                    }
                    //System.err.println("Val: "+y+" "+yMax+" "+yMin);
                    thisSeries.add(x, y)
                }
            }
        } else thisSeries.add(x, y)
        if (n > 0) {
            xMax = thisSeries.getX(n - 1)
            xMin = thisSeries.getX(0)
            dX = xMax - xMin
            Log.i(
                "xrange0",
                xMax.toString() + " " + xMin.toString() + " " + dX.toString() + " " + n.toString()
            )
            if (dX > dT) thisSeries.remove(0)
        }
    }

    //
    //--------------------------------------------------------------------------
    //    
    protected fun reDraw() {
        if (doReDraw) if ((mTimeChartView != null) && (stopRecording == false)) {
            mTimeChartView.zoomReset()
            mRenderer.setYAxisMax(yMax + 0.2)
            mRenderer.setYAxisMin(yMin - 0.2)
            mTimeChartView.repaint()
        }
    }

    //
    //--------------------------------------------------------------------------
    //    
    //	@Override protected void onProgressUpdate(Integer... values) 
    //@Override protected void onProgressUpdate(XYSeries... values) 
    protected fun publishProgress(vararg values: XYSeries?) {
        //super.onProgressUpdate(values);

        addNewData(values[0], x0, y0)
        reDraw()
    }


    //@Override protected void onCancelled() 
    protected fun phinish() {
        finish()
    }
}
