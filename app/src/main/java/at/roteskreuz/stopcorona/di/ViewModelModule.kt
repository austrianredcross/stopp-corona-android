package at.roteskreuz.stopcorona.di

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.screens.base.DebugViewModel
import at.roteskreuz.stopcorona.screens.dashboard.DashboardViewModel
import at.roteskreuz.stopcorona.screens.dashboard.changelog.ChangelogViewModel
import at.roteskreuz.stopcorona.screens.debug.exposure_notifications.DebugExposureNotificationsViewModel
import at.roteskreuz.stopcorona.screens.infection_info.InfectionInfoViewModel
import at.roteskreuz.stopcorona.screens.onboarding.OnboardingViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.QuestionnaireViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.guideline.QuestionnaireGuidelineViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.hint.QuestionnaireHintViewModel
import at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring.QuestionnaireSelfMonitoringViewModel
import at.roteskreuz.stopcorona.screens.reporting.ReportingViewModel
import at.roteskreuz.stopcorona.screens.reporting.personalData.ReportingPersonalDataViewModel
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ReportingStatusViewModel
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.guideline.CertificateReportGuidelinesViewModel
import at.roteskreuz.stopcorona.screens.reporting.tanCheck.ReportingTanCheckViewModel
import at.roteskreuz.stopcorona.screens.routing.RouterViewModel
import at.roteskreuz.stopcorona.screens.webView.WebViewViewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

/**
 * Module for providing viewModels.
 */
val viewModelModule = module {

    viewModel {
        DebugViewModel(
            appDispatchers = get(),
            notificationsRepository = get(),
            quarantineRepository = get(),
            infectionMessageDao = get()
        )
    }

    viewModel {
        DebugExposureNotificationsViewModel(
            appDispatchers = get(),
            apiInteractor = get(),
            contextInteractor = get(),
            exposureNotificationRepository = get()
        )
    }

    viewModel {
        DashboardViewModel(
            appDispatchers = get(),
            dashboardRepository = get(),
            contextInteractor = get(),
            quarantineRepository = get(),
            configurationRepository = get(),
            exposureNotificationRepository = get(),
            databaseCleanupManager = get(),
            googlePlayAvailability = get(),
            changelogManager = get()
        )
    }

    viewModel {
        InfectionInfoViewModel(
            appDispatchers = get()
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

    viewModel {
        QuestionnaireViewModel(
            appDispatchers = get(),
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
    viewModel { (messageType: MessageType) ->
        ReportingViewModel(
            appDispatchers = get(),
            reportingRepository = get(),
            messageType = messageType
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
}
