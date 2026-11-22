package battlefield.level2;

import battlefield.level1.SaveManager;
import game.GameplayBridge;
import game.GameManager;
import game.Reward;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * CHEMION Level 2 - Contaminated Hospital.
 * A playable side-scrolling level with four infected types, chemistry lab,
 * research journal, shop, save/resume, and boss unlock progression.
 * Existing character and zombie PNG sprites are used when present; all
 * environments and missing-sprite fallbacks are drawn in Java.
 */
public final class Level2GamePanel extends JPanel {
    private static final int W = 960, H = 540, WORLD = 3160;
    private static final int FLOOR_TOP = 216, FLOOR_BOTTOM = 448;
    private static final Color WHITE = new Color(239, 233, 253);
    private static final Color PURPLE = new Color(202, 165, 249);
    private static final Color MUTED = new Color(174, 163, 194);
    private static final Color TEAL = new Color(114, 214, 200);
    private static final Color GOLD = new Color(244, 197, 120);
    private static final Color RED = new Color(242, 123, 149);
    private static final Color[] VIAL_COLORS = {
        new Color(110, 216, 157), new Color(127, 187, 243),
        new Color(206, 159, 241), new Color(244, 185, 116)
    };
    private static final String[] ENEMY_IDS = {
        "acid_zombie", "patient_zero", "toxic_zombie", "plague_doctor"
    };
    private static final String[] NAMES = {
        "Acid Zombie", "Patient Zero", "Toxic Zombie", "Plague Doctor"
    };
    private static final String[] SHORT_NAMES = {"ACID", "CASE ZERO", "TOXIC", "DOCTOR"};
    // Same four-slot dimensions and placement as Level 1.
    private static final int HOTBAR_X = 22, HOTBAR_Y = 468, HOTBAR_SLOT_W = 126;
    private static final int HOTBAR_SLOT_H = 51, HOTBAR_GAP = 6;
    private static final String[] CHEMS = {
        "Calcium Carbonate", "Distilled Water", "Disodium EDTA", "Activated Carbon",
        "Sodium Thiosulfate", "Potassium Iodide", "Magnesium Sulfate", "Sodium Bicarbonate"
    };
    private static final int[] PRICES = {5, 2, 7, 6, 5, 6, 4, 4};
    private static final int[] START_STOCK = {5, 8, 3, 3, 2, 2, 2, 0};
    private static final int[][] RECIPES = {
        {0, 1}, {2, 1}, {3, 1}, {0, 2, 3}
    };
    private static final String[] FORMULAS = {
        "Calcium Carbonate + Distilled Water",
        "Disodium EDTA + Distilled Water",
        "Activated Carbon + Distilled Water",
        "Calcium Carbonate + Disodium EDTA + Activated Carbon"
    };
    private static final String[] CLUES = {
        "NEUTRALIZATION: Carbonate minerals can react with acidic residues. Add a suitable carrier.",
        "CHELATION: EDTA can bind certain metal ions. Add a suitable liquid carrier.",
        "ADSORPTION: Activated carbon has pores that can hold some contaminants. Add a carrier.",
        "COMBINE neutralization, chelation and adsorption to weaken the layered defenses."
    };
    private static final String[] CONDITIONS = {
        "Corrosive residue surrounds the patient. Tests indicate a strongly acidic protective layer.",
        "Elevated free metal-ion activity is recorded in this hospital's earliest infected case.",
        "The infected host carries airborne chemical contaminants on its damaged clothing.",
        "The physician exhibits acidic, ionic and contaminant barriers at once."
    };
    private static final String[] OBJECTIVES = {
        "Identify a carbonate and a carrier that can react with the acidic barrier.",
        "Find a chelating agent and a carrier to reduce free metal-ion activity.",
        "Choose a porous adsorbent and a carrier to trap the contamination.",
        "Combine neutralization, complexation and adsorption in one fictional treatment."
    };
    private static final String[] DESCRIPTIONS = {
        "An infected patient exposed to a spill in the treatment wing.",
        "The hospital's first recorded infected patient, not necessarily the outbreak's origin.",
        "A patient altered by contaminated ventilation and experimental fumes.",
        "A physician transformed while trying to contain the outbreak."
    };
    private static final String[] NAV = {"Mix Potion", "Recipes", "Elements", "Shop", "Research"};
    private static final String[] JOURNAL_TABS = {"Research Journal", "Periodic Table", "Zombies", "Recipes"};
    private static final Color[] POTION_COLORS = VIAL_COLORS;
    private static final String[] SHORT_POTION_NAMES = SHORT_NAMES;
    private static final int[] SHOP_PRICES = PRICES;
    private static final String[] TABS = JOURNAL_TABS;
    private static final String[] DESC = DESCRIPTIONS;
    private static final String[] CONDITION = CONDITIONS;
    private static final String[] OBJECTIVE = OBJECTIVES;
    private static final String[] CLUE = CLUES;
    private static final String[] FORMULA = FORMULAS;
    private static final Rectangle HUD_PAUSE = new Rectangle(606, 22, 30, 30);
    private static final Rectangle HUD_EXIT = new Rectangle(651, 22, 30, 30);
    private static final Rectangle RESUME = new Rectangle(333, 211, 294, 47);
    private static final Rectangle SAVE_EXIT = new Rectangle(333, 277, 294, 47);
    private static final Rectangle NO_SAVE = new Rectangle(333, 343, 294, 47);

    private final String scientist;
    private final Runnable onWin;
    private final Runnable onMenu;
    private final javax.swing.Timer timer;
    private final Random rng = new Random(27);
    private final SpriteLoader sprites = new SpriteLoader();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Defeated> defeated = new ArrayList<>();
    private final Set<Integer> keys = new HashSet<>();
    private final int[] stock = START_STOCK.clone();
    private final int[] vials = new int[4];
    private final int[] stabilized = new int[4];
    private final boolean[] seen = new boolean[4];
    private final boolean[] unlockedRecipes = new boolean[4];
    private final boolean[] selected = new boolean[CHEMS.length];
    private final BufferedImage hospital = loadHospitalBackground();

    private double px = 132, py = 369, camera, hp = 100;
    private int ticks, wave = 1, spawned, spawnTimer = 90, waveTimer;
    private int xp, coins = 18, medkits = 2, equipped, target, batchQty = 1;
    private int hurtTicks, throwCooldown, labTicks, labTab, journalTab = 2, journalChoice;
    private int noticeTicks, encounterType = -1, hoverControl;
    private String facing = "right";
    private String notice = "Contaminated Hospital: find the missing research.";
    private String labNotice = "Select a zombie and examine its chemistry clue.";
    private boolean paused, lab, journal, encounter, dead, won, winNotified;
    private Enemy encountered;

    private static final class Popup {
        int x, y, h, objectiveY, objectiveH, buttonY;
        Rectangle button() { return new Rectangle(x + 311, y + buttonY, 96, 25); }
    }
    private static final class Enemy {
        final int type;
        double x, y;
        int hp, cooldown, flash, slowed;
        String face = "left";
        Enemy(int type, double x, double y) {
            this.type = type; this.x = x; this.y = y;
            this.hp = type == 3 ? 4 : type == 0 ? 1 : 2;
        }
    }
    private static final class Defeated {
        final int type;
        final double x, y;
        final String face;
        int age;
        Defeated(Enemy z) { type = z.type; x = z.x; y = z.y; face = z.face; }
    }
    private static final class Projectile {
        final Enemy enemy;
        final int type;
        final double sx, sy;
        double x, y, t;
        Projectile(Enemy enemy, int type, double sx, double sy) {
            this.enemy = enemy; this.type = type;
            this.sx = sx; this.sy = sy; this.x = sx; this.y = sy;
        }
    }

    public Level2GamePanel(String scientist, Runnable onWin, Runnable onMenu) {
        this.scientist = scientist == null ? "nicole" : scientist;
        GameplayBridge.startLevel(2, this.scientist);
        this.onWin = onWin;
        this.onMenu = onMenu;
        setBackground(new Color(14, 13, 30));
        setOpaque(true);
        setFocusable(true);
        setDoubleBuffered(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                handleClick(toX(e.getX()), toY(e.getY()));
            }
            @Override public void mouseExited(MouseEvent e) {
                hoverControl = 0; repaint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int x = toX(e.getX()), y = toY(e.getY());
                hoverControl = hudControlsAvailable() ?
                        (HUD_PAUSE.contains(x, y) ? 1 : HUD_EXIT.contains(x, y) ? 2 : 0) : 0;
                setCursor(hoverControl != 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                repaint();
            }
        });
        installKeys();
        timer = new javax.swing.Timer(16, e -> updateGame());
        timer.start();
    }

    private int toX(int mouseX) { return (int) (mouseX * W / (double) Math.max(1, getWidth())); }
    private int toY(int mouseY) { return (int) (mouseY * H / (double) Math.max(1, getHeight())); }

    private void bind(int key, String id, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), id);
        getActionMap().put(id, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); repaint(); }
        });
    }
    private void hold(int key) {
        String id = "hold" + key;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0, false), id);
        getActionMap().put(id, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { keys.add(key); }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0, true), "release" + key);
        getActionMap().put("release" + key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { keys.remove(key); }
        });
    }
    private void installKeys() {
        for (int k : new int[] {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
                KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT}) hold(k);
        bind(KeyEvent.VK_SPACE, "throw", this::throwPotion);
        bind(KeyEvent.VK_M, "lab", this::toggleLab);
        bind(KeyEvent.VK_J, "journal", this::toggleJournal);
        bind(KeyEvent.VK_H, "heal", this::heal);
        bind(KeyEvent.VK_Q, "nextPotion", () -> equipped = (equipped + 1) % 4);
        bind(KeyEvent.VK_P, "pause", this::togglePause);
        bind(KeyEvent.VK_ESCAPE, "escape", this::escape);
        bind(KeyEvent.VK_R, "restart", () -> { if (dead) reset(); });
        bind(KeyEvent.VK_ENTER, "enter", () -> {
            if (encounter) encounter = false;
            else if (won) advanceAfterWin();
        });
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            bind(KeyEvent.VK_1 + i, "slot" + i, () -> equipped = slot);
        }
    }
    private void escape() {
        if (encounter) encounter = false;
        else if (lab) lab = false;
        else if (journal) journal = false;
        else togglePause();
    }
    private void togglePause() { if (!dead && !won) { if (paused) paused = false; else openPauseMenu(); } }
    public void openPauseMenu() {
        if (dead || won) return;
        keys.clear(); paused = true; hoverControl = 0; repaint();
    }
    public void disposeGame() { timer.stop(); keys.clear(); }
    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    // Same per-treatment crafting countdown as Level 1.
    private int craftingSeconds(int t) {
        return (t == 3 ? 60 : t == 2 ? 45 : t == 1 ? 35 : 25)
                + GameplayBridge.craftingBonus(scientist);
    }
    private void chooseTreatment(int recipe) {
        target = recipe; batchQty = 1;
        Arrays.fill(selected, false);
        if (unlockedRecipes[recipe]) {
            for (int ingredient : RECIPES[recipe]) selected[ingredient] = true;
            labNotice = "Known recipe loaded: " + NAMES[recipe] + ". Select quantity and mix.";
        } else {
            labNotice = "Treatment selected: " + NAMES[recipe] + ". Read Recipes for clues.";
        }
    }
    private void toggleLab() {
        if (paused || dead || won || encounter) return;
        journal = false;
        if (lab) { lab = false; keys.clear(); return; }
        boolean any = false;
        for (boolean value : seen) if (value) any = true;
        if (!any) { message("Encounter a zombie first."); return; }
        lab = true; labTab = 0; batchQty = 1;
        Arrays.fill(selected, false);
        labNotice = "Choose a treatment or select the ingredients for a new recipe.";
        labTicks = craftingSeconds(target) * 60;
        keys.clear();
    }
    private void toggleJournal() {
        if (paused || dead || won || encounter) return;
        lab = false; journal = !journal; keys.clear();
    }
    private void heal() {
        if (paused || lab || journal || encounter || dead || won) return;
        if (medkits <= 0) { message("No medkits left."); return; }
        if (hp >= 100) { message("Health is already full."); return; }
        medkits--; hp = GameplayBridge.useMedkit(hp, scientist);
        message("Medkit used. Health restored.");
    }
    private void message(String value) { notice = value; noticeTicks = 240; }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private int quota() { return wave == 1 ? 3 : wave == 2 ? 4 : wave == 3 ? 5 : 1; }
    private int activeLimit() { return wave == 1 ? 2 : wave == 2 ? 2 : wave == 3 ? 3 : 1; }
    private int nextEnemyType() {
        if (wave == 1) return 0;
        if (wave == 2) return spawned % 3 == 0 ? 0 : 1;
        if (wave == 3) return spawned % 3 == 0 ? 2 : spawned % 3 == 1 ? 1 : 0;
        return 3;
    }
    private void spawnEnemy() {
        int type = nextEnemyType();
        double ex = Math.min(WORLD - 85, Math.max(px + 290, camera + W + 40));
        ex = Math.min(WORLD - 85, ex + rng.nextInt(115));
        double ey = 322 + rng.nextInt(74);
        enemies.add(new Enemy(type, ex, ey));
        spawned++; spawnTimer = type == 3 ? 170 : 125;
    }

    private void updateGame() {
        ticks++;
        if (noticeTicks > 0) noticeTicks--;
        if (!paused) {
            defeated.removeIf(d -> ++d.age > 38);
        }
        if (paused || dead || won || encounter || journal) { repaint(); return; }
        if (lab) {
            if (labTab != 3 && --labTicks <= 0) {
                lab = false; message("Lab time expired. Reopen it to try again.");
            }
            repaint(); return;
        }
        if (hurtTicks > 0) hurtTicks--;
        if (throwCooldown > 0) throwCooldown--;
        updatePlayer();
        if (spawned < quota() && enemies.size() < activeLimit() && --spawnTimer <= 0) spawnEnemy();
        updateEnemies();
        updateProjectiles();
        if (!encounter && spawned >= quota() && enemies.isEmpty()) {
            if (++waveTimer >= 95 && wave < 4) {
                wave++; spawned = 0; spawnTimer = 85; waveTimer = 0;
                message("WAVE " + wave + " - new infected signatures detected.");
                try { saveProgress(); } catch (IOException e) { message("Checkpoint save failed: " + e.getMessage()); }
            }
        } else waveTimer = 0;
        if (hp <= 0) { hp = 0; dead = true; keys.clear(); }
        repaint();
    }
    private void updatePlayer() {
        double dx = 0, dy = 0;
        if (keys.contains(KeyEvent.VK_A) || keys.contains(KeyEvent.VK_LEFT)) dx--;
        if (keys.contains(KeyEvent.VK_D) || keys.contains(KeyEvent.VK_RIGHT)) dx++;
        if (keys.contains(KeyEvent.VK_W) || keys.contains(KeyEvent.VK_UP)) dy--;
        if (keys.contains(KeyEvent.VK_S) || keys.contains(KeyEvent.VK_DOWN)) dy++;
        if (dx != 0 || dy != 0) {
            double oldX = px, oldY = py;
            double length = Math.hypot(dx, dy);
            px = clamp(px + dx / length * 3.0, 34, WORLD - 35);
            py = clamp(py + dy / length * 2.5, FLOOR_TOP + 60, FLOOR_BOTTOM - 15);
            GameplayBridge.recordMovement(oldX, oldY, px, py);
            if (dx < 0) facing = "left";
            else if (dx > 0) facing = "right";
            else if (dy < 0) facing = "up";
            else facing = "down";
        }
        camera = clamp(px - W * .38, 0, WORLD - W);
    }
    private void updateEnemies() {
        for (Enemy z : new ArrayList<>(enemies)) {
            if (z.flash > 0) z.flash--;
            if (z.cooldown > 0) z.cooldown--;
            if (z.slowed > 0) z.slowed--;
            double dx = px - z.x, dy = py - z.y;
            double distance = Math.hypot(dx, dy);
            if (distance < 450 && !seen[z.type]) {
                seen[z.type] = true;
                GameplayBridge.scan(2, z.type, CHEMS[RECIPES[z.type][0]]);
                encounterType = z.type;
                encountered = z;
                encounter = true;
                keys.clear();
                message("NEW RESEARCH: " + NAMES[z.type]);
                return;
            }
            if (distance > 43) {
                double speed = (z.type == 1 ? 1.35 : z.type == 2 ? 1.13 : z.type == 3 ? .82 : 1.04);
                if (z.slowed > 0) speed *= .55;
                z.x += dx / Math.max(distance, 1) * speed;
                z.y += dy / Math.max(distance, 1) * speed * .65;
                z.face = dx < 0 ? "left" : "right";
            } else if (z.cooldown == 0 && hurtTicks == 0) {
                hp = GameplayBridge.damagePlayer(hp, GameplayBridge.attackDamage(2, z.type));
                hurtTicks = 45;
                z.cooldown = 75;
                message("Infected contact! Use a matching treatment.");
            }
        }
    }
    private void updateProjectiles() {
        for (Iterator<Projectile> it = projectiles.iterator(); it.hasNext();) {
            Projectile p = it.next();
            p.t += .055;
            double t = Math.min(1, p.t);
            p.x = p.sx + (p.enemy.x - p.sx) * t;
            p.y = p.sy + (p.enemy.y - 25 - p.sy) * t - Math.sin(Math.PI * t) * 75;
            if (p.t >= 1) {
                it.remove();
                if (!enemies.contains(p.enemy)) continue;
                int previousHealth = p.enemy.hp;
                p.enemy.hp = GameplayBridge.applyTreatment(2, p.enemy.type, p.enemy.hp, p.type);
                if (p.enemy.hp == previousHealth) {
                    p.enemy.slowed = 80;
                    message("Incorrect treatment! Review the Research Journal.");
                } else {
                    p.enemy.flash = 18;
                    if (p.enemy.hp <= 0) stabilize(p.enemy);
                    else message("Treatment effective. " + NAMES[p.type] + " is weakening.");
                }
            }
        }
    }
    private void stabilize(Enemy z) {
        enemies.remove(z);
        defeated.add(new Defeated(z));
        stabilized[z.type]++;
        Reward earned = GameplayBridge.rewards(2, z.type);
        int[] progress = GameplayBridge.award(xp, coins, earned);
        xp = progress[0]; coins = progress[1];
        int reward = rng.nextInt(CHEMS.length - 1);
        stock[reward]++;
        if (rng.nextInt(5) == 0) medkits++;
        message(NAMES[z.type] + " stabilized! XP, coins and an ingredient earned.");
        if (z.type == 3) completeLevel();
    }
    private void completeLevel() {
        won = true; keys.clear();
        try {
            GameplayBridge.completeLevel(2);
            SaveManager.markCompleted(2); // Unlock Level 3.
            saveProgress();
            message("LEVEL 2 COMPLETE - Level 3 unlocked!");
        } catch (IOException e) {
            message("Level completed, but saving failed: " + e.getMessage());
        }
    }
    private void advanceAfterWin() {
        if (winNotified) return;
        try {
            SaveManager.markCompleted(2);
            saveProgress();
        } catch (IOException e) {
            message("Cannot unlock Level 3: " + e.getMessage());
            return;
        }
        winNotified = true;
        disposeGame();
        if (onWin != null) onWin.run();
    }
    private void throwPotion() {
        if (paused || lab || journal || encounter || dead || won || throwCooldown > 0) return;
        if (vials[equipped] <= 0) { message("No " + SHORT_NAMES[equipped] + " potions. Press M to craft."); return; }
        Enemy closest = null;
        double nearest = 290;
        for (Enemy z : enemies) {
            double d = Math.hypot(z.x - px, z.y - py);
            if (d < nearest) { nearest = d; closest = z; }
        }
        if (closest == null) { message("No infected within throwing range."); return; }
        vials[equipped]--;
        projectiles.add(new Projectile(closest, equipped, px + 10, py - 31));
        throwCooldown = 36;
    }
    private void reset() {
        px = 132; py = 369; camera = 0; hp = 100;
        ticks = 0; wave = 1; spawned = 0; spawnTimer = 90; waveTimer = 0;
        xp = 0; coins = 18; medkits = 2; equipped = 0; target = 0; batchQty = 1;
        hurtTicks = 0; throwCooldown = 0; labTicks = 0; labTab = 0;
        journalTab = 2; journalChoice = 0; encounterType = -1;
        facing = "right"; encountered = null; paused = false; lab = false; journal = false;
        encounter = false; dead = false; won = false; winNotified = false;
        Arrays.fill(vials, 0); Arrays.fill(stabilized, 0);
        Arrays.fill(seen, false); Arrays.fill(unlockedRecipes, false);
        Arrays.fill(selected, false);
        System.arraycopy(START_STOCK, 0, stock, 0, stock.length);
        enemies.clear(); projectiles.clear(); defeated.clear(); keys.clear();
        message("Hospital run restarted.");
        try { saveProgress(); } catch (IOException e) { message("Restart save failed: " + e.getMessage()); }
    }

    // ---- Persistent Level 2 state; coexists with existing Level 1 save keys. ----
    public void saveProgress() throws IOException {
        Properties p = SaveManager.load();
        p.setProperty("scientist", scientist);
        p.setProperty("level2.started", "true");
        p.setProperty("level2.won", Boolean.toString(won));
        p.setProperty("level2.px", Double.toString(px));
        p.setProperty("level2.py", Double.toString(py));
        p.setProperty("level2.hp", Double.toString(hp));
        p.setProperty("level2.ticks", Integer.toString(ticks));
        p.setProperty("level2.wave", Integer.toString(wave));
        p.setProperty("level2.spawned", Integer.toString(spawned));
        p.setProperty("level2.spawnTimer", Integer.toString(spawnTimer));
        p.setProperty("level2.waveTimer", Integer.toString(waveTimer));
        p.setProperty("level2.xp", Integer.toString(xp));
        if (GameManager.getInstance().getPlayer() != null)
            p.setProperty("level2.researchPoints", Integer.toString(
                GameManager.getInstance().getPlayer().getResearchPoints()));
        p.setProperty("level2.coins", Integer.toString(coins));
        p.setProperty("level2.medkits", Integer.toString(medkits));
        p.setProperty("level2.equipped", Integer.toString(equipped));
        p.setProperty("level2.target", Integer.toString(target));
        p.setProperty("level2.facing", facing);
        p.setProperty("level2.stock", join(stock));
        p.setProperty("level2.vials", join(vials));
        p.setProperty("level2.stabilized", join(stabilized));
        p.setProperty("level2.seen", join(seen));
        p.setProperty("level2.recipes", join(unlockedRecipes));
        StringJoiner zombies = new StringJoiner(";");
        for (Enemy z : enemies) {
            zombies.add(z.type + "," + z.x + "," + z.y + "," + z.hp + ","
                    + z.cooldown + "," + z.flash + "," + z.slowed + "," + z.face);
        }
        p.setProperty("level2.enemies", zombies.toString());
        StringJoiner shots = new StringJoiner(";");
        for (Projectile shot : projectiles) {
            int idx = enemies.indexOf(shot.enemy);
            if (idx < 0) continue;
            shots.add(idx + "," + shot.type + "," + shot.sx + "," + shot.sy + ","
                    + shot.x + "," + shot.y + "," + shot.t);
        }
        p.setProperty("level2.projectiles", shots.toString());
        GameManager.getInstance().saveProgress(() -> SaveManager.save(p));
    }
    public boolean loadProgress() {
        Properties p = SaveManager.load();
        if (!"true".equals(p.getProperty("level2.started"))
                || "true".equals(p.getProperty("level2.won"))
                || !scientist.equals(p.getProperty("scientist"))) return false;
        try {
            double newX = clamp(Double.parseDouble(p.getProperty("level2.px", "132")), 34, WORLD - 35);
            double newY = clamp(Double.parseDouble(p.getProperty("level2.py", "369")), FLOOR_TOP + 60, FLOOR_BOTTOM - 15);
            double newHp = clamp(Double.parseDouble(p.getProperty("level2.hp", "100")), 1, 100);
            int newWave = clamp(Integer.parseInt(p.getProperty("level2.wave", "1")), 1, 4);
            int[] newStock = parseInts(p.getProperty("level2.stock"), START_STOCK);
            int[] newVials = parseInts(p.getProperty("level2.vials"), new int[4]);
            int[] newStabilized = parseInts(p.getProperty("level2.stabilized"), new int[4]);
            boolean[] newSeen = parseBools(p.getProperty("level2.seen"), new boolean[4]);
            boolean[] newRecipes = parseBools(p.getProperty("level2.recipes"), new boolean[4]);
            List<Enemy> loadedEnemies = new ArrayList<>();
            String enemyData = p.getProperty("level2.enemies", "");
            if (!enemyData.isEmpty()) for (String line : enemyData.split(";")) {
                String[] a = line.split(",", -1);
                if (a.length != 8) throw new IllegalArgumentException("Bad enemy data");
                int type = Integer.parseInt(a[0]);
                if (type < 0 || type > 3) throw new IllegalArgumentException("Bad enemy type");
                Enemy z = new Enemy(type, Double.parseDouble(a[1]), Double.parseDouble(a[2]));
                z.hp = Integer.parseInt(a[3]); z.cooldown = Integer.parseInt(a[4]);
                z.flash = Integer.parseInt(a[5]); z.slowed = Integer.parseInt(a[6]);
                z.face = a[7]; loadedEnemies.add(z);
            }
            List<Projectile> loadedShots = new ArrayList<>();
            String projectileData = p.getProperty("level2.projectiles", "");
            if (!projectileData.isEmpty()) for (String line : projectileData.split(";")) {
                String[] a = line.split(",", -1);
                if (a.length != 7) throw new IllegalArgumentException("Bad projectile data");
                int idx = Integer.parseInt(a[0]);
                if (idx < 0 || idx >= loadedEnemies.size()) continue;
                Projectile shot = new Projectile(loadedEnemies.get(idx), Integer.parseInt(a[1]),
                        Double.parseDouble(a[2]), Double.parseDouble(a[3]));
                shot.x = Double.parseDouble(a[4]); shot.y = Double.parseDouble(a[5]);
                shot.t = Double.parseDouble(a[6]); loadedShots.add(shot);
            }
            px = newX; py = newY; hp = newHp;
            wave = newWave; ticks = parseInt(p, "level2.ticks", 0);
            spawned = clamp(parseInt(p, "level2.spawned", 0), 0, quota());
            spawnTimer = parseInt(p, "level2.spawnTimer", 90);
            waveTimer = parseInt(p, "level2.waveTimer", 0);
            xp = parseInt(p, "level2.xp", 0);
            coins = parseInt(p, "level2.coins", 18);
            medkits = parseInt(p, "level2.medkits", 2);
            equipped = clamp(parseInt(p, "level2.equipped", 0), 0, 3);
            target = clamp(parseInt(p, "level2.target", 0), 0, 3);
            facing = p.getProperty("level2.facing", "right");
            System.arraycopy(newStock, 0, stock, 0, stock.length);
            System.arraycopy(newVials, 0, vials, 0, vials.length);
            System.arraycopy(newStabilized, 0, stabilized, 0, stabilized.length);
            System.arraycopy(newSeen, 0, seen, 0, seen.length);
            System.arraycopy(newRecipes, 0, unlockedRecipes, 0, unlockedRecipes.length);
            enemies.clear(); enemies.addAll(loadedEnemies);
            projectiles.clear(); projectiles.addAll(loadedShots);
            defeated.clear();
            camera = clamp(px - W * .38, 0, WORLD - W);
            paused = false; lab = false; journal = false; encounter = false; encountered = null; dead = false; won = false;
            GameplayBridge.restoreScientistProgress(xp, coins,
                parseInt(p, "level2.researchPoints", 0), hp, px, py);
            keys.clear();
            message("Level 2 progress restored. Continue your research.");
            repaint();
            return true;
        } catch (RuntimeException e) {
            System.err.println("CHEMION Level 2: Invalid save: " + e.getMessage());
            return false;
        }
    }
    private static int parseInt(Properties p, String key, int fallback) {
        return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)));
    }
    private static String join(int[] a) {
        StringJoiner s = new StringJoiner(","); for (int x : a) s.add(Integer.toString(x)); return s.toString();
    }
    private static String join(boolean[] a) {
        StringJoiner s = new StringJoiner(","); for (boolean x : a) s.add(Boolean.toString(x)); return s.toString();
    }
    private static int[] parseInts(String raw, int[] fallback) {
        if (raw == null) return fallback.clone();
        String[] parts = raw.split(",", -1);
        if (parts.length != fallback.length) throw new IllegalArgumentException("Bad inventory length");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Integer.parseInt(parts[i]);
            if (values[i] < 0) throw new IllegalArgumentException("Negative inventory");
        }
        return values;
    }
    private static boolean[] parseBools(String raw, boolean[] fallback) {
        if (raw == null) return fallback.clone();
        String[] parts = raw.split(",", -1);
        if (parts.length != fallback.length) throw new IllegalArgumentException("Bad research length");
        boolean[] values = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) values[i] = Boolean.parseBoolean(parts[i]);
        return values;
    }

    // ---- Lab crafting and purchasing ----
    private int detectedRecipe() {
        return GameplayBridge.detectRecipe(2, CHEMS, RECIPES, NAMES, selected);
    }
    private int maxBatch(int recipe) {
        return GameplayBridge.maxBatch(2, CHEMS, RECIPES, NAMES, stock, recipe);
    }
    private int maxBatch() {
        int recipe = detectedRecipe();
        return recipe < 0 ? 0 : maxBatch(recipe);
    }
    private void mix() {
        int recipe = detectedRecipe();
        if (recipe < 0) {
            labTicks = Math.max(0, labTicks - 180);
            labNotice = "Incorrect mixture! -3 seconds. Check Recipes for chemistry clues.";
            return;
        }
        if (!seen[recipe]) {
            labNotice = "Encounter " + NAMES[recipe] + " to unlock its research first.";
            return;
        }
        int max = maxBatch(recipe);
        if (max < 1) {
            labNotice = "Not enough ingredients. Buy more in the Shop.";
            return;
        }
        if (batchQty > max) {
            batchQty = max;
            labNotice = "Quantity adjusted to " + max + " based on your inventory.";
            return;
        }
        if (!GameplayBridge.craft(2, CHEMS, RECIPES, NAMES, stock, vials,
                unlockedRecipes, recipe, batchQty)) {
            labNotice = "Crafting failed. Check your ingredients and quantity.";
            return;
        }
        target = recipe;
        equipped = recipe;
        lab = false;
        Arrays.fill(selected, false);
        message("Crafted " + batchQty + " " + NAMES[recipe] + " potion(s)! Press SPACE or click to throw.");
    }
    private void purchase(int index) {
        if (coins < PRICES[index]) { labNotice = "Not enough coins."; return; }
        int remaining = GameplayBridge.buy(coins, CHEMS, stock, index, PRICES[index]);
        if (remaining < 0) { labNotice = "Not enough coins."; return; }
        coins = remaining;
        labNotice = "Purchased " + CHEMS[index] + ".";
    }

    private boolean hudControlsAvailable() {
        return !paused && !lab && !journal && !encounter && !dead && !won;
    }
    private void handleClick(int x, int y) {
        if (won) {
            if (new Rectangle(362, 325, 236, 38).contains(x, y)) advanceAfterWin();
            return;
        }
        if (dead) {
            if (new Rectangle(362, 325, 236, 38).contains(x, y)) reset();
            return;
        }
        if (paused) {
            if (RESUME.contains(x, y)) paused = false;
            else if (SAVE_EXIT.contains(x, y)) {
                try { saveProgress(); disposeGame(); if (onMenu != null) onMenu.run(); }
                catch (IOException ex) { message("SAVE FAILED: " + ex.getMessage()); }
            } else if (NO_SAVE.contains(x, y)) {
                disposeGame(); if (onMenu != null) onMenu.run();
            }
            repaint(); return;
        }
        if (encounter) {
            if (popupLayout(getFontMetrics(new Font("SansSerif", Font.PLAIN, 11))).button().contains(x, y)) encounter = false;
            repaint(); return;
        }
        if (lab) { clickLab(x, y); repaint(); return; }
        if (journal) { clickJournal(x, y); repaint(); return; }
        if (HUD_PAUSE.contains(x, y) || HUD_EXIT.contains(x, y)) {
            openPauseMenu(); return; // Purple menu only; never a white JOptionPane.
        }
        for (int i = 0; i < 4; i++) {
            if (new Rectangle(22 + i * 132, 468, 126, 51).contains(x, y)) {
                equipped = i; repaint(); return;
            }
        }
        if (new Rectangle(568, 473, 181, 48).contains(x, y)) toggleLab();
        else if (new Rectangle(760, 473, 184, 48).contains(x, y)) toggleJournal();
        else if (y < 460) throwPotion();
        repaint();
    }
    private void clickLab(int x, int y) {
        if (new Rectangle(884, 29, 31, 30).contains(x, y)) { lab = false; return; }
        for (int i = 0; i < NAV.length; i++) {
            if (new Rectangle(44, 110 + i * 57, 138, 44).contains(x, y)) {
                labTab = i; return;
            }
        }
        if (labTab == 0) {
            for (int i = 0; i < 4; i++) {
                if (new Rectangle(211 + i * 172, 99, 162, 30).contains(x, y) && seen[i]) {
                    chooseTreatment(i); return;
                }
            }
            for (int i = 0; i < CHEMS.length; i++) {
                int bx = 210 + i % 2 * 164, by = 174 + i / 2 * 54;
                if (new Rectangle(bx, by, 155, 47).contains(x, y)) {
                    selected[i] = !selected[i]; return;
                }
            }
            if (new Rectangle(604, 406, 35, 24).contains(x, y)) batchQty = Math.max(1, batchQty - 1);
            else if (new Rectangle(681, 406, 35, 24).contains(x, y)) batchQty = Math.min(Math.max(1, maxBatch()), batchQty + 1);
            else if (new Rectangle(727, 406, 66, 24).contains(x, y)) {
                int recipe = detectedRecipe();
                if (recipe >= 0) batchQty = Math.max(1, maxBatch(recipe));
            } else if (new Rectangle(646, 443, 214, 36).contains(x, y)) mix();
        } else if (labTab == 1) {
            for (int i = 0; i < 4; i++) {
                int bx = 210 + i % 2 * 350, by = 157 + i / 2 * 147;
                if (new Rectangle(bx, by, 334, 131).contains(x, y) && seen[i]) {
                    chooseTreatment(i); labTab = 0; return;
                }
            }
        } else if (labTab == 3) {
            for (int i = 0; i < CHEMS.length; i++) {
                int bx = 210 + i % 2 * 350, by = 151 + i / 2 * 81;
                if (new Rectangle(bx + 239, by + 18, 82, 34).contains(x, y)) {
                    purchase(i); return;
                }
            }
        }
    }
    private void clickJournal(int x, int y) {
        if (new Rectangle(882, 34, 30, 29).contains(x, y)) { journal = false; return; }
        for (int i = 0; i < 4; i++) {
            if (new Rectangle(56 + i * 213, 101, 201, 33).contains(x, y)) {
                journalTab = i; return;
            }
        }
        if (journalTab == 2) for (int i = 0; i < 4; i++) {
            if (new Rectangle(61, 163 + i * 71, 243, 58).contains(x, y)) journalChoice = i;
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.scale(getWidth() / (double) W, getHeight() / (double) H);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(hospital, -(int) camera, 0, null);
        drawActors(g);
        drawHUD(g);
        if (paused) drawPauseMenu(g);
        else if (encounter) drawEncounter(g);
        else if (lab) drawLab(g);
        else if (journal) drawJournal(g);
        else if (dead) drawEnd(g, "GAME OVER", "Your scientist was overwhelmed.", false);
        else if (won) drawEnd(g, "LEVEL 2 COMPLETE", "The Plague Doctor has been stabilized!", true);
        g.dispose();
    }

    private static BufferedImage loadHospitalBackground() {
        // Optional custom art. The level is fully playable without these files.
        String[] candidates = {
            "assets/backgrounds/level2_hospital.png",
            "assets/backgrounds/contaminated_hospital.png",
            "assets/backgrounds/level2.png"
        };
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (!file.isFile()) file = new File("../" + candidate);
            if (!file.isFile()) continue;
            try {
                BufferedImage source = ImageIO.read(file);
                if (source == null) continue;
                BufferedImage result = new BufferedImage(WORLD, H, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(source, 0, 0, WORLD, H, null);
                g.dispose();
                return result;
            } catch (IOException e) {
                System.err.println("Hospital background could not load: " + e.getMessage());
            }
        }
        return buildHospital();
    }

    private static BufferedImage buildHospital() {
        BufferedImage img = new BufferedImage(WORLD, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(13, 17, 33)); g.fillRect(0, 0, WORLD, H);
        // Repeating hospital walls, tiles and clinical lighting.
        for (int x = 0; x < WORLD; x += 32) {
            int sector = x / 790;
            g.setColor(sector % 2 == 0 ? new Color(36, 46, 60) : new Color(35, 42, 59));
            g.fillRect(x, 65, 32, FLOOR_TOP - 65);
            g.setColor(new Color(54, 65, 77)); g.fillRect(x, 62, 31, 3);
            g.setColor(new Color(24, 30, 46)); g.fillRect(x, 205, 31, 12);
            g.setColor(new Color(37, 42, 57)); g.fillRect(x, FLOOR_TOP, 32, FLOOR_BOTTOM - FLOOR_TOP);
            g.setColor(new Color(47, 52, 65)); g.drawLine(x, FLOOR_TOP, x, FLOOR_BOTTOM);
            for (int y = FLOOR_TOP + 32; y < FLOOR_BOTTOM; y += 32) {
                g.setColor(new Color(50, 54, 68)); g.drawLine(x, y, x + 31, y);
            }
            g.setColor(new Color(17, 19, 31)); g.fillRect(x, FLOOR_BOTTOM, 32, H - FLOOR_BOTTOM);
        }
        // Ceiling lights, hospital room identifiers, doors, shattered windows.
        String[] sections = {"EMERGENCY WING", "ISOLATION WARD", "VENTILATION UNIT", "RESEARCH WING"};
        for (int s = 0; s < 4; s++) {
            int start = s * 790;
            g.setColor(new Color(14, 19, 31)); g.fillRect(start, 0, 790, 64);
            g.setColor(new Color(80, 91, 103)); g.fillRect(start + 40, 20, 208, 8);
            g.setColor(new Color(112, 217, 202, 70)); g.fillRect(start + 45, 28, 198, 14);
            g.setColor(new Color(20, 31, 45));
            g.fillRoundRect(start + 283, 84, 270, 28, 5, 5);
            g.setColor(new Color(130, 188, 183));
            g.drawRoundRect(start + 283, 84, 270, 28, 5, 5);
            g.setColor(new Color(190, 223, 221));
            g.setFont(new Font("Monospaced", Font.BOLD, 15));
            g.drawString(sections[s], start + 295, 104);
            g.setColor(new Color(16, 24, 39));
            g.fillRect(start + 110, 92, 160, 119);
            g.setColor(new Color(96, 131, 143));
            g.drawRect(start + 110, 92, 160, 119);
            g.drawLine(start + 190, 92, start + 190, 211);
            g.setColor(new Color(20, 35, 48)); g.fillRect(start + 124, 106, 51, 84);
            g.fillRect(start + 205, 106, 51, 84);
            g.setColor(new Color(110, 194, 188, 52));
            g.fillRect(start + 124, 106, 51, 84);
            g.fillRect(start + 205, 106, 51, 84);
            g.setColor(new Color(86, 105, 119));
            g.fillRect(start + 565, 78, 120, 138);
            g.setColor(new Color(21, 28, 43));
            g.fillRect(start + 575, 87, 101, 129);
            g.setColor(new Color(110, 147, 156));
            g.drawRect(start + 575, 87, 101, 129);
            g.setColor(new Color(231, 159, 119)); g.fillRect(start + 657, 146, 5, 9);
            // Distinctive hospital wall cross.
            g.setColor(s == 3 ? new Color(216, 154, 225) : new Color(102, 202, 187));
            g.fillRect(start + 403, 110, 15, 64);
            g.fillRect(start + 379, 134, 63, 15);
            // Beds and mobile medical carts are scenery, not collision obstacles.
            for (int b = 0; b < 2; b++) {
                int bx = start + 70 + b * 320;
                g.setColor(new Color(34, 46, 59)); g.fillRect(bx, 296, 162, 15);
                g.setColor(new Color(118, 143, 152)); g.fillRect(bx + 4, 287, 155, 10);
                g.setColor(new Color(67, 103, 113)); g.fillRect(bx + 10, 281, 38, 7);
                g.setColor(new Color(27, 31, 45)); g.fillRect(bx + 10, 311, 6, 20);
                g.fillRect(bx + 143, 311, 6, 20);
            }
            g.setColor(new Color(25, 36, 49)); g.fillRect(start + 580, 358, 85, 48);
            g.setColor(new Color(99, 134, 142)); g.drawRect(start + 580, 358, 85, 48);
            g.setColor(new Color(97, 183, 168)); g.fillRect(start + 612, 367, 12, 23);
            g.fillRect(start + 606, 375, 24, 8);
            g.setColor(new Color(16, 19, 30));
            g.fillOval(start + 589, 402, 13, 13); g.fillOval(start + 643, 402, 13, 13);
            // Hazard tape and contamination stains.
            for (int k = 0; k < 13; k++) {
                g.setColor(k % 2 == 0 ? new Color(188, 153, 72) : new Color(37, 36, 39));
                g.fillRect(start + 450 + k * 13, 213, 13, 7);
            }
            g.setColor(new Color(54, 110, 96, 80));
            g.fillOval(start + 310, 397, 145, 20);
            g.fillOval(start + 230, 422, 82, 11);
            g.setColor(new Color(153, 80, 98, 64));
            g.fillOval(start + 681, 381, 66, 14);
        }
        g.setColor(new Color(9, 12, 25, 178)); g.fillRect(0, 0, WORLD, 17);
        g.setColor(new Color(26, 25, 43)); g.fillRect(0, H - 17, WORLD, 17);
        g.dispose();
        return img;
    }
    private void drawActors(Graphics2D g) {
        List<Enemy> sorted = new ArrayList<>(enemies);
        sorted.sort(Comparator.comparingDouble(z -> z.y));
        for (Defeated d : defeated) drawDefeated(g, d);
        for (Enemy z : sorted) drawEnemy(g, z);
        drawPlayer(g);
        for (Projectile p : projectiles) {
            int x = (int) (p.x - camera), y = (int) p.y;
            g.setColor(VIAL_COLORS[p.type]);
            g.fillRoundRect(x - 6, y - 9, 13, 16, 3, 3);
            g.setColor(WHITE); g.drawRect(x - 5, y - 8, 11, 14);
            g.setColor(new Color(80, 59, 55)); g.fillRect(x - 3, y - 12, 7, 5);
        }
    }
    private void drawPlayer(Graphics2D g) {
        int x = (int) (px - camera), y = (int) py;
        g.setColor(new Color(6, 7, 13, 130)); g.fillOval(x - 19, y - 8, 38, 12);
        boolean moving = !keys.isEmpty() && !paused && !lab && !journal;
        String action = dead ? "death" : hurtTicks > 0 ? "hurt"
                : throwCooldown > 23 ? "throw potion" : "walk";
        BufferedImage sprite = sprites.get(true, scientist, action, facing, moving ? ticks / 8 : 0);
        if (sprite != null) {
            drawSprite(g, sprite, x, y, 62, hurtTicks > 0 ? .65f : .94f);
        } else {
            g.setColor(hurtTicks > 0 ? RED : new Color(223, 218, 239));
            g.fillRect(x - 15, y - 49, 30, 31); // coat
            g.setColor(new Color(68, 55, 92)); g.fillRect(x - 10, y - 20, 8, 19);
            g.fillRect(x + 3, y - 20, 8, 19);
            g.setColor(new Color(217, 168, 142)); g.fillRect(x - 9, y - 65, 18, 16);
            g.setColor(new Color(48, 35, 51)); g.fillRect(x - 11, y - 68, 22, 8);
            g.setColor(TEAL); g.fillRect(x - 3, y - 41, 6, 10);
        }
    }
    private void drawEnemy(Graphics2D g, Enemy z) {
        int x = (int) (z.x - camera), y = (int) z.y;
        if (x < -100 || x > W + 100) return;
        g.setColor(new Color(5, 8, 15, 150));
        g.fillOval(x - (z.type == 3 ? 31 : 20), y - 7, z.type == 3 ? 62 : 40, 13);
        String action = z.flash > 0 ? "hurt" : z.cooldown > 55 ? "attack" : "walk";
        BufferedImage sprite = sprites.get(false, ENEMY_IDS[z.type], action, z.face, ticks / 10);
        if (sprite != null) {
            drawSprite(g, sprite, x, y, z.type == 3 ? 92 : 67, z.flash > 0 ? .55f : .9f);
        } else drawFallbackEnemy(g, z, x, y);
    }
    private void drawDefeated(Graphics2D g, Defeated d) {
        int x = (int) (d.x - camera), y = (int) d.y;
        if (x < -100 || x > W + 100) return;
        BufferedImage sprite = sprites.get(false, ENEMY_IDS[d.type], "death", d.face, d.age / 9);
        if (sprite != null) {
            drawSprite(g, sprite, x, y, d.type == 3 ? 92 : 67,
                    Math.max(.05f, 1f - d.age / 40f));
        } else {
            // Gentle dissolving pixels; no flames or fire effects.
            g.setColor(new Color(111, 216, 169, Math.max(0, 140 - d.age * 3)));
            for (int i = 0; i < 12; i++) {
                int ox = (i * 19 % 51) - 25;
                int oy = (i * 13 % 42) + d.age / 2;
                g.fillRect(x + ox, y - 45 + oy, 4, 4);
            }
        }
    }

    private static void drawSprite(Graphics2D g, BufferedImage img, int centerX, int footY, int height, float alpha) {
        int width = Math.max(1, (int) Math.round(height * img.getWidth() / (double) Math.max(1, img.getHeight())));
        width = Math.min(width, height * 2);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(img, centerX - width / 2, footY - height, width, height, null);
        g.setComposite(old);
    }
    private void drawFallbackEnemy(Graphics2D g, Enemy z, int x, int y) {
        boolean boss = z.type == 3;
        int h = boss ? 87 : 62;
        Color coat = z.type == 0 ? new Color(94, 128, 110)
                : z.type == 1 ? new Color(154, 151, 166)
                : z.type == 2 ? new Color(105, 145, 109) : new Color(53, 48, 67);
        if (z.flash > 0) coat = new Color(215, 118, 142);
        int bodyW = boss ? 43 : 31;
        g.setColor(coat); g.fillRect(x - bodyW / 2, y - h + 25, bodyW, h - 40);
        g.setColor(new Color(40, 41, 57));
        g.fillRect(x - bodyW / 2 + 4, y - 16, 9, 16);
        g.fillRect(x + bodyW / 2 - 13, y - 16, 9, 16);
        g.setColor(new Color(125, 143, 135));
        g.fillRect(x - bodyW / 2 - 9, y - h + 32, 10, 29);
        g.fillRect(x + bodyW / 2 - 1, y - h + 32, 10, 29);
        g.setColor(z.type == 3 ? new Color(211, 209, 191) : new Color(154, 172, 143));
        g.fillRect(x - 12, y - h + 5, 24, 22);
        g.setColor(new Color(222, 84, 107));
        g.fillRect(x - 8, y - h + 13, 5, 4); g.fillRect(x + 4, y - h + 13, 5, 4);
        if (z.type == 0) { // Acid: green leaking residue.
            g.setColor(new Color(102, 221, 137));
            g.fillRect(x - 17, y - h + 39, 8, 18); g.fillRect(x + 11, y - h + 49, 6, 12);
        } else if (z.type == 1) { // Case Zero: medical patient bandage.
            g.setColor(new Color(230, 229, 209));
            g.fillRect(x - 11, y - h + 9, 22, 6);
            g.setColor(new Color(175, 75, 103)); g.fillRect(x - 4, y - h + 10, 8, 3);
        } else if (z.type == 2) { // Toxic: mask and chemical vapor.
            g.setColor(new Color(113, 225, 161));
            g.fillRect(x - 8, y - h + 20, 16, 9);
            g.fillRect(x + 18, y - h + 22, 8, 12);
            g.fillRect(x + 25, y - h + 14, 5, 6);
        } else { // Plague Doctor: hat and beaked mask.
            g.setColor(new Color(34, 29, 43));
            g.fillRect(x - 21, y - h + 1, 42, 8);
            g.fillRect(x - 15, y - h - 8, 30, 12);
            g.setColor(new Color(211, 206, 173));
            g.fillPolygon(new int[] {x - 1, x + 24, x - 1},
                    new int[] {y - h + 19, y - h + 24, y - h + 27}, 3);
            g.setColor(new Color(105, 213, 164));
            g.fillRect(x - 18, y - h + 52, 8, 13);
            g.fillRect(x + 10, y - h + 49, 8, 16);
        }
    }
    private void drawHUD(Graphics2D g) {
        hudPanel(g, 14, 14, 286, 75);
        g.setColor(new Color(77, 52, 105)); g.fillOval(25, 25, 49, 49);
        g.setColor(PURPLE); g.drawOval(25, 25, 49, 49);
        g.setColor(new Color(210, 180, 161)); g.fillOval(40, 35, 18, 20);
        g.setColor(new Color(47, 33, 63)); g.fillArc(36, 30, 28, 27, 0, 180);
        g.setColor(new Color(174, 143, 218)); g.fillRoundRect(35, 58, 29, 13, 6, 6);
        label(g, scientist.toUpperCase(Locale.ROOT), 88, 35, 14, WHITE, true);
        label(g, "HP", 89, 53, 11, new Color(170, 225, 181), true);
        for (int i = 0; i < 10; i++) {
            int x = 113 + i * 16;
            g.setColor(new Color(50, 44, 63)); g.fillRoundRect(x, 43, 13, 12, 3, 3);
            if (hp > i * 10) {
                g.setColor(hp > 30 ? new Color(102, 206, 139) : new Color(223, 100, 114));
                g.fillRoundRect(x, 43, 13, 12, 3, 3);
            }
        }
        label(g, (int) hp + " / 100", 89, 74, 11, WHITE, true);
        hudPanel(g, 399, 14, 164, 61);
        label(g, "CONTAMINATED HOSPITAL", 413, 33, 10, MUTED, true);
        label(g, "WAVE " + wave + " / 4", 426, 55, 16, PURPLE, true);
        hudPanel(g, 721, 14, 225, 75);
        label(g, "XP  " + xp, 735, 38, 13, new Color(186, 222, 166), true);
        label(g, "COINS  " + coins, 827, 38, 13, new Color(243, 210, 148), true);
        int total = 0; for (int n : stabilized) total += n;
        label(g, "STABILIZED  " + total, 735, 61, 11, WHITE, true);
        label(g, "MEDKITS  " + medkits, 846, 61, 10, WHITE, true);
        // Icon-only controls occupy the top HUD gap, not the battlefield itself.
        if (hudControlsAvailable()) {
            drawHudControl(g, HUD_PAUSE, true, hoverControl == 1);
            drawHudControl(g, HUD_EXIT, false, hoverControl == 2);
        }
        // Show EVERY crafted potion type, including its individual inventory count.
        // Selection never consumes a vial; only throwing does.
        hudPanel(g, 14, 461, 539, 65);
        for (int i = 0; i < 4; i++) drawPotionSlot(g, i);
        label(g, "1-4 select  |  Q switch  |  SPACE throw  |  WASD move  |  H heal",
                20, 454, 10, MUTED, false);
        hudButton(g, 568, 473, 181, 48, "MAKE POTION [M]", true);
        hudButton(g, 760, 473, 184, 48, "RESEARCH [J]", false);
        if (noticeTicks > 0) {
            hudPanel(g, 218, 413, 530, 34);
            label(g, fit(g, notice, 502, 11), 231, 435, 11, new Color(245, 207, 155), true);
        }
    }


    private void drawPotionIcon(Graphics2D g, int x, int y, Color liquid, boolean available) {
        // Pixel-style vial, drawn in Java so no extra asset is needed.
        Color glass = available ? new Color(225, 218, 244) : new Color(110, 104, 128);
        Color fill = available ? liquid : new Color(80, 75, 94);
        g.setColor(new Color(99, 75, 66)); g.fillRect(x + 7, y - 2, 11, 5); // cork
        g.setColor(glass); g.drawRect(x + 8, y + 3, 9, 5); // neck
        g.drawRoundRect(x + 2, y + 9, 22, 22, 5, 5); // bottle
        g.setColor(fill); g.fillRoundRect(x + 5, y + 17, 16, 11, 3, 3);
        g.setColor(available ? new Color(250, 246, 255) : new Color(150, 144, 166));
        g.fillRect(x + 7, y + 12, 3, 7); // glass reflection
        g.setColor(new Color(25, 22, 42)); g.drawLine(x + 7, y + 28, x + 19, y + 28);
    }
    private void drawEncounter(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics(new Font("SansSerif", Font.PLAIN, 11));
        Popup p = popupLayout(fm);
        int x = p.x, y = p.y, i = encounterType;
        if (encountered != null) {
            int ex = (int) (encountered.x - camera), ey = (int) encountered.y - 38;
            g.setColor(new Color(201, 164, 241, 165));
            g.drawLine(ex, ey, ex > x + 210 ? x + 420 : x, y + 27);
        }
        panel(g, x, y, 420, p.h, 243);
        label(g, NAMES[i].toUpperCase(Locale.ROOT), x + 13, y + 21, 13, PURPLE, true);
        label(g, "[ SCAN ]", x + 346, y + 21, 10, PURPLE, true);
        g.setColor(new Color(112, 87, 142)); g.drawLine(x + 11, y + 30, x + 409, y + 30);
        int end = wrap(g, DESC[i], x + 13, y + 45, 390, 11, 14, WHITE);
        label(g, "OBSERVED CONDITION", x + 13, end + 5, 11, PURPLE, true);
        wrap(g, CONDITION[i], x + 13, end + 21, 390, 11, 14, WHITE);
        g.setColor(new Color(53, 41, 69));
        g.fillRoundRect(x + 10, y + p.objectiveY, 400, p.objectiveH, 7, 7);
        label(g, "SCIENTIFIC OBJECTIVE", x + 17, y + p.objectiveY + 16, 11, new Color(241, 208, 155), true);
        wrap(g, OBJECTIVE[i], x + 17, y + p.objectiveY + 30, 370, 11, 14, WHITE);
        Rectangle b = p.button();
        button(g, b.x, b.y, b.width, b.height, "CONTINUE", new Color(124, 58, 237, 164));
    }
    private void drawLab(Graphics2D g) {
        g.setColor(new Color(8, 6, 17, 200)); g.fillRect(0, 0, W, H);
        panel(g, 24, 19, 912, 501, 250);
        label(g, "CHEMISTRY LAB", 43, 53, 19, PURPLE, true);
        label(g, labTab == 3 ? "SHOP - TIMER PAUSED" :
                "TIME LEFT  " + Math.max(0, (labTicks + 59) / 60) + "s", 687, 52,
                labTab == 3 ? 11 : 13,
                labTab == 3 ? GOLD :
                labTicks < 360 ? new Color(244, 125, 144) : new Color(244, 202, 146), true);
        g.setColor(new Color(58, 48, 75)); g.fillRoundRect(680, 62, 193, 7, 4, 4);
        g.setColor(PURPLE);
        g.fillRoundRect(680, 62, (int) (193 * clamp(labTicks / (craftingSeconds(target) * 60.0), 0, 1)), 7, 4, 4);
        button(g, 885, 29, 30, 29, "X", new Color(102, 63, 138));
        g.setColor(new Color(91, 69, 117)); g.drawLine(194, 83, 194, 498);
        for (int i = 0; i < 5; i++)
            button(g, 44, 110 + i * 57, 138, 44, NAV[i], labTab == i ? new Color(109, 70, 158) : new Color(48, 38, 69));
        switch (labTab) {
            case 0: drawLabMix(g); break;
            case 1: drawLabRecipes(g); break;
            case 2: drawLabElements(g); break;
            case 3: drawLabShop(g); break;
            default: drawLabResearch(g); break;
        }
    }
    private void drawLabMix(Graphics2D g) {
        label(g, "SELECT TREATMENT", 210, 91, 11, MUTED, true);
        for (int i = 0; i < 4; i++) {
            button(g, 211 + i * 172, 99, 162, 30, seen[i] ? NAMES[i] : "LOCKED",
                    !seen[i] ? new Color(43, 35, 57) : target == i ? new Color(116, 77, 166) : new Color(56, 44, 77));
        }
        label(g, "INGREDIENT INVENTORY", 210, 156, 12, PURPLE, true);
        label(g, "SELECTED INGREDIENTS", 565, 156, 12, PURPLE, true);
        for (int i = 0; i < CHEMS.length; i++) {
            int x = 210 + (i % 2) * 164, y = 174 + (i / 2) * 54;
            g.setColor(selected[i] ? new Color(94, 65, 133) : new Color(47, 38, 67));
            g.fillRoundRect(x, y, 155, 47, 8, 8);
            g.setColor(selected[i] ? PURPLE : new Color(100, 82, 130));
            g.drawRoundRect(x, y, 155, 47, 8, 8);
            label(g, fit(g, CHEMS[i], 141, 10), x + 8, y + 19, 10, WHITE, false);
            label(g, "x" + stock[i] + (selected[i] ? "   SELECTED" : ""), x + 8, y + 36, 10, selected[i] ? PURPLE : MUTED, false);
        }
        g.setColor(new Color(32, 26, 49)); g.fillRoundRect(555, 167, 350, 153, 10, 10);
        int n = 0;
        for (int i = 0; i < CHEMS.length; i++) {
            if (!selected[i]) continue;
            int x = 564 + (n % 2) * 168, y = 181 + (n / 2) * 32;
            g.setColor(new Color(74, 55, 106)); g.fillRoundRect(x, y, 160, 26, 6, 6);
            g.setColor(new Color(155, 121, 207)); g.drawRoundRect(x, y, 160, 26, 6, 6);
            label(g, fit(g, CHEMS[i], 143, 10), x + 8, y + 17, 10, WHITE, false);
            n++;
        }
        if (n == 0) label(g, "Click chemicals on the left to add them.", 566, 193, 10, MUTED, false);
        g.setColor(new Color(104, 82, 137)); g.drawLine(564, 331, 897, 331);
        label(g, "RESULTING COMPOUND", 565, 350, 11, new Color(244, 203, 147), true);
        int recognized = detectedRecipe();
        String result = n == 0 ? "No ingredients selected" :
                recognized >= 0 ? (maxBatch(recognized) > 0 ?
                    "VALID RECIPE - READY TO MIX" : "VALID RECIPE - INGREDIENTS MISSING") :
                    "Unverified combination";
        label(g, fit(g, result, 322, 11), 565, 373, 11,
                recognized >= 0 ? new Color(183, 233, 166) : WHITE, true);
        label(g, "FOR: " + NAMES[recognized >= 0 ? recognized : target],
                565, 392, 10, MUTED, false);
        // Batch quantity. One mix can make 1, 2, 3 ... MAX potions, never a fixed cap.
        label(g, "QTY", 566, 422, 11, new Color(245, 207, 155), true);
        button(g, 604, 406, 35, 24, "-", new Color(65, 48, 97));
        label(g, String.valueOf(batchQty), 651, 422, 12, WHITE, true);
        button(g, 681, 406, 35, 24, "+", new Color(65, 48, 97));
        button(g, 727, 406, 66, 24, "MAX", new Color(88, 57, 127));
        label(g, "Max: " + maxBatch(), 810, 422, 10, MUTED, false);
        g.setColor(new Color(100, 81, 129)); g.drawLine(210, 420, 543, 420);
        wrap(g, labNotice, 210, 445, 414, 11, 16, new Color(245, 207, 155));
        button(g, 646, 443, 214, 36, "MIX POTION", new Color(114, 71, 167));
        label(g, "TIP: open Recipes for chemistry clues", 210, 486, 10, MUTED, false);
    }
    private void drawLabRecipes(Graphics2D g) {
        label(g, "RECIPES & CHEMISTRY CLUES", 210, 122, 14, PURPLE, true);
        label(g, "Click an unlocked card to try its treatment.", 210, 141, 11, MUTED, false);
        for (int i = 0; i < 4; i++) {
            int x = 210 + (i % 2) * 350, y = 157 + (i / 2) * 147;
            panel(g, x, y, 334, 131, 238);
            label(g, seen[i] ? NAMES[i].toUpperCase(Locale.ROOT) : "UNKNOWN - LOCKED", x + 13, y + 23, 12, seen[i] ? PURPLE : MUTED, true);
            if (seen[i]) {
                label(g, unlockedRecipes[i] ? "RECIPE UNLOCKED" : "RESEARCH CLUE", x + 13, y + 43, 10,
                        unlockedRecipes[i] ? new Color(181, 231, 161) : new Color(241, 203, 147), true);
                wrap(g, unlockedRecipes[i] ? FORMULA[i] : CLUE[i], x + 13, y + 62, 307, 11, 15, WHITE);
            } else wrap(g, "Encounter this zombie to unlock its chemistry clue.", x + 13, y + 64, 307, 11, 17, MUTED);
        }
        label(g, "Fictional treatments use real chemistry concepts, not medical prescriptions.", 210, 478, 10, MUTED, false);
    }
    private void drawLabElements(Graphics2D g) {
        label(g, "CHEMICAL & MATERIAL INVENTORY", 210, 124, 14, PURPLE, true);
        for (int i = 0; i < CHEMS.length; i++) {
            int x = 210 + (i % 2) * 350, y = 151 + (i / 2) * 81;
            panel(g, x, y, 334, 69, 238);
            label(g, CHEMS[i], x + 14, y + 28, 12, WHITE, true);
            label(g, "AVAILABLE: " + stock[i], x + 14, y + 50, 10, PURPLE, true);
        }
    }
    private void drawLabShop(Graphics2D g) {
        label(g, "INGREDIENT SHOP", 210, 122, 14, PURPLE, true);
        label(g, "COINS: " + coins, 737, 122, 14, GOLD, true);
        label(g, "Buy missing chemicals, then return to Mix Potion.",
                210, 139, 10, MUTED, false);

        for (int i = 0; i < CHEMS.length; i++) {
            int x = 210 + (i % 2) * 350;
            int y = 151 + (i / 2) * 81;
            panel(g, x, y, 334, 69, 238);
            label(g, CHEMS[i], x + 13, y + 23, 11, WHITE, true);
            label(g, "OWNED: " + stock[i] + "   PRICE: " + SHOP_PRICES[i] + " coins",
                    x + 13, y + 47, 10, MUTED, false);
            button(g, x + 239, y + 18, 82, 34, "BUY +1",
                    coins >= SHOP_PRICES[i] ?
                    new Color(113, 71, 167) : new Color(57, 46, 72));
        }
        label(g, fit(g, labNotice, 660, 10), 210, 489, 10, GOLD, false);
    }
    private void drawLabResearch(Graphics2D g) {
        label(g, "OBSERVATION LOG - LEVEL 2", 210, 128, 14, PURPLE, true);
        int y = 163;
        for (int i = 0; i < 4; i++) {
            if (seen[i]) {
                label(g, NAMES[i].toUpperCase(Locale.ROOT), 210, y, 12, PURPLE, true);
                y = wrap(g, CLUE[i], 210, y + 18, 665, 11, 16, WHITE) + 17;
            }
        }
        if (y == 163) label(g, "No research collected yet.", 210, 166, 12, MUTED, false);
    }
    private void drawJournal(Graphics2D g) {
        g.setColor(new Color(8, 6, 17, 200)); g.fillRect(0, 0, W, H);
        panel(g, 34, 24, 892, 492, 250);
        label(g, "RESEARCH JOURNAL", 54, 62, 19, PURPLE, true);
        label(g, "LEVEL 2 / CONTAMINATED HOSPITAL", 611, 59, 10, MUTED, true);
        button(g, 882, 34, 30, 29, "X", new Color(102, 63, 138));
        for (int i = 0; i < 4; i++)
            button(g, 56 + i * 213, 101, 201, 33, TABS[i], i == journalTab ? new Color(111, 70, 163) : new Color(48, 38, 69));
        if (journalTab == 2) drawJournalZombies(g);
        else if (journalTab == 1) {
            label(g, "PERIODIC TABLE - RELEVANT ELEMENTS", 62, 169, 14, PURPLE, true);
            String[] elements = {"H / Hydrogen", "O / Oxygen", "Na / Sodium", "Cl / Chlorine", "C / Carbon", "Ca / Calcium", "N / Nitrogen", "Mg / Magnesium"};
            for (int i = 0; i < elements.length; i++) {
                int x = 63 + (i % 4) * 210, y = 196 + (i / 4) * 105;
                panel(g, x, y, 190, 88, 240);
                label(g, elements[i], x + 14, y + 46, 12, WHITE, true);
            }
        } else if (journalTab == 3) {
            label(g, "DISCOVERED RECIPES & CLUES", 62, 165, 14, PURPLE, true);
            for (int i = 0; i < 4; i++) {
                int y = 193 + i * 74;
                label(g, NAMES[i], 62, y, 12, PURPLE, true);
                String s = !seen[i] ? "Locked - encounter this zombie first." : unlockedRecipes[i] ? FORMULA[i] : CLUE[i];
                wrap(g, s, 62, y + 18, 820, 11, 15, WHITE);
            }
        } else {
            label(g, "RESEARCH PROGRESS", 62, 166, 14, PURPLE, true);
            for (int i = 0; i < 4; i++) {
                int y = 207 + i * 66;
                label(g, NAMES[i].toUpperCase(Locale.ROOT), 62, y, 13, WHITE, true);
                label(g, (seen[i] ? "OBSERVED" : "LOCKED") + "   /   STABILIZED: " + stabilized[i] +
                        "   /   RECIPE: " + (unlockedRecipes[i] ? "UNLOCKED" : "HIDDEN"), 62, y + 22, 11, MUTED, false);
            }
        }
    }
    private void drawJournalZombies(Graphics2D g) {
        for (int i = 0; i < 4; i++) {
            int y = 163 + i * 71;
            button(g, 61, y, 243, 58, seen[i] ? NAMES[i] : "??? LOCKED ???",
                    i == journalChoice ? new Color(97, 65, 139) : new Color(47, 38, 68));
            label(g, !seen[i] ? "LOCKED" : stabilized[i] > 0 ? "COMPLETED" : "IN PROGRESS",
                    76, y + 48, 10, seen[i] ? new Color(187, 226, 165) : MUTED, true);
        }
        panel(g, 327, 158, 564, 326, 238);
        int i = journalChoice;
        if (!seen[i]) {
            label(g, "UNKNOWN INFECTED", 345, 192, 16, PURPLE, true);
            wrap(g, "This zombie's information unlocks when it first enters the Contaminated Hospital battlefield.",
                    345, 225, 523, 12, 19, WHITE);
            return;
        }
        label(g, NAMES[i].toUpperCase(Locale.ROOT), 345, 191, 16, PURPLE, true);
        label(g, "TYPE: " + (i == 3 ? "Fallen Physician" : "Infected Patient"), 345, 220, 11, WHITE, true);
        label(g, "DIFFICULTY: " + (i == 0 ? "Moderate" : i == 1 ? "Challenging" : i == 2 ? "Hard" : "Boss"), 345, 241, 11, WHITE, true);
        label(g, "STATUS: " + (stabilized[i] > 0 ? "Completed" : "In Progress"), 345, 262, 11, WHITE, true);
        label(g, "OBSERVED CONDITION", 345, 290, 11, new Color(244, 204, 151), true);
        int next = wrap(g, CONDITION[i], 345, 308, 521, 11, 16, WHITE);
        label(g, "RESEARCH NOTES", 345, next + 10, 11, PURPLE, true);
        int end = wrap(g, CLUE[i], 345, next + 28, 521, 11, 16, WHITE);
        if (unlockedRecipes[i])
            label(g, fit(g, "UNLOCKED FORMULA: " + FORMULA[i], 522, 10), 345, Math.min(470, end + 13),
                    10, new Color(186, 231, 165), true);
    }
    private void drawPauseMenu(Graphics2D g) {
        g.setColor(new Color(7, 5, 17, 215));
        g.fillRect(0, 0, W, H);
        panel(g, 252, 98, 456, 341, 245);
        centered(g, "GAME MENU", 252, 119, 456, 54, 26, PURPLE, true);
        centered(g, "ESC / P to resume", 252, 172, 456, 22, 12, MUTED, false);
        button(g, RESUME.x, RESUME.y, RESUME.width,
            RESUME.height, "RESUME", new Color(98, 65, 146));
        button(g, SAVE_EXIT.x, SAVE_EXIT.y, SAVE_EXIT.width,
            SAVE_EXIT.height, "SAVE & EXIT", new Color(78, 100, 100));
        button(g, NO_SAVE.x, NO_SAVE.y, NO_SAVE.width,
            NO_SAVE.height, "EXIT WITHOUT SAVING", new Color(91, 54, 76));
        centered(g, "Save & Exit returns to the Main Menu", 252, 400, 456, 22, 11, MUTED, false);
    }
    private void drawEnd(Graphics2D g, String title, String sub, boolean victory) {
        g.setColor(new Color(7, 5, 17, 205)); g.fillRect(0, 0, W, H);
        panel(g, 260, 164, 440, 215, 244);
        label(g, title, 304, 227, 25, victory ? new Color(185, 227, 155) : PURPLE, true);
        label(g, sub, 300, 268, 12, WHITE, false);
        button(g, 362, 325, 236, 38, victory ? "CONTINUE" : "RESTART [R]", new Color(111, 67, 163));
    }
    private void panel(Graphics2D g, int x, int y, int w, int h, int alpha) {
        g.setColor(new Color(36, 27, 54, alpha)); g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(new Color(158, 119, 203)); g.drawRoundRect(x, y, w, h, 12, 12);
    }
    private void button(Graphics2D g, int x, int y, int w, int h, String text, Color color) {
        g.setColor(color); g.fillRoundRect(x, y, w, h, 7, 7);
        g.setColor(new Color(180, 147, 218)); g.drawRoundRect(x, y, w, h, 7, 7);
        centered(g, text, x, y, w, h, text.length() > 19 ? 10 : 11, WHITE, true);
    }
    private static void text(Graphics2D g, String s, int x, int y, int size, Color c, boolean bold) {
        g.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, size));
        g.setColor(c); g.drawString(s, x, y);
    }
    private static void center(Graphics2D g, String s, int x, int y, int w, int h, int size, Color c, boolean bold) {
        g.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, size));
        FontMetrics fm = g.getFontMetrics(); g.setColor(c);
        g.drawString(s, x + (w - fm.stringWidth(s)) / 2,
                y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }
    private String fit(Graphics2D g, String text, int width, int size) {
        g.setFont(new Font("Monospaced", Font.PLAIN, size)); FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= width) return text;
        while (text.length() > 1 && fm.stringWidth(text + "...") > width) text = text.substring(0, text.length() - 1);
        return text + "...";
    }
    private int wrap(Graphics2D g, String text, int x, int y, int width, int size, int line, Color color) {
        g.setFont(new Font("SansSerif", Font.PLAIN, size)); g.setColor(color);
        FontMetrics fm = g.getFontMetrics(); StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = current.length() == 0 ? word : current + " " + word;
            if (fm.stringWidth(next) > width && current.length() > 0) {
                g.drawString(current.toString(), x, y); y += line; current = new StringBuilder(word);
            } else { if (current.length() > 0) current.append(' '); current.append(word); }
        }
        if (current.length() > 0) { g.drawString(current.toString(), x, y); y += line; }
        return y;
    }
    private void drawHudControl(Graphics2D g, Rectangle r, boolean pauseIcon, boolean hover) {
        Graphics2D b = (Graphics2D) g.create();
        b.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        int x = r.x, y = r.y, w = r.width, h = r.height;
        int cut = 5;
        Polygon frame = new Polygon(
            new int[]{x + cut, x + w - cut - 1, x + w - 1, x + w - 1,
                x + w - cut - 1, x + cut, x, x},
            new int[]{y, y, y + cut, y + h - cut - 1,
                y + h - 1, y + h - 1, y + h - cut - 1, y + cut}, 8);
        b.setColor(new Color(10, 7, 20, 100));
        b.fillRect(x + 2, y + 3, w - 2, h - 2);
        b.setPaint(new GradientPaint(x, y,
            hover ? new Color(94, 65, 139) : new Color(45, 33, 70),
            x, y + h,
            hover ? new Color(67, 43, 112) : new Color(27, 23, 48)));
        b.fillPolygon(frame);
        b.setColor(hover ? new Color(242, 205, 255) : new Color(151, 116, 196));
        b.setStroke(new BasicStroke(hover ? 2f : 1f));
        b.drawPolygon(frame);
        b.setColor(hover ? new Color(234, 183, 255) : new Color(127, 90, 171));
        b.fillRect(x + 6, y + 3, 7, 2);
        b.setColor(hover ? new Color(250, 230, 255) : new Color(216, 190, 245));
        int cx = x + w / 2, cy = y + h / 2;
        if (pauseIcon) {
            b.fillRect(cx - 6, cy - 7, 4, 14);
            b.fillRect(cx + 2, cy - 7, 4, 14);
        } else {
            b.setStroke(new BasicStroke(2f));
            b.drawLine(cx - 9, cy - 8, cx - 9, cy + 8);
            b.drawLine(cx - 9, cy - 8, cx - 2, cy - 8);
            b.drawLine(cx - 9, cy + 8, cx - 2, cy + 8);
            b.fillRect(cx - 4, cy - 2, 11, 4);
            b.fillPolygon(new int[]{cx + 4, cx + 10, cx + 4},
                new int[]{cy - 6, cy, cy + 6}, 3);
        }
        b.dispose();
    }

    private void drawPotionSlot(Graphics2D g, int type) {
        int x = HOTBAR_X + type * (HOTBAR_SLOT_W + HOTBAR_GAP), y = HOTBAR_Y;
        boolean active = equipped == type;
        boolean available = vials[type] > 0;
        g.setColor(active ? new Color(104, 67, 149, 245) : new Color(43, 35, 61, 230));
        g.fillRoundRect(x, y, HOTBAR_SLOT_W, HOTBAR_SLOT_H, 8, 8);
        g.setColor(active ? new Color(241, 205, 143) : new Color(106, 86, 137));
        g.setStroke(new BasicStroke(active ? 2f : 1f));
        g.drawRoundRect(x, y, HOTBAR_SLOT_W, HOTBAR_SLOT_H, 8, 8);
        g.setStroke(new BasicStroke(1f));
        label(g, String.valueOf(type + 1), x + 7, y + 13, 9,
                active ? new Color(255, 216, 153) : MUTED, true);
        drawPotionIcon(g, x + 16, y + 14, POTION_COLORS[type], available);
        label(g, SHORT_POTION_NAMES[type], x + 42, y + 21, 10,
                available ? WHITE : MUTED, true);
        label(g, "x" + vials[type], x + 42, y + 41, 14,
                available ? POTION_COLORS[type] : new Color(140, 128, 158), true);
        if (active) {
            g.setColor(new Color(255, 219, 148));
            g.fillRect(x + 43, y + 46, HOTBAR_SLOT_W - 52, 2);
        }
    }

    private void hudPanel(Graphics2D g, int x, int y, int w, int h) {
        Path2D path = new Path2D.Double();
        path.moveTo(x + 11, y); path.lineTo(x + w - 1, y);
        path.lineTo(x + w, y + h - 11); path.lineTo(x + w - 11, y + h);
        path.lineTo(x, y + h); path.lineTo(x, y + 11); path.closePath();
        g.setPaint(new GradientPaint(x, y, new Color(38, 28, 58, 239), x, y + h, new Color(22, 19, 37, 244)));
        g.fill(path); g.setColor(new Color(153, 116, 207)); g.draw(path);
        g.setColor(new Color(182, 140, 245, 135)); g.fillRect(x + 14, y + 3, Math.min(32, w - 30), 2);
    }

    private void hudButton(Graphics2D g, int x, int y, int w, int h, String text, boolean primary) {
        g.setPaint(new GradientPaint(x, y, primary ? new Color(111, 69, 173) : new Color(65, 50, 100),
                x, y + h, primary ? new Color(72, 43, 122) : new Color(43, 34, 73)));
        g.fillRoundRect(x, y, w, h, 9, 9);
        g.setColor(primary ? new Color(210, 171, 255) : new Color(151, 125, 196));
        g.drawRoundRect(x, y, w, h, 9, 9);
        centered(g, text, x, y, w, h, 12, WHITE, true);
    }

    private void centered(Graphics2D g, String text, int x, int y, int w, int h, int size, Color color, boolean bold) {
        g.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, size));
        FontMetrics fm = g.getFontMetrics(); g.setColor(color);
        g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void label(Graphics2D g, String text, int x, int y, int size, Color color, boolean bold) {
        g.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, size));
        g.setColor(color); g.drawString(text, x, y);
    }

    private Popup popupLayout(FontMetrics fm) {
        Popup p = new Popup();
        int d = lineCount(DESCRIPTIONS[encounterType], fm, 390);
        int c = lineCount(CONDITION[encounterType], fm, 390);
        int o = lineCount(OBJECTIVE[encounterType], fm, 370);
        p.objectiveY = 73 + d * 14 + c * 14;
        p.objectiveH = 30 + o * 14;
        p.buttonY = p.objectiveY + p.objectiveH + 8;
        p.h = p.buttonY + 25 + 9;
        int ex = encountered == null ? 840 : (int) (encountered.x - camera);
        int ey = encountered == null ? 385 : (int) encountered.y;
        p.x = ex + 55 + 420 < W - 12 ? ex + 55 : clamp(ex - 55 - 420, 12, W - 432);
        p.y = clamp(ey - p.h + 34, 70, H - p.h - 12);
        return p;
    }

    private static int lineCount(String text, FontMetrics fm, int width) {
        int lines = 1, used = 0;
        for (String word : text.split(" ")) {
            int w = fm.stringWidth(word), sp = fm.charWidth(' ');
            if (used > 0 && used + sp + w > width) { lines++; used = w; }
            else used += (used == 0 ? 0 : sp) + w;
        }
        return lines;
    }

    private static int countTrue(boolean[] values) {
        int n = 0; for (boolean value : values) if (value) n++; return n;
    }

    /** Loads 1-row PNG sprite sheets; falls back to built-in pixel silhouettes. */
    private static final class SpriteLoader {
        private final Map<String, BufferedImage[]> cache = new HashMap<>();
        private final Set<String> missing = new HashSet<>();
        private final File assets;
        SpriteLoader() {
            File root = new File("assets");
            if (!root.isDirectory()) root = new File("../assets");
            assets = root;
        }
        BufferedImage get(boolean player, String name, String action, String direction, int frame) {
            String key = (player ? "characters/" : "enemies/level2/") + name;
            BufferedImage[] frames = load(key, name, action, direction);
            if (frames == null && !action.equals("walk")) frames = load(key, name, "walk", direction);
            if (frames == null) return null;
            return frames[Math.floorMod(frame, frames.length)];
        }
        private BufferedImage[] load(String key, String name, String action, String direction) {
            String id = key + ":" + action + ":" + direction;
            if (missing.contains(id)) return null;
            if (cache.containsKey(id)) return cache.get(id);
            File folder = new File(assets, key);
            if (!folder.isDirectory() && key.startsWith("characters/")) {
                File chars = new File(assets, "characters");
                File[] options = chars.listFiles(File::isDirectory);
                if (options != null) for (File option : options) {
                    if (norm(option.getName()).equals(norm(name))
                            || norm(option.getName()).equals("dr" + norm(name))) {
                        folder = option; break;
                    }
                }
            }
            if (!folder.isDirectory() && key.startsWith("enemies/level2/")) {
                File enemyRoot = new File(assets, "enemies/level2");
                File[] options = enemyRoot.listFiles(File::isDirectory);
                if (options != null) for (File option : options) {
                    String actual = norm(option.getName());
                    String expected = norm(name);
                    if (actual.equals(expected) || actual.equals(expected + "zombie")
                            || actual.equals("zombie" + expected)) {
                        folder = option; break;
                    }
                }
            }
            List<File> files = new ArrayList<>();
            findPng(folder, files, 0);
            String dir = norm(direction), act = norm(action), n = norm(name);
            File best = null; int bestScore = Integer.MIN_VALUE;
            for (File file : files) {
                String filename = norm(file.getName().replaceFirst("(?i)\\.png$", ""));
                String parent = norm(file.getParentFile().getName());
                if (!filename.endsWith(dir) && !filename.endsWith(dir.equals("down") ? "front" : dir.equals("up") ? "back" : dir)) continue;
                if (!filename.contains(act) && !parent.equals(act)) continue;
                int score = (filename.contains(n) ? 10 : 0)
                        + (filename.contains(act) ? 20 : 0) + (parent.equals(act) ? 8 : 0);
                if (score > bestScore) { best = file; bestScore = score; }
            }
            if (best == null) { missing.add(id); return null; }
            try {
                BufferedImage sheet = ImageIO.read(best);
                if (sheet == null) throw new IOException("Unreadable PNG");
                BufferedImage[] frames = extract(sheet);
                cache.put(id, frames);
                return frames;
            } catch (IOException e) {
                System.err.println("Level 2 sprite: " + best + ": " + e.getMessage());
                missing.add(id); return null;
            }
        }
        private static void findPng(File folder, List<File> files, int depth) {
            if (!folder.isDirectory() || depth > 3) return;
            File[] children = folder.listFiles();
            if (children == null) return;
            for (File f : children) {
                if (f.isDirectory()) findPng(f, files, depth + 1);
                else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".png")) files.add(f);
            }
        }
        private static String norm(String value) {
            return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }
        private static BufferedImage[] extract(BufferedImage sheet) {
            int w = sheet.getWidth(), h = sheet.getHeight();
            if (w < h * 1.5) return new BufferedImage[] {crop(sheet)};
            // Transparent gutters separate sprite frames. Keep each entire sprite.
            int[] ink = new int[w];
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++)
                if ((sheet.getRGB(x, y) >>> 24) > 24) ink[x]++;
            boolean[] occupied = new boolean[w];
            for (int x = 0; x < w; x++) occupied[x] = ink[x] > 0;
            int gap = Math.max(2, Math.min(5, h / 35));
            int last = -1;
            for (int x = 0; x < w; x++) if (occupied[x]) {
                if (last >= 0 && x - last - 1 <= gap)
                    for (int j = last + 1; j < x; j++) occupied[j] = true;
                last = x;
            }
            List<int[]> regions = new ArrayList<>();
            int start = -1;
            for (int x = 0; x <= w; x++) {
                boolean on = x < w && occupied[x];
                if (on && start < 0) start = x;
                if (!on && start >= 0) { regions.add(new int[] {start, x - 1}); start = -1; }
            }
            if (regions.size() < 2) return new BufferedImage[] {crop(sheet)};
            int widest = 0;
            for (int[] r : regions) widest = Math.max(widest, r[1] - r[0] + 1);
            int minimum = Math.max(7, (int) Math.ceil(widest * .18));
            List<int[]> major = new ArrayList<>();
            for (int[] r : regions) if (r[1] - r[0] + 1 >= minimum) major.add(r);
            while (major.size() > 8) {
                int best = 0, smallest = Integer.MAX_VALUE;
                for (int i = 0; i < major.size() - 1; i++) {
                    int space = major.get(i + 1)[0] - major.get(i)[1] - 1;
                    if (space < smallest) { smallest = space; best = i; }
                }
                major.get(best)[1] = major.get(best + 1)[1];
                major.remove(best + 1);
            }
            if (major.size() < 2) return new BufferedImage[] {crop(sheet)};
            BufferedImage[] frames = new BufferedImage[major.size()];
            int[] boundaries = new int[major.size() + 1];
            boundaries[0] = 0; boundaries[major.size()] = w;
            for (int i = 1; i < major.size(); i++)
                boundaries[i] = (major.get(i - 1)[1] + major.get(i)[0] + 1) / 2;
            int maxW = 1, maxH = 1;
            for (int i = 0; i < frames.length; i++) {
                frames[i] = crop(sheet.getSubimage(boundaries[i], 0,
                        Math.max(1, boundaries[i + 1] - boundaries[i]), h));
                maxW = Math.max(maxW, frames[i].getWidth());
                maxH = Math.max(maxH, frames[i].getHeight());
            }
            for (int i = 0; i < frames.length; i++) {
                BufferedImage normalized = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = normalized.createGraphics();
                g.drawImage(frames[i], (maxW - frames[i].getWidth()) / 2,
                        maxH - frames[i].getHeight(), null);
                g.dispose(); frames[i] = normalized;
            }
            return frames;
        }
        private static BufferedImage crop(BufferedImage src) {
            int minX = src.getWidth(), minY = src.getHeight(), maxX = -1, maxY = -1;
            for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++)
                if ((src.getRGB(x, y) >>> 24) > 20) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            if (maxX < 0) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            minX = Math.max(0, minX - 2); minY = Math.max(0, minY - 2);
            maxX = Math.min(src.getWidth() - 1, maxX + 2);
            maxY = Math.min(src.getHeight() - 1, maxY + 2);
            BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(),
                    minX, minY, maxX + 1, maxY + 1, null);
            g.dispose(); return out;
        }
    }
}
