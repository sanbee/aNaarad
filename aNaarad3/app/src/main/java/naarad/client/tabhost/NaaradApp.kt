package naarad.client.tabhost

import android.app.Application
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.DisplayMetrics
import android.view.WindowManager

//import android.net.wifi.WifiManager.WifiLock;
class NaaradApp : Application() {
    var swipeState: Boolean = true
    @JvmField
    var densityDpi: Float = 0f
    @JvmField
    var myWakeLock: WakeLock? = null
    @JvmField
    var myWifiLock: WifiLock? = null
    var myWifiManager: WifiManager? = null
    @JvmField
    var metrics: DisplayMetrics? = null

    fun setWifiState(turnON: Boolean) {
        myWifiManager = getSystemService(WIFI_SERVICE) as WifiManager?
        myWifiManager!!.setWifiEnabled(turnON)
    }

    fun makeLocks() {
        val wakeLockPM = getSystemService(POWER_SERVICE) as PowerManager
        myWifiManager = getSystemService(WIFI_SERVICE) as WifiManager?
        //if (myWakeLock == null)
        run {
            myWakeLock = wakeLockPM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag")
            myWifiLock = myWifiManager!!.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLockTag")
            System.err.println("Making wakelock")
        }
    }

    override fun onCreate() {
        super.onCreate()

        val mgr = getSystemService(WINDOW_SERVICE) as WindowManager
        metrics = DisplayMetrics()
        mgr.getDefaultDisplay().getMetrics(metrics)


        // Drawable image = getResources().getDrawable(R.drawables.someImage);
        // float scale = metrics.widthPixels / (float) image.getIntrinsicWidth();


        //DisplayMetrics dm = getResources().getDisplayMetrics(); 
        //densityDpi = dm.densityDpi;
        densityDpi = getResources().getDisplayMetrics().density

        this.swipeState = true
        makeLocks()
    }

    fun dpToPixel(dp: Int): Int {
        return (dp * densityDpi).toInt()
    }
}
