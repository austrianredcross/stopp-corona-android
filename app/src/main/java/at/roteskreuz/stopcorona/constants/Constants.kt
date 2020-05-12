package at.roteskreuz.stopcorona.constants

import at.roteskreuz.stopcorona.BuildConfig
import at.roteskreuz.stopcorona.BuildConfig.APPLICATION_ID
import at.roteskreuz.stopcorona.skeleton.core.constants.BaseAppRequest
import org.threeten.bp.Duration

/**
 * Constants for whole app scope.
 */
object Constants {

    /**
     * Constants useful for debugging.
     */
    object Debug {

        const val STRICT_MODE_ENABLED = false
    }

    /**
     * Constants for behavior tuning
     */
    object Behavior {

        /**
         * Minimum time between offline syncs in ms.
         */
        val OFFLINE_SYNC_INTERVAL: Duration = if (isDebug) {
            Duration.ofMinutes(5)
        } else {
            Duration.ofHours(1)
        }

        /**
         * Minimum delay between self retest notifications.
         */
        val SELF_RETEST_NOTIFICATION_INTERVAL: Duration = Duration.ofHours(6)
    }

    /**
     * Constants related to the domain.
     */
    object Domain {

        /**
         * Array of weights to calculate the risk of being in a proximity class for one minute.
         *
         * [PROXIMITY_SCORE_WEIGHT]\[0\] means PeerLost and must always have a weight of 0.0
         */
        val PROXIMITY_SCORE_WEIGHT = arrayOf(0.0, 0.0, 0.0, 1.0, 2.0, 15.0)

        /**
         * Interval (sliding window) of time to consider when detecting intensive contacts.
         */
        val INTENSIVE_CONTACT_DETECTION_INTERVAL: Duration = Duration.ofHours(1)

        /**
         * Score from which a contact is considered an intensive contact.
         */
        const val INTENSIVE_CONTACT_SCORE = 30.0
    }

    /**
     * Constants for shared preferences.
     */
    object Prefs {

        /**
         * Name of shared preferences file.
         */
        const val FILE_NAME = "prefs"
        private const val PREFIX = "pref_"

        const val ONBOARDING_REPOSITORY_PREFIX = PREFIX + "onboarding_repository_"
        const val INFECTION_MESSENGER_REPOSITORY_PREFIX = PREFIX + "infection_messenger_repository_"
        const val DATA_PRIVACY_REPOSITORY_PREFIX = PREFIX + "terms_and_conditions_repository_"
        const val DASHBOARD_PREFIX = PREFIX + "dashboard_"
        const val QUARANTINE_REPOSITORY_PREFIX = PREFIX + "quarantine_repository_"
        const val CORONA_DETECTION_REPOSITORY_PREFIX = PREFIX + "corona_detection_repository_"
        const val OFFLINE_SYNC_PREFIX = PREFIX + "offline_sync_"
        const val PREFERENCES_MIGRATION_MANAGER_PREFIX = PREFIX + "preferences_migration_manager_"
    }

    /**
     * Constants related to network interaction.
     */
    object API {

        const val HOSTNAME = BuildConfig.HOSTNAME
        const val BASE_URL = BuildConfig.BASE_URL

        const val HOSTNAME_TAN = BuildConfig.HOSTNAME_TAN
        const val BASE_URL_TAN = BuildConfig.BASE_URL_TAN

        val CERTIFICATE_CHAIN_DEFAULT: Array<String> = BuildConfig.CERTIFICATE_CHAIN
        val CERTIFICATE_CHAIN_TAN: Array<String> = BuildConfig.CERTIFICATE_CHAIN_TAN

        const val HTTP_CACHE_SIZE = 64L * 1024L * 1024L // 64 MB

        object Header {
            const val AUTHORIZATION_KEY = "AuthorizationKey"
            const val AUTHORIZATION_VALUE = BuildConfig.AUTHORIZATION_VALUE
            const val APP_ID_KEY = "X-AppId"
            const val APP_ID_VALUE = APPLICATION_ID
        }
    }

    /**
     * Constants related to P2Pkit.
     */
    object P2PDiscovery {

        const val APPLICATION_KEY = BuildConfig.P2P_APPLICATION_KEY
    }

    /**
     * Constants related to database.
     */
    object DB {

        const val FILE_NAME = "default.db"
    }

    /**
     * Request codes for fragment for result or any other identification which is required to be unique.
     * This ensures unique request codes for every screen, permission and whatever you need to be unique
     * across whole application.
     *
     * @see [BaseAppRequest].
     */
    object Request {

        private const val APP_BASE_REQUEST = BaseAppRequest.APP_BASE_REQUEST
        private const val OFFSET = BaseAppRequest.OFFSET

        const val REQUEST_DASHBOARD = APP_BASE_REQUEST + (1 shl OFFSET)
        const val AUTOMATIC_DETECTION_NOTIFICATION_ID = APP_BASE_REQUEST + (2 shl OFFSET)
    }

    /**
     * Constants related to security.
     */
    object Security {

        const val KEYSTORE = "AndroidKeyStore"
        const val KEYSTORE_ALIAS = "CoronaAppAlias"
        const val KEY_ALGORITHM = "RSA"
        const val FULL_ALGORITHM = "RSA/None/PKCS1Padding"
        const val KEY_SIZE = 1024
        const val BLOCK_SIZE = KEY_SIZE / 8 - 42
        const val X500_PRINCIPAL_NAME = "CN=$KEYSTORE_ALIAS, O=Android Authority"
        const val KEY_VALIDILITY_YEARS = 20
        const val CRYPTO_PROVIDER_CALLER_POSITION = 99
        const val FINGERPRINT_ALGORITHM = "SHA256"
        const val ADDRESS_PREFIX_LENGTH = 8
    }

    /**
     * Constants related to Google nearby.
     */
    object Nearby {

        /**
         * [RANDOM_IDENTIFICATION_MIN] defines the lower and [RANDOM_IDENTIFICATION_MAX] defines the
         * upper bound of the randomly generated four digit long random identification number
         * which is transmitted to all nearby devices
         */
        const val RANDOM_IDENTIFICATION_MIN = 1000
        const val RANDOM_IDENTIFICATION_MAX = 9999

        /**
         * Byte length of the transmitted identification number
         */
        const val IDENTIFICATION_BYTE_LENGTH = 4

        /**
         * Delay for checking found contacts and to show the loading indicator, if no contacts
         * where discovered in this interval
         */
        const val LOADING_INDICATOR_DELAY_MILLIS = 500L

        /**
         * Lookup public keys in the nearby record database of the last given minutes
         */
        const val PUBLIC_KEY_LOOKUP_THRESHOLD_MINUTES = 15L
    }

    /**
     * Ids of the notification channels the app is notifying through.
     */
    object NotificationChannels {

        const val CHANNEL_INFECTION_MESSAGE = "channel_infection_message"
        const val CHANNEL_SELF_RETEST = "channel_self_retest"
        const val CHANNEL_RECOVERED = "channel_recovered"
        const val CHANNEL_QUARANTINE = "channel_quarantine"
        const val CHANNEL_AUTOMATIC_DETECTION = "channel_automatic_detection"
    }

    /**
     * Country codes for questionnaires.
     */
    object Questionnaire {

        const val COUNTRY_CODE_CZ = "cz"
        const val COUNTRY_CODE_DE = "de"
        const val COUNTRY_CODE_EN = "en"
        const val COUNTRY_CODE_FR = "fr"
        const val COUNTRY_CODE_HU = "hu"
        const val COUNTRY_CODE_SK = "sk"
    }

    /**
     * Other constants without some relation.
     */
    object Misc {

        const val EMPTY_STRING = ""
        const val SPACE = " "
    }
}