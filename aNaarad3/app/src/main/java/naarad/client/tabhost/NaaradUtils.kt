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

import android.util.Log
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException
import org.json.JSONException
import android.view.Gravity
import android.security.NetworkSecurityPolicy
import android.os.Build

object NaaradUtils {
    var prefs: SharedPreferences? = null
    var nodeID2Ndx: java.util.HashMap<Int?, Int?>? = null

    // Default time range of 30 min (in seconds)
    var livePlotReSyncTimescale: Double = currentPlotReSyncTimescale // To be used in loops
    var currentPlotReSyncTimescaleStr: String
        get() = getPreference("reSyncTimescale", "30m") ?: "30m"
        set(value) {
            setPreference("reSyncTimescale", value)
            livePlotReSyncTimescale = parseHmsToSeconds(value)
        }
    var currentPlotReSyncTimescale: Double             // User setting (infrequently changed)
        get() = parseHmsToSeconds(currentPlotReSyncTimescaleStr)
        set(value) {
            livePlotReSyncTimescale = value
        }

    // Default time range of 2 days (in seconds)
    var livePlotTimerange: Double = currentPlotTimerange // To be used in loops
    var currentPlotTimerangeStr: String
        get() = getPreference("plotTimerange", "2d") ?: "2d"
        set(value) {
            setPreference("plotTimerange", value)
            livePlotTimerange = parseHmsToSeconds(value)
        }
    var currentPlotTimerange: Double             // User setting (infrequently changed)
        get() = parseHmsToSeconds(currentPlotTimerangeStr)
        set(value) {
            livePlotTimerange = value
        }

    // Timeout in seconds used to ping the server for live sensor data
    // If a new values is set, it also interrupts the ISleep.sleep()
    // in NPF::doInBackgroupd() which then gets then instantly gets
    // the new value.
    var livePingTimeout: Double = currentPingTimeout // To be used in loops
    var currentPingTimeoutStr: String
        get() = getPreference("pingTimeout", "1m") ?: "1m"
        set(value) {
            val oldVal = livePingTimeout
            setPreference("pingTimeout", value)
            val newVal = parseHmsToSeconds(value)
            livePingTimeout = newVal
            if (oldVal != newVal) {
                NaaradPlotFragment.activeInstance?.abortSleeperForTimeoutChange()
            }
        }
    var currentPingTimeout: Double                   // User setting (infrequently changed)
        get() = parseHmsToSeconds(currentPingTimeoutStr)
        set(value) {
            val oldVal = livePingTimeout
            livePingTimeout = value
            if (oldVal != value) {
                NaaradPlotFragment.activeInstance?.abortSleeperForTimeoutChange()
            }
        }

    val defaultPort: Int
        get() = getPreference("serverPort", 1234)

    val defaultServer: String?
        get() = getPreference("serverName", "192.168.0.126")

    var serverName: String?
        get() {
            return getPreference("serverName", defaultServer) ?: defaultServer
        }
        set(name) {
            setPreference("serverName", name)
        }

    var serverPort: Int
        get() {
            return getPreference("serverPort", defaultPort)
        }
        set(port) {
            setPreference("serverPort", port)
        }
    //
    //---------------------------------------------------------------------------------------------
    //
    inline fun <reified T> setPreference(name: String?, value: T) {
	val ctx = NaaradAbstractFragment.mActivity ?: return
	val prefs: SharedPreferences = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
	val editor: SharedPreferences.Editor = prefs.edit()

	when (value) {
            is String? -> editor.putString(name, value)
            is Int -> editor.putInt(name, value)
            is Boolean -> editor.putBoolean(name, value)
            is Long -> editor.putLong(name, value)
            is Float -> editor.putFloat(name, value)
            else -> throw IllegalArgumentException("Unsupported preference type: ${T::class.java.name}")
	}

	editor.apply() // Highly recommended over commit() to prevent UI freezing
    }

    //---------------------------------------------------------------------------------------------

    inline fun <reified T> getPreference(name: String?, defValue: T): T {
	val ctx = NaaradAbstractFragment.mActivity ?: return defValue
	val prefs: SharedPreferences = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)

	val result: Any? = when (T::class) {
            String::class -> prefs.getString(name, defValue as? String)
            Int::class -> prefs.getInt(name, defValue as Int)
            Boolean::class -> prefs.getBoolean(name, defValue as Boolean)
            Long::class -> prefs.getLong(name, defValue as Long)
            Float::class -> prefs.getFloat(name, defValue as Float)
            else -> throw IllegalArgumentException("Unsupported preference type: ${T::class.java.name}")
	}

	return (result as? T) ?: defValue
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    // Parses a time input string in DHMS format (e.g., "1d2h33m21s",
    // "1h30m20s", "2.1d", "1.5h", "10m", "45s", "120") and returns
    // the total time in seconds as a Double.
    //
    // - D (days) H (Hours), M (Minutes), and S (Seconds) are all
    // optional and case-insensitive.  - If no unit suffix (D, H, M, S)
    // is present (e.g. "120"), the input is treated as seconds.
    fun parseHmsToSeconds(input: String): Double {
	val text = input.trim()
	if (text.isEmpty()) return 0.0

	// Regex to capture optional days (d/D), hours (h/H), minutes (m/M), and seconds (s/S)
	val regex = Regex("(?i)(?:([0-9.]+)\\s*d)?\\s*(?:([0-9.]+)\\s*h)?\\s*(?:([0-9.]+)\\s*m)?\\s*(?:([0-9.]+)\\s*s)?")
	val match = regex.matchEntire(text)

	if (match != null) {
            val dStr = match.groupValues[1]
            val hStr = match.groupValues[2]
            val mStr = match.groupValues[3]
            val sStr = match.groupValues[4]

            // If at least one HMS unit was present in the input
            if (dStr.isNotEmpty() || hStr.isNotEmpty() || mStr.isNotEmpty() || sStr.isNotEmpty()) {
		val days = dStr.toDoubleOrNull() ?: 0.0
		val hours = hStr.toDoubleOrNull() ?: 0.0
		val minutes = mStr.toDoubleOrNull() ?: 0.0
		val seconds = sStr.toDoubleOrNull() ?: 0.0
		return (days * 3600*24) + (hours * 3600.0) + (minutes * 60.0) + seconds
            }
	}

	// Fallback: If no D, H, M, or S suffix was provided (e.g., "120"), treat as raw seconds
	return text.toDoubleOrNull() ?: 0.0
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    val nodeID2NdxMap: HashMap<Int?, Int?>
        get() {
            val map = HashMap<Int?, Int?>()
            map[1] = 0
            map[3] = 1
            nodeID2Ndx = map
            return map
        }
    //
    //---------------------------------------------------------------------------------------------
    //
    @Suppress("UNCHECKED_CAST")
    fun getKeysAsArray(myMap: java.util.HashMap<*, *>): IntArray {
        val keys: MutableSet<Int> = myMap.keys as MutableSet<Int>
        val array = IntArray(keys.size)
        var index = 0
        for (element in keys) array[index++] = element
        return array
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    fun mapNodeID2Ndx(nodeid: Int): Int {
        if (nodeid == 1) return 0
        if (nodeid == 3) return 1
        return Math.abs(nodeid) % 2
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    fun recreateView(v: View?): Boolean {
        if (v != null) {
            (v.parent as ViewGroup).removeView(v)
            return true
        } else return false
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    fun mkMessage(message: String): String {
        val n = message.length
        val lenStr = n.toString()
        val totalLen = lenStr.length + n + 1
        return totalLen.toString() + " " + message
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    fun toast(msg: String?, gravity: Int) {
        val ctx = NaaradAbstractFragment.mActivity ?: return
        val mToast: Toast = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT)
        mToast.setGravity(gravity, 0, 0)
        mToast.show()
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    fun uiToast(msg: String?, gravity: Int) {
        class ToastOnUIThread(var thisText: String?, var thisGravity: Int) : Runnable {
            override fun run() {
                toast(thisText, thisGravity)
            }
        }
        NaaradAbstractFragment.mActivity?.runOnUiThread(ToastOnUIThread(msg, gravity))
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    val isWifiConnected: Boolean
        get() {
            val ctx = NaaradAbstractFragment.mActivity ?: return false
            val connManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val mWifi: NetworkInfo? = connManager?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            return mWifi?.isConnected == true
        }
    //
    //---------------------------------------------------------------------------------------------
    //
    @Throws(IOException::class)
    fun naaradReader_kt(socReader: BufferedReader): String? {
        val lenBuf = StringBuilder()
        var ch: Int
        while (socReader.read().also { ch = it } != -1) {
            val c = ch.toChar()
            if (c == ' ' && lenBuf.isNotEmpty()) break
            if (c.isDigit()) lenBuf.append(c)
        }
        if (lenBuf.isEmpty()) return null
        val msgLen = try {
            lenBuf.toString().toInt()
        } catch (e: NumberFormatException) {
            return null
        }

        val bodyBuf = StringBuilder()
        var count = 0
        while (count < msgLen && socReader.read().also { ch = it } != -1) {
            bodyBuf.append(ch.toChar())
            count++
        }

        return "${lenBuf} ${bodyBuf.toString().trim()}"
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    @Throws(IOException::class)
    fun naaradReader(socReader: BufferedReader,
		     tag: String=""): String
    {
	var charsRead :Int = 0
	//char[] buffer = new char[1024];
	var oneChar: Char? = null
	var message=""
	// socReader.read() is a blocking call, which is
	// what we want since this is in a separate thread and
	// all that this thread does is wait for data to arrive,
	// and supply it to the plotter
	try
	{
	    // First read the message length.
	    //		while ((oneChar = (char)socReader.read()) != -1) && (charsRead < 1024))
	    oneChar=null
	    // while (charsRead < 1024)
	    while (charsRead < 16)
	    {
		oneChar = socReader.read().toChar()
		if ((oneChar == ' ') && (charsRead > 0))
		    break
		else
		{
		    message += oneChar
		    charsRead++
		}
	    }
	    // Convert the message length string to integer
	    //Log.d("NU::naaradReader","Msglen message:"+message)
	    var msgLen: Int
	    try
	    {
		msgLen = Integer.parseInt(message)
	    }
	    catch(nfe: NumberFormatException)
	    {
		Log.d("NU::naaradReader","Error in converting message length to integer");
		return "";
	    }
	    // Add blank to the message so far.  The read and
	    // add next msgLen characters to the message
	    // string.
	    //if (tag != "") Log.d("NU::naaradReader", tag+": Msglen:"+msgLen);

	    message+=" ";
	    // while (((int)(oneChar = (char)socReader.read()) != -1) &&
	    //        (charsRead < msgLen)
	    //        )
	    oneChar=null
	    charsRead=0
	    while (charsRead < msgLen)
	    {
		oneChar = socReader.read().toChar()

		// // Do a bit of lexical analaysis...
		if (oneChar != '}')
		{
		    message += oneChar;
		    charsRead++;
		}
		else break;
	    }
	    //if (tag != "") Log.d("NU::naaradReader" , tag+": message: "+message)

	    if (oneChar != '}')  throw JSONException("End \"}\" not found")
	    message += oneChar + "\n"
	    //		message += "\n"

	    return message;
	}
	catch (e: JSONException)
	{
	    Log.d("NU::naaradReader", e.message ?: "JSONException");
	    uiToast(e.message ?: "JSONException",Gravity.BOTTOM);
	    return "";
	}
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    @Throws(IOException::class, UnknownHostException::class)
    fun naaradWriter(socWriter: PrintWriter, message: String) {
        socWriter.write(mkMessage(message))
        socWriter.flush()
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    fun naaradSocket(hostName: String?, port: Int): Socket? {
        if (hostName.isNullOrEmpty() || port <= 0) return null
        return try {
            val clientSoc = Socket()
            clientSoc.soTimeout = 10000 //Time out receiving
            clientSoc.connect(java.net.InetSocketAddress(hostName, port), 20000) // TimeOut for connection
            if (clientSoc.isConnected) clientSoc else null
        } catch (e: Exception) {
            null
        }
    }
    //
    //---------------------------------------------------------------------------------------------
    //
    fun isCleartextPermittedForHost(host: String): Boolean {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Checks the exact host against your NetworkSecurityConfig XML rules
            NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(host)
	} else {
            // On API 23, it's a global true/false setting
            NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted
	}
    }

}
