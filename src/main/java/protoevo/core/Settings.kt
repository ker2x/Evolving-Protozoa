package protoevo.core

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class Settings {
    var simulation_seed: Long = 0

    // World parameters
    var tank_radius = 0f
    var fluid_resistance_multiplier = 0f
    var num_rock_ring_clusters = 0
    var max_rock_size = 0f
    var min_rock_size = 0f
    var rock_clustering = 0f
    var min_rock_opening_size = 0f

    // Initial Conditions
    var num_initial_protozoa = 0
    var num_initial_plants = 0
    var num_initial_pop_centres = 0
    var pop_cluster_radius = 0f

    // Simulation parameters
    var global_mutation_chance = 0f
    var plant_energy_density = 0f
    var meat_energy_density = 0f
    var enable_chemical_field = false
    var plant_regen = 0f
    var spike_damage = 0f
    var spike_plant_consumption_penalty = 0f
    var max_particle_radius = 0f
    var chemicals_decay = 0f
    var chemicals_flow = 0f
    var pheromones_deposit = 0f
    var protozoa_starvation_rate = 0f
    var starting_retina_size = 0
    var max_retina_size = 0
    var retina_grow_cost = 0f
    var min_health_to_split = 0f
    var max_protozoa_growth_rate = 0f
    var max_plant_growth = 0f
    var retina_growth_cost = 0f
    var cell_repair_rate = 0f
    var food_waste_multiplier = 0f
    var cam_energy_cost = 0f

    // Performance parameters
    var physics_substeps = 0
    var spatial_hash_resolution = 0
    var chemical_field_resolution = 0
    var chemical_update_interval = 0
    var max_interact_range = 0f
    var max_protozoa = 0
    var max_plants = 0
    var max_meat = 0

    companion object {
        fun loadSettingsYAML(): Settings {
            val inputStream: InputStream
            inputStream = try {
                println("Loading settings from " + Simulation.settingsPath)
                FileInputStream(Simulation.settingsPath)
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }
            val yaml = Yaml(
                Constructor(
                    Settings::class.java
                )
            )
            return yaml.load(inputStream)
        }

        private var instance: Settings? = null
        private fun getInstance(): Settings? {
            if (instance == null) {
                instance = loadSettingsYAML()
            }
            return instance
        }

        // Simulation settings
        val simulationSeed =
            if (getInstance()!!.simulation_seed == 0L) System.currentTimeMillis() else getInstance()!!.simulation_seed
        const val simulationUpdateDelta = 5f / 1000f
        const val maxProtozoaSpeed = .01f
        const val maxParticleSpeed = 1e-4f
        const val timeBetweenSaves = 2000.0f
        const val historySnapshotTime = 2.0f
        const val writeGenomes = true
        const val finishOnProtozoaExtinction = true
        val physicsSubSteps = getInstance()!!.physics_substeps
        const val numPossibleCAMs = 64
        @JvmField
        val camProductionEnergyCost = getInstance()!!.cam_energy_cost
        const val startingAvailableCellEnergy = 0.01f
        @JvmField
        val foodExtractionWasteMultiplier = getInstance()!!.food_waste_multiplier
        @JvmField
        val cellRepairRate = getInstance()!!.cell_repair_rate
        const val occludingBindingEnergyTransport = 0.5f
        const val channelBindingEnergyTransport = 0.5f
        const val enableAnchoringBinding = false
        const val enableOccludingBinding = false
        const val enableChannelFormingBinding = true
        const val enableSignalRelayBinding = false
        val maxPlants = getInstance()!!.max_plants
        val maxProtozoa = getInstance()!!.max_protozoa
        val maxMeat = getInstance()!!.max_meat
        @JvmField
        val plantEnergyDensity = getInstance()!!.plant_energy_density
        @JvmField
        val meatEnergyDensity = getInstance()!!.meat_energy_density

        // Tank settings
        @JvmField
        val numInitialProtozoa = getInstance()!!.num_initial_protozoa
        @JvmField
        val numInitialPlantPellets = getInstance()!!.num_initial_plants
        const val initialPopulationClustering = true
        @JvmField
        val numRingClusters = getInstance()!!.num_rock_ring_clusters
        @JvmField
        val numPopulationClusters = getInstance()!!.num_initial_pop_centres
        @JvmField
        val populationClusterRadius = getInstance()!!.pop_cluster_radius
        const val populationClusterRadiusRange = 0f
        @JvmField
        val tankRadius = getInstance()!!.tank_radius
        const val sphericalTank = false
        @JvmField
        val numChunkBreaks = getInstance()!!.spatial_hash_resolution
        @JvmField
        val maxParticleRadius = getInstance()!!.max_particle_radius
        const val minParticleRadius = 0.005f
        val tankFluidResistance = 8e-4f * getInstance()!!.fluid_resistance_multiplier
        const val brownianFactor = 1000f
        const val coefRestitution = 0.005f
        @JvmField
        val maxRockSize = getInstance()!!.max_rock_size
        @JvmField
        val minRockSize = getInstance()!!.min_rock_size
        @JvmField
        val minRockSpikiness = Math.toRadians(45.0).toFloat()
        @JvmField
        val minRockOpeningSize = getInstance()!!.min_rock_opening_size
        const val rockGenerationIterations = 2000
        const val rockSeedingIterations = 0
        @JvmField
        val rockClustering = getInstance()!!.rock_clustering

        // Chemical settings
        @JvmField
        val enableChemicalField = getInstance()!!.enable_chemical_field
        @JvmField
        val numChemicalBreaks = getInstance()!!.chemical_field_resolution
        @JvmField
        val chemicalsUpdateTime = simulationUpdateDelta * getInstance()!!.chemical_update_interval
        @JvmField
        val chemicalsDecay = getInstance()!!.chemicals_decay
        @JvmField
        val chemicalsFlow = getInstance()!!.chemicals_flow
        @JvmField
        val plantPheromoneDeposit = getInstance()!!.pheromones_deposit

        // Protozoa settings
        const val minProtozoanBirthRadius = 0.01f
        const val maxProtozoanBirthRadius = 0.015f
        val protozoaStarvationFactor = getInstance()!!.protozoa_starvation_rate
        @JvmField
        val defaultRetinaSize = getInstance()!!.starting_retina_size
        @JvmField
        val maxRetinaSize = getInstance()!!.max_retina_size
        val retinaCellGrowthCost = getInstance()!!.retina_growth_cost
        const val numContactSensors = 0
        @JvmField
        val minRetinaRayAngle = Math.toRadians(10.0).toFloat()
        @JvmField
        val minHealthToSplit = getInstance()!!.min_health_to_split
        const val maxProtozoanSplitRadius = 0.03f
        const val minProtozoanSplitRadius = 0.015f
        const val minProtozoanGrowthRate = .05f
        @JvmField
        val maxProtozoanGrowthRate = getInstance()!!.max_protozoa_growth_rate
        const val maxTurnAngle = 25
        const val spikeGrowthPenalty = .08f
        const val spikeMovementPenaltyFactor = 0.97f
        val spikePlantConsumptionPenalty = getInstance()!!.spike_plant_consumption_penalty
        const val spikeDeathRatePenalty = 1.015f
        const val maxSpikeGrowth = 0.1f
        val spikeDamage = getInstance()!!.spike_damage
        const val matingTime = 0.1f
        @JvmField
        val globalMutationChance = getInstance()!!.global_mutation_chance
        @JvmField
        val protozoaInteractRange = getInstance()!!.max_interact_range
        const val eatingConversionRatio = 0.75f

        // Plant Settings
        const val minMaxPlantRadius = 0.015f
        const val minPlantSplitRadius = 0.01f
        const val minPlantBirthRadius = 0.005f
        const val maxPlantBirthRadius = 0.03f
        const val minPlantGrowth = 0.01f
        @JvmField
        val plantGrowthRange = getInstance()!!.max_plant_growth - minPlantGrowth
        const val plantCrowdingGrowthDecay = 1.0f
        const val plantCriticalCrowding = 6.0f
        @JvmField
        val plantRegen = getInstance()!!.plant_regen

        // Stats
        const val statsDistanceScalar = 100.0f
        const val statsTimeScalar = 100.0f
        const val statsMassScalar = 1000f

        // Rendering
        const val showFPS = false
        const val antiAliasing = true
    }
}