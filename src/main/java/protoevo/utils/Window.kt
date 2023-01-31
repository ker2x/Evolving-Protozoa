package protoevo.utils

import protoevo.core.Application
import protoevo.core.Controller
import protoevo.core.Renderer
import protoevo.core.Simulation
import java.awt.Canvas
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JFrame
import javax.swing.Timer

/***
 * The window class for the application.
 * @param title The title of the window.
 * @param simulation The simulation to be rendered.
 * @constructor Creates a new window.
 */
class Window(title: String = "Evolving Protozoa", simulation: Simulation) : Canvas(), Runnable, ActionListener {

    val frame: JFrame
	val input: Input = Input()

    private var screenSize = Toolkit.getDefaultToolkit().screenSize
    private val renderer   = Renderer(simulation, this)
    private val controller = Controller(input, simulation, renderer)
    private val timer      = Timer(Application.refreshDelay.toInt(), this)

    val currentMousePosition: Vector2
        get() = input.currentMousePosition

    init {
        TextStyle.loadFonts()
        frame = JFrame(title)
        frame.preferredSize = screenSize
        frame.maximumSize   = screenSize
        frame.minimumSize   = screenSize
        frame.isResizable   = false
        frame.isUndecorated = true
        frame.isVisible     = true
        frame.extendedState = JFrame.MAXIMIZED_BOTH
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        frame.setLocationRelativeTo(null)
        frame.add(renderer)

        renderer.addKeyListener(input)
        renderer.addMouseListener(input)
        renderer.addMouseMotionListener(input)
        renderer.addMouseWheelListener(input)
        renderer.addFocusListener(input)
    }

    override fun run() {
        timer.start()
    }

    override fun actionPerformed(event: ActionEvent) {
        if (frame.isActive) {   // Better than isVisible. isVisible is rendering even if the window is minimized.
            controller.update()
            renderer.render()
            timer.restart()
        }
    }

    override fun getWidth(): Int {
        return screenSize.width
    }

    override fun getHeight(): Int {
        return screenSize.height
    }

    companion object {
        private const val serialVersionUID = -2111860594941368902L
    }
}