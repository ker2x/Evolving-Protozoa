package core;

import java.awt.Graphics;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import biology.*;
import utils.FileIO;
import utils.Vector2;

public class Tank implements Iterable<Entity>, Serializable
{
	private static final long serialVersionUID = 2804817237950199223L;
	private final float radius = Settings.tankRadius;
	private float elapsedTime;
	private final ConcurrentHashMap<Class<? extends Entity>, Integer> entityCounts;
	private final ChunkManager chunkManager;
	private int generation = 1;
	private int protozoaBorn = 0;
	private int totalEntitiesAdded = 0;

	private String genomeFile = null;
	private final List<String> genomesToWrite = new ArrayList<>();

	private final List<Entity> entitiesToAdd = new ArrayList<>();

	public Tank() 
	{
		float chunkSize = 2 * radius / Settings.numChunkBreaks;
		chunkManager = new ChunkManager(-radius, radius, -radius, radius, chunkSize);
		entityCounts = new ConcurrentHashMap<>();
		elapsedTime = 0;
	}
	
	public Vector2 randomPosition(float entityRadius) {
		float rad = radius - 2*entityRadius;
		float t = (float) (2 * Math.PI * Simulation.RANDOM.nextDouble());
		float r = (float) Simulation.RANDOM.nextDouble();
		return new Vector2(
				(float) (rad * (1 - r*r) * Math.cos(t)),
				(float) (rad * (1 - r*r) * Math.sin(t))
		);
	}

	public void handleTankEdge(Entity e) {
		if (e.getPos().len() - e.getRadius() > radius)
			e.setPos(e.getPos().setLength(-0.98f * radius));
	}

	public void updateEntity(Entity e, float delta) {

		if (e instanceof Protozoa)
			((Protozoa) e).handleInteractions(delta);

		e.update(delta);
		e.handleCollisions();

		handleTankEdge(e);
	}

	public void update(float delta) 
	{
		elapsedTime += delta;

		entitiesToAdd.forEach(chunkManager::add);
		entitiesToAdd.clear();
		chunkManager.update();

		Collection<Entity> entities = chunkManager.getAllEntities();
		entities.forEach(e -> updateEntity(e, delta));
		entities.forEach(this::handleDeadEntities);

	}

	private void handleDeadEntities(Entity e) {
		if (!e.isDead())
			return;

		entityCounts.put(e.getClass(), -1 + entityCounts.get(e.getClass()));
		e.handleDeath();
	}

	private void handleNewProtozoa(Protozoa p) {
		protozoaBorn++;
		generation = Math.max(generation, p.getGeneration());

		if (genomeFile != null && Settings.writeGenomes) {
			String genomeStr = p.getGenome().toString();
			String genomeLine = "generation=" + p.getGeneration() + "," + genomeStr;
			genomesToWrite.add(genomeLine);
			List<String> genomeWritesHandled = new ArrayList<>();
			for (String line : genomesToWrite) {
				FileIO.appendLine(genomeFile, line);
				genomeWritesHandled.add(line);
			}

			genomesToWrite.removeAll(genomeWritesHandled);
		}
	}

	public void add(Entity e) {
		if (e instanceof PlantPellet &&
				entityCounts.getOrDefault(PlantPellet.class, 0) >= Settings.maxPlants)
			return;
		if (e instanceof Protozoa &&
				entityCounts.getOrDefault(Protozoa.class, 0) >= Settings.maxProtozoa)
			return;
		if (e instanceof MeatPellet &&
				entityCounts.getOrDefault(MeatPellet.class, 0) >= Settings.maxMeat)
			return;

		totalEntitiesAdded++;
		entitiesToAdd.add(e);

		if (e instanceof Protozoa)
			handleNewProtozoa((Protozoa) e);

		if (!entityCounts.containsKey(e.getClass()))
			entityCounts.put(e.getClass(), 1);
		else
			entityCounts.put(e.getClass(), 1 + entityCounts.get(e.getClass()));
	}

	public Collection<Entity> getEntities() {
		return chunkManager.getAllEntities();
	}

	public Map<String, Float> getStats() {
		Map<String, Float> stats = new HashMap<>();
		stats.put("Number of Protozoa", (float) numberOfProtozoa());
		stats.put("Number of Plant Pellets", (float) entityCounts.getOrDefault(PlantPellet.class, 0));
		stats.put("Number of Meat Pellets", (float) entityCounts.getOrDefault(MeatPellet.class, 0));
		stats.put("Max Generation", (float) generation);
		stats.put("Time Elapsed", elapsedTime);
		stats.put("Protozoa Born", (float) protozoaBorn);
		stats.put("Total Entities Born", (float) totalEntitiesAdded);

		Collection<Entity> entities = chunkManager.getAllEntities();
		float n = (float) entities.size();
		for (Entity e : entities) {
			if (e instanceof Protozoa) {
				for (Map.Entry<String, Float> stat : e.getStats().entrySet()) {
					String key = "Mean " + stat.getKey();
					float currentValue = stats.getOrDefault(key, 0.0f);
					stats.put(key, stat.getValue() / n + currentValue);
				}
			}
		}

		return stats;
	}
	
	public int numberOfProtozoa() {
		return entityCounts.getOrDefault(Protozoa.class, 0);
	}
	
	public int numberOfPellets() {
		int nPellets = entityCounts.getOrDefault(PlantPellet.class, 0);
		nPellets += entityCounts.getOrDefault(MeatPellet.class, 0);
		return nPellets;
	}

	public ChunkManager getChunkManager() {
		return chunkManager;
	}

	@Override
	public Iterator<Entity> iterator() {
		return chunkManager.getAllEntities().iterator();
	}

	public float getRadius() {
		return radius;
	}

	public int getGeneration() {
		return generation;
	}

    public void addRandom(Entity e) {
		e.setPos(randomPosition(e.getRadius()));
		for (int i = 0; i < 5 && chunkManager.getAllEntities().stream().anyMatch(e::isCollidingWith); i++)
			e.setPos(randomPosition(e.getRadius()));
		if (chunkManager.getAllEntities().stream().noneMatch(e::isCollidingWith))
			add(e);
    }

	public float getElapsedTime() {
		return elapsedTime;
	}

	public void setGenomeFile(String genomeFile) {
		this.genomeFile = genomeFile;
	}
}