package at.roteskreuz.stopcorona.model.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import at.roteskreuz.stopcorona.model.db.dao.ConfigurationDao
import at.roteskreuz.stopcorona.model.db.dao.InfectionMessageDao
import at.roteskreuz.stopcorona.model.db.dao.TemporaryExposureKeysDao
import at.roteskreuz.stopcorona.model.entities.configuration.*
import at.roteskreuz.stopcorona.model.entities.exposure.DbSentTemporaryExposureKeys
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningTypeConverter
import at.roteskreuz.stopcorona.model.entities.infection.message.DbReceivedInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageTypeConverter
import at.roteskreuz.stopcorona.model.entities.infection.message.UUIDConverter
import at.roteskreuz.stopcorona.skeleton.core.model.db.converters.DateTimeConverter

/**
 * Room database description with DAOs specification.
 */
@Database(
    entities = [
        DbConfiguration::class,
        DbQuestionnaire::class,
        DbQuestionnaireAnswer::class,
        DbPageContent::class,
        DbReceivedInfectionMessage::class,
        DbSentTemporaryExposureKeys::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(
    DateTimeConverter::class,
    ConfigurationLanguageConverter::class,
    MessageTypeConverter::class,
    WarningTypeConverter::class,
    UUIDConverter::class,
    DecisionConverter::class,
    ArrayOfIntegerConverter::class
)
abstract class DefaultDatabase : RoomDatabase() {

    companion object {
        val migrations = arrayOf(
            // app v1.0.0 with version DB 6
            /**
             * Added fields to the table [DbConfiguration].
             */
            migration(6, 7) {
                execSQL("DELETE FROM `configuration`") // clear DB
                execSQL("ALTER TABLE `configuration` ADD COLUMN `redWarningQuarantine` INTEGER")
                execSQL("ALTER TABLE `configuration` ADD COLUMN `yellowWarningQuarantine` INTEGER")
                execSQL("ALTER TABLE `configuration` ADD COLUMN `selfDiagnosedQuarantine` INTEGER")
            },
            /**
             * Added new debug table.
             * Added field to the [DbNearbyRecord].
             */
            migration(7, 8) {
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `debug_playground_entity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timeStamp` INTEGER NOT NULL, `publicKey` TEXT NOT NULL, `proximity` INTEGER NOT NULL)"
                )
                execSQL("CREATE INDEX IF NOT EXISTS `index_debug_playground_entity_publicKey` ON `debug_playground_entity` (`publicKey`)")
                execSQL("ALTER TABLE `nearby_record` ADD COLUMN `detectedAutomatically` INTEGER DEFAULT 0 NOT NULL")
            },
            /**
             * Added new automatic discovery table.
             */
            migration(8, 9) {
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `automatic_discovery` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timeStamp` INTEGER NOT NULL, `publicKey` BLOB NOT NULL, `proximity` INTEGER NOT NULL)"
                )
                execSQL("CREATE INDEX IF NOT EXISTS `index_automatic_discovery_publicKey` ON `automatic_discovery` (`publicKey`)")
            },
            /**
             * Updated definitions of [DbAutomaticDiscoveryEvent] and [DbNearbyRecord].
             */
            migration(9, 10) {
                // delete old table
                execSQL("DROP TABLE `automatic_discovery`")
                // create new table
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `automatic_discovery` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `publicKey` BLOB NOT NULL, `proximity` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER)"
                )
                execSQL("CREATE INDEX IF NOT EXISTS `index_automatic_discovery_publicKey` ON `automatic_discovery` (`publicKey`)")

                // create new temp table
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `nearby_record_temp` (`publicKey` BLOB NOT NULL, `timestamp` INTEGER NOT NULL, `detectedAutomatically` INTEGER NOT NULL, PRIMARY KEY(`publicKey`))")
                // copy data from old table to temp
                execSQL(
                    "INSERT INTO `nearby_record_temp` (`publicKey`, `timestamp`, `detectedAutomatically`) SELECT `publicKey`, max(`timestamp`), `detectedAutomatically` FROM `nearby_record` GROUP BY `publicKey`"
                )
                // delete old table
                execSQL("DROP TABLE `nearby_record`")
                // rename temp to original
                execSQL("ALTER TABLE `nearby_record_temp` RENAME TO `nearby_record`")
            },
            /**
             * Deleted debug table.
             */
            migration(10, 11) {
                // delete old table
                execSQL("DROP TABLE `debug_playground_entity`")
            },
            /**
             * Fixed cascade rule on [DbContactWithInfectionMessage].
             */
            migration(11, 12) {
                // create temp table
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `contact_with_infection_message_temp` (`messageUuid` TEXT NOT NULL, `publicKey` BLOB NOT NULL, PRIMARY KEY(`messageUuid`), FOREIGN KEY(`messageUuid`) REFERENCES `infection_message`(`uuid`) ON UPDATE CASCADE ON DELETE CASCADE )"
                )
                // copy data from old table to temp
                execSQL(
                    "INSERT INTO `contact_with_infection_message_temp` (`messageUuid`, `publicKey`) SELECT `messageUuid`, `publicKey` FROM `contact_with_infection_message`"
                )
                // delete old table
                execSQL("DROP TABLE `contact_with_infection_message`")
                // rename temp to original
                execSQL("ALTER TABLE `contact_with_infection_message_temp` RENAME TO `contact_with_infection_message`")
            },
            /**
             * Split [DbInfectionMessage] entity to [DbSentInfectionMessage] and [DbReceivedInfectionMessage].
             */
            migration(12, 13) {
                // add new tables
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `received_infection_message` (`uuid` TEXT NOT NULL, `messageType` TEXT NOT NULL, `timeStamp` INTEGER NOT NULL, PRIMARY KEY(`uuid`))"
                )
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `sent_infection_message` (`uuid` TEXT NOT NULL, `messageType` TEXT NOT NULL, `timeStamp` INTEGER NOT NULL, `publicKey` BLOB NOT NULL, PRIMARY KEY(`uuid`))"
                )
                // copy data from old table to new tables
                execSQL(
                    "INSERT INTO `received_infection_message` (`uuid`, `messageType`, `timeStamp`) SELECT `uuid`, `messageType`, `timeStamp` FROM `infection_message` WHERE `isReceived` = 1"
                )
                execSQL("""
                        INSERT INTO `sent_infection_message` (`uuid`, `messageType`, `timeStamp`, `publicKey`) 
                        SELECT `infection_message`.`uuid`, `infection_message`.`messageType`, `infection_message`.`timeStamp`, `contact_with_infection_message`.`publicKey` 
                        FROM `infection_message` JOIN `contact_with_infection_message`
                        ON (`infection_message`.`uuid` = `contact_with_infection_message`.`messageUuid`)
                        WHERE `infection_message`.`isReceived` = 0
                        """)

                // delete old tables
                execSQL("DROP TABLE `infection_message`")
                execSQL("DROP TABLE `contact_with_infection_message`")
            },
            /**
             * Update [Decision] to use UpperCase.
             */
            migration(13, 14) {
                execSQL("UPDATE `configuration_questionnaire_answer` SET `decision` = UPPER(`decision`)")
            },
            /**
             * Rename [DbQuestionnaireAnswer.id] to answerId, because of @Relation doesn't know which id to use for mapping.
             */
            migration(14, 15) {
                // create new temp table
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `configuration_questionnaire_answer_temp` (`answerId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `questionnaireId` INTEGER NOT NULL, `text` TEXT, `decision` TEXT, FOREIGN KEY(`questionnaireId`) REFERENCES `configuration_questionnaire`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )"
                )
                execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_configuration_questionnaire_answer_temp_questionnaireId` ON `configuration_questionnaire_answer_temp` (`questionnaireId`)"
                )
                // copy data from old table to temp
                execSQL(
                    "INSERT INTO `configuration_questionnaire_answer_temp` (`answerId`, `questionnaireId`, `text`, `decision`) SELECT `id`, `questionnaireId`, `text`, `decision` FROM `configuration_questionnaire_answer`"
                )
                // delete old table
                execSQL("DROP TABLE `configuration_questionnaire_answer`")
                // rename temp to original
                execSQL("ALTER TABLE `configuration_questionnaire_answer_temp` RENAME TO `configuration_questionnaire_answer`")
            },
            /**
             * Removing [DbNearbyRecord], [DbAutomaticDiscoveryEvent].
             */
            migration(15, 16) {
                // delete DbNearbyRecord
                execSQL("DROP TABLE `nearby_record`")
                // delete DbAutomaticDiscoveryEvent
                execSQL("DROP TABLE `automatic_discovery`")
            },
            /**
             * Removing [DbSentInfectionMessage].
             */
            migration(16, 17) {
                // delete DbSentInfectionMessage
                execSQL("DROP TABLE `sent_infection_message`")
            },
            /**
             * Add new column in `configuration` table for the number of days of temporary
             * exposure keys to be uploaded.
             */
            migration(17, 18) {
                // add new column for the number of days of temporary exposure keys that will be uploaded
                execSQL("ALTER TABLE `configuration` ADD COLUMN `uploadKeysDays` INTEGER")
            },
            /**
             * Add new table [DbSentTemporaryExposureKeys].
             */
            migration(18, 19) {
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `sent_temporary_exposure_keys` (`rollingStartIntervalNumber` INTEGER NOT NULL, `password` TEXT NOT NULL, `messageType` TEXT NOT NULL, PRIMARY KEY(`rollingStartIntervalNumber`))"
                )
            },
            /**
             * adding the exposure configuration parameters to the database
             */
            migration(19, 20) {
                execSQL("ALTER TABLE `configuration` ADD COLUMN `minimumRiskScore` INTEGER")
                execSQL("ALTER TABLE `configuration` ADD COLUMN `dailyRiskThreshold` INTEGER")
                execSQL("ALTER TABLE `configuration` ADD COLUMN `attenuationDurationThresholds` String")
                execSQL("ALTER TABLE `configuration` ADD COLUMN `attenuationLevelValues` String")
                execSQL("ALTER TABLE `configuration` ADD COLUMN `daysSinceLastExposureLevelValues` String")
                execSQL("ALTER TABLE `configuration` ADD COLUMN `durationLevelValues` String")
                execSQL("ALTER TABLE `configuration` ADD COLUMN `transmissionRiskLevelValues` String")
            }
        )
    }

    abstract fun configurationDao(): ConfigurationDao

    abstract fun infectionMessageDao(): InfectionMessageDao

    abstract fun temporaryExposureKeysDao(): TemporaryExposureKeysDao
}

/**
 * Helper fun to create migration instance.
 */
private fun migration(startVersion: Int, endVersion: Int, migrateProcedure: SupportSQLiteDatabase.() -> Unit): Migration {
    return object : Migration(startVersion, endVersion) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.migrateProcedure()
        }
    }
}