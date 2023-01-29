package protoevo.utils

import java.awt.event.*
import java.util.*
import javax.swing.SwingUtilities

class Input : KeyListener, FocusListener, MouseListener, MouseMotionListener, MouseWheelListener {
    var currentMousePosition = Vector2(0f, 0f)
        private set
    private var positionOnLeftClickDown = Vector2(0f, 0f)
    var mouseLeftClickDelta: Vector2? = Vector2(0f, 0f)
        private set
    private val keys = BooleanArray(65536)
    private val mouseButtons = BooleanArray(4)
    private val mouseJustDown = BooleanArray(4)
    private val onPressHandlers = HashMap<Int, Runnable>()
    @JvmField
    var onLeftMouseRelease: Runnable? = null
    fun registerOnPressHandler(key: Int, handler: Runnable) {
        onPressHandlers[key] = handler
    }

    fun unregisterOnPressHandler(key: Int) {
        onPressHandlers.remove(key)
    }

    var mouseWheelRotation = 0
        private set

    fun reset() {
        mouseWheelRotation = 0
        currentMousePosition = Vector2(0f, 0f)
        mouseLeftClickDelta = Vector2(0f, 0f)
        positionOnLeftClickDown = Vector2(0f, 0f)
    }

    fun getKey(key: Int): Boolean {
        return keys[key]
    }

    fun getMouse(button: Int): Boolean {
        return mouseButtons[button]
    }

    private fun mouseButtonJustDown(button: Int): Boolean {
        if (getMouse(button) && !mouseJustDown[button]) {
            mouseJustDown[button] = true
            return true
        } else if (!getMouse(button)) {
            mouseJustDown[button] = false
        }
        return false
    }

    val isLeftMouseJustPressed: Boolean
        get() = mouseButtonJustDown(1)
    val isRightMouseJustPressed: Boolean
        get() = mouseButtonJustDown(3)
    val isLeftMousePressed: Boolean
        get() = getMouse(1)
    val isRightMousePressed: Boolean
        get() = getMouse(3)

    override fun mouseDragged(event: MouseEvent) {
        currentMousePosition = Vector2(event.x.toFloat(), event.y.toFloat())
        mouseLeftClickDelta = if (SwingUtilities.isLeftMouseButton(event)) {
            val newPosition = Vector2(event.x.toFloat(), event.y.toFloat())
            newPosition.sub(positionOnLeftClickDown)
        } else {
            Vector2(0f, 0f)
        }
    }

    override fun mouseMoved(event: MouseEvent) {
        currentMousePosition = Vector2(event.x.toFloat(), event.y.toFloat())
    }

    override fun mouseClicked(event: MouseEvent) {
        currentMousePosition = Vector2(event.x.toFloat(), event.y.toFloat())
    }

    override fun mouseEntered(event: MouseEvent) {
        currentMousePosition = Vector2(event.x.toFloat(), event.y.toFloat())
    }

    override fun mouseExited(event: MouseEvent) {
        currentMousePosition = Vector2(event.x.toFloat(), event.y.toFloat())
    }

    override fun mousePressed(event: MouseEvent) {
        val button = event.button
        currentMousePosition = Vector2(event.x.toFloat(), event.y.toFloat())
        if (SwingUtilities.isLeftMouseButton(event)) positionOnLeftClickDown = currentMousePosition
        if (0 < button && button < mouseButtons.size) mouseButtons[button] = true
    }

    override fun mouseReleased(event: MouseEvent) {
        val button = event.button
        if (SwingUtilities.isLeftMouseButton(event)) {
            mouseLeftClickDelta = Vector2(0f, 0f)
            if (onLeftMouseRelease != null) onLeftMouseRelease!!.run()
        }
        if (0 < button && button < mouseButtons.size) mouseButtons[button] = false
    }

    override fun focusGained(event: FocusEvent) {}
    override fun focusLost(event: FocusEvent) {
        Arrays.fill(keys, false)
        Arrays.fill(mouseButtons, false)
    }

    override fun keyPressed(event: KeyEvent) {
        val key = event.keyCode
        if (0 < key && key < keys.size) {
            if (!keys[key] and onPressHandlers.containsKey(key)) {
                try {
                    onPressHandlers[key]!!.run()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
            keys[key] = true
        }
    }

    override fun keyReleased(event: KeyEvent) {
        val key = event.keyCode
        if (0 < key && key < keys.size) keys[key] = false
    }

    override fun keyTyped(event: KeyEvent) {}
    override fun mouseWheelMoved(arg0: MouseWheelEvent) {
        mouseWheelRotation += arg0.wheelRotation
    }
}