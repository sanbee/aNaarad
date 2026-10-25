package naarad.client.tabhost;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.Gravity;
import android.app.Activity;
import android.widget.Toast;
import android.view.Gravity;
import android.view.View;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import android.os.SystemClock;
import java.util.Set;

public abstract class NaaradAbstractFragment extends Fragment 
{
    private SharedPreferences prefs; 
    public HashMap<Integer, Integer> nodeID2Ndx;

    static public Activity mActivity=null;        
    // client = new Socket("10.0.2.2", 1234); // connect to the server on local machine
    // client = new Socket("raspberrypi", 1234); // connect to the Naarad server
    public int getDefaultPort() 
    {
	return getPreference("serverPort",1234);
    }
    //final public String getDefaultServer() {return "10.0.2.2";}
    public String getDefaultServer() 
    {
	return getPreference("serverName","raspberrypi");
    }

    public int[] getKeysAsArray(HashMap myMap)
    {
	Set<Integer> keys = myMap.keySet();
	int[] array = new int[keys.size()];
	int index = 0;
	for(Integer element : keys) array[index++] = element.intValue();
	return array;
    }

    public HashMap getNodeID2NdxMap()
    {
	nodeID2Ndx = new HashMap<Integer, Integer>();
	nodeID2Ndx.put(1,0);
	nodeID2Ndx.put(3,1);

	return nodeID2Ndx;
    }
    //
    // Hold a reference to the activity and use it in place of
    // getActivity().  This is required since getActivity() may return
    // NULL when the view is created from existing activity (either
    // via swipping or re-start of the app).
    //
    @Override public void onAttach(Activity activity) 
    {
        super.onAttach(activity);
	if (mActivity==null)
        mActivity = activity;
	//mActivity.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
	mActivity.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	
	getNodeID2NdxMap();
    }

    public int mapNodeID2Ndx(int nodeid)
    {
	if (nodeid == 1)      return 0;
	else if (nodeid == 3) return 1;
	else return -1;
    }

    public boolean recreateView(View v)
    {
	if (v != null)
	    {
		// Do not inflate the layout again.
		// The returned View of onCreateView will be added into the fragment.
		// However it is not allowed to be added twice even if the parent is same.
		// So we must remove root View (nView) from the existing parent view group
		// (it will be added back).
		((ViewGroup)v.getParent()).removeView(v);
		return true;
	    }
	else return false;
    }

    public void setPreference(String name, String value)
    {
	prefs = mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE); 
	SharedPreferences.Editor editor = prefs.edit();
	editor.putString(name, value);  //or you can use putInt, putBoolean ... 
	//editor.apply();
	editor.commit();
    }
    public void setPreference(String name, int value)
    {
	prefs = mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE); 
	SharedPreferences.Editor editor = prefs.edit();
	editor.putInt(name, value);  //or you can use putInt, putBoolean ... 
	//editor.apply();
	editor.commit();
    }
    public int getPreference(String name, int defValue)
    {
	prefs = mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE);
	return prefs.getInt(name,defValue);
    }
    public String getPreference(String name, String defValue)
    {
	prefs = mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE);
	return prefs.getString(name,defValue);
    }
    public void setServerName(String name)
    {
	setPreference("serverName",name);
	// prefs = mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE); 
	// SharedPreferences.Editor editor = prefs.edit();
	// editor.putString("serverName", name);  //or you can use putInt, putBoolean ... 
	// //editor.apply();
	// editor.commit();
    }
    public String getServerName()    
    {
	prefs = mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE);
	return prefs.getString("serverName",getDefaultServer());
    }


    public void setServerPort(int port)
    {
	setPreference("serverPort",port);
	// prefs = mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE); 
	// SharedPreferences.Editor editor = prefs.edit();
	// editor.putInt("serverPort", port);  //or you can use putInt, putBoolean ... 
	// //editor.apply();
	// editor.commit();
    }
    public int getServerPort()    
    {
	prefs = mActivity.getSharedPreferences("nSettings", Context.MODE_PRIVATE);
	return prefs.getInt("serverPort",getDefaultPort());
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    public String mkMessage(String message) 
    {
	Integer n, totalLen;
	n = message.length();
	String msg,lenStr;
	
	lenStr=Integer.toString(n);
	totalLen =lenStr.length() + n + 1;
	lenStr=Integer.toString(totalLen);
	msg = lenStr + " " + message;
	//Log.i("Message: ",msg);
	return msg;
    }
    public void toast (String msg, int gravity)
    {
	// //Toast.makeText (getApplicationContext(), msg, Toast.LENGTH_SHORT).show ();
	// Toast.makeText (mActivity, msg, Toast.LENGTH_SHORT).show ();

	//Toast.makeText (getApplicationContext(), msg, Toast.LENGTH_SHORT).show ();
	Toast mToast = Toast.makeText (mActivity, msg, Toast.LENGTH_SHORT);
	//mToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
	mToast.setGravity(gravity, 0, 0);
	mToast.show();
    } 

    //
    // Use from anywhere in the package (e.g., from inside anothe
    // thread) to toast a message).  This always runs the Toast in the
    // UI thread.
    //
    public void uiToast(String msg, int gravity)
    {
	class ToastOnUIThread implements Runnable
	{
	    String thisText;
	    int thisGravity;
	    ToastOnUIThread(String text, int gravity)   {thisText = text;thisGravity=gravity;}
	    public void run()  {toast(thisText,thisGravity);} 
	};
	
	mActivity.runOnUiThread(new ToastOnUIThread(msg,gravity));
    }

    public boolean isWifiConnected()
    {
	ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

	return mWifi.isConnected();
    }

    public String naaradReader(BufferedReader socReader) throws IOException
    {
	int charsRead = 0;
	//char[] buffer = new char[1024];
	char oneChar;
	String message="";
	// socReader.read() is a blocking call, which is
	// what we want since this is in a separate thread and
	// all that this thread does is wait for data to arrive,
	// and supply it to the plotter
	try
	    {
		// First read the message length.  
		while (((int)(oneChar = (char)socReader.read()) != -1) &&
		       (charsRead < 1024))
		    {
			if ((oneChar == ' ') && (charsRead > 0))
			    break;
			else
			    {
				message += oneChar;
				charsRead++;
				//System.err.print(oneChar);
			    }
		    }
		// Convert the message length string to integer
		int msgLen;
		try 
		    {
			msgLen = Integer.parseInt(message);
		    } 
		catch(NumberFormatException nfe)
		    {
			System.err.println("Error in converting message length to integer");
			return null;
		    }
		// Add blank to the message so far.  The read and
		// add next msgLen characters to the message
		// string.
		//System.err.println("Msglen:"+msgLen);
		message+=" ";
		while (((int)(oneChar = (char)socReader.read()) != -1) &&
		       (charsRead < msgLen)
		       )
		    {
			// Do a bit of lexical analaysis...
			if (oneChar != '}') 
			    {
				message += oneChar;
				charsRead++;
			    }
			else break;
		    }
		if (oneChar != '}')  throw(new JSONException("End \"}\" not found"));
		message += oneChar + "\n";
		
		//System.err.println("Msg:"+message);
		//return charsRead;
		return message;
	    }
	// catch (IOException e) 
	// 	{
	// 	    String msg = "Error connecting to "+getServerName()+":"+Integer.toString(getServerPort())+"\nCheck settings";
	// 	    uiToast(msg,Gravity.BOTTOM);
	// 	    return msg;
	// 	}
	catch (JSONException e) 
	    {
		//throw new RuntimeException(e);
		System.err.println(e.getMessage());
		uiToast(e.getMessage(),Gravity.BOTTOM);
		return null;
	    }
    };

    public void naaradWriter(PrintWriter socWriter, String message) throws IOException, UnknownHostException
    {
	socWriter.write(mkMessage(message)); socWriter.flush();
    }

    public Socket naaradSocket(String hostName, int port) throws IOException, UnknownHostException
    {
	Socket clientSoc = new Socket(hostName, port);
	
	// int attempts = 0;
	// if (!clientSoc.isConnected() && attempts < 10)
	//     {
	// 	SystemClock.sleep(10);
	// 	attempts++;
	//     }
	// else
	//     throw(new IOException("Naarad could not connection socket: \""+hostName+"\":"+port));
	if (clientSoc.isConnected())
	    return clientSoc;
	else
	    return null;
    }
}
