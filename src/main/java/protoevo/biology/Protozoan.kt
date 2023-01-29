package protoevo.biology

import protoevo.biology.genes.CAMProductionGene
import protoevo.biology.genes.ProtozoaGenome
import protoevo.biology.genes.RetinalProductionGene
import protoevo.core.Collidable
import protoevo.core.Particle
import protoevo.core.Settings
import protoevo.core.Simulation
import protoevo.env.Tank
import protoevo.utils.Vector2
import java.io.Serializable
import kotlin.math.pow

class Protozoan(@JvmField val genome: ProtozoaGenome, tank: Tank?,
) : Cell(tank) {
    @JvmField
	@Transient
    var id = Simulation.RANDOM.nextInt()
    private var crossOverGenome: ProtozoaGenome? = null
    var mate: Protozoan? = null
        //private set
    private var timeMating = 0f
    private var retina: Retina
    @JvmField
	val brain: Brain = genome.brain()
    private var shieldFactor = 1.3f
    private val attackFactor = 10f
    private var deathRate = 0f
    private val herbivoreFactor: Float
    private val splitRadius: Float
    @JvmField
	val dir = Vector2(0f, 0f)

    class Spike : Serializable {
        @JvmField
		var length = 0f
        @JvmField
		var angle = 0f
        @JvmField
		var hidden_growthRate = 0f
        @JvmField
		var currentLength = 0f
        fun update(delta: Float) {
            if (currentLength < length) {
                currentLength = Math.min(currentLength + delta * hidden_growthRate, length)
            }
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    class ContactSensor : Serializable {
        var angle = 0f
        var contact: Collidable? = null
        fun reset() {
            contact = null
        }

        fun inContact(): Boolean {
            return contact != null
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    @JvmField
	val contactSensors: Array<ContactSensor?>
    @JvmField
	val spikes: Array<Spike>
    @JvmField
	var wasJustDamaged = false
    private var cosHalfFov = 0f

    constructor(tank: Tank?) : this(ProtozoaGenome(), tank)

    fun getSensorPosition(sensor: ContactSensor?): Vector2 {
        return pos!!.add(dir.rotate(sensor!!.angle).setLength(1.01f * getRadius()))
    }

    override fun handlePotentialCollision(other: Collidable?, delta: Float): Boolean {
        if (other !== this) {
            for (contactSensor in contactSensors) {
                if (other!!.pointInside(getSensorPosition(contactSensor))) {
                    contactSensor!!.contact = other
                }
            }
        }
        return super.handlePotentialCollision(other, delta)
    }

    override val prettyName: String?
        get() = "Protozoan"

    fun cullFromRayCasting(o: Collidable?): Boolean {
        if (o is Particle) {
            val p = o
            val dx = p.pos!!.x - pos!!.x
            val dy = p.pos!!.y - pos!!.y
            val d2 = dx * dx + dy * dy
            val dirX = dir.x
            val dirY = dir.y
            val dirLength2 = dir.len2()
            return (dx * dirX + dy * dirY) / Math.sqrt((d2 * dirLength2).toDouble()) < cosHalfFov
        }
        return false
    }

    private val rayEndTmp = Vector2(0f, 0f)
    private val rayStartTmp = Vector2(0f, 0f)
    private val collisions = arrayOf(
        Collision(), Collision()
    )

    init {
        retina = genome.retina()
        spikes = genome.spikes
        herbivoreFactor = genome.herbivoreFactor
        setRadius(genome.radius)
        healthyColour = genome.colour
        growthRate = genome.growthRate
        splitRadius = genome.splitRadius
        pos = Vector2(0f, 0f)
        val t = (2 * Math.PI * Simulation.RANDOM.nextDouble()).toFloat()
        dir[(0.1f * Math.cos(t.toDouble())).toFloat()] = (0.1f * Math.sin(t.toDouble())).toFloat()
        contactSensors = arrayOfNulls(Settings.numContactSensors)
        for (i in 0 until Settings.numContactSensors) {
            contactSensors[i] = ContactSensor()
            contactSensors[i]!!.angle = (2 * Math.PI * i / Settings.numContactSensors).toFloat()
        }
        setDigestionRate(Food.Type.Meat, 1 / herbivoreFactor)
        setDigestionRate(Food.Type.Plant, herbivoreFactor)
        setComplexMoleculeProductionRate(
            Food.ComplexMolecule.Retinal,
            genome.getGeneValue(RetinalProductionGene::class.java)
        )
        if (retina.numberOfCells() > 0) addConstructionProject(retina.constructionProject)
        val camProduction = genome.getGeneValue(
            CAMProductionGene::class.java
        )
        if (camProduction != null) for (cam in camProduction.keys) setCAMProductionRate(cam, camProduction[cam]!!)
    }

    private fun see(o: Collidable) {
        if (cullFromRayCasting(o)) return
        rayStartTmp.set(pos!!)
        val interactRange = interactRange
        val dirAngle = dir.angle()
        for (cell in retina.cells) {
            val rays = cell.rays
            for (i in rays.indices) {
                rayEndTmp.set(rays[i])
                    .turn(dirAngle)
                    .setLength(interactRange)
                    .translate(rayStartTmp)
                o.rayCollisions(rayStartTmp, rayEndTmp, collisions)
                var sqLen = Float.MAX_VALUE
                for (collision in collisions) if (collision.collided) sqLen =
                    Math.min(sqLen, collision.point.squareDistanceTo(rayStartTmp))
                if (sqLen < cell.collisionSqLen(i)) cell[i, o.getColor()] = sqLen
            }
        }
    }

    private fun eat(e: EdibleCell?, delta: Float) {
        var extraction = 1f
        if (e is PlantCell) {
            if (spikes.size > 0) extraction *= Math.pow(
                Settings.spikePlantConsumptionPenalty.toDouble(),
                spikes.size.toDouble()
            ).toFloat()
            extraction *= herbivoreFactor
        } else if (e is MeatCell) {
            extraction /= herbivoreFactor
        }
        extractFood(e!!, extraction * delta)
    }

    private fun damage(damage: Float) {
        wasJustDamaged = true
        _health = _health - damage
    }

    private fun attack(p: Protozoan, spike: Spike, delta: Float) {
        val myAttack =
            (2 * _health + Settings.spikeDamage * getSpikeLength(spike) + 2 * Simulation.RANDOM.nextDouble()).toFloat()
        val theirDefense: Float = (2 * p._health + 0.3 * p.getRadius() + 2 * Simulation.RANDOM.nextDouble()).toFloat()
        if (myAttack > p.shieldFactor * theirDefense) p.damage(delta * attackFactor * (myAttack - p.shieldFactor * theirDefense))
    }

    private fun think(delta: Float) {
        brain.tick(this)
        dir.turn(delta * 80 * brain.turn(this))
        val spikeDecay = Math.pow(Settings.spikeMovementPenaltyFactor.toDouble(), spikes.size.toDouble()).toFloat()
        val sizePenalty: Float = getRadius() / splitRadius // smaller flagella generate less impulse
        val speed = Math.abs(brain.speed(this))
        val vel = dir.mul(sizePenalty * spikeDecay * speed)
        val work = .5f * _mass * vel.len2()
        if (enoughEnergyAvailable(work)) {
            useEnergy(work)
            pos!!.translate(vel.scale(delta))
        }
    }

    private fun shouldSplit(): Boolean {
        return getRadius() > splitRadius && _health > Settings.minHealthToSplit
    }

    @Throws(MiscarriageException::class)
    private fun createSplitChild(r: Float): Protozoan {
        val stuntingFactor: Float = r / getRadius()
        val child = genome.createChild(tank, crossOverGenome)
        child.setRadius(stuntingFactor * child.getRadius())
        return child
    }

    private fun interact(other: Collidable, delta: Float) {
        if (other === this) return
        if (isDead()) {
            handleDeath()
            return
        }
        if (retina.numberOfCells() > 0 && retina.health > 0) see(other)
        if (other is Cell) {
            interact(other, delta)
        }
    }

    private fun interact(other: Cell, delta: Float) {
        val d = other.pos!!.distanceTo(pos!!)
        if (d - other.getRadius() > interactRange) return
        if (shouldSplit()) {
            super.burst(Protozoan::class.java) { r: Float? -> createSplitChild(r!!) }
            return
        }
        val r: Float = getRadius() + other.getRadius()
        if (other is Protozoan) {
            val p = other
            for (spike in spikes) {
                val spikeLen = getSpikeLength(spike)
                if (d < r + spikeLen && spikeInContact(spike, spikeLen, p)) attack(p, spike, delta)
            }
        }
        if (0.95 * d < r) {
            if (other is Protozoan) {
                val p = other
                if (brain.wantToMateWith(p) && p.brain.wantToMateWith(this)) {
                    if (p !== mate) {
                        timeMating = 0f
                        mate = p
                    } else {
                        timeMating += delta
                        if (timeMating >= Settings.matingTime) crossOverGenome = p.genome
                    }
                }
            } else if (other.isEdible()) eat(other as EdibleCell, delta)
        }
    }

    private fun spikeInContact(spike: Spike, spikeLen: Float, other: Cell): Boolean {
        val spikeStartPos = dir.unit().rotate(spike.angle).setLength(getRadius()).translate(pos!!)
        val spikeEndPos = spikeStartPos.add(spikeStartPos.sub(pos!!).setLength(spikeLen))
        return other.pos!!.sub(spikeEndPos).len2() < other.getRadius() * other.getRadius()
    }

    val interactRange: Float
        get() = if (retina.numberOfCells() > 0 && retina.health > 0) Settings.protozoaInteractRange else getRadius() + 0.005f

    override fun handleInteractions(delta: Float) {
        super.handleInteractions(delta)
        wasJustDamaged = false
        retina.reset()
        val chunkManager = tank.chunkManager
        val entities = chunkManager
            .broadCollisionDetection(pos!!, interactRange)
        entities.forEachRemaining { e: Collidable? ->
            if (e != null) {
                interact(e, delta)
            }
        }
    }

    private fun breakIntoPellets() {
        burst(MeatCell::class.java) { r: Float? -> MeatCell(r!!, tank) }
    }

    override fun handleDeath() {
        if (!hasHandledDeath) {
            super.handleDeath()
            breakIntoPellets()
        }
    }


    override fun getStats(): MutableMap<String, Float?> {
        val stats = super.getStats()!!
        stats["Death Rate"] = 100 * deathRate
        stats["Split Radius"] = Settings.statsDistanceScalar * splitRadius
        stats["Max Turning"] = genome.maxTurn
        stats["Mutations"] = genome.numMutations.toFloat()
        stats["Genetic Size"] = Settings.statsDistanceScalar * genome.radius
        stats["Has Mated"] = if (crossOverGenome == null) 0f else 1f
        if (spikes.size > 0) stats["Num Spikes"] = spikes.size.toFloat()
        if (brain is NNBrain) {
            val nn = brain.network
            stats["Network Depth"] = nn.depth.toFloat()
            stats["Network Size"] = nn.size.toFloat()
        }
        if (retina.numberOfCells() > 0) {
            stats["Retina Cells"] = retina.numberOfCells().toFloat()
            stats["Retina FoV"] = Math.toDegrees(retina.fov.toDouble()).toFloat()
            stats["Retina Health"] = retina.health
        }
        stats["Herbivore Factor"] = herbivoreFactor
        return stats
    }

/*
    fun getGrowthRate(): Float {
        var localgrowthRate = super.growthRate
        if (getRadius() > splitRadius) localgrowthRate *= _health * splitRadius / (5 * getRadius())
        //		for (Spike spike : spikes)
//			growthRate -= Settings.spikeGrowthPenalty * spike.growthRate;
//		growthRate -= Settings.retinaCellGrowthCost * retina.numberOfCells();
        return localgrowthRate
    }
*/
    fun age(delta: Float) {
        deathRate = getRadius() * delta * Settings.protozoaStarvationFactor
        //		deathRate *= 0.75f + 0.25f * getSpeed();
        deathRate *= Settings.spikeDeathRatePenalty.pow(spikes.size).toFloat()
        _health -= deathRate
    }

    override fun update(delta: Float) {
        super.update(delta)
        age(delta)
        if (isDead()) handleDeath()
        think(delta)
        for (spike in spikes) spike.update(delta)
        for (contactSensor in contactSensors) contactSensor!!.reset()
        maintainRetina(delta)
    }

    private fun maintainRetina(delta: Float) {
        val availableRetinal = getComplexMoleculeAvailable(Food.ComplexMolecule.Retinal)
        val usedRetinal = retina.updateHealth(delta, availableRetinal)
        depleteComplexMolecule(Food.ComplexMolecule.Retinal, usedRetinal)
    }

    override fun isEdible(): Boolean {
        return false
    }

    fun getRetina(): Retina {
        return retina
    }

    fun setRetina(retina: Retina) {
        this.retina = retina
        cosHalfFov = Math.cos((retina.fov / 2f).toDouble()).toFloat()
    }

    fun getSpikeLength(spike: Spike): Float {
        return brain.attack(this) * spike.currentLength * getRadius() / splitRadius
    }

    val isHarbouringCrossover: Boolean
        get() = crossOverGenome != null

    companion object {
        private const val serialVersionUID = 2314292760446370751L
    }
}