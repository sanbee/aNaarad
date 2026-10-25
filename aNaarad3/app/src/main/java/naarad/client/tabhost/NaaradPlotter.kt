package naarad.client.tabhost

import org.achartengine.ChartFactory

class NaaradPlotter
    (metrics: DisplayMetrics?) {
    var mDataset: XYMultipleSeriesDataset = XYMultipleSeriesDataset()
    var mRenderer: XYMultipleSeriesRenderer = XYMultipleSeriesRenderer()
    var mTimeChartView: GraphicalView? = null
    var doReDraw: kotlin.Boolean = false
    var stopRecording: kotlin.Boolean = false
    protected var xMax: kotlin.Double = 0.0
    protected var xMin: kotlin.Double = 0.0
    protected var yMax: kotlin.Double = 0.0
    protected var yMin: kotlin.Double = 100.0
    private val myMetrics: DisplayMetrics?

    init {
        myMetrics = metrics
    }

    // private void makeSeries(XYMultipleSeriesRenderer multiRenderer,
    // 			   XYMultipleSeriesDataset multiDataset,
    fun makeSeries(color: kotlin.Int) {
        val seriesTitle = "Series " + (mDataset.getSeriesCount() + 1)
        val series0: XYSeries = XYSeries(seriesTitle)
        mDataset.addSeries(series0)
        val renderer0: XYSeriesRenderer = XYSeriesRenderer()
        renderer0.setPointStyle(PointStyle.CIRCLE)
        renderer0.setFillPoints(true)
        renderer0.setLineWidth(2f)
        //renderer0.setColor(Color.GREEN);
        renderer0.setColor(color)
        mRenderer.addSeriesRenderer(renderer0)
    }

    fun initMultiRenderer() {
        mRenderer.setApplyBackgroundColor(true)
        mRenderer.setBackgroundColor(android.graphics.Color.argb(100, 50, 50, 50))
        mRenderer.setAxisTitleTextSize(10)
        mRenderer.setChartTitleTextSize(10)
        mRenderer.setLabelsTextSize(10)
        mRenderer.setLegendTextSize(10)
        mRenderer.setMargins(kotlin.intArrayOf(2, 20, 5, 2)) //Top, Left, Bottom, Right
        mRenderer.setZoomButtonsVisible(false)
        mRenderer.setPointSize(2)
        mRenderer.setXTitle("")
        mRenderer.setYTitle("Temperature")
        mRenderer.setShowGrid(true)
        mRenderer.setShowLegend(false)
        //mRenderer.setShowLegend(true);
        mRenderer.setXLabels(10) // No. of xtics
        mRenderer.setClickEnabled(false)
        mRenderer.setPanEnabled(false)
        //multiRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        //DisplayMetrics metrics = myContext.getResources().getDisplayMetrics();
        var `val` = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            12f,
            myMetrics
        )
        mRenderer.setLabelsTextSize(`val`)
        `val` = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            8f,
            myMetrics
        )
        mRenderer.setTextTypeface("sans_serif", android.graphics.Typeface.BOLD)
    }

    fun makeChart(currentActivity: android.app.Activity?, currentView: android.view.View) {
        java.lang.System.err.println("#### makeChart")
        // mDataset.getSeriesAt(0).add(0,new Date().getTime());
        // mDataset.getSeriesAt(0).add(1,new Date().getTime()+30000.0);
        mTimeChartView = ChartFactory.getTimeChartView(
            currentActivity,
            mDataset,
            mRenderer,
            "hh:mm:ss\ndd/MM"
        ) // "%tT"

        val layout: LinearLayout =
            currentView.findViewById<android.view.View?>(R.id.chart) as LinearLayout
        val lp = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.FILL_PARENT,
            android.view.ViewGroup.LayoutParams.FILL_PARENT
        )
        layout.addView(mTimeChartView, 0, lp)

        //nConnected=true;
    }

    fun reDraw() {
        if (doReDraw) if ((mTimeChartView != null) && (stopRecording == false)) {
            mTimeChartView.zoomReset()
            mRenderer.setYAxisMax(yMax + 0.2)
            mRenderer.setYAxisMin(yMin - 0.2)
            // mRenderer.setYAxisMax(40.0);
            // mRenderer.setYAxisMin(-10.0);
            mTimeChartView.repaint()
        }
    }
}