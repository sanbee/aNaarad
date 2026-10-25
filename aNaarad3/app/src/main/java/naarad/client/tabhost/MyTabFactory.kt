package naarad.client.tabhost

import android.content.Context
import android.view.View
import android.widget.TabHost.TabContentFactory

class MyTabFactory(private val mContext: Context?) : TabContentFactory {
    override fun createTabContent(tag: String?): View {
        val v = View(mContext)
        v.setMinimumWidth(0)
        v.setMinimumHeight(0)
        return v
    }
}