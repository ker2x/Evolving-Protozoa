package protoevo.utils

import java.awt.*
import java.awt.font.FontRenderContext

class TextObject(var text: String?, private var m_font: String?, var size: Int, var position: Vector2) {
    var style: Int
    var color: Color?

    constructor(text: String?, size: Int) : this(text, TextStyle.fontName, size, Vector2(0f, 0f))
    constructor(text: String?, size: Int, position: Vector2) : this(text, TextStyle.fontName, size, position)

    init {
        style = Font.PLAIN
        color = Color.BLACK
    }

    fun render(g: Graphics2D) {
        g.font = Font(m_font, style, size)
        g.color = color
        g.drawString(text, position.x.toInt(), position.y.toInt())
    }

    fun setTextStyle(textStyle: TextStyle) {
        size = textStyle.size
        m_font = textStyle.font
        color = textStyle.color
        style = textStyle.style
    }

    val width: Int
        get() = Font(m_font, style, size)
            .getStringBounds(
                text,
                FontRenderContext(null, true, false)
            ).width.toInt()
    val height: Int
        get() = Font(m_font, style, size)
            .getStringBounds(
                text,
                FontRenderContext(null, true, false)
            ).height.toInt()

    fun setFont(font: String?) {
        m_font = font
    }

    val font: Font
        get() = Font(m_font, style, size)
}