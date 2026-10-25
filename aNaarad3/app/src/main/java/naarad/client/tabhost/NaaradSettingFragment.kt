package naarad.client.tabhost

//import android.widget.CheckedTextView;
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import java.io.PrintWriter

//public class NaaradSettingFragment extends Fragment 
class NaaradSettingFragment : NaaradAbstractFragment() {
    // private SharedPreferences prefs; 
    private val printwriter: PrintWriter? = null
    private var serverNameField: EditText? = null
    private var serverPortField: EditText? = null
    private var setButton: Button? = null
    private var wakeButton: CheckBox? = null
    private var wifiControl: CheckBox? = null
    var myApp: NaaradApp? = null

    //private TextView wakeText;
    //    private CheckedTextView ctView;
    private val message: String? = null
    private var wifiTurnedOnByMe = false
    private var gWifiControl = 0
    private var gWakeLockControl = 0
    private var gServerPort = 0
    private var gServerName: String? = null

    //
    //-----------------------------------------------------------------------------------------
    //
    public override fun onPause() {
        super.onPause()
        var giveMsg = false
        if (myApp!!.myWakeLock!!.isHeld()) {
            myApp!!.myWakeLock!!.release()
            giveMsg = true
        }
        if (myApp!!.myWifiLock!!.isHeld()) {
            myApp!!.myWifiLock!!.release()
            giveMsg = true
        }
        if (giveMsg) toast("Wake and Wifi locks released.", Gravity.BOTTOM)
        setPreference("serverName", gServerName)
        setPreference("gServerPort", gServerPort)
        setPreference("wifiControl", gWifiControl)
        setPreference("wakeControl", gWakeLockControl)
        if (wifiTurnedOnByMe) {
            myApp!!.setWifiState(false)
            toast("Turning off wifi...", Gravity.BOTTOM)
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun wifiControlClick(v: CheckBox) {
        if (v.isChecked()) {
            if (!isWifiConnected) {
                wifiTurnedOnByMe = true
                myApp!!.setWifiState(true)

                toast("Turning wifi on...", Gravity.BOTTOM)

                val nWifiReceiver = NaaradWifiBroadcastReceiver()

                val filters = IntentFilter()
                filters.addAction("android.net.wifi.WIFI_STATE_CHANGED")
                //	filters.addAction("android.net.wifi.STATE_CHANGE");
                //super.registerReceiver(yourReceiver, filters);
                //getActivity().registerReceiver(nWifiReceiver, filters);
            }
            gWifiControl = 1
        } else {
            if (isWifiConnected && wifiTurnedOnByMe) myApp!!.setWifiState(false)
            toast("Turning wifi off...", Gravity.BOTTOM)
            gWifiControl = 0
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun wakeLockClick(v: CheckBox) {
        if (isWifiConnected) {
            if ((v as CheckBox).isChecked()) {
                if (!myApp!!.myWakeLock!!.isHeld()) myApp!!.myWakeLock!!.acquire()
                if (!myApp!!.myWifiLock!!.isHeld()) myApp!!.myWifiLock!!.acquire()
                toast("Wake and Wifi locks set.", Gravity.BOTTOM)
                gWakeLockControl = 1
                //System.err.println("Checked");
            } else {
                if (myApp!!.myWakeLock!!.isHeld()) myApp!!.myWakeLock!!.release()
                if (myApp!!.myWifiLock!!.isHeld()) myApp!!.myWifiLock!!.release()
                gWakeLockControl = 0
                //System.err.println("UnChecked");
            }
        } else {
            v.setChecked(false)
            gWakeLockControl = 0
            toast("Wifi not connected.\nWake and Wifi locks not set.", Gravity.BOTTOM)
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setRetainInstance(true)
        myApp = getActivity().getApplication() as NaaradApp
        myApp!!.makeLocks()
        // if (mView != null)
        //     {
        // 	// Do not inflate the layout again.
        // 	// The returned View of onCreateView will be added into the fragment.
        // 	// However it is not allowed to be added twice even if the parent is same.
        // 	// So we must remove root View (nView) from the existing parent view group
        // 	// (it will be added back).
        // 	((ViewGroup)mView.getParent()).removeView(mView);
        // 	return mView;
        //     }
        if (mView == null) mView = inflater.inflate(
            R.layout.activity_naarad_setting,
            container, false
        )
        else (mView!!.getParent() as ViewGroup).removeView(mView)

        serverNameField =
            mView!!.findViewById<View?>(R.id.serverName) as EditText // reference to the text field
        serverPortField =
            mView!!.findViewById<View?>(R.id.serverPort) as EditText // reference to the text field
        serverPortField.setText(defaultPort.toString())
        System.err.println("Default Server Name" + defaultServer)
        serverNameField.setText(defaultServer)

        setButton =
            mView!!.findViewById<View?>(R.id.setButton) as Button // reference to the send button

        wifiControl = mView!!.findViewById<View?>(R.id.wifiControl) as CheckBox
        wakeButton = mView!!.findViewById<View?>(R.id.wake) as CheckBox
        wakeButton.setChecked(false)

        if ((getPreference("wifiControl", 0).also { gWifiControl = it }) == 1) {
            wifiControl.setChecked(true)
            wifiControlClick(wifiControl)
        }
        wifiControl.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                wifiControlClick(v as CheckBox?)
            }
        })

        //	if (wakeButton.isChecked()) wakeLockClick(wakeButton);
        if ((getPreference("wakeControl", 0).also { gWakeLockControl = it }) == 1) {
            wakeButton.setChecked(true)
            wakeLockClick(wakeButton)
        }

        wakeButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                wakeLockClick(v as CheckBox?)
            }
        })

        // Button press event listener
        setButton!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                gServerPort = defaultPort
                gServerName =
                    serverNameField.getText().toString() // get the text message on the text field
                try {
                    gServerPort = serverPortField.getText().toString()
                        .toInt() // get the text message on the text field
                    if (gServerPort <= 0) throw (NumberFormatException("Port number < 0"))
                    else setButton!!.setTextColor(Color.GREEN)
                } catch (nfe: NumberFormatException) {
                    val msg = "Wrong Port number: " + nfe.message
                    println(msg)
                    Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show()
                    setButton!!.setTextColor(Color.RED)
                }
                //setServerName(serverName);
                setPreference("serverName", gServerName)
                //setServerPort(serverPort);
                setPreference("serverPort", gServerPort)
                // setPreference("lamp0X", 100);
                // setPreference("lamp0Y", 200);
            }
        })
        return mView
    }

    companion object {
        private var mView: View? = null

        //
        //-----------------------------------------------------------------------------------------
        //
        fun newInstanceNSF(sampleText: String?): NaaradSettingFragment {
            val f = NaaradSettingFragment()

            val b = Bundle()
            b.putString("bString", sampleText)
            f.setArguments(b)

            return f
        }
    }
}
