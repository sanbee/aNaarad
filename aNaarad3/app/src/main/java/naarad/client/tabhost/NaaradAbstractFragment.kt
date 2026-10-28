package naarad.client.tabhost

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException

abstract class NaaradAbstractFragment : Fragment() {
    private var prefs: SharedPreferences? = null
    @JvmField
    var nodeID2Ndx: java.util.HashMap<Int?, Int?>? = null

    val defaultPort: Int
        get() = NaaradUtils.defaultPort
    val defaultServer: String?
        get() = NaaradUtils.defaultServer

    @Suppress("UNCHECKED_CAST")
    fun getKeysAsArray(myMap: java.util.HashMap<*, *>): IntArray {
        val keys: MutableSet<Int> = myMap.keys as MutableSet<Int>
        val array = IntArray(keys.size)
        var index = 0
        for (element in keys) array[index++] = element
        return array
    }

    val nodeID2NdxMap: HashMap<*, *>
        get() {
            nodeID2Ndx = java.util.HashMap<Int?, Int?>()
            nodeID2Ndx!!.put(1, 0)
            nodeID2Ndx!!.put(3, 1)

            return nodeID2Ndx!!
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            if (mActivity == null) mActivity = context
            mActivity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        this.nodeID2NdxMap
    }

    fun mapNodeID2Ndx(nodeid: Int): Int {
        if (nodeid == 1) return 0
        if (nodeid == 3) return 1
        return Math.abs(nodeid) % 2
    }

    fun recreateView(v: View?): Boolean {
        if (v != null) {
            (v.parent as ViewGroup).removeView(v)
            return true
        } else return false
    }

    fun setPreference(name: String?, value: String?) {
        val ctx = activity ?: mActivity ?: return
        prefs = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putString(name, value)
        editor.commit()
    }

    fun setPreference(name: String?, value: Int) {
        val ctx = activity ?: mActivity ?: return
        prefs = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putInt(name, value)
        editor.commit()
    }

    fun getPreference(name: String?, defValue: Int): Int {
        val ctx = activity ?: mActivity ?: return defValue
        prefs = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
        return prefs?.getInt(name, defValue) ?: defValue
    }

    fun getPreference(name: String?, defValue: String?): String? {
        val ctx = activity ?: mActivity ?: return defValue
        prefs = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
        return prefs?.getString(name, defValue) ?: defValue
    }

    var serverName: String?
        get() = NaaradUtils.serverName
        set(name) {
            NaaradUtils.serverName = name
        }

    var serverPort: Int
        get() = NaaradUtils.serverPort
        set(port) {
            NaaradUtils.serverPort = port
        }

    fun mkMessage(message: String): String {
        val n = message.length
        val lenStr = n.toString()
        val totalLen = lenStr.length + n + 1
        return totalLen.toString() + " " + message
    }

    fun toast(msg: String?, gravity: Int) {
        val mToast: Toast = Toast.makeText(mActivity ?: activity, msg, Toast.LENGTH_SHORT)
        mToast.setGravity(gravity, 0, 0)
        mToast.show()
    }

    fun uiToast(msg: String?, gravity: Int) {
        class ToastOnUIThread(var thisText: String?, var thisGravity: Int) : Runnable {
            override fun run() {
                toast(thisText, thisGravity)
            }
        }
        (activity ?: mActivity)?.runOnUiThread(ToastOnUIThread(msg, gravity))
    }

    val isWifiConnected: Boolean
        get() {
            val connManager = (activity ?: mActivity)?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val mWifi = connManager?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            return mWifi?.isConnected == true
        }

    @Throws(IOException::class)
    fun naaradReader(socReader: BufferedReader): String? {
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

    @Throws(IOException::class, UnknownHostException::class)
    fun naaradWriter(socWriter: PrintWriter, message: String) {
        socWriter.write(mkMessage(message))
        socWriter.flush()
    }

    fun naaradSocket(hostName: String?, port: Int): Socket? {
        if (hostName.isNullOrEmpty() || port <= 0) return null
        return try {
            val clientSoc = Socket()
            clientSoc.soTimeout = 3000
            clientSoc.connect(java.net.InetSocketAddress(hostName, port), 2000)
            if (clientSoc.isConnected) clientSoc else null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @JvmField
        var mActivity: Activity? = null
    }
}
