package at.roteskreuz.stopcorona.screens.statistics.legend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.dialog.FullWidthDialog
import kotlinx.android.synthetic.main.fragment_statistics_legend.*

class StatisticsLegendFragment : FullWidthDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_statistics_legend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose.setOnClickListener {
            dismiss()
        }

        // This is necessary because to escape the < should be &lt; and babelish escapes the & symbol to amp
        underOneHundredTxt.text = getString(R.string.covid_statistics_legend_under_one_hundred, "<")
    }
}
fun Fragment.showStatisticsLegendFragment() {
    StatisticsLegendFragment().show(requireFragmentManager(), StatisticsLegendFragment::class.java.name)
}