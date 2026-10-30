package naarad.client.tabhost

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.sanbee.anaarad.R
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale

class VerticalTitleLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LineChart(context, attrs, defStyle) {
    var yAxisTitle: String = "Temperature (C)"
    var yAxisTitleColor: Int = Color.WHITE
    var yAxisTitleSize: Float = 10f

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (yAxisTitle.isNotEmpty()) {
            titlePaint.color = yAxisTitleColor
            titlePaint.textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                yAxisTitleSize,
                resources.displayMetrics
            )

            canvas.save()
            val x = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                10f,
                resources.displayMetrics
            )
            val y = height / 2f
            canvas.rotate(-90f, x, y)
            canvas.drawText(yAxisTitle, x, y, titlePaint)
            canvas.restore()
        }
    }
}

class NaaradPlotFragment : NaaradAbstractFragment() {
    private var mLineChart: LineChart? = null

    private var apiLevel = 0
    var myApp: NaaradApp? = null

    protected var mSensorDataSource: SensorDataSource? = null
    private var nConnected = false
    var mMainActivityCallback: nPlotDataArrivalListener? = null

    interface nPlotDataArrivalListener {
        /** Called by NaaradPlotFragment when OTA RF data arrives  */
        fun onDataArrival(json: String?)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        apiLevel = Build.VERSION.SDK_INT

        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)
        @Suppress("DEPRECATION")
        setRetainInstance(true)

        val currentActivity = activity
        if (currentActivity != null) {
            myApp = currentActivity.application as NaaradApp
        }

        if (!recreateView(mView))
            mView = inflater.inflate(R.layout.activity_naarad_plot, container, false)

        try {
            mMainActivityCallback = activity as nPlotDataArrivalListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                activity.toString() + " must implement mMainActivityCallback"
            )
        }

        mLineChart = mView!!.findViewById(R.id.lineChart)
        mLineChart?.let { initChart(it) }

        nConnected = true
        startAllCharts(-1)

        return mView!!
    }

    private fun initChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setBackgroundColor(Color.argb(100, 50, 50, 50))
        chart.legend.textColor = Color.WHITE
        chart.setExtraOffsets(20f, 0f, 0f, 0f)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.DKGRAY
        xAxis.textColor = Color.WHITE
        xAxis.textSize = 10f
        xAxis.valueFormatter = object : ValueFormatter() {
            private val sdf = SimpleDateFormat("HH:mm:ss\ndd/MM", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return try {
                    sdf.format(Date(value.toLong()))
                } catch (e: Exception) {
                    ""
                }
            }
        }

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.DKGRAY
        leftAxis.textColor = Color.WHITE
        leftAxis.textSize = 10f

        chart.axisRight.isEnabled = false

        val dataSet1 = LineDataSet(ArrayList(), "Node 1").apply {
            color = Color.GREEN
            lineWidth = 2.0f
            setDrawCircles(false)
            valueTextColor = Color.TRANSPARENT
            setDrawValues(false)
        }

        val dataSet2 = LineDataSet(ArrayList(), "Node 3").apply {
            color = Color.BLUE
            lineWidth = 2.0f
            setDrawCircles(false)
            valueTextColor = Color.TRANSPARENT
            setDrawValues(false)
        }

        val lineData = LineData(dataSet1, dataSet2)
        chart.data = lineData
        chart.invalidate()
    }

    fun addNewData(seriesIndex: Int, x: Double, y: Double) {
        val chart = mLineChart ?: return
        val data = chart.data ?: return
        val dataSet = data.getDataSetByIndex(seriesIndex) as? LineDataSet ?: return

        val entryCount = dataSet.entryCount
        if (entryCount > 0) {
            val lastEntry = dataSet.getEntryForIndex(entryCount - 1)
            if (x.toFloat() == lastEntry.x && y.toFloat() == lastEntry.y) {
                return
            }
        }

        dataSet.addEntry(Entry(x.toFloat(), y.toFloat()))
        data.notifyDataChanged()
        chart.notifyDataSetChanged()
    }

    fun stopDataSource() {
        if (mSensorDataSource != null) mSensorDataSource!!.finish()
    }

    override fun onDestroy() {
        System.err.println("NPF destroyed")
        super.onDestroy()
        stopDataSource()
    }

    override fun onResume() {
        System.err.println("NPF resumed")
        super.onResume()
    }

    override fun onPause() {
        System.err.println("NPF paused")
        super.onPause()
        stopDataSource()
        if (myApp?.myWakeLock?.isHeld == true) {
            myApp!!.myWakeLock!!.release()
            System.err.println("Releasing wake lock")
        }
    }

    fun startAllCharts(n: Int) {
        if ((n == 0) || (n == -1)) {
            mSensorDataSource = SensorDataSource()

            if (apiLevel <= 9) mSensorDataSource?.execute()
            else mSensorDataSource?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun stopAllCharts(n: Int) {
        mSensorDataSource?.cancel(true)
    }

    protected inner class SensorDataSource :
        AsyncTask<Void?, Void?, String?>() {
        protected var nodeid: Int = 0
        @Volatile private var allDone = false
        private var doReDraw = true
        protected var x0: Double = 0.0
        protected var y0: Double = 0.0
        protected var stopRecording: Boolean = false
        private var activeSocket: Socket? = null

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

        private fun publishJSON(message: String) {
            val jsonStart = message.indexOf('{')
            val jsonEnd = message.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd <= jsonStart) {
                System.err.println("publishJSON: Error in message" + message)
                return
            }

            val jsonStr = message.substring(jsonStart, jsonEnd + 1)
            val json = JSONObject(jsonStr)
            if (json.optInt("rf_fail", 0) == 0) {
                val nodeid: Int = json.optInt("node_id", 1)
                var timestamp = json.optDouble("time", System.currentTimeMillis().toDouble())
                if (timestamp > 0 && timestamp < 10_000_000_000.0) {
                    timestamp *= 1000.0
                }
                x0 = timestamp
                y0 = json.optDouble("degc", 0.0)

                val map: HashMap<Int?, Int?> = NaaradUtils.nodeID2NdxMap
                val seriesIndex: Int = map[nodeid] ?: 0

                val curX = x0
                val curY = y0

                activity?.runOnUiThread {
                    addNewData(seriesIndex, curX, curY)
                    reDrawThrottled()
                    mMainActivityCallback?.onDataArrival(jsonStr)
                }
            } else {
                uiToast("Packet invalid (rf_fail=1)", Gravity.BOTTOM)
                return
            }
        }

        private fun naaradGetDataHistoryEngine(
            keys: IntArray,
            mySoc: Socket
        ) {
            val mySocWriter = PrintWriter(mySoc.getOutputStream(), true)
            val mySocReader = BufferedReader(InputStreamReader(mySoc.getInputStream()), 100)
            naaradWriter(mySocWriter, "SensorDataSync")
            SystemClock.sleep(100)

            var nPackets: Int = 0
            for (i in keys.indices) {
                if (allDone || isCancelled) break
                val msg = "gethpkt " + keys[i].toString()
                Log.i("NaaradData", "=== Requesting Historical Data for Sensor Node ===" + msg)
                System.err.println("=== Requesting Historical Data for Sensor Node ===" + msg)
                naaradWriter(mySocWriter, msg)
                SystemClock.sleep(100)

                doReDraw = true
                var message = ""
                while (!message.contains("PHINISHED")) {
                    message = NaaradUtils.naaradReader(mySocReader)
                    nPackets++
                    if (message.contains("PHINISHED")) {
                        Log.i("NaaradData", "=== Completed Historical Data for Sensor Node ${keys[i]} === " + nPackets)
                        System.err.println("=== Completed Historical Data for Sensor Node ${keys[i]} === " + nPackets)
                    }
                    if (!message.contains("PHINISHED")) {
                        publishJSON(message)
                    }
                }
            }
            if (nPackets > 0) {
                activity?.runOnUiThread {
                    reDrawForce()
                }
            }
            naaradWriter(mySocWriter, "done")
            mySoc.close()
        }

        private fun naaradGetDataOnlyEngine(
            keys: IntArray,
            mySoc: Socket
        ) {
            val mySocWriter = PrintWriter(mySoc.getOutputStream(), true)
            val mySocReader = BufferedReader(InputStreamReader(mySoc.getInputStream()), 100)
            naaradWriter(mySocWriter, "SensorDataSync")
            SystemClock.sleep(100)

            for (i in keys.indices) {
                if (allDone || isCancelled) break
                val msg = "getcpkt " + keys[i]
                naaradWriter(mySocWriter, msg)
                SystemClock.sleep(100)

                val message = naaradReader(mySocReader)
                if (message != null && message.contains("{")) {
                    publishJSON(message)
                }
            }

            naaradWriter(mySocWriter, "done")
            mySoc.close()
        }

        private fun naaradGetData(
            keys: IntArray,
            getHistory: Boolean
        ) {
            val host = NaaradUtils.serverName
            val port = NaaradUtils.serverPort
            val mySoc = openSocket(host ?: "", port) ?: return
            try {
                if (getHistory) naaradGetDataHistoryEngine(keys, mySoc)
                else naaradGetDataOnlyEngine(keys, mySoc)
            } catch (e: Exception) {
                System.err.println("naaradGetData error: " + e.message)
            } finally {
                try {
                    mySoc.close()
                } catch (ignored: Exception) {}
                activeSocket = null
            }
        }

        override fun doInBackground(vararg params: Void?): String? {
            val retStr = "AllOK"
            allDone = false
            nConnected = true
            val keys = getKeysAsArray(nodeID2NdxMap)

            naaradGetData(keys, true)

            while (!allDone) {
                SystemClock.sleep(5000)
                if (!allDone) {
                    naaradGetData(keys, false)
                }
            }

            return retStr
        }

        @Volatile private var lastRedrawTime = 0L

        protected fun reDrawThrottled() {
            if (doReDraw && mLineChart != null && !stopRecording) {
                val now = SystemClock.uptimeMillis()
                if (now - lastRedrawTime >= 100L) {
                    lastRedrawTime = now
                    mLineChart?.data?.notifyDataChanged()
                    mLineChart?.notifyDataSetChanged()
                    mLineChart?.invalidate()
                }
            }
        }

        protected fun reDrawForce() {
            if (doReDraw && mLineChart != null && !stopRecording) {
                mLineChart?.data?.notifyDataChanged()
                mLineChart?.notifyDataSetChanged()
                mLineChart?.invalidate()
            }
        }

        override fun onCancelled() {
            finish()
            super.onCancelled()
        }
    }

    companion object {
        private var mView: View? = null

        fun newInstanceNPF(sampleText: String?): NaaradPlotFragment {
            val f = NaaradPlotFragment()
            val b = Bundle()
            b.putString("bString", sampleText)
            f.setArguments(b)
            return f
        }
    }
}
