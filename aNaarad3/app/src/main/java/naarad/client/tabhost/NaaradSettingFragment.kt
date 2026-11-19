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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import naarad.client.tabhost.theme.NaaradTheme

class NaaradSettingFragment : NaaradAbstractFragment() {
    var myApp: NaaradApp? = null
    private var wifiTurnedOnByMe = false

    private fun handleWifiControl(isChecked: Boolean) {
        if (isChecked) {
            if (!isWifiConnected) {
                wifiTurnedOnByMe = true
                myApp?.setWifiState(true)
                toast("Turning wifi on...", Gravity.BOTTOM)
            }
            setPreference("wifiControl", 1)
        } else {
            if (isWifiConnected && wifiTurnedOnByMe) {
                myApp?.setWifiState(false)
            }
            toast("Turning wifi off...", Gravity.BOTTOM)
            setPreference("wifiControl", 0)
        }
    }

    private fun handleWakeLock(isChecked: Boolean) {
        if (isWifiConnected) {
            if (isChecked) {
                if (myApp?.myWakeLock?.isHeld == false) myApp?.myWakeLock?.acquire()
                if (myApp?.myWifiLock?.isHeld == false) myApp?.myWifiLock?.acquire()
                toast("Wake and Wifi locks set.", Gravity.BOTTOM)
                setPreference("wakeControl", 1)
            } else {
                if (myApp?.myWakeLock?.isHeld == true) myApp?.myWakeLock?.release()
                if (myApp?.myWifiLock?.isHeld == true) myApp?.myWifiLock?.release()
                setPreference("wakeControl", 0)
            }
        } else {
            setPreference("wakeControl", 0)
            toast("Wifi not connected.\nWake and Wifi locks not set.", Gravity.BOTTOM)
        }
    }

    private fun saveSettings(
        name: String,
        portStr: String,
        pingTO: String,
        plotRange: String,
        reSyncScale: String,
        wifiOn: Boolean,
        wakeOn: Boolean
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isNotEmpty()) {
            NaaradUtils.serverName = trimmedName
        }

        val port = portStr.trim().toIntOrNull() ?: NaaradUtils.serverPort
        if (port > 0) {
            NaaradUtils.serverPort = port
        }

        val secPing = NaaradUtils.parseHmsToSeconds(pingTO)
        if (secPing > 0.0) {
            NaaradUtils.currentPingTimeoutStr = pingTO
        }

        val secRange = NaaradUtils.parseHmsToSeconds(plotRange)
        if (secRange > 0.0) {
            NaaradUtils.currentPlotTimerangeStr = plotRange
        }

        val secScale = NaaradUtils.parseHmsToSeconds(reSyncScale)
        if (secScale > 0.0) {
            NaaradUtils.currentPlotReSyncTimescaleStr = reSyncScale
        }

        handleWifiControl(wifiOn)
        handleWakeLock(wakeOn)

        toast("Settings saved: ${NaaradUtils.serverName}:${NaaradUtils.serverPort}", Gravity.BOTTOM)
    }

    override fun onPause() {
        super.onPause()
        var giveMsg = false
        if (myApp?.myWakeLock?.isHeld == true) {
            myApp?.myWakeLock?.release()
            giveMsg = true
        }
        if (myApp?.myWifiLock?.isHeld == true) {
            myApp?.myWifiLock?.release()
            giveMsg = true
        }
        if (giveMsg) toast("Wake and Wifi locks released.", Gravity.BOTTOM)

        if (wifiTurnedOnByMe) {
            myApp?.setWifiState(false)
            toast("Turning off wifi...", Gravity.BOTTOM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        myApp = requireActivity().application as? NaaradApp
        myApp?.makeLocks()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NaaradTheme {
                    SettingsScreen(
                        initialServerName = NaaradUtils.serverName,
                        initialServerPort = NaaradUtils.serverPort.toString(),
                        initialPingTimeout = NaaradUtils.currentPingTimeoutStr,
                        initialPlotTimerange = NaaradUtils.currentPlotTimerangeStr,
                        initialReSyncTimescale = NaaradUtils.currentPlotReSyncTimescaleStr,
                        initialWifiControl = getPreference("wifiControl", 0) == 1,
                        initialWakeControl = getPreference("wakeControl", 0) == 1,
                        onSaveSettings = { name, portStr, pingTO, plotRange, reSyncScale, wifiOn, wakeOn ->
                            saveSettings(name, portStr, pingTO, plotRange, reSyncScale, wifiOn, wakeOn)
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstanceNSF(sampleText: String?): NaaradSettingFragment {
            val f = NaaradSettingFragment()
            val b = Bundle()
            b.putString("bString", sampleText)
            f.arguments = b
            return f
        }
    }
}

@Composable
fun SettingsScreen(
    initialServerName: String,
    initialServerPort: String,
    initialPingTimeout: String,
    initialPlotTimerange: String,
    initialReSyncTimescale: String,
    initialWifiControl: Boolean,
    initialWakeControl: Boolean,
    onSaveSettings: (String, String, String, String, String, Boolean, Boolean) -> Unit
) {
    var serverName by remember { mutableStateOf(initialServerName) }
    var serverPort by remember { mutableStateOf(initialServerPort) }
    var pingTimeout by remember { mutableStateOf(initialPingTimeout) }
    var plotTimerange by remember { mutableStateOf(initialPlotTimerange) }
    var reSyncTimescale by remember { mutableStateOf(initialReSyncTimescale) }
    var wifiControl by remember { mutableStateOf(initialWifiControl) }
    var wakeControl by remember { mutableStateOf(initialWakeControl) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Naarad Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Server IP") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { serverPort = it },
                    label = { Text("Port") },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    onClick = {
                        onSaveSettings(
                            serverName,
                            serverPort,
                            pingTimeout,
                            plotTimerange,
                            reSyncTimescale,
                            wifiControl,
                            wakeControl
                        )
                    }
                ) {
                    Text("Set")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = wifiControl,
                    onCheckedChange = { wifiControl = it }
                )
                Text(
                    text = "Turn wifi on/off?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = wakeControl,
                    onCheckedChange = { wakeControl = it }
                )
                Text(
                    text = "Keep CPU + WiFi on in the background?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            OutlinedTextField(
                value = pingTimeout,
                onValueChange = { newValue ->
                    pingTimeout = newValue
                    val sec = NaaradUtils.parseHmsToSeconds(newValue)
                    if (sec > 0.0) {
                        NaaradUtils.currentPingTimeoutStr = newValue
                    }
                },
                label = { Text("Ping Timeout (e.g. 1m, 30s)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            OutlinedTextField(
                value = plotTimerange,
                onValueChange = { newValue ->
                    plotTimerange = newValue
                    val sec = NaaradUtils.parseHmsToSeconds(newValue)
                    if (sec > 0.0) {
                        NaaradUtils.currentPlotTimerangeStr = newValue
                    }
                },
                label = { Text("Plot Timerange (e.g. 2d, 12h)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            OutlinedTextField(
                value = reSyncTimescale,
                onValueChange = { newValue ->
                    reSyncTimescale = newValue
                    val sec = NaaradUtils.parseHmsToSeconds(newValue)
                    if (sec > 0.0) {
                        NaaradUtils.currentPlotReSyncTimescaleStr = newValue
                    }
                },
                label = { Text("ReSync Timescale (e.g. 30m, 1h)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }
    }
}
