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

class Window(title: String?, simulation: Simulation) : Canvas(), Runnable, ActionListener {
    val frame: JFrame
    @JvmField
	val input: Input
    private val renderer: Renderer
    private val simulation: Simulation
    val controller: Controller
    private val width: Int
    private val height: Int
    private val timer = Timer(Application.refreshDelay.toInt(), this)

    init {
        TextStyle.loadFonts()
        val d = Toolkit.getDefaultToolkit().screenSize
        width = d.getWidth().toInt()
        height = d.getHeight().toInt()
        //		width = 1920;
//		height = 1080;
        this.simulation = simulation
        input = Input()
        renderer = Renderer(simulation, this)
        frame = JFrame(title)
        frame.preferredSize = Dimension(width, height)
        frame.maximumSize = Dimension(width, height)
        frame.minimumSize = Dimension(width, height)
        frame.extendedState = JFrame.MAXIMIZED_BOTH
        frame.isUndecorated = true
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isResizable = false
        frame.setLocationRelativeTo(null)
        frame.add(renderer)
        frame.isVisible = true
        controller = Controller(input, simulation, renderer)
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
        if (frame.isVisible) {
            controller.update()
            renderer.render()
            timer.restart()
        }
    }

    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    val dimensions: Vector2
        get() = Vector2(width.toFloat(), height.toFloat())

    val currentMousePosition: Vector2
        get() = input.currentMousePosition

    companion object {
        private const val serialVersionUID = -2111860594941368902L
    }
}