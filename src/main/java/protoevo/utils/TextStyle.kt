package protoevo.utils

import java.awt.Color
import java.awt.Font
import java.awt.FontFormatException
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.IOException

class TextStyle {
    var size = 0
    var style = 0
    var color: Color? = null
    var font: String? = null

    companion object {
        @JvmField
        var fontName = "Fira Code Retina"
        @JvmStatic
        fun numberToString(d: Float, dp: Int): String {
            val ten = Math.pow(10.0, dp.toDouble()).toFloat()
            val v = (d * ten).toInt() / ten
            return if (v.toInt().toFloat() == v) Integer.toString(v.toInt()) else java.lang.Float.toString(v)
        }

        fun loadFonts() {
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            try {
                val f = File("resources/fonts/FiraCode-Retina.ttf")
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, f))
            } catch (e: IOException) {
                println("Could not load FiraCode-Retina font")
            } catch (e: FontFormatException) {
                println("Could not load FiraCode-Retina font")
            } catch (e: NullPointerException) {
                println("Could not load FiraCode-Retina font")
            }
        }
    }
}