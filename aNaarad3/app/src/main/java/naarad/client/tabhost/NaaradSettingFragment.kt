package naarad.client.tabhost

import android.content.Context
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import com.sanbee.anaarad.R
import java.io.PrintWriter

class NaaradSettingFragment : NaaradAbstractFragment() {
    private val printwriter: PrintWriter? = null
    private var serverNameField: EditText? = null
    private var serverPortField: EditText? = null
    private var pingTimeoutField: EditText? = null
    private var setButton: Button? = null
    private var wakeButton: CheckBox? = null
    private var wifiControl: CheckBox? = null
    var myApp: NaaradApp? = null

    private val message: String? = null
    private var wifiTurnedOnByMe = false
    private var gWifiControl = 0
    private var gWakeLockControl = 0
    private var gServerPort = 0
    private var gServerName: String? = null

    private fun saveSettingsFromFields() {
        val name = serverNameField?.text?.toString()?.trim()
        if (!name.isNullOrEmpty()) {
            gServerName = name
            serverName = name
        }
        val portStr = serverPortField?.text?.toString()?.trim()
        try {
            val port = portStr?.toInt() ?: defaultPort
            if (port > 0) {
                gServerPort = port
                serverPort = port
            }
        } catch (nfe: NumberFormatException) {
            // keep existing port
        }
        validateAndUpdatePingTimeout()
    }

    private fun hideKeyboard(v: View) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun validateAndUpdatePingTimeout() {
        val inputStr = pingTimeoutField?.text?.toString()?.trim()
        val numVal = inputStr?.toDoubleOrNull()
        if (numVal != null && numVal > 0.0) {
            val oldVal = NaaradUtils.currentPingTimeout
            NaaradUtils.currentPingTimeout = numVal
            pingTimeoutField?.setText(numVal.toString())
            if (oldVal != numVal) {
                toast("Setting saved: Ping timeout = $numVal sec", Gravity.BOTTOM)
            }
        } else {
            val lastValid = NaaradUtils.currentPingTimeout
            pingTimeoutField?.setText(lastValid.toString())
        }
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

        setPreference("wifiControl", gWifiControl)
        setPreference("wakeControl", gWakeLockControl)
        if (wifiTurnedOnByMe) {
            myApp?.setWifiState(false)
            toast("Turning off wifi...", Gravity.BOTTOM)
        }
    }

    fun wifiControlClick(v: CheckBox) {
        if (v.isChecked) {
            if (!isWifiConnected) {
                wifiTurnedOnByMe = true
                myApp?.setWifiState(true)

                toast("Turning wifi on...", Gravity.BOTTOM)

                val nWifiReceiver = NaaradWifiBroadcastReceiver()

                val filters = IntentFilter()
                filters.addAction("android.net.wifi.WIFI_STATE_CHANGED")
            }
            gWifiControl = 1
        } else {
            if (isWifiConnected && wifiTurnedOnByMe) myApp?.setWifiState(false)
            toast("Turning wifi off...", Gravity.BOTTOM)
            gWifiControl = 0
        }
    }

    fun wakeLockClick(v: CheckBox) {
        if (isWifiConnected) {
            if (v.isChecked) {
                if (myApp?.myWakeLock?.isHeld == false) myApp?.myWakeLock?.acquire()
                if (myApp?.myWifiLock?.isHeld == false) myApp?.myWifiLock?.acquire()
                toast("Wake and Wifi locks set.", Gravity.BOTTOM)
                gWakeLockControl = 1
            } else {
                if (myApp?.myWakeLock?.isHeld == true) myApp?.myWakeLock?.release()
                if (myApp?.myWifiLock?.isHeld == true) myApp?.myWifiLock?.release()
                gWakeLockControl = 0
            }
        } else {
            v.isChecked = false
            gWakeLockControl = 0
            toast("Wifi not connected.\nWake and Wifi locks not set.", Gravity.BOTTOM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setRetainInstance(true)
        myApp = requireActivity().application as NaaradApp
        myApp?.makeLocks()

        if (mView == null) mView = inflater.inflate(
            R.layout.activity_naarad_setting,
            container, false
        )
        else (mView?.parent as? ViewGroup)?.removeView(mView)

        serverNameField =
            mView?.findViewById<View?>(R.id.serverName) as? EditText
        serverPortField =
            mView?.findViewById<View?>(R.id.serverPort) as? EditText

        // Pre-populate fields with remembered serverName and serverPort
        val savedServerName = serverName ?: defaultServer
        val savedServerPort = serverPort

        gServerName = savedServerName
        gServerPort = savedServerPort

        serverNameField?.setText(savedServerName)
        serverPortField?.setText(savedServerPort.toString())

        // Save immediately as user types
        serverNameField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = s?.toString()?.trim()
                if (!name.isNullOrEmpty()) {
                    gServerName = name
                    serverName = name
                }
            }
        })

        serverPortField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    val port = s?.toString()?.trim()?.toInt() ?: defaultPort
                    if (port > 0) {
                        gServerPort = port
                        serverPort = port
                        setButton?.setTextColor(Color.GREEN)
                    }
                } catch (e: NumberFormatException) {
                    setButton?.setTextColor(Color.RED)
                }
            }
        })

        val pingId = resources.getIdentifier("pingTimeout", "id", requireContext().packageName)
        if (pingId != 0) {
            pingTimeoutField = mView?.findViewById(pingId)
        }
        pingTimeoutField?.setText(NaaradUtils.livePingTimeout.toString())

        pingTimeoutField?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                validateAndUpdatePingTimeout()
                hideKeyboard(v)
                v.clearFocus()
                true
            } else {
                false
            }
        }

        pingTimeoutField?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndUpdatePingTimeout()
            }
        }

        setButton =
            mView?.findViewById<View?>(R.id.setButton) as? Button

        wifiControl = mView?.findViewById<View?>(R.id.wifiControl) as? CheckBox
        wakeButton = mView?.findViewById<View?>(R.id.wake) as? CheckBox
        wakeButton?.isChecked = false

        if ((getPreference("wifiControl", 0).also { gWifiControl = it }) == 1) {
            wifiControl?.isChecked = true
            wifiControl?.let { wifiControlClick(it) }
        }
        wifiControl?.setOnClickListener {
            (it as? CheckBox)?.let { cb -> wifiControlClick(cb) }
        }

        if ((getPreference("wakeControl", 0).also { gWakeLockControl = it }) == 1) {
            wakeButton?.isChecked = true
            wakeButton?.let { wakeLockClick(it) }
        }

        wakeButton?.setOnClickListener {
            (it as? CheckBox)?.let { cb -> wakeLockClick(cb) }
        }

        setButton?.setOnClickListener {
            saveSettingsFromFields()
            setButton?.setTextColor(Color.GREEN)
            toast("Settings saved: $gServerName:$gServerPort", Gravity.BOTTOM)
        }
        return mView
    }

    companion object {
        private var mView: View? = null

        fun newInstanceNSF(sampleText: String?): NaaradSettingFragment {
            val f = NaaradSettingFragment()

            val b = Bundle()
            b.putString("bString", sampleText)
            f.arguments = b

            return f
        }
    }
}
