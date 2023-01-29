package protoevo.core

import protoevo.utils.Vector2
import java.awt.Color
import java.io.Serializable

abstract class Collidable {
    class Collision : Serializable {
        val point = Vector2(0f, 0f)
        var collided = false

        companion object {
            const val serialVersionUID = 1L
        }
    }

    abstract fun pointInside(p: Vector2?): Boolean
    abstract fun rayIntersects(start: Vector2?, end: Vector2?): Boolean
    abstract fun rayCollisions(start: Vector2?, end: Vector2?, collisions: Array<Collision>)
    abstract fun getColor(): Color?
    abstract fun getMass(): Float
    abstract fun getBoundingBox(): Array<Vector2?>?
    abstract fun handlePotentialCollision(other: Collidable?, delta: Float): Boolean
}