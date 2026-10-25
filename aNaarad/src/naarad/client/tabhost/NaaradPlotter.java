package naarad.client.tabhost;

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
import android.graphics.Typeface;
import android.util.TypedValue;
import android.util.DisplayMetrics;
import android.content.Context;

import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.app.Activity;

public class NaaradPlotter
{

    public XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    public XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    public GraphicalView mTimeChartView;
    public Boolean doReDraw=false,stopRecording=false;
    protected double xMax, xMin, yMax, yMin=100.0;
    private DisplayMetrics myMetrics;

    public NaaradPlotter(DisplayMetrics metrics)
    {
	myMetrics=metrics;
    }
    
    // private void makeSeries(XYMultipleSeriesRenderer multiRenderer,
    // 			   XYMultipleSeriesDataset multiDataset,
    public void makeSeries(int color)
    {
	String seriesTitle = "Series " + (mDataset.getSeriesCount() + 1);
	XYSeries series0 = new XYSeries(seriesTitle);
	mDataset.addSeries(series0);
	XYSeriesRenderer renderer0 = new XYSeriesRenderer();
	renderer0.setPointStyle(PointStyle.CIRCLE);
	renderer0.setFillPoints(true);
	renderer0.setLineWidth(2f);
	//renderer0.setColor(Color.GREEN);
	renderer0.setColor(color);
	mRenderer.addSeriesRenderer(renderer0);
    }

    public void initMultiRenderer()
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
	//DisplayMetrics metrics = myContext.getResources().getDisplayMetrics();
	float val = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, myMetrics);
	mRenderer.setLabelsTextSize(val);
	val = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8, myMetrics);
	mRenderer.setTextTypeface("sans_serif",Typeface.BOLD);
    }

    public void makeChart(Activity currentActivity, View currentView)
    {
	System.err.println("#### makeChart");
	// mDataset.getSeriesAt(0).add(0,new Date().getTime());
	// mDataset.getSeriesAt(0).add(1,new Date().getTime()+30000.0);
	mTimeChartView = ChartFactory.getTimeChartView(currentActivity, mDataset, mRenderer,"hh:mm:ss\ndd/MM");	// "%tT"
	
	LinearLayout layout = (LinearLayout) currentView.findViewById(R.id.chart);
	LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
	layout.addView(mTimeChartView,0, lp);

	//nConnected=true;
    }

    public void reDraw()
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

}