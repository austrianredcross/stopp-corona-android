package at.roteskreuz.stopcorona.screens.statistics

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.statistics.Bundesland
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.statistics.legend.showStatisticsLegendFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.*
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_statistics.*
import kotlinx.android.synthetic.main.fragment_statistics.numberPicker
import org.koin.androidx.viewmodel.ext.android.viewModel

class StatisticsFragment : BaseFragment(R.layout.fragment_statistics) {

    override val isToolbarVisible: Boolean = true
    private val viewModel: StatisticsViewModel by viewModel()

    private val controller: StatisticsController by lazy {
        StatisticsController(
            requireContext(),
            onLegendClick = {
                showStatisticsLegendFragment()
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(contentRecyclerView) {
            setController(controller)
        }

        disposables += viewModel.observeSelectedState()
            .observeOnMainThread()
            .subscribe { selectedState ->
                viewModel.selectedState = selectedState

                when (selectedState) {
                    Bundesland.Oesterreich -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_all)
                    }
                    Bundesland.Wien -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_9)
                    }
                    Bundesland.Vorarlberg -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_8)
                    }
                    Bundesland.Tirol -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_7)
                    }
                    Bundesland.Steiermark -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_6)
                    }
                    Bundesland.Salzburg -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_5)
                    }
                    Bundesland.Oberoesterreich -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_4)
                    }
                    Bundesland.Niederoesterreich -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_3)
                    }
                    Bundesland.Kaernten -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_2)
                    }
                    Bundesland.Burgenland -> {
                        numberPicker.text = getString(R.string.covid_statistics_state_id_1)
                    }
                }

                txtTitle.text =
                    getString(R.string.covid_statistics_description, selectedState.value)

                updateStatisticCardInfo()
                updateStatisticIncidences()

                controller.statisticCardInfos = viewModel.statisticCardInfos
                controller.statisticIncidenceItems = viewModel.statisticIncidenceItems
                controller.selectedState = selectedState
            }
        disposables += viewModel.observeStatistics()
            .observeOnMainThread()
            .subscribe { statistics ->
                viewModel.statistics = statistics
                updateStatisticCardInfo()
                updateStatisticIncidences()

                controller.statistics = statistics
                controller.currentDate =
                    statistics.covidFaelleTimeline.lastTwoTimeLines(viewModel.selectedState)[1].time
                controller.compareDate =
                    statistics.covidFaelleTimeline.lastTwoTimeLines(viewModel.selectedState)[0].time
            }

        controller.requestModelBuild()

        numberPicker.setOnClickListener {
            StatePickerFragmentDialog().show(
                requireFragmentManager(),
                StatePickerFragmentDialog::class.java.name
            )
        }
    }

    private fun updateStatisticIncidences() {
        viewModel.statisticIncidenceItems.clear()

        if (viewModel.selectedState == Bundesland.Oesterreich) {
            Bundesland.values().forEach { state ->

                val lastTwoTimeLines =
                    viewModel.statistics?.covidFaelleTimeline?.filter { it.bundesland.value == state.value && it.bundesland != Bundesland.Oesterreich }
                        ?.sortedBy { it.time }?.takeLast(2)

                lastTwoTimeLines?.let {
                    if (lastTwoTimeLines.isNotEmpty()) {

                        // Calculate difference for seven days incidences
                        val siebenTageInzidenzFaelleDiff =
                            lastTwoTimeLines[1].siebenTageInzidenzFaelle - lastTwoTimeLines[0].siebenTageInzidenzFaelle

                        viewModel.statisticIncidenceItems.add(
                            StatisticIncidenceItem(
                                state.value,
                                lastTwoTimeLines[1].siebenTageInzidenzFaelle.roundTo(
                                    1
                                ).formatDecimal(),
                                siebenTageInzidenzFaelleDiff.roundTo(1).formatIncidenceValue(),
                                siebenTageInzidenzFaelleDiff.incidenceIcon(),
                                lastTwoTimeLines[1].siebenTageInzidenzFaelle.incidenceColorMark()
                            )
                        )
                    }
                }
            }
        } else {
            viewModel.statistics?.covidFaelleGKZ?.filter {
                it.state(childFragmentManager) == viewModel.selectedState
            }?.forEach { gkz ->
                viewModel.statisticIncidenceItems.add(
                    StatisticIncidenceItem(
                        gkz.bezirk,
                        gkz.incidenceValue().roundTo(1).formatDecimal(),
                        null,
                        null,
                        gkz.incidenceValue().roundTo(1).incidenceColorMark()
                    )
                )
            }
        }
    }


    private fun updateStatisticCardInfo() {
        viewModel.statistics?.covidFaelleTimeline?.lastTwoTimeLines(viewModel.selectedState)
            ?.let { lastTwoTimeLines ->
                viewModel.statistics?.covidFallzahlen?.lastTwoFallZahlen(viewModel.selectedState)
                    ?.let { lastTwoFallZahlen ->
                        viewModel.statisticCardInfos.clear()

                        // Calculate difference for confirmed cases and seven days incidences
                        val anzahlFaelleDiff =
                            lastTwoTimeLines[1].anzahlFaelle - lastTwoTimeLines[0].anzahlFaelle
                        val siebenTageInzidenzFaelleDiff =
                            lastTwoTimeLines[1].siebenTageInzidenzFaelle - lastTwoTimeLines[0].siebenTageInzidenzFaelle

                        viewModel.statisticCardInfos.add(
                            StatisticCardInfo(
                                getString(R.string.covid_statistics_confirmed_cases),
                                lastTwoTimeLines[1].anzahlFaelle.formatDecimal(),
                                anzahlFaelleDiff.formatIncidenceValue(),
                                anzahlFaelleDiff.toDouble().incidenceIcon(),
                                getString(R.string.covid_statistics_seven_day_incidence),
                                lastTwoTimeLines[1].siebenTageInzidenzFaelle.roundTo(
                                    1
                                ).formatDecimal(),
                                siebenTageInzidenzFaelleDiff.roundTo(1).formatIncidenceValue(),
                                siebenTageInzidenzFaelleDiff.incidenceIcon()
                            )
                        )

                        // Calculate difference for current hospitalized and current intensive
                        val fzHospDiff = lastTwoFallZahlen[1].fzHosp - lastTwoFallZahlen[0].fzHosp
                        val fzicuDiff = lastTwoFallZahlen[1].fzicu - lastTwoFallZahlen[0].fzicu

                        viewModel.statisticCardInfos.add(
                            StatisticCardInfo(
                                getString(R.string.covid_statistics_current_hospitalized),
                                lastTwoFallZahlen[1].fzHosp.formatDecimal(),
                                fzHospDiff.formatIncidenceValue(),
                                fzHospDiff.toDouble().incidenceIcon(),
                                getString(R.string.covid_statistics_current_intensive),
                                lastTwoFallZahlen[1].fzicu.formatDecimal(),
                                fzicuDiff.formatIncidenceValue(),
                                fzicuDiff.toDouble().incidenceIcon()
                            )
                        )

                        // Calculate difference for confirmed laboratory and all tests
                        val confirmedLaboratoryDiff =
                            lastTwoTimeLines[1].anzahlFaelleSum - lastTwoTimeLines[0].anzahlFaelleSum
                        val testGesamtDiff =
                            lastTwoFallZahlen[1].testGesamt - lastTwoFallZahlen[0].testGesamt

                        viewModel.statisticCardInfos.add(
                            StatisticCardInfo(
                                getString(R.string.covid_statistics_confirmed_laboratory),
                                lastTwoTimeLines[1].anzahlFaelleSum.formatDecimal(),
                                confirmedLaboratoryDiff.formatIncidenceValue(),
                                null,
                                getString(R.string.covid_statistics_accomplished_tests),
                                lastTwoFallZahlen[1].testGesamt.formatDecimal(),
                                testGesamtDiff.formatIncidenceValue(),
                                null
                            )
                        )

                        // Calculate difference for healed cases and all death cases
                        val anzahlGeheiltSumDiff =
                            lastTwoTimeLines[1].anzahlGeheiltSum - lastTwoTimeLines[0].anzahlGeheiltSum
                        val anzahlTotSumDiff =
                            lastTwoTimeLines[1].anzahlTotSum - lastTwoTimeLines[0].anzahlTotSum

                        viewModel.statisticCardInfos.add(
                            StatisticCardInfo(
                                getString(R.string.covid_statistics_healed_cases),
                                lastTwoTimeLines[1].anzahlGeheiltSum.formatDecimal(),
                                anzahlGeheiltSumDiff.formatIncidenceValue(),
                                null,
                                getString(R.string.covid_statistics_death_cases),
                                lastTwoTimeLines[1].anzahlTotSum.formatDecimal(),
                                anzahlTotSumDiff.formatIncidenceValue(),
                                null
                            )
                        )
                    }
            }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun getTitle(): String? {
        return getString(R.string.covid_statistics_title)
    }
}

fun Fragment.startStatisticsFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = StatisticsFragment::class.java.name
    )
}