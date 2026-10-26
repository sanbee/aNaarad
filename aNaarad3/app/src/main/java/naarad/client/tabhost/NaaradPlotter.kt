package naarad.client.tabhost

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.sanbee.anaarad.R
import org.achartengine.ChartFactory
import org.achartengine.GraphicalView
import org.achartengine.chart.PointStyle
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.model.XYSeries
import org.achartengine.renderer.XYMultipleSeriesRenderer
import org.achartengine.renderer.XYSeriesRenderer

class NaaradPlotter(metrics: DisplayMetrics?) {
    var mDataset: XYMultipleSeriesDataset = XYMultipleSeriesDataset()
    var mRenderer: XYMultipleSeriesRenderer = XYMultipleSeriesRenderer()
    var mTimeChartView: GraphicalView? = null
    var doReDraw: Boolean = false
    var stopRecording: Boolean = false
    protected var xMax: Double = 0.0
    protected var xMin: Double = 0.0
    protected var yMax: Double = 0.0
    protected var yMin: Double = 100.0
    private val myMetrics: DisplayMetrics? = metrics

    fun makeSeries(color: Int) {
        val seriesTitle = "Series " + (mDataset.getSeriesCount() + 1)
        val series0 = XYSeries(seriesTitle)
        mDataset.addSeries(series0)
        val renderer0 = XYSeriesRenderer()
        renderer0.setPointStyle(PointStyle.CIRCLE)
        renderer0.setFillPoints(true)
        renderer0.setLineWidth(2f)
        renderer0.setColor(color)
        mRenderer.addSeriesRenderer(renderer0)
    }

    fun initMultiRenderer() {
        mRenderer.setApplyBackgroundColor(true)
        mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50))
        mRenderer.setAxisTitleTextSize(10f)
        mRenderer.setChartTitleTextSize(10f)
        mRenderer.setLabelsTextSize(10f)
        mRenderer.setLegendTextSize(10f)
        mRenderer.setMargins(intArrayOf(2, 20, 5, 2)) //Top, Left, Bottom, Right
        mRenderer.setZoomButtonsVisible(false)
        mRenderer.setPointSize(2f)
        mRenderer.setXTitle("")
        mRenderer.setYTitle("Temperature")
        mRenderer.setShowGrid(true)
        mRenderer.setShowLegend(false)
        mRenderer.setXLabels(10) // No. of xtics
        mRenderer.setClickEnabled(false)
        mRenderer.setPanEnabled(false, false)
        var value = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            12f,
            myMetrics
        )
        mRenderer.setLabelsTextSize(value)
        value = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            8f,
            myMetrics
        )
        mRenderer.setTextTypeface("sans_serif", Typeface.BOLD)
    }

    fun makeChart(currentActivity: Activity?, currentView: View) {
        System.err.println("#### makeChart")
        mTimeChartView = ChartFactory.getTimeChartView(
            currentActivity,
            mDataset,
            mRenderer,
            "hh:mm:ss\ndd/MM"
        )

        val layout = currentView.findViewById<View?>(R.id.chart) as LinearLayout
        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layout.addView(mTimeChartView, 0, lp)
    }

    fun reDraw() {
        if (doReDraw && (mTimeChartView != null) && (!stopRecording)) {
            mTimeChartView?.zoomReset()
            mRenderer.setYAxisMax(yMax + 0.2)
            mRenderer.setYAxisMin(yMin - 0.2)
            mTimeChartView?.repaint()
        }
    }
}
