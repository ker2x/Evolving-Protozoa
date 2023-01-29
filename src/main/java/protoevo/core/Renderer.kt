package protoevo.core

import protoevo.biology.Cell
import protoevo.biology.EdibleCell
import protoevo.biology.Protozoan
import protoevo.env.Rock
import protoevo.env.Tank
import protoevo.utils.Utils.timeSeconds
import protoevo.utils.Vector2
import protoevo.utils.Window
import java.awt.*
import java.util.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Renderer(private val simulation: Simulation, private val window: Window) : Canvas() {
    var time = 0f
    private val tankViewCentre: Vector2
    private val tankRenderRadius: Float
    private var pan: Vector2
    private var panPosTemp: Vector2
    private var zoom: Float
    private var targetZoom: Float
    private val initialZoom = 1f
    private var zoomRange = 5.0
    private val zoomSlowness = 8.0
    private var superSimpleRender = false
    private var renderChemicals = true
    private var isAdvancedDebugInfo = false
    private val rotate = 0f
    private var lastFPSTime = 0.0
    private var framesRendered = 0
    var tracked: Cell? = null
        private set
    val uI: UI
    private var showUI = true
    var antiAliasing = Settings.antiAliasing
    val stats = HashMap<String, Int>(5, 1f)
    private val microscopePolygonNPoints = 500
    private val microscopePolygonXPoints = IntArray(microscopePolygonNPoints)
    private val microscopePolygonYPoints = IntArray(microscopePolygonNPoints)

    init {
        window.input.onLeftMouseRelease = Runnable { updatePanTemp() }
        stats["FPS"] = 0
        stats["Chunks Rendered"] = 0
        stats["Protozoa Rendered"] = 0
        stats["Pellets Rendered"] = 0
        stats["Broad Collision"] = 0
        stats["Broad Interact"] = 0
        stats["Zoom"] = 0
        tankRenderRadius = window.height / 2.0f
        tankViewCentre = Vector2(window.width * 0.5f, window.height * 0.5f)
        pan = Vector2(0f, 0f)
        panPosTemp = pan
        zoom = 1f
        targetZoom = zoom
        zoomRange *= simulation.tank!!.radius.toDouble()
        uI = UI(window, simulation, this)
        requestFocus()
        isFocusable = true
        lastFPSTime = timeSeconds
    }

    fun retina(g: Graphics2D, p: Protozoan) {
        val pos = toRenderSpace(p.pos)
        val r: Float = toRenderSpace(p.getRadius()).toFloat()
        val c = p.getColor()
        val dt = p.getRetina().cellAngle
        val fov = p.getRetina().fov
        val t0 = -p.dir.angle() - 0.5f * fov - rotate
        var t = t0
        g.color = c!!.darker()
        g.fillArc(
            (pos.x - r).toInt(),
            (pos.y - r).toInt(),
            (2 * r).toInt(),
            (2 * r).toInt(),
            Math.toDegrees(t0 - 2.8 * dt).toInt(),
            Math.toDegrees(fov + 5.6 * dt).toInt()
        )
        if (stats["FPS"]!! >= 0) {
            val constructionProgress = p.getRetina().health
            for (cell in p.getRetina()) {
                if (cell.anythingVisible()) {
                    val col = cell.colour
                    g.color = col
                } else {
                    if (constructionProgress < 1) g.color =
                        Color(255, 255, 255, (255 * constructionProgress).toInt()) else g.color = Color.WHITE
                }
                g.fillArc(
                    (pos.x - r).toInt(),
                    (pos.y - r).toInt(),
                    (2 * r).toInt(),
                    (2 * r).toInt(),
                    Math.toDegrees(t - 0.01).toInt(),
                    Math.toDegrees(dt + 0.01).toInt()
                )
                t += dt
            }
        }
        g.color = c.darker()
        g.fillArc(
            (pos.x - 0.8 * r).toInt(),
            (pos.y - 0.8 * r).toInt(),
            (2 * 0.8 * r).toInt(),
            (2 * 0.8 * r).toInt(),
            Math.toDegrees((t0 - 3 * dt).toDouble()).toInt(),
            Math.toDegrees((fov + 6 * dt).toDouble()).toInt()
        )
        g.color = c
        g.fillOval(
            (pos.x - 0.75 * r).toInt(),
            (pos.y - 0.75 * r).toInt(),
            (2 * 0.75 * r).toInt(),
            (2 * 0.75 * r).toInt()
        )
        if (p == tracked && simulation.inDebugMode()) {
            g.color = Color.YELLOW.darker()
            val dirAngle = p.dir.angle()
            for (cell in p.getRetina().cells) {
                for (ray in cell.rays) {
                    val rayRotated = ray.rotate(dirAngle)
                    val rayDir = rayRotated.unit().scale(toRenderSpace(Settings.protozoaInteractRange).toFloat())
                    val rayStart = pos.add(rayRotated.unit().scale(r))
                    val rayEnd = pos.add(rayDir)
                    g.drawLine(rayStart.x.toInt(), rayStart.y.toInt(), rayEnd.x.toInt(), rayEnd.y.toInt())
                }
            }
        }
    }

    private fun protozoa(g: Graphics2D, p: Protozoan) {
        val pos = toRenderSpace(p.pos)
        val r: Float = toRenderSpace(p.getRadius()).toFloat()
        if (circleNotVisible(pos, r)) return
        if (!p.wasJustDamaged) {
            drawOutlinedCircle(g, pos, r, p.getColor()!!)
        } else {
            drawOutlinedCircle(g, pos, r, p.getColor()!!, Color.RED)
        }
        for (spike in p.spikes) {
            if (r > 0.001 * window.height) {
                val s = g.stroke
                g.color = p.getColor()!!.darker().darker()
                g.stroke = BasicStroke((r * 0.2).toInt().toFloat())
                val spikeStartPos = p.dir.unit().rotate(spike.angle).setLength(r).translate(pos)
                val spikeLen = toRenderSpace(p.getSpikeLength(spike)).toFloat()
                val spikeEndPos = spikeStartPos.add(spikeStartPos.sub(pos).setLength(spikeLen))
                g.drawLine(
                    spikeStartPos.x.toInt(),
                    spikeStartPos.y.toInt(),
                    spikeEndPos.x.toInt(),
                    spikeEndPos.y.toInt()
                )
                g.stroke = s
            }
        }
        stats["Protozoa Rendered"] = stats["Protozoa Rendered"]!! + 1
        if (r >= 0.005 * window.height && p.getRetina().numberOfCells() > 0) retina(g, p)
        if (stats["FPS"]!! > 10 && r >= 10) {
            if (p.isHarbouringCrossover) {
                val nucleus = Polygon()
                val dt = (2 * Math.PI / 7.0).toFloat()
                val t0 = p.getVel().angle()
                val random = Random((p.id + p.mate!!.id).toLong())
                var t = 0f
                while (t < 2 * Math.PI) {
                    val percent = 0.1f + 0.2f * random.nextFloat()
                    val radius: Float = toRenderSpace(percent * p.getRadius()).toFloat()
                    val x = (radius * (0.1 + cos((t + t0).toDouble())) + pos.x).toInt()
                    val y = (radius * (-0.1 + sin((t + t0).toDouble())) + pos.y).toInt()
                    nucleus.addPoint(x, y)
                    t += dt
                }
                val b = p.mate!!.getColor()!!.brighter()
                g.color = Color(b.red, b.green, b.blue, 50)
                g.fillPolygon(nucleus)
            }
            val b = p.getColor()!!.brighter()
            fillCircle(g, pos, 3 * r / 7f, Color(b.red, b.green, b.blue, 50))
        }
    }

    private fun circleNotVisible(pos: Vector2, r: Float): Boolean {
        return pos.x - r > window.width || pos.x + r < 0 || pos.y - r > window.height || pos.y + r < 0
    }

    fun pointNotVisible(pos: Vector2): Boolean {
        return circleNotVisible(pos, 0f)
    }

    @JvmOverloads
    fun drawCircle(g: Graphics2D, pos: Vector2, r: Float, c: Color?, strokeSize: Int = (0.2 * r).toInt()) {
        g.color = c
        val s = g.stroke
        g.stroke = BasicStroke(strokeSize.toFloat())
        g.drawOval((pos.x - r).toInt(), (pos.y - r).toInt(), (2 * r).toInt(), (2 * r).toInt())
        g.stroke = s
    }

    private fun fillCircle(g: Graphics2D, pos: Vector2, r: Float, c: Color?) {
        g.color = c
        g.fillOval((pos.x - r).toInt(), (pos.y - r).toInt(), (2 * r).toInt(), (2 * r).toInt())
    }

    @JvmOverloads
    fun drawOutlinedCircle(g: Graphics2D, pos: Vector2, r: Float, c: Color, outline: Color? = c.darker()) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        g.color = c
        if (r <= 3) {
            val l = max(r.toInt(), 1)
            g.fillRect(
                (pos.x - l).toInt(), (pos.y - l).toInt(),
                (2 * l),
                (2 * l)
            )
        } else {
            g.fillOval((pos.x - r).toInt(), (pos.y - r).toInt(), (2 * r).toInt(), (2 * r).toInt())
        }
        if (antiAliasing) g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        ) else g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        if (r >= 10 && !superSimpleRender) drawCircle(g, pos, r, outline)
    }

    fun drawOutlinedCircle(g: Graphics2D, pos: Vector2, r: Float, c: Color, edgeAlpha: Float) {
        var edgeColour = c.darker()
        if (edgeAlpha < 1) edgeColour = Color(
            edgeColour.red, edgeColour.green, edgeColour.blue, (255 * edgeAlpha).toInt()
        )
        drawOutlinedCircle(g, pos, r, c, edgeColour)
    }

    private fun pellet(g: Graphics2D, p: EdibleCell) {
        val pos = toRenderSpace(p.pos)
        val r: Float = toRenderSpace(p.getRadius()).toFloat()
        drawOutlinedCircle(g, pos, r, p.getColor()!!)
        if (simulation.inDebugMode()) stats["Pellets Rendered"] = stats["Pellets Rendered"]!! + 1
    }

    private fun renderEntity(g: Graphics2D, e: Cell?) {
        if (e is Protozoan) protozoa(g, e) else if (e is EdibleCell) pellet(g, e)
    }

    private fun pointOnScreen(x: Int, y: Int): Boolean {
        return 0 <= x && x <= window.width && 0 <= y && y <= window.height
    }

    private fun squareInView(origin: Vector2, size: Int): Boolean {
        val originX = origin.x.toInt()
        val originY = origin.y.toInt()
        return pointOnScreen(originX, originY) ||
                pointOnScreen(originX + size, originY) ||
                pointOnScreen(originX, originY + size) ||
                pointOnScreen(originX + size, originY + size)
    }

    private fun chunkInView(chunk: Chunk): Boolean {
        val chunkCoords = toRenderSpace(chunk.tankCoords)
        val chunkSize = toRenderSpace(simulation.tank!!.chunkManager.chunkSize)
        return squareInView(chunkCoords, chunkSize)
    }

    private fun renderChunk(g: Graphics2D, chunk: Chunk) {
        if (chunkInView(chunk)) {
            if (simulation.inDebugMode()) stats["Chunks Rendered"] = stats["Chunks Rendered"]!! + 1
            for (e in chunk.cells) renderEntity(g, e)
        }
    }

    private fun renderEntityAttachments(g: Graphics2D, e: Cell) {
        val r1: Float = toRenderSpace(e.getRadius()).toFloat()
        val ePos = toRenderSpace(e.pos)
        if (circleNotVisible(ePos, r1) || e.cellBindings.isEmpty()) return
        val eColor = e.getColor()
        val red = eColor!!.red
        val green = eColor.green
        val blue = eColor.blue
        for (binding in e.cellBindings) {
            val attached = binding.destinationEntity
            val r2: Float = toRenderSpace(attached.getRadius()).toFloat()
            val r = min(r1, r2)
            val s = g.stroke
            g.stroke = BasicStroke(1.5f * r)
            val attachedPos = toRenderSpace(attached.pos)
            val attachedColor = attached.getColor()
            g.color = Color(
                (red + attachedColor!!.red) / 2,
                (green + attachedColor.green) / 2,
                (blue + attachedColor.blue) / 2
            ).brighter()
            g.drawLine(ePos.x.toInt(), ePos.y.toInt(), attachedPos.x.toInt(), attachedPos.y.toInt())
            g.stroke = s
        }
    }

    fun entities(g: Graphics2D, tank: Tank?) {
        for (chunk in tank!!.chunkManager.chunks) for (e in chunk!!.cells) renderEntityAttachments(g, e!!)
        for (chunk in tank.chunkManager.chunks) renderChunk(g, chunk!!)
        if (simulation.inDebugMode() && tracked != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            val chunkManager = tank.chunkManager
            val collisionEntities = chunkManager.broadCollisionDetection(
                tracked!!.pos!!, tracked!!.getRadius()
            )
            collisionEntities.forEachRemaining { o: Collidable? ->
                stats["Broad Collision"] = 1 + stats.getOrDefault("Broad Collision", 0)
                drawCollisionBounds(g, o, Color.RED.darker())
            }
            if (tracked is Protozoan) {
                val p = tracked as Protozoan
                drawCollisionBounds(g, tracked as Protozoan, p.interactRange, Color.WHITE.darker())
                val interactCells = chunkManager.broadEntityDetection(
                    (tracked as Protozoan).pos!!, p.interactRange
                )
                while (interactCells.hasNext()) {
                    val cell = interactCells.next()
                    if (p.cullFromRayCasting(cell)) continue
                    stats["Broad Interact"] = 1 + stats.getOrDefault("Broad Interact", 0)
                    drawCollisionBounds(g, cell!!, 1.1f * cell.getRadius(), Color.WHITE.darker())
                }
                for (sensor in p.contactSensors) {
                    val sensorPos = p.getSensorPosition(sensor)
                    fillCircle(g, toRenderSpace(sensorPos), 2f, Color.WHITE.darker())
                }
            }
            if (antiAliasing) g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            ) else g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        }
    }

    private fun rocks(g: Graphics2D, tank: Tank?) {
        val screenPoints = arrayOfNulls<Vector2>(3)
        val xPoints = IntArray(screenPoints.size)
        val yPoints = IntArray(screenPoints.size)
        for (rock in tank!!.rocks) {
            screenPoints[0] = toRenderSpace(rock!!.points[0])
            screenPoints[1] = toRenderSpace(rock.points[1])
            screenPoints[2] = toRenderSpace(rock.points[2])
            for (i in screenPoints.indices) xPoints[i] = screenPoints[i]!!.x.toInt()
            for (i in screenPoints.indices) yPoints[i] = screenPoints[i]!!.y.toInt()
            val color = Color(
                rock.getColor().red,
                rock.getColor().green,
                rock.getColor().blue,
                if (simulation.inDebugMode()) 100 else 255
            )
            g.color = color
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            g.fillPolygon(xPoints, yPoints, screenPoints.size)
            if (antiAliasing) g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            ) else g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            g.color = color.darker()
            val s = g.stroke
            g.stroke = BasicStroke(toRenderSpace(0.02f * Settings.maxRockSize).toFloat())
            //			g.drawPolygon(xPoints, yPoints, screenPoints.length);
            for (i in rock.edges.indices) {
                if (rock.isEdgeAttached(i)) continue
                val edge = rock.getEdge(i)
                val start = toRenderSpace(edge[0])
                val end = toRenderSpace(edge[1])
                g.drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
            }
            g.stroke = s
            if (simulation.inDebugMode()) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
                g.color = Color.YELLOW.darker()
                for (i in 0..2) {
                    val edge = rock.getEdge(i)
                    val edgeCentre = edge[0]!!.add(edge[1]!!).scale(0.5f)
                    val normalEnd = edgeCentre.add(rock.normals[i]!!.mul(0.005f))
                    val a = toRenderSpace(edgeCentre)
                    val b = toRenderSpace(normalEnd)
                    g.drawLine(a.x.toInt(), a.y.toInt(), b.x.toInt(), b.y.toInt())
                }
                if (antiAliasing) g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                ) else g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            }
        }
    }

    private fun drawCollisionBounds(g: Graphics2D, collidable: Collidable?, color: Color?) {
        if (collidable is Cell) {
            drawCollisionBounds(g, collidable, collidable.getRadius(), color)
        } else if (collidable is Rock) {
            drawCollisionBounds(g, collidable, color)
        }
    }

    private fun drawCollisionBounds(g: Graphics2D, rock: Rock, color: Color?) {
        val screenPoints = arrayOf(
            toRenderSpace(rock.points[0]),
            toRenderSpace(rock.points[1]),
            toRenderSpace(rock.points[2])
        )
        val xPoints = IntArray(screenPoints.size)
        for (i in screenPoints.indices) xPoints[i] = screenPoints[i].x.toInt()
        val yPoints = IntArray(screenPoints.size)
        for (i in screenPoints.indices) yPoints[i] = screenPoints[i].y.toInt()
        val strokeSize = 5
        val s = g.stroke
        g.stroke = BasicStroke(strokeSize.toFloat())
        g.color = color
        for (i in rock.edges.indices) {
            if (rock.isEdgeAttached(i)) continue
            val edge = rock.getEdge(i)
            val start = toRenderSpace(edge[0])
            val end = toRenderSpace(edge[1])
            g.drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
        }
        g.stroke = s
    }

    private fun drawCollisionBounds(g: Graphics2D, e: Cell, r: Float, color: Color?) {
        var r = r
        val pos = toRenderSpace(e.pos)
        r = toRenderSpace(r).toFloat()
        if (!circleNotVisible(pos, r)) drawCircle(g, pos, r, color, window.height / 500)
    }

    private fun maskTank(g: Graphics, coords: Vector2, r: Float, alpha: Int) {
        val n = microscopePolygonNPoints - 7
        for (i in 0 until n) {
            val t = (2 * Math.PI * i / n.toFloat()).toFloat()
            microscopePolygonXPoints[i] = (coords.x + r * cos(t.toDouble())).toInt()
            microscopePolygonYPoints[i] = (coords.y + r * sin(t.toDouble())).toInt()
        }
        microscopePolygonXPoints[n] = coords.x.toInt() + r.toInt()
        microscopePolygonYPoints[n] = coords.y.toInt()
        microscopePolygonXPoints[n + 1] = window.width
        microscopePolygonYPoints[n + 1] = coords.y.toInt()
        microscopePolygonXPoints[n + 2] = window.width
        microscopePolygonYPoints[n + 2] = 0
        microscopePolygonXPoints[n + 3] = 0
        microscopePolygonYPoints[n + 3] = 0
        microscopePolygonXPoints[n + 4] = 0
        microscopePolygonYPoints[n + 4] = window.height
        microscopePolygonXPoints[n + 5] = window.width
        microscopePolygonYPoints[n + 5] = window.height
        microscopePolygonXPoints[n + 6] = window.width
        microscopePolygonYPoints[n + 6] = coords.y.toInt()
        g.color = Color(0, 0, 0, alpha)
        g.fillPolygon(microscopePolygonXPoints, microscopePolygonYPoints, microscopePolygonNPoints)
    }

    private fun background(graphics: Graphics2D) {
        time += 0.1.toFloat()
        val backgroundR = 25 + (5 * cos(time / 100.0)).toInt()
        val backgroundG = 40 + (30 * sin(time / 100.0)).toInt()
        val backgroundB = 35 + (15 * cos(time / 100.0 + 1)).toInt()
        val backgroundColour = Color(backgroundR, backgroundG, backgroundB)
        graphics.color = backgroundColour
        graphics.fillRect(0, 0, window.width, window.height)
        if (Settings.enableChemicalField && renderChemicals) {
            if (antiAliasing) graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF
            ) else graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val chemicalSolution = simulation.tank!!.chemicalSolution
            val chemicalCellSize = toRenderSpace(chemicalSolution!!.gridSize)
            for (i in 0 until chemicalSolution.nXChunks) {
                for (j in 0 until chemicalSolution.nYChunks) {
                    val chemicalCellCoords = toRenderSpace(chemicalSolution.toTankCoords(i, j))
                    val x = chemicalCellCoords.x.toInt()
                    val y = chemicalCellCoords.y.toInt()
                    val density = chemicalSolution.getPlantPheromoneDensity(i, j)
                    if (density < 0.05f || !squareInView(chemicalCellCoords, chemicalCellSize)) continue
                    val alpha = density / 2f
                    val r = (alpha * 80 + (1 - alpha) * backgroundR).toInt()
                    val g = (alpha * 200 + (1 - alpha) * backgroundG).toInt()
                    val b = (alpha * 60 + (1 - alpha) * backgroundB).toInt()
                    graphics.color = Color(r, g, b)
                    val nextCellCoords = toRenderSpace(chemicalSolution.toTankCoords(i + 1, j + 1))
                    val nextX = nextCellCoords.x.toInt()
                    val nextY = nextCellCoords.y.toInt()
                    graphics.fillRect(x, y, nextX - x, nextY - y)
                    if (simulation.inDebugMode() && isAdvancedDebugInfo) {
                        graphics.color = Color.ORANGE.darker()
                        graphics.drawRect(x, y, chemicalCellSize, chemicalCellSize)
                    }
                }
            }
            if (antiAliasing) graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            ) else graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        }
        if (simulation.inDebugMode() && isAdvancedDebugInfo) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            graphics.color = Color.YELLOW.darker()
            val chunkManager = simulation.tank!!.chunkManager
            val w = toRenderSpace(chunkManager.chunkSize)
            for (chunk in chunkManager.chunks) {
                val chunkCoords = toRenderSpace(chunk!!.tankCoords)
                graphics.drawRect(chunkCoords.x.toInt(), chunkCoords.y.toInt(), w, w)
            }
            if (antiAliasing) graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            ) else graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        }
    }

    fun render() {
        val fps = stats["FPS"]!!
        stats.replaceAll { s: String?, v: Int? -> 0 }
        stats["FPS"] = fps
        val fpsDT = timeSeconds - lastFPSTime
        if (fpsDT >= 1) {
            stats["FPS"] = (framesRendered / fpsDT).toInt()
            framesRendered = 0
            lastFPSTime = timeSeconds
        }
        superSimpleRender = stats["FPS"]!! <= 10
        val bs = bufferStrategy
        if (bs == null) {
            this.createBufferStrategy(3)
            return
        }
        val graphics = bs.drawGraphics as Graphics2D
        if (antiAliasing) graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        ) else graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED)
        graphics.setRenderingHint(
            RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
        )
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
        zoom = targetZoom
        stats["Zoom"] = (100 * zoom).toInt()
        synchronized(simulation.tank!!) {
            try {
                background(graphics)
                entities(graphics, simulation.tank)
                rocks(graphics, simulation.tank)
                maskTank(
                    graphics,
                    tankViewCentre,
                    if (tracked != null) trackingScopeRadius else tankRenderRadius,
                    if (simulation.inDebugMode()) 150 else 200
                )
                maskTank(
                    graphics,
                    toRenderSpace(Vector2(0f, 0f)),
                    tankRenderRadius * zoom,
                    if (simulation.inDebugMode()) 100 else 255
                )
                if (showUI) uI.render(graphics)
                graphics.dispose()
                bs.show()
                framesRendered++
            } catch (ignored: ConcurrentModificationException) {
            }
        }
    }

    val tankViewRadius: Float
        get() = if (tracked != null) trackingScopeRadius else tankRenderRadius
    val trackingScopeRadius: Float
        get() = 3 * tankRenderRadius / 4

    fun toRenderSpace(v: Vector2?): Vector2 {
        return if (tracked == null) v!!.copy()
            .scale(1 / simulation.tank!!.radius)
            .translate(pan.mul(1 / tankRenderRadius))
            .scale(tankRenderRadius * zoom)
            .translate(tankViewCentre) else {
            v!!.copy()
                .take(tracked!!.pos!!) //					.rotate(rotate)
                .scale(tankRenderRadius * zoom / simulation.tank!!.radius)
                .translate(tankViewCentre)
        }
    }

    fun toRenderSpace(s: Float): Int {
        return (zoom * tankRenderRadius * s / simulation.tank!!.radius).toInt()
    }

    fun setZoom(d: Float) {
        targetZoom = (initialZoom + zoomRange * (d - 1f) / zoomSlowness).toFloat()
        if (targetZoom < 0) {
            pan = Vector2(0f, 0f)
            targetZoom = 0.01f
        }
        //		if (targetZoom > 20)
//			targetZoom = 20;
    }

    fun setPan(delta: Vector2?) {
        pan = panPosTemp.add(delta!!)
    }

    private fun updatePanTemp() {
        panPosTemp = pan
    }

    fun track(e: Cell?) {
        if (e != null) pan = Vector2(0f, 0f) else if (tracked != null) pan = tracked!!.pos!!.mul(tankRenderRadius)
        tracked = e
    }

    fun getZoom(): Float {
        return zoom
    }

    fun resetCamera() {
        tracked = null
        pan = Vector2(0f, 0f)
        panPosTemp = Vector2(0f, 0f)
        targetZoom = 1f
        zoom = 1f
    }

    fun toggleChemicalGrid() {
        renderChemicals = !renderChemicals
    }

    fun toggleAA() {
        antiAliasing = !antiAliasing
    }

    fun toggleUI() {
        showUI = !showUI
    }

    fun toggleAdvancedDebugInfo() {
        isAdvancedDebugInfo = !isAdvancedDebugInfo
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}