package at.roteskreuz.stopcorona.skeleton.core.model.entities

/**
 * Interface for app API data classes (entities).
 */
interface ApiEntity<D : DbEntity> {

    /**
     * Convert API entity to DB entity.
     */
    fun asDbEntity(): D
}