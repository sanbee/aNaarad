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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.SystemClock
import android.widget.Toast

class NaaradWifiBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val connManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        var tries = 0
        while (tries < 10) {
            if (mWifi!!.isConnected) {
                Toast.makeText(context, "Checking Wifi...", Toast.LENGTH_SHORT).show()
                break
            } else {
                SystemClock.sleep(10)
                tries++
            }
        }
        if (!mWifi!!.isConnected) Toast.makeText(
            context,
            "Wifi yet not connected...",
            Toast.LENGTH_SHORT
        ).show()

        //     // TODO Auto-generated method stub
        //     Integer flag;
        //     flag = 0;
        //     ConnectivityManager check = (ConnectivityManager) context
        //             .getSystemService(Context.CONNECTIVITY_SERVICE);
        //     NetworkInfo[] info = check.getAllNetworkInfo();     

        //     for (int i = 0; i < info.length; i++) {
        //         if (info[i].getState() == NetworkInfo.State.CONNECTED) {
        //             Toast.makeText(context, "Internet is connected",
        //                     Toast.LENGTH_SHORT).show();
        //             flag = 1;
        //         }
        //     }

        //     if (flag != 0) {

        //         StringBuffer sb = new StringBuffer();

        //         TelephonyManager TelephonyMgr = (TelephonyManager) context
        //                 .getSystemService(Context.TELEPHONY_SERVICE);
        //         m_deviceId = TelephonyMgr.getDeviceId();

        //         m_androidId = Secure.getString(context.getContentResolver(),
        //                 Secure.ANDROID_ID);

        //         String bm = android.os.Build.MANUFACTURER + ","
        //                 + android.os.Build.MODEL;
        //         System.out.println("Device Name: " + bm + "\n");

        //         try {

        //             URL url = new URL(
        //                     "http://saslabtech.com/atsdemo/import_vr.php?d="
        //                             + URLEncoder.encode(m_deviceId, "UTF-8") + "&a="
        //                             + URLEncoder.encode(m_androidId, "UTF-8")
        //                             + "&model=" + URLEncoder.encode(bm, "UTF-8")
        //                             + "&ip="
        //                             + URLEncoder.encode(getLocalIpv4Address(), "UTF-8"));
        //             System.out.println(url);
        //             HttpURLConnection con = (HttpURLConnection) url
        //                     .openConnection();
        //             readStream(con.getInputStream());
        //         } catch (Exception e) {
        //             e.printStackTrace();
        //         }

        //         Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show();
        //     }
        // }
    }
}