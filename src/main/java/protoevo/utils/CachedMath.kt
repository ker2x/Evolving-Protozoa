package protoevo.utils

/**
 * A class that caches the results of the sin, cos, tanh functions.
 * This is useful for when you need to call these functions a lot, as it can be much faster than the built-in functions.
 * The precision of the cache is set by the precision variable.
 * The cache is only accurate to 3 decimal places.
 */
object CachedMath {
    private const val precision = 1000  // The number of values to cache

    // Create the cache
    private val sinLookupTable = FloatArray(precision) { i -> kotlin.math.sin(2 * Math.PI * i / precision).toFloat() }
    private val tanhLookupTable = FloatArray(precision) { i -> kotlin.math.tanh(2 * Math.PI * i / precision).toFloat() }

    // Get the value from the cache
    private fun sinLookup(a: Int): Float = if (a >= 0) sinLookupTable[a % precision] else -sinLookupTable[-a % precision]
    private fun tanhLookup(a: Int): Float = if (a >= 0) tanhLookupTable[a % precision] else -tanhLookupTable[-a % precision]

    // Return the sin, cos, tanh of the given angle
    fun sin(a: Float): Float = sinLookup((a * precision / (2 * Math.PI)).toInt())
    fun cos(a: Float): Float = sinLookup(((a + Math.PI / 2) * precision / (2 * Math.PI)).toInt())
    fun tanh(a: Float): Float = tanhLookup((a * precision / (2 * Math.PI)).toInt())
}