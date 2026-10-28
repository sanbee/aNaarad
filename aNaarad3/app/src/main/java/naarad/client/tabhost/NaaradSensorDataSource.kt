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
    @Volatile private var allDone = false
    private var doReDraw = true
    protected var x0: Double = 0.0
    protected var y0: Double = 0.0
    protected var xMax: Double = 0.0
    protected var xMin: Double = 0.0
    protected var yMax: Double = 0.0
    protected var yMin: Double = 100.0
    protected var stopRecording: Boolean = false
    private var activeSocket: Socket? = null

    private var mDataset: XYMultipleSeriesDataset? = thisDataset
    private var mRenderer: XYMultipleSeriesRenderer? = thisRenderer
    private var mTimeChartView: GraphicalView? = thisTimeChartView

    fun finish() {
        allDone = true
        try {
            activeSocket?.close()
        } catch (ignored: Exception) {}
    }

    private fun openSocket(host: String, port: Int): Socket? {
        if (host.isEmpty() || port <= 0) return null
        return try {
            val socket = Socket()
            socket.soTimeout = 3000
            socket.connect(java.net.InetSocketAddress(host, port), 2000)
            if (socket.isConnected) {
                activeSocket = socket
                socket
            } else null
        } catch (e: Exception) {
            null
        }
    }

    @Throws(JSONException::class)
    private fun publishJSON(message: String, params: Array<XYMultipleSeriesDataset?>) {
        val jsonStart = message.indexOf('{')
        val jsonEnd = message.lastIndexOf('}')
        if (jsonStart < 0 || jsonEnd <= jsonStart) return

        val jsonStr = message.substring(jsonStart, jsonEnd + 1)
        val json = JSONObject(jsonStr)
        if (json.optInt("rf_fail", 0) == 0) {
            nodeid = json.optInt("node_id", 1)
            var timestamp = json.optDouble("time", System.currentTimeMillis().toDouble())
            if (timestamp > 0 && timestamp < 10_000_000_000.0) {
                timestamp *= 1000.0
            }
            x0 = timestamp
            y0 = json.optDouble("degc", 0.0)
            val seriesCount = params[0]?.seriesCount ?: 0
            if (seriesCount > 0) {
                val ndx = NaaradUtils.nodeID2Ndx?.get(nodeid) ?: (Math.abs(nodeid) % seriesCount)
                val series = params[0]?.getSeriesAt(ndx)
                if (series != null) {
                    publishProgress(series)
                }
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
        SystemClock.sleep(50)

        for (i in keys.indices) {
            if (allDone) break
            val msg = "gethpkt " + keys[i]
            NaaradUtils.naaradWriter(mySocWriter, msg)
            SystemClock.sleep(50)

            doReDraw = true
            while (!allDone) {
                val message = NaaradUtils.naaradReader(mySocReader) ?: break
                if (message.contains("PHINISHED")) break
                if (message.contains("{")) {
                    publishJSON(message, params)
                }
            }
            reDraw()
        }
        NaaradUtils.naaradWriter(mySocWriter, "done")
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
        SystemClock.sleep(50)

        for (i in keys.indices) {
            if (allDone) break
            val msg = "getcpkt " + keys[i]
            NaaradUtils.naaradWriter(mySocWriter, msg)
            SystemClock.sleep(50)

            val message = NaaradUtils.naaradReader(mySocReader)
            if (message != null && message.contains("{")) {
                publishJSON(message, params)
            }
        }
        NaaradUtils.naaradWriter(mySocWriter, "done")
    }

    private fun naaradGetData(
        keys: IntArray, params: Array<XYMultipleSeriesDataset?>,
        getHistory: Boolean
    ) {
        val host = NaaradUtils.serverName
        val port = NaaradUtils.serverPort
        val mySoc = openSocket(host ?: "", port) ?: return
        try {
            if (getHistory) {
                naaradGetDataHistoryEngine(keys, params, mySoc)
            } else {
                naaradGetDataOnlyEngine(keys, params, mySoc)
            }
        } catch (e: Exception) {
            System.err.println("naaradGetData error: " + e.message)
        } finally {
            try { mySoc.close() } catch (ignored: Exception) {}
            activeSocket = null
        }
    }

    fun addNewData(thisSeries: XYSeries, x: Double, y: Double) {
        if (y == MathHelper.NULL_VALUE) return

        if (yMax == 0.0 || y > yMax) yMax = y
        if (yMin == 100.0 || y < yMin) yMin = y

        thisSeries.add(x, y)

        val n = thisSeries.itemCount
        if (n > 1) {
            val xMaxVal = thisSeries.getX(n - 1)
            val xMinVal = thisSeries.getX(0)
            val dX = xMaxVal - xMinVal
            val dT = 1000.0 * 60 * 60 * 12 // 12 hours
            if (dX > dT) thisSeries.remove(0)
        }
    }

    protected fun reDraw() {
        if (doReDraw && (mTimeChartView != null) && (!stopRecording)) {
            NaaradAbstractFragment.mActivity?.runOnUiThread {
                mTimeChartView?.zoomReset()
                if (yMax > yMin) {
                    mRenderer?.yAxisMax = yMax + 0.5
                    mRenderer?.yAxisMin = yMin - 0.5
                }
                mTimeChartView?.repaint()
            }
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
