package naarad.client.tabhost

import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    public override fun onPause() {
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
        setPreference("serverName", gServerName)
        setPreference("gServerPort", gServerPort)
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

    public override fun onCreateView(
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
        serverPortField?.setText(defaultPort.toString())
        System.err.println("Default Server Name" + defaultServer)
        serverNameField?.setText(defaultServer)

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
            gServerPort = defaultPort
            gServerName =
                serverNameField?.text?.toString()
            try {
                gServerPort = serverPortField?.text?.toString()?.toInt() ?: defaultPort
                if (gServerPort <= 0) throw NumberFormatException("Port number < 0")
                else setButton?.setTextColor(Color.GREEN)
            } catch (nfe: NumberFormatException) {
                val msg = "Wrong Port number: " + nfe.message
                println(msg)
                context?.let { ctx ->
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                }
                setButton?.setTextColor(Color.RED)
            }
            setPreference("serverName", gServerName)
            setPreference("serverPort", gServerPort)
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
