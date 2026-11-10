/*
 * Copyright (C) 2026 aNaarad Project
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

import android.app.Application
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.DisplayMetrics
import android.view.WindowManager
import android.util.Log

//import android.net.wifi.WifiManager.WifiLock;
class NaaradApp : Application() {
    var onSwipeStateChanged: ((Boolean) -> Unit)? = null

    var swipeState: Boolean = true
        set(value) {
            field = value
            onSwipeStateChanged?.invoke(value)
        }
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
        myWifiManager!!.isWifiEnabled = turnON
    }

    fun makeLocks() {
        val wakeLockPM = getSystemService(POWER_SERVICE) as PowerManager
        myWifiManager = getSystemService(WIFI_SERVICE) as WifiManager?
        //if (myWakeLock == null)
        run {
            myWakeLock = wakeLockPM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag")
            myWifiLock = myWifiManager!!.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLockTag")
            Log.d("NApp::makeLocks","Making wakelock")
        }
    }

    override fun onCreate() {
        super.onCreate()

        val mgr = getSystemService(WINDOW_SERVICE) as WindowManager
        metrics = DisplayMetrics()
        mgr.defaultDisplay.getMetrics(metrics)


        // Drawable image = getResources().getDrawable(R.drawables.someImage);
        // float scale = metrics.widthPixels / (float) image.getIntrinsicWidth();


        //DisplayMetrics dm = getResources().getDisplayMetrics(); 
        //densityDpi = dm.densityDpi;
        densityDpi = resources.displayMetrics.density

        this.swipeState = true
        makeLocks()
    }

    fun dpToPixel(dp: Int): Int {
        return (dp * densityDpi).toInt()
    }
}
