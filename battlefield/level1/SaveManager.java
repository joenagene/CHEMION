package battlefield.level1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** One local save slot for CHEMION. Does not depend on the working directory. */
public final class SaveManager {
    private static final Path SAVE_FILE = Paths.get(
        System.getProperty("user.home"), ".chemion-the-last-cure", "save.properties"
    );

    private SaveManager() { }

    public static Path saveFile() { return SAVE_FILE; }

    public static synchronized Properties load() {
        Properties data = new Properties();
        if (Files.isRegularFile(SAVE_FILE)) {
            try (InputStream in = Files.newInputStream(SAVE_FILE)) {
                data.load(in);
            } catch (IOException e) {
                System.err.println("CHEMION: Could not read save: " + e.getMessage());
            }
        }
        return data;
    }

    /** Write via a temporary file to avoid corrupting an existing save. */
    public static synchronized void save(Properties data) throws IOException {
        Files.createDirectories(SAVE_FILE.getParent());
        Path temporary = Files.createTempFile(SAVE_FILE.getParent(), "chemion-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporary)) {
                data.store(out, "CHEMION: The Last Cure - saved game");
            }
            try {
                Files.move(temporary, SAVE_FILE,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, SAVE_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static synchronized void clear() throws IOException {
        Files.deleteIfExists(SAVE_FILE);
    }

    /** True only for a resumable run, not a completed level. */
    public static boolean hasSavedLevel1() {
        Properties p = load();
        return "true".equals(p.getProperty("level1.started"))
            && !"true".equals(p.getProperty("level1.won"))
            && p.getProperty("scientist") != null;
    }

    public static boolean hasAnyProgress() {
        Properties p = load();
        return "true".equals(p.getProperty("level1.started"))
            || "true".equals(p.getProperty("level.1.completed"))
            || highestUnlockedLevel() > 1;
    }

    /** Replay an unlocked level without losing completed-level achievements. */
    public static synchronized void clearLevel1KeepUnlocks() throws IOException {
        Properties p = load();
        for (String key : new java.util.HashSet<String>(p.stringPropertyNames())) {
            if (key.startsWith("level1.") || key.equals("scientist")) {
                p.remove(key);
            }
        }
        save(p);
    }

    public static String savedScientist() {
        return load().getProperty("scientist", "");
    }

    public static boolean isCompleted(int level) {
        return level >= 1 && level <= 5
            && "true".equals(load().getProperty("level." + level + ".completed"));
    }

    /** Completing a level unlocks the next; level 1 is always available. */
    public static int highestUnlockedLevel() {
        Properties p = load();
        int highest = 1;
        for (int level = 1; level < 5; level++) {
            if (!"true".equals(p.getProperty("level." + level + ".completed"))) break;
            highest = level + 1;
        }
        return highest;
    }

    public static synchronized void markCompleted(int level) throws IOException {
        if (level < 1 || level > 5) throw new IllegalArgumentException("Invalid level: " + level);
        Properties p = load();
        p.setProperty("level." + level + ".completed", "true");
        save(p);
    }
}
