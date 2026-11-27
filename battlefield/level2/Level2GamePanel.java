package battlefield.level2;

import battlefield.level1.SaveManager;
import game.AudioManager;
import game.HealthEffects;
import game.GameManager;
import game.GameplayBridge;
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
    // Shared visual language with Level 1: identical HUD, menu, and card colors.
    private static final Color WHITE = new Color(242, 234, 255);
    private static final Color PURPLE = new Color(204, 170, 251);
    private static final Color MUTED = new Color(184, 170, 204);
    private static final Color TEAL = new Color(114, 214, 200);
    private static final Color GOLD = new Color(244, 178, 83);
    private static final Color RED = new Color(242, 123, 149);
    private static final Color[] VIAL_COLORS = {
        new Color(110, 216, 157), new Color(127, 187, 243),
        new Color(206, 159, 241), new Color(244, 185, 116)
    };
    private static final String[] ENEMY_IDS = {
        "acid_zombie", "patient_zero", "toxic_zombie", "plague_doctor"
    };
    // Per-zombie movement speeds (pixels per 16ms tick). No wave-based speed increase.
    // Acid: slow/corroded; Patient Zero: agile; Toxic: heavy/sluggish; Doctor: deliberate boss.
    private static final double[] ENEMY_BASE_SPEED = {0.43, 0.78, 0.34, 0.50};
    private static final int SHATTER_TICKS = 17; // 272 ms at 60 FPS; no zombie death sprites
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
    private static final int[] PRICES = {4, 2, 7, 2, 3, 5, 4, 4};
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
    private final HealthEffects healthEffects = new HealthEffects();
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
    private final BufferedImage hospital = buildHospital();

    private double px = 132, py = 369, camera, hp = 100;
    private int ticks, wave = 1, spawned, spawnTimer = 90, waveTimer;
    private int xp, coins = 65, medkits = 2, equipped, target, batchQty = 1;
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
        // Slight individual variation, saved so Continue Game preserves each enemy's pace.
        double speedFactor = 1.0;
        String face = "left";
        Enemy(int type, double x, double y) {
            this.type = type; this.x = x; this.y = y;
            this.hp = type == 3 ? 4 : type == 0 ? 1 : 2;
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
        z.speedFactor = .84 + rng.nextDouble() * .32; // Individual pace: 84%-116%; no wave multiplier
        enemies.add(z);
        spawned++; spawnTimer = type == 3 ? 170 : 125;
    }

    private void updateGame() {

    healthEffects.update(hp, 100);

    if (paused || lab || journal || encounter || dead || won) {
        AudioManager.stopZombieSounds();
        AudioManager.stopHeartbeat();
    } else {
        AudioManager.updateHeartbeat(hp, 100);
    }

    // Same overlay priority as Level 1: paused means the game really stops.
    if (paused) { repaint(); return; }


        if (lab) {
            if (labTab != 3 && --labTicks <= 0) {
                lab = false; Arrays.fill(selected, false);
                message("Crafting time expired. Open the lab to retry.");
            }
            repaint(); return;
        }
        if (dead || won || encounter || journal) {
            // Let the boss finish shattering before the victory screen appears.
            if (won) defeated.removeIf(d -> ++d.age >= SHATTER_TICKS);
            repaint(); return;
        }
        ticks++;
        if (noticeTicks > 0) noticeTicks--;
        defeated.removeIf(d -> ++d.age >= SHATTER_TICKS);
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
            double speed = keys.contains(KeyEvent.VK_SHIFT) ? 2.75 : 2.15; // Controlled sprint, not too fast
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
    private void updateEnemies() {
        for (Enemy z : new ArrayList<>(enemies)) {
            if (z.flash > 0) z.flash--;
            if (z.cooldown > 0) z.cooldown--;
            if (z.slowed > 0) z.slowed--;
            double dx = px - z.x, dy = py - z.y;
            double distance = Math.hypot(dx, dy);
            if (distance < 900) {
            AudioManager.playLevel2ZombieGrowl(z.type);
}
            if (distance < 450 && !seen[z.type]) {
                seen[z.type] = true;
                GameplayBridge.scan(2, z.type, CHEMS[RECIPES[z.type][0]]);
                encounterType = z.type;
                encountered = z;
                encounter = true;
                AudioManager.stopZombieSounds();
                keys.clear();
                message("NEW RESEARCH: " + NAMES[z.type]);
                return;
            }
            if (distance > 43) {
                double speed = ENEMY_BASE_SPEED[z.type] * z.speedFactor;
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
                p.enemy.flash = 28; // red immediately on impact, including a final hit

                    if (p.enemy.hp > 0) {
                        AudioManager.playZombieDamage();
                    }

                    if (p.enemy.hp <= 0) stabilize(p.enemy);
                    else message("Treatment effective. " + NAMES[p.type] + " is weakening.");
                }
            }
        }
    }
    private void stabilize(Enemy z) {
        enemies.remove(z);
        BufferedImage pose = sprites.get(false, ENEMY_IDS[z.type],
                z.type == 1 ? "run" : "walk", z.face, ticks / 9);
        if (pose == null) pose = fallbackShatterPose(z);
        defeated.add(new Defeated(z, pose, rng));
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
        xp = 0; coins = 65; medkits = 2; equipped = 0; target = 0; batchQty = 1;
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
                    + z.cooldown + "," + z.flash + "," + z.slowed + "," + z.face
                    + "," + z.speedFactor);
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
                // Existing 8-field save data still loads; new saves add speedFactor.
                if (a.length != 8 && a.length != 9) throw new IllegalArgumentException("Bad enemy data");
                int type = Integer.parseInt(a[0]);
                if (type < 0 || type > 3) throw new IllegalArgumentException("Bad enemy type");
                Enemy z = new Enemy(type, Double.parseDouble(a[1]), Double.parseDouble(a[2]));
                z.hp = Integer.parseInt(a[3]); z.cooldown = Integer.parseInt(a[4]);
                z.flash = Integer.parseInt(a[5]); z.slowed = Integer.parseInt(a[6]);
                z.face = a[7];
                if (a.length == 9) {
                    double factor = Double.parseDouble(a[8]);
                    if (!Double.isFinite(factor)) throw new IllegalArgumentException("Bad enemy speed");
                    z.speedFactor = clamp(factor, .80, 1.20);
                }
                loadedEnemies.add(z);
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
            coins = parseInt(p, "level2.coins", 65);
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
    private int askQuantity(String title, int max, int initial, int price) {
        if (max < 1) return 0;
        JTextField field = new JTextField(String.valueOf(Math.min(max, Math.max(1, initial))), 10);
        JLabel total = new JLabel();
        Runnable refresh = () -> {
            try { int n = Integer.parseInt(field.getText().trim());
                total.setText(n >= 1 && n <= max ? (price > 0 ? "Total: " + ((long)n * price) + " coins" : "Potions: " + n) : "Enter 1 to " + max);
            } catch (NumberFormatException ex) { total.setText("Enter a whole number."); }
        };
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
        });
        refresh.run();
        Color bg = new Color(32, 24, 48), accent = new Color(101, 61, 153);
        Font font = new Font("Monospaced", Font.BOLD, 14);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new GridLayout(0, 1, 0, 12));
        content.setBackground(bg);
        content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PURPLE), BorderFactory.createEmptyBorder(22, 24, 22, 24)));
        JLabel heading = new JLabel(title.toUpperCase(Locale.ROOT));
        JLabel range = new JLabel("Quantity (1 - " + max + ")");
        for (JLabel l : new JLabel[]{heading, range, total}) { l.setFont(font); l.setForeground(PURPLE); }
        total.setForeground(GOLD);
        field.setFont(font); field.setBackground(new Color(49, 36, 70)); field.setForeground(WHITE); field.setCaretColor(WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PURPLE), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JButton confirm = new JButton("CONFIRM"), cancel = new JButton("CANCEL");
        for (JButton button : new JButton[]{confirm, cancel}) { button.setFont(font); button.setBackground(accent); button.setForeground(WHITE); button.setFocusPainted(false); button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PURPLE), BorderFactory.createEmptyBorder(9, 12, 9, 12))); }
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0)); actions.setOpaque(false); actions.add(confirm); actions.add(cancel);
        content.add(heading); content.add(range); content.add(field); content.add(total); content.add(actions);
        int[] result = {0};
        confirm.addActionListener(e -> { try { int n = Integer.parseInt(field.getText().trim()); if (n >= 1 && n <= max) { result[0] = n; dialog.dispose(); return; } } catch (NumberFormatException ex) { } total.setText("Enter a whole number from 1 to " + max); });
        cancel.addActionListener(e -> dialog.dispose());
        dialog.setContentPane(content); dialog.getRootPane().setDefaultButton(confirm);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack(); dialog.setSize(Math.max(360, dialog.getWidth()), dialog.getHeight()); dialog.setLocationRelativeTo(this);
        boolean running = timer.isRunning(); timer.stop(); keys.clear();
        try { SwingUtilities.invokeLater(() -> { field.requestFocusInWindow(); field.selectAll(); }); dialog.setVisible(true); return result[0]; }
        finally { dialog.dispose(); if (running) timer.start(); requestFocusInWindow(); }
    }

    private void purchase(int index, boolean bulk) {
        int max = coins / PRICES[index];
        if (max < 1) { labNotice = "Not enough coins."; return; }
        int qty = bulk ? askQuantity("Buy " + CHEMS[index], max, 1, PRICES[index]) : 1;
        if (qty == 0) return;
        int[] updated = stock.clone(); int remaining = coins;
        for (int i = 0; i < qty; i++) {
            remaining = GameplayBridge.buy(remaining, CHEMS, updated, index, PRICES[index]);
            if (remaining < 0) { labNotice = "Purchase failed."; return; }
        }
        System.arraycopy(updated, 0, stock, 0, stock.length);
        coins = remaining;
        labNotice = "Purchased " + qty + " x " + CHEMS[index] + " for " + (qty * PRICES[index]) + " coins.";
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
                if (new Rectangle(bx + 200, by + 18, 52, 34).contains(x, y)) { purchase(i, false); return; }
                if (new Rectangle(bx + 256, by + 18, 69, 34).contains(x, y)) { purchase(i, true); return; }
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
            drawEnd(g, "LEVEL 2 COMPLETE", "The Plague Doctor has been stabilized!", true);
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

    private static void hospitalRect(Graphics2D g, int x, int y, int w, int h, int rgb) {
        g.setColor(new Color(rgb)); g.fillRect(x,y,w,h);
    }
    private static void hospitalMonitor(Graphics2D g,int x,int y) {
        hospitalRect(g,x,y,56,39,0x151d29);
        hospitalRect(g,x+4,y+4,48,28,0x254a48);
        g.setColor(new Color(0x76c6a6));
        g.drawPolyline(new int[]{x+7,x+17,x+22,x+27,x+32,x+38,x+48},new int[]{y+20,y+20,y+10,y+27,y+16,y+20,y+20},7);
        hospitalRect(g,x+24,y+39,8,10,0x69777b);
        hospitalRect(g,x+13,y+49,30,4,0x49575e);
    }
    private static void hospitalBed(Graphics2D g,int x,int y) {
        hospitalRect(g,x+5,y+30,150,10,0x171e29);
        hospitalRect(g,x,y,150,30,0x53676c);
        hospitalRect(g,x+4,y+3,142,21,0xa0aaa4);
        hospitalRect(g,x+8,y+5,29,17,0xd0d3c7);
        hospitalRect(g,x+43,y+4,100,20,0x526f70);
        hospitalRect(g,x+43,y+4,100,3,0x77938e);
        hospitalRect(g,x+80,y+13,23,7,0x69363e);
        hospitalRect(g,x,y-9,5,40,0x85999b);
        hospitalRect(g,x+145,y-5,5,36,0x85999b);
        hospitalRect(g,x+160,y-38,3,72,0x839395);
        hospitalRect(g,x+151,y-38,20,3,0x839395);
        hospitalRect(g,x+153,y-34,13,21,0x9eb8b1);
        hospitalRect(g,x+155,y-25,9,10,0x597f7d);
        hospitalRect(g,x+150,y+33,24,3,0x839395);
    }
    private static void hospitalFloorProps(Graphics2D g, int x, int y, int type) {
        g.setColor(new Color(10,16,22,100)); g.fillOval(x-4,y+13,48,13);
        if(type==0) { // fallen wheelchair, flat on the floor
            g.setColor(new Color(0x18242c)); g.fillOval(x,y,18,23); g.fillOval(x+25,y+2,17,22);
            g.setColor(new Color(0x718386)); g.drawOval(x+2,y+2,13,18); g.drawOval(x+27,y+4,12,17);
            hospitalRect(g,x+12,y+3,17,16,0x526c70);
            hospitalRect(g,x+14,y+5,13,10,0x354c54);
            hospitalRect(g,x+8,y-3,24,3,0x899795);
            hospitalRect(g,x+17,y+20,12,4,0x7b8b8a);
        } else if(type==1) { // spilled medical kit and papers
            hospitalRect(g,x,y,30,19,0x202b33);
            hospitalRect(g,x+2,y+1,26,15,0x89978d);
            hospitalRect(g,x+10,y+3,9,10,0x713b43);
            hospitalRect(g,x+7,y+6,15,4,0x713b43);
            hospitalRect(g,x+33,y+9,10,13,0xabb3a5);
            hospitalRect(g,x+36,y+12,5,2,0x63716d);
            hospitalRect(g,x-8,y+18,12,4,0x9aa9a4);
        } else if(type==2) { // overturned instrument tray
            hospitalRect(g,x,y,40,19,0x26333b);
            hospitalRect(g,x+2,y+2,36,14,0x758784);
            hospitalRect(g,x+5,y+4,30,10,0x4c6064);
            hospitalRect(g,x+9,y+6,17,3,0xb0b9ae);
            hospitalRect(g,x+29,y+8,4,5,0xb0b9ae);
            hospitalRect(g,x+12,y+20,18,3,0x8e9e9b);
        } else { // broken tiles and discarded documents
            g.setColor(new Color(0x1b252e)); g.drawPolyline(new int[]{x,x+10,x+7,x+24,x+35},new int[]{y+4,y+9,y+17,y+12,y+22},5);
            hospitalRect(g,x+4,y+12,11,8,0x89958d);
            hospitalRect(g,x+22,y+1,14,10,0xa4ada0);
            hospitalRect(g,x+24,y+4,9,2,0x687671);
        }
    }
    private static BufferedImage buildHospital() {
        BufferedImage img = new BufferedImage(WORLD,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=img.createGraphics();
        Random rng=new Random(2202);
        String[] names={"EMERGENCY WING","ISOLATION WARD","VENTILATION UNIT","RESEARCH WING"};
        for(int s=0;s<4;s++) {
            int x=s*790;
            hospitalRect(g,x,0,790,H,0x101723);
            hospitalRect(g,x,65,790,151,s==2?0x354449:0x414e55);
            hospitalRect(g,x,66,790,5,0x738184);
            hospitalRect(g,x,153,790,6,0x66817e);
            hospitalRect(g,x,201,790,15,0x202d35);
            for(int i=0;i<790;i+=32) for(int y=216;y<448;y+=24) {
                int shade=44+rng.nextInt(9);
                g.setColor(new Color(shade,shade+7,shade+10)); g.fillRect(x+i,y,31,23);
            }
            hospitalRect(g,x,448,790,92,0x18212d);
            hospitalRect(g,x,448,790,5,0x647478);
            for(int i=0;i<790;i+=158) {
                hospitalRect(g,x+i+30,27,98,6,0x7e9995);
                g.setColor(new Color(110,190,170,15)); g.fillRect(x+i+20,33,118,175);
            }
            hospitalRect(g,x+278,76,270,28,0x192831);
            g.setFont(new Font("Monospaced",Font.BOLD,15)); g.setColor(new Color(0xb7d4cc));
            g.drawString(names[s],x+292,95);
            for(int i=0;i<2;i++) {
                int d=x+25+i*630;
                hospitalRect(g,d,109,106,103,0x1b2932);
                hospitalRect(g,d+5,114,96,98,0x52666a);
                hospitalRect(g,d+11,120,39,47,0x263d46);
                hospitalRect(g,d+56,120,39,47,0x263d46);
                hospitalRect(g,d+51,114,3,98,0x202c33);
                hospitalRect(g,d+43,178,15,4,0xa2a8a0);
            }
            if(s==0) {
                hospitalRect(g,x+156,174,215,40,0x25383f);
                hospitalRect(g,x+149,164,229,14,0x81908d);
                hospitalMonitor(g,x+180,111);
                hospitalRect(g,x+280,158,34,12,0xb4b9a9);
                hospitalRect(g,x+317,160,25,9,0x8d9b94);
                hospitalBed(g,x+427,181);
            } else if(s==1) {
                hospitalBed(g,x+155,180); hospitalBed(g,x+420,180);
                for(int k=0;k<6;k++) hospitalRect(g,x+350+k*8,110,7,94,k%2==0?0x557d79:0x3d625f);
                hospitalRect(g,x+344,107,60,4,0x899c99);
            } else if(s==2) {
                for(int k=0;k<3;k++) {
                    int u=x+155+k*155;
                    hospitalRect(g,u,118,120,87,0x26373e);
                    hospitalRect(g,u+7,125,106,70,0x61716e);
                    for(int j=0;j<7;j++) hospitalRect(g,u+12,130+j*9,96,4,0x26363c);
                    hospitalRect(g,u+48,106,18,15,0x718580);
                }
                hospitalRect(g,x+150,106,450,8,0x768b84);
                g.setColor(new Color(80,150,101,90)); g.fillOval(x+360,219,140,25);
            } else {
                hospitalBed(g,x+310,180);
                hospitalMonitor(g,x+180,137); hospitalMonitor(g,x+520,137);
                hospitalRect(g,x+140,188,115,25,0x536669);
                hospitalRect(g,x+500,188,105,25,0x536669);
                hospitalRect(g,x+365,108,45,6,0x84948e);
                hospitalRect(g,x+382,114,8,22,0x84948e);
                g.setColor(new Color(0xb7c5b8)); g.fillOval(x+350,132,73,14);
            }
            for(int k=0;k<32;k++) {
                int px=x+20+rng.nextInt(745), py=230+rng.nextInt(207);
                hospitalRect(g,px,py,3+rng.nextInt(9),2+rng.nextInt(4),k%3==0?0x69373d:0x536065);
            }
            g.setColor(new Color(105,37,48,95)); g.fillOval(x+270,345,85,17); g.fillOval(x+590,408,55,12);
            hospitalFloorProps(g,x+83,250,s%4);
            hospitalFloorProps(g,x+275,365,(s+1)%4);
            hospitalFloorProps(g,x+700,270,(s+2)%4);
            hospitalFloorProps(g,x+760,395,(s+3)%4);
            for(int k=0;k<9;k++) hospitalRect(g,x+680+k*9,217,9,5,k%2==0?0x9d874e:0x263039);
        }
        g.dispose(); return img;
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
        // Hurt uses normal walk/run frames, NEVER the separate hurt PNG folder.
        String moveAction = running ? "run" : "walk";
        String action = hurtTicks > 0 ? moveAction
                : dead ? "death"
                : throwCooldown > 14 ? "throw potion" : moveAction;
        int frame = moving ? ticks / (running ? 6 : 10) : 0; // Dedicated run sheet at moderate FPS
        BufferedImage sprite = sprites.get(true, scientist, action, facing, frame);
        // Strong red blink over the entire sprite, not a translucent red glow.
        boolean red = hurtTicks > 0 && hurtTicks % 8 >= 4;
        int bob = running && (ticks / 6) % 2 == 0 ? 2 : 0;
        if (running) drawRunTrail(g, x, y);
        if (sprite != null) {
            // Slight forward lean makes sprinting visibly different even if
            // a character has only walk sheets and no dedicated run sheets.
            Graphics2D actor = (Graphics2D) g.create();
            if (running && (facing.equals("left") || facing.equals("right"))) {
                double angle = facing.equals("right") ? .035 : -.035;
                actor.rotate(angle, x, y);
            }
            drawSprite(actor, red ? sprites.red(sprite) : sprite,
                    x, y - bob, 65, 1f, 59);
            actor.dispose();
        } else {
            Color body = red ? new Color(247, 34, 52) : new Color(161, 126, 204);
            Color head = red ? new Color(247, 34, 52) : new Color(207, 168, 150);
            g.setColor(body);
            g.fillRoundRect(x - 12, y - 49 - bob, 24, 45, 8, 8);
            g.setColor(head);
            g.fillOval(x - 11, y - 68 - bob, 22, 24);
        }
        g.setColor(red ? new Color(255, 60, 77, 220) : new Color(179, 148, 234, 130));
        g.drawOval(x - 15, y - 5, 30, 9);
    }
    private void drawRunTrail(Graphics2D g, int x, int y) {
        // Pixel speed lines + dust; no new art and no fire effects.
        int sign = facing.equals("left") ? 1 : -1;
        int pulse = ticks % 8;
        g.setColor(new Color(223, 194, 255, 215));
        g.fillRect(x + sign * 24 - 4, y - 29 + pulse / 3, 12, 3);
        g.fillRect(x + sign * 30 - 3, y - 15, 10, 3);
        g.setColor(new Color(174, 145, 224, 185));
        g.fillRect(x + sign * 19 - 3, y - 7, 10, 3);
        g.fillRect(x + sign * 33 - 2, y - 5 - pulse / 2, 5, 3);
        g.setColor(new Color(224, 213, 244, 190));
        g.fillRect(x + sign * 13 - 2, y - 2, 5, 3);
    }
    private void drawEnemy(Graphics2D g, Enemy z) {
        int x = (int) (z.x - camera), y = (int) z.y;
        if (x < -100 || x > W + 100) return;
        g.setColor(new Color(5, 8, 15, 150));
        g.fillOval(x - (z.type == 3 ? 31 : 20), y - 7, z.type == 3 ? 62 : 40, 13);
        // Attack is a 24-tick, one-time four-frame sequence tied to the hit cooldown.
        // Unlike ticks/animationDelay, this starts at frame zero on every attack.
        boolean attacking = z.flash <= 0 && z.cooldown > 51;
        String action = attacking ? "attack" : z.type == 1 ? "run" : "walk";
        int animationDelay = z.type == 1 ? 8 : z.type == 2 ? 14 : z.type == 3 ? 13 : 11;
        int frame = attacking ? Math.min(3, (75 - z.cooldown) / 6)
                : ticks / animationDelay;
        BufferedImage sprite = sprites.get(false, ENEMY_IDS[z.type], action, z.face, frame);
        boolean red = z.flash > 0 && z.flash % 8 >= 4;
        if (sprite != null) {
            drawSprite(g, red ? sprites.red(sprite) : sprite,
                    x, y, z.type == 3 ? 92 : 67, 1f);
        } else {
            drawFallbackEnemy(g, z, x, y, red);
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
        label(g, "HOSPITAL WING", 413, 33, 10, MUTED, true);
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
        label(g, "Fictional treatments use real chemistry concepts, not medical prescriptions.", 210, 478, 10, MUTED, false);
    }
    private void drawLabElements(Graphics2D g) {
        label(g, "CHEMICAL & MATERIAL INFORMATION", 210, 124, 14, PURPLE, true);
        label(g, "pH: typical water behavior; *pure water at 25 C", 210, 140, 10, WHITE, false);
        for (int i = 0; i < CHEMS.length; i++) {
            int x = 210 + (i % 2) * 350, y = 151 + (i / 2) * 81;
            panel(g, x, y, 334, 69, 238);
            String[] info = game.ChemistryInfo.get(CHEMS[i]);
            label(g, CHEMS[i], x + 14, y + 15, 12, WHITE, true);
            label(g, "Formula: " + info[0] + " | " + info[4], x + 14, y + 30, 10, PURPLE, true);
            label(g, "Atomic No: " + info[1], x + 14, y + 44, 10, WHITE, false);
            label(g, "Bond: " + info[2] + " | pH: " + info[3], x + 14, y + 58, 10, WHITE, false);
        }
    }
    private void drawLabShop(Graphics2D g) {
        label(g, "INGREDIENT SHOP", 210, 122, 14, PURPLE, true);
        label(g, "COINS: " + coins, 737, 122, 14, GOLD, true);
        label(g, "BUY 1 for instant purchase, or QUANTITY for bulk.",
                210, 139, 10, MUTED, false);

        for (int i = 0; i < CHEMS.length; i++) {
            int x = 210 + (i % 2) * 350;
            int y = 151 + (i / 2) * 81;
            panel(g, x, y, 334, 69, 238);
            label(g, CHEMS[i], x + 13, y + 23, 11, WHITE, true);
            label(g, "OWNED: " + stock[i] + "   PRICE: " + SHOP_PRICES[i] + " coins",
                    x + 13, y + 47, 10, MUTED, false);
            Color buyColor = coins >= SHOP_PRICES[i] ? new Color(113, 71, 167) : new Color(57, 46, 72);
            button(g, x + 200, y + 18, 52, 34, "BUY 1", buyColor);
            button(g, x + 256, y + 18, 69, 34, "QUANTITY", buyColor);
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
        // Cache red-tinted frames so hurt flashing does not allocate every tick.
        private final Map<BufferedImage, BufferedImage> redCache = new IdentityHashMap<>();
        private final Set<String> missing = new HashSet<>();

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
                BufferedImage[] frames = key.startsWith("characters/") ? extractPlayer(sheet) : extract(sheet);
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
        private static BufferedImage[] extractPlayer(BufferedImage sheet) {
            int w = sheet.getWidth(), h = sheet.getHeight();
            if (w <= 1 || h <= 1) return new BufferedImage[] {crop(sheet)};
            if (w < h * 1.45) return new BufferedImage[] {crop(sheet)};

            // Count nontransparent pixels per column. Transparent empty space
            // between frames is a stronger signal than image width / 4.
            int[] ink = new int[w];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if ((sheet.getRGB(x, y) >>> 24) > 24) ink[x]++;
                }
            }
            boolean[] occupied = new boolean[w];
            for (int x = 0; x < w; x++) occupied[x] = ink[x] > 0;

            // Join tiny holes INSIDE a single character (e.g. between strands
            // of hair). Larger gutters remain available as frame boundaries.
            int smallGap = Math.max(2, Math.min(5, h / 35));
            int last = -1;
            for (int x = 0; x < w; x++) {
                if (!occupied[x]) continue;
                if (last >= 0 && x - last - 1 <= smallGap) {
                    for (int k = last + 1; k < x; k++) occupied[k] = true;
                }
                last = x;
            }

            List<int[]> runs = new ArrayList<>();
            int start = -1;
            for (int x = 0; x <= w; x++) {
                boolean on = x < w && occupied[x];
                if (on && start < 0) start = x;
                if (!on && start >= 0) {
                    runs.add(new int[] {start, x - 1});
                    start = -1;
                }
            }
            if (runs.isEmpty()) return new BufferedImage[] {crop(sheet)};
            int widest = 0;
            for (int[] r : runs) widest = Math.max(widest, r[1] - r[0] + 1);
            int majorMin = Math.max(7, (int) Math.ceil(widest * .18));
            List<int[]> major = new ArrayList<>();
            for (int[] r : runs) if (r[1] - r[0] + 1 >= majorMin) major.add(r);

            // If tiny separated components produced too many runs, group
            // adjacent ones until the most likely 2..8 actual frame regions remain.
            while (major.size() > 8) {
                int closest = 0, minGap = Integer.MAX_VALUE;
                for (int i = 0; i < major.size() - 1; i++) {
                    int gap = major.get(i + 1)[0] - major.get(i)[1] - 1;
                    if (gap < minGap) { minGap = gap; closest = i; }
                }
                major.get(closest)[1] = major.get(closest + 1)[1];
                major.remove(closest + 1);
            }

            // A genuinely single-frame image must never be split just because
            // its PNG filename contains the word "walk" or "attack".
            if (major.size() < 2) return new BufferedImage[] {crop(sheet)};

            // Split at the transparent gutter between each detected main sprite.
            int count = major.size();
            int[] boundaries = new int[count + 1];
            boundaries[0] = 0; boundaries[count] = w;
            for (int i = 1; i < count; i++) {
                int prevEnd = major.get(i - 1)[1];
                int nextStart = major.get(i)[0];
                boundaries[i] = (prevEnd + nextStart + 1) / 2;
            }

            BufferedImage[] frames = new BufferedImage[count];
            int maxW = 1, maxH = 1;
            for (int i = 0; i < count; i++) {
                int left = boundaries[i], right = boundaries[i + 1];
                frames[i] = crop(sheet.getSubimage(left, 0, Math.max(1, right - left), h));
                maxW = Math.max(maxW, frames[i].getWidth());
                maxH = Math.max(maxH, frames[i].getHeight());
            }
            // Equal-sized animation canvases keep feet anchored and prevent
            // jitter from varying cropped frame dimensions.
            for (int i = 0; i < count; i++) {
                BufferedImage normalized = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = normalized.createGraphics();
                g.drawImage(frames[i], (maxW - frames[i].getWidth()) / 2,
                        maxH - frames[i].getHeight(), null);
                g.dispose(); frames[i] = normalized;
            }
            return frames;
        }
        private static BufferedImage[] extract(BufferedImage sheet) {
            int w = sheet.getWidth(), h = sheet.getHeight();
            // CHEMION directional sheets are one horizontal row of FOUR equal cells.
            // Do not guess frame boundaries from transparent gaps: connected poses,
            // non-transparent backgrounds, or close limbs could merge all four poses.
            if (w < 4 || w < h * 1.5) return new BufferedImage[] {crop(sheet)};
            BufferedImage[] frames = new BufferedImage[4];
            int maxW = 1, maxH = 1;
            for (int i = 0; i < 4; i++) {
                int left = (int) ((long) i * w / 4);
                int right = (int) ((long) (i + 1) * w / 4);
                frames[i] = crop(sheet.getSubimage(left, 0, right - left, h));
                maxW = Math.max(maxW, frames[i].getWidth());
                maxH = Math.max(maxH, frames[i].getHeight());
            }
            // Normalize sizes and align feet, avoiding jitter between frames.
            for (int i = 0; i < frames.length; i++) {
                BufferedImage normalized = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = normalized.createGraphics();
                g.drawImage(frames[i], (maxW - frames[i].getWidth()) / 2,
                        maxH - frames[i].getHeight(), null);
                g.dispose();
                frames[i] = normalized;
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
