package naarad.client.tabhost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sanbee.anaarad.R

class MySampleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mView = inflater.inflate(
            R.layout.activity_my_sample_fragment,
            container, false
        )
        val sampleText: String? = arguments?.getString("bString")

        val txtSampleText = mView!!.findViewById<View>(R.id.txtViewSample) as TextView
        txtSampleText.text = sampleText

        return mView!!
    }

    companion object {
        private var mView: View? = null

        fun newInstance(sampleText: String?): MySampleFragment {
            val f = MySampleFragment()
            val b = Bundle()
            b.putString("bString", sampleText)
            f.arguments = b
            return f
        }
    }
}
