package at.roteskreuz.stopcorona.skeleton.core.constants

/**
 * Request codes for fragment for result or any other identification which is required to be unique.
 * This ensures unique request codes for every screen, permission and whatever you need to be unique
 * across whole application.
 *
 * For fragments if `onActivityResult()` is called in deep fragment stack, only the right fragment
 * will check the answer, so you have not to worry about consuming the result by a bad fragment.
 *
 * In fragment you must define a companion object like below:
 * ```
 * companion object {
 *      val REQUEST_PHASE1 = Constants.Request.REQUEST_NAME_PARENT_SCREEN + 1
 *      val REQUEST_PHASE2 = Constants.Request.REQUEST_NAME_PARENT_SCREEN + 2
 *      val REQUEST_PHASE3 = Constants.Request.REQUEST_NAME_PARENT_SCREEN + 3
 *      ...
 * }
 * ```
 * which is based on one screen request code.
 * Maximum number of sub request codes based on one category request is 2^8 = 256. (8 is OFFSET)
 *
 * Maximum number of request codes based on [APP_BASE_REQUEST] for categories can be 0xFF - 0xC2 = 0x3D = 61.
 * If exceed this will be greater than 16 bits, which doesn't allow android permission manager.
 */
object BaseAppRequest {

    /**
     * C2 is the random identifier which should be unique by SinnerSchrader apps.
     */
    const val APP_BASE_REQUEST = 0xC2 shl 8 // length is 8, so 16 bits are used
    const val OFFSET = 8

    const val REQUEST_PERMISSION = APP_BASE_REQUEST + (0 shl OFFSET)
}