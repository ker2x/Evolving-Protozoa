package protoevo.utils

object Utils {
    @JvmStatic
    val timeSeconds: Double
        get() = System.currentTimeMillis() / 1000.0
}