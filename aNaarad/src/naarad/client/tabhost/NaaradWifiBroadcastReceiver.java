package naarad.client.tabhost;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

public class NaaradWifiBroadcastReceiver extends BroadcastReceiver 
{   
    public void onReceive(Context context, Intent intent) 
    {
	ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

	int tries = 0;
	while (tries < 10)
	    {
		if (mWifi.isConnected())
		    {
			Toast.makeText(context, "Checking Wifi...", Toast.LENGTH_SHORT).show();
			break;
		    }
		else
		    {
			SystemClock.sleep(10);
			tries++;
		    }	
	    }
	if (!mWifi.isConnected())
	    Toast.makeText(context, "Wifi yet not connected...", Toast.LENGTH_SHORT).show();

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
};