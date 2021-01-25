package at.roteskreuz.stopcorona.di

import at.roteskreuz.stopcorona.model.entities.configuration.ConfigurationLanguage
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.screens.base.DebugViewModel
import at.roteskreuz.stopcorona.screens.base.dialog.datepicker.DatePickerFragmentDialogViewModel
import at.roteskreuz.stopcorona.screens.dashboard.DashboardViewModel
import at.roteskreuz.stopcorona.screens.dashboard.changelog.ChangelogViewModel
import at.roteskreuz.stopcorona.screens.debug.diagnosis_keys.DebugDiagnosisKeysViewModel
import at.roteskreuz.stopcorona.screens.debug.exposure_notifications.DebugExposureNotificationsViewModel
import at.roteskreuz.stopcorona.screens.debug.scheduling.SchedulingObserverViewModel
import at.roteskreuz.stopcorona.screens.infection_info.InfectionInfoViewModel
import at.roteskreuz.stopcorona.screens.onboarding.OnboardingViewModel
import at.roteskreuz.stopcorona.screens.dashboard.privacy_update.PrivacyUpdateViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.QuestionnaireViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.guideline.QuestionnaireGuidelineViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.hint.QuestionnaireHintViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.report.QuestionnaireReportViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring.QuestionnaireSelfMonitoringViewModel
import at.roteskreuz.stopcorona.screens.reporting.ReportingViewModel
import at.roteskreuz.stopcorona.screens.reporting.personalData.ReportingPersonalDataViewModel
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ReportingStatusViewModel
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.guideline.CertificateReportGuidelinesViewModel
import at.roteskreuz.stopcorona.screens.reporting.tanCheck.ReportingTanCheckViewModel
import at.roteskreuz.stopcorona.screens.routing.RouterViewModel
import at.roteskreuz.stopcorona.screens.savedIDs.InfoDeleteExposureKeysViewModel
import at.roteskreuz.stopcorona.screens.webView.WebViewViewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import org.threeten.bp.ZonedDateTime

/**
 * Module for providing viewModels.
 */
val viewModelModule = module {

    viewModel {
        DebugViewModel(
            appDispatchers = get(),
            diagnosisKeysRepository = get(),
            notificationsRepository = get(),
            quarantineRepository = get(),
            configurationRepository = get()
        )
    }

    viewModel {
        DebugExposureNotificationsViewModel(
            appDispatchers = get(),
            apiInteractor = get(),
            contextInteractor = get(),
            exposureNotificationRepository = get(),
            exposureNotificationClient = get()
        )
    }

    viewModel {
        DebugDiagnosisKeysViewModel(
            appDispatchers = get(),
            apiInteractor = get(),
            contextInteractor = get(),
            exposureNotificationRepository = get(),
            exposureNotificationClient = get(),
            diagnosisKeysRepository = get(),
            filesRepository = get(),
            configurationRepository = get()
        )
    }

    viewModel {
        SchedulingObserverViewModel(
            appDispatchers = get(),
            workManager = get()
        )
    }

    viewModel {
        DashboardViewModel(
            appDispatchers = get(),
            dashboardRepository = get(),
            diagnosisKeysRepository = get(),
            quarantineRepository = get(),
            changelogManager = get(),
            exposureNotificationManager = get(),
            dataPrivacyRepository = get()
        )
    }

    viewModel {
        InfectionInfoViewModel(
            appDispatchers = get(),
            quarantineRepository = get()
        )
    }

    viewModel {
        RouterViewModel(
            appDispatchers = get(),
            onboardingRepository = get()
        )
    }

    viewModel {
        OnboardingViewModel(
            appDispatchers = get(),
            onboardingRepository = get(),
            dataPrivacyRepository = get()
        )
    }

    viewModel { (configurationLanguage: ConfigurationLanguage) ->
        QuestionnaireViewModel(
            appDispatchers = get(),
            configurationLanguage = configurationLanguage,
            configurationRepository = get()
        )
    }

    viewModel {
        QuestionnaireGuidelineViewModel(
            appDispatchers = get(),
            quarantineRepository = get()
        )
    }

    /**
     * The fragment which implements this viewModel needs to call
     * ```
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     connectToScope(ReportingRepository.SCOPE_NAME)
     *     super.onCreate(savedInstanceState)
     * }
     * ```
     */
    viewModel {
        ReportingPersonalDataViewModel(
            appDispatchers = get(),
            reportingRepository = get()
        )
    }

    /**
     * The fragment which implements this viewModel needs to call
     * ```
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     connectToScope(ReportingRepository.SCOPE_NAME)
     *     super.onCreate(savedInstanceState)
     * }
     * ```
     */
    viewModel {
        ReportingTanCheckViewModel(
            appDispatchers = get(),
            reportingRepository = get()
        )
    }

    /**
     * The fragment which implements this viewModel needs to call
     * ```
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     connectToScope(ReportingRepository.SCOPE_NAME)
     *     super.onCreate(savedInstanceState)
     * }
     * ```
     */
    viewModel {
        ReportingStatusViewModel(
            appDispatchers = get(),
            reportingRepository = get(),
            quarantineRepository = get(),
            exposureNotificationManager = get(),
            exposureNotificationRepository = get()
        )
    }

    /**
     * The fragment which implements this viewModel needs to call
     * ```
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     connectToScope(ReportingRepository.SCOPE_NAME)
     *     super.onCreate(savedInstanceState)
     * }
     * ```
     */
    viewModel { (messageType: MessageType, dateWithMissingExposureKeys: ZonedDateTime?) ->
        ReportingViewModel(
            appDispatchers = get(),
            reportingRepository = get(),
            messageType = messageType,
            dateWithMissingExposureKeys = dateWithMissingExposureKeys
        )
    }

    viewModel {
        WebViewViewModel(
            appDispatchers = get()
        )
    }

    viewModel {
        CertificateReportGuidelinesViewModel(
            appDispatchers = get(),
            quarantineRepository = get()
        )
    }

    viewModel {
        QuestionnaireSelfMonitoringViewModel(
            appDispatchers = get(),
            quarantineRepository = get()
        )
    }

    viewModel {
        QuestionnaireHintViewModel(
            appDispatchers = get(),
            quarantineRepository = get()
        )
    }

    viewModel {
        ChangelogViewModel(
            appDispatchers = get(),
            changelogManager = get()
        )
    }

    viewModel {
        InfoDeleteExposureKeysViewModel(
            appDispatchers = get(),
            exposureNotificationRepository = get()
        )
    }

    /**
     * The fragment which implements this viewModel needs to call
     * ```
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     connectToScope(ReportingRepository.SCOPE_NAME)
     *     super.onCreate(savedInstanceState)
     * }
     * ```
     */
    viewModel {
        QuestionnaireReportViewModel(
            appDispatchers = get(),
            reportingRepository = get()
        )
    }

    /**
     * The fragment which implements this viewModel needs to call
     * ```
     * override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
     *     connectToScope(ReportingRepository.SCOPE_NAME)
     * }
     * ```
     */
    viewModel {
        DatePickerFragmentDialogViewModel(
            appDispatchers = get(),
            reportingRepository = get()
        )
    }

    viewModel {
        PrivacyUpdateViewModel(
            appDispatchers = get(),
            dataPrivacyRepository = get()
        )
    }
}
