package at.roteskreuz.stopcorona.model.repositories

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.NotificationChannels
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.screens.dashboard.getDashboardActivityIntent
import at.roteskreuz.stopcorona.screens.infection_info.getInfectionInfoFragmentIntent
import at.roteskreuz.stopcorona.screens.questionnaire.getQuestionnaireIntent
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.utils.string
import kotlinx.coroutines.CoroutineScope
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * Repository that manages the notifications.
 */
interface NotificationsRepository {

    /**
     * Display notification about received infection by [infectionLevel].
     */
    fun displayInfectionNotification(infectionLevel: MessageType.InfectionLevel)

    /**
     * Display notification to remind a self test.
     */
    fun displaySelfRetestNotification()

    /**
     * Display notification that someone has recovered.
     */
    fun displaySomeoneHasRecoveredNotification()

    /**
     * Display notification when quarantine has ended.
     * It can happen in two cases:
     * - User has been quarantined for specific amount of time without symptoms.
     * - All contacts has been recovered and user don't have any symptoms.
     */
    fun displayEndQuarantineNotification()

    /**
     * Get uncancelable notification that automatic bluetooth devices detection is in progress.
     */
    fun getCoronaAutomaticDetectionNotification(): Notification

    /**
     * Display uncancelable notification that automatic bluetooth devices detection is in progress.
     */
    fun updateAndDisplayCoronaAutomaticDetectionNotification(id: Int)

    /**
     * Display notification when bluetooth is off.
     */
    fun displayPleaseActivateBluetoothNotification()

    /**
     * Hide notification by [id].
     */
    fun hideNotification(id: Int)

    /**
     * Display a notification, informing the user that the combined risk of the exposures from
     * the Exposure Notifications Framework was too low to qualify for a quarantine.
     */
    fun displayNotificationForLowRisc()
}

class NotificationsRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val contextInteractor: ContextInteractor,
    private val dataPrivacyRepository: DataPrivacyRepository,
    private val exposureNotificationRepository: ExposureNotificationRepository
) : NotificationsRepository,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    private val context: Context
        get() = contextInteractor.applicationContext

    private val notificationManager: NotificationManager
        get() = contextInteractor.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Flags to have clear task.
     * It must be added to the first activity intent.
     */
    private val firstActivityFlags = Intent.FLAG_ACTIVITY_SINGLE_TOP or // call onNewIntent() on running activity
        Intent.FLAG_ACTIVITY_NEW_TASK or // can open activity from service
        Intent.FLAG_ACTIVITY_CLEAR_TOP // remove stack and place this activity on top (will continue)

    override fun displayInfectionNotification(infectionLevel: MessageType.InfectionLevel) {
        val (title, message) = when (infectionLevel.warningType) {
            WarningType.YELLOW -> {
                context.string(R.string.local_notification_suspected_sick_contact_headline) to
                    context.string(R.string.local_notification_suspected_sick_contact_message)
            }
            WarningType.RED -> {
                context.string(R.string.local_notification_sick_contact_headline) to
                    context.string(R.string.local_notification_sick_contact_message)
            }
            WarningType.GREEN -> {
                context.string(R.string.local_notification_someone_has_recovered_headline) to
                    context.string(R.string.local_notification_quarantine_end_message)
            }
        }

        buildNotification(
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_MAX,
            pendingIntent = buildPendingIntentWithActivityStack {
                addNextIntent(context.getDashboardActivityIntent().addFlags(firstActivityFlags))
                addNextIntent(context.getInfectionInfoFragmentIntent())
            },
            channelId = NotificationChannels.CHANNEL_INFECTION_MESSAGE
        ).show()
    }

    override fun displaySelfRetestNotification() {
        val title = context.string(R.string.local_notification_self_retest_headline)

        buildNotification(
            title = title,
            pendingIntent = buildPendingIntentWithActivityStack {
                addNextIntent(context.getDashboardActivityIntent().addFlags(firstActivityFlags))
                addNextIntent(context.getQuestionnaireIntent())
            },
            channelId = NotificationChannels.CHANNEL_SELF_RETEST
        ).show()
    }

    override fun displaySomeoneHasRecoveredNotification() {
        val title = context.string(R.string.local_notification_someone_has_recovered_headline)

        buildNotification(
            title = title,
            pendingIntent = buildPendingIntentWithActivityStack {
                addNextIntent(context.getDashboardActivityIntent().addFlags(firstActivityFlags))
            },
            channelId = NotificationChannels.CHANNEL_RECOVERED
        ).show()
    }

    override fun displayEndQuarantineNotification() {
        val title = context.string(R.string.local_notification_quarantine_end_headline)
        val message = context.string(R.string.local_notification_quarantine_end_message)

        buildNotification(
            title = title,
            message = message,
            pendingIntent = buildPendingIntentWithActivityStack {
                addNextIntent(context.getDashboardActivityIntent().addFlags(firstActivityFlags))
            },
            channelId = NotificationChannels.CHANNEL_QUARANTINE
        ).show()
    }

    override fun getCoronaAutomaticDetectionNotification(): Notification {
        val title = context.string(R.string.local_notification_automatic_handshake_title)
        val message = context.string(R.string.local_notification_automatic_handshake_message)

        return buildNotification(
            title = title,
            message = message,
            pendingIntent = buildPendingIntentWithActivityStack {
                addNextIntent(context.getDashboardActivityIntent().addFlags(firstActivityFlags))
            },
            channelId = NotificationChannels.CHANNEL_AUTOMATIC_DETECTION,
            ongoing = true
        )
    }

    override fun updateAndDisplayCoronaAutomaticDetectionNotification(id: Int) {
        val notification = getCoronaAutomaticDetectionNotification()

        notificationManager.notify(id, notification)
    }

    override fun displayPleaseActivateBluetoothNotification() {
        val title = context.string(R.string.local_notification_bluetooth_is_off_title)
        val message = context.string(R.string.local_notification_bluetooth_is_off_message)

        buildNotification(
            title = title,
            message = message,
            pendingIntent = buildPendingIntentWithActivityStack {
                addNextIntent(context.getDashboardActivityIntent().addFlags(firstActivityFlags))
            },
            channelId = NotificationChannels.CHANNEL_AUTOMATIC_DETECTION
        ).show()
    }

    override fun hideNotification(id: Int) {
        notificationManager.cancel(id)
    }

    override fun displayNotificationForLowRisc() {
        val title = context.string(R.string.local_notification_low_risk_title)
        val message = context.string(R.string.local_notification_low_risk_message)

        buildNotification(
            title = title,
            message = message,
            pendingIntent = buildPendingIntentWithActivityStack {
                addNextIntent(context.getDashboardActivityIntent().addFlags(firstActivityFlags))
            },
            channelId = NotificationChannels.CHANNEL_INFECTION_MESSAGE,
            ongoing = true
        ).show()
    }

    private fun buildNotification(
        title: String,
        pendingIntent: PendingIntent,
        channelId: String,
        message: String? = null,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        ongoing: Boolean = false
    ): Notification {
        dataPrivacyRepository.assertDataPrivacyAccepted()

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_red_cross)
            .setOngoing(ongoing)
            .let {
                if (message != null) {
                    it.setContentText(message)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                } else it
            }
            .build()
    }

    private fun buildPendingIntentWithActivityStack(activityStackBuilder: TaskStackBuilder.() -> Unit): PendingIntent {
        val requestCode = UUID.randomUUID().mostSignificantBits.toInt() // should be unique

        return TaskStackBuilder.create(context).apply {
            activityStackBuilder()
        }.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT)!! // !! because no FLAG_NO_CREATE
    }

    private fun Notification.show() {
        notificationManager.notify(hashCode(), this)
    }
}