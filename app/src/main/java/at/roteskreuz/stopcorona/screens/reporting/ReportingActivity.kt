package at.roteskreuz.stopcorona.screens.reporting

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.model.repositories.ReportingState
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.reporting.personalData.ReportingPersonalDataFragment
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ReportingStatusFragment
import at.roteskreuz.stopcorona.screens.reporting.tanCheck.ReportingTanCheckFragment
import at.roteskreuz.stopcorona.skeleton.core.model.scope.connectToScope
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.argument
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber

/**
 * Certificate reporting container activity to hold and switch the content by the reporting state.
 */
class ReportingActivity : CoronaPortraitBaseActivity() {

    companion object {
        private const val ARGUMENT_MESSAGE_TYPE = "argument_message_type"
        private const val ARGUMENT_DATE_WITH_MISSING_EXPOSURE_KEYS_AS_STRING =
            "argument_upload_missing_exposure_keys"

        fun args(messageType: MessageType, dateWithMissingExposureKeys: ZonedDateTime?): Bundle {
            return bundleOf(
                ARGUMENT_MESSAGE_TYPE to messageType,
                ARGUMENT_DATE_WITH_MISSING_EXPOSURE_KEYS_AS_STRING to dateWithMissingExposureKeys?.let {
                    DateTimeFormatter.ISO_ZONED_DATE_TIME.format(it)
                }
            )
        }
    }

    private val messageType: MessageType by argument(ARGUMENT_MESSAGE_TYPE)

    private val dateWithMissingExposureKeysAsString: String? by argument(
        ARGUMENT_DATE_WITH_MISSING_EXPOSURE_KEYS_AS_STRING
    )

    private val viewModel: ReportingViewModel by viewModel {
        parametersOf(
            messageType,
            dateWithMissingExposureKeysAsString?.let {
                ZonedDateTime.parse(it)
            }
        )
    }

    /**
     * Contains disposables that have to be disposed in [onStop].
     */
    private var disposablesWhileViewIsVisible: Disposable? = null

    override val navigateUpOnBackPress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        /**
         * When savedInstanceState is set, system probably kicked of the activity from memory, which
         * causes reset of ReportRepository and this activity has been restored.
         * To not confuse user by reset of flow progress, we just destroy it completely
         * and return him to the previous screen.
         * Reporting process must be started again as an user explicit action then.
         */
        if (savedInstanceState != null) {
            Timber.e(SilentError("Certificate reporting process is destroyed due to lost state in ReportingActivity."))
            finish()
        }

        connectToScope(ReportingRepository.SCOPE_NAME)
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        disposablesWhileViewIsVisible = viewModel.observeReportingState()
            .subscribe { reportSendingState ->
                when (reportSendingState) {
                    is ReportingState.PersonalDataEntry -> {
                        if (currentFragment !is ReportingPersonalDataFragment) {
                            replaceFragment(ReportingPersonalDataFragment(), addToBackStack = false)
                        }
                    }
                    is ReportingState.TanEntry -> {
                        if (currentFragment !is ReportingTanCheckFragment) {
                            replaceFragment(ReportingTanCheckFragment(), addToBackStack = false)
                        }
                    }
                    is ReportingState.ReportingAgreement -> {
                        if (currentFragment !is ReportingStatusFragment) {
                            replaceFragment(ReportingStatusFragment(), addToBackStack = false)
                        }
                    }
                }
            }.also { disposables += it }
    }

    override fun onStop() {
        disposablesWhileViewIsVisible?.dispose()
        disposablesWhileViewIsVisible = null
        super.onStop()
    }
}

fun Fragment.startReportingActivity(
    messageType: MessageType,
    dateWithMissingExposureKeys: ZonedDateTime? = null
) {
    startFragmentActivity<ReportingActivity>(
        fragmentName = ReportingPersonalDataFragment::class.java.name,
        activityBundle = ReportingActivity.args(messageType, dateWithMissingExposureKeys)
    )
}
