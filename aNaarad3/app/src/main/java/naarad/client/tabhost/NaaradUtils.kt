package naarad.client.tabhost

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException

object NaaradUtils {
    var prefs: SharedPreferences? = null
    var nodeID2Ndx: java.util.HashMap<Int?, Int?>? = null

    //public Activity mActivity=null;        
    fun setPreference(name: String?, value: String?) {
        prefs = NaaradAbstractFragment.mActivity!!.getSharedPreferences(
            "nSettings",
            Context.MODE_PRIVATE
        )
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putString(name, value) //or you can use putInt, putBoolean ... 
        //editor.apply();
        editor.commit()
    }

    fun setPreference(name: String?, value: Int) {
        prefs = NaaradAbstractFragment.mActivity!!.getSharedPreferences(
            "nSettings",
            Context.MODE_PRIVATE
        )
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putInt(name, value) //or you can use putInt, putBoolean ... 
        //editor.apply();
        editor.commit()
    }

    fun getPreference(name: String?, defValue: Int): Int {
        prefs = NaaradAbstractFragment.mActivity!!.getSharedPreferences(
            "nSettings",
            Context.MODE_PRIVATE
        )
        return prefs?.getInt(name, defValue) ?: defValue
    }

    fun getPreference(name: String?, defValue: String?): String? {
        prefs = NaaradAbstractFragment.mActivity!!.getSharedPreferences(
            "nSettings",
            Context.MODE_PRIVATE
        )
        return prefs?.getString(name, defValue) ?: defValue
    }

    // fun getServerName(): String? = serverName
    // fun getServerPort(): Int = serverPort

    var serverName: String?
        get() {
            prefs = NaaradAbstractFragment.mActivity!!.getSharedPreferences(
                "nSettings",
                Context.MODE_PRIVATE
            )
            return prefs?.getString("serverName", defaultServer) ?: defaultServer
        }
        set(name) {
            setPreference("serverName", name)
            // prefs = NaaradAbstractFragment.mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE); 
            // SharedPreferences.Editor editor = prefs.edit();
            // editor.putString("serverName", name);  //or you can use putInt, putBoolean ... 
            // //editor.apply();
            // editor.commit();
        }


    var serverPort: Int
        get() {
            prefs = NaaradAbstractFragment.mActivity!!.getSharedPreferences(
                "nSettings",
                Context.MODE_PRIVATE
            )
            return prefs?.getInt("serverPort", defaultPort) ?: defaultPort
        }
        set(port) {
            setPreference("serverPort", port)
            // prefs = NaaradAbstractFragment.mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE); 
            // SharedPreferences.Editor editor = prefs.edit();
            // editor.putInt("serverPort", port);  //or you can use putInt, putBoolean ... 
            // //editor.apply();
            // editor.commit();
        }

    val defaultPort: Int
        get() = getPreference("serverPort", 1234)

    val defaultServer: String?
        get() = getPreference("serverName", "raspberrypi")

    val nodeID2NdxMap: HashMap<*, *>?
        get() {
            nodeID2Ndx = java.util.HashMap<Int?, Int?>()
            nodeID2Ndx!!.put(1, 0)
            nodeID2Ndx!!.put(3, 1)

            return nodeID2Ndx
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
        else if (nodeid == 3) return 1
        else return -1
    }

    fun recreateView(v: View?): Boolean {
        if (v != null) {
            // Do not inflate the layout again.
            // The returned View of onCreateView will be added into the fragment.
            // However it is not allowed to be added twice even if the parent is same.
            // So we must remove root View (nView) from the existing parent view group
            // (it will be added back).
            (v.parent as ViewGroup).removeView(v)
            return true
        } else return false
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun mkMessage(message: String): String {
        val n: Int
        val totalLen: Int
        n = message.length
        val msg: String
        var lenStr: String

        lenStr = n.toString()
        totalLen = lenStr.length + n + 1
        lenStr = totalLen.toString()
        msg = lenStr + " " + message
        //Log.i("Message: ",msg);
        return msg
    }

    fun toast(msg: String?, gravity: Int) {
        // //Toast.makeText (getApplicationContext(), msg, Toast.LENGTH_SHORT).show ();
        // Toast.makeText (mActivity, msg, Toast.LENGTH_SHORT).show ();

        //Toast.makeText (getApplicationContext(), msg, Toast.LENGTH_SHORT).show ();

        val mToast: Toast =
            Toast.makeText(NaaradAbstractFragment.mActivity, msg, Toast.LENGTH_SHORT)
        //mToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
        mToast.setGravity(gravity, 0, 0)
        mToast.show()
    }

    //
    // Use from anywhere in the package (e.g., from inside anothe
    // thread) to toast a message).  This always runs the Toast in the
    // UI thread.
    //
    fun uiToast(msg: String?, gravity: Int) {
        class ToastOnUIThread(var thisText: String?, var thisGravity: Int) :
            Runnable {
            override fun run() {
                toast(thisText, thisGravity)
            }
        }



        NaaradAbstractFragment.mActivity!!.runOnUiThread(ToastOnUIThread(msg, gravity))
    }

    val isWifiConnected: Boolean
        get() {
            //ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            val connManager: ConnectivityManager =
                NaaradAbstractFragment.mActivity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mWifi: NetworkInfo? = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            return mWifi?.isConnected == true
        }

    @Throws(IOException::class)
    fun naaradReader(socReader: BufferedReader): String? {
        var charsRead = 0
        //char[] buffer = new char[1024];
        var oneChar: Char
        var message = ""
        // socReader.read() is a blocking call, which is
        // what we want since this is in a separate thread and
        // all that this thread does is wait for data to arrive,
        // and supply it to the plotter
        try {
            // First read the message length.  
            while (((socReader.read().toChar().also { oneChar = it }).code != -1) &&
                (charsRead < 1024)
            ) {
                if ((oneChar == ' ') && (charsRead > 0)) break
                else {
                    message += oneChar
                    charsRead++
                    //System.err.print(oneChar);
                }
            }
            // Convert the message length string to integer
            val msgLen: Int
            try {
                msgLen = message.toInt()
            } catch (nfe: NumberFormatException) {
                System.err.println("Error in converting message length to integer")
                return null
            }
            // Add blank to the message so far.  The read and
            // add next msgLen characters to the message
            // string.
            //System.err.println("Msglen:"+msgLen);
            message += " "
            while (((socReader.read().toChar().also { oneChar = it }).code != -1) &&
                (charsRead < msgLen)
            ) {
                // Do a bit of lexical analaysis...
                if (oneChar != '}') {
                    message += oneChar
                    charsRead++
                } else break
            }
            if (oneChar != '}') throw (JSONException("End \"}\" not found"))
            message += oneChar.toString() + "\n"


            //System.err.println("Msg:"+message);
            //return charsRead;
            return message
        } // catch (IOException e) 
        // 	{
        // 	    String msg = "Error connecting to "+getServerName()+":"+Integer.toString(getServerPort())+"\nCheck settings";
        // 	    uiToast(msg,Gravity.BOTTOM);
        // 	    return msg;
        // 	}
        catch (e: JSONException) {
            //throw new RuntimeException(e);
            System.err.println(e.message)
            uiToast(e.message, Gravity.BOTTOM)
            return null
        }
    }

    @Throws(IOException::class, UnknownHostException::class)
    fun naaradWriter(socWriter: PrintWriter, message: String) {
        socWriter.write(mkMessage(message))
        socWriter.flush()
    }

    @Throws(IOException::class, UnknownHostException::class)
    fun naaradSocket(hostName: String?, port: Int): Socket? {
        val clientSoc = Socket(hostName, port)


        // int attempts = 0;
        // if (!clientSoc.isConnected() && attempts < 10)
        //     {
        // 	SystemClock.sleep(10);
        // 	attempts++;
        //     }
        // else
        //     throw(new IOException("Naarad could not connection socket: \""+hostName+"\":"+port));
        if (clientSoc.isConnected) return clientSoc
        else return null
    }
}
