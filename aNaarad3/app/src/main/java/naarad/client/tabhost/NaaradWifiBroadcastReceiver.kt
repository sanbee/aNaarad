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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.SystemClock
import android.widget.Toast

class NaaradWifiBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val connManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        @Suppress("DEPRECATION")
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        var tries = 0
        while (tries < 10) {
            @Suppress("DEPRECATION")
            if (mWifi?.isConnected == true) {
                Toast.makeText(context, "Checking Wifi...", Toast.LENGTH_SHORT).show()
                break
            } else {
                SystemClock.sleep(10)
                tries++
            }
        }
        @Suppress("DEPRECATION")
        if (mWifi?.isConnected != true) {
            Toast.makeText(
                context,
                "Wifi yet not connected...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
