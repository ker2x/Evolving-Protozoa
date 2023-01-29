package protoevo.biology

import protoevo.biology.CellAdhesion.*
import protoevo.biology.Food.ComplexMolecule
import protoevo.core.Particle
import protoevo.core.Settings
import protoevo.core.Simulation
import protoevo.env.Rock
import protoevo.env.Tank
import protoevo.utils.Geometry.getSphereVolume
import protoevo.utils.Vector2
import java.awt.Color
import java.io.Serializable
import java.util.*
import java.util.function.Consumer

abstract class Cell(tank: Tank?) : Particle(tank!!), Serializable {
    fun interface EntityBuilder<T, R> {
        @Throws(MiscarriageException::class)
        fun apply(t: T): R
    }

    @JvmField
	var healthyColour: Color
    private var fullyDegradedColour: Color? = null
    var generation = 1
    var _mass = -1f
    private var dead = false
    protected var hasHandledDeath = false
    private var timeAlive = 0f
    var _health = 1f
    open var growthRate = 0.0f
        get() = if (recentRigidCollisions > 2) 0f else field
    var energyAvailable = Settings.startingAvailableCellEnergy
    var constructionMassAvailable = 0f
        private set
    private var wasteMass = 0f
    private val availableComplexMolecules: MutableMap<ComplexMolecule, Float>
    val cellBindings: MutableCollection<CellBinding>
    private val toAttach: MutableCollection<CellBinding>
    private val surfaceCAMs: MutableMap<CellAdhesionMolecule, Float>
    private val foodDigestionRates: MutableMap<Food.Type, Float>
    private val foodToDigest: MutableMap<Food.Type, Food>
    private val constructionProjects: MutableCollection<ConstructionProject>
    private val complexMoleculeProductionRates: MutableMap<ComplexMolecule, Float>
    private val camProductionRates: MutableMap<CellAdhesionMolecule, Float>
    val children = ArrayList<Cell>()

    init {
        healthyColour = Color(255, 255, 255)
        foodDigestionRates = TreeMap()
        foodToDigest = TreeMap()
        cellBindings = ArrayList(10)
        toAttach = ArrayList(5)
        surfaceCAMs = HashMap(10)
        constructionProjects = ArrayList(10)
        complexMoleculeProductionRates = TreeMap()
        camProductionRates = HashMap(10)
        availableComplexMolecules = TreeMap()
    }

    open fun update(delta: Float) {
        _mass = computeMass()
        timeAlive += delta
        digest(delta)
        repair(delta)
        resourceProduction(delta)
        progressConstructionProjects(delta)
        if (!toAttach.isEmpty()) {
            cellBindings.addAll(toAttach)
            toAttach.clear()
        }
        cellBindings.removeIf { binding: CellBinding -> detachCondition(binding) }
        for (binding in cellBindings) handleBindingInteraction(binding, delta)
    }

    fun progressConstructionProjects(delta: Float) {
        for (project in constructionProjects) {
            if (project.notFinished() && project.canMakeProgress(
                    energyAvailable,
                    constructionMassAvailable,
                    availableComplexMolecules,
                    delta
                )
            ) {
                useEnergy(project.energyToMakeProgress(delta))
                useConstructionMass(project.massToMakeProgress(delta))
                if (project.requiresComplexMolecules()) for (molecule in project.requiredMolecules) {
                    val amountUsed = project.complexMoleculesToMakeProgress(delta, molecule)
                    depleteComplexMolecule(molecule, amountUsed)
                }
                project.progress(delta)
            }
        }
    }

    fun resourceProduction(delta: Float) {
        for (molecule in complexMoleculeProductionRates.keys) {
            val producedMass = delta * complexMoleculeProductionRates.getOrDefault(molecule, 0f)
            val requiredEnergy = molecule.productionCost * producedMass
            if (producedMass > 0 && constructionMassAvailable > producedMass && energyAvailable > requiredEnergy) {
                addAvailableComplexMolecule(molecule, producedMass)
                useConstructionMass(producedMass)
                useEnergy(requiredEnergy)
            }
        }
        for (cam in camProductionRates.keys) {
            val producedMass = delta * camProductionRates.getOrDefault(cam, 0f)
            val requiredEnergy = cam.productionCost * producedMass
            if (producedMass > 0 && constructionMassAvailable > producedMass && energyAvailable > requiredEnergy) {
                val currentAmount = surfaceCAMs.getOrDefault(cam, 0f)
                surfaceCAMs[cam] = currentAmount + producedMass
                useConstructionMass(producedMass)
                useEnergy(requiredEnergy)
            }
        }
    }

    fun getDigestionRate(foodType: Food.Type): Float {
        return foodDigestionRates.getOrDefault(foodType, 0f)
    }

    fun setDigestionRate(foodType: Food.Type, rate: Float) {
        foodDigestionRates[foodType] = rate
    }

    fun extractFood(cell: EdibleCell, extraction: Float) {
        val foodType = cell.foodType
        val extractedMass = cell._mass * extraction
        cell.removeMass(Settings.foodExtractionWasteMultiplier * extractedMass)
        cell._health = cell._health * (1 - 5f * extraction)
        val food = foodToDigest.getOrDefault(foodType, Food(extractedMass, foodType))
        food.addSimpleMass(extractedMass)
        for (molecule in cell.complexMolecules) {
            if (cell.getComplexMoleculeAvailable(molecule) > 0) {
                val extractedAmount = extraction * cell.getComplexMoleculeAvailable(molecule)
                cell.depleteComplexMolecule(molecule, extractedAmount)
                food.addComplexMoleculeMass(molecule, extractedMass)
            }
        }
        foodToDigest[foodType] = food
    }

    fun digest(delta: Float) {
        for (food in foodToDigest.values) {
            val rate = delta * 2f * getDigestionRate(food.type)
            if (food.simpleMass > 0) {
                val massExtracted = food.simpleMass * rate
                addConstructionMass(massExtracted)
                food.subtractSimpleMass(massExtracted)
                energyAvailable += food.getEnergy(massExtracted)
            }
            for (molecule in food.complexMolecules) {
                val amount = food.getComplexMoleculeMass(molecule)
                if (amount == 0f) continue
                val extracted = Math.min(amount, amount * rate)
                addAvailableComplexMolecule(molecule, extracted)
                food.subtractComplexMolecule(molecule, extracted)
            }
        }
    }

    fun repair(delta: Float) {
        if (!isDead() && getHealth() < 1f && growthRate > 0) {
            val massRequired = _mass * 0.01f * delta
            val energyRequired = massRequired * 3f
            if (massRequired < constructionMassAvailable && energyRequired < energyAvailable) {
                useEnergy(energyRequired)
                useConstructionMass(massRequired)
                setHealth(getHealth() + delta * Settings.cellRepairRate)
            }
        }
    }

    fun detachCondition(binding: CellBinding): Boolean {
        val e = binding.destinationEntity
        if (e.isDead()) return true
        val dist = e.pos!!.sub(pos!!).len()
        val maxDist: Float = 1.3f * (e.getRadius() + getRadius())
        val minDist: Float = 0.95f * (e.getRadius() + getRadius())
        return dist > maxDist || dist < minDist
    }

    override fun physicsStep(delta: Float) {
        for (binding in cellBindings) handleBindingConstraint(binding.destinationEntity)
        super.physicsStep(delta)
    }

    fun addConstructionProject(project: ConstructionProject) {
        constructionProjects.add(project)
    }

    open fun handleInteractions(delta: Float) {
        grow(delta)
    }

    fun grow(delta: Float) {
        val gr = growthRate
        val newR = super.getRadius() * (1 + gr * delta)
        val massChange = getMass(newR) - getMass(super.getRadius())
        if (massChange < constructionMassAvailable &&
            (newR > Settings.minPlantBirthRadius || gr > 0)
        ) {
            setRadius(newR)
            if (massChange > 0) useConstructionMass(massChange) else wasteMass -= massChange
        }
        if (java.lang.Float.isNaN(getRadius())) killCell()
    }

    @Synchronized
    fun attach(binding: CellBinding) {
        if (!cellBindings.contains(binding)) toAttach.add(binding)
    }
/*
    fun getCellBindings(): Collection<CellBinding> {
        return cellBindings
    }
*/
    fun getSurfaceCAMs(): Collection<CellAdhesionMolecule> {
        return surfaceCAMs.keys
    }

    open fun cannotMakeBinding(): Boolean {
        return false
    }

    override fun onParticleCollisionCallback(p: Particle?, delta: Float) {
        if (p is Cell) {
            val otherCell = p
            if (otherCell.cannotMakeBinding() || cannotMakeBinding()) return
            for (myCAM in getSurfaceCAMs()) {
                for (theirCAM in otherCell.getSurfaceCAMs()) {
                    // TODO: implement probabilistic CAM binding based on amounts
                    if (myCAM.bindsTo(theirCAM)) {
                        createNewBinding(myCAM, otherCell)
                        otherCell.createNewBinding(theirCAM, this)
                    }
                }
            }
        }
    }

    fun handleBindingInteraction(binding: CellBinding, delta: Float) {
        val junctionType = binding.cam.junctionType
        if (junctionType == CAMJunctionType.OCCLUDING) handleOcclusionBindingInteraction(
            binding,
            delta
        ) else if (junctionType == CAMJunctionType.CHANNEL_FORMING) handleChannelBindingInteraction(
            binding,
            delta
        ) else if (junctionType == CAMJunctionType.SIGNAL_RELAYING) handleSignallingBindingInteraction(binding, delta)
    }

    fun handleOcclusionBindingInteraction(binding: CellBinding?, delta: Float) {}
    fun handleChannelBindingInteraction(binding: CellBinding, delta: Float) {
        val other = binding.destinationEntity
        val transferRate = Settings.channelBindingEnergyTransport
        val massDelta = constructionMassAvailable - other.constructionMassAvailable
        val constructionMassTransfer = Math.abs(transferRate * massDelta * delta)
        if (massDelta > 0) {
            other.addConstructionMass(constructionMassTransfer)
            useConstructionMass(constructionMassTransfer)
        } else {
            addConstructionMass(constructionMassTransfer)
            other.useConstructionMass(constructionMassTransfer)
        }
        val energyDelta = energyAvailable - other.energyAvailable
        val energyTransfer = Math.abs(transferRate * energyDelta * delta)
        if (energyDelta > 0) {
            other.addAvailableEnergy(energyTransfer)
            useEnergy(energyTransfer)
        } else {
            addAvailableEnergy(energyTransfer)
            other.useEnergy(energyTransfer)
        }
        for (molecule in complexMolecules) handleComplexMoleculeTransport(other, molecule, delta)
        for (molecule in other.complexMolecules) other.handleComplexMoleculeTransport(this, molecule, delta)
    }

    private fun handleComplexMoleculeTransport(other: Cell, molecule: ComplexMolecule, delta: Float) {
        val massDelta = getComplexMoleculeAvailable(molecule) - other.getComplexMoleculeAvailable(molecule)
        val transferRate = Settings.occludingBindingEnergyTransport
        if (massDelta > 0) {
            val massTransfer = transferRate * massDelta * delta
            other.addAvailableComplexMolecule(molecule, massTransfer)
            depleteComplexMolecule(molecule, massTransfer)
        }
    }

    fun handleSignallingBindingInteraction(binding: CellBinding?, delta: Float) {}
    fun isAttached(e: Cell): Boolean {
        for (binding in cellBindings) if (binding.destinationEntity == e) return true
        return false
    }

    abstract fun isEdible(): Boolean
    fun createNewBinding(cam: CellAdhesionMolecule?, e: Cell?) {
        attach(CellBinding(this, e, cam))
    }

    fun setHealth(h: Float) {
        _health = h
        if (_health > 1) _health = 1f
        if (_health < 0.05) killCell()
    }

    open fun handleDeath() {
        hasHandledDeath = true
    }

    override fun handlePotentialCollision(rock: Rock, delta: Float): Boolean {
        if (rock.pointInside(pos)) {
            killCell()
            return true
        }
        return super.handlePotentialCollision(rock, delta)
    }

    abstract val prettyName: String?
    open fun getStats(): MutableMap<String, Float?>?
    {
            val stats = TreeMap<String, Float?>()
            stats["Age"] = 100 * timeAlive
            stats["Health"] = 100 * getHealth()
            stats["Size"] = Settings.statsDistanceScalar * getRadius()
            stats["Speed"] = Settings.statsDistanceScalar * speed
            stats["Generation"] = generation.toFloat()
            val energyScalar = Settings.statsMassScalar * Settings.statsDistanceScalar * Settings.statsDistanceScalar
            stats["Available Energy"] = energyScalar * energyAvailable
            stats["Total Mass"] = Settings.statsMassScalar * _mass
            stats["Construction Mass"] = Settings.statsMassScalar * constructionMassAvailable
            if (wasteMass > 0) stats["Waste Mass"] = Settings.statsDistanceScalar * wasteMass
            val gr = growthRate
            stats["Growth Rate"] = Settings.statsDistanceScalar * gr
            for (molecule in availableComplexMolecules.keys) if (availableComplexMolecules[molecule]!! > 0) stats["$molecule Available"] =
                availableComplexMolecules[molecule]
            if (cellBindings.size > 0) stats["Num Cell Bindings"] = cellBindings.size.toFloat()
            for (junctionType in CAMJunctionType.values()) {
//			int count = 0;
//			for (CellAdhesion.CellBinding binding : cellBindings)
//				if (binding.getJunctionType().equals(junctionType))
//					count++;
//			if (count > 0)
//				stats.put(junctionType + " Bindings", (float) count);
                var camMass = 0f
                for (molecule in surfaceCAMs.keys) if (molecule.junctionType == junctionType) camMass += surfaceCAMs[molecule]!!
                if (camMass > 0) stats["$junctionType CAM Mass"] = camMass
            }
            val massTimeScalar = Settings.statsMassScalar / Settings.statsTimeScalar
            for (molecule in complexMoleculeProductionRates.keys) if (complexMoleculeProductionRates[molecule]!! > 0) stats["$molecule Production"] =
                massTimeScalar * complexMoleculeProductionRates[molecule]!!
            for (molecule in availableComplexMolecules.keys) if (availableComplexMolecules[molecule]!! > 0) stats["$molecule Available"] =
                100f * Settings.statsMassScalar * availableComplexMolecules[molecule]!!
            for (foodType in foodDigestionRates.keys) if (foodDigestionRates[foodType]!! > 0) stats["$foodType Digestion Rate"] =
                massTimeScalar * foodDigestionRates[foodType]!!
            for (food in foodToDigest.values) stats["$food to Digest"] = Settings.statsMassScalar * food.simpleMass
            return stats
        }
    val debugStats: Map<String, Float>
        get() {
            val stats = TreeMap<String, Float>()
            stats["Position X"] = Settings.statsDistanceScalar * pos!!.x
            stats["Position Y"] = Settings.statsDistanceScalar * pos!!.y
            return stats
        }

    fun getHealth(): Float {
        return _health
    }

    fun isDead(): Boolean {
        return dead || _health < 0.05f
    }

    fun killCell() {
        dead = true
        _health = 0f
    }

    override fun getColor(): Color? {
        val healthyColour = healthyColour
        val degradedColour = getFullyDegradedColour()
        return Color(
            (healthyColour.red + (1 - getHealth()) * (degradedColour.red - healthyColour.red)).toInt(),
            (healthyColour.green + (1 - getHealth()) * (degradedColour.green - healthyColour.green)).toInt(),
            (healthyColour.blue + (1 - getHealth()) * (degradedColour.blue - healthyColour.blue)).toInt()
        )
    }

    fun setDegradedColour(fullyDegradedColour: Color?) {
        this.fullyDegradedColour = fullyDegradedColour
    }

    fun getFullyDegradedColour(): Color {
        if (fullyDegradedColour == null) {
            val healthyColour = healthyColour
            val r = healthyColour.red
            val g = healthyColour.green
            val b = healthyColour.blue
            val p = 0.7f
            return Color((r * p).toInt(), (g * p).toInt(), (b * p).toInt())
        }
        return fullyDegradedColour as Color
    }

    open fun burstMultiplier(): Int {
        return 20
    }

    fun <T : Cell> burst(type: Class<T>?, createChild: EntityBuilder<Float?, T>) {
        killCell()
        hasHandledDeath = true
        var angle = (2 * Math.PI * Simulation.RANDOM.nextDouble()).toFloat()
        val maxChildren = (burstMultiplier() * getRadius() / Settings.maxParticleRadius).toInt()
        val nChildren = if (maxChildren <= 1) 2 else 2 + Simulation.RANDOM.nextInt(maxChildren)

        // Tank tank = tank;	// TODO: fix this
        for (i in 0 until nChildren) {
            val dir = Vector2(Math.cos(angle.toDouble()).toFloat(), Math.sin(angle.toDouble()).toFloat())
            val p = (0.3 + 0.7 * Simulation.RANDOM.nextDouble() / nChildren).toFloat()
            val nEntities: Int = tank.cellCounts.getOrDefault(type!!, 0)
            val maxEntities: Int = tank.cellCapacities.getOrDefault(type!!, 0)
            if (nEntities > maxEntities) return
            try {
                val child = createChild.apply(getRadius() * p)
                child!!.pos = pos!!.add(dir.mul(2 * child.getRadius()))
                child.generation = generation + 1
                allocateChildResources(child, p)
                for (otherChild in children) child.handlePotentialCollision(otherChild, 0f)
                children.add(child)
            } catch (ignored: MiscarriageException) {
            }
            angle += (2 * Math.PI / nChildren).toFloat()
        }
        for (j in 0..7) for (child1 in children) for (child2 in children) child1.handlePotentialCollision(child2, 0f)
        children.forEach(Consumer { e: Cell? -> tank.add(e!!) })
    }

    private fun allocateChildResources(child: Cell, p: Float) {
        child.setAvailableConstructionMass(constructionMassAvailable * p)
        child.energyAvailable = energyAvailable * p
        for (molecule in availableComplexMolecules.keys) child.setComplexMoleculeAvailable(
            molecule,
            p * getComplexMoleculeAvailable(molecule)
        )
        for (cam in getSurfaceCAMs()) child.setCAMAvailable(cam, p * getCAMAvailable(cam))
        for (foodType in foodToDigest.keys) {
            val oldFood = foodToDigest[foodType]
            val newFood = Food(p * oldFood!!.simpleMass, foodType)
            for (molecule in oldFood.complexMolecules) {
                val moleculeAmount = p * oldFood.getComplexMoleculeMass(molecule)
                newFood.addComplexMoleculeMass(molecule, moleculeAmount)
            }
            child.setFoodToDigest(foodType, newFood)
        }
    }

    fun setFoodToDigest(foodType: Food.Type, food: Food) {
        foodToDigest[foodType] = food
    }

    fun getChildren(): Collection<Cell> {
        return children
    }

    fun getCAMAvailable(cam: CellAdhesionMolecule): Float {
        return surfaceCAMs.getOrDefault(cam, 0f)
    }

    fun setCAMAvailable(cam: CellAdhesionMolecule, amount: Float) {
        surfaceCAMs[cam] = amount
    }

    fun enoughEnergyAvailable(work: Float): Boolean {
        return work < energyAvailable
    }

    fun addAvailableEnergy(energy: Float) {
        energyAvailable = Math.min(energyAvailable + energy, availableEnergyCap)
    }

    private val availableEnergyCap: Float
        private get() = Settings.startingAvailableCellEnergy * getRadius() / Settings.minParticleRadius

    fun useEnergy(energy: Float) {
        energyAvailable = Math.max(0f, energyAvailable - energy)
    }

    val complexMolecules: Collection<ComplexMolecule>
        get() = availableComplexMolecules.keys

    fun depleteComplexMolecule(molecule: ComplexMolecule, amount: Float) {
        val currAmount = getComplexMoleculeAvailable(molecule)
        setComplexMoleculeAvailable(molecule, currAmount - amount)
    }

    fun getComplexMoleculeAvailable(molecule: ComplexMolecule): Float {
        return availableComplexMolecules.getOrDefault(molecule, 0f)
    }

    private fun addAvailableComplexMolecule(molecule: ComplexMolecule, amount: Float) {
        val currentAmount = availableComplexMolecules.getOrDefault(molecule, 0f)
        val newAmount = Math.min(complexMoleculeMassCap, currentAmount + amount)
        availableComplexMolecules[molecule] = newAmount
        _mass = computeMass()
    }

    private val complexMoleculeMassCap: Float
        private get() = getMass(getRadius() * 0.1f)

    fun setComplexMoleculeAvailable(molecule: ComplexMolecule, amount: Float) {
        availableComplexMolecules[molecule] = Math.max(0f, amount)
        _mass = computeMass()
    }

    val constructionMassCap: Float
        get() = 2 * massDensity * getSphereVolume(getRadius() * 0.25f)

    fun setAvailableConstructionMass(mass: Float) {
        constructionMassAvailable = Math.min(mass, constructionMassCap)
        this._mass = computeMass()
    }

    fun addConstructionMass(mass: Float) {
        setAvailableConstructionMass(constructionMassAvailable + mass)
    }

    fun useConstructionMass(mass: Float) {
        constructionMassAvailable = Math.max(0f, constructionMassAvailable - mass)
    }

    fun setComplexMoleculeProductionRate(molecule: ComplexMolecule, rate: Float) {
        complexMoleculeProductionRates[molecule] = rate
    }

    fun setCAMProductionRate(cam: CellAdhesionMolecule, rate: Float) {
        camProductionRates[cam] = rate
    }

    override fun getMass(): Float {
        if (_mass < 0) _mass = computeMass()
        return _mass
    }

    fun computeMass(): Float {
        var extraMass = constructionMassAvailable + wasteMass
        for (m in availableComplexMolecules.values) extraMass += m
        return getMass(getRadius(), extraMass)
    }

    /**
     * Changes the radius of the cell to remove the given amount of mass
     * @param mass mass to remove
     */
    fun removeMass(mass: Float) {
        val x = 3 * mass / (4 * massDensity * Math.PI)
        val r = getRadius()
        val newR = Math.pow(r * r * r - x, 1 / 3.0).toFloat()
        if (newR < Settings.minParticleRadius * 0.9f) killCell()
        setRadius(newR)
    }

    companion object {
        private const val serialVersionUID = -4333766895269415282L
    }
}