package protoevo.utils

/***
 * A collection of utility functions.
 */
object Utils {
    @JvmStatic
    val timeSeconds: Double
        get() = System.currentTimeMillis() / 1000.0
}