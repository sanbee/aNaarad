package naarad.client.tabhost

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.common.fill
import com.sanbee.anaarad.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.HashMap
import java.util.Locale
import kotlin.math.roundToLong

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter

class NaaradPlotFragment : NaaradAbstractFragment() {
    private var apiLevel = 0
    var myApp: NaaradApp? = null

    protected var mSensorDataSource: SensorDataSource? = null
    private var nConnected = false
    var mMainActivityCallback: nPlotDataArrivalListener? = null

    data class TelemetryDataPoint(val x: Double, val y: Double)

    private val nodeSeriesData = arrayOf(
        Collections.synchronizedList(ArrayList<TelemetryDataPoint>()),
        Collections.synchronizedList(ArrayList<TelemetryDataPoint>())
    )

    private val modelProducer = CartesianChartModelProducer()
    private val baseTimestampState = mutableStateOf(System.currentTimeMillis().toDouble())

    private val plotUnitState = mutableStateOf("C")
    private var themeModeState by mutableIntStateOf(0)


    companion object {
        private var mRootView: View? = null
        var activeInstance: NaaradPlotFragment? = null
        
        fun newInstanceNPF(sampleText: String?): NaaradPlotFragment {
            val f = NaaradPlotFragment()
            val b = Bundle()
            b.putString("bString", sampleText)
            f.arguments = b
            return f
        }
    }

    interface nPlotDataArrivalListener {
        fun onDataArrival(json: String?)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activeInstance = this
        apiLevel = Build.VERSION.SDK_INT

        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)
        @Suppress("DEPRECATION")
        setRetainInstance(true)

        activity?.let { act ->
            myApp = act.application as? NaaradApp
        }

        val rootView = if (!recreateView(mRootView)) {
            inflater.inflate(R.layout.activity_naarad_plot, container, false).also { mRootView = it }
        } else {
            mRootView ?: inflater.inflate(R.layout.activity_naarad_plot, container, false).also { mRootView = it }
        }

        try {
            mMainActivityCallback = activity as? nPlotDataArrivalListener
        } catch (e: ClassCastException) {
            throw ClassCastException("${activity} must implement mMainActivityCallback")
        }

        plotUnitState.value = getPreference("plotUnit", "C") ?: "C"
        baseTimestampState.value = System.currentTimeMillis().toDouble()

        rootView.findViewById<ComposeView>(R.id.composeView).setContent {
            NaaradChartScreen()
        }

        mSensorDataSource = SensorDataSource()
        nConnected = true
        mSensorDataSource?.execute()

        return rootView
    }

    @Composable
    fun NaaradChartScreen() {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val isNightMode = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isDark = when (themeModeState) {
            1 -> false
            2 -> true
            else -> isNightMode
        }


        val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        val textColor = if (isDark) Color.White else Color.Black
        val gridColor = if (isDark) Color.DarkGray else Color.LightGray
        
        val unit by plotUnitState

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgColor
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Node 1", color = textColor, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF2196F3)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Node 3", color = textColor, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                }

                val bottomAxisValueFormatter = CartesianValueFormatter { _, x, _ ->
                    val timestampMs = baseTimestampState.value + x
                    val date = Date(timestampMs.toLong())
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                    timeFormat.format(date) + "\n" + dateFormat.format(date)
                }
                
                val bottomAxis = HorizontalAxis.rememberBottom(
                    label = rememberAxisLabelComponent(
                        color = textColor,
                        lineCount = 2,
                        textSize = 8.sp
                    ),
                    valueFormatter = bottomAxisValueFormatter,
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                        spacing = { 3 },
                        shiftExtremeLines = false
                    ),
                    guideline = rememberAxisGuidelineComponent(fill = fill(gridColor))
                )
                
                val startAxisValueFormatter = CartesianValueFormatter { _, y, _ ->
                    String.format(Locale.getDefault(), "%.0f", y)
                }

                val startAxis = VerticalAxis.rememberStart(
                    label = rememberAxisLabelComponent(
                        color = textColor,
                        textSize = 8.sp
                    ),
                    valueFormatter = startAxisValueFormatter,
                    guideline = rememberAxisGuidelineComponent(fill = fill(gridColor))
                )

                val customRangeProvider = remember {
                    object : CartesianLayerRangeProvider {
                        override fun getMinX(minX: Double, maxX: Double, extraStore: ExtraStore) = minX
                        override fun getMaxX(minX: Double, maxX: Double, extraStore: ExtraStore) = maxX
                        override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                            val padding = if (maxY == minY) 1.0 else (maxY - minY) * 0.05
                            return minY - padding
                        }
                        override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                            val padding = if (maxY == minY) 1.0 else (maxY - minY) * 0.05
                            return maxY + padding
                        }
                    }
                }

                val lineLayer = rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(LineCartesianLayer.LineFill.single(fill(Color(0xFF4CAF50)))),
                        LineCartesianLayer.rememberLine(LineCartesianLayer.LineFill.single(fill(Color(0xFF2196F3))))
                    ),
                    rangeProvider = customRangeProvider
                )

                val chart = rememberCartesianChart(
                    lineLayer,
                    startAxis = startAxis,
                    bottomAxis = bottomAxis
                )

                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    // Clickable Y-axis label vertically on the immediate left
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.height, placeable.width) {
                                    placeable.place(
                                        x = -(placeable.width / 2 - placeable.height / 2),
                                        y = -(placeable.height / 2 - placeable.width / 2)
                                    )
                                }
                            }
                            .rotate(-90f)
                            .background(Color(0xFFD6D7D7), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .clickable {
                                val newUnit = if (unit == "C") "F" else "C"
                                plotUnitState.value = newUnit
                                setPreference("plotUnit", newUnit)
                                updateVicoChart()
                            }
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Temperature ($unit)",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    CartesianChartHost(
                        chart = chart,
			animationSpec = null,
                        modelProducer = modelProducer,
                        modifier = Modifier.fillMaxSize().weight(1f),
                        scrollState = rememberVicoScrollState(scrollEnabled = true),
                        zoomState = rememberVicoZoomState(zoomEnabled = true, initialZoom = remember { Zoom.Content })
                    )
                }
            }
        }
    }

    private fun updateVicoChart() {
        if (nodeSeriesData[0].isEmpty() && nodeSeriesData[1].isEmpty()) return

        val series0 = synchronized(nodeSeriesData[0]) { nodeSeriesData[0].toList() }
        val series1 = synchronized(nodeSeriesData[1]) { nodeSeriesData[1].toList() }

        val unit = plotUnitState.value
        val convertY: (Double) -> Double = { y ->
            if (unit == "F") y * 9.0 / 5.0 + 32.0 else y
        }

        lifecycleScope.launch(Dispatchers.Default) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = series0.map { it.x },
                        y = series0.map { convertY(it.y) }
                    )
                    series(
                        x = series1.map { it.x },
                        y = series1.map { convertY(it.y) }
                    )
                }
            }
        }
    }

    private fun pruneOldData(seriesList: MutableList<TelemetryDataPoint>) {
        if (seriesList.isEmpty()) return
        val currentMaxX = seriesList.last().x
        val timeRangeSec = NaaradUtils.livePlotTimerange
        val minX = currentMaxX - (timeRangeSec * 1000.0)
        
        synchronized(seriesList) {
            val idx = seriesList.indexOfFirst { it.x >= minX }
            if (idx > 0) {
                seriesList.subList(0, idx).clear()
            }
        }
    }
    
    fun abortSleeperForTimeoutChange() {
        mSensorDataSource?.abortSleeper()
    }

    fun stopDataSource() {
        mSensorDataSource?.finish()
    }


    override fun onResume() {
        super.onResume()
        themeModeState = getPreference("themeMode", 0)
    }

    override fun onDestroyView() {

        super.onDestroyView()
        activeInstance = null
    }

    //
    // Make a wakeup receiver.  Upon wake up reset the ISleep so that
    // the loop in doInBackground() exectures syncData().
    //
    private val screenWakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_USER_PRESENT) {
                Log.d("NaaradPlotFragment", "Device woke up ($action) - resetting sleeper")
                mSensorDataSource?.let { ds ->
                    ds.sleeper.reset()
                    ds.abortSleeper()
                }
            }
        }
    }

    // Register the wakeup receiver
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(screenWakeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            requireContext().registerReceiver(screenWakeReceiver, filter)
        }
    }

    // Un-register the wakeup receiver
    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(screenWakeReceiver)
        } catch (ignored: Exception) {}
    }

    fun parseReading(json: JSONObject): Triple<Double, Double, Int>? {
	var timestamp = json.optDouble("time", -1.0)
	if (timestamp < 0) return null
	if (timestamp > 0 && timestamp < 10_000_000_000.0) {
            timestamp *= 1000.0
	}

	val nodeId = json.optInt("node_id", -1)
	if (nodeId < 0) return null

	val temp = json.optDouble("degc", Double.NaN)
	if (temp.isNaN()) return null

	return Triple(timestamp, temp, nodeId)
    }

    private fun publishJSON(jsonList: ArrayList<String>, startIndex: Int) {
        val map = NaaradUtils.nodeID2NdxMap
	val mapCount = map?.size ?: 0
        var updated = false
	var lastY=-1.0
	
        for (i in startIndex until jsonList.size) {
            val message = jsonList[i]
            if (!message.contains("{")) continue
            val jsonStart = message.indexOf('{')
            val jsonEnd = message.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd <= jsonStart) continue

            try {
                val json = JSONObject(message.substring(jsonStart, jsonEnd + 1))
		var (timestamp, temp, nodeid) = parseReading(json) ?: continue

		if (temp !=  lastY) {
                    val sIndex = map[nodeid]
		    lastY=temp
                    if (sIndex != null && sIndex in 0..mapCount) {
			val relativeX = timestamp - baseTimestampState.value
			// Vico throws 'The x values are too precise' if they exceed 4 decimal places of precision.
			// By forcing relativeX to a whole number, we guarantee no decimal issues during GCD.
			val roundedRelativeX = kotlin.math.round(relativeX)
			val point = TelemetryDataPoint(roundedRelativeX, temp)
			synchronized(nodeSeriesData[sIndex]) {


                            nodeSeriesData[sIndex].add(point)
                            pruneOldData(nodeSeriesData[sIndex])
			}
			updated = true
		    }
                }
            } catch (e: Exception) {
                Log.d("NaaradPlotFragment", "Error parsing JSON: ${e.message}")
            }
        }

    }

    private fun publishJSON(message: String, force: Boolean = false) {
        publishJSON(arrayListOf(message), 0)
        val jsonStart = message.indexOf('{')
        val jsonEnd = message.lastIndexOf('}')
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            val jsonStr = message.substring(jsonStart, jsonEnd + 1)
            try {
                // Pre-validate that it parses correctly before broadcasting to MainActivity
                org.json.JSONObject(jsonStr)
                mMainActivityCallback?.onDataArrival(jsonStr)
            } catch (e: Exception) {
                android.util.Log.e("NaaradPlotFragment", "Failed to parse JSON for callback: $jsonStr")
            }
        }
    }
    
    protected inner class SensorDataSource {
        protected var nodeid: Int = 0
        @Volatile var allDone = false
        private var sensorJob: Job? = null

        val isRunning: Boolean
            get() = !allDone && sensorJob?.isActive == true

        val isCancelled: Boolean
            get() = allDone || sensorJob?.isCancelled == true

        private var doReDraw = true
        protected var x0: Double = 0.0
        protected var y0: Double = 0.0
        protected var stopRecording: Boolean = false
        private var activeSocket: Socket? = null

        val sleeper = iSleep()

        fun abortSleeper() {
            sleeper.abort()
        }

        fun finish() {
            allDone = true
            sleeper.abort()
            sensorJob?.cancel()
            try {
                activeSocket?.close()
            } catch (ignored: Exception) {}
        }

        fun cancel(mayInterruptIfRunning: Boolean) {
            finish()
        }

        fun execute() {
            sensorJob?.cancel()
            sensorJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                doInBackground()
            }
        }

        private fun openNaaradSocket(host: String, port: Int, client: String=""): Socket? {
            val socket = activeSocket ?: naaradSocket(host, port)?.also { activeSocket = it } ?: run {
                Log.d(client, "Error in openNaaradSocket (${NaaradUtils.serverName}, ${NaaradUtils.serverPort})")
                return null
            }
            return socket
        }

        private fun finishNaaradProtocol(mySocWriter: PrintWriter) {
            naaradWriter(mySocWriter, "done")
        }

        private fun closeNaaradSocket() {
            activeSocket?.close()
            activeSocket = null
        }

        fun naaradGetDataHistoryEngine(
            keys: IntArray,
            mySoc: Socket,
            publish: Boolean = true
        ): ArrayList<ArrayList<String>>? {
            val messagesArray = ArrayList<ArrayList<String>>()
            for (k in keys) messagesArray.add(ArrayList())
            val mySocWriter = PrintWriter(mySoc.getOutputStream(), true)
            val mySocReader = BufferedReader(InputStreamReader(mySoc.getInputStream()), 1000)
            activeSocket = mySoc

            naaradWriter(mySocWriter, message = "NaaradApp_GetDataHistoryEngine")

            var nPackets = 0
            for (i in keys.indices) {
                if (allDone || isCancelled) break
                val nodeid = keys[i]
                val request = "gethpkt $nodeid"
                naaradWriter(mySocWriter, request)

                var message = ""
                while (message.isNotEmpty() || !message.contains("PHINISHED")) {
                    if (allDone || isCancelled) break
                    message = NaaradUtils.naaradReader(mySocReader, tag = "gethpkt")
                    if (message.contains("PHINISHED")) {
                        break
                    }
                    if (!message.contains("PHINISHED") && message.contains("{")) {
                        messagesArray[i].add(message)
                        nPackets++
                    }
                }
                
                if (publish && messagesArray[i].isNotEmpty()) {
                    publishJSON(messagesArray[i], 0)
                    
                    // Find the last packet that actually contains a valid JSON payload
                    val lastValidMsg = messagesArray[i].lastOrNull { it.contains("{") }
                    if (lastValidMsg != null) {
                        val jsonStart = lastValidMsg.indexOf('{')
                        val jsonEnd = lastValidMsg.lastIndexOf('}')
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            mMainActivityCallback?.onDataArrival(lastValidMsg.substring(jsonStart, jsonEnd + 1))
                        }
                    }
                }
            }

            if (nPackets > 0 && publish) {
                activity?.runOnUiThread {
                    updateVicoChart()
                }
            }

            try {
                finishNaaradProtocol(mySocWriter)
                closeNaaradSocket()
            } catch (ignored: Exception) {}

            return messagesArray
        }

        private fun naaradGetCurrentDataEngine(
            keys: IntArray,
            mySoc: Socket,
            publish: Boolean = true
        ): ArrayList<String> {
            val messagesList = ArrayList<String>()
            val mySocWriter = PrintWriter(mySoc.getOutputStream(), true)
            val mySocReader = BufferedReader(InputStreamReader(mySoc.getInputStream()), 1000)
            activeSocket = mySoc

            naaradWriter(mySocWriter, message = "NaaradApp_GetCurrentDataEngine")

            for (i in keys.indices) {
                if (allDone || isCancelled) break
                val request = "getcpkt " + keys[i].toString()
                naaradWriter(mySocWriter, request)
                val message = NaaradUtils.naaradReader(mySocReader, tag = "getcpkt")

                if (message.isNotEmpty()) {
                    if (message.contains("{") && publish) publishJSON(message)
                    messagesList.add(message)
                }
            }

            if (messagesList.isNotEmpty() && publish) {
                activity?.runOnUiThread { updateVicoChart() }
            }

            try {
                finishNaaradProtocol(mySocWriter)
                closeNaaradSocket()
            } catch (ignored: Exception) {}

            return messagesList
        }

        private fun naaradGetData(keys: IntArray, getHistory: Boolean) {
            val host = NaaradUtils.serverName
            val port = NaaradUtils.serverPort
            val socket = openNaaradSocket(host, port, "naaradGetData") ?: return
            activeSocket = socket

            try {
                if (getHistory) {
                    naaradGetDataHistoryEngine(keys, socket)
                } else {
                    val messages = naaradGetCurrentDataEngine(keys, socket)
                    for (m in messages) {
                        Log.d("naaradGetDat", m)
                    }
                }
            } catch (e: Exception) {
                Log.d("NaaradPlotFragment", "Error: " + e.message)
            } finally {
                closeNaaradSocket()
            }
        }

        private fun parseTimestampFromMessage(message: String): Long {
            return try {
                val jsonStart = message.indexOf('{')
                val jsonEnd = message.lastIndexOf('}')
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val json = JSONObject(message.substring(jsonStart, jsonEnd + 1))
                    var timestamp = json.optDouble("time", System.currentTimeMillis().toDouble())
                    if (timestamp > 0 && timestamp < 10_000_000_000.0) {
                        timestamp *= 1000.0
                    }
                    timestamp.toLong()
                } else {
                    System.currentTimeMillis()
                }
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        private fun syncData(keys: IntArray) {
            if (keys.isEmpty() || allDone || isCancelled) return
            val host = NaaradUtils.serverName
            val port = NaaradUtils.serverPort

            try {
                val socket = openNaaradSocket(host, port, "syncData") ?: return
                activeSocket = socket
                val messages = naaradGetCurrentDataEngine(keys, socket, publish = false)
                val firstMsg = messages.getOrNull(0) ?: ""

                if (firstMsg.contains("{")) {
                    val latestTimeMs = parseTimestampFromMessage(firstMsg)
                    val firstNode = keys[0]
                    val map = NaaradUtils.nodeID2NdxMap
                    val seriesIndex = map[firstNode] ?: 0

                    val list = synchronized(nodeSeriesData[seriesIndex]) { nodeSeriesData[seriesIndex].toList() }
                    val lastPlotTimeMs = if (list.isNotEmpty()) (list.last().x + baseTimestampState.value).toLong() else 0L

                    val timeDiffMs = latestTimeMs - lastPlotTimeMs
                    var gotHistory = false

                    if (lastPlotTimeMs > 0 && timeDiffMs > NaaradUtils.livePlotReSyncTimescale * 1000L) {
			Log.d("syncData", "Gap of ${"%.2f".format(timeDiffMs / 60000F)}m > "
			      +"${"%.2f".format(NaaradUtils.livePlotReSyncTimescale * 1000L / 60000F)}m "
			      +"for node $firstNode. Fetching history...")

                        closeNaaradSocket()
                        val histSocket = openNaaradSocket(host, port, "syncData") ?: return
                        activeSocket = histSocket
                        val hmessages = naaradGetDataHistoryEngine(keys, histSocket, publish = false)
                        if (hmessages != null) {
                            gotHistory = true
                            for (nodeid in keys) {
                                val sIndex = map[nodeid] ?: 0
                                val nodeHistMessages = hmessages.getOrNull(sIndex) ?: ArrayList()
                                val nodeList = synchronized(nodeSeriesData[sIndex]) { nodeSeriesData[sIndex].toList() }
        
                                if (nodeHistMessages.isNotEmpty()) {
                                    if (nodeList.isNotEmpty()) {
                                        val lastT = (nodeList.last().x + baseTimestampState.value).toLong()
                                        val n = nodeHistMessages.indexOfFirst { msg ->
                                            parseTimestampFromMessage(msg) > lastT
                                        }
                                        if (n != -1) {
                                            publishJSON(nodeHistMessages, n)
                                        }
                                    } else {
                                        for (msg in nodeHistMessages) {
                                            publishJSON(msg, force = true)
                                        }
                                    }
                                }
                            }
                        }

                        activity?.runOnUiThread {
                            updateVicoChart()
                        }
                    }

                    for (m in messages) {
                        if (allDone || isCancelled) break
                        if (m.contains("{")) {
                            publishJSON(m, force = gotHistory)
                        }
                    }
                    activity?.runOnUiThread { updateVicoChart() }
                }
            } catch (e: Exception) {
                Log.d("NaaradPlotFragment", "Error in syncData: ${e.message}")
            } finally {
                try {
                    activeSocket?.getOutputStream()?.let { outStream ->
                        val writer = PrintWriter(outStream, true)
                        finishNaaradProtocol(writer)
                    }
                } catch (ignored: Exception) {}
                closeNaaradSocket()
            }
        }

        fun doInBackground(): String? {
            allDone = false
            nConnected = true
            sleeper.reset()
            val keys = getKeysAsArray(nodeID2NdxMap)

            naaradGetData(keys, getHistory = true)

            while (!allDone) {
                val pingTO: Long = ((NaaradUtils.livePingTimeout) * 1000).roundToLong()
                sleeper.sleep(pingTO)
                sleeper.reset()
                if (allDone) break
                syncData(keys)
            }
            return "AllOK"
        }
    }
}
