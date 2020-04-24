package at.roteskreuz.stopcorona.screens.debug.events

import at.roteskreuz.stopcorona.model.db.dao.AutomaticDiscoveryDao
import at.roteskreuz.stopcorona.model.entities.discovery.DbAutomaticDiscoveryEvent
import at.roteskreuz.stopcorona.model.repositories.CryptoRepository
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.asDbObservable
import at.roteskreuz.stopcorona.utils.formatDayAndMonthAndYearAndTime
import io.reactivex.Observable

class DebugAutomaticEventsViewModel(
    appDispatchers: AppDispatchers,
    private val automaticDiscoveryDao: AutomaticDiscoveryDao,
    private val contextInteractor: ContextInteractor,
    private val cryptoRepository: CryptoRepository
) : ScopedViewModel(appDispatchers) {


    fun observe(): Observable<String> {
        return automaticDiscoveryDao.observeAllEvents().asDbObservable()
            .map {
                it.sortedByDescending { it.startTime }
                    .joinToString("\n") { it.asPrintable() }
            }
    }

    private fun DbAutomaticDiscoveryEvent.asPrintable(): String {
        return "${cryptoRepository.getPublicKeyPrefix(publicKey)}\n" +
            "startTime = ${startTime.formatDayAndMonthAndYearAndTime(contextInteractor.applicationContext)}\n" +
            "endTime = ${endTime?.formatDayAndMonthAndYearAndTime(contextInteractor.applicationContext)}\n" +
            "proximity = $proximity\n"
    }
}