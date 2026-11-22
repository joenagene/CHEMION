package game;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Proposal: levelNumber, map, objectives, enemies, unlocked. */
public class GameLevel {
    private final int levelNumber;
    private final String map;
    private final Set<String> objectives = new LinkedHashSet<>();
    private final Set<String> completedObjectives = new LinkedHashSet<>();
    private final List<String> enemies;
    private boolean unlocked, started;
    public GameLevel(int levelNumber, String map, List<String> enemies, String... objectives) {
        if (levelNumber < 1 || levelNumber > 5) throw new IllegalArgumentException("Level must be 1..5");
        this.levelNumber = levelNumber;
        this.map = Inventory.requireName(map);
        this.enemies = Collections.unmodifiableList(Arrays.asList(enemies.toArray(new String[0])));
        this.objectives.addAll(Arrays.asList(objectives));
        this.unlocked = levelNumber == 1;
    }
    public int getLevelNumber() { return levelNumber; }
    public String getMap() { return map; }
    public List<String> getEnemies() { return enemies; }
    public Set<String> getObjectives() { return Collections.unmodifiableSet(objectives); }
    public boolean isUnlocked() { return unlocked; }
    public boolean isStarted() { return started; }
    public boolean isComplete() { return completedObjectives.containsAll(objectives); }
    public void startLevel() {
        if (!unlocked) throw new IllegalStateException("Level is locked");
        started = true;
    }
    public boolean completeObjective(String objective) {
        if (!objectives.contains(objective)) return false;
        completedObjectives.add(objective);
        return true;
    }
    public void unlockLevel() { unlocked = true; }
    public void resetProgress() {
        unlocked = levelNumber == 1;
        started = false;
        completedObjectives.clear();
    }
}
