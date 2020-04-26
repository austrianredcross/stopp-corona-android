package at.roteskreuz.stopcorona.skeleton.core.model.db.converters

import androidx.room.TypeConverter

/**
 * Converter for using generic enum.
 * Usage:
 * ```
 * class MyEnumTypeConverter : EnumTypeConverter<MyEnum>({ enumValueOf(it) })
 * ```
 * and use on top of the db entity data class
 * ```
 * @TypeConverters(MyEnumTypeConverter::class)
 * data class DbCity(...)
 * ```
 */
open class EnumTypeConverter<T : Enum<T>>(private val enumValueOfParser: ((String) -> T)) {

    @TypeConverter
    fun enumTypeToString(taxType: T?): String? = taxType?.name

    @TypeConverter
    fun stringToEnum(taxType: String?): T? = taxType?.let { enumValueOfParser(it) }
}