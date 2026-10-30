package naarad.client.tabhost

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

    fun setPreference(name: String?, value: String?) {
        val ctx = NaaradAbstractFragment.mActivity ?: return
        prefs = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putString(name, value)
        editor.commit()
    }

    fun setPreference(name: String?, value: Int) {
        val ctx = NaaradAbstractFragment.mActivity ?: return
        prefs = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putInt(name, value)
        editor.commit()
    }

    fun getPreference(name: String?, defValue: Int): Int {
        val ctx = NaaradAbstractFragment.mActivity ?: return defValue
        prefs = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
        return prefs?.getInt(name, defValue) ?: defValue
    }

    fun getPreference(name: String?, defValue: String?): String? {
        val ctx = NaaradAbstractFragment.mActivity ?: return defValue
        prefs = ctx.getSharedPreferences("nSettings", Context.MODE_PRIVATE)
        return prefs?.getString(name, defValue) ?: defValue
    }

    val nodeID2NdxMap: HashMap<Int?, Int?>
        get() {
            val map = HashMap<Int?, Int?>()
            map[1] = 0
            map[3] = 1
            nodeID2Ndx = map
            return map
        }

    @Suppress("UNCHECKED_CAST")
    fun getKeysAsArray(myMap: java.util.HashMap<*, *>): IntArray {
        val keys: MutableSet<Int> = myMap.keys as MutableSet<Int>
        val array = IntArray(keys.size)
        var index = 0
        for (element in keys) array[index++] = element
        return array
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

    fun mkMessage(message: String): String {
        val n = message.length
        val lenStr = n.toString()
        val totalLen = lenStr.length + n + 1
        return totalLen.toString() + " " + message
    }

    fun toast(msg: String?, gravity: Int) {
        val ctx = NaaradAbstractFragment.mActivity ?: return
        val mToast: Toast = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT)
        mToast.setGravity(gravity, 0, 0)
        mToast.show()
    }

    fun uiToast(msg: String?, gravity: Int) {
        class ToastOnUIThread(var thisText: String?, var thisGravity: Int) : Runnable {
            override fun run() {
                toast(thisText, thisGravity)
            }
        }
        NaaradAbstractFragment.mActivity?.runOnUiThread(ToastOnUIThread(msg, gravity))
    }

    val isWifiConnected: Boolean
        get() {
            val ctx = NaaradAbstractFragment.mActivity ?: return false
            val connManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val mWifi: NetworkInfo? = connManager?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            return mWifi?.isConnected == true
        }

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

    @Throws(IOException::class)
    fun naaradReader(socReader: BufferedReader): String
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
		while (charsRead < 1024)
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
		//System.err.println("Msglen message:"+message)
		var msgLen: Int
		try 
		    {
			msgLen = Integer.parseInt(message)
		    } 
		catch(nfe: NumberFormatException)
		    {
			System.err.println("Error in converting message length to integer");
			return "";
		    }
		// Add blank to the message so far.  The read and
		// add next msgLen characters to the message
		// string.
		//System.err.println("Msglen:"+msgLen);
		message+=" ";
		// while (((int)(oneChar = (char)socReader.read()) != -1) &&
		//        (charsRead < msgLen)
		//        )
		oneChar=null
		while (charsRead < msgLen)
		    {
		    	oneChar = socReader.read().toChar()
			// Do a bit of lexical analaysis...
			if (oneChar != '}') 
			    {
				message += oneChar;
				charsRead++;
			    }
			else break;
		    }
		if (oneChar != '}')  throw JSONException("End \"}\" not found")
		message += oneChar + "\n";
		
		//System.err.println("naaradReader() Msg:"+message);
		//return charsRead;
		return message;
	    }
	catch (e: JSONException) 
	    {
		//throw new RuntimeException(e);
		System.err.println(e.message);
		uiToast(e.message,Gravity.BOTTOM);
		return "";
	    }
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
            clientSoc.soTimeout = 1000 //Time out receiving 
            clientSoc.connect(java.net.InetSocketAddress(hostName, port), 20000) // TimeOut for connection
            if (clientSoc.isConnected) clientSoc else null
        } catch (e: Exception) {
            null
        }
    }

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
