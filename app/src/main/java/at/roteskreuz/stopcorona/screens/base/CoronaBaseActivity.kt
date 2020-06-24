package at.roteskreuz.stopcorona.screens.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.LayoutRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.isDebug
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.screens.debug.diagnosis_keys.startDebugDiagnosisKeysFragment
import at.roteskreuz.stopcorona.screens.debug.exposure_notifications.startDebugExposureNotificationsFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.BaseActivity
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * Base activity specific for Corona project.
 */
open class CoronaBaseActivity(@LayoutRes layout: Int = R.layout.framelayout) : BaseActivity(layout) {

    private lateinit var debugViewModel: DebugViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // instantiated only on debug build
        if (isDebug) {
            debugViewModel = getViewModel()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (isDebug) {
            menuInflater.inflate(R.menu.debug, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.debugMenuNotificationInfectionRed -> {
                debugViewModel.displayInfectionNotification(MessageType.InfectionLevel.Red)
                true
            }
            R.id.debugMenuNotificationInfectionYellow -> {
                debugViewModel.displayInfectionNotification(MessageType.InfectionLevel.Yellow)
                true
            }
            R.id.debugMenuNotificationSelfRetest -> {
                debugViewModel.displaySelfRetestNotification()
                true
            }
            R.id.debugMenuNotificationSomeoneRecovered -> {
                debugViewModel.displaySomeoneHasRecoveredNotification()
                true
            }
            R.id.debugMenuNotificationEndQuarantine -> {
                debugViewModel.displayEndQuarantineNotification()
                true
            }
            R.id.debugMenuQuarantineStatus -> {
                val quarantineStatus = debugViewModel.getQuarantineStatus()
                Toast.makeText(this, quarantineStatus.toString(), Toast.LENGTH_LONG).show()
                true
            }
            R.id.debugMenuReportMedicalConfirmation -> {
                debugViewModel.reportMedicalConfirmation()
                true
            }
            R.id.debugMenuReportPositiveSelfDiagnose -> {
                debugViewModel.reportPositiveSelfDiagnose()
                true
            }
            R.id.debugExposureNotifications -> {
                startDebugExposureNotificationsFragment()
                true
            }
            R.id.debugDiagnosisKeys -> {
                startDebugDiagnosisKeysFragment()
                true
            }
            R.id.debugZeroDaysOfRedQuarantine -> {
                debugViewModel.quarantineRedForZeroDays()
                true
            }
            R.id.debugZeroDaysOfYellowQuarantine -> {
                debugViewModel.quarantineYellowForZeroDays()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}