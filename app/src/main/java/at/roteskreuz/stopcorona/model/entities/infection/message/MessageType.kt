package at.roteskreuz.stopcorona.model.entities.infection.message

import android.os.Parcelable
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import timber.log.Timber

/**
 * Message types informing about different states.
 */
sealed class MessageType(val identifier: String) : Parcelable {

    companion object {
        operator fun invoke(identifier: String): MessageType? {
            return when (identifier) {
                InfectionLevel.Red.identifier -> InfectionLevel.Red
                InfectionLevel.Yellow.identifier -> InfectionLevel.Yellow
                Revoke.Suspicion.identifier -> Revoke.Suspicion
                else -> {
                    Timber.e(SilentError("Unknown message type identifier $identifier"))
                    null
                }
            }
        }
    }

    abstract val warningType: WarningType

    /**
     * Levels of infections by source of diagnose.
     */
    sealed class InfectionLevel(identifier: String) : MessageType(identifier) {

        /**
         * Diagnosed by doctor.
         */
        @Parcelize
        object Red : InfectionLevel("r") {

            @IgnoredOnParcel
            override val warningType: WarningType = WarningType.RED
        }

        /**
         * Diagnosed by user.
         */
        @Parcelize
        object Yellow : InfectionLevel("y") {

            @IgnoredOnParcel
            override val warningType: WarningType = WarningType.YELLOW
        }
    }

    /**
     * Revoke-from options.
     */
    sealed class Revoke(identifier: String) : MessageType(identifier) {

        /**
         * User is recovered as result of self test.
         */
        @Parcelize
        object Suspicion : MessageType("g") {

            @IgnoredOnParcel
            override val warningType: WarningType = WarningType.GREEN
        }

        /**
         * Placeholder to initialize a sickness revoke process.
         * Will be replaced by [Suspicion] or [InfectionLevel.Yellow] during the process.
         */
        @Parcelize
        object Sickness : MessageType("g") {

            @IgnoredOnParcel
            override val warningType: WarningType = WarningType.GREEN
        }
    }
}