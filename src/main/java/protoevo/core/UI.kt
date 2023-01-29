package protoevo.core

import protoevo.biology.NNBrain
import protoevo.biology.Protozoan
import protoevo.neat.NeuralNetwork
import protoevo.neat.Neuron
import protoevo.utils.TextObject
import protoevo.utils.TextStyle
import protoevo.utils.TextStyle.Companion.numberToString
import protoevo.utils.Vector2
import protoevo.utils.Window
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.util.*
import javax.swing.JLabel
import javax.swing.JSlider
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class UI(private val window: Window, private val simulation: Simulation, private val renderer: Renderer) :
    ChangeListener {
    private val title: TextObject
    private val creatingTank: TextObject
    private val info: ArrayList<TextObject>
    private val debugInfo: ArrayList<TextObject>
    private val infoTextSize: Int
    private val textAwayFromEdge: Int
    private var showFPS = Settings.showFPS

    init {
        title = TextObject(
            "Evolving Protozoa",
            TextStyle.fontName,
            window.height / 20,
            Vector2(window.width / 60f, window.height / 15f)
        )
        title.color = Color.WHITE
        info = ArrayList()
        infoTextSize = window.height / 50
        val pelletText = TextObject("Number of pellets: ", infoTextSize)
        pelletText.color = Color.WHITE.darker()
        val protozoaText = TextObject("Number of protozoa: ", infoTextSize)
        protozoaText.color = Color.WHITE.darker()
        val trackingFitness = TextObject("Generation", infoTextSize)
        trackingFitness.color = Color.WHITE.darker()
        info.add(protozoaText)
        info.add(pelletText)
        info.add(trackingFitness)
        val fpsText = TextObject(
            "FPS: ",
            infoTextSize,
            Vector2(window.width * 0.9f, window.height / 20f)
        )
        fpsText.color = Color.YELLOW.darker()
        debugInfo = ArrayList()
        debugInfo.add(fpsText)
        textAwayFromEdge = window.width / 60
        creatingTank = TextObject(
            "Generating Initial Tank...", infoTextSize,
            Vector2(textAwayFromEdge.toFloat(), getYPosLHS(1))
        )
        creatingTank.color = Color.WHITE.darker()

        //Create the slider
        val framesPerSecond = JSlider(
            JSlider.VERTICAL,
            10, 60, 30
        )
        framesPerSecond.addChangeListener(this)
        framesPerSecond.majorTickSpacing = 10
        framesPerSecond.paintTicks = true

        //Create the label table
        val labelTable = Hashtable<Int, JLabel>(3)
        labelTable[0] = JLabel("Stop")
        labelTable[60 / 10] = JLabel("Slow")
        labelTable[60] = JLabel("Fast")
        framesPerSecond.labelTable = labelTable
        framesPerSecond.paintLabels = true
    }

    fun getYPosLHS(i: Int): Float {
        return 1.3f * infoTextSize * i + 3 * window.height / 20f
    }

    fun getYPosRHS(i: Int): Float {
        return 1.3f * infoTextSize * i + window.height / 20f
    }

    private fun renderStats(g: Graphics2D, lineNumber: Int, stats: Map<String, Float?>): Int {
        var lineNumber = lineNumber
        for ((key, value) in stats) {
            val text = key + ": " + numberToString(value!!, 2)
            val statText = TextObject(
                text, infoTextSize,
                Vector2(textAwayFromEdge.toFloat(), getYPosLHS(lineNumber))
            )
            statText.color = Color.WHITE.darker()
            statText.render(g)
            lineNumber++
        }
        return lineNumber
    }

    fun render(g: Graphics2D) {
        title.render(g)
        var lineNumber = 0
        if (!simulation.tank!!.hasBeenInitialised()) {
            creatingTank.render(g)
            return
        }
        var tracked = renderer.tracked
        if (tracked == null) {
            val tankStats = simulation.tank!!.stats
            lineNumber = renderStats(g, lineNumber, tankStats)
        } else {
            lineNumber = 0
            while (lineNumber < info.size) {
                info[lineNumber].position = Vector2(textAwayFromEdge.toFloat(), getYPosLHS(lineNumber))
                lineNumber++
            }
            info[0].text = "Number of pellets: " + simulation.tank!!.numberOfPellets()
            info[0].render(g)
            info[1].text = "Number of protozoa: " + simulation.tank!!.numberOfProtozoa()
            info[1].render(g)
            //			lineNumber++;
            if (tracked.isDead() && !tracked.children.isEmpty()) {
                renderer.track(tracked.children.iterator().next())
                tracked = renderer.tracked
            }
            val statsTitle = TextObject(
                tracked!!.prettyName + " Stats", (infoTextSize * 1.1).toInt(),
                Vector2(textAwayFromEdge.toFloat(), getYPosLHS(lineNumber))
            )
            statsTitle.color = Color.WHITE.darker()
            statsTitle.render(g)
            lineNumber++
            renderStats(g, lineNumber++, tracked.getStats()!!)
            if (tracked is Protozoan && tracked.brain is NNBrain) {
                val brain = tracked.brain as NNBrain
                renderBrainNetwork(brain.network, g)
            }
        }
        renderDebugStats(g)
    }

    fun toggleShowFPS() {
        showFPS = !showFPS
    }

    private fun renderDebugStats(g: Graphics2D) {
        if (!simulation.inDebugMode() && !showFPS) return
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        var lineNumber = 0
        val stats = renderer.stats
        val statTexts = arrayOfNulls<TextObject>(stats.keys.size)
        var maxWidth = 0
        for ((key, value) in stats) {
            val text = key + ": " + numberToString(value.toFloat(), 2)
            val statText = TextObject(
                text, infoTextSize,
                Vector2(0f, getYPosRHS(lineNumber))
            )
            maxWidth = max(maxWidth, statText.width)
            statText.color = Color.YELLOW.darker()
            statTexts[lineNumber] = statText
            lineNumber++
        }
        val x = (0.98 * window.width - maxWidth).toInt()
        for (statText in statTexts) {
            val y = statText!!.position.y.toInt()
            statText.position = Vector2(x.toFloat(), y.toFloat())
            if (simulation.inDebugMode() || showFPS && statText.text!!.contains("FPS")) statText.render(g)
        }
        if (renderer.antiAliasing) g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        ) else g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
    }

    private fun renderBrainNetwork(nn: NeuralNetwork, g: Graphics2D) {
        val networkDepth = nn.depth
        val boxWidth = (window.width / 2.0 - 1.2 * renderer.trackingScopeRadius).toInt()
        val boxHeight = 3 * window.height / 4
        val boxXStart = window.width - (boxWidth * 1.1).toInt()
        val boxYStart = (window.height - boxHeight) / 2
        if (simulation.inDebugMode()) {
            g.color = Color.YELLOW.darker()
            g.drawRect(boxXStart, boxYStart, boxWidth, boxHeight)
            var y = boxYStart
            while (y < boxYStart + boxHeight) {
                g.drawLine(boxXStart, y, boxXStart + boxWidth, y)
                y += boxHeight / networkDepth
            }
        }
        if (!nn.hasComputedGraphicsPositions()) precomputeGraphicsPositions(
            nn,
            boxXStart,
            boxYStart,
            boxWidth,
            boxHeight
        )
        val r = nn.graphicsNodeSpacing / 8
        for (neuron in nn.neurons) {
            if (neuron!!.type != Neuron.Type.SENSOR && neuron.isConnectedToOutput) {
                val s = g.stroke
                for (i in neuron.inputs.indices) {
                    val inputNeuron = neuron.inputs[i]
                    val weight = inputNeuron!!.lastState * neuron.weights[i]
                    if (abs(weight) <= 1e-4) continue
                    if (weight > 0) {
                        val p: Float = if (weight > 1) 1F else weight
                        g.color = Color((240 - 100 * p).toInt(), 240, (255 - 100 * p).toInt())
                    } else if (weight < 0) {
                        val p: Float = if (weight < -1) 1F else -weight
                        g.color = Color(
                            240, (240 - 100 * p).toInt(), (255 - 100 * p).toInt()
                        )
                    } else {
                        g.color = Color(240, 240, 240)
                    }
                    g.stroke = BasicStroke((r * abs(weight)).toInt().toFloat())
                    if (neuron === inputNeuron) {
                        g.drawOval(
                            neuron.graphicsX,
                            neuron.graphicsY - 2 * r,
                            3 * r,
                            3 * r
                        )
                    } else if (inputNeuron.depth == neuron.depth) {
                        val width = boxWidth / (2 * networkDepth)
                        val height = abs(neuron.graphicsY - inputNeuron.graphicsY)
                        val x = neuron.graphicsX - width / 2
                        val y = min(neuron.graphicsY, inputNeuron.graphicsY)
                        g.drawArc(x, y, width, height, -90, 180)
                    } else {
                        g.drawLine(
                            neuron.graphicsX, neuron.graphicsY,
                            inputNeuron.graphicsX, inputNeuron.graphicsY
                        )
                    }
                }
                g.stroke = s
            }
        }
        for (neuron in nn.neurons) {
            if (!neuron!!.isConnectedToOutput) continue
            var colour: Color
            var state = neuron.lastState.toDouble()
            if (state > 0) {
                state = if (state > 1) 1.0 else state
                colour = Color(
                    30, (50 + state * 150).toInt(), 30
                )
            } else if (state < 0) {
                state = if (state < -1) -1.0 else state
                colour = Color(
                    (50 - state * 150).toInt(), 30, 30
                )
            } else {
                colour = Color(10, 10, 10)
            }
            g.color = colour
            g.fillOval(
                neuron.graphicsX - r,
                neuron.graphicsY - r,
                2 * r,
                2 * r
            )
            if (simulation.inDebugMode()) if (neuron.type == Neuron.Type.HIDDEN) g.color =
                Color.YELLOW.darker() else if (neuron.type == Neuron.Type.SENSOR) g.color =
                Color.BLUE.brighter() else g.color = Color.WHITE.darker() else {
                g.color = Color.WHITE.darker()
            }
            if (neuron.depth == networkDepth && neuron.type == Neuron.Type.HIDDEN) g.color = Color(150, 30, 150)
            val s = g.stroke
            g.stroke = BasicStroke((0.3 * r).toInt().toFloat())
            g.drawOval(
                neuron.graphicsX - r,
                neuron.graphicsY - r,
                2 * r,
                2 * r
            )
            g.stroke = s
        }
        val mousePos = window.currentMousePosition
        val mouseX = mousePos.x.toInt()
        val mouseY = mousePos.y.toInt()
        if (boxXStart - 2 * r < mouseX && mouseX < boxXStart + boxWidth + 2 * r && boxYStart - 2 * r < mouseY && mouseY < boxYStart + boxHeight + 2 * r) {
            for (neuron in nn.neurons) {
                val x = neuron!!.graphicsX
                val y = neuron.graphicsY
                if (simulation.inDebugMode()) {
                    g.color = Color.YELLOW.darker()
                    g.drawRect(x - 2 * r, y - 2 * r, 4 * r, 4 * r)
                    g.color = Color.RED
                    val r2 = r / 5
                    g.drawOval(mouseX - r2, mouseY - r2, 2 * r2, 2 * r2)
                }
                if (neuron.label != null && x - 2 * r <= mouseX && mouseX <= x + 2 * r && y - 2 * r <= mouseY && mouseY <= y + 2 * r) {
                    val labelStr = neuron.label + " = " + numberToString(neuron.lastState, 2)
                    val label = TextObject(labelStr, infoTextSize)
                    label.color = Color.WHITE.darker()
                    var labelX = x - label.width / 2
                    val pad = (infoTextSize * 0.3).toInt()
                    val infoWidth = label.width + 2 * pad
                    if (labelX + infoWidth >= window.width) labelX = (window.width - 1.1 * infoWidth).toInt()
                    val labelY = (y - 1.1 * r - label.height / 2).toInt()
                    label.position = Vector2(labelX.toFloat(), labelY.toFloat())
                    g.color = Color.BLACK
                    g.fillRoundRect(
                        labelX - pad, labelY - 2 * pad - label.height / 2,
                        infoWidth, label.height + pad,
                        pad, pad
                    )
                    g.color = Color.WHITE.darker()
                    label.render(g)
                }
            }
        }
    }

    private fun precomputeGraphicsPositions(
        nn: NeuralNetwork,
        boxXStart: Int,
        boxYStart: Int,
        boxWidth: Int,
        boxHeight: Int
    ) {
        val neurons = nn.neurons
        val networkDepth = nn.calculateDepth()
        val depthWidthValues = IntArray(networkDepth + 1)
        Arrays.fill(depthWidthValues, 0)
        for (n in neurons) if (n!!.isConnectedToOutput) depthWidthValues[n.depth]++
        var maxWidth = 0
        for (width in depthWidthValues) maxWidth = max(maxWidth, width)
        val nodeSpacing = boxHeight / maxWidth
        nn.graphicsNodeSpacing = nodeSpacing
        for (depth in 0..networkDepth) {
            val x = boxXStart + depth * boxWidth / networkDepth
            val nNodes = depthWidthValues[depth]
            var i = 0
            for (n in neurons) {
                if (n!!.depth == depth && n.isConnectedToOutput) {
                    val y = (boxYStart + nodeSpacing / 2f + boxHeight / 2f - (nNodes / 2f - i) * nodeSpacing).toInt()
                    n.setGraphicsPosition(x, y)
                    i++
                }
            }
        }
        nn.setComputedGraphicsPositions(true)
    }

    override fun stateChanged(e: ChangeEvent) {
        println("change event")
    }
}