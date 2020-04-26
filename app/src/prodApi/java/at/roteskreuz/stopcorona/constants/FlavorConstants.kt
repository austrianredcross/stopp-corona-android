package at.roteskreuz.stopcorona.constants

/**
 * Constants depending on the build flavor.
 */
object FlavorConstants {

    object API {

        const val HOSTNAME = "insert_hostname"
        const val BASE_URL = "https://$HOSTNAME/Rest/v5/"

        val CERTIFICATE_CHAIN = listOf(
            "sha256/insert_pub_key_hash1",
            "sha256/insert_pub_key_hash2",
            "sha256/insert_pub_key_hash3"
        )

        object Header {
            const val AUTHORIZATION_VALUE = "insert_header_authorization_value"
        }
    }

    object P2PDiscovery {

        const val APPLICATION_KEY = "insert_p2pkit_application_key"
    }
}
