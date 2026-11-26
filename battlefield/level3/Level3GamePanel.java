package battlefield.level3;

import game.AudioManager;
import game.HealthEffects;

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
 * CHEMION Level 3 - Toxic Chemical Factory.
 * A playable side-scrolling level with four infected types, chemistry lab,
 * research journal, shop, save/resume, and boss unlock progression.
 * Existing character and zombie PNG sprites are used when present; all
 * environments and missing-sprite fallbacks are drawn in Java.
 */
public final class Level3GamePanel extends JPanel {
    private static final int W = 960, H = 540, WORLD = 3160;
    private static final int FLOOR_TOP = 216, FLOOR_BOTTOM = 448;
    // Shared visual language with Level 1: identical HUD, menu, and card colors.
    private static final Color WHITE = new Color(242, 234, 255);
    private static final Color PURPLE = new Color(204, 170, 251);
    private static final Color MUTED = new Color(184, 170, 204);
    private static final Color TEAL = new Color(114, 214, 200);
    private static final Color GOLD = new Color(244, 178, 83);
    private static final Color RED = new Color(242, 123, 149);
    private static final Color[] VIAL_COLORS = {
        new Color(100, 222, 180), new Color(94, 198, 225),
        new Color(246, 176, 100), new Color(215, 135, 237)
    };
    private static final String[] ENEMY_IDS = {
        "toxic_brute", "venom_spitter", "explosive_zombie", "chemical_abomination"
    };
    // Per-zombie movement speeds (pixels per 16ms tick). No wave-based speed increase.
    // Brute: slow heavy melee; Spitter: ranged; Explosive: fast charge; Abomination: boss.
    private static final double[] ENEMY_BASE_SPEED = {0.39, 0.60, 0.82, 0.44};
    private static final int SHATTER_TICKS = 17; // 272 ms at 60 FPS; no zombie death sprites
    private static final String[] NAMES = {
        "Toxic Brute", "Venom Spitter", "Explosive Zombie", "Chemical Abomination"
    };
    private static final String[] SHORT_NAMES = {"BRUTE", "SPITTER", "VOLATILE", "ABOMIN."};
    // Same four-slot dimensions and placement as Level 1.
    private static final int HOTBAR_X = 22, HOTBAR_Y = 468, HOTBAR_SLOT_W = 126;
    private static final int HOTBAR_SLOT_H = 51, HOTBAR_GAP = 6;
    // The four recipes require exactly 2, 3, 3 and 4 ingredients.
    // These are fictional in-game formulations inspired by chemistry concepts;
    // they are NOT real medicines, chemical mixing instructions or antidotes.
    private static final String[] CHEMS = {
        "Sodium Bicarbonate", "Distilled Water", "Activated Carbon", "Zeolite",
        "Sodium Thiosulfate", "Silica Gel", "Calcium Carbonate", "Potassium Iodide"
    };
    private static final int[] PRICES = {4, 2, 6, 7, 6, 5, 4, 7};
    private static final int[] START_STOCK = {5, 12, 4, 3, 3, 3, 2, 2};
    private static final int[][] RECIPES = {
        {0, 1},          // Brute: buffer + carrier (2)
        {2, 3, 1},       // Spitter: adsorbents + carrier (3)
        {4, 5, 1},       // Explosive: reducing agent + desiccant + carrier (3)
        {0, 2, 3, 4}     // Abomination: combined four-stage treatment (4)
    };
    private static final String[] FORMULAS = {
        "Sodium Bicarbonate + Distilled Water",
        "Activated Carbon + Zeolite + Distilled Water",
        "Sodium Thiosulfate + Silica Gel + Distilled Water",
        "Sodium Bicarbonate + Activated Carbon + Zeolite + Sodium Thiosulfate"
    };
    private static final String[] CLUES = {
        "BUFFERING: The brute's acidic waste coating needs a mild base and liquid carrier. Select TWO.",
        "ADSORPTION: The spitter carries dissolved and airborne residues. Use TWO adsorbents plus a carrier (THREE).",
        "STABILIZATION: Reactive residues and moisture worsen the blast. Pair a reducing agent, desiccant and carrier (THREE).",
        "MULTI-STAGE: The boss combines acidity, contamination and oxidizing residues. Use a buffer, two adsorbents and a reducing agent (FOUR)."
    };
    private static final String[] CONDITIONS = {
        "A former waste handler, covered in acidic industrial sludge. Its bulk makes it slow but its blows are heavy.",
        "A mixing-line technician altered by airborne toxins. It projects corrosive fluid from a distance.",
        "An infected maintenance worker carrying unstable residues. Its body releases a short-range shock burst after a warning.",
        "A failed processing-chamber experiment with layered chemical defenses and a ranged contamination attack."
    };
    private static final String[] OBJECTIVES = {
        "Find TWO ingredients: a mild buffering compound and a safe carrier in the game's fictional system.",
        "Select THREE ingredients: two porous adsorbents and a liquid carrier for the spitter's residue.",
        "Select THREE ingredients: a reducing compound, a drying material and a liquid carrier.",
        "Combine FOUR ingredients: a buffer, two adsorbents and a reducing compound to stabilize the final mutation."
    };
    private static final String[] DESCRIPTIONS = {
        "Toxic Brute - a huge, slow factory worker mutated near the waste tanks.",
        "Venom Spitter - a mobile mixing technician that launches contaminated droplets.",
        "Explosive Zombie - an unstable infected worker that charges a close-range burst.",
        "Chemical Abomination - the factory's heavily mutated final experiment."
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
    private final HealthEffects healthEffects = new HealthEffects();
    private final Random rng = new Random(27);
    private final SpriteLoader sprites = new SpriteLoader();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<EnemyShot> enemyShots = new ArrayList<>();
    private int bossAuraTicks;
    private final List<Defeated> defeated = new ArrayList<>();
    private final Set<Integer> keys = new HashSet<>();
    // Track Shift directly, including Shift+WASD events on different Java runtimes.
    // This dispatcher does not consume keys or change any menu/lab shortcuts.
    private final KeyEventDispatcher sprintKeys = event -> {
        if (!isShowing() || !isFocusOwner()) return false;
        if (event.getID() == KeyEvent.KEY_PRESSED
                && (event.getKeyCode() == KeyEvent.VK_SHIFT || event.isShiftDown())) {
            keys.add(KeyEvent.VK_SHIFT);
        } else if (event.getID() == KeyEvent.KEY_RELEASED
                && event.getKeyCode() == KeyEvent.VK_SHIFT) {
            keys.remove(KeyEvent.VK_SHIFT);
        }
        return false;
    };
    private final int[] stock = START_STOCK.clone();
    private final int[] vials = new int[4];
    private final int[] stabilized = new int[4];
    private final boolean[] seen = new boolean[4];
    private final boolean[] unlockedRecipes = new boolean[4];
    private final boolean[] selected = new boolean[CHEMS.length];
    private final BufferedImage factory = loadFactoryBackground();

    private double px = 132, py = 369, camera, hp = 100;
    private int ticks, wave = 1, spawned, spawnTimer = 90, waveTimer;
    private int xp, coins = 22, medkits = 2, equipped, target, batchQty = 1;
    private int hurtTicks, throwCooldown, labTicks, labTab, journalTab = 2, journalChoice;
    private int noticeTicks, encounterType = -1, hoverControl;
    private String facing = "right";
    private String notice = "Toxic Chemical Factory: trace the missing shipment records.";
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
        int hp, cooldown, flash, slowed, charge, attackTicks;
        // Slight individual variation, saved so Continue Game preserves each enemy's pace.
        double speedFactor = 1.0;
        String face = "left";
        Enemy(int type, double x, double y) {
            this.type = type; this.x = x; this.y = y;
            this.hp = type == 3 ? 5 : type == 0 ? 1 : 2;
        }
    }
    // A zombie breaks into tiny pieces of its CURRENT walk/run sprite.
    // Never loads or plays zombie death sheets.
    private static final class Shard {
        final BufferedImage image;
        final int localX, localY;
        final double vx, vy;
        Shard(BufferedImage image, int localX, int localY, double vx, double vy) {
            this.image = image; this.localX = localX; this.localY = localY;
            this.vx = vx; this.vy = vy;
        }
    }
    private static final class Defeated {
        final double x, y;
        final int width, height;
        final List<Shard> shards = new ArrayList<>();
        int age;
        Defeated(Enemy z, BufferedImage sprite, Random rng) {
            x = z.x; y = z.y;
            height = z.type == 3 ? 92 : 67;
            width = Math.min(height * 2, Math.max(1,
                    (int) Math.round(height * sprite.getWidth() / (double) sprite.getHeight())));
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(sprite, 0, 0, width, height, null);
            g.dispose();
            int tile = z.type == 3 ? 7 : 5;
            for (int sy = 0; sy < height; sy += tile) {
                for (int sx = 0; sx < width; sx += tile) {
                    int tw = Math.min(tile, width - sx), th = Math.min(tile, height - sy);
                    boolean visible = false;
                    for (int iy = sy; iy < sy + th && !visible; iy++)
                        for (int ix = sx; ix < sx + tw; ix++)
                            if ((scaled.getRGB(ix, iy) >>> 24) > 30) { visible = true; break; }
                    if (!visible) continue;
                    BufferedImage part = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D pg = part.createGraphics();
                    pg.drawImage(scaled, 0, 0, tw, th, sx, sy, sx + tw, sy + th, null);
                    pg.dispose();
                    double outward = (sx + tw / 2.0 - width / 2.0) / Math.max(1.0, width / 2.0);
                    double vx = outward * (1.1 + rng.nextDouble() * 1.5)
                            + (rng.nextDouble() - .5) * 1.4;
                    double vy = -1.0 - rng.nextDouble() * 2.5;
                    shards.add(new Shard(part, sx, sy, vx, vy));
                }
            }
        }
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

    // Dodgeable factory projectiles; never use a death or hurt sprite.
    private static final class EnemyShot {
        final int type;
        double x, y, vx, vy;
        int ttl;
        EnemyShot(int type, double x, double y, double vx, double vy, int ttl) {
            this.type = type; this.x = x; this.y = y;
            this.vx = vx; this.vy = vy; this.ttl = ttl;
        }
    }

    public Level3GamePanel(String scientist, Runnable onWin, Runnable onMenu) {
        this.scientist = scientist == null ? "nicole" : scientist;
        GameplayBridge.startLevel(3, this.scientist);
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
        // Never keep movement/run pressed when the game loses keyboard focus.
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { keys.clear(); }
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
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(sprintKeys);
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
        // A Shift+movement key event can arrive even when Swing never delivered
        // a standalone Shift press. Track both keys to make sprinting reliable.
        String shiftedId = id + "Shift";
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(key, InputEvent.SHIFT_DOWN_MASK, false), shiftedId);
        getActionMap().put(shiftedId, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                keys.add(key);
                keys.add(KeyEvent.VK_SHIFT);
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(key, InputEvent.SHIFT_DOWN_MASK, true), "release" + key);
        if (key == KeyEvent.VK_SHIFT) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK, false), id);
        }
    }
    private void installKeys() {
        for (int k : new int[] {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
                KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                KeyEvent.VK_SHIFT}) hold(k);
        bind(KeyEvent.VK_SPACE, "throw", this::throwPotion);
        bind(KeyEvent.VK_M, "lab", this::toggleLab);
        bind(KeyEvent.VK_J, "journal", this::toggleJournal);
        bind(KeyEvent.VK_H, "heal", this::heal);
        bind(KeyEvent.VK_Q, "nextPotion", this::switchPotion);
        bind(KeyEvent.VK_P, "pause", this::togglePause);
        bind(KeyEvent.VK_ESCAPE, "escape", this::escape);
        bind(KeyEvent.VK_R, "restart", () -> { if (dead) reset(); });
        bind(KeyEvent.VK_ENTER, "enter", () -> {
            if (encounter) encounter = false;
            else if (won) advanceAfterWin();
        });
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            bind(KeyEvent.VK_1 + i, "slot" + i, () -> equipPotion(slot));
            bind(KeyEvent.VK_NUMPAD1 + i, "numpadSlot" + i, () -> equipPotion(slot));
        }
    }
    private void escape() {
        if (encounter) encounter = false;
        else if (lab) lab = false;
        else if (journal) journal = false;
        else togglePause();
    }
    private void togglePause() { openPauseMenu(); }
    public void openPauseMenu() {
        if (dead || won) return;
        paused = !paused;
        keys.clear(); hoverControl = 0;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }
    public void disposeGame() {
        timer.stop();
        keys.clear();

        AudioManager.stopZombieSounds();
        AudioManager.stopHeartbeat();
        healthEffects.reset();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(sprintKeys);
    }
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
    private boolean canPlay() {
        return !paused && !lab && !journal && !encounter && !dead && !won;
    }

    private void equipPotion(int type) {
        if (!canPlay() || type < 0 || type >= vials.length) return;
        equipped = type;
        if (vials[type] > 0) message("Equipped: " + NAMES[type] + " potion (x" + vials[type] + ")");
        else message("No " + NAMES[type] + " potions yet. Press M to craft.");
    }

    private void switchPotion() {
        if (!canPlay()) return;
        for (int n = 1; n <= vials.length; n++) {
            int type = (equipped + n) % vials.length;
            if (vials[type] > 0) { equipPotion(type); return; }
        }
        message("No crafted potions yet. Press M to craft.");
    }

    private void heal() {
        if (paused || lab || journal || encounter || dead || won) return;
        if (medkits <= 0) { message("No medkits left."); return; }
        if (hp >= 100) { message("Health is already full."); return; }

        medkits--;

        double previousHp = hp;
        hp = GameplayBridge.useMedkit(hp, scientist);

        if (hp > previousHp) {
            AudioManager.playMedkitSound();
            healthEffects.triggerHeal();
            AudioManager.updateHeartbeat(hp, 100);
        }

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
        Enemy z = new Enemy(type, ex, ey);
        z.speedFactor = .85 + rng.nextDouble() * .30;
        enemies.add(z);
        spawned++; spawnTimer = type == 3 ? 165 : 120;
    }
    private void updateGame() {

        healthEffects.update(hp, 100);

        if (paused || lab || journal || encounter || dead || won) {
            AudioManager.stopZombieSounds();
            AudioManager.stopHeartbeat();
        } else {
            AudioManager.updateHeartbeat(hp, 100);
        }

        if (paused) { repaint(); return; }
        if (lab) {
            if (labTab != 3 && --labTicks <= 0) {
                lab = false; Arrays.fill(selected, false);
                message("Crafting time expired. Open the lab to retry.");
            }
            repaint(); return;
        }
        if (dead || won || encounter || journal) {
            if (won) defeated.removeIf(d -> ++d.age >= SHATTER_TICKS);
            repaint(); return;
        }
        ticks++;
        if (noticeTicks > 0) noticeTicks--;
        if (bossAuraTicks > 0) bossAuraTicks--;
        defeated.removeIf(d -> ++d.age >= SHATTER_TICKS);
        if (hurtTicks > 0) hurtTicks--;
        if (throwCooldown > 0) throwCooldown--;
        updatePlayer();
        if (spawned < quota() && enemies.size() < activeLimit() && --spawnTimer <= 0) spawnEnemy();
        updateEnemies();
        updateEnemyShots();
        updateProjectiles();
        if (!encounter && spawned >= quota() && enemies.isEmpty()) {
            if (++waveTimer >= 95 && wave < 4) {
                wave++; spawned = 0; spawnTimer = 85; waveTimer = 0;
                message(wave == 4 ? "WARNING: REACTOR CHAMBER - ABOMINATION DETECTED"
                        : "WAVE " + wave + " - new factory mutations detected.");
                try { saveProgress(); } catch (IOException e) { message("Checkpoint save failed: " + e.getMessage()); }
            }
        } else waveTimer = 0;
        if (hp <= 0) {
            hp = 0;
            dead = true;
            keys.clear();

            AudioManager.stopHeartbeat();
            AudioManager.stopZombieSounds();
            healthEffects.reset();
        }
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
            double speed = keys.contains(KeyEvent.VK_SHIFT) ? 3.05 : 2.15;
            px = clamp(px + dx / length * speed, 34, WORLD - 35);
            py = clamp(py + dy / length * speed, FLOOR_TOP + 60, FLOOR_BOTTOM - 15);
            GameplayBridge.recordMovement(oldX, oldY, px, py);
            if (dx < 0) facing = "left";
            else if (dx > 0) facing = "right";
            else if (dy < 0) facing = "up";
            else facing = "down";
        }
        camera = clamp(px - W * .38, 0, WORLD - W);
    }
    private void hurtPlayer(int damage) {
        if (hurtTicks > 0 || dead) return;
        hp = GameplayBridge.damagePlayer(hp, damage);
        hurtTicks = 42;
        message("Chemical exposure! Dodge attacks and use a matching potion.");
    }
    private void fireEnemyShot(Enemy z, double speed) {
        double dx = px - z.x, dy = py - 37 - (z.y - 43);
        double len = Math.max(1, Math.hypot(dx, dy));
        enemyShots.add(new EnemyShot(z.type, z.x, z.y - 43,
            dx / len * speed, dy / len * speed, 130));
        z.attackTicks = 24;
    }
    private void updateEnemies() {
        for (Enemy z : new ArrayList<>(enemies)) {
            if (z.flash > 0) z.flash--;
            if (z.cooldown > 0) z.cooldown--;
            if (z.slowed > 0) z.slowed--;
            if (z.attackTicks > 0) z.attackTicks--;
            double dx = px - z.x, dy = py - z.y;
            double distance = Math.hypot(dx, dy);
            if (distance < 900) {
                AudioManager.playLevel3ZombieGrowl(z.type);
            }

            if (distance < 450 && !seen[z.type]) {
                seen[z.type] = true;
                GameplayBridge.scan(3, z.type, CHEMS[RECIPES[z.type][0]]);
               encounterType = z.type; encountered = z; encounter = true;
                AudioManager.stopZombieSounds();
            if (z.type == 3) bossAuraTicks = 36;
                keys.clear();
                message("NEW RESEARCH: " + NAMES[z.type]);
                return;
            }
            if (z.type == 2 && z.charge > 0) {
                z.charge--;
                if (z.charge == 0) {
                    z.attackTicks = 24;
                    z.cooldown = 125;
                    if (distance < 88) hurtPlayer(15);
                    message("Volatile discharge! Keep away from the warning ring.");
                }
                continue;
            }
            // Spitter and boss fire dodgeable ranged attacks, not contact-only hits.
            if ((z.type == 1 || z.type == 3) && distance < (z.type == 3 ? 270 : 255)
                    && z.cooldown == 0 && distance > 55) {
                fireEnemyShot(z, z.type == 3 ? 3.4 : 3.0);
                z.cooldown = z.type == 3 ? 105 : 125;
            }
            if (z.type == 2 && distance < 88 && z.cooldown == 0) {
                z.charge = 25;
                message("Explosive Zombie is charging - move out of the ring!");
                continue;
            }
            if (distance > (z.type == 1 ? 125 : z.type == 3 ? 68 : 43)) {
                double speed = ENEMY_BASE_SPEED[z.type] * z.speedFactor;
                if (z.slowed > 0) speed *= .55;
                z.x += dx / Math.max(distance, 1) * speed;
                z.y += dy / Math.max(distance, 1) * speed * .65;
                z.face = dx < 0 ? "left" : "right";
            } else if (distance <= 46 && z.cooldown == 0) {
                hurtPlayer(GameplayBridge.attackDamage(3, z.type));
                z.attackTicks = 24;
                z.cooldown = z.type == 3 ? 90 : 85;
            }
        }
    }
    private void updateEnemyShots() {
        for (Iterator<EnemyShot> it = enemyShots.iterator(); it.hasNext();) {
            EnemyShot shot = it.next();
            shot.x += shot.vx; shot.y += shot.vy;
            if (--shot.ttl <= 0 || shot.x < 0 || shot.x > WORLD) { it.remove(); continue; }
            if (Math.hypot(shot.x - px, shot.y - (py - 37)) < 22) {
                hurtPlayer(shot.type == 3 ? 16 : 10);
                it.remove();
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
                p.enemy.hp = GameplayBridge.applyTreatment(3, p.enemy.type, p.enemy.hp, p.type);
                if (p.enemy.hp == previousHealth) {
                    p.enemy.slowed = 80;
                    message("Incorrect treatment! Review the Research Journal.");
                } else {
                    p.enemy.flash = 28;

                    if (p.enemy.hp > 0) {
                        AudioManager.playZombieDamage();
                    }

                    if (p.enemy.hp <= 0) stabilize(p.enemy);
                    else message("Treatment effective. " + NAMES[p.enemy.type] + " is weakening.");
                }
            }
        }
    }
    private void stabilize(Enemy z) {
        enemies.remove(z);
        BufferedImage pose = sprites.get(false, ENEMY_IDS[z.type],
                z.type == 2 ? "run" : "walk", z.face, ticks / 9);
        if (pose == null) pose = fallbackShatterPose(z);
        // Final hit flashes RED, then the original walking pose shatters.
        defeated.add(new Defeated(z, sprites.red(pose), rng));
        stabilized[z.type]++;
        Reward earned = GameplayBridge.rewards(3, z.type);
        int[] progress = GameplayBridge.award(xp, coins, earned);
        xp = progress[0]; coins = progress[1];
        int reward = rng.nextInt(CHEMS.length - 2);
        stock[reward]++;
        if (rng.nextInt(6) == 0) medkits++;
        message(NAMES[z.type] + " stabilized! XP, coins and an ingredient earned.");
        if (z.type == 3) completeLevel();
    }
    private void completeLevel() {
        won = true; keys.clear();
        try {
            GameplayBridge.completeLevel(3);
            SaveManager.markCompleted(3); // Unlock Level 4.
            saveProgress();
            message("LEVEL 3 COMPLETE - Level 4 unlocked!");
        } catch (IOException e) {
            message("Level completed, but saving failed: " + e.getMessage());
        }
    }
    private void advanceAfterWin() {
        if (winNotified) return;
        try {
            SaveManager.markCompleted(3);
            saveProgress();
        } catch (IOException e) {
            message("Cannot unlock Level 4: " + e.getMessage());
            return;
        }
        winNotified = true;
        disposeGame();
        if (onWin != null) onWin.run();
    }

    private Enemy autoTarget() {
        Enemy best = null;
        double score = Double.POSITIVE_INFINITY;
        for (Enemy z : enemies) {
            double dx = z.x - px, dy = z.y - py;
            double dist = Math.hypot(dx, dy);
            boolean close = dist <= 305;
            boolean aligned =
                (facing.equals("right") && dx > 0 && Math.abs(dy) < 77 && dx <= 440) ||
                (facing.equals("left") && dx < 0 && Math.abs(dy) < 77 && -dx <= 440) ||
                (facing.equals("up") && dy < 0 && Math.abs(dx) < 77 && -dy <= 385) ||
                (facing.equals("down") && dy > 0 && Math.abs(dx) < 77 && dy <= 385);
            if (!close && !aligned) continue;
            double candidate = dist - (aligned ? 65 : 0) - (z.type == equipped ? 40 : 0);
            if (candidate < score) { score = candidate; best = z; }
        }
        return best;
    }

    private void throwPotion() { throwPotion(null); }

    private void throwPotion(Enemy clicked) {
        if (!canPlay() || throwCooldown > 0) return;
        if (vials[equipped] <= 0) {
            message("No " + NAMES[equipped] + " potions. Press M to craft."); return;
        }
        Enemy targetEnemy = clicked == null ? autoTarget() : clicked;
        if (targetEnemy == null) {
            message("No zombie in range. Move closer or face the zombie."); return;
        }
        vials[equipped]--;
        projectiles.add(new Projectile(targetEnemy, equipped,
                px + (facing.equals("right") ? 9 : facing.equals("left") ? -9 : 0), py - 39));
        throwCooldown = 26;
    }
    private void reset() {
        px = 132; py = 369; camera = 0; hp = 100;

        healthEffects.reset();
        AudioManager.stopHeartbeat();

        ticks = 0; wave = 1; spawned = 0; spawnTimer = 90; waveTimer = 0;
        xp = 0; coins = 22; medkits = 2; equipped = 0; target = 0; batchQty = 1;
        hurtTicks = 0; throwCooldown = 0; labTicks = 0; labTab = 0;
        journalTab = 2; journalChoice = 0; encounterType = -1;
        facing = "right"; encountered = null; paused = false; lab = false; journal = false;
        encounter = false; dead = false; won = false; winNotified = false;
        Arrays.fill(vials, 0); Arrays.fill(stabilized, 0);
        Arrays.fill(seen, false); Arrays.fill(unlockedRecipes, false);
        Arrays.fill(selected, false);
        System.arraycopy(START_STOCK, 0, stock, 0, stock.length);
        enemies.clear(); projectiles.clear(); enemyShots.clear(); defeated.clear(); keys.clear(); bossAuraTicks = 0;
        message("Factory run restarted.");
        try { saveProgress(); } catch (IOException e) { message("Restart save failed: " + e.getMessage()); }
    }

    // ---- Persistent Level 3 state; coexists with Levels 1 and 2. ----
    public void saveProgress() throws IOException {
        Properties p = SaveManager.load();
        p.setProperty("scientist", scientist);
        p.setProperty("level3.started", "true");
        p.setProperty("level3.won", Boolean.toString(won));
        p.setProperty("level3.px", Double.toString(px));
        p.setProperty("level3.py", Double.toString(py));
        p.setProperty("level3.hp", Double.toString(hp));
        p.setProperty("level3.ticks", Integer.toString(ticks));
        p.setProperty("level3.wave", Integer.toString(wave));
        p.setProperty("level3.spawned", Integer.toString(spawned));
        p.setProperty("level3.spawnTimer", Integer.toString(spawnTimer));
        p.setProperty("level3.waveTimer", Integer.toString(waveTimer));
        p.setProperty("level3.xp", Integer.toString(xp));
        if (GameManager.getInstance().getPlayer() != null)
            p.setProperty("level3.researchPoints", Integer.toString(
                GameManager.getInstance().getPlayer().getResearchPoints()));
        p.setProperty("level3.coins", Integer.toString(coins));
        p.setProperty("level3.medkits", Integer.toString(medkits));
        p.setProperty("level3.equipped", Integer.toString(equipped));
        p.setProperty("level3.target", Integer.toString(target));
        p.setProperty("level3.facing", facing);
        p.setProperty("level3.stock", join(stock));
        p.setProperty("level3.vials", join(vials));
        p.setProperty("level3.stabilized", join(stabilized));
        p.setProperty("level3.seen", join(seen));
        p.setProperty("level3.recipes", join(unlockedRecipes));
        StringJoiner zombies = new StringJoiner(";");
        for (Enemy z : enemies) {
            zombies.add(z.type + "," + z.x + "," + z.y + "," + z.hp + ","
                    + z.cooldown + "," + z.flash + "," + z.slowed + "," + z.face
                    + "," + z.speedFactor + "," + z.charge + "," + z.attackTicks);
        }
        p.setProperty("level3.enemies", zombies.toString());
        StringJoiner shots = new StringJoiner(";");
        for (Projectile shot : projectiles) {
            int idx = enemies.indexOf(shot.enemy);
            if (idx < 0) continue;
            shots.add(idx + "," + shot.type + "," + shot.sx + "," + shot.sy + ","
                    + shot.x + "," + shot.y + "," + shot.t);
        }
        p.setProperty("level3.projectiles", shots.toString());
        StringJoiner hostile = new StringJoiner(";");
        for (EnemyShot shot : enemyShots) {
            hostile.add(shot.type + "," + shot.x + "," + shot.y + "," + shot.vx + "," + shot.vy + "," + shot.ttl);
        }
        p.setProperty("level3.enemyShots", hostile.toString());
        GameManager.getInstance().saveProgress(() -> SaveManager.save(p));
    }
    public boolean loadProgress() {
        Properties p = SaveManager.load();
        if (!"true".equals(p.getProperty("level3.started"))
                || "true".equals(p.getProperty("level3.won"))
                || !scientist.equals(p.getProperty("scientist"))) return false;
        try {
            double newX = clamp(Double.parseDouble(p.getProperty("level3.px", "132")), 34, WORLD - 35);
            double newY = clamp(Double.parseDouble(p.getProperty("level3.py", "369")), FLOOR_TOP + 60, FLOOR_BOTTOM - 15);
            double newHp = clamp(Double.parseDouble(p.getProperty("level3.hp", "100")), 1, 100);
            int newWave = clamp(Integer.parseInt(p.getProperty("level3.wave", "1")), 1, 4);
            int[] newStock = parseInts(p.getProperty("level3.stock"), START_STOCK);
            int[] newVials = parseInts(p.getProperty("level3.vials"), new int[4]);
            int[] newStabilized = parseInts(p.getProperty("level3.stabilized"), new int[4]);
            boolean[] newSeen = parseBools(p.getProperty("level3.seen"), new boolean[4]);
            boolean[] newRecipes = parseBools(p.getProperty("level3.recipes"), new boolean[4]);
            List<Enemy> loadedEnemies = new ArrayList<>();
            String enemyData = p.getProperty("level3.enemies", "");
            if (!enemyData.isEmpty()) for (String line : enemyData.split(";")) {
                String[] a = line.split(",", -1);
                // Existing 8-field save data still loads; new saves add speedFactor.
                if (a.length != 8 && a.length != 9 && a.length != 11)
                    throw new IllegalArgumentException("Bad enemy data");
                int type = Integer.parseInt(a[0]);
                if (type < 0 || type > 3) throw new IllegalArgumentException("Bad enemy type");
                Enemy z = new Enemy(type, Double.parseDouble(a[1]), Double.parseDouble(a[2]));
                z.hp = Integer.parseInt(a[3]); z.cooldown = Integer.parseInt(a[4]);
                z.flash = Integer.parseInt(a[5]); z.slowed = Integer.parseInt(a[6]);
                z.face = a[7];
                if (a.length >= 9) {
                    double factor = Double.parseDouble(a[8]);
                    if (!Double.isFinite(factor)) throw new IllegalArgumentException("Bad enemy speed");
                    z.speedFactor = clamp(factor, .80, 1.20);
                }
                if (a.length == 11) {
                    z.charge = clamp(Integer.parseInt(a[9]), 0, 30);
                    z.attackTicks = clamp(Integer.parseInt(a[10]), 0, 24);
                }
                loadedEnemies.add(z);
            }
            List<Projectile> loadedShots = new ArrayList<>();
            String projectileData = p.getProperty("level3.projectiles", "");
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
            List<EnemyShot> loadedEnemyShots = new ArrayList<>();
            String hostileData = p.getProperty("level3.enemyShots", "");
            if (!hostileData.isEmpty()) for (String line : hostileData.split(";")) {
                String[] a = line.split(",", -1);
                if (a.length != 6) throw new IllegalArgumentException("Bad enemy projectile");
                int type = Integer.parseInt(a[0]);
                if (type < 0 || type > 3) throw new IllegalArgumentException("Bad shot type");
                loadedEnemyShots.add(new EnemyShot(type, Double.parseDouble(a[1]),
                    Double.parseDouble(a[2]), Double.parseDouble(a[3]),
                    Double.parseDouble(a[4]), clamp(Integer.parseInt(a[5]), 0, 150)));
            }
            px = newX; py = newY; hp = newHp;
            wave = newWave; ticks = parseInt(p, "level3.ticks", 0);
            spawned = clamp(parseInt(p, "level3.spawned", 0), 0, quota());
            spawnTimer = parseInt(p, "level3.spawnTimer", 90);
            waveTimer = parseInt(p, "level3.waveTimer", 0);
            xp = parseInt(p, "level3.xp", 0);
            coins = parseInt(p, "level3.coins", 22);
            medkits = parseInt(p, "level3.medkits", 2);
            equipped = clamp(parseInt(p, "level3.equipped", 0), 0, 3);
            target = clamp(parseInt(p, "level3.target", 0), 0, 3);
            facing = p.getProperty("level3.facing", "right");
            System.arraycopy(newStock, 0, stock, 0, stock.length);
            System.arraycopy(newVials, 0, vials, 0, vials.length);
            System.arraycopy(newStabilized, 0, stabilized, 0, stabilized.length);
            System.arraycopy(newSeen, 0, seen, 0, seen.length);
            System.arraycopy(newRecipes, 0, unlockedRecipes, 0, unlockedRecipes.length);
            enemies.clear(); enemies.addAll(loadedEnemies);
            projectiles.clear(); projectiles.addAll(loadedShots);
            enemyShots.clear(); enemyShots.addAll(loadedEnemyShots);
            bossAuraTicks = 0;
            defeated.clear();
            camera = clamp(px - W * .38, 0, WORLD - W);
            paused = false; lab = false; journal = false; encounter = false; encountered = null; dead = false; won = false;
            GameplayBridge.restoreScientistProgress(xp, coins,
                parseInt(p, "level3.researchPoints", 0), hp, px, py);
            keys.clear();
            message("Level 3 progress restored. Continue your research.");
            repaint();
            return true;
        } catch (RuntimeException e) {
            System.err.println("CHEMION Level 3: Invalid save: " + e.getMessage());
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
        return GameplayBridge.detectRecipe(3, CHEMS, RECIPES, NAMES, selected);
    }
    private int maxBatch(int recipe) {
        return GameplayBridge.maxBatch(3, CHEMS, RECIPES, NAMES, stock, recipe);
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
        if (!GameplayBridge.craft(3, CHEMS, RECIPES, NAMES, stock, vials,
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
        if (paused || lab || journal || encounter || dead || won
                || (hudControlsAvailable() && (HUD_PAUSE.contains(x, y) || HUD_EXIT.contains(x, y)))
                || new Rectangle(568, 473, 181, 48).contains(x, y)
                || new Rectangle(760, 473, 184, 48).contains(x, y)) {
            AudioManager.playClick();
        }

        if (won) {
            if (new Rectangle(362, 325, 236, 38).contains(x, y)) advanceAfterWin();
            return;
        }
        if (dead) {
            if (new Rectangle(362, 325, 236, 38).contains(x, y)) reset();
            return;
        }
        if (paused) {
            if (RESUME.contains(x, y)) openPauseMenu();
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
        if (hudControlsAvailable() && (HUD_PAUSE.contains(x, y) || HUD_EXIT.contains(x, y))) {
            openPauseMenu(); return; // Same in-game purple menu as Level 1.
        }
        for (int i = 0; i < 4; i++) {
            int slotX = HOTBAR_X + i * (HOTBAR_SLOT_W + HOTBAR_GAP);
            if (new Rectangle(slotX, HOTBAR_Y, HOTBAR_SLOT_W, HOTBAR_SLOT_H).contains(x, y)) {
                equipPotion(i); repaint(); return;
            }
        }
        if (new Rectangle(568, 473, 181, 48).contains(x, y)) { toggleLab(); repaint(); return; }
        if (new Rectangle(760, 473, 184, 48).contains(x, y)) { toggleJournal(); repaint(); return; }
        if (y < 460) {
            Enemy clicked = null;
            for (Enemy z : enemies) {
                if (Math.hypot(z.x - camera - x, z.y - 34 - y) < 46
                        && Math.hypot(z.x - px, z.y - py) < 470) {
                    clicked = z; break;
                }
            }
            throwPotion(clicked);
        }
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
        g.drawImage(factory, -(int) camera, 0, null);
        drawFactoryEffects(g);
        drawActors(g);

        if (canPlay()) {
            healthEffects.drawVignette(g, W, H);
        }

        drawHUD(g);
        if (paused) drawPauseMenu(g);
        else if (encounter) drawEncounter(g);
        else if (lab) drawLab(g);
        else if (journal) drawJournal(g);
        else if (dead) drawEnd(g, "GAME OVER", "Your scientist was overwhelmed.", false);
        else if (won && defeated.isEmpty())
            drawEnd(g, "LEVEL 3 COMPLETE", "The Chemical Abomination has been stabilized!", true);
        g.dispose();
    }

    // A separate factory environment. Optional custom art is scaled once;
    // otherwise the complete, layered industrial pixel-art map is drawn in Java.
    private static BufferedImage loadFactoryBackground() {
        String[] candidates = {
            "assets/backgrounds/level3_factory.png",
            "assets/backgrounds/toxic_chemical_factory.png",
            "assets/backgrounds/level3.png"
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
                System.err.println("Factory background failed to load: " + e.getMessage());
            }
        }
        return buildFactory();
    }

    private static BufferedImage buildFactory() {
        BufferedImage image = new BufferedImage(WORLD, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setPaint(new GradientPaint(0, 0, new Color(10, 19, 35),
                                     0, H, new Color(30, 37, 51)));
        g.fillRect(0, 0, WORLD, H);

        // Distant industrial skyline through tall smoke-tinted factory windows.
        for (int s = 0; s < 4; s++) {
            int start = s * 790;
            g.setColor(new Color(14, 22, 36)); g.fillRect(start, 48, 790, 166);
            for (int x = start + 26; x < start + 790; x += 111) {
                g.setColor(new Color(29, 55, 69)); g.fillRect(x, 76, 91, 122);
                g.setPaint(new GradientPaint(x, 75, new Color(29, 71, 77, 150),
                    x, 196, new Color(10, 24, 39, 150)));
                g.fillRect(x + 3, 79, 85, 116);
                g.setColor(new Color(81, 112, 116));
                g.fillRect(x, 74, 4, 125); g.fillRect(x + 87, 74, 4, 125);
                g.fillRect(x, 74, 91, 4); g.fillRect(x, 137, 91, 3);
                g.setColor(new Color(16, 33, 47, 175));
                g.fillRect(x + 11, 142, 23, 56); g.fillRect(x + 49, 121, 22, 77);
                g.setColor(new Color(17, 29, 43));
                g.fillRect(x + 14, 117, 13, 81); g.fillRect(x + 53, 108, 13, 90);
            }
            // Heavy I-beams, exposed trusses and multi-level pipework.
            for (int x = start; x <= start + 790; x += 158) {
                g.setColor(new Color(46, 56, 70)); g.fillRect(x, 17, 15, 205);
                g.setColor(new Color(88, 103, 111)); g.fillRect(x + 2, 17, 3, 205);
                g.setColor(new Color(24, 30, 43)); g.fillRect(x + 12, 17, 3, 205);
                for (int y = 39; y < 211; y += 43) {
                    g.setColor(new Color(108, 120, 127)); g.fillRect(x - 3, y, 20, 4);
                }
            }
            g.setColor(new Color(26, 35, 50)); g.fillRect(start, 0, 790, 26);
            g.setColor(new Color(72, 88, 99)); g.fillRect(start, 25, 790, 6);
            g.setColor(new Color(22, 29, 43)); g.fillRect(start, 34, 790, 10);
            for (int x = start; x < start + 790; x += 72) {
                g.setColor(new Color(70, 85, 97));
                g.drawLine(x, 32, x + 37, 61);
                g.drawLine(x + 37, 61, x + 72, 32);
            }
            pipe(g, start, 61, 790, 11, new Color(69, 96, 105));
            pipe(g, start, 189, 790, 9, new Color(67, 88, 96));
            pipe(g, start + 41, 47, 9, 165, new Color(81, 104, 109));
            pipe(g, start + 728, 48, 9, 160, new Color(83, 106, 108));

            // Industrial walkway, grated platform, perspective floor and rails.
            g.setColor(new Color(18, 24, 35)); g.fillRect(start, 210, 790, 13);
            g.setColor(new Color(93, 100, 105)); g.fillRect(start, 218, 790, 3);
            for (int x = start; x < start + 790; x += 24) {
                g.setColor(new Color(57, 65, 75)); g.fillRect(x, 216, 10, 6);
            }
            g.setPaint(new GradientPaint(start, 224, new Color(45, 52, 64),
                start, 450, new Color(28, 32, 44)));
            g.fillRect(start, 224, 790, 226);
            g.setColor(new Color(61, 69, 80));
            for (int y = 248; y < 450; y += 32) g.fillRect(start, y, 790, 2);
            for (int x = start; x < start + 790; x += 52) {
                g.setColor(new Color(58, 66, 77)); g.fillRect(x, 225, 2, 224);
                g.setColor(new Color(32, 37, 51)); g.fillRect(x + 2, 225, 2, 224);
            }
            g.setColor(new Color(18, 22, 35)); g.fillRect(start, 449, 790, 91);
            g.setColor(new Color(86, 93, 99)); g.fillRect(start, 449, 790, 4);
            g.setColor(new Color(46, 55, 65)); g.fillRect(start, 459, 790, 7);
            for (int x = start + 10; x < start + 790; x += 34) {
                g.setColor((x / 34) % 2 == 0 ? new Color(188, 147, 60) : new Color(28, 30, 37));
                g.fillPolygon(new int[]{x, x + 17, x + 33, x + 16},
                    new int[]{452, 452, 459, 459}, 4);
            }

            // Four visually distinct zones, each with its own machinery.
            if (s == 0) {
                tank(g, start + 92, 111, 146, 171, new Color(67, 96, 104), new Color(93, 209, 166));
                tank(g, start + 274, 125, 125, 155, new Color(58, 88, 100), new Color(83, 180, 208));
                console(g, start + 467, 255, 181, 100, "INLET CONTROL");
                crate(g, start + 57, 349, 74, 61); crate(g, start + 137, 365, 65, 47);
            } else if (s == 1) {
                tank(g, start + 78, 120, 125, 157, new Color(71, 92, 104), new Color(141, 211, 106));
                tank(g, start + 242, 105, 150, 176, new Color(70, 92, 107), new Color(108, 219, 181));
                pipe(g, start + 390, 161, 160, 13, new Color(115, 104, 91));
                console(g, start + 503, 252, 186, 103, "MIXING PANEL");
                vat(g, start + 324, 356, 125, new Color(93, 210, 143));
            } else if (s == 2) {
                tank(g, start + 70, 119, 124, 164, new Color(82, 87, 102), new Color(241, 164, 79));
                tank(g, start + 232, 114, 126, 171, new Color(66, 84, 103), new Color(91, 198, 210));
                console(g, start + 470, 260, 200, 93, "PRESSURE MONITOR");
                pipe(g, start + 380, 92, 19, 174, new Color(132, 93, 81));
                for (int i = 0; i < 3; i++) {
                    g.setColor(new Color(220, 147, 72));
                    g.fillRect(start + 388, 115 + i * 43, 5, 7);
                }
            } else {
                tank(g, start + 65, 94, 175, 191, new Color(67, 73, 95), new Color(188, 95, 178));
                tank(g, start + 293, 111, 135, 170, new Color(58, 86, 102), new Color(102, 220, 189));
                console(g, start + 479, 260, 185, 94, "REACTOR LOCK");
                g.setColor(new Color(120, 57, 82, 135)); g.fillRect(start + 76, 298, 351, 6);
                for (int x = start + 89; x < start + 423; x += 37) {
                    g.setColor(new Color(170, 67, 91)); g.fillRect(x, 297, 7, 9);
                }
            }
            // Signage above the playable area: clear zone identity.
            String[] zones = {"01 / RECEIVING BAY", "02 / REAGENT MIXING", "03 / PRESSURE LINE", "04 / REACTOR CORE"};
            g.setColor(new Color(14, 24, 37)); g.fillRoundRect(start + 446, 89, 289, 35, 4, 4);
            g.setColor(new Color(93, 154, 157)); g.drawRoundRect(start + 446, 89, 289, 35, 4, 4);
            g.setColor(new Color(211, 226, 220)); g.setFont(new Font("Monospaced", Font.BOLD, 15));
            g.drawString(zones[s], start + 458, 112);
            // Hanging task lights and soft pools of cyan illumination.
            for (int lx = start + 95; lx < start + 790; lx += 310) {
                g.setColor(new Color(101, 111, 113)); g.fillRect(lx + 2, 29, 3, 74);
                g.setColor(new Color(30, 40, 52)); g.fillRect(lx - 17, 101, 38, 11);
                g.setColor(new Color(169, 219, 201)); g.fillRect(lx - 12, 112, 29, 4);
                g.setColor(new Color(80, 182, 171, 25));
                g.fillPolygon(new int[]{lx - 10, lx + 17, lx + 82, lx - 72},
                    new int[]{116, 116, 321, 321}, 4);
            }
            // Factory spill reflections and scattered bolts (decorative only).
            g.setColor(new Color(50, 149, 129, 65));
            g.fillOval(start + 236, 402, 170, 16);
            g.setColor(new Color(105, 189, 159, 48));
            g.fillOval(start + 246, 406, 135, 6);
            for (int k = 0; k < 12; k++) {
                int bx = start + 33 + ((k * 131) % 720);
                int by = 337 + ((k * 23) % 102);
                g.setColor(k % 2 == 0 ? new Color(88, 110, 112) : new Color(72, 80, 94));
                g.fillRect(bx, by, 4, 2);
            }
        }
        g.dispose();
        return image;
    }
    private static void pipe(Graphics2D g, int x, int y, int w, int h, Color color) {
        g.setColor(new Color(19, 27, 39)); g.fillRoundRect(x - 2, y - 2, w + 4, h + 4, 5, 5);
        g.setColor(color); g.fillRoundRect(x, y, w, h, 4, 4);
        g.setColor(new Color(181, 195, 186, 75));
        if (w > h) g.fillRect(x + 2, y + 2, w - 4, 2);
        else g.fillRect(x + 2, y + 2, 2, h - 4);
    }
    private static void tank(Graphics2D g, int x, int y, int w, int h, Color shell, Color liquid) {
        g.setColor(new Color(9, 17, 30, 110)); g.fillRoundRect(x + 7, y + 9, w, h, 25, 25);
        g.setPaint(new GradientPaint(x, y, shell.brighter(), x + w, y, shell.darker()));
        g.fillRoundRect(x, y, w, h, 25, 25);
        g.setColor(new Color(122, 140, 145)); g.drawRoundRect(x, y, w, h, 25, 25);
        g.setColor(new Color(38, 51, 63)); g.fillRect(x + 11, y + 24, w - 22, h - 55);
        g.setColor(liquid.darker()); g.fillRect(x + 18, y + h / 2, w - 36, h / 2 - 38);
        g.setColor(new Color(liquid.getRed(), liquid.getGreen(), liquid.getBlue(), 80));
        g.fillRect(x + 18, y + 31, w - 36, h / 2 - 31);
        g.setColor(new Color(198, 227, 214, 125)); g.fillRect(x + 25, y + 37, 7, h - 85);
        g.setColor(new Color(24, 34, 46)); g.fillRect(x - 7, y + h - 21, w + 14, 16);
        g.setColor(new Color(127, 139, 144)); g.drawRect(x - 7, y + h - 21, w + 14, 16);
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(174, 186, 183)); g.fillRect(x + 10 + i * (w - 20) / 4, y + h - 15, 4, 4);
        }
        pipe(g, x + w / 2 - 5, y - 25, 11, 28, shell);
        g.setColor(new Color(16, 24, 37)); g.fillRect(x + 13, y + h - 6, 13, 23);
        g.fillRect(x + w - 26, y + h - 6, 13, 23);
    }
    private static void console(Graphics2D g, int x, int y, int w, int h, String text) {
        g.setColor(new Color(13, 21, 35)); g.fillRoundRect(x, y, w, h, 5, 5);
        g.setColor(new Color(108, 125, 135)); g.drawRoundRect(x, y, w, h, 5, 5);
        g.setColor(new Color(26, 43, 53)); g.fillRect(x + 12, y + 12, w - 24, 44);
        g.setColor(new Color(94, 195, 169)); g.drawRect(x + 12, y + 12, w - 24, 44);
        for (int i = 0; i < 7; i++) {
            g.setColor(i % 3 == 0 ? new Color(213, 156, 89) : new Color(87, 192, 162));
            g.fillRect(x + 22 + i * 19, y + 29 - (i % 3) * 3, 11, 3);
        }
        g.setColor(new Color(211, 220, 210)); g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.drawString(text, x + 12, y + h - 18);
        for (int i = 0; i < 5; i++) {
            g.setColor(i == 3 ? new Color(218, 115, 92) : new Color(103, 180, 159));
            g.fillRect(x + 17 + i * 25, y + 65, 8, 5);
        }
    }
    private static void crate(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(27, 33, 45)); g.fillRect(x + 5, y + 5, w, h);
        g.setColor(new Color(91, 99, 100)); g.fillRect(x, y, w, h);
        g.setColor(new Color(139, 145, 128)); g.drawRect(x, y, w, h);
        g.setColor(new Color(48, 60, 66)); g.fillRect(x + 10, y + 8, w - 20, h - 16);
        g.setColor(new Color(177, 144, 79)); g.fillRect(x + 13, y + h / 2 - 4, w - 26, 8);
        g.setColor(new Color(33, 38, 42)); g.drawLine(x + 10, y + 8, x + w - 10, y + h - 8);
    }
    private static void vat(Graphics2D g, int x, int y, int w, Color liquid) {
        g.setColor(new Color(17, 24, 35)); g.fillRoundRect(x, y, w, 49, 9, 9);
        g.setColor(new Color(116, 133, 138)); g.drawRoundRect(x, y, w, 49, 9, 9);
        g.setColor(new Color(liquid.getRed(), liquid.getGreen(), liquid.getBlue(), 125));
        g.fillOval(x + 8, y + 3, w - 16, 15);
        g.setColor(new Color(74, 95, 100)); g.fillRect(x + 8, y + 15, w - 16, 27);
        g.setColor(new Color(140, 158, 155)); g.fillRect(x + 10, y + 35, w - 20, 3);
    }

    private void drawFactoryEffects(Graphics2D g) {
        // Animated warning LEDs and restrained steam, all behind the characters.
        for (int s = 0; s < 4; s++) {
            int wx = s * 790 + 725 - (int) camera;
            if (wx < -40 || wx > W + 40) continue;
            boolean blink = (ticks / 28 + s) % 2 == 0;
            g.setColor(blink ? new Color(230, 98, 91, 170) : new Color(106, 51, 64, 120));
            g.fillRect(wx, 152, 8, 8);
            if (blink) {
                g.setColor(new Color(225, 78, 76, 28)); g.fillOval(wx - 13, 139, 34, 34);
            }
            int vent = s * 790 + 406 - (int) camera;
            int rise = ticks % 70;
            g.setColor(new Color(119, 205, 193, Math.max(0, 48 - rise / 2)));
            g.fillOval(vent - 10 - rise / 6, 370 - rise, 29 + rise / 5, 12 + rise / 5);
        }
    }

    private void drawActors(Graphics2D g) {
        List<Enemy> sorted = new ArrayList<>(enemies);
        sorted.sort(Comparator.comparingDouble(z -> z.y));
        for (Defeated d : defeated) drawDefeated(g, d);
        for (Enemy z : sorted) drawEnemy(g, z);
        drawPlayer(g);

        if (canPlay()) {
            healthEffects.drawHealEffect(
                g,
                (int) (px - camera),
                (int) py - 35,
                45
            );
        }

        for (Projectile p : projectiles) {
            int x = (int) (p.x - camera), y = (int) p.y;
            g.setColor(VIAL_COLORS[p.type]);
            g.fillRoundRect(x - 6, y - 9, 13, 16, 3, 3);
            g.setColor(WHITE); g.drawRect(x - 5, y - 8, 11, 14);
            g.setColor(new Color(80, 59, 55)); g.fillRect(x - 3, y - 12, 7, 5);
        }
        // Distinct hostile projectiles: emerald spit vs violet boss discharge.
        for (EnemyShot shot : enemyShots) {
            int x = (int) (shot.x - camera), y = (int) shot.y;
            Color glow = shot.type == 3 ? new Color(198, 91, 214) : new Color(95, 221, 133);
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 47));
            g.fillOval(x - 13, y - 13, 26, 26);
            g.setColor(glow); g.fillOval(x - 5, y - 5, 10, 10);
            g.setColor(new Color(242, 240, 234)); g.fillRect(x - 1, y - 2, 3, 3);
        }
    }
    private void drawPlayer(Graphics2D g) {
        int x = (int) (px - camera), y = (int) py;
        g.setColor(new Color(6, 7, 13, 130));
        g.fillOval(x - 19, y - 8, 38, 12);
        boolean moving = keys.contains(KeyEvent.VK_W) || keys.contains(KeyEvent.VK_A)
                || keys.contains(KeyEvent.VK_S) || keys.contains(KeyEvent.VK_D)
                || keys.contains(KeyEvent.VK_UP) || keys.contains(KeyEvent.VK_DOWN)
                || keys.contains(KeyEvent.VK_LEFT) || keys.contains(KeyEvent.VK_RIGHT);
        boolean running = moving && keys.contains(KeyEvent.VK_SHIFT);
        // Show the ORIGINAL run/walk/throw artwork, with no artificial lean,
        // duplicate silhouettes, added trails, or hurt sprite animations.
        String moveAction = running ? "run" : "walk";
        // No death or hurt sprites: keep walk/run/throw and tint red on damage.
        String action = throwCooldown > 14 && !dead ? "throw potion" : moveAction;
        int frame = moving ? ticks / (running ? 5 : 10) : 0;
        BufferedImage sprite = sprites.get(true, scientist, action, facing, frame);
        boolean red = hurtTicks > 0 && hurtTicks % 8 >= 4;
        if (sprite != null) {
            drawSprite(g, red ? sprites.red(sprite) : sprite, x, y, 65, 1f, 59);
        } else {
            // Pixel scientist fallback: lab coat, hair, gloves, boots and a vial.
            // If damaged, EVERY part flashes solid red (no hurt/death sprites).
            Color flash = new Color(247, 34, 52);
            Color coat = red ? flash : new Color(202, 188, 229);
            Color shirt = red ? flash : new Color(118, 83, 171);
            Color skin = red ? flash : new Color(218, 171, 148);
            Color hair = red ? flash : new Color(58, 43, 71);
            Color boot = red ? flash : new Color(44, 43, 66);
            int bob = moving && (ticks / 8) % 2 == 0 ? 1 : 0;
            g.setColor(boot); g.fillRect(x - 10, y - 15, 8, 15 - bob);
            g.fillRect(x + 3, y - 15, 8, 15 + bob);
            g.setColor(shirt); g.fillRect(x - 10, y - 48, 20, 29);
            g.setColor(coat); g.fillRect(x - 15, y - 45, 10, 32);
            g.fillRect(x + 5, y - 45, 10, 32);
            g.setColor(red ? flash : new Color(150, 126, 195));
            g.fillRect(x - 15, y - 17, 30, 5);
            g.setColor(coat); g.fillRect(x - 21, y - 42, 7, 24);
            g.fillRect(x + 14, y - 42, 7, 24);
            g.setColor(skin); g.fillRect(x - 21, y - 19, 7, 6);
            g.fillRect(x + 14, y - 19, 7, 6);
            g.fillRect(x - 10, y - 65, 20, 21);
            g.setColor(hair); g.fillRect(x - 12, y - 68, 24, 9);
            g.fillRect(x - 12, y - 63, 5, 10);
            g.setColor(red ? flash : new Color(54, 39, 70));
            if (!facing.equals("up")) {
                g.fillRect(x - 6, y - 55, 3, 3);
                g.fillRect(x + 4, y - 55, 3, 3);
            }
            g.setColor(red ? flash : new Color(109, 222, 201));
            g.fillRect(x + 17, y - 25, 5, 7);
        }
        g.setColor(red ? new Color(255, 60, 77, 220) : new Color(179, 148, 234, 130));
        g.drawOval(x - 15, y - 5, 30, 9);
    }
    private void drawEnemy(Graphics2D g, Enemy z) {
        int x = (int) (z.x - camera), y = (int) z.y;
        if (x < -120 || x > W + 120) return;
        boolean boss = z.type == 3;
        g.setColor(new Color(5, 8, 15, 150));
        g.fillOval(x - (boss ? 35 : 20), y - 7, boss ? 70 : 40, 13);

        // A single subtle, fast red glow when the boss is first discovered.
        if (boss && bossAuraTicks > 0) {
            float fade = bossAuraTicks / 36f;
            int pulse = (int) (4 * Math.sin((36 - bossAuraTicks) * 0.45));
            g.setColor(new Color(226, 53, 80, (int) (31 * fade)));
            g.fillOval(x - 61 - pulse, y - 123 - pulse, 122 + pulse * 2, 132 + pulse * 2);
            g.setColor(new Color(238, 75, 94, (int) (85 * fade)));
            g.drawOval(x - 49 - pulse, y - 112 - pulse, 98 + pulse * 2, 114 + pulse * 2);
        }
        // Explosive zombie clearly telegraphs its charge. The ring is harmless
        // until the countdown ends, so the player can dodge it.
        if (z.type == 2 && z.charge > 0) {
            int r = 67 + (25 - z.charge) / 2;
            g.setColor(new Color(255, 152, 64, 37));
            g.fillOval(x - r, y - r / 3, 2 * r, r * 2 / 3);
            g.setColor(new Color(250, 172, 76, 150));
            g.drawOval(x - r, y - r / 3, 2 * r, r * 2 / 3);
        }
        boolean attacking = z.attackTicks > 0 && z.flash <= 0;
        String action = attacking ? "attack" : z.type == 2 ? "run" : "walk";
        int frame = attacking ? Math.min(3, (24 - z.attackTicks) / 6)
                : ticks / (z.type == 2 ? 8 : z.type == 3 ? 13 : 11);
        BufferedImage sprite = sprites.get(false, ENEMY_IDS[z.type], action, z.face, frame);
        boolean red = z.flash > 0 && z.flash % 8 >= 4;
        if (sprite != null) {
            drawSprite(g, red ? sprites.red(sprite) : sprite, x, y, boss ? 102 : z.type == 0 ? 75 : 68, 1f);
        } else drawFallbackEnemy(g, z, x, y, red);
        // Same visual style as Levels 1 and 2, plus a compact boss HP bar.
        if (boss) {
            int barW = 90;
            g.setColor(new Color(24, 16, 34, 220)); g.fillRect(x - barW / 2, y - 118, barW, 7);
            g.setColor(new Color(212, 81, 99));
            g.fillRect(x - barW / 2 + 1, y - 117, (int) ((barW - 2) * clamp(z.hp / 5.0, 0, 1)), 5);
            g.setColor(new Color(197, 155, 202)); g.drawRect(x - barW / 2, y - 118, barW, 7);
        }
    }
    private BufferedImage fallbackShatterPose(Enemy z) {
        // If an asset is missing, even the Java fallback breaks into its own pixels.
        BufferedImage fallback = new BufferedImage(190, 170, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fallback.createGraphics();
        g.translate(95, 154);
        drawFallbackEnemyBase(g, z, 0, 0);
        g.dispose();
        return SpriteLoader.crop(fallback);
    }
    private void drawDefeated(Graphics2D g, Defeated d) {
        int x = (int) (d.x - camera), y = (int) d.y;
        if (x < -140 || x > W + 140) return;
        // Instant pixel shatter: pieces scatter outward, drop, and fade in < 0.3s.
        float opacity = Math.max(0f, 1f - d.age / (float) SHATTER_TICKS);
        Graphics2D pieces = (Graphics2D) g.create();
        pieces.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        for (Shard shard : d.shards) {
            int sx = (int) Math.round(x - d.width / 2.0 + shard.localX + shard.vx * d.age);
            int sy = (int) Math.round(y - d.height + shard.localY + shard.vy * d.age
                    + .17 * d.age * d.age);
            pieces.drawImage(shard.image, sx, sy, null);
        }
        pieces.dispose();
    }

    private static void drawSprite(Graphics2D g, BufferedImage img, int centerX, int footY, int height, float alpha) {
        drawSprite(g, img, centerX, footY, height, alpha, height * 2);
    }
    // Scientist sprite has the same 65px height and 59px max width as Level 1.
    private static void drawSprite(Graphics2D g, BufferedImage img, int centerX, int footY,
                                   int height, float alpha, int maxWidth) {
        int width = Math.max(1, (int) Math.round(height * img.getWidth() / (double) Math.max(1, img.getHeight())));
        width = Math.min(width, maxWidth);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(img, centerX - width / 2, footY - height, width, height, null);
        g.setComposite(old);
    }
    private void drawFallbackEnemy(Graphics2D g, Enemy z, int x, int y, boolean red) {
        // Render the whole fallback to a temporary image so the head, arms,
        // and legs ALL turn red, not just the body.
        if (red) {
            BufferedImage image = new BufferedImage(190, 170, BufferedImage.TYPE_INT_ARGB);
            Graphics2D fg = image.createGraphics();
            fg.translate(95 - x, 154 - y);
            drawFallbackEnemyBase(fg, z, x, y);
            fg.dispose();
            BufferedImage tinted = new BufferedImage(image.getWidth(), image.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int iy = 0; iy < image.getHeight(); iy++) {
                for (int ix = 0; ix < image.getWidth(); ix++) {
                    int alpha = (image.getRGB(ix, iy) >>> 24) & 255;
                    if (alpha != 0) tinted.setRGB(ix, iy,
                            (alpha << 24) | (247 << 16) | (34 << 8) | 52);
                }
            }
            g.drawImage(tinted, x - 95, y - 154, null);
        } else drawFallbackEnemyBase(g, z, x, y);
    }
    private void drawFallbackEnemyBase(Graphics2D g, Enemy z, int x, int y) {
        // Distinct original pixel silhouettes. All four are drawn without
        // external assets if the player's sprite PNGs are not present.
        int h = z.type == 3 ? 98 : z.type == 0 ? 73 : 66;
        int top = y - h;
        Color skin = new Color(133, 164, 142);
        if (z.type == 0) { // TOXIC BRUTE: wide shoulders, waste tank and huge fists.
            g.setColor(new Color(43, 66, 65)); g.fillRect(x - 19, top + 29, 38, 32);
            g.setColor(new Color(90, 117, 95)); g.fillRect(x - 26, top + 27, 52, 28);
            g.setColor(new Color(45, 76, 69)); g.fillRect(x - 12, top + 54, 11, 19);
            g.fillRect(x + 4, top + 54, 11, 19);
            g.setColor(new Color(58, 86, 76)); g.fillRect(x - 36, top + 32, 13, 25);
            g.fillRect(x + 23, top + 32, 13, 25);
            g.setColor(new Color(125, 161, 117)); g.fillRect(x - 37, top + 53, 15, 12);
            g.fillRect(x + 22, top + 53, 15, 12);
            g.setColor(skin); g.fillRect(x - 13, top + 8, 26, 21);
            g.setColor(new Color(47, 64, 57)); g.fillRect(x - 15, top + 5, 30, 9);
            g.setColor(new Color(210, 211, 88)); g.fillRect(x - 9, top + 17, 6, 4);
            g.fillRect(x + 4, top + 17, 6, 4);
            g.setColor(new Color(125, 208, 99)); g.fillRect(x - 17, top + 42, 6, 17);
            g.setColor(new Color(217, 180, 73)); g.fillRect(x - 22, top + 40, 44, 5);
        } else if (z.type == 1) { // VENOM SPITTER: narrow hunched mask and acid sacs.
            g.setColor(new Color(37, 57, 65)); g.fillRect(x - 9, top + 48, 7, 18);
            g.fillRect(x + 5, top + 48, 7, 18);
            g.setColor(new Color(65, 110, 97)); g.fillRect(x - 15, top + 25, 30, 26);
            g.setColor(new Color(109, 193, 122)); g.fillRect(x - 18, top + 30, 8, 13);
            g.fillRect(x + 10, top + 30, 8, 13);
            g.setColor(new Color(55, 77, 77)); g.fillRect(x - 23, top + 29, 8, 28);
            g.fillRect(x + 15, top + 29, 8, 28);
            g.setColor(new Color(137, 171, 150)); g.fillRect(x - 9, top + 6, 18, 21);
            g.setColor(new Color(37, 66, 65)); g.fillRect(x - 12, top + 15, 24, 9);
            g.setColor(new Color(111, 236, 139)); g.fillRect(x - 8, top + 17, 16, 4);
            g.setColor(new Color(78, 185, 117)); g.fillRect(x + 21, top + 32, 9, 10);
            g.fillRect(x + 24, top + 43, 5, 5);
        } else if (z.type == 2) { // EXPLOSIVE ZOMBIE: orange reactor core and warning cables.
            g.setColor(new Color(57, 52, 65)); g.fillRect(x - 12, top + 47, 9, 19);
            g.fillRect(x + 4, top + 47, 9, 19);
            g.setColor(new Color(91, 78, 78)); g.fillRect(x - 20, top + 23, 40, 27);
            g.setColor(new Color(47, 56, 63)); g.fillRect(x - 27, top + 26, 9, 25);
            g.fillRect(x + 18, top + 26, 9, 25);
            g.setColor(new Color(234, 155, 65)); g.fillRect(x - 10, top + 30, 20, 15);
            g.setColor(new Color(250, 205, 100)); g.fillRect(x - 5, top + 34, 10, 7);
            g.setColor(new Color(141, 157, 142)); g.fillRect(x - 10, top + 5, 20, 21);
            g.setColor(new Color(73, 55, 60)); g.fillRect(x - 13, top + 10, 26, 7);
            g.setColor(new Color(245, 106, 73)); g.fillRect(x - 9, top + 13, 6, 3);
            g.fillRect(x + 4, top + 13, 6, 3);
            g.setColor(new Color(209, 134, 69)); g.fillRect(x - 23, top + 25, 4, 20);
            g.fillRect(x + 19, top + 25, 4, 20);
        } else { // CHEMICAL ABOMINATION: asymmetrical horns, tanks and violet core.
            g.setColor(new Color(33, 34, 53)); g.fillRect(x - 17, top + 74, 13, 24);
            g.fillRect(x + 5, top + 74, 13, 24);
            g.setColor(new Color(57, 50, 78)); g.fillRect(x - 30, top + 34, 60, 45);
            g.setColor(new Color(81, 64, 99)); g.fillRect(x - 34, top + 38, 68, 27);
            g.setColor(new Color(44, 49, 64)); g.fillRect(x - 43, top + 43, 11, 38);
            g.fillRect(x + 32, top + 43, 11, 38);
            g.setColor(new Color(141, 167, 148)); g.fillRect(x - 17, top + 13, 34, 26);
            g.setColor(new Color(42, 42, 63));
            g.fillPolygon(new int[]{x - 20, x - 29, x - 12},
                          new int[]{top + 20, top - 4, top + 12}, 3);
            g.fillPolygon(new int[]{x + 20, x + 31, x + 11},
                          new int[]{top + 20, top - 8, top + 13}, 3);
            g.setColor(new Color(235, 93, 112)); g.fillRect(x - 11, top + 24, 8, 5);
            g.fillRect(x + 5, top + 24, 8, 5);
            g.setColor(new Color(176, 106, 217)); g.fillRect(x - 13, top + 49, 26, 17);
            g.setColor(new Color(238, 185, 242)); g.fillRect(x - 5, top + 54, 10, 7);
            g.setColor(new Color(100, 220, 169)); g.fillRect(x - 34, top + 45, 7, 25);
            g.fillRect(x + 27, top + 44, 7, 27);
            g.setColor(new Color(170, 121, 188)); g.fillRect(x - 47, top + 77, 13, 7);
            g.fillRect(x + 34, top + 77, 13, 7);
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
        label(g, "FACTORY SECTOR", 413, 33, 10, MUTED, true);
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
        label(g, "WASD move  |  SHIFT run  |  1-4 select  |  Q switch  |  SPACE throw  |  H heal",
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
        label(g, "Fictional in-game recipes only - NOT real-world chemical or medical advice.", 210, 478, 10, MUTED, false);
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
        label(g, "OBSERVATION LOG - LEVEL 3", 210, 128, 14, PURPLE, true);
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
        label(g, "LEVEL 3 / TOXIC CHEMICAL FACTORY", 611, 59, 10, MUTED, true);
        button(g, 882, 34, 30, 29, "X", new Color(102, 63, 138));
        for (int i = 0; i < 4; i++)
            button(g, 56 + i * 213, 101, 201, 33, TABS[i], i == journalTab ? new Color(111, 70, 163) : new Color(48, 38, 69));
        if (journalTab == 2) drawJournalZombies(g);
        else if (journalTab == 1) {
            label(g, "PERIODIC TABLE - RELEVANT ELEMENTS", 62, 169, 14, PURPLE, true);
            String[] elements = {"H / Hydrogen", "O / Oxygen", "Na / Sodium", "C / Carbon", "S / Sulfur", "Si / Silicon", "K / Potassium", "Ca / Calcium"};
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
            wrap(g, "Research unlocks when this infected worker first enters the factory battlefield.",
                    345, 225, 523, 12, 19, WHITE);
            return;
        }
        label(g, NAMES[i].toUpperCase(Locale.ROOT), 345, 191, 16, PURPLE, true);
        label(g, "TYPE: " + (i == 3 ? "Experimental Boss" : "Factory Mutation"), 345, 220, 11, WHITE, true);
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
        // Cache red-tinted frames so hurt flashing does not allocate every tick.
        private final Map<BufferedImage, BufferedImage> redCache = new IdentityHashMap<>();
        private final Set<String> missing = new HashSet<>();
        private final Set<String> missingRunNotices = new HashSet<>();

        // Strong solid-red damage sprite: keep the exact original silhouette,
        // transparency and pixel edges, but never display a hurt animation.
        BufferedImage red(BufferedImage img) {
            BufferedImage result = redCache.get(img);
            if (result != null) return result;
            result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int c = img.getRGB(x, y);
                    int alpha = (c >>> 24) & 255;
                    int r = 247;
                    int green = 34;
                    int blue = 52;
                    result.setRGB(x, y, (alpha << 24) | (r << 16) | (green << 8) | blue);
                }
            }
            redCache.put(img, result);
            return result;
        }
        private final File assets;
        SpriteLoader() {
            File root = new File("assets");
            if (!root.isDirectory()) root = new File("../assets");
            assets = root;
        }
        BufferedImage get(boolean player, String name, String action, String direction, int frame) {
            String key = (player ? "characters/" : "enemies/level3/") + name;
            BufferedImage[] frames = load(key, name, action, direction);
            if (frames == null && player && action.equals("run")
                    && missingRunNotices.add(name + ":" + direction)) {
                // Log only once per direction, never once per animation tick.
                System.err.println("CHEMION Level 3: RUN sprite not found for " + name
                        + " (" + direction + "). Check assets/characters/<scientist>/run/.");
            }
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
            if (!folder.isDirectory() && key.startsWith("enemies/level3/")) {
                File enemyRoot = new File(assets, "enemies/level3");
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
            // Also accept Level 3 PNGs stored directly under the level folder,
            // or in assets/zombies/level3/<name> instead of assets/enemies/level3.
            if (files.isEmpty() && key.startsWith("enemies/level3/")) {
                for (String rootName : new String[]{"enemies/level3", "zombies/level3", "level3"}) {
                    File root = new File(assets, rootName);
                    List<File> found = new ArrayList<>();
                    findPng(root, found, 3);
                    for (File f : found) {
                        String relative = norm(f.getAbsolutePath().substring(root.getAbsolutePath().length()));
                        if (relative.contains(n) || relative.contains(n + "zombie")) files.add(f);
                    }
                }
            }
            File best = null; int bestScore = Integer.MIN_VALUE;
            for (File file : files) {
                String filename = norm(file.getName().replaceFirst("(?i)\\.png$", ""));
                String parent = norm(file.getParentFile().getName());
                boolean matchingDirection = filename.endsWith(dir)
                        || filename.endsWith(dir.equals("down") ? "front" : dir.equals("up") ? "back" : dir)
                        || filename.endsWith(dir.equals("down") ? "forward" : dir.equals("up") ? "backward" : dir);
                if (!matchingDirection) continue;
                // Support the user's original run sprites in run/, running/ or sprint/.
                boolean matchingAction = filename.contains(act) || parent.equals(act)
                        || (action.equals("run") && (filename.contains("sprint")
                                || parent.startsWith("run") || parent.startsWith("sprint")));
                if (!matchingAction) continue;
                int score = (filename.contains(n) ? 10 : 0)
                        + (filename.contains(act) ? 20 : 0) + (parent.equals(act) ? 8 : 0)
                        + (parent.startsWith("run") && action.equals("run") ? 6 : 0);
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
                System.err.println("Level 3 sprite: " + best + ": " + e.getMessage());
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

            // Detect actual frame count from separate sprite silhouettes.
            // CHEMION assets can contain FOUR or FIVE (or more) poses in one row.
            // Never hardcode 4: that cuts a 5-pose sheet across two characters.
            boolean hasTransparency = false;
            for (int x = 0; x < w && !hasTransparency; x += Math.max(1, w / 60)) {
                for (int y = 0; y < h; y += Math.max(1, h / 30)) {
                    if ((sheet.getRGB(x, y) >>> 24) < 24) {
                        hasTransparency = true; break;
                    }
                }
            }
            // Opaque sheets with a plain background can also be segmented.
            int bg = sheet.getRGB(0, 0);
            boolean plainBackground = !hasTransparency
                    && closeColor(bg, sheet.getRGB(w - 1, 0))
                    && closeColor(bg, sheet.getRGB(0, h - 1))
                    && closeColor(bg, sheet.getRGB(w - 1, h - 1));

            boolean[] occupied = new boolean[w];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int pixel = sheet.getRGB(x, y);
                    boolean ink = hasTransparency ? (pixel >>> 24) > 24
                            : plainBackground ? !closeColor(pixel, bg)
                            : (pixel >>> 24) > 24;
                    if (ink) { occupied[x] = true; break; }
                }
            }
            // Reconnect tiny gaps within one pose (e.g., between a hand and coat).
            int bridge = Math.max(2, Math.min(6, h / 28));
            int last = -1;
            for (int x = 0; x < w; x++) if (occupied[x]) {
                if (last >= 0 && x - last - 1 <= bridge)
                    for (int k = last + 1; k < x; k++) occupied[k] = true;
                last = x;
            }
            List<int[]> regions = new ArrayList<>();
            int left = -1;
            for (int x = 0; x <= w; x++) {
                boolean on = x < w && occupied[x];
                if (on && left < 0) left = x;
                if (!on && left >= 0) {
                    regions.add(new int[] {left, x - 1});
                    left = -1;
                }
            }
            int widest = 0;
            for (int[] region : regions) widest = Math.max(widest, region[1] - region[0] + 1);
            int minWidth = Math.max(7, (int) Math.ceil(widest * .20));
            List<int[]> poses = new ArrayList<>();
            for (int[] region : regions)
                if (region[1] - region[0] + 1 >= minWidth) poses.add(region);

            if (poses.size() >= 2 && poses.size() <= 10) {
                // Split in the EMPTY SPACE between poses, not at arbitrary quarters.
                int[] cuts = new int[poses.size() + 1];
                cuts[0] = 0; cuts[poses.size()] = w;
                for (int i = 1; i < poses.size(); i++)
                    cuts[i] = (poses.get(i - 1)[1] + poses.get(i)[0] + 1) / 2;
                return splitAt(sheet, cuts);
            }

            // If a sheet has no usable transparent/solid-color gaps, never draw
            // the ENTIRE strip as one giant character. Use a conservative grid
            // fallback, and report it so that irregular sheets can be corrected.
            int count = w >= h * 3 ? 4 : 2;
            System.err.println("CHEMION sprite: could not detect frame gaps in "
                    + w + "x" + h + " sheet; using " + count + " equal cells.");
            int[] cuts = new int[count + 1];
            for (int i = 0; i <= count; i++) cuts[i] = (int) ((long) i * w / count);
            return splitAt(sheet, cuts);
        }
        private static boolean closeColor(int a, int b) {
            int ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
            int br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
            return Math.abs(ar - br) + Math.abs(ag - bg) + Math.abs(ab - bb) < 45;
        }
        private static BufferedImage[] splitAt(BufferedImage sheet, int[] cuts) {
            int count = cuts.length - 1;
            BufferedImage[] frames = new BufferedImage[count];
            int maxW = 1, maxH = 1;
            for (int i = 0; i < count; i++) {
                frames[i] = crop(sheet.getSubimage(cuts[i], 0,
                        Math.max(1, cuts[i + 1] - cuts[i]), sheet.getHeight()));
                maxW = Math.max(maxW, frames[i].getWidth());
                maxH = Math.max(maxH, frames[i].getHeight());
            }
            // All frames use the same canvas dimensions and foot alignment.
            for (int i = 0; i < count; i++) {
                BufferedImage aligned = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = aligned.createGraphics();
                g.drawImage(frames[i], (maxW - frames[i].getWidth()) / 2,
                        maxH - frames[i].getHeight(), null);
                g.dispose();
                frames[i] = aligned;
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
