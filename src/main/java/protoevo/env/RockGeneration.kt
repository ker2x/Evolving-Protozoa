package protoevo.env

import protoevo.core.Settings
import protoevo.core.Simulation
import protoevo.utils.Geometry.doesLineIntersectCircle
import protoevo.utils.Vector2
import protoevo.utils.Vector2.Companion.fromAngle

object RockGeneration {
    @JvmOverloads
    fun generateRingOfRocks(tank: Tank, ringCentre: Vector2?, ringRadius: Float, breakProb: Float = 0f) {
        val angleDelta = (2 * Math.asin((Settings.minRockSize / (2 * ringRadius)).toDouble())).toFloat()
        var currentRock: Rock? = null
        var angle = 0f
        while (angle < 2 * Math.PI) {
            if (breakProb > 0 && Simulation.RANDOM.nextFloat() < breakProb) {
                currentRock = null
                angle += angleDelta * 10
            }
            if (currentRock == null || currentRock.allEdgesAttached()) {
                currentRock = newCircumferenceRockAtAngle(ringCentre, ringRadius, angle)
                if (isRockObstructed(currentRock, tank.rocks, Settings.minRockOpeningSize)) {
                    currentRock = null
                } else {
                    tank.rocks.add(currentRock)
                }
            } else {
                var bestNextRock: Rock? = null
                var bestRockDistToCirc = Float.MAX_VALUE
                var bestRockAttachIdx = -1
                for (i in currentRock.edges.indices) {
                    val sizeRange = Settings.maxRockSize - Settings.minRockOpeningSize
                    val rockSize = 1.5f * Settings.minRockOpeningSize + sizeRange * Simulation.RANDOM.nextFloat()
                    if (!currentRock.isEdgeAttached(i)) {
                        val newRock = newAttachedRock(currentRock, i, tank.rocks, rockSize)
                        if (newRock != null) {
                            val dist = Math.abs(newRock.centre.sub(ringCentre!!).len() - ringRadius)
                            if (dist < bestRockDistToCirc) {
                                bestRockDistToCirc = dist
                                bestNextRock = newRock
                                bestRockAttachIdx = i
                            }
                        }
                    }
                }
                if (bestNextRock != null) {
                    tank.rocks.add(bestNextRock)
                    bestNextRock.setEdgeAttached(0)
                    currentRock.setEdgeAttached(bestRockAttachIdx)
                }
                currentRock = bestNextRock
            }
            angle += angleDelta
        }
    }

    private fun newCircumferenceRockAtAngle(pos: Vector2?, r: Float, angle: Float): Rock {
        val dir = fromAngle(angle)
        val centre = dir.mul(r).add(pos!!)
        return newRockAt(centre, dir)
    }

    fun generateRocks(tank: Tank) {
        val unattachedRocks: MutableList<Rock?> = ArrayList()
        for (rock in tank.rocks) if (!rock!!.allEdgesAttached()) unattachedRocks.add(rock)
        for (i in 0 until Settings.rockGenerationIterations) {
            if (i < Settings.rockSeedingIterations || unattachedRocks.size == 0 || Simulation.RANDOM.nextFloat() > Settings.rockClustering) {
                val rock = newRock(tank)
                if (tryAdd(rock, tank.rocks)) {
                    unattachedRocks.add(rock)
                }
            } else {
                val toAttach = selectRandomUnattachedRock(tank, unattachedRocks)
                var edgeIdx = 0
                while (edgeIdx < 3) {
                    if (!toAttach!!.isEdgeAttached(edgeIdx)) break
                    edgeIdx++
                }
                if (edgeIdx == 3) continue
                val rock = newAttachedRock(toAttach, edgeIdx, tank.rocks)
                if (rock != null) {
                    tank.rocks.add(rock)
                    unattachedRocks.add(rock)
                    rock.setEdgeAttached(0)
                    toAttach!!.setEdgeAttached(edgeIdx)
                    if (edgeIdx == 2) // no edges left to attach to
                        unattachedRocks.remove(toAttach)
                }
            }
        }
    }

    fun newAttachedRock(toAttach: Rock?, edgeIdx: Int, rocks: List<Rock?>?): Rock? {
        val sizeRange = Settings.maxRockSize - Settings.minRockSize
        val rockSize = Settings.minRockSize + sizeRange * Simulation.RANDOM.nextFloat()
        return newAttachedRock(toAttach, edgeIdx, rocks, rockSize)
    }

    fun newAttachedRock(toAttach: Rock?, edgeIdx: Int, rocks: List<Rock?>?, rockSize: Float): Rock? {
        val edge = toAttach!!.getEdge(edgeIdx)
        val normal = toAttach.normals[edgeIdx]
        val p1 = edge[0]
        val p2 = edge[1]
        val p3 = p1!!.add(p2!!).scale(0.5f).translate(normal!!.unit().scale(rockSize))
        val newEdge1 = arrayOf<Vector2?>(p1, p3)
        val newEdge2 = arrayOf<Vector2?>(p2, p3)
        return if (notInAnyRocks(newEdge1, newEdge2, rocks, toAttach)
            && leavesOpening(p3, rocks, Settings.minRockOpeningSize)
        ) {
            Rock(p1, p2, p3)
        } else null
    }

    private fun selectRandomUnattachedRock(tank: Tank, unattachedRocks: List<Rock?>): Rock? {
        val i = Simulation.RANDOM.nextInt(unattachedRocks.size)
        return unattachedRocks[i]
    }

    private fun tryAdd(rock: Rock, rocks: MutableList<Rock?>?): Boolean {
        for (otherRock in rocks!!) if (rock.intersectsWith(otherRock)) return false
        rocks.add(rock)
        return true
    }

    private fun isRockObstructed(rock: Rock?, rocks: List<Rock?>?, openingSize: Float): Boolean {
        for (otherRock in rocks!!) if (otherRock!!.intersectsWith(rock)) return true
        if (openingSize > 0) for (point in rock!!.points) if (!leavesOpening(point, rocks, openingSize)) return true
        return false
    }

    private fun notInAnyRocks(
        e1: Array<Vector2?>,
        e2: Array<Vector2?>,
        rocks: List<Rock?>?,
        excluding: Rock?
    ): Boolean {
        for (rock in rocks!!) for (rockEdge in rock!!.edges) if (rock != excluding &&
            (Rock.Companion.edgesIntersect(rockEdge, e1) || Rock.Companion.edgesIntersect(rockEdge, e2))
        ) return false
        return true
    }

    private fun leavesOpening(rockPoint: Vector2?, rocks: List<Rock?>?, openingSize: Float): Boolean {
        for (rock in rocks!!) {
            for (edge in rock!!.edges) {
                if (doesLineIntersectCircle(edge, rockPoint!!, openingSize)) return false
            }
        }
        return true
    }

    fun newRock(tank: Tank): Rock {
        val centreR = tank.radius * Simulation.RANDOM.nextFloat()
        val centreT = (2 * Math.PI * Simulation.RANDOM.nextFloat()).toFloat()
        val centre = fromAngle(centreT).setLength(centreR)
        return newRockAt(centre)
    }

    fun newRockAt(centre: Vector2): Rock {
        val dir = fromAngle((2 * Math.PI * Simulation.RANDOM.nextFloat()).toFloat())
        return newRockAt(centre, dir)
    }

    fun newRockAt(centre: Vector2, dir: Vector2): Rock {
        var dir = dir
        val sizeRange = Settings.maxRockSize - Settings.minRockSize
        val rockSize = Settings.minRockSize + sizeRange * Simulation.RANDOM.nextFloat()
        val k1 = 0.95f + 0.1f * Simulation.RANDOM.nextFloat()
        val p1 = centre.add(dir.setLength(k1 * rockSize))
        val tMin = Settings.minRockSpikiness
        val tMax = (2 * Math.PI / 3).toFloat()
        val t1 = tMin + (tMax - 2 * tMin) * Simulation.RANDOM.nextFloat()
        val k2 = 0.95f + 0.1f * Simulation.RANDOM.nextFloat()
        dir = dir.rotate(t1)
        val p2 = centre.add(dir.setLength(k2 * rockSize))
        val t2 = tMin + (tMax - tMin) * Simulation.RANDOM.nextFloat()
        val l3 = Settings.minRockSize + sizeRange * Simulation.RANDOM.nextFloat()
        dir = dir.rotate(t2)
        val p3 = centre.add(dir.setLength(l3))
        return Rock(p1, p2, p3)
    }
}