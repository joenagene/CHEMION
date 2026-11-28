package game;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameManager {
    @FunctionalInterface public interface SaveAction { void run() throws IOException; }
    private static final GameManager INSTANCE = new GameManager();
    private final Map<Integer, GameLevel> levels = new LinkedHashMap<>();
    private int currentLevel = 1;
    private Scientist player;
    private String gameState = "MENU";
    private GameManager() {
        levels.put(1, new GameLevel(1, "Abandoned Town",
            Arrays.asList("Common Zombie", "Runner Zombie", "Crawler Zombie", "Mutated Survivor"), "Stabilize Mutated Survivor"));
        levels.put(2, new GameLevel(2, "Contaminated Hospital",
            Arrays.asList("Acid Zombie", "Patient Zero", "Toxic Zombie", "Plague Doctor"), "Stabilize Plague Doctor"));
        levels.put(3, new GameLevel(3, "Toxic Chemical Factory", Arrays.asList("Toxic Brute", "Venom Spitter", "Explosive Zombie", "Chemical Abomination"), "Complete Level 3 boss"));
        levels.put(4, new GameLevel(4, "Lost Research Facility", Arrays.asList("Experiment 01", "Mutant Stalker", "Project Chimera", "Regenerator Zombie"), "Complete Level 4 boss"));
        levels.put(5, new GameLevel(5, "Genesis Laboratory", Arrays.asList("Elite Infected", "Biohazard Guardian", "Alpha Mutant", "Patient Zero - The Origin"), "Complete Final Cure"));
    }
    public static GameManager getInstance() { return INSTANCE; }
    public void resetProgress() {
        for (GameLevel level : levels.values()) level.resetProgress();
        currentLevel = 1;
        player = null;
        gameState = "MENU";
    }
    public void startGame(Scientist scientist) {
        if (scientist == null) throw new IllegalArgumentException("Select a scientist");
        player = scientist; gameState = "PLAYING"; currentLevel = 1;
        levels.get(1).startLevel();
    }
    public void changeLevel(int levelNumber) {
        GameLevel level = getLevel(levelNumber);
        level.startLevel();
        currentLevel = levelNumber;
        if (player != null) player.setLevel(levelNumber);
        gameState = "PLAYING";
    }
    public void restoreUnlocks(int highestUnlocked) {
        for (int i = 1; i <= Math.min(5, Math.max(1, highestUnlocked)); i++) levels.get(i).unlockLevel();
    }
    public void completeCurrentLevel() {
        GameLevel level = getLevel(currentLevel);
        String objective = level.getObjectives().iterator().next();
        level.completeObjective(objective);
        if (level.isComplete() && currentLevel < 5) levels.get(currentLevel + 1).unlockLevel();
        gameState = "LEVEL_COMPLETE";
    }
    public void saveProgress(SaveAction persistence) throws IOException {
        if (persistence == null) throw new IllegalArgumentException("Save action required");
        persistence.run();
    }
    public void endGame() { gameState = "MENU"; }
    public int getCurrentLevel() { return currentLevel; }
    public GameLevel getLevel(int level) {
        GameLevel result = levels.get(level);
        if (result == null) throw new IllegalArgumentException("Unknown level " + level);
        return result;
    }
    public Scientist getPlayer() { return player; }
    public String getGameState() { return gameState; }
}
