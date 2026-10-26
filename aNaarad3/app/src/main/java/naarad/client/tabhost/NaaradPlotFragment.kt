package naarad.client.tabhost

// import java.util.Calendar;
// import java.util.TimeZone;
//import android.widget.TabHost;
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.sanbee.anaarad.R
import org.achartengine.ChartFactory
import org.achartengine.GraphicalView
import org.achartengine.chart.PointStyle
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.model.XYSeries
import org.achartengine.renderer.XYMultipleSeriesRenderer
import org.achartengine.renderer.XYSeriesRenderer
import org.achartengine.util.MathHelper
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException
import java.util.Random

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
class NaaradPlotFragment : NaaradAbstractFragment() {
    private val mDataset: XYMultipleSeriesDataset = XYMultipleSeriesDataset()
    private val mRenderer: XYMultipleSeriesRenderer = XYMultipleSeriesRenderer()

    //    private XYSeriesRenderer mCurrentRenderer;
    private val mDateFormat: String? = null
    private val mNewSeries: Button? = null
    private val mAdd: Button? = null
    private val mChartView: GraphicalView? = null
    private var mTimeChartView: GraphicalView? = null
    private val index = 0


    private var apiLevel = 0

    //    private Socket client;
    //private PrintWriter printwriter;
    private val textField: EditText? = null

    //private ToggleButton plotButton;
    private val messsage: String? = null

    //private DynamicDataSource dataSource;
    private val myThread: Thread? = null
    var myApp: NaaradApp? = null

    private val series0: XYSeries? = null //, series1;

    //private ArrayList<XYSeries> seriesList;
    private val renderer0: XYSeriesRenderer? = null //,renderer1;

    //protected Update mUpdateTask0, mUpdateTask1;
    protected var mSensorDataSource: SensorDataSource? = null

    //protected SensorDataSourceSim mSensorDataSource;
    //protected ArrayList<Update> TaskList;
    private var nConnected = false
    var mMainActivityCallback: nPlotDataArrivalListener? = null

    private var myPlotter: NaaradPlotter? = null
    private var nSDS: NaaradSensorDataSource? = null

    // The container Activity must implement this interface so the frag can deliver messages
    interface nPlotDataArrivalListener {
        /** Called by NaaradPlotFragment when OTA RF data arrives  */
        fun onDataArrival(json: String?)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val msg = intent.getBundleExtra("RFPacket")
            val jsonStr = msg!!.getStringArrayList("JSONPackets")
            Log.i("BCR:", "Got it!")
        }
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        apiLevel = Build.VERSION.SDK_INT

        //Log.i("API Level: ",android.os.Build.VERSION.RELEASE+" "+Integer.toString(apiLevel));
        setHasOptionsMenu(true)
        setRetainInstance(true)
//	val myApp = getActivity().getApplication() as NaaradApp

//	val myApp = (getActivity()?.getApplication() as? NaaradApp) 
	val currentActivity = activity
	if (currentActivity != null) {
	myApp = currentActivity.application as NaaradApp
	} else {
	// Log an error or handle the missing activity state here safely
	}

        if (!recreateView(mView))
	    mView = inflater.inflate(R.layout.activity_naarad_plot, container, false)
	

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mMainActivityCallback = activity as nPlotDataArrivalListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                activity.toString()
                        + " must implement mMainActivityCallback"
            )
        }

        myPlotter = NaaradPlotter(resources.displayMetrics)
        myPlotter!!.initMultiRenderer()
        myPlotter!!.makeSeries(Color.GREEN)
        myPlotter!!.makeSeries(Color.BLUE)
        nSDS = NaaradSensorDataSource(
            myPlotter!!.mDataset,
            myPlotter!!.mRenderer,
            myPlotter!!.mTimeChartView
        )
        myPlotter!!.makeChart(activity, mView!!)

        initMultiRenderer(mRenderer)
        makeSeries(mRenderer, mDataset, Color.GREEN)
        makeSeries(mRenderer, mDataset, Color.BLUE)
        //
        //--------------------------------------------------------------------------
        //    
        // plotButton = (ToggleButton)  mView.findViewById(R.id.plotButton); // reference to the send button
        // plotButton.setChecked(false);
        nConnected = true
        startAllCharts(-1)
        stopAllCharts(1)
        startAllCharts(-1)

        // Button press event listener
        //plotButton.setOnClickListener(new View.OnClickListener() 
        /*
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

		    
		    transaction.add(R.id.plotRL, f1);
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
	*/
        //
        //--------------------------------------------------------------------------
        //    
        return mView!!
    }

    fun stopDataSource() {
        if (mSensorDataSource != null) mSensorDataSource!!.finish()
    }

    override fun onDestroy() {
        System.err.println("NPF destroyed")
        super.onDestroy()
        stopDataSource()
    }

    override fun onResume() {
        System.err.println("NPF resumed")
        super.onResume()
    }

    override fun onPause() {
        // if (mUpdateTask0 != null) mUpdateTask0.cancel(true);
        // if (mUpdateTask1 != null) mUpdateTask1.cancel(true);

        System.err.println("NPF paused")
        super.onPause()
        stopDataSource()
        if (myApp!!.myWakeLock!!.isHeld) {
            myApp!!.myWakeLock!!.release()
            System.err.println("Releasing wake lock")
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    private fun makeSeries(
        multiRenderer: XYMultipleSeriesRenderer,
        multiDataset: XYMultipleSeriesDataset,
        color: Int
    ) {
        val seriesTitle = "Series " + (multiDataset.seriesCount + 1)
        val series0: XYSeries = XYSeries(seriesTitle)
        multiDataset.addSeries(series0)
        val renderer0: XYSeriesRenderer = XYSeriesRenderer()
        renderer0.pointStyle = PointStyle.CIRCLE
        renderer0.isFillPoints = true
        renderer0.lineWidth = 2f
        //renderer0.setColor(Color.GREEN);
        renderer0.color = color
        multiRenderer.addSeriesRenderer(renderer0)
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    private fun initMultiRenderer(multiRenderer: XYMultipleSeriesRenderer?) {
        mRenderer.isApplyBackgroundColor = true
        mRenderer.backgroundColor = Color.argb(100, 50, 50, 50)
        mRenderer.axisTitleTextSize = 10f
        mRenderer.chartTitleTextSize = 10f
        mRenderer.labelsTextSize = 10f
        mRenderer.legendTextSize = 10f
        mRenderer.margins = intArrayOf(2, 20, 5, 2) //Top, Left, Bottom, Right
        mRenderer.isZoomButtonsVisible = false
        mRenderer.pointSize = 2f
        mRenderer.xTitle = ""
        mRenderer.yTitle = "Temperature"
        mRenderer.setShowGrid(true)
        mRenderer.isShowLegend = false
        mRenderer.xLabels = 10 // No. of xtics
        mRenderer.isClickEnabled = false
        mRenderer.setPanEnabled(false, false)
        //multiRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        val metrics: DisplayMetrics? = resources.displayMetrics
        var `val` = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, metrics)
        mRenderer.labelsTextSize = `val`
        `val` = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8f, metrics)
        mRenderer.setTextTypeface("sans_serif", Typeface.BOLD)
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun makeChart(
        multiRenderer: XYMultipleSeriesRenderer?,
        multiDataset: XYMultipleSeriesDataset?
    ) {
        System.err.println("#### makeChart")
        // mDataset.getSeriesAt(0).add(0,new Date().getTime());
        // mDataset.getSeriesAt(0).add(1,new Date().getTime()+30000.0);
        mTimeChartView = ChartFactory.getTimeChartView(
            activity,
            mDataset,
            mRenderer,
            "hh:mm:ss\ndd/MM"
        ) // "%tT"

        val layout: LinearLayout = mView!!.findViewById<View?>(R.id.chart) as LinearLayout
        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT
        )
        layout.addView(mTimeChartView, 0, lp)
        nConnected = true
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun startAllCharts(n: Int) {
        val m0: Int
        val m1: Int
        if (n == -1) {
            m0 = 0
            m1 = 2
        } else {
            m0 = n
            m1 = n + 1
        }

        makeChart(mRenderer, mDataset)

        if ((n == 0) || (n == -1)) {
            // For reasons I do not understand, an instance of
            //AsyncTask is not re-usable.  If it needs to be
            //re-used, it has to be construction afresh.  if
            //(mSensorDataSource == null)

            mSensorDataSource = SensorDataSource()

            if (apiLevel <= 9) mSensorDataSource?.execute(mDataset)
            else mSensorDataSource?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mDataset)
            //----------------Use this code -------------------------
        }
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    fun stopAllCharts(n: Int) {
    //        if (mSensorDataSource != null)
	   mSensorDataSource?.cancel(true)
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    //
    //-----------------------------------------------------------------------------------------
    //
    private fun generateRandomNum(n: Int): Int {
        val randomGenerator = Random()
        val randomVal = randomGenerator.nextInt(n)
        return randomVal
    }

    //
    //-----------------------------------------------------------------------------------------
    //
    //protected class SensorDataSource extends AsyncTask<XYSeries, XYSeries, String> 
    protected inner class SensorDataSource :
        AsyncTask<XYMultipleSeriesDataset?, XYSeries?, String?>() {
        //protected Socket clientSoc;
        //protected PrintWriter socWriter;
        //protected BufferedReader socReader;
        protected var nodeid: Int = 0
        private var allDone = false
        private var doReDraw = true
        protected var x0: Double = 0.0
        protected var y0: Double = 0.0
        protected var xMax: Double = 0.0
        protected var xMin: Double = 0.0
        protected var yMax: Double = 0.0
        protected var yMin: Double = 100.0
        protected var stopRecording: Boolean = false

        //protected XYSeries thisSeries;
        //
        //----------------------------------------------------------------------------------------------
        // Given the various tokens that together form the JSON
        // string, this assembles, parse and publish the data.
        @Throws(JSONException::class)
        private fun publishJSON(
            jsonTokens: Array<String?>,
            params: Array<out XYMultipleSeriesDataset?>
        ) {
            var jsonStr: String? = ""
            for (j in 1..2) jsonStr += jsonTokens[j]


            //...publish the data...
            val json = JSONObject(jsonStr)
            if (json.getInt("rf_fail") == 0)  // Indicates that the packet is valid
            {
                nodeid = json.getInt("node_id")
                x0 = json.getDouble("time")
                y0 = json.getDouble("degc").toFloat().toDouble()
                val seriesCount = params[0]?.seriesCount ?: 0
                if (seriesCount > 0) {
                    val ndx = nodeID2Ndx?.get(nodeid) ?: (Math.abs(nodeid) % seriesCount)
                    val series = params[0]?.getSeriesAt(ndx)
                    if (series != null) {
                        publishProgress(series)
                    }
                }
            } else {
                uiToast("Packet invalid (rf_fail=1)", Gravity.BOTTOM)
                return
            }
            activity?.runOnUiThread {
                mMainActivityCallback?.onDataArrival(jsonStr)
            }
        }

        //
        //----------------------------------------------------------------------------------------------
        //
        @Throws(IOException::class, JSONException::class)
        private fun naaradGetDataHistoryEngine(
            keys: IntArray,
            params: Array<out XYMultipleSeriesDataset?>,
            mySoc: Socket
        ) {
            val mySocWriter = PrintWriter(mySoc.getOutputStream(), true)
            val mySocReader = BufferedReader(InputStreamReader(mySoc.getInputStream()), 100)
            naaradWriter(mySocWriter, "SensorDataSync")

            for (i in keys.indices) {
                val msg = ("gethpkt " + keys[i].toString())
                naaradWriter(mySocWriter, msg)
                SystemClock.sleep(100)
                var message: String? = ""
                var msgData: String? = ""
                doReDraw = false
                while (msgData != "PHINISHED") {
                    if ((naaradReader(mySocReader).also {
                            message = it
                        }) == null) throw (JSONException("JSON message is null"))
                    val tokens: Array<String?> =
                        message!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    msgData = tokens[1]!!.trim { it <= ' ' }
                    if (msgData != "PHINISHED") publishJSON(tokens, params)
                }
                doReDraw = true
                reDraw()
            }
            naaradWriter(mySocWriter, "done")
            mySoc.close()
            doReDraw = true
        }

        @Throws(IOException::class, JSONException::class)
        private fun naaradGetDataOnlyEngine(
            keys: IntArray,
            params: Array<out XYMultipleSeriesDataset?>,
            mySoc: Socket
        ) {
            val mySocWriter = PrintWriter(mySoc.getOutputStream(), true)
            val mySocReader = BufferedReader(InputStreamReader(mySoc.getInputStream()), 100)
            naaradWriter(mySocWriter, "SensorDataSync")

            for (i in keys.indices) {
                val msg = ("getcpkt " + keys[i].toString())
                var message = ""
                naaradWriter(mySocWriter, msg)
                SystemClock.sleep(100)
                if ((naaradReader(mySocReader).also {
                        message = it!!
                    }) == null) throw (JSONException("JSON message is null"))

                val tokens: Array<String?> =
                    message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var jsonStr: String? = ""
                for (j in 1..2) jsonStr += tokens[j]
                publishJSON(tokens, params)
            }

            naaradWriter(mySocWriter, "done")
            mySoc.close()
        }

        private fun naaradGetData(
            keys: IntArray, params: Array<out XYMultipleSeriesDataset?>,
            getHistory: Boolean
        ) {
            try {
                var mySoc: Socket? = null
                var tries = 0
                val host = serverName ?: defaultServer ?: "raspberrypi"
                val port = serverPort
                while (tries < 10) {
                    mySoc = naaradSocket(host, port)
                    if (mySoc != null) break
                    tries++
                    val msg = "Connecting to $host:$port (try $tries/10)..."
                    uiToast(msg, Gravity.BOTTOM)
                    System.err.println(msg)
                    SystemClock.sleep(500)
                }
                if (mySoc == null) uiToast(
                    "Could not connect to $host:$port after 10 tries",
                    Gravity.BOTTOM
                )
                else {
                    if (getHistory) naaradGetDataHistoryEngine(keys, params, mySoc)
                    else naaradGetDataOnlyEngine(keys, params, mySoc)
                }
            } catch (e: JSONException) {
                System.err.println(e.message)
                uiToast(e.message ?: "JSONException", Gravity.BOTTOM)
            } catch (e: UnknownHostException) {
                val msg =
                    "Unknown host: " + serverName + ":" + serverPort.toString() + "\nCheck settings"
                uiToast(msg, Gravity.BOTTOM)
            } catch (e: IOException) {
                val msg =
                    "Error connecting to " + serverName + ":" + serverPort.toString() + "\nCheck settings"
                uiToast(msg, Gravity.BOTTOM)
            }
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
        private fun stopRecording(status: Boolean) {
            stopRecording = status
        }

        //
        //----------------------------------------------------------------------------------------------
        //
        fun finish() {
            allDone = true
        }

        //
        //----------------------------------------------------------------------------------------------
        //
        //	@Override protected String doInBackground(XYSeries... params) 
        override fun doInBackground(vararg params: XYMultipleSeriesDataset?): String {
            val retStr = "AllOK"
            run {
                allDone = false
                nConnected = true
                val keys: IntArray = getKeysAsArray(nodeID2Ndx!!)

                naaradGetData(keys, params, true)
                while (!allDone) {
                    SystemClock.sleep(5000)
                    if (!allDone) {
                        naaradGetData(keys, params, false)
                    }
                }
            }

            return retStr
        }

        //
        //--------------------------------------------------------------------------
        //    
        fun addNewData(thisSeries: XYSeries, x: Double, y: Double) {
            if (y == MathHelper.NULL_VALUE) return

            if (yMax == 0.0 || y > yMax) yMax = y
            if (yMin == 100.0 || y < yMin) yMin = y

            thisSeries.add(x, y)

            val n = thisSeries.itemCount
            if (n > 1) {
                val xMaxVal = thisSeries.getX(n - 1)
                val xMinVal = thisSeries.getX(0)
                val dX = xMaxVal - xMinVal
                val dT = 1000.0 * 60 * 60 * 12 // 12 hours
                if (dX > dT) thisSeries.remove(0)
            }
        }

        protected fun reDraw() {
            if (doReDraw && mTimeChartView != null && !stopRecording) {
                activity?.runOnUiThread {
                    mTimeChartView?.zoomReset()
                    mRenderer.yAxisMax = if (yMax > yMin) yMax + 0.5 else yMax + 2.0
                    mRenderer.yAxisMin = if (yMax > yMin) yMin - 0.5 else yMin - 2.0
                    mTimeChartView?.repaint()
                }
            }
        }

        //
        //--------------------------------------------------------------------------
        //    
        //	@Override protected void onProgressUpdate(Integer... values) 
        override fun onProgressUpdate(vararg values: XYSeries?) {
            super.onProgressUpdate(*values)
            values[0]?.let {
                addNewData(it, x0, y0)
            }
            reDraw()


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


        override fun onCancelled() {
            finish()
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
            super.onCancelled()
        }
    } //============================================================================================================
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


    companion object {
        private var mView: View? = null

        //
        //-----------------------------------------------------------------------------------------
        //
        fun newInstanceNPF(sampleText: String?): NaaradPlotFragment {
            val f = NaaradPlotFragment()

            val b = Bundle()
            b.putString("bString", sampleText)
            f.setArguments(b)

            return f
        }
    }
}
