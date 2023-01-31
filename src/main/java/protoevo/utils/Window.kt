package protoevo.utils

import protoevo.core.Application
import protoevo.core.Controller
import protoevo.core.Renderer
import protoevo.core.Simulation
import java.awt.Canvas
import java.awt.Dimension
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

    private val renderer: Renderer
    private val controller: Controller
    private var screenSize :Dimension = Toolkit.getDefaultToolkit().screenSize
    private val timer = Timer(Application.refreshDelay.toInt(), this)

    init {
        TextStyle.loadFonts()

        renderer = Renderer(simulation, this)
        controller = Controller(input, simulation, renderer)

        frame = JFrame(title)
        frame.preferredSize = screenSize
        frame.maximumSize   = screenSize
        frame.minimumSize   = screenSize
        frame.extendedState = JFrame.MAXIMIZED_BOTH
        frame.isUndecorated = true
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isResizable = false
        frame.setLocationRelativeTo(null)
        frame.add(renderer)
        frame.isVisible = true

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

    val currentMousePosition: Vector2
        get() = input.currentMousePosition

    companion object {
        private const val serialVersionUID = -2111860594941368902L
    }
}