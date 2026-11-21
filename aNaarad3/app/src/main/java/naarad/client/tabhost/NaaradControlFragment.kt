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

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sanbee.anaarad.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import naarad.client.tabhost.theme.NaaradTheme
import org.json.JSONObject
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException
import kotlin.math.roundToInt

const val SENSOR_VALUE_COLOR = "black"
const val SENSOR_VALUE_FONT_SIZE = 11f

class NaaradControlFragment : NaaradAbstractFragment() {
    private val controlViewModel: ControlViewModel by lazy {
        ViewModelProvider(this)[ControlViewModel::class.java]
    }
    private var client: Socket? = null
    private var printwriter: PrintWriter? = null
    var myApp: NaaradApp? = null

    private val ALL_WELL = "All well"

    // Compose state collections
    private var themeModeState by mutableIntStateOf(0)
    private var dynamicColorState by mutableStateOf(true)
    private val lampStates = mutableStateListOf(false, false, false)
    private val sensorValues = mutableStateMapOf<Int, Pair<Float, String>>()
    private val bulbPositions = mutableStateListOf(Offset.Zero, Offset.Zero, Offset.Zero)
    private val bubblePositions = mutableStateListOf(Offset.Zero, Offset.Zero)

    private fun loadSavedPositions() {
        val defaultBulbPositions = listOf(
            Offset(395f, 105f),
            Offset(330f, 140f),
            Offset(165f, 50f)
        )
        for (i in 0 until 3) {
            val defaultPos = defaultBulbPositions[i]
            val x = getPreference("bulbX$i", defaultPos.x.toInt())
            val y = getPreference("bulbY$i", defaultPos.y.toInt())
            bulbPositions[i] = Offset(x.toFloat(), y.toFloat())
        }

        val defaultBubblePositions = listOf(
            Offset(285f, 120f),
            Offset(275f, 35f)
        )
        for (i in 0 until 2) {
            val defaultPos = defaultBubblePositions[i]
            val x = getPreference("bubbleX$i", defaultPos.x.toInt())
            val y = getPreference("bubbleY$i", defaultPos.y.toInt())
            bubblePositions[i] = Offset(x.toFloat(), y.toFloat())
        }
    }

    fun setSensorValues(jsonStr: String): TextView? {
        controlViewModel.lastJsonData = jsonStr
        try {
            val json = JSONObject(jsonStr)
            val temp = json.getDouble("degc").toFloat()
            val nodeid = json.getInt("node_id")
            val unit = getPreference("bubbleUnit$nodeid", "C") ?: "C"
            sensorValues[nodeid] = Pair(temp, unit)
        } catch (e: Exception) {
            Log.d("NCF::setSensorValues", "setSensorValues error: " + e.message)
        }
        return null
    }

    private fun toggleLamp(index: Int) {
        if (index !in 0 until 3) return
        val currentOn = lampStates[index]
        val newOn = !currentOn
        lampStates[index] = newOn
        val cmd = "tell $index ${if (newOn) "1" else "0"}"
        sendMessageAsync(cmd)
    }

    private fun toggleBubbleUnit(nodeid: Int) {
        val currentPair = sensorValues[nodeid] ?: Pair(20.0f, "C")
        val newUnit = if (currentPair.second == "C") "F" else "C"
        sensorValues[nodeid] = Pair(currentPair.first, newUnit)
        setPreference("bubbleUnit$nodeid", newUnit)
    }

    fun sendCommand(cmd: String): String? {
        try {
            if (cmd.isEmpty()) return null

            val host = NaaradUtils.serverName
            val port = NaaradUtils.serverPort

            val soc = NaaradUtils.naaradSocket(host, port)
            val pw = PrintWriter(soc.getOutputStream(), true)
            client = soc
            printwriter = pw

            NaaradUtils.naaradWriter(pw, "open")
            Thread.sleep(500)

            NaaradUtils.naaradWriter(pw, cmd)
            Thread.sleep(500)

            NaaradUtils.naaradWriter(pw, "done")

            pw.close()
            soc.close()
            printwriter = null
            client = null
        } catch (e: UnknownHostException) {
            var msg = ""
            if (!isWifiConnected) msg = "Wifi is not connected.\n"
            return msg + "Unknown host: " + NaaradUtils.serverName + ":" + NaaradUtils.serverPort.toString() + "\nCheck settings"
        } catch (e: IOException) {
            return "Error connecting to " + NaaradUtils.serverName + ":" + NaaradUtils.serverPort.toString() + "\nCheck settings"
        }
        return ALL_WELL
    }

    fun resetBulbHighlight(tag: Int) {}

    fun moveIconAndTextTogether(v: View, x: Int, y: Int, w: Int, h: Int) {}

    fun saveSingleViewPosition(v: View, rawX: Int, rawY: Int) {}

    private fun sendMessageAsync(cmd: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = sendCommand(cmd)
            if (result != ALL_WELL) {
                withContext(Dispatchers.Main) {
                    toast(result, Gravity.TOP or Gravity.RIGHT)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        myApp = requireActivity().application as? NaaradApp
        loadSavedPositions()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val systemDark = isSystemInDarkTheme()
                val isDark = when (themeModeState) {
                    1 -> false
                    2 -> true
                    else -> systemDark
                }

                NaaradTheme(
                    darkTheme = isDark,
                    dynamicColor = dynamicColorState
                ) {
                    ControlScreen(
                        lampStates = lampStates,
                        sensorValues = sensorValues,
                        bulbPositions = bulbPositions,
                        bubblePositions = bubblePositions,
                        onLampToggle = { index -> toggleLamp(index) },
                        onBubbleUnitToggle = { nodeid -> toggleBubbleUnit(nodeid) },
                        onBulbPositionChanged = { index, newPos ->
                            bulbPositions[index] = newPos
                            setPreference("bulbX$index", newPos.x.toInt())
                            setPreference("bulbY$index", newPos.y.toInt())
                        },
                        onBubblePositionChanged = { index, newPos ->
                            bubblePositions[index] = newPos
                            setPreference("bubbleX$index", newPos.x.toInt())
                            setPreference("bubbleY$index", newPos.y.toInt())
                        },
                        onDragStart = { myApp?.swipeState = false },
                        onDragEnd = { myApp?.swipeState = true },
                        onLongPress = { toast("DnD activated", Gravity.TOP or Gravity.RIGHT) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        themeModeState = getPreference("themeMode", 0)
        dynamicColorState = getPreference("dynamicColorEnabled", true)
        controlViewModel.lastJsonData?.let { json -> setSensorValues(json) }
        Log.d("NCF::onResume", "From NCF::onResume")
    }

    companion object {
        fun newInstance(sampleText: String?): NaaradControlFragment {
            val f = NaaradControlFragment()
            val b = Bundle()
            b.putString("bString", sampleText)
            f.arguments = b
            return f
        }
    }
}

@Composable
fun ControlScreen(
    lampStates: SnapshotStateList<Boolean>,
    sensorValues: SnapshotStateMap<Int, Pair<Float, String>>,
    bulbPositions: SnapshotStateList<Offset>,
    bubblePositions: SnapshotStateList<Offset>,
    onLampToggle: (Int) -> Unit,
    onBubbleUnitToggle: (Int) -> Unit,
    onBulbPositionChanged: (Int, Offset) -> Unit,
    onBubblePositionChanged: (Int, Offset) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onLongPress: () -> Unit
) {
    var highlightedBulbIndex by remember { mutableStateOf<Int?>(null) }
    var highlightedBubbleIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = R.drawable.ourhouse2),
                contentScale = ContentScale.FillBounds,
                alpha = 0.7f
            )
    ) {
        // Render 3 Lamp Icons + Number Badges
        for (i in 0 until 3) {
            val isLampOn = lampStates.getOrNull(i) == true
            val isHighlighted = highlightedBulbIndex == i
            val pos = bulbPositions.getOrNull(i) ?: Offset.Zero
            val drawableRes = if (isLampOn) R.drawable.lamp_on else R.drawable.lamp_off

            Box(
                modifier = Modifier
                    .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .size(width = 25.dp, height = 25.dp)
                    .background(
                        color = if (isHighlighted) ComposeColor(0xFF22C55E).copy(alpha = 0.6f) else ComposeColor.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .pointerInput(i) {
                        detectTapGestures(
                            onTap = { onLampToggle(i) }
                        )
                    }
                    .pointerInput(i) {
                        var currentX = 0f
                        var currentY = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                currentX = bulbPositions.getOrNull(i)?.x ?: 0f
                                currentY = bulbPositions.getOrNull(i)?.y ?: 0f
                                highlightedBulbIndex = i
                                onLongPress()
                                onDragStart()
                            },
                            onDragEnd = {
                                highlightedBulbIndex = null
                                onDragEnd()
                            },
                            onDragCancel = {
                                highlightedBulbIndex = null
                                onDragEnd()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentX += dragAmount.x
                                currentY += dragAmount.y
                                onBulbPositionChanged(i, Offset(currentX, currentY))
                            }
                        )
                    }
            ) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = "Lamp ${i + 1}",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${i + 1}",
                    style = TextStyle(
                        color = ComposeColor(0xFF795548),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(color = ComposeColor.White, offset = Offset(1f, 1f), blurRadius = 1f)
                    ),
                    modifier = Modifier.offset(x = 12.dp, y = 12.dp)
                )
            }
        }

        // Render Telemetry Sensor Speak-Bubbles for nodes 1 and 3
        val nodes = listOf(1, 3)
        for ((idx, nodeid) in nodes.withIndex()) {
            val sensorData = sensorValues[nodeid]
            val tempVal = sensorData?.first ?: 20.0f
            val unit = sensorData?.second ?: "C"
            val displayTemp = if (unit == "F") tempVal * 9.0f / 5.0f + 32.0f else tempVal
            val textStr = "%.2f%s".format(displayTemp, unit)

            val isHighlighted = highlightedBubbleIndex == idx
            val pos = bubblePositions.getOrNull(idx) ?: Offset.Zero

            Box(
                modifier = Modifier
                    .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .size(width = 48.dp, height = 26.dp)
                    .background(
                        color = if (isHighlighted) ComposeColor(0xFF22C55E).copy(alpha = 0.6f) else ComposeColor.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .paint(
                        painter = painterResource(id = R.drawable.speech_bubble_l_orange),
                        contentScale = ContentScale.FillBounds
                    )
                    .pointerInput(nodeid) {
                        detectTapGestures(
                            onTap = { onBubbleUnitToggle(nodeid) }
                        )
                    }
                    .pointerInput(nodeid) {
                        var currentX = 0f
                        var currentY = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                currentX = bubblePositions.getOrNull(idx)?.x ?: 0f
                                currentY = bubblePositions.getOrNull(idx)?.y ?: 0f
                                highlightedBubbleIndex = idx
                                onLongPress()
                                onDragStart()
                            },
                            onDragEnd = {
                                highlightedBubbleIndex = null
                                onDragEnd()
                            },
                            onDragCancel = {
                                highlightedBubbleIndex = null
                                onDragEnd()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentX += dragAmount.x
                                currentY += dragAmount.y
                                onBubblePositionChanged(idx, Offset(currentX, currentY))
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = textStr,
                    style = TextStyle(
                        color = ComposeColor.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }

        // Right-Hand Lamp Toggle Buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until 3) {
                val isLampOn = lampStates.getOrNull(i) == true
                Button(
                    onClick = { onLampToggle(i) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLampOn) MaterialTheme.colorScheme.primary else ComposeColor(0xFF374151),
                        contentColor = ComposeColor.White
                    )
                ) {
                    Text(
                        text = "Lamp ${i + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ComposeColor.White
                    )
                }
            }
        }
    }
}
