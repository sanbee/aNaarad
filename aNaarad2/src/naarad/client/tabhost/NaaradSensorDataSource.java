package naarad.client.tabhost;

import android.os.AsyncTask;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.util.MathHelper;

import org.json.JSONObject;
import org.json.JSONException;
import java.net.Socket;
import java.io.IOException;
import android.view.Gravity;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import android.os.SystemClock;
import org.achartengine.util.MathHelper;
import android.util.Log;
import java.net.UnknownHostException;

//public class NaaradSensorDataSource extends AsyncTask<XYMultipleSeriesDataset, XYSeries, String>
public class NaaradSensorDataSource
{
    protected int nodeid;
    private boolean allDone=false, doReDraw=true;
    protected double x0,y0, xMax, xMin, yMax, yMin=100.0;
    protected Boolean stopRecording=false;
    private NaaradUtils nU;

    private XYMultipleSeriesDataset mDataset = null;
    private XYMultipleSeriesRenderer mRenderer = null;
    private GraphicalView mTimeChartView = null;

    //protected XYSeries thisSeries;

    public NaaradSensorDataSource(XYMultipleSeriesDataset thisDataset,
				  XYMultipleSeriesRenderer thisRenderer,
				  GraphicalView  thisTimeChartView)
    {
	mDataset = thisDataset;
	mRenderer = thisRenderer;
	mTimeChartView = thisTimeChartView;
    }
    
    //
    //----------------------------------------------------------------------------------------------
    // Given the various tokens that together form the JSON
    // string, this assembles, parse and publish the data.
    private void publishJSON(String[] jsonTokens, XYMultipleSeriesDataset[] params) throws JSONException
    {
	String jsonStr="";
	for (int j=1;j<3;j++) jsonStr += jsonTokens[j];
	
	//...publish the data...
	JSONObject json = new JSONObject(jsonStr);
	if (json.getInt("rf_fail") == 0) // Indicates that the packet is valid
	    {
		nodeid = json.getInt("node_id");
		x0 =  json.getDouble("time");
		y0 = (float)json.getDouble("degc");
		publishProgress(params[0].getSeriesAt(nU.nodeID2Ndx.get(nodeid)));
	    }
	else
	    {
		nU.uiToast("Packet invalid (rf_fail=1)",Gravity.BOTTOM);	
		return;
	    }
	//mMainActivityCallback.onDataArrival(jsonStr);
    }
    //
    //----------------------------------------------------------------------------------------------
    //
    private void naaradGetDataHistoryEngine(int[] keys, XYMultipleSeriesDataset[] params, Socket mySoc) 
	throws IOException,JSONException
	{
	    PrintWriter mySocWriter = new PrintWriter(mySoc.getOutputStream(), true);
	    BufferedReader mySocReader = new BufferedReader(new InputStreamReader(mySoc.getInputStream()),100);
	    nU.naaradWriter(mySocWriter,"SensorDataSync");
	    
	    for (int i=0;i<keys.length;i++)
		{
		    String msg=("gethpkt "+Integer.toString(keys[i]));
		    nU.naaradWriter(mySocWriter, msg);
		    SystemClock.sleep(100);
		    String message="", msgData="";
		    doReDraw=false;
		    while (!msgData.equals("PHINISHED"))
			{
			    if ((message = nU.naaradReader(mySocReader)) == null)
				throw(new JSONException("JSON message is null"));
			    //SystemClock.sleep(100);
			    String[] tokens = message.split(" ");
			    msgData=tokens[1].trim();
			    //System.err.println("Got: "+message+" \""+msgData+"\""+(msgData.equals("PHINISHED")));
			    if (!msgData.equals("PHINISHED"))
				publishJSON(tokens,params);
			}
		    doReDraw=true;
		    reDraw();
		    //System.err.println("Got all");
		}
	    nU.naaradWriter(mySocWriter, "done");
	    mySoc.close();
	    doReDraw=true;
	}
    //
    //----------------------------------------------------------------------------------------------
    //
    private void naaradGetDataOnlyEngine(int[] keys, XYMultipleSeriesDataset[] params, Socket mySoc) 
	throws IOException, JSONException
	{
	    PrintWriter mySocWriter = new PrintWriter(mySoc.getOutputStream(), true);
	    BufferedReader mySocReader = new BufferedReader(new InputStreamReader(mySoc.getInputStream()),100);
	    nU.naaradWriter(mySocWriter,"SensorDataSync");
	    
	    for (int i=0;i<keys.length;i++)
		{
		    String msg=("getcpkt "+Integer.toString(keys[i])),
			message="";
		    
		    nU.naaradWriter(mySocWriter, msg);
		    SystemClock.sleep(100);
		    if ((message = nU.naaradReader(mySocReader)) == null)
			throw(new JSONException("JSON message is null"));
		    //======================================================
		    String[] tokens = message.split(" ");
		    String jsonStr="";
		    for (int j=1;j<3;j++) jsonStr += tokens[j];
		    publishJSON(tokens, params);
		}
	    
	    //...close the socket.
	    nU.naaradWriter(mySocWriter, "done");
	    mySoc.close();
	}
    //
    //----------------------------------------------------------------------------------------------
    //
    private void naaradGetData(int[] keys, XYMultipleSeriesDataset[] params,
			       boolean getHistory)
    {
	try 
	    {
		// Open the socket connection....
		Socket mySoc;
		int tries=0;
		while ((mySoc = nU.naaradSocket(nU.getServerName(), nU.getServerPort()))==null &&
		       tries < 10)
		    {
			tries++;
			String msg="Wifi not yet connected..."+Integer.toString(tries);
			nU.uiToast(msg,Gravity.BOTTOM);
			System.err.println(msg);
			SystemClock.sleep(100);
		    }
		if (mySoc == null)
		    nU.uiToast("Wifi not connected after 10 tries"+Integer.toString(tries),Gravity.BOTTOM);
		else
		    {
			if (getHistory)
			    naaradGetDataHistoryEngine(keys, params, mySoc);
			else
			    naaradGetDataOnlyEngine(keys, params, mySoc);
		    }
	    }
	catch (JSONException e) 
	    {
		//throw new RuntimeException(e);
		System.err.println(e.getMessage());
		nU.uiToast(e.getMessage(),Gravity.BOTTOM);
		//cancel(true);
	    }
	catch (UnknownHostException e) 
	    {
		String msg = "Unknown host: "+nU.getServerName()+":"+Integer.toString(nU.getServerPort())+"\nCheck settings";
		nU.uiToast(msg,Gravity.BOTTOM);
	    } 
	catch (IOException e) 
	    {
		String msg = "Error connecting to "+nU.getServerName()+":"+Integer.toString(nU.getServerPort())+"\nCheck settings";
		nU.uiToast(msg,Gravity.BOTTOM);
	    }
	
    }
    //
    //----------------------------------------------------------------------------------------------
    //
    private void stopRecording(Boolean status)
    {
	stopRecording = status;
    }
    //
    //----------------------------------------------------------------------------------------------
    //
    public void finish() {allDone=true;};
    //
    //----------------------------------------------------------------------------------------------
    //
    //	@Override protected String doInBackground(XYSeries... params) 
    // public String doInBackground(XYMultipleSeriesDataset... params) 
    // 	{
    // 	    String retStr="AllOK";
    // 	    {
    // 		allDone=false;
    // 		int keys[] = nU.getKeysAsArray(nU.nodeID2Ndx);
    // 		naaradGetData(keys,params,true);
		
    // 		while(true && !allDone)
    // 		    {
    // 			//naaradGetDataOnly(keys,params);
    // 			naaradGetData(keys,params,false);
    // 			SystemClock.sleep(60000);
    // 		    }
    // 	    }
	    
    // 	    return retStr;
    // 	}
    //
    //--------------------------------------------------------------------------
    //    
    public void addNewData(XYSeries thisSeries, double x,double y)
    {
	double xMax,xMin, dX=0, dT=1000.0*60*60*12;
	int n;
	n=thisSeries.getItemCount();
	if ((n > 0))
	    {
		if ((thisSeries.getX(n-1)-x) >= 300000)
		    thisSeries.add(x,y);
		else
		    {
			if (y != thisSeries.getY(n-1))
			    {
				if (y != MathHelper.NULL_VALUE)
				    {
					if (y > yMax) yMax = y;
					if (y < yMin) yMin = y;
				    }
				//System.err.println("Val: "+y+" "+yMax+" "+yMin);
				thisSeries.add(x, y);
			    }
		    }
	    }
	else
	    thisSeries.add(x, y);
	if (n>0)
	    {
		xMax = thisSeries.getX(n-1);
		xMin = thisSeries.getX(0);
		dX=xMax-xMin;
		Log.i("xrange0",Double.toString(xMax)+" "+Double.toString(xMin)+" "+Double.toString(dX)+" "+Integer.toString(n));
		if (dX > dT) thisSeries.remove(0);
	    }
    }
    //
    //--------------------------------------------------------------------------
    //    
    protected void reDraw()
    {
	if (doReDraw)
	    if ((mTimeChartView != null) && (stopRecording==false))
		{
		    mTimeChartView.zoomReset();
		    mRenderer.setYAxisMax(yMax+0.2);
		    mRenderer.setYAxisMin(yMin-0.2);
		    mTimeChartView.repaint();
		}
    }
    //
    //--------------------------------------------------------------------------
    //    
    //	@Override protected void onProgressUpdate(Integer... values) 
    //@Override protected void onProgressUpdate(XYSeries... values) 
    protected void publishProgress(XYSeries... values)
	{
	    //super.onProgressUpdate(values);

	    addNewData(values[0],x0,y0);
	    reDraw();
	}
    
    
    //@Override protected void onCancelled() 
    protected void phinish()
	{
	    finish();
	}
}
