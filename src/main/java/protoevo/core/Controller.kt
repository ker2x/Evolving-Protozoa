package protoevo.core

import protoevo.core.Application.exit
import protoevo.utils.Input
import protoevo.utils.Vector2
import java.awt.event.KeyEvent

class Controller(private val input: Input, private val simulation: Simulation, private val renderer: Renderer) {
    init {
        input.registerOnPressHandler(KeyEvent.VK_F1) { simulation.togglePause() }
        input.registerOnPressHandler(KeyEvent.VK_F2) { renderer.uI.toggleShowFPS() }
        input.registerOnPressHandler(KeyEvent.VK_F3) { simulation.toggleDebug() }
        input.registerOnPressHandler(KeyEvent.VK_F4) { renderer.toggleAdvancedDebugInfo() }
        input.registerOnPressHandler(KeyEvent.VK_F9) { resetCamera() }
        input.registerOnPressHandler(KeyEvent.VK_F10) { renderer.toggleChemicalGrid() }
        input.registerOnPressHandler(KeyEvent.VK_F11) { renderer.toggleAA() }
        input.registerOnPressHandler(KeyEvent.VK_F12) { renderer.toggleUI() }
    }

    fun resetCamera() {
        renderer.resetCamera()
        input.reset()
    }

    private fun isPosInChunk(pos: Vector2, chunk: Chunk?): Boolean {
        val chunkCoords = renderer.toRenderSpace(chunk!!.tankCoords)
        val originX = chunkCoords.x.toInt()
        val originY = chunkCoords.y.toInt()
        val chunkSize = renderer.toRenderSpace(simulation.tank!!.chunkManager.chunkSize)
        return originX <= pos.x && pos.x < originX + chunkSize && originY <= pos.y && pos.y < originY + chunkSize
    }

    fun update() {
        if (input.getKey(KeyEvent.VK_ESCAPE)) {
            simulation.close()
            exit()
        }
        renderer.setZoom(1 - input.mouseWheelRotation / 7.0f)
        if (input.isLeftMouseJustPressed) handleLeftMouseClick()
        if (input.isRightMousePressed) handleRightMouseClick()
        renderer.setPan(input.mouseLeftClickDelta)
    }

    fun handleLeftMouseClick() {
        val pos: Vector2 = input.currentMousePosition
        var track = false
        synchronized(simulation.tank!!) {
            for (chunk in simulation.tank!!.chunkManager.chunks) {
                if (isPosInChunk(pos, chunk)) {
                    for (e in chunk!!.cells) {
                        val s = renderer.toRenderSpace(e!!.pos)
                        val r: Double = renderer.toRenderSpace(e.radius).toDouble()
                        if (s.sub(pos).len2() < r * r) {
                            renderer.track(e)
                            track = true
                            break
                        }
                    }
                }
            }
        }
        if (!track) renderer.track(null)
    }

    fun handleRightMouseClick() {
        val pos: Vector2 = input.currentMousePosition
        val tank = simulation.tank
        val r = renderer.toRenderSpace(tank!!.radius / 25f)
        synchronized(simulation.tank!!) {
            for (chunk in simulation.tank!!.chunkManager.chunks) {
                for (cell in chunk!!.cells) {
                    val cellPos = renderer.toRenderSpace(cell!!.pos)
                    val dir = cellPos.sub(pos)
                    var dist = dir.len2()
                    var i = 0
                    while (dist < r * r && i < 8) {
                        val p = r * r / dist
                        val strength = 1 / 100f
                        dir.setLength(strength * p * tank.radius / 8)
                        cell.physicsStep(Settings.simulationUpdateDelta)
                        cell.pos!!.translate(dir)
                        dist = cellPos.sub(pos).len2()
                        i++
                    }
                }
            }
        }
    }

    private fun isCircleInChunk(pos: Vector2, r: Int, chunk: Chunk): Boolean {
        return isPosInChunk(Vector2(pos.x - r, pos.y - r), chunk) ||
                isPosInChunk(Vector2(pos.x - r, pos.y + r), chunk) ||
                isPosInChunk(Vector2(pos.x + r, pos.y - r), chunk) ||
                isPosInChunk(Vector2(pos.x + r, pos.y + r), chunk)
    }
}