package naarad.client.tabhost

import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import org.achartengine.GraphicalView
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.model.XYSeries
import org.achartengine.renderer.XYMultipleSeriesRenderer
import org.achartengine.util.MathHelper
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException

class NaaradSensorDataSource(
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

    private var mDataset: XYMultipleSeriesDataset? = thisDataset
    private var mRenderer: XYMultipleSeriesRenderer? = thisRenderer
    private var mTimeChartView: GraphicalView? = thisTimeChartView

    @Throws(JSONException::class)
    private fun publishJSON(jsonTokens: Array<String?>, params: Array<XYMultipleSeriesDataset?>) {
        var jsonStr: String? = ""
        for (j in 1..2) jsonStr += jsonTokens[j]

        val json = JSONObject(jsonStr)
        if (json.getInt("rf_fail") == 0) {
            nodeid = json.getInt("node_id")
            x0 = json.getDouble("time")
            y0 = json.getDouble("degc").toFloat().toDouble()
            val ndx = NaaradUtils.nodeID2Ndx?.get(nodeid) ?: 0
            val series = params[0]?.getSeriesAt(ndx)
            if (series != null) {
                publishProgress(series)
            }
        } else {
            NaaradUtils.uiToast("Packet invalid (rf_fail=1)", Gravity.BOTTOM)
            return
        }
    }

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
                message = NaaradUtils.naaradReader(mySocReader)
                if (message == null) throw JSONException("JSON message is null")
                val tokens: Array<String?> =
                    message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                msgData = tokens[1]!!.trim { it <= ' ' }
                if (msgData != "PHINISHED") publishJSON(tokens, params)
            }
            doReDraw = true
            reDraw()
        }
        NaaradUtils.naaradWriter(mySocWriter, "done")
        mySoc.close()
        doReDraw = true
    }

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
            var message: String?

            NaaradUtils.naaradWriter(mySocWriter, msg)
            SystemClock.sleep(100)
            message = NaaradUtils.naaradReader(mySocReader)
            if (message == null) throw JSONException("JSON message is null")
            val tokens: Array<String?> =
                message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var jsonStr: String? = ""
            for (j in 1..2) jsonStr += tokens[j]
            publishJSON(tokens, params)
        }

        NaaradUtils.naaradWriter(mySocWriter, "done")
        mySoc.close()
    }

    private fun naaradGetData(
        keys: IntArray, params: Array<XYMultipleSeriesDataset?>,
        getHistory: Boolean
    ) {
        try {
	    var port=NaaradUtils.serverPort
	    var serverName=NaaradUtils.serverName
            var mySoc: Socket? = NaaradUtils.naaradSocket(serverName, port)
            var tries = 0
            while (mySoc == null && tries < 10) {
                tries++
                val msg = "Wifi not yet connected..." + tries.toString()
                NaaradUtils.uiToast(msg, Gravity.BOTTOM)
                System.err.println(msg)
                SystemClock.sleep(100)
                mySoc = NaaradUtils.naaradSocket(NaaradUtils.serverName, NaaradUtils.serverPort)
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
            System.err.println(e.message)
            NaaradUtils.uiToast(e.message ?: "JSONException", Gravity.BOTTOM)
        } catch (e: UnknownHostException) {
            val msg =
                "Unknown host: " + NaaradUtils.serverName + ":" + NaaradUtils.serverPort
                    .toString() + "\nCheck settings"
            NaaradUtils.uiToast(msg, Gravity.BOTTOM)
        } catch (e: IOException) {
            val msg =
                "Error connecting to " + NaaradUtils.serverName + ":" + NaaradUtils.serverPort
                    .toString() + "\nCheck settings"
            NaaradUtils.uiToast(msg, Gravity.BOTTOM)
        }
    }

    private fun stopRecording(status: Boolean) {
        stopRecording = status
    }

    fun finish() {
        allDone = true
    }

    fun addNewData(thisSeries: XYSeries, x: Double, y: Double) {
        val xMaxVal: Double
        val xMinVal: Double
        var dX = 0.0
        val dT = 1000.0 * 60 * 60 * 12
        val n: Int = thisSeries.itemCount
        if (n > 0) {
            if ((thisSeries.getX(n - 1) - x) >= 300000) thisSeries.add(x, y)
            else {
                if (y != thisSeries.getY(n - 1)) {
                    if (y != MathHelper.NULL_VALUE) {
                        if (y > yMax) yMax = y
                        if (y < yMin) yMin = y
                    }
                    thisSeries.add(x, y)
                }
            }
        } else thisSeries.add(x, y)
        if (n > 0) {
            xMaxVal = thisSeries.getX(n - 1)
            xMinVal = thisSeries.getX(0)
            dX = xMaxVal - xMinVal
            Log.i(
                "xrange0",
                xMaxVal.toString() + " " + xMinVal.toString() + " " + dX.toString() + " " + n.toString()
            )
            if (dX > dT) thisSeries.remove(0)
        }
    }

    protected fun reDraw() {
        if (doReDraw && (mTimeChartView != null) && (!stopRecording)) {
            mTimeChartView?.zoomReset()
            mRenderer?.yAxisMax = yMax + 0.2
            mRenderer?.yAxisMin = yMin - 0.2
            mTimeChartView?.repaint()
        }
    }

    protected fun publishProgress(vararg values: XYSeries?) {
        values[0]?.let {
            addNewData(it, x0, y0)
            reDraw()
        }
    }

    protected fun phinish() {
        finish()
    }
}
