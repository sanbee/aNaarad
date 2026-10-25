package naarad.client.tabhost;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.View;
import android.os.Bundle;

import android.widget.ToggleButton;
import android.widget.EditText;
import android.os.SystemClock;
import android.widget.Button;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.util.MathHelper;

import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.os.Build.VERSION;
import java.util.Date;
import java.util.ArrayList;
import android.view.Gravity;
import java.util.Map;
import java.util.HashMap;
// import java.util.Calendar;
// import java.util.TimeZone;

import android.view.Menu;
import android.view.MenuInflater;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONException;
//import android.widget.TabHost;
import java.util.Set;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.graphics.Typeface;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

// class DynamicDataSource implements Runnable 
// {
//     int i=0;
//     private boolean keepRunning = true;
    
//     // @Override 
//     public void run() 
//     {
// 	i=0;
// 	if (keepRunning == true)
// 	    {
// 		keepRunning = false;
// 		Log.i("Run: ","stopping");
// 	    }
// 	else
// 	    {
// 		keepRunning=true;
// 		Log.i("Run: ","starting");
// 	    }
// 	while(keepRunning)
// 	    {
// 		Log.i("Run: ",Integer.toString(i++));
// 		SystemClock.sleep(1000);
// 	    }
//     }
//     public void stopThread() 
//     {
// 	keepRunning = false;
//     }
// }

//public class NaaradPlotFragment extends Fragment 
public class NaaradPlotFragment extends NaaradAbstractFragment
{
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    //    private XYSeriesRenderer mCurrentRenderer;
    private String mDateFormat;
    private Button mNewSeries;
    private Button mAdd;
    private GraphicalView mChartView, mTimeChartView;
    private int index = 0;


    private int apiLevel;
    private static View mView;
    
    //    private Socket client;
    //private PrintWriter printwriter;
    private EditText textField;
    private ToggleButton plotButton;
    private String messsage;

    //private DynamicDataSource dataSource;
    private Thread myThread;
    public NaaradApp myApp;

    private XYSeries series0;//, series1;
    //private ArrayList<XYSeries> seriesList;
    private XYSeriesRenderer renderer0;//,renderer1;
    //protected Update mUpdateTask0, mUpdateTask1;
    protected SensorDataSource mSensorDataSource;
    //protected SensorDataSourceSim mSensorDataSource;
    //protected ArrayList<Update> TaskList;

    private boolean nConnected=false;
    nPlotDataArrivalListener mMainActivityCallback;

    private NaaradPlotter myPlotter;
    private NaaradSensorDataSource nSDS;

    // The container Activity must implement this interface so the frag can deliver messages
    public interface nPlotDataArrivalListener {
        /** Called by NaaradPlotFragment when OTA RF data arrives */
        public void onDataArrival(String json);
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    //    @Override public void onViewCreated(View v, Bundle b) 
    // @Override public void onActivityCreated(Bundle b) 
    // {
    // 	//super.onViewCreated(v,b);
    // 	super.onActivityCreated(b);
    // 	TextView tabLabel;
    // 	//TabHost myTabHost = (TabHost) mView.findViewById(android.R.id.tabhost);
    // 	TabHost myTabHost = ((MainActivity)getActivity()).getTabHost();
    // 	if (myTabHost == null)
    // 	    System.err.println("tabhost == null");
    // 	else
    // 	    {
    // 		tabLabel = (TextView) myTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title); 
    // 		tabLabel.setText("Sensors.");
    // 	    }
    // }
    //
    //-----------------------------------------------------------------------------------------
    //
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
				       Bundle savedInstanceState) 
    {
	apiLevel=android.os.Build.VERSION.SDK_INT;
	System.err.println("NPF onCreateView()");

	//Log.i("API Level: ",android.os.Build.VERSION.RELEASE+" "+Integer.toString(apiLevel));

	setHasOptionsMenu(true);
	setRetainInstance(true);
	myApp = (NaaradApp) getActivity().getApplication();

	if (!recreateView(mView)) 
	    mView = inflater.inflate(R.layout.activity_naarad_plot, container, false);
	

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try 
	    {
		mMainActivityCallback = (nPlotDataArrivalListener) getActivity();
	    } 
	catch (ClassCastException e) 
	    {
		throw new ClassCastException(getActivity().toString()
					     + " must implement mMainActivityCallback");
	    }

	// myPlotter = new NaaradPlotter(getResources().getDisplayMetrics());
	// myPlotter.initMultiRenderer();
	// myPlotter.makeSeries(Color.GREEN);
	// myPlotter.makeSeries(Color.BLUE);
	// nSDS = new NaaradSensorDataSource(myPlotter.mDataset, myPlotter.mRenderer, myPlotter.mTimeChartView);

	initMultiRenderer(mRenderer);
	makeSeries(mRenderer, mDataset, Color.GREEN);
	makeSeries(mRenderer, mDataset, Color.BLUE);
	//
	//--------------------------------------------------------------------------
	//    
	plotButton = (ToggleButton)  mView.findViewById(R.id.plotButton); // reference to the send button
	plotButton.setChecked(true);
	nConnected=true;
	startAllCharts(-1);
	stopAllCharts(1);
	startAllCharts(-1);

	// Button press event listener
	//plotButton.setOnClickListener(new View.OnClickListener() 
	View.OnClickListener plotButtonHandler = new View.OnClickListener() 
	    {
		public void onClick(View v) 
		{
		    android.support.v4.app.FragmentTransaction transaction=getActivity().getSupportFragmentManager().beginTransaction();
		    Fragment f1 = (Fragment) NaaradControlFragment.newInstance("Naarad Control Fragment");
		    
		    // if (f1.getView() == null)
		    // 	System.err.println("it's null!");
		    // else
		    // f1.getView().getBackground().setAlpha(100);

		    boolean on = ((ToggleButton)v).isChecked();
		    
		    if (on)
			transaction.add(R.id.plotRL, f1);
		    else
			//transaction.addToBackStack(f1);
			transaction.hide(f1);
			
		    transaction.commit();

		    // boolean on = ((ToggleButton)v).isChecked();
		    // if (on) 
		    // 	{
		    // 	    //makeChart(mRenderer,mDataset);
		    // 	    nConnected=true;
		    // 	    startAllCharts(-1);
		    // 	}
		    // else 
		    // 	{
		    // 	    nConnected=false;
		    // 	    stopAllCharts(1);
		    // 	}
		    // ((ToggleButton)v).setChecked(nConnected);
		}
	    };
	plotButton.setOnClickListener(plotButtonHandler);
	//
	//--------------------------------------------------------------------------
	//    
	return mView;
    }
    public void stopDataSource()
    {
	if (mSensorDataSource != null) mSensorDataSource.finish();
	//if (nSDS != null) nSDS.finish();
    }
    @Override public void onDestroy() 
    {
	System.err.println("NPF destroyed");
        super.onDestroy();
	// stopDataSource();
	// if (myApp.myWakeLock.isHeld())
	//     {
	// 	myApp.myWakeLock.release();
	// 	System.err.println("Releasing wake lock");
	//     }
    }
    @Override public void onResume() 
    {
	System.err.println("NPF resumed");
        super.onResume();
    }
    
    @Override public void onPause() 
    {
	// if (mUpdateTask0 != null) mUpdateTask0.cancel(true);
	// if (mUpdateTask1 != null) mUpdateTask1.cancel(true);

	System.err.println("NPF paused");
	super.onPause();
	// stopDataSource();
	// if (myApp.myWakeLock.isHeld())
	//     {
	// 	myApp.myWakeLock.release();
	// 	System.err.println("Releasing wake lock");
	//     }
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public static final NaaradPlotFragment newInstanceNPF(String sampleText) 
    {
	NaaradPlotFragment f = new NaaradPlotFragment();
	
	Bundle b = new Bundle();
	b.putString("bString", sampleText);
	f.setArguments(b);
	
	return f;
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    
    private void makeSeries(XYMultipleSeriesRenderer multiRenderer,
			   XYMultipleSeriesDataset multiDataset,
			   int color)
    {
	String seriesTitle = "Series " + (multiDataset.getSeriesCount() + 1);
	XYSeries series0 = new XYSeries(seriesTitle);
	multiDataset.addSeries(series0);
	XYSeriesRenderer renderer0 = new XYSeriesRenderer();
	renderer0.setPointStyle(PointStyle.CIRCLE);
	renderer0.setFillPoints(true);
	renderer0.setLineWidth(2f*getResources().getDisplayMetrics().scaledDensity);
	//renderer0.setColor(Color.GREEN);
	renderer0.setColor(color);
	multiRenderer.addSeriesRenderer(renderer0);
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    private void initMultiRenderer(XYMultipleSeriesRenderer multiRenderer)
    {
	mRenderer.setApplyBackgroundColor(true);
	mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
	mRenderer.setAxisTitleTextSize(10);
	mRenderer.setChartTitleTextSize(10);
	mRenderer.setLabelsTextSize(10);
	mRenderer.setLegendTextSize(10);
	mRenderer.setMargins(new int[] { 2, 20, 5, 2 }); //Top, Left, Bottom, Right
	mRenderer.setZoomButtonsVisible(false);
	mRenderer.setPointSize(2);
	mRenderer.setXTitle("");
	mRenderer.setYTitle("Temperature");
	mRenderer.setShowGrid(true);
	mRenderer.setShowLegend(false);
	//mRenderer.setShowLegend(true);
	mRenderer.setXLabels(10); // No. of xtics
	mRenderer.setClickEnabled(false);
	mRenderer.setPanEnabled(false);
	//multiRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
	DisplayMetrics metrics = getResources().getDisplayMetrics();
	float val = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, metrics);
	mRenderer.setLabelsTextSize(val);
	val = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8, metrics);
	mRenderer.setTextTypeface("sans_serif",Typeface.BOLD);
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public void makeChart(XYMultipleSeriesRenderer multiRenderer,
			  XYMultipleSeriesDataset multiDataset)
    {
	System.err.println("#### makeChart");
	// mDataset.getSeriesAt(0).add(0,new Date().getTime());
	// mDataset.getSeriesAt(0).add(1,new Date().getTime()+30000.0);
	mTimeChartView = ChartFactory.getTimeChartView(getActivity(), mDataset, mRenderer,"hh:mm:ss\ndd/MM");	// "%tT"
	
	LinearLayout layout = (LinearLayout) mView.findViewById(R.id.chart);
	LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
	layout.addView(mTimeChartView,0, lp);
	nConnected=true;
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public void startAllCharts(int n)
    {
	int m0, m1;
	if (n==-1) {m0=0;m1=2;}
	else {m0=n;m1=n+1;}

	makeChart(mRenderer,mDataset);

	if ((n==0) || (n==-1))
	    {
		// For reasons I do not understand, an instance of
		//AsyncTask is not re-usable.  If it needs to be
		//re-used, it has to be construction afresh.  if
		//(mSensorDataSource == null)

		mSensorDataSource = new SensorDataSource();
		//nSDS = new NaaradSensorDataSource(myPlotter.mDataset, myPlotter.mRenderer, myPlotter.mTimeChartView);

		//mSensorDataSource = new SensorDataSourceSim();

		//----------------Use this code -------------------------
		// if (apiLevel <= 9) mSensorDataSource.execute(mDataset.getSeriesAt(0));
		// else               mSensorDataSource.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,mDataset.getSeriesAt(0));

		if (apiLevel <= 9) mSensorDataSource.execute(mDataset);
		else               mSensorDataSource.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,mDataset);

		// if (apiLevel <= 9) nSDS.execute(myPlotter.mDataset);
		// else               nSDS.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,myPlotter.mDataset);

		//----------------Use this code -------------------------
	    }
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    public void stopAllCharts(int n)
    {
	if (mSensorDataSource != null) mSensorDataSource.cancel(true);
	//if (nSDS != null) nSDS.cancel(true);
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    //
    //-----------------------------------------------------------------------------------------
    //
    private int generateRandomNum(int n) 
    {
	Random randomGenerator = new Random();
	int randomVal = randomGenerator.nextInt(n);
	return randomVal;
    }
    //
    //-----------------------------------------------------------------------------------------
    //
    //protected class SensorDataSource extends AsyncTask<XYSeries, XYSeries, String> 
    protected class SensorDataSource extends AsyncTask<XYMultipleSeriesDataset, XYSeries, String> 
    {
	//protected Socket clientSoc;
	//protected PrintWriter socWriter;
	//protected BufferedReader socReader;
	protected int nodeid;
	private boolean allDone=false, doReDraw=true;
	protected double x0,y0, xMax, xMin, yMax, yMin=100.0;
	protected Boolean stopRecording=false;
	//protected XYSeries thisSeries;

	//
	//----------------------------------------------------------------------------------------------
	// Given the various tokens that together form the JSON
	// string, this assembles, parse and publish the data.
	private void publishJSON(String[] jsonTokens, XYMultipleSeriesDataset[] params) throws JSONException
	{
	    String jsonStr="";
	    //for (int j=1;j<3;j++) jsonStr += jsonTokens[j];
	    jsonStr = jsonTokens[1];
	    Log.d("publishJSON: ",jsonTokens[0]+' '+jsonTokens[1]// +' '+jsonTokens[2]
		  );
				    
	    //...publish the data...
	    JSONObject json = new JSONObject(jsonStr);
	    if (json.getInt("rf_fail") == 0) // Indicates that the packet is valid
		{
		    // svolt  = (float)json.getDouble("node_v");
		    // rssi   = (float)json.getDouble("node_p");
		    nodeid = json.getInt("node_id");
		    //x0 =  new Date().getTime();
		    x0 =  json.getDouble("time");
		    //double tt=new Date().getTime();
		    //System.err.println(tt+" "+x0+" "+(tt-x0));
		    y0 = (float)json.getDouble("degc");
		    publishProgress(params[0].getSeriesAt(nodeID2Ndx.get(nodeid)));
		}
	    else
		{
		    uiToast("Packet invalid (rf_fail=1)",Gravity.BOTTOM);	
		    return;
		}
	    mMainActivityCallback.onDataArrival(jsonStr);
	}
	//
	//----------------------------------------------------------------------------------------------
	//
	private void naaradGetDataHistoryEngine(int[] keys, XYMultipleSeriesDataset[] params, Socket mySoc) 
	    throws IOException,JSONException
	{
	    PrintWriter mySocWriter = new PrintWriter(mySoc.getOutputStream(), true);
	    BufferedReader mySocReader = new BufferedReader(new InputStreamReader(mySoc.getInputStream()),100);
	    naaradWriter(mySocWriter,"SensorDataSync");

	    for (int i=0;i<keys.length;i++)
		{
		    //String msg=mkMessage("getcpkt "+Integer.toString(tt[i]));
		    String msg=("gethpkt "+Integer.toString(keys[i]));
		    
		    //System.err.println("Sending: "+msg);
		    naaradWriter(mySocWriter, msg);
		    SystemClock.sleep(100);
		    //System.err.println("Receiving...");
		    String message="", msgData="";
		    doReDraw=false;
		    //while (!msgData.equals("PHINISHED"))
		    while (!msgData.split(" ")[0].equals("PHINISHED"))
			{
			    if ((message = naaradReader(mySocReader)) == null)
				throw(new JSONException("JSON message is null"));
			    //SystemClock.sleep(100);
			    String[] tokens = message.split(" ",2);
			    //String[] tokens = message.split(" ");
			    msgData=tokens[1].trim();
			    //System.err.println("Got: "+message+" \""+msgData+"\""+(msgData.equals("PHINISHED")));
			    //if (!msgData.equals("PHINISHED"))
			    if (!msgData.split(" ")[0].equals("PHINISHED"))
				publishJSON(tokens,params);
			}
		    doReDraw=true;
		    reDraw();
		    //System.err.println("Got all");
		}
	    naaradWriter(mySocWriter, "done");
	    mySoc.close();
	    doReDraw=true;
	}
	//
	//----------------------------------------------------------------------------------------------
	//
	private boolean naaradGetDataOnlyEngine(int[] keys, XYMultipleSeriesDataset[] params, Socket mySoc) 
	    throws IOException, JSONException
	    {
		//throw(new IOException("Wifi not connected after 10 tries"));
		//Socket mySoc = new Socket(getServerName(), getServerPort());
		//SystemClock.sleep(500);
		PrintWriter mySocWriter = new PrintWriter(mySoc.getOutputStream(), true);
		BufferedReader mySocReader = new BufferedReader(new InputStreamReader(mySoc.getInputStream()),100);
		naaradWriter(mySocWriter,"SensorDataSync");
		boolean staleDataDetected=false;
		for (int i=0;i<keys.length;i++)
		    {
			//String msg=mkMessage("getcpkt "+Integer.toString(tt[i]));
			String msg=("getcpkt "+Integer.toString(keys[i])),
			    message="";
			
			//System.err.println("Sending: "+msg);
			
			naaradWriter(mySocWriter, msg);
			SystemClock.sleep(100);
			//System.err.println("Receiving...");
			if ((message = naaradReader(mySocReader)) == null)
			    throw(new JSONException("JSON message is null"));
			//System.err.println("Got: "+message);
			
			//======================================================
			// String[] tokens = message.split(" ");
			// String jsonStr="";
			// for (int j=1;j<3;j++) jsonStr += tokens[j];

			String[] tokens = message.split(" ",2); // Split at the first blank
			double xLatest=x0; // This is the last x0 before publishJSON() updates it?
			double TLIMIT=1000.0*60*60*2;
			publishJSON(tokens, params);
			// An OR operation is required to determine if
			// the timestamp is stale.  Staleness is
			// determined from x0, which is updated by
			// publishJSON() after it publishes the data
			// for the first key.  Data from subsequent
			// keys will have the last timestamp of the
			// previous key in x0.  That may not be stale.
			// The statelDataDetected therefore should be
			// true if the first (or any) key reports
			// stale data.
			staleDataDetected |= (Math.abs(xLatest-x0)>TLIMIT) ; 
			if (staleDataDetected)
			    {
				Log.d("getDataOnly:","Stale data detected: "+Double.toString(Math.abs(xLatest-x0))+" "+Double.toString(TLIMIT)+" "+Boolean.toString(Math.abs(xLatest-x0)>TLIMIT)
				      +" "+Boolean.toString(staleDataDetected));
				//				staleDataDetected=true;
			    }
		    }
		
		//...close the socket.
		naaradWriter(mySocWriter, "done");
		mySoc.close();
		Log.d("getDataOnly:", "Returning " +Boolean.toString(staleDataDetected));
		return staleDataDetected;
	    }
	//
	//----------------------------------------------------------------------------------------------
	//
	private boolean naaradGetData(int[] keys, XYMultipleSeriesDataset[] params,
					  boolean getHistory)
	{
	    boolean staleData=false;
	    try 
		{
		    // Open the socket connection....
		    Socket mySoc;
		    int tries=0;
		    while ((mySoc = naaradSocket(getServerName(), getServerPort()))==null &&
			   tries < 10)
			{
			    tries++;
			    String msg="Wifi not yet connected..."+Integer.toString(tries);
			    uiToast(msg,Gravity.BOTTOM);
			    System.err.println(msg);
			    SystemClock.sleep(100);
			}
		    if (mySoc == null)
			uiToast("Wifi not connected after 10 tries"+Integer.toString(tries),Gravity.BOTTOM);
		    else
			{
			    if (getHistory)
				naaradGetDataHistoryEngine(keys, params, mySoc);
			    else
				staleData=naaradGetDataOnlyEngine(keys, params, mySoc);
			}
		}
	    catch (JSONException e) 
		{
		    //throw new RuntimeException(e);
		    System.err.println(e.getMessage());
		    uiToast(e.getMessage(),Gravity.BOTTOM);
		    //cancel(true);
		}
	    catch (UnknownHostException e) 
		{
		    String msg = "Unknown host: "+getServerName()+":"+Integer.toString(getServerPort())+"\nCheck settings";
		    uiToast(msg,Gravity.BOTTOM);
		} 
	    catch (IOException e) 
		{
		    String msg = "Error connecting to "+getServerName()+":"+Integer.toString(getServerPort())+"\nCheck settings";
		    uiToast(msg,Gravity.BOTTOM);
		}
	    return staleData;
	}
	//
	//----------------------------------------------------------------------------------------------
	//
	// private void naaradGetDataOnly(int[] keys,XYMultipleSeriesDataset[] params)
	// {
	//     try 
	// 	{
	// 	    String message="";
	// 	    // Open the socket connection....
	// 	    Socket mySoc;
	// 	    int tries=0;
	// 	    while ((mySoc = naaradSocket(getServerName(), getServerPort()))==null &&
	// 		   tries < 10)
	// 		{
	// 		    tries++;
	// 		    String msg="Wifi not yet connected..."+Integer.toString(tries);
	// 		    uiToast(msg,Gravity.BOTTOM);
	// 		    System.err.println(msg);
	// 		    SystemClock.sleep(100);
	// 		}
	// 	    if (mySoc == null)
	// 		uiToast("Wifi not connected after 10 tries"+Integer.toString(tries),Gravity.BOTTOM);
	// 	    else
	// 		{
	// 		    //throw(new IOException("Wifi not connected after 10 tries"));
	// 		    //Socket mySoc = new Socket(getServerName(), getServerPort());
	// 		    //SystemClock.sleep(500);
	// 		    PrintWriter mySocWriter = new PrintWriter(mySoc.getOutputStream(), true);
	// 		    BufferedReader mySocReader = new BufferedReader(new InputStreamReader(mySoc.getInputStream()),100);
	// 		    naaradWriter(mySocWriter,"SensorDataSync");
			    
	// 		    for (int i=0;i<keys.length;i++)
	// 			{
	// 			    //String msg=mkMessage("getcpkt "+Integer.toString(tt[i]));
	// 			    String msg=("getcpkt "+Integer.toString(keys[i]));
				    
	// 			    //System.err.println("Sending: "+msg);
				    
	// 			    naaradWriter(mySocWriter, msg);
	// 			    SystemClock.sleep(100);
	// 			    //System.err.println("Receiving...");
	// 			    if ((message = naaradReader(mySocReader)) == null)
	// 				throw(new JSONException("JSON message is null"));
	// 			    //System.err.println("Got: "+message);
				    
	// 			    //======================================================
	// 			    String[] tokens = message.split(" ");
	// 			    String jsonStr="";
	// 			    for (int j=1;j<3;j++) jsonStr += tokens[j];
				    
	// 			    //...publish the data...
	// 			    JSONObject json = new JSONObject(jsonStr);
	// 			    if (json.getInt("rf_fail") == 0) // Indicates that the packet is valid
	// 				{
	// 				    // svolt  = (float)json.getDouble("node_v");
	// 				    // rssi   = (float)json.getDouble("node_p");
	// 				    nodeid = json.getInt("node_id");
	// 				    //x0 =  new Date().getTime();
	// 				    x0 =  json.getDouble("time");
	// 				    double tt=new Date().getTime();
	// 				    System.err.println(tt+" "+x0+" "+(tt-x0));
	// 				    y0 = (float)json.getDouble("degc");
	// 				    publishProgress(params[0].getSeriesAt(nodeID2Ndx.get(nodeid)));
	// 				}
	// 			    else
	// 				throw(new JSONException("Packet invalid (rf_fail=1)"));
	// 			    mMainActivityCallback.onDataArrival(jsonStr);
	// 			}
			    
	// 		    //...close the socket.
	// 		    naaradWriter(mySocWriter, "done");
	// 		}
	// 	}
	//     catch (JSONException e) 
	// 	{
	// 	    //throw new RuntimeException(e);
	// 	    System.err.println(e.getMessage());
	// 	    uiToast(e.getMessage(),Gravity.BOTTOM);
	// 	    cancel(true);
	// 	}
	//     catch (UnknownHostException e) 
	// 	{
	// 	    String msg = "Unknown host: "+getServerName()+":"+Integer.toString(getServerPort())+"\nCheck settings";
	// 	    uiToast(msg,Gravity.BOTTOM);
	// 	} 
	//     catch (IOException e) 
	// 	{
	// 	    String msg = "Error connecting to "+getServerName()+":"+Integer.toString(getServerPort())+"\nCheck settings";
	// 	    uiToast(msg,Gravity.BOTTOM);
	// 	}
	// }
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
	@Override protected String doInBackground(XYMultipleSeriesDataset... params) 
	    {
		String retStr="AllOK";
		{
		    allDone=false;
		    nConnected=true;
		    int keys[] = getKeysAsArray(nodeID2Ndx);
		    //thisSeries = params[0].getSeriesAt(0);
		    
		    // System.err.println("########");
		    // System.err.println("TEMP. DISABLING HISTROCICAL DATA COLLECTION!!!");
		    // System.err.println("########");
		    boolean staleData;
		    staleData=naaradGetData(keys,params,true);
		    
		    while(true && !allDone)
			{
			    //naaradGetDataOnly(keys,params);
			    staleData=naaradGetData(keys,params,false);
			    if (staleData)
				{
				    Log.d("doInBackground:","Stale data detected; loading history records ");
				    naaradGetData(keys,params,true);
				}
			    SystemClock.sleep(60000);
			}
		    System.err.println("Exiting doInBackground");
		}
		
		return retStr;
	    }
	//
	//--------------------------------------------------------------------------
	//    
	public void addNewData(XYSeries thisSeries, double x,double y)
	{
	    double xMax,xMin, dX=0, dT=1000.0*60*60*12;
	    double quantization=0.2;
	    int n;
	    //mRenderer.setXAxisMax(x+360000.0);	    
	    // xMax = thisSeries.getMaxX();
	    // xMin = thisSeries.getMinX();
	    // if (x > xMax) xMax = x;
	    // if (x < xMin)) xMin = x;
	    
	    //double thisY=(y == MathHelper.NULL_VALUE) ? y : ((Math.round(y/0.2))*0.2);
	    //double thisY=Math.round(y/10.0)*10.0;
	    double myY=y;
	    double latestY,dTi,dY;
	    n=thisSeries.getItemCount();
	    if (n == 0) thisSeries.add(x, myY);
	    else
		{
		    dTi=thisSeries.getX(n-1)-x;
		    latestY=thisSeries.getY(n-1);

		    // if (myY != MathHelper.NULL_VALUE)
		    // 	{
		    // 	    double dd=myY-latestY;
		    // 	    if ((dd >0) & (Math.abs(dd) <= quantization))
		    // 		myY=latestY+0.01;
		    // 	    if ((dd <0) & (Math.abs(dd) <= quantization))
		    // 		myY=latestY-0.01;
		    // 	}

		    // if (myY != MathHelper.NULL_VALUE)
		    // 	{
		    // 	    double dd=Math.abs(myY-latestY);
		    // 	    if ((dd >=0) & (dd <= quantization))
		    // 		myY=latestY;
		    // 	}

		    // if (myY != MathHelper.NULL_VALUE)
		    // 	{
		    // 	    dY=myY-latestY;
		    // 	    //myY=latestY+dTi*dY/2.0;
		    // 	    myY=latestY+dY/5.0;
		    // 	    System.err.println("myY: "+latestY+" "+y+" "+myY);
		    // 	    //Log.i("myY",Double.toString(y)+" "+Double.toString(latestY)+" "+Double.toString(dY));
		    // 	}

		    // if (myY != MathHelper.NULL_VALUE)
		    // 	{
		    // 	    //myY=Math.round(y/10.0)*10.0;
		    // 	    if (myY > yMax) yMax = myY;
		    // 	    if (myY < yMin) yMin = myY;
		    // //Log.i("myY1",Double.toString(y)+" "+Double.toString(latestY)+" "+Double.toString(dY));
		    // thisSeries.add(x, myY);
		    // 	}
		    
		    if (dTi >= 60000)
		    	{
		    	    thisSeries.add(x,myY);
		    	    //System.err.println("dTi>=1m: "+myY);
		    	}
		    else
		    	{
		    	    if (myY != latestY)
		    		{
		    		    if (myY != MathHelper.NULL_VALUE)
		    			{
		    			    //myY=Math.round(y/10.0)*10.0;
		    			    if (myY > yMax) yMax = myY;
		    			    if (myY < yMin) yMin = myY;
		    			}
		    		    //Log.i("myY1",Double.toString(y)+" "+Double.toString(latestY)+" "+Double.toString(dY));
		    		    thisSeries.add(x, myY);
		    		    //System.err.println("Val: "+myY+" "+yMax+" "+yMin);
		    		}
			}
		}

	    if (n>0)
	    	{
	    	    xMax = thisSeries.getX(n-1);
	    	    xMin = thisSeries.getX(0);
	    	    dX=xMax-xMin;
	    	    if (dX > dT)
			{
			    thisSeries.remove(0);
			    //System.err.println("xrange0"+Double.toString(xMax)+" "+Double.toString(xMin)+" "+Double.toString(dX)+" "+Integer.toString(n));
			}
	    	}
	    
	    
	    // if (dX > dT)
	    // 	{
	    // 	    xMax = mCurrentSeries.getX(n-1);
	    // 	    xMin = mCurrentSeries.getX(0);
	    // 	    dX=xMax-xMin;
	    // 	    Log.i("xrange1",Double.toString(xMax)+" "+Double.toString(xMin)+" "+Double.toString(dX)+" "+Integer.toString(n));
	    // 	}
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
			// mRenderer.setYAxisMax(40.0);
			// mRenderer.setYAxisMin(-10.0);
			mTimeChartView.repaint();
		    }
	}
	//
	//--------------------------------------------------------------------------
	//    
	//	@Override protected void onProgressUpdate(Integer... values) 
	@Override protected void onProgressUpdate(XYSeries... values) 
	    {
		super.onProgressUpdate(values);
		
		//mCurrentSeries.add(x, y);
		//addNewData(mCurrentSeries,x0,y0);
		//		Log.i("onProg: ",seriesTitle+Integer.toString(values[0].getItemCount()));
		addNewData(values[0],x0,y0);
		reDraw();
		// if ((mTimeChartView != null) && (stopRecording==false))
		//     {
		// 	mTimeChartView.zoomReset();
		// 	mRenderer.setYAxisMax(yMax+0.2);
		// 	mRenderer.setYAxisMin(yMin-0.2);
		// 	// mRenderer.setYAxisMax(40.0);
		// 	// mRenderer.setYAxisMin(-10.0);
		// 	mTimeChartView.repaint();
		//     }



		// Bitmap bitmap = mChartView.toBitmap();
		// try {
		// 	File file = new File(Environment.getExternalStorageDirectory(),
		// 			"test" + index++ + ".png");
		// 	FileOutputStream output = new FileOutputStream(file);
		// 	bitmap.compress(CompressFormat.PNG, 100, output);
		
		// } catch (Exception e) {
		// 	e.printStackTrace();
		// }
	    }
	
	
	@Override protected void onCancelled() 
	    {
		System.err.println("NPF AsyncTask onCancelled()");
		
		//finish();


		// try
		//     {
		// 	socWriter.write(mkMessage("done"));
		// 	socWriter.flush();
		// 	clientSoc.close();
		//     }
		// catch (UnknownHostException e) 
		//     {
		// 	System.err.println("Unknown host exception caught in SensorDataSource::onCancelled()");
		//     } 
		// catch (IOException e) 
		//     {
		// 	System.err.println("IOException caught in SensorDataSource::onCancelled()");
		//     }
		super.onCancelled();
	    }
    }
    //============================================================================================================
    //
    // A class to simulate data packets internally.  This is mainly
    // for use for testing the handling of data packets throughout the
    // app.  NPF is the only class that recevies the packets from
    // outside.  They are processed in this class and sent to the rest
    // of the app as necessary.
    //

    // protected class SensorDataSourceSim extends AsyncTask<XYMultipleSeriesDataset, XYSeries, String> 
    // {
    // 	protected Socket clientSoc;
    // 	protected PrintWriter socWriter;
    // 	protected BufferedReader socReader;
    // 	protected float temp, svolt, rssi;
    // 	protected int nodeid;
    // 	private boolean allDone=false;
    // 	private String message = "";
    // 	protected double x0,y0, xMax, xMin, yMax, yMin;
    // 	protected Boolean stopRecording=false;
    // 	protected XYSeries thisSeries;
    // 	int nPkts=0;
    // 	private void stopRecording(Boolean status)
    // 	{
    // 	    stopRecording = status;
    // 	}
	
    // 	public String naaradReader()
    // 	{
    // 	    String message="";
    // 	    SystemClock.sleep(2000);
    // 	    message="";
    // 	    double y0 = generateRandomNum(10)/10.0+20.10;
    // 	    nPkts++;
    // 	    if (nPkts%10==0)
    // 		message = "10 {\"rf_fail\":0,\"node_v\":2.2,\"node_p\":-50,\"node_id\":3,\"degc\":"+Double.toString(y0)+" }";
    // 	    else
    // 		message = "10 {\"rf_fail\":0,\"node_v\":2.2,\"node_p\":-50,\"node_id\":1,\"degc\":"+Double.toString(y0)+" }";
    // 	    System.err.println("M: "+message);
    // 	    int charsRead = message.length();
    // 	    return message;
    // 	};
    // 	public void finish() {allDone=true;};
	
    // 	@Override protected String doInBackground(XYMultipleSeriesDataset... params) 
    // 	    {
    // 		String retStr="AllOK";
    // 		{
    // 		    thisSeries = params[0].getSeriesAt(0);
    // 		    SystemClock.sleep(500);
    // 		    allDone=false;
    // 		    nConnected=true;
		    
		    
    // 		    while(true && !allDone)
    // 			{
    // 			    {
    // 				int charsRead = 0;
    // 				char[] buffer = new char[1024];
    // 				char oneChar;
				
    // 				while ((message = naaradReader()) != null)
    // 				    {
    // 					System.err.println("message="+message);
    // 					break;
    // 				    }
    // 				try 
    // 				    {
    // 					String[] tokens = message.split(" ");
    // 					String jsonStr="";
    // 					for (int j=1;j<3;j++) jsonStr += tokens[j];
					
    // 					JSONObject json = new JSONObject(jsonStr);
    // 					if (json.getInt("rf_fail") == 0) // Indicates that the packet is valid
    // 					    {
    // 						temp   = (float)json.getDouble("degc");
    // 						svolt  = (float)json.getDouble("node_v");
    // 						rssi   = (float)json.getDouble("node_p");
    // 						nodeid = json.getInt("node_id");
    // 						x0 =  new Date().getTime();
    // 						y0 = temp;
    // 						publishProgress(thisSeries);
    // 					    }
    // 					else
    // 					    throw(new JSONException("Packet invalid (rf_fail=1)"));
    // 					mMainActivityCallback.onDataArrival(jsonStr);
    // 				    }
    // 				catch (JSONException e) 
    // 				    {
    // 					System.err.println(e.getMessage());
    // 					uiToast(e.getMessage(),Gravity.BOTTOM);
    // 					cancel(true);
    // 				    }
    // 			    }
    // 			}
    // 		}
		
    // 		{
    // 		    SystemClock.sleep(500);
    // 		    nConnected=false;
    // 		}
		
    // 		return retStr;
    // 	    }
    // 	//
    // 	//--------------------------------------------------------------------------
    // 	//    
    // 	public void addNewData(XYSeries thisSeries, double x,double y)
    // 	{
    // 	    double xMax,xMin, dX=0, dT=60000.0*10;
    // 	    int n;
    // 	    //mRenderer.setXAxisMax(x+360000.0);	    
    // 	    // xMax = thisSeries.getMaxX();
    // 	    // xMin = thisSeries.getMinX();
    // 	    // if (x > xMax) xMax = x;
    // 	    // if (x < xMin)) xMin = x;
	    
    // 	    n=thisSeries.getItemCount();
    // 	    if (n>0)
    // 	    	{
    // 	    	    xMax = thisSeries.getX(n-1);
    // 	    	    xMin = thisSeries.getX(0);
    // 	    	    dX=xMax-xMin;
    // 		    // Log.i("xrange0",Double.toString(xMax)+" "+Double.toString(xMin)+" "+Double.toString(dX)+" "+Integer.toString(n));
    // 	    	    if (dX > dT) thisSeries.remove(0);
    // 	    	}
    // 	    if (n == 1)
    // 		{
    // 		    mRenderer.setXAxisMin(thisSeries.getX(0));
    // 		    mRenderer.setXAxisMax(thisSeries.getX(0)+30000);
    // 		}
    // 	    if ((y != MathHelper.NULL_VALUE) && (y > yMax)) yMax = y;
    // 	    if ((y != MathHelper.NULL_VALUE) && (y < yMin)) yMin = y;
    // 	    thisSeries.add(x, y);
	    
    // 	    // if (dX > dT)
    // 	    // 	{
    // 	    // 	    xMax = mCurrentSeries.getX(n-1);
    // 	    // 	    xMin = mCurrentSeries.getX(0);
    // 	    // 	    dX=xMax-xMin;
    // 	    // 	    Log.i("xrange1",Double.toString(xMax)+" "+Double.toString(xMin)+" "+Double.toString(dX)+" "+Integer.toString(n));
    // 	    // 	}
    // 	}
    // 	//
    // 	//--------------------------------------------------------------------------
    // 	//    
    // 	//	@Override protected void onProgressUpdate(Integer... values) 
    // 	@Override protected void onProgressUpdate(XYSeries... values) 
    // 	    {
    // 		super.onProgressUpdate(values);
		
    // 		//mCurrentSeries.add(x, y);
    // 		//addNewData(mCurrentSeries,x0,y0);
    // 		//		Log.i("onProg: ",seriesTitle+Integer.toString(values[0].getItemCount()));
    // 		addNewData(values[0],x0,y0);
		
    // 		if ((mTimeChartView != null) && (stopRecording==false))
    // 		    {
    // 			mTimeChartView.zoomReset();
    // 			// mRenderer.setYAxisMax(yMax);
    // 			// mRenderer.setYAxisMin(yMin);
    // 			mRenderer.setYAxisMax(40.0);
    // 			mRenderer.setYAxisMin(-10.0);
    // 			mTimeChartView.repaint();
    // 		    }
    // 		// Bitmap bitmap = mChartView.toBitmap();
    // 		// try {
    // 		// 	File file = new File(Environment.getExternalStorageDirectory(),
    // 		// 			"test" + index++ + ".png");
    // 		// 	FileOutputStream output = new FileOutputStream(file);
    // 		// 	bitmap.compress(CompressFormat.PNG, 100, output);
		
    // 		// } catch (Exception e) {
    // 		// 	e.printStackTrace();
    // 		// }
    // 	    }
    // 	@Override protected void onCancelled() 
    // 	    {
    // 		finish();
    // 		super.onCancelled();
    // 	    }
    // }
    //============================================================================================================
}
