package battlefield.level1;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Level1GamePanel extends JPanel {
    private static final int W = 960, H = 540, WORLD = 3200;
    private static final int TOWN_HEIGHT = 620, ROAD_TOP = 207;
    private static final int PLAYER_H = 65, ZOMBIE_H = 61, BOSS_H = 88;
    private static final float SPRITE_OPACITY = 0.87f;
    private static final Color WHITE = new Color(242, 234, 255);
    private static final Color PURPLE = new Color(204, 170, 251);
    private static final Color MUTED = new Color(184, 170, 204);
    private static final Color GOLD = new Color(244, 178, 83);
    private static final String[] TYPES = {"common", "runner", "crawler", "boss"};
    // Four visible potion inventory slots at the bottom of the battlefield.
    private static final int HOTBAR_X = 22, HOTBAR_Y = 468, HOTBAR_SLOT_W = 126;
    private static final int HOTBAR_SLOT_H = 51, HOTBAR_GAP = 6;
    private static final String[] SHORT_POTION_NAMES = {"COMMON", "RUNNER", "CRAWLER", "SURVIVOR"};
    private static final Color[] POTION_COLORS = {
        new Color(117, 214, 150), new Color(102, 194, 235),
        new Color(241, 185, 107), new Color(193, 135, 239)
    };
    private static final String[] NAMES = {"Common Zombie", "Runner Zombie", "Crawler Zombie", "Mutated Survivor"};
    private static final String[] CHEMS = {"Sodium Bicarbonate", "Distilled Water", "Sodium Chloride", "Citric Acid", "Ascorbic Acid", "Sodium Citrate", "Activated Carbon", "Crystal Sample"};
    private static final String[] NAV = {"Mix Potion", "Recipes", "Elements", "Shop", "Research"};
    // Shop prices, in the same order as CHEMS. One purchase adds one ingredient.
    private static final int[] SHOP_PRICES = {4, 2, 3, 3, 5, 5, 4, 6};
    private static final String[] TABS = {"Research Journal", "Periodic Table", "Zombies", "Recipes"};
    private static final String[] DESC = {
        "Once an ordinary resident of the Abandoned Town, this infected civilian has lost control of its body. Although its muscles have weakened, the pathogen continues to stimulate movement and aggression. A thick acidic secretion now covers its infected tissue, acting as a protective barrier against treatment.",
        "This infected resident moves at an abnormal speed. Its mutation causes uncontrolled muscle activity and erratic aggression, making the Runner difficult to approach.",
        "Severe mutation has forced this infected resident to crawl. Its body releases a reactive secretion that damages nearby organic material.",
        "The Mutated Survivor has developed multiple protective mutations. Earlier research suggests that several chemical mechanisms must be addressed."
    };
    private static final String[] CONDITION = {
        "Initial analysis reveals a secretion with a pH of 3.0. The protective layer becomes unstable as its acidity decreases toward neutral.",
        "Tests indicate unusually high free metal-ion activity around the infected tissue.",
        "The secretion shows strong oxidizing behavior in laboratory tests.",
        "An acidic barrier, metal-ion activity, and oxidizing secretions are present together."
    };
    private static final String[] OBJECTIVE = {
        "Find a compound capable of reducing the secretion's acidity and weakening the barrier without harming healthy tissue.",
        "Find a compound that can bind excess metal ions and reduce the mutation's activity.",
        "Identify a mild reducing agent that can counter the reactive secretion.",
        "Apply the three chemistry principles learned in Level 1 to formulate a combined treatment."
    };
    private static final String[] CLUE = {
        "Acid-base neutralization: a mild base can reduce acidity. Add a suitable liquid carrier.",
        "Complexation: citrate ions can bind certain metal ions. Add a suitable liquid carrier.",
        "Redox chemistry: an antioxidant can act as a reducing agent. Add a suitable liquid carrier.",
        "Combine the mild base, metal-binding compound, and reducing agent discovered earlier."
    };
    private static final String[] FORMULA = {
        "Sodium Bicarbonate + Distilled Water",
        "Sodium Citrate + Distilled Water",
        "Ascorbic Acid + Distilled Water",
        "Sodium Bicarbonate + Sodium Citrate + Ascorbic Acid"
    };
    // Recipe indexes refer directly to CHEMS. No string comparison = no false mismatch.
    private static final int[][] RECIPES = {{0, 1}, {5, 1}, {4, 1}, {0, 5, 4}};

    private final String scientist;
    private final Runnable onWin, onMenu;
    private final Random random = new Random();
    private final SpriteLoader sprites = new SpriteLoader();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Potion> shots = new ArrayList<>();
    private final List<Rectangle> obstacles = new ArrayList<>();
    private final List<Prop> props = new ArrayList<>();
    private final int[] stock = {7, 11, 3, 2, 2, 0, 2, 1};
    private final int[] vials = new int[4];
    private final int[] stabilized = new int[4];
    private final boolean[] selected = new boolean[8];
    private final boolean[] seen = new boolean[4];
    private final boolean[] unlockedRecipes = new boolean[4];
    private final Set<Integer> pressed = new HashSet<>();
    private final javax.swing.Timer timer;
    private BufferedImage town;

    private double px = 130, py = 386, camera = 0, hp = 100;
    private int ticks, hurtTicks, throwCooldown, medkits = 2, xp, coins;
    private int wave = 1, spawned, spawnTimer = 105, waveTimer;
    private int labTicks, labTab, journalTab = 2, journalChoice, noticeTicks;
    private int encounterType, target, equipped, batchQty = 1;
    private boolean encounter, lab, journal, paused, dead, won;
    private String facing = "right";
    private String notice = "Stay alert. The infected are approaching.";
    private String labNotice = "Read Recipes for clues, then select ingredients.";
    private Enemy encountered;

    private static class Prop {
        final String kind;
        final int x, y, w, h;
        Prop(String kind, int x, int y, int w, int h) {
            this.kind = kind; this.x = x; this.y = y; this.w = w; this.h = h;
        }
        Rectangle2D bounds() {
            int half = kind.equals("lamp") ? 6 : kind.equals("fence") ? w / 2 : w / 2 - 7;
            int depth = kind.equals("van") ? 27 : kind.equals("barrier") ? 13 : 15;
            return new Rectangle2D.Double(x - half, y - depth, half * 2, depth + 5);
        }
    }

    private static class Enemy {
        int type, attackCooldown, flash;
        String face = "left";
        double x, y, hp, maxHp;
        boolean entering = true;
        Enemy(int type, double x, double y) {
            this.type = type; this.x = x; this.y = y;
            maxHp = type == 3 ? 10 : type == 0 ? 2 : 3;
            hp = maxHp;
        }
        double speed() { return type == 1 ? .78 : type == 2 ? .35 : type == 3 ? .44 : .46; }
        double entranceSpeed() { return type == 1 ? 1.23 : type == 2 ? .76 : .91; }
    }
    private static class Potion {
        final Enemy enemy;
        final int type;
        final double sx, sy, arc;
        double x, y, t;
        Potion(Enemy enemy, int type, double x, double y, double distance) {
            this.enemy = enemy; this.type = type; sx = this.x = x; sy = this.y = y;
            arc = Math.min(43, 24 + distance * .045);
        }
        void move() {
            t = Math.min(1, t + .019); // deliberately slower curved throw
            double ey = enemy.y - (enemy.type == 3 ? 48 : 34);
            x = sx + (enemy.x - sx) * t;
            y = sy + (ey - sy) * t - arc * Math.sin(Math.PI * t);
        }
    }
    private static class Popup {
        int x, y, h, objectiveY, objectiveH, buttonY;
        Rectangle button() { return new Rectangle(x + 311, y + buttonY, 96, 25); }
    }

    // The selected scientist must come from Main -> Character Selection.
    // No default character or standalone battlefield testing constructor.
    public Level1GamePanel(String scientist, Runnable onWin, Runnable onMenu) {
        if (scientist == null || scientist.trim().isEmpty()) {
            throw new IllegalArgumentException("A scientist must be selected in Character Selection.");
        }
        this.scientist = scientist.trim().toLowerCase(Locale.ROOT);
        this.onWin = onWin; this.onMenu = onMenu;
        setPreferredSize(new Dimension(1280, 720));
        setBackground(new Color(20, 15, 33));
        setDoubleBuffered(true); setFocusable(true);
        reset(); createTown(); keys();
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                click((int) (e.getX() * W / (double) Math.max(1, getWidth())),
                      (int) (e.getY() * H / (double) Math.max(1, getHeight())));
                repaint();
            }
        });
        timer = new javax.swing.Timer(16, e -> updateGame());
        timer.start();
    }
    @Override public void addNotify() { super.addNotify(); SwingUtilities.invokeLater(this::requestFocusInWindow); }
    public void disposeGame() { timer.stop(); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private void reset() {
        enemies.clear(); shots.clear(); pressed.clear();
        int[] initial = {7, 11, 3, 2, 2, 0, 2, 1};
        System.arraycopy(initial, 0, stock, 0, stock.length);
        Arrays.fill(vials, 0); Arrays.fill(stabilized, 0); Arrays.fill(selected, false);
        Arrays.fill(seen, false); Arrays.fill(unlockedRecipes, false);
        px = 130; py = 386; camera = 0; hp = 100;
        ticks = hurtTicks = throwCooldown = xp = 0;
        coins = 10; // Starting funds so players can recover a missing essential ingredient.
        medkits = 2;
        wave = 1; spawned = 0; spawnTimer = 105; waveTimer = 0;
        labTicks = labTab = journalChoice = 0; journalTab = 2;
        encounterType = target = equipped = 0; batchQty = 1;
        encounter = lab = journal = paused = dead = won = false;
        facing = "right"; encountered = null;
        notice = "Stay alert. The infected are approaching."; noticeTicks = 160;
    }
    private void bind(int key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); repaint(); }
        });
    }
    private void keys() {
        int[] movement = {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
                KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT, KeyEvent.VK_SHIFT};
        for (int k : movement) {
            String name = "move" + k;
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(k, 0, false), name + "D");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(k, 0, true), name + "U");
            getActionMap().put(name + "D", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { pressed.add(k); }
            });
            getActionMap().put(name + "U", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { pressed.remove(k); }
            });
        }
        bind(KeyEvent.VK_M, "lab", () -> { if (canPlay()) openLab(); });
        bind(KeyEvent.VK_J, "journal", () -> { if (canPlay()) journal = true; });
        // Number keys choose the corresponding visible potion slot, even when empty.
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            bind(KeyEvent.VK_1 + i, "potionSlot" + (i + 1), () -> equipPotion(slot));
            bind(KeyEvent.VK_NUMPAD1 + i, "potionNumpad" + (i + 1), () -> equipPotion(slot));
        }
        bind(KeyEvent.VK_Q, "switch", this::switchPotion);
        bind(KeyEvent.VK_H, "heal", this::heal);
        bind(KeyEvent.VK_SPACE, "throw", () -> throwPotion(null));
        bind(KeyEvent.VK_ENTER, "continue", () -> {
            if (encounter) encounter = false;
            else if (won && onWin != null) onWin.run();
        });
        bind(KeyEvent.VK_P, "pause", () -> { if (paused) paused = false; else if (canPlay()) paused = true; });
        bind(KeyEvent.VK_R, "restart", () -> { if (dead || won) reset(); });
        bind(KeyEvent.VK_ESCAPE, "escape", () -> {
            if (lab) lab = false;
            else if (journal) journal = false;
            else if (paused) paused = false;
            else if ((dead || won) && onMenu != null) onMenu.run();
        });
    }
    private boolean canPlay() { return !encounter && !lab && !journal && !paused && !dead && !won; }

    // ABANDONED TOWN: boarded shops, damaged apartment blocks, broken lights,
    // closed signs, cracked asphalt and scattered debris. Same gameplay map.
    private void createTown() {
        town = new BufferedImage(WORLD, TOWN_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = town.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setPaint(new GradientPaint(0, 0, new Color(26, 21, 47), 0, TOWN_HEIGHT, new Color(49, 40, 69)));
        g.fillRect(0, 0, WORLD, TOWN_HEIGHT);

        for (int x = -20, n = 0; x < WORLD; x += 350, n++) {
            building(g, x, n);
        }
        decorateBuildings(g);

        g.setColor(new Color(18, 16, 31));
        g.fillRect(0, 192, WORLD, 20);
        g.setColor(new Color(105, 82, 118));
        g.fillRect(0, 203, WORLD, 4);
        g.setColor(new Color(42, 36, 56));
        g.fillRect(0, ROAD_TOP, WORLD, TOWN_HEIGHT - ROAD_TOP);
        g.setPaint(new GradientPaint(0, 208, new Color(71, 57, 88, 155), 0, 620, new Color(31, 29, 49, 115)));
        g.fillRect(0, ROAD_TOP, WORLD, TOWN_HEIGHT - ROAD_TOP);

        for (int x = 0; x < WORLD; x += 210) {
            g.setColor(new Color(142, 119, 145, 48));
            g.fillRect(x, 247, 2, 372);
            g.setColor(new Color(13, 13, 26, 58));
            g.fillRect(x + 3, 247, 2, 372);
        }
        for (int y = 298; y < TOWN_HEIGHT; y += 136) {
            g.setColor(new Color(154, 130, 156, 57));
            g.fillRect(0, y, WORLD, 2);
            g.setColor(new Color(16, 15, 28, 50));
            g.fillRect(0, y + 3, WORLD, 2);
        }
        for (int x = 28; x < WORLD; x += 158) {
            g.setColor(new Color(165, 141, 135, 74));
            g.fillRect(x, 390, 46, 3);
        }

        Random r = new Random(7207);
        for (int i = 0; i < 49000; i++) {
            int x = r.nextInt(WORLD), y = ROAD_TOP + r.nextInt(TOWN_HEIGHT - ROAD_TOP);
            int type = r.nextInt(5);
            g.setColor(type == 0 ? new Color(143, 122, 154, 45)
                : type == 1 ? new Color(13, 13, 26, 55)
                : type == 2 ? new Color(111, 94, 129, 42)
                : type == 3 ? new Color(89, 71, 109, 47)
                : new Color(158, 127, 119, 28));
            g.fillRect(x, y, 1 + r.nextInt(4), 1 + r.nextInt(3));
        }
        for (int i = 0; i < 190; i++) {
            int x = r.nextInt(WORLD), y = 232 + r.nextInt(368);
            crack(g, x, y, 15 + r.nextInt(52), r);
        }
        for (int i = 0; i < 83; i++) {
            int x = r.nextInt(WORLD), y = 250 + r.nextInt(355);
            blood(g, x, y, 9 + r.nextInt(23), r);
        }
        for (int i = 0; i < 22; i++) {
            int x = r.nextInt(WORLD), y = 260 + r.nextInt(300);
            puddle(g, x, y, 20 + r.nextInt(46));
        }
        for (int i = 0; i < 1050; i++) {
            int x = r.nextInt(WORLD), y = 214 + r.nextInt(405);
            g.setColor(i % 5 == 0 ? new Color(135, 118, 95) : new Color(87, 79, 100));
            int s = 2 + r.nextInt(6);
            g.fillRect(x, y, s, 2 + r.nextInt(3));
            g.setColor(new Color(14, 12, 26, 80));
            g.fillRect(x + 2, y + 3, s, 2);
        }

        decorateRoad(g);
        for (int x = 220; x < WORLD; x += 620) {
            glow(g, x, 133, 166, new Color(250, 167, 66, 82));
            g.setColor(new Color(241, 168, 74, 18));
            g.fillPolygon(new int[]{x - 9, x + 9, x + 135, x - 135},
                new int[]{133, 133, 425, 425}, 4);
            g.setColor(new Color(253, 183, 85, 29));
            g.fillOval(x - 84, 226, 168, 36);
        }
        g.dispose();
        createProps();
    }

    private void building(Graphics2D g, int x, int n) {
        Random r = new Random(1149L + n * 79L);
        int w = 350;
        Color wall = new Color(59 + r.nextInt(17), 43 + r.nextInt(13), 81 + r.nextInt(16));
        g.setColor(new Color(13, 11, 28));
        g.fillRect(x + 4, 0, w - 3, 207);
        g.setColor(wall);
        g.fillRect(x + 2, 0, w - 9, 187);
        g.setColor(new Color(29, 24, 47));
        g.fillRect(x + 3, 0, w - 10, 20);
        g.setColor(new Color(116, 87, 124));
        g.fillRect(x + 3, 17, w - 10, 4);
        g.fillRect(x + 3, 119, w - 10, 5);
        g.setColor(new Color(21, 18, 38));
        g.fillRect(x + 7, 184, w - 12, 10);

        for (int j = 0; j < 4; j++) {
            int wx = x + 22 + j * 79;
            g.setColor(new Color(16, 14, 31));
            g.fillRect(wx - 3, 32, 58, 72);
            g.setColor(new Color(119, 94, 133));
            g.fillRect(wx - 3, 30, 58, 5);
            g.fillRect(wx - 3, 30, 4, 73);
            g.fillRect(wx + 52, 30, 4, 73);
            g.setColor(new Color(36, 32, 56));
            g.fillRect(wx + 3, 36, 45, 60);
            g.setColor(new Color(12, 13, 29));
            g.fillRect(wx + 7, 38, 37, 54);
            g.setColor(new Color(85, 83, 120, 105));
            g.fillRect(wx + 11, 41, 4, 33);
            g.fillRect(wx + 19, 41, 2, 28);
            g.setColor(new Color(107, 91, 126));
            g.fillRect(wx + 26, 36, 3, 58);
            g.fillRect(wx + 1, 65, 50, 3);
            if (r.nextBoolean()) {
                g.setColor(new Color(18, 16, 30));
                g.fillPolygon(new int[]{wx + 25, wx + 44, wx + 31},
                    new int[]{38, 38, 55}, 3);
                g.fillPolygon(new int[]{wx + 4, wx + 20, wx + 9},
                    new int[]{75, 63, 94}, 3);
            }
        }
        for (int j = 0; j < 4; j++) {
            int sx = x + 12 + j * 85;
            g.setColor(new Color(18, 17, 34));
            g.fillRect(sx, 138, 70, 47);
            g.setColor(new Color(102, 83, 112));
            g.fillRect(sx, 136, 70, 4);
            g.setColor(new Color(49, 43, 66));
            g.fillRect(sx + 5, 144, 60, 34);
            g.setColor(new Color(18, 17, 31));
            g.fillRect(sx + 9, 148, 52, 26);
            g.setColor(new Color(106, 88, 121));
            g.fillRect(sx + 34, 144, 3, 35);
        }
        for (int i = 0; i < 240; i++) {
            int px = x + r.nextInt(336), py = r.nextInt(184);
            g.setColor(i % 3 == 0 ? new Color(12, 12, 27, 63) : new Color(145, 111, 145, 39));
            g.fillRect(px, py, 2 + r.nextInt(7), 1 + r.nextInt(3));
        }
        for (int i = 0; i < 35; i++) {
            int vx = x + r.nextInt(339), vy = r.nextInt(105);
            g.setColor(new Color(28, 50, 38, 190));
            g.fillRect(vx, vy, 4 + r.nextInt(8), 3 + r.nextInt(6));
            if (r.nextInt(3) == 0) {
                g.setColor(new Color(61, 77, 39, 190));
                g.fillRect(vx + 2, vy + 4, 5, 4);
            }
        }
        if (n == 0) {
            g.setColor(new Color(13, 12, 26));
            g.fillRect(x + 150, 49, 151, 111);
            g.setColor(new Color(132, 111, 147));
            g.fillRect(x + 145, 45, 160, 6);
            g.fillRect(x + 145, 45, 6, 119);
            g.fillRect(x + 300, 45, 6, 119);
            g.setColor(new Color(58, 48, 80));
            g.fillRect(x + 154, 55, 141, 97);
            g.setColor(new Color(20, 19, 36));
            g.fillRect(x + 161, 61, 127, 82);
            g.setFont(new Font("Monospaced", Font.BOLD, 24));
            g.setColor(new Color(201, 175, 211));
            g.drawString("OLD TOWN", x + 164, 93);
            g.setColor(new Color(196, 156, 63));
            g.fillRect(x + 164, 128, 123, 11);
            g.setColor(new Color(24, 21, 32));
            for (int i = 0; i < 7; i++) {
                int bx = x + 164 + i * 19;
                g.fillPolygon(new int[]{bx, bx + 9, bx + 14, bx + 5},
                    new int[]{128, 128, 139, 139}, 4);
            }
            g.setColor(new Color(224, 174, 60));
            g.setFont(new Font("SansSerif", Font.BOLD, 27));
            g.drawString("!", x + 215, 124);
        }
        if (n == 4 || n == 8) {
            g.setColor(new Color(16, 14, 30));
            g.fillRect(x + 105, 145, 145, 31);
            g.setColor(new Color(192, 156, 203));
            g.setFont(new Font("Monospaced", Font.BOLD, 16));
            g.drawString(n == 4 ? "EVACUATE" : "QUARANTINE", x + 118, 167);
        }
    }

    // Visual-only detail based on the earlier abandoned-town battlefield demo.
    // No collision, enemy, camera, combat, or lab rules are modified here.
    private void decorateBuildings(Graphics2D g) {
        Random r = new Random(10927);
        String[] signs = {"GENERAL STORE", "PHARMACY", "BAKERY", "EVACUATED", "MARKET", "CLOSED", "KEEP OUT", "TOWN HALL", "NO ENTRY"};
        for (int block = 0, x = -20; x < WORLD; block++, x += 350) {
            // Crumbling signboard / old local shops, not an active city.
            int sx = x + 21;
            int sy = 151;
            g.setColor(new Color(20, 16, 31));
            g.fillRect(sx - 2, sy - 2, 137, 24);
            g.setColor(new Color(96, 66, 80));
            g.fillRect(sx, sy, 133, 19);
            g.setColor(new Color(139, 105, 117));
            g.drawRect(sx, sy, 133, 19);
            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.setColor(new Color(224, 186, 145));
            g.drawString(signs[block % signs.length], sx + 8, sy + 13);
            // Boarded-up glass and splintered shop shutters.
            for (int i = 0; i < 2; i++) {
                int wx = x + 193 + i * 75;
                int wy = 143;
                g.setColor(new Color(103, 72, 67));
                g.fillRect(wx, wy, 55, 7);
                g.fillRect(wx + 2, wy + 19, 51, 7);
                g.setColor(new Color(160, 113, 88));
                g.drawLine(wx + 2, wy + 3, wx + 51, wy + 3);
                g.drawLine(wx + 4, wy + 23, wx + 52, wy + 23);
                g.setColor(new Color(25, 21, 34));
                g.fillRect(wx + 12, wy + 1, 3, 2);
                g.fillRect(wx + 41, wy + 21, 3, 2);
            }
            // Zig-zag wall fractures, hanging wires and ivy.
            for (int i = 0; i < 7; i++) {
                int cx = x + 12 + r.nextInt(320), cy = 24 + r.nextInt(153);
                g.setColor(new Color(23, 17, 37, 150));
                g.drawLine(cx, cy, cx + 8, cy + 8);
                g.drawLine(cx + 8, cy + 8, cx + 4, cy + 18);
                g.drawLine(cx + 8, cy + 8, cx + 14, cy + 13);
            }
            for (int i = 0; i < 17; i++) {
                int vx = x + 2 + r.nextInt(342), vy = r.nextInt(185);
                g.setColor(i % 3 == 0 ? new Color(72, 92, 51) : new Color(32, 62, 48));
                g.fillRect(vx, vy, 3 + r.nextInt(7), 2 + r.nextInt(5));
            }
            // Emergency notices and missing-person papers.
            for (int i = 0; i < 3; i++) {
                int px = x + 15 + r.nextInt(320), py = 123 + r.nextInt(43);
                g.setColor(new Color(188, 162, 143));
                g.fillRect(px, py, 9, 12);
                g.setColor(new Color(92, 56, 73));
                g.fillRect(px + 2, py + 3, 5, 1);
                g.fillRect(px + 2, py + 6, 4, 1);
                g.fillRect(px + 2, py + 9, 5, 1);
            }
        }
    }

    private void decorateRoad(Graphics2D g) {
        Random r = new Random(17117);
        // Faded pedestrian crossings and roadwork warnings.
        for (int base = 180; base < WORLD; base += 1100) {
            g.setColor(new Color(169, 149, 137, 51));
            for (int i = 0; i < 5; i++) g.fillRect(base + i * 23, 248, 13, 48);
            g.setColor(new Color(26, 23, 37, 155));
            g.fillOval(base + 156, 360, 38, 15);
            g.setColor(new Color(95, 80, 101));
            g.drawOval(base + 159, 361, 33, 12);
        }
        // Rubble piles, weeds, paper scraps, and ruined pavement.
        for (int i = 0; i < 205; i++) {
            int x = r.nextInt(WORLD), y = 226 + r.nextInt(TOWN_HEIGHT - 227);
            int size = 3 + r.nextInt(13);
            g.setColor(new Color(17, 14, 29, 120));
            g.fillOval(x - 2, y + 2, size + 10, 5);
            g.setColor(i % 4 == 0 ? new Color(138, 116, 120) : new Color(99, 90, 112));
            g.fillPolygon(new int[]{x, x + size / 2, x + size, x + size + 3},
                new int[]{y, y - size / 3, y - 1, y + 3}, 4);
            if (i % 6 == 0) {
                g.setColor(new Color(51, 81, 50));
                g.drawLine(x + 3, y, x + 3, y - 8);
                g.drawLine(x + 3, y - 4, x - 1, y - 9);
                g.drawLine(x + 3, y - 4, x + 7, y - 10);
            }
        }
        // Torn hazard tape and abandoned street signs.
        for (int base = 420; base < WORLD; base += 890) {
            g.setColor(new Color(26, 22, 35));
            g.fillRect(base, 233, 5, 53);
            g.setColor(new Color(147, 116, 92));
            g.fillRect(base - 23, 229, 51, 18);
            g.setColor(new Color(30, 24, 37));
            g.setFont(new Font("Monospaced", Font.BOLD, 8));
            g.drawString("DANGER", base - 18, 241);
            g.setColor(new Color(213, 161, 58, 110));
            for (int j = 0; j < 7; j++) {
                int bx = base + 13 + j * 9;
                g.fillPolygon(new int[]{bx, bx + 4, bx + 9, bx + 5},
                    new int[]{270, 270, 279, 279}, 4);
            }
        }
    }

    private void crack(Graphics2D g, int x, int y, int size, Random r) {
        g.setColor(new Color(15, 14, 27, 130));
        int x2 = x + size / 2, y2 = y + r.nextInt(12) - 6;
        int x3 = x2 + size / 3, y3 = y2 + r.nextInt(12) - 6;
        g.drawLine(x, y, x2, y2);
        g.drawLine(x2, y2, x3, y3);
        g.drawLine(x2, y2, x2 + r.nextInt(16), y2 + 13);
        g.setColor(new Color(126, 107, 135, 35));
        g.drawLine(x, y + 2, x2, y2 + 2);
    }

    private void blood(Graphics2D g, int x, int y, int radius, Random r) {
        g.setColor(new Color(87, 21, 39, 110));
        g.fillOval(x - radius, y - radius / 3, radius * 2, radius / 2 + 7);
        g.setColor(new Color(129, 30, 45, 95));
        g.fillOval(x - radius / 2, y - radius / 4, radius, radius / 3 + 5);
        for (int i = 0; i < 19; i++) {
            int sx = x + r.nextInt(radius * 4) - radius * 2;
            int sy = y + r.nextInt(radius * 2) - radius;
            g.setColor(i % 3 == 0 ? new Color(159, 32, 50, 105) : new Color(85, 18, 36, 105));
            g.fillRect(sx, sy, 2 + r.nextInt(6), 1 + r.nextInt(3));
        }
    }

    private void puddle(Graphics2D g, int x, int y, int radius) {
        g.setColor(new Color(21, 22, 43, 120));
        g.fillOval(x - radius, y - radius / 4, radius * 2, radius / 2);
        g.setColor(new Color(85, 84, 129, 100));
        g.fillRect(x - radius / 2, y, radius, 2);
        g.setColor(new Color(134, 123, 172, 75));
        g.fillRect(x - radius / 3, y - 3, radius / 2, 2);
    }

    private void glow(Graphics2D g, int x, int y, int radius, Color center) {
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(x, y), radius,
            new float[]{0f, .18f, 1f},
            new Color[]{center, new Color(center.getRed(), center.getGreen(), center.getBlue(), 28),
                new Color(center.getRed(), center.getGreen(), center.getBlue(), 0)}));
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    private void createProps() {
        props.clear();
        props.add(new Prop("fence", 62, 226, 118, 61));
        props.add(new Prop("lamp", 220, 219, 18, 94));
        props.add(new Prop("barrier", 365, 291, 112, 45));
        props.add(new Prop("barrel", 460, 307, 35, 47));
        props.add(new Prop("van", 705, 265, 162, 99));
        props.add(new Prop("lamp", 840, 218, 18, 94));
        props.add(new Prop("crate", 1060, 304, 63, 61));
        props.add(new Prop("barrier", 1230, 514, 112, 45));
        props.add(new Prop("barrel", 1300, 523, 35, 47));
        props.add(new Prop("van", 1560, 542, 162, 99));
        props.add(new Prop("lamp", 1460, 218, 18, 94));
        props.add(new Prop("barrier", 1830, 293, 112, 45));
        props.add(new Prop("crate", 2050, 534, 63, 61));
        props.add(new Prop("lamp", 2080, 218, 18, 94));
        props.add(new Prop("van", 2410, 263, 162, 99));
        props.add(new Prop("barrel", 2610, 525, 35, 47));
        props.add(new Prop("barrier", 2740, 513, 112, 45));
        props.add(new Prop("lamp", 2700, 218, 18, 94));
        props.add(new Prop("crate", 3040, 288, 63, 61));
        props.add(new Prop("van", 3290, 542, 162, 99));
        props.add(new Prop("lamp", 3320, 218, 18, 94));
        props.add(new Prop("barrel", 3560, 307, 35, 47));
        props.add(new Prop("barrier", 3740, 288, 112, 45));
        props.add(new Prop("fence", 4100, 236, 118, 61));
        props.add(new Prop("lamp", 3940, 218, 18, 94));
        // Props are also solid obstacles, so the visual battlefield
        // and player collision stay synchronized.
        obstacles.clear();
        for (Prop p : props) {
            Rectangle2D b = p.bounds();
            obstacles.add(new Rectangle((int)b.getX(), (int)b.getY(),
                    (int)b.getWidth(), (int)b.getHeight()));
        }
    }
    private boolean walkable(double x, double y) {
        if (x < 25 || x > WORLD - 25 || y < 274 || y > H - 28) return false;
        Rectangle feet = new Rectangle((int) x - 10, (int) y - 12, 20, 18);
        for (Rectangle b : obstacles) if (feet.intersects(b)) return false;
        return true;
    }
    private int quota() { return wave == 1 ? 4 : wave == 2 ? 5 : wave == 3 ? 6 : 1; }
    private int activeLimit() { return wave == 1 ? 2 : wave == 2 ? 3 : wave == 3 ? 4 : 1; }
    private int nextType() {
        if (wave == 1) return 0;
        if (wave == 2) return spawned % 3 == 0 ? 0 : 1;
        if (wave == 3) return spawned % 3 == 0 ? 2 : spawned % 3 == 1 ? 1 : 0;
        return 3;
    }
    private void spawn() {
        enemies.add(new Enemy(nextType(), camera + W + 75, 347 + random.nextInt(115)));
        spawned++; spawnTimer = 155;
    }
    private void firstEncounter(Enemy z) {
        if (seen[z.type]) return;
        seen[z.type] = true;
        // Unlock enough supplies for each new enemy type; extras remain distractors.
        if (z.type == 1) { stock[0] += 5; stock[5] += 9; stock[1] += 12; }
        if (z.type == 2) { stock[4] += 8; stock[1] += 10; }
        if (z.type == 3) { stock[0] += 8; stock[5] += 8; stock[4] += 8; }
        encounterType = target = z.type;
        batchQty = 1; Arrays.fill(selected, false);
        encountered = z; encounter = true;
    }
    private void message(String s) { notice = s; noticeTicks = 175; }

    private void updateGame() {
        if (lab) {
            // Shopping does not consume the limited crafting timer.
            if (labTab != 3 && --labTicks <= 0) {
                lab = false; Arrays.fill(selected, false);
                message("Crafting time expired. Open the lab to retry.");
            }
            repaint(); return;
        }
        if (encounter || journal || paused || dead || won) { repaint(); return; }
        ticks++;
        if (hurtTicks > 0) hurtTicks--;
        if (throwCooldown > 0) throwCooldown--;
        if (noticeTicks > 0) noticeTicks--;
        double dx = 0, dy = 0;
        if (pressed.contains(KeyEvent.VK_A) || pressed.contains(KeyEvent.VK_LEFT)) dx--;
        if (pressed.contains(KeyEvent.VK_D) || pressed.contains(KeyEvent.VK_RIGHT)) dx++;
        if (pressed.contains(KeyEvent.VK_W) || pressed.contains(KeyEvent.VK_UP)) dy--;
        if (pressed.contains(KeyEvent.VK_S) || pressed.contains(KeyEvent.VK_DOWN)) dy++;
        if (dx != 0 || dy != 0) {
            double len = Math.hypot(dx, dy);
            double speed = pressed.contains(KeyEvent.VK_SHIFT) ? 3.2 : 2.15;
            dx = dx / len * speed; dy = dy / len * speed;
            facing = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "right" : "left") : (dy > 0 ? "down" : "up");
            if (walkable(px + dx, py)) px += dx;
            if (walkable(px, py + dy)) py += dy;
        }
        camera = clamp(px - 270, 0, WORLD - W);
        boolean entering = false;
        for (Enemy z : enemies) if (z.entering) entering = true;
        if (!entering && spawned < quota() && --spawnTimer <= 0 && enemies.size() < activeLimit()) spawn();
        if (spawned >= quota() && enemies.isEmpty()) {
            if (++waveTimer > 105 && wave < 4) {
                wave++; spawned = 0; spawnTimer = 80; waveTimer = 0;
                message("Wave " + wave + " incoming!");
            }
        } else waveTimer = 0;
        for (Enemy z : new ArrayList<>(enemies)) {
            if (z.flash > 0) z.flash--;
            if (z.attackCooldown > 0) z.attackCooldown--;
            if (z.entering) {
                z.face = "left"; z.x -= z.entranceSpeed();
                double stop = camera + W - (z.type == 3 ? 70 : 46);
                if (z.x <= stop) {
                    z.x = stop; z.entering = false;
                    if (!seen[z.type]) { firstEncounter(z); repaint(); return; }
                }
                continue;
            }
            double vx = px - z.x, vy = py - z.y, d = Math.hypot(vx, vy);
            if (d > 28 && d < 1200) {
                double nx = z.x + vx / d * z.speed();
                double ny = z.y + vy / d * z.speed();
                if (walkable(nx, z.y)) z.x = nx;
                if (walkable(z.x, ny)) z.y = ny;
                z.face = Math.abs(vx) > Math.abs(vy) ? (vx > 0 ? "right" : "left") : (vy > 0 ? "down" : "up");
            }
            if (d < 34 && z.attackCooldown == 0) {
                z.attackCooldown = 105;
                if (hurtTicks == 0) {
                    hp -= z.type == 3 ? 17 : 8;
                    hurtTicks = 50; // red blinking
                    message("You were hit! Press H to heal.");
                    if (hp <= 0) { hp = 0; dead = true; }
                }
            }
        }
        for (Potion p : new ArrayList<>(shots)) {
            if (!enemies.contains(p.enemy)) { shots.remove(p); continue; }
            p.move();
            if (p.t >= 1) {
                shots.remove(p);
                Enemy z = p.enemy;
                if (p.type == z.type) {
                    z.hp -= 2; z.flash = 14;
                    if (z.hp <= 0) {
                        enemies.remove(z); stabilized[z.type]++; reward();
                        if (z.type == 3) { won = true; message("Mutated Survivor stabilized! Level 1 complete."); }
                    }
                } else message("Wrong treatment for " + NAMES[z.type] + ".");
            }
        }
        repaint();
    }
    private void reward() {
        // Every stabilized zombie awards coins: the shop must remain useful.
        coins += 5;
        if (random.nextDouble() >= .55) {
            message("Zombie stabilized! +5 coins");
            return;
        }
        int roll = random.nextInt(3);
        if (roll == 0) {
            xp += 10;
            message("Zombie stabilized! +5 coins, +10 XP");
        } else if (roll == 1) {
            coins += 5;
            message("Zombie stabilized! +10 coins");
        } else {
            int[] pool = {1, 0, seen[1] ? 5 : 1, seen[2] ? 4 : 1};
            int chem = pool[random.nextInt(pool.length)];
            stock[chem]++;
            message("Zombie stabilized! +5 coins, +1 " + CHEMS[chem]);
        }
    }

    // Shop: coins are deducted only if the player can afford the ingredient.
    private boolean buyIngredient(int ingredient) {
        if (ingredient < 0 || ingredient >= CHEMS.length) return false;
        int price = SHOP_PRICES[ingredient];
        if (coins < price) {
            labNotice = "Not enough coins for " + CHEMS[ingredient] + ". Need "
                    + price + ", have " + coins + ".";
            return false;
        }
        coins -= price;
        stock[ingredient]++;
        labNotice = "Purchased 1 " + CHEMS[ingredient] + " for " + price
                + " coins. Owned: " + stock[ingredient] + ".";
        return true;
    }

    // POTION FIX: recipes and selection are ingredient INDEXES, never strings.
    // Detect which treatment the ingredient combination actually makes.
    // This fixes crafting Common after Runner (and vice versa) without
    // requiring the player to manually switch treatment tabs first.
    private int detectedRecipe() {
        for (int recipe = 0; recipe < RECIPES.length; recipe++) {
            // Recognition depends only on the ingredients, not the previous potion or selected tab.
            boolean matches = true;
            for (int ingredient = 0; ingredient < selected.length; ingredient++) {
                boolean needed = false;
                for (int required : RECIPES[recipe]) {
                    if (required == ingredient) { needed = true; break; }
                }
                if (selected[ingredient] != needed) {
                    matches = false;
                    break;
                }
            }
            if (matches) return recipe;
        }
        return -1;
    }

    private boolean correctRecipe() { return detectedRecipe() >= 0; }

    private int maxBatch(int recipe) {
        int max = Integer.MAX_VALUE;
        for (int ingredient : RECIPES[recipe])
            max = Math.min(max, stock[ingredient]);
        return Math.max(0, max);
    }

    // Use the identified recipe's ingredients for MAX and quantity controls.
    private int maxBatch() {
        int detected = detectedRecipe();
        return maxBatch(detected >= 0 ? detected : target);
    }

    private String missingIngredients(int recipe) {
        StringBuilder missing = new StringBuilder();
        for (int ingredient : RECIPES[recipe]) {
            if (stock[ingredient] <= 0) {
                if (missing.length() > 0) missing.append(", ");
                missing.append(CHEMS[ingredient]);
            }
        }
        return missing.toString();
    }

    // Already discovered formulas can be selected again with one click.
    // Unknown formulas still need to be discovered through chemistry clues.
    private void chooseTreatment(int recipe) {
        target = recipe;
        batchQty = 1;
        Arrays.fill(selected, false);
        if (unlockedRecipes[recipe]) {
            for (int ingredient : RECIPES[recipe]) selected[ingredient] = true;
            labNotice = "Known recipe loaded: " + NAMES[recipe] + ". Select quantity and mix.";
        } else {
            labNotice = "Treatment selected: " + NAMES[recipe] + ". Read Recipes for clues.";
        }
    }

    private int craftingSeconds(int t) { return t == 3 ? 60 : t == 2 ? 45 : t == 1 ? 35 : 25; }
    private void openLab() {
        boolean any = false;
        for (boolean b : seen) if (b) any = true;
        if (!any) { message("Encounter a zombie first."); return; }
        lab = true; labTab = 0;
        batchQty = 1;
        Arrays.fill(selected, false); // Never carry ingredients from the previous mix.
        labNotice = "Choose a treatment or select the ingredients for a new recipe.";
        labTicks = craftingSeconds(target) * 60;
    }
    private void craft() {
        int recipe = detectedRecipe();
        if (recipe < 0) {
            labTicks = Math.max(0, labTicks - 180);
            labNotice = "Incorrect mixture! -3 seconds. Clear old selections and check Recipes.";
            return;
        }
        int max = maxBatch(recipe);
        if (max < 1) {
            labNotice = "Missing ingredients: " + missingIngredients(recipe) + ". Check inventory.";
            return;
        }
        if (batchQty > max) {
            batchQty = max;
            labNotice = "Quantity adjusted to " + max + " based on available ingredients.";
            return;
        }
        // Each potion uses one of EACH ingredient in the identified recipe.
        // No arbitrary limit on total vials; inventory is the only limit.
        for (int ingredient : RECIPES[recipe]) stock[ingredient] -= batchQty;
        vials[recipe] += batchQty;
        unlockedRecipes[recipe] = true;
        target = recipe;
        equipped = recipe;
        lab = false;
        Arrays.fill(selected, false);
        message("Crafted " + batchQty + " " + NAMES[recipe] + " potion(s)! Press SPACE or click to throw.");
    }
    private void equipPotion(int type) {
        if (!canPlay() || type < 0 || type >= vials.length) return;
        equipped = type;
        if (vials[type] > 0) message("Equipped: " + NAMES[type] + " potion (x" + vials[type] + ")");
        else message("No " + NAMES[type] + " potions yet. Press M to craft.");
    }
    private void switchPotion() {
        if (!canPlay()) return;
        for (int n = 1; n <= 4; n++) {
            int t = (equipped + n) % 4;
            if (vials[t] > 0) { equipped = t; message("Equipped: " + NAMES[t] + " treatment"); return; }
        }
    }
    private void heal() {
        if (!canPlay() || medkits == 0 || hp >= 100) return;
        medkits--; hp = Math.min(100, hp + 35); message("Medkit used. +35 HP.");
    }
    private Enemy autoTarget() {
        Enemy best = null; double score = Double.POSITIVE_INFINITY;
        for (Enemy z : enemies) {
            if (z.entering) continue;
            double dx = z.x - px, dy = z.y - py, dist = Math.hypot(dx, dy);
            boolean close = dist <= 305;
            boolean aligned =
                (facing.equals("right") && dx > 0 && Math.abs(dy) < 77 && dx <= 440) ||
                (facing.equals("left") && dx < 0 && Math.abs(dy) < 77 && -dx <= 440) ||
                (facing.equals("up") && dy < 0 && Math.abs(dx) < 77 && -dy <= 385) ||
                (facing.equals("down") && dy > 0 && Math.abs(dx) < 77 && dy <= 385);
            if (!close && !aligned) continue;
            double s = dist - (aligned ? 65 : 0) - (z.type == equipped ? 40 : 0);
            if (s < score) { score = s; best = z; }
        }
        return best;
    }
    private void throwPotion(Enemy clicked) {
        if (!canPlay() || throwCooldown > 0) return;
        if (vials[equipped] < 1) { message("No potion equipped. Press M to craft."); return; }
        Enemy z = clicked == null ? autoTarget() : clicked;
        if (z == null) { message("No zombie in range. Move closer or face the zombie."); return; }
        double d = Math.hypot(z.x - px, z.y - py);
        shots.add(new Potion(z, equipped, px + (facing.equals("right") ? 9 : facing.equals("left") ? -9 : 0), py - 39, d));
        vials[equipped]--; throwCooldown = 26;
    }

    private void click(int x, int y) {
        if (encounter) {
            FontMetrics fm = getFontMetrics(new Font("SansSerif", Font.PLAIN, 11));
            if (popupLayout(fm).button().contains(x, y)) encounter = false;
            return;
        }
        if (dead || won) {
            if (new Rectangle(362, 325, 236, 38).contains(x, y)) {
                if (won && onWin != null) onWin.run(); else reset();
            }
            return;
        }
        if (lab) { clickLab(x, y); return; }
        if (journal) { clickJournal(x, y); return; }
        if (paused) { paused = false; return; }
        for (int i = 0; i < 4; i++) {
            int slotX = HOTBAR_X + i * (HOTBAR_SLOT_W + HOTBAR_GAP);
            if (new Rectangle(slotX, HOTBAR_Y, HOTBAR_SLOT_W, HOTBAR_SLOT_H).contains(x, y)) {
                equipPotion(i);
                return;
            }
        }
        if (new Rectangle(568, 473, 181, 48).contains(x, y)) { openLab(); return; }
        if (new Rectangle(760, 473, 184, 48).contains(x, y)) { journal = true; return; }
        Enemy clicked = null;
        for (Enemy z : enemies) {
            if (z.entering) continue;
            if (Math.hypot(z.x - camera - x, z.y - 34 - y) < 46 && Math.hypot(z.x - px, z.y - py) < 470) {
                clicked = z; break;
            }
        }
        throwPotion(clicked);
    }
    private void clickLab(int x, int y) {
        if (new Rectangle(885, 29, 30, 29).contains(x, y)) { lab = false; return; }
        for (int i = 0; i < 5; i++) {
            if (new Rectangle(44, 110 + i * 57, 138, 44).contains(x, y)) { labTab = i; return; }
        }
        if (labTab == 0) {
            for (int i = 0; i < 4; i++) {
                if (new Rectangle(211 + i * 172, 99, 162, 30).contains(x, y) && seen[i]) {
                    chooseTreatment(i);
                    return;
                }
            }
            for (int i = 0; i < CHEMS.length; i++) {
                int cx = 210 + (i % 2) * 164, cy = 174 + (i / 2) * 54;
                if (new Rectangle(cx, cy, 155, 47).contains(x, y)) {
                    selected[i] = !selected[i]; return;
                }
            }
            // Quantity controls: one potion, more, or maximum affordable batch.
            if (new Rectangle(604, 406, 35, 24).contains(x, y)) { batchQty = Math.max(1, batchQty - 1); return; }
            if (new Rectangle(681, 406, 35, 24).contains(x, y)) { batchQty = Math.min(Math.max(1, maxBatch()), batchQty + 1); return; }
            if (new Rectangle(727, 406, 66, 24).contains(x, y)) { batchQty = Math.max(1, maxBatch()); return; }
            if (new Rectangle(646, 443, 214, 36).contains(x, y)) craft();
        } else if (labTab == 1) {
            for (int i = 0; i < 4; i++) {
                if (new Rectangle(210 + (i % 2) * 350, 157 + (i / 2) * 147, 334, 131).contains(x, y) && seen[i]) {
                    chooseTreatment(i);
                    labTab = 0;
                    return;
                }
            }
        } else if (labTab == 3) {
            // Shop buttons correspond exactly to the ingredient cards drawn below.
            for (int i = 0; i < CHEMS.length; i++) {
                int cardX = 210 + (i % 2) * 350;
                int cardY = 151 + (i / 2) * 81;
                if (new Rectangle(cardX + 239, cardY + 18, 82, 34).contains(x, y)) {
                    buyIngredient(i);
                    return;
                }
            }
        }
    }
    private void clickJournal(int x, int y) {
        if (new Rectangle(882, 34, 30, 29).contains(x, y)) { journal = false; return; }
        for (int i = 0; i < 4; i++) {
            if (new Rectangle(56 + i * 213, 101, 201, 33).contains(x, y)) { journalTab = i; return; }
        }
        if (journalTab == 2) {
            for (int i = 0; i < 4; i++) {
                if (new Rectangle(61, 163 + i * 71, 243, 58).contains(x, y)) { journalChoice = i; return; }
            }
        }
    }

    private Popup popupLayout(FontMetrics fm) {
        Popup p = new Popup();
        int d = lineCount(DESC[encounterType], fm, 390);
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

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.scale(getWidth() / (double) W, getHeight() / (double) H);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(town, 0, 0, W, H, (int) camera, 0, (int) camera + W, H, null);
        Graphics2D propGraphics = (Graphics2D) g.create();
        propGraphics.translate(-camera, 0);
        for (Prop p : props) drawProp(propGraphics, p);
        propGraphics.dispose();
        for (Enemy z : enemies) drawEnemy(g, z);
        drawPlayer(g);
        for (Potion p : shots) drawPotion(g, p);
        // Targeting remains automatic; no green bar or target indicator above zombies.
        drawHUD(g);
        if (encounter) drawEncounter(g);
        else if (lab) drawLab(g);
        else if (journal) drawJournal(g);
        else if (paused) drawEnd(g, "PAUSED", "Press P or click to resume", false);
        else if (dead) drawEnd(g, "GAME OVER", "Your scientist was overwhelmed.", false);
        else if (won) drawEnd(g, "LEVEL 1 COMPLETE", "The Mutated Survivor has been stabilized!", true);
        g.dispose();
    }
    private void drawObstacle(Graphics2D g, Rectangle r) {
        int x = r.x - (int) camera;
        if (x + r.width < 0 || x > W) return;
        g.setColor(new Color(15, 13, 26, 100));
        g.fillOval(x - 12, r.y + r.height - 8, r.width + 24, 24);
        if (r.width > 90) {
            g.setColor(new Color(55, 59, 73)); g.fillRoundRect(x, r.y - 35, r.width, r.height + 34, 10, 10);
            g.setColor(new Color(101, 102, 116)); g.fillRect(x + 14, r.y - 25, r.width - 28, 23);
            g.setColor(new Color(41, 34, 54)); g.fillRect(x + 18, r.y - 21, r.width - 36, 16);
        } else {
            g.setColor(new Color(99, 74, 88)); g.fillRect(x, r.y - 30, r.width, r.height + 30);
            g.setColor(new Color(143, 110, 118)); g.drawRect(x + 4, r.y - 25, r.width - 8, r.height + 20);
            g.drawLine(x + 4, r.y - 25, x + r.width - 4, r.y + r.height - 5);
        }
    }

    private void drawProp(Graphics2D g, Prop p) {
        int x = p.x, y = p.y, w = p.w;
        g.setColor(new Color(9, 8, 22, 125));
        g.fillOval(x - w / 2, y - 13, w, 24);
        switch (p.kind) {
            case "barrier":
                g.setColor(new Color(17, 15, 29));
                g.fillRect(x - w / 2, y - 39, w, 43);
                g.setColor(new Color(93, 83, 108));
                g.fillRect(x - w / 2 + 3, y - 45, w - 6, 32);
                g.setColor(new Color(143, 128, 142));
                g.fillPolygon(new int[]{x - w / 2 + 3, x - w / 2 + 17, x + w / 2 + 5, x + w / 2 - 3},
                    new int[]{y - 45, y - 55, y - 55, y - 45}, 4);
                g.setColor(new Color(38, 35, 49));
                g.fillRect(x - w / 2 + 7, y - 10, w - 14, 6);
                g.setColor(new Color(215, 163, 65));
                for (int i = 0; i < 5; i++) {
                    int bx = x - w / 2 + 8 + i * 21;
                    g.fillPolygon(new int[]{bx, bx + 8, bx + 14, bx + 6},
                        new int[]{y - 44, y - 44, y - 34, y - 34}, 4);
                }
                break;
            case "barrel":
                g.setColor(new Color(19, 19, 34));
                g.fillRoundRect(x - 18, y - 43, 36, 47, 7, 6);
                g.setColor(new Color(65, 62, 88));
                g.fillRect(x - 15, y - 37, 30, 32);
                g.setColor(new Color(117, 108, 129));
                g.fillOval(x - 18, y - 48, 36, 13);
                g.setColor(new Color(31, 28, 48));
                g.fillOval(x - 14, y - 45, 28, 8);
                g.setColor(new Color(229, 179, 58));
                g.setFont(new Font("SansSerif", Font.BOLD, 22));
                g.drawString("☣", x - 11, y - 10);
                g.setColor(new Color(26, 24, 40));
                g.fillRect(x - 16, y - 8, 32, 4);
                break;
            case "crate":
                g.setColor(new Color(18, 15, 28));
                g.fillRect(x - 32, y - 48, 65, 53);
                g.setColor(new Color(91, 69, 84));
                g.fillRect(x - 29, y - 44, 58, 44);
                g.setColor(new Color(141, 104, 96));
                g.fillPolygon(new int[]{x - 29, x - 15, x + 38, x + 29},
                    new int[]{y - 44, y - 56, y - 56, y - 44}, 4);
                g.setColor(new Color(46, 33, 51));
                g.setStroke(new BasicStroke(4));
                g.drawLine(x - 25, y - 40, x + 24, y - 3);
                g.drawLine(x + 24, y - 40, x - 25, y - 3);
                g.setStroke(new BasicStroke(1));
                break;
            case "van":
                g.setColor(new Color(13, 12, 26));
                g.fillOval(x - 80, y - 19, 164, 27);
                g.setColor(new Color(26, 24, 40));
                g.fillRoundRect(x - 77, y - 58, 156, 50, 10, 9);
                g.setColor(new Color(102, 73, 97));
                g.fillPolygon(new int[]{x - 67, x - 39, x + 43, x + 67},
                    new int[]{y - 58, y - 91, y - 91, y - 58}, 4);
                g.setColor(new Color(37, 35, 58));
                g.fillPolygon(new int[]{x - 51, x - 34, x + 35, x + 52},
                    new int[]{y - 61, y - 83, y - 83, y - 61}, 4);
                g.setColor(new Color(129, 92, 106));
                g.fillRect(x - 71, y - 51, 143, 32);
                g.setColor(new Color(42, 37, 55));
                g.fillRect(x - 67, y - 36, 134, 13);
                g.setColor(new Color(21, 19, 31));
                g.fillOval(x - 56, y - 25, 27, 27);
                g.fillOval(x + 34, y - 25, 27, 27);
                g.setColor(new Color(108, 99, 118));
                g.fillOval(x - 49, y - 19, 13, 13);
                g.fillOval(x + 41, y - 19, 13, 13);
                g.setColor(new Color(123, 34, 56, 175));
                g.fillRect(x - 56, y - 49, 29, 4);
                g.fillRect(x + 5, y - 53, 35, 3);
                g.fillRect(x + 30, y - 32, 18, 3);
                // Shattered windows, rust, and a broken door on the wreck.
                g.setColor(new Color(178, 149, 156, 135));
                g.drawLine(x - 29, y - 81, x - 8, y - 63);
                g.drawLine(x - 8, y - 63, x + 6, y - 82);
                g.drawLine(x + 7, y - 80, x + 29, y - 65);
                g.setColor(new Color(69, 38, 48));
                g.fillRect(x - 50, y - 47, 24, 5);
                g.fillRect(x + 18, y - 48, 33, 4);
                g.fillRect(x - 11, y - 55, 9, 7);
                g.setColor(new Color(16, 13, 27));
                g.drawLine(x - 3, y - 52, x - 10, y - 18);
                g.drawLine(x + 45, y - 49, x + 40, y - 20);
                break;
            case "fence":
                g.setColor(new Color(25, 24, 39));
                g.fillRect(x - w / 2, y - 62, w, 60);
                g.setColor(new Color(122, 115, 142));
                for (int i = 0; i < w; i += 12) {
                    g.drawLine(x - w / 2 + i, y - 58, x - w / 2 + i + 45, y - 5);
                    g.drawLine(x - w / 2 + i + 45, y - 58, x - w / 2 + i, y - 5);
                }
                g.setColor(new Color(65, 61, 83));
                g.fillRect(x - w / 2, y - 65, w, 4);
                g.fillRect(x - w / 2, y - 3, w, 4);
                g.fillRect(x - w / 2, y - 68, 5, 70);
                g.fillRect(x + w / 2 - 5, y - 68, 5, 70);
                break;
            case "lamp":
                g.setColor(new Color(20, 19, 33));
                g.fillRect(x - 5, y - 106, 11, 111);
                g.setColor(new Color(96, 85, 109));
                g.fillRect(x - 2, y - 104, 3, 104);
                g.setColor(new Color(21, 18, 31));
                g.fillRoundRect(x - 20, y - 115, 40, 17, 9, 8);
                g.setColor(GOLD);
                g.fillRect(x - 12, y - 102, 24, 7);
                g.setColor(new Color(255, 224, 142));
                g.fillRect(x - 8, y - 101, 16, 3);
                break;
        }
    }
    private void drawSprite(Graphics2D g, BufferedImage img, int x, int bottom, int height, int maxWidth, boolean red) {
        if (img == null) return;
        int width = clamp((int) Math.round(height * img.getWidth() / (double) Math.max(1, img.getHeight())), 20, maxWidth);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SPRITE_OPACITY));
        g.drawImage(red ? sprites.red(img) : sprites.dim(img), x - width / 2, bottom - height, width, height, null);
        g.setComposite(old);
    }
    private void drawPlayer(Graphics2D g) {
        int x = (int) (px - camera), y = (int) py;
        boolean moving = pressed.contains(KeyEvent.VK_W) || pressed.contains(KeyEvent.VK_A) ||
            pressed.contains(KeyEvent.VK_S) || pressed.contains(KeyEvent.VK_D) ||
            pressed.contains(KeyEvent.VK_UP) || pressed.contains(KeyEvent.VK_DOWN) ||
            pressed.contains(KeyEvent.VK_LEFT) || pressed.contains(KeyEvent.VK_RIGHT);
        String action = moving && pressed.contains(KeyEvent.VK_SHIFT) ? "run" : "walk";
        BufferedImage img = sprites.get(true, scientist, action, facing, moving ? ticks / 10 : 0);
        boolean red = hurtTicks > 0 && (ticks / 5) % 2 == 0;
        if (img != null) drawSprite(g, img, x, y, PLAYER_H, 59, red);
        else {
            g.setColor(red ? new Color(218, 72, 87) : new Color(161, 126, 204));
            g.fillRoundRect(x - 12, y - 49, 24, 45, 8, 8);
            g.setColor(new Color(207, 168, 150)); g.fillOval(x - 11, y - 68, 22, 24);
        }
        g.setColor(red ? new Color(240, 102, 119, 170) : new Color(179, 148, 234, 130));
        g.drawOval(x - 15, y - 5, 30, 9);
    }
    private void drawEnemy(Graphics2D g, Enemy z) {
        int x = (int) (z.x - camera), y = (int) z.y;
        if (x < -110 || x > W + 110) return;
        String name = z.type == 3 ? "mutated_survivor" : TYPES[z.type] + "_zombie";
        String action = z.attackCooldown > 78 ? "attack" : z.type == 1 ? "run" : z.type == 2 ? "crawl" : "walk";
        BufferedImage img = sprites.get(false, name, action, z.face, ticks / (z.type == 1 ? 8 : 12));
        int height = z.type == 3 ? BOSS_H : ZOMBIE_H;
        if (img != null) drawSprite(g, img, x, y, height, z.type == 3 ? 85 : 63, z.flash > 0);
        else {
            g.setColor(z.flash > 0 ? new Color(220, 80, 100) : new Color(99, 112, 92));
            g.fillRoundRect(x - 14, y - height + 19, 28, height - 22, 7, 7);
            g.fillOval(x - 13, y - height, 26, 27);
            g.setColor(new Color(220, 91, 107));
            g.fillRect(x - 8, y - height + 12, 4, 3); g.fillRect(x + 4, y - height + 12, 4, 3);
        }
        // No floating HP bar above zombies (requested).
    }
    private void drawPotion(Graphics2D g, Potion p) {
        int x = (int) (p.x - camera), y = (int) p.y;
        g.setColor(new Color(146, 226, 175, 45)); g.fillOval(x - 11, y - 11, 22, 22);
        g.setColor(new Color(147, 231, 175)); g.fillOval(x - 5, y - 6, 10, 12);
        g.setColor(WHITE); g.fillRect(x - 2, y - 9, 4, 5);
        g.setColor(new Color(60, 125, 94)); g.fillRect(x - 3, y - 1, 6, 5);
    }
    private void drawLock(Graphics2D g) {
        Enemy z = autoTarget();
        if (z == null || vials[equipped] < 1) return;
        int x = (int) (z.x - camera), y = (int) z.y;
        g.setColor(z.type == equipped ? new Color(151, 221, 172, 185) : new Color(236, 180, 106, 185));
        g.drawOval(x - 25, y - 12, 50, 17);
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
        label(g, "ABANDONED TOWN", 413, 33, 10, MUTED, true);
        label(g, "WAVE " + wave + " / 4", 426, 55, 16, PURPLE, true);
        hudPanel(g, 721, 14, 225, 75);
        label(g, "XP  " + xp, 735, 38, 13, new Color(186, 222, 166), true);
        label(g, "COINS  " + coins, 827, 38, 13, new Color(243, 210, 148), true);
        int total = 0; for (int n : stabilized) total += n;
        label(g, "STABILIZED  " + total, 735, 61, 11, WHITE, true);
        label(g, "MEDKITS  " + medkits, 846, 61, 10, WHITE, true);
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
            label(g, CHEMS[i], x + 8, y + 19, 10, WHITE, false);
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
    // Ingredient Shop replaces the old Equipment tab. The rest of the lab stays intact.
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
        label(g, "OBSERVATION LOG - LEVEL 1", 210, 128, 14, PURPLE, true);
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
        label(g, "LEVEL 1 / ABANDONED TOWN", 640, 59, 11, MUTED, true);
        button(g, 882, 34, 30, 29, "X", new Color(102, 63, 138));
        for (int i = 0; i < 4; i++)
            button(g, 56 + i * 213, 101, 201, 33, TABS[i], i == journalTab ? new Color(111, 70, 163) : new Color(48, 38, 69));
        if (journalTab == 2) drawJournalZombies(g);
        else if (journalTab == 1) {
            label(g, "PERIODIC TABLE - RELEVANT ELEMENTS", 62, 169, 14, PURPLE, true);
            String[] elements = {"H / Hydrogen", "O / Oxygen", "Na / Sodium", "Cl / Chlorine", "C / Carbon", "Fe / Iron", "Cu / Copper", "S / Sulfur"};
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
            wrap(g, "This zombie's information unlocks when it first enters the Abandoned Town battlefield.",
                    345, 225, 523, 12, 19, WHITE);
            return;
        }
        label(g, NAMES[i].toUpperCase(Locale.ROOT), 345, 191, 16, PURPLE, true);
        label(g, "TYPE: " + (i == 3 ? "Boss Mutant" : "Infected Human"), 345, 220, 11, WHITE, true);
        label(g, "DIFFICULTY: " + (i == 0 ? "Easy" : i == 1 ? "Moderate" : i == 2 ? "Challenging" : "Boss"), 345, 241, 11, WHITE, true);
        label(g, "STATUS: " + (stabilized[i] > 0 ? "Completed" : "In Progress"), 345, 262, 11, WHITE, true);
        label(g, "OBSERVED CONDITION", 345, 290, 11, new Color(244, 204, 151), true);
        int next = wrap(g, CONDITION[i], 345, 308, 521, 11, 16, WHITE);
        label(g, "RESEARCH NOTES", 345, next + 10, 11, PURPLE, true);
        int end = wrap(g, CLUE[i], 345, next + 28, 521, 11, 16, WHITE);
        if (unlockedRecipes[i])
            label(g, fit(g, "UNLOCKED FORMULA: " + FORMULA[i], 522, 10), 345, Math.min(470, end + 13),
                    10, new Color(186, 231, 165), true);
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
    private void centered(Graphics2D g, String text, int x, int y, int w, int h, int size, Color color, boolean bold) {
        g.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, size));
        FontMetrics fm = g.getFontMetrics(); g.setColor(color);
        g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }
    private void label(Graphics2D g, String text, int x, int y, int size, Color color, boolean bold) {
        g.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, size));
        g.setColor(color); g.drawString(text, x, y);
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

    private static class SpriteLoader {
        private final Map<String, BufferedImage[]> cache = new HashMap<>();
        private final Set<String> missing = new HashSet<>();
        private final Map<BufferedImage, BufferedImage> dimCache = new IdentityHashMap<>();
        private final Map<BufferedImage, BufferedImage> redCache = new IdentityHashMap<>();
        private final File assets;
        SpriteLoader() {
            File root = new File("assets");
            if (!root.isDirectory()) root = new File("../assets");
            assets = root;
        }
        BufferedImage get(boolean player, String name, String action, String direction, int frame) {
            String root = (player ? "characters/" : "enemies/level1/") + name;
            BufferedImage[] frames = load(root, name, action, direction);
            if (frames == null) {
                String fallback = player ? "walk" : name.equals("runner_zombie") ? "run" : name.equals("crawler_zombie") ? "crawl" : "walk";
                if (!action.equals(fallback)) frames = load(root, name, fallback, direction);
            }
            return frames == null ? null : frames[Math.floorMod(frame, frames.length)];
        }
        private BufferedImage[] load(String root, String name, String action, String direction) {
            String key = root + "|" + action + "|" + direction;
            if (missing.contains(key)) return null;
            if (cache.containsKey(key)) return cache.get(key);
            List<File> files = new ArrayList<>(); findPng(new File(assets, root), files, 0);
            String d = norm(direction);
            String alt = norm(direction.equals("down") ? "front" : direction.equals("up") ? "back" : direction);
            String a = norm(action), n = norm(name);
            File best = null; int bestScore = Integer.MIN_VALUE;
            for (File f : files) {
                String base = norm(f.getName().replaceFirst("(?i)\\.png$", ""));
                if (!base.endsWith(d) && !base.endsWith(alt)) continue;
                String parent = norm(f.getParentFile().getName());
                // Don't accidentally use an unrelated animation as the requested action.
                if (!base.contains(a) && !parent.equals(a) && !(action.equals("crawl") && parent.equals("walk"))) continue;
                int score = (base.contains(n) ? 10 : 0) + (base.contains(a) ? 20 : 0) +
                        (parent.equals(a) ? 7 : 0) + (base.endsWith(d) ? 3 : 0);
                if (score > bestScore) { bestScore = score; best = f; }
            }
            if (best == null) { missing.add(key); return null; }
            try {
                BufferedImage sheet = ImageIO.read(best);
                if (sheet == null) throw new IllegalArgumentException("Unreadable PNG");
                BufferedImage[] frames = extract(sheet);
                cache.put(key, frames); return frames;
            } catch (Exception ex) {
                System.err.println("Sprite error: " + best + " / " + ex.getMessage());
                missing.add(key); return null;
            }
        }
        private static String norm(String s) { return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
        private static void findPng(File dir, List<File> out, int depth) {
            if (depth > 3 || !dir.isDirectory()) return;
            File[] list = dir.listFiles(); if (list == null) return;
            for (File f : list) {
                if (f.isDirectory()) findPng(f, out, depth + 1);
                else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".png")) out.add(f);
            }
        }
        // Source sprite sheets do NOT all have four frames: some have FIVE.
        // Cutting a 5-frame sheet into 4 equal pieces draws the next character's
        // hair/limbs beside the current sprite. Detect real transparent gutters.
        // Also support a single-frame PNG without slicing it into quarters.
        private static BufferedImage[] extract(BufferedImage sheet) {
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
        private static BufferedImage crop(BufferedImage src) {
            int minX = src.getWidth(), minY = src.getHeight(), maxX = -1, maxY = -1;
            for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
                if ((src.getRGB(x, y) >>> 24) > 20) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
            if (maxX < 0) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            minX = Math.max(0, minX - 2); minY = Math.max(0, minY - 2);
            maxX = Math.min(src.getWidth() - 1, maxX + 2);
            maxY = Math.min(src.getHeight() - 1, maxY + 2);
            BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), minX, minY, maxX + 1, maxY + 1, null);
            g.dispose(); return out;
        }
        BufferedImage dim(BufferedImage img) { return tint(img, false); }
        BufferedImage red(BufferedImage img) { return tint(img, true); }
        private BufferedImage tint(BufferedImage img, boolean red) {
            Map<BufferedImage, BufferedImage> cache = red ? redCache : dimCache;
            BufferedImage result = cache.get(img); if (result != null) return result;
            result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y), a = (c >>> 24) & 255;
                int r = (c >>> 16) & 255, gr = (c >>> 8) & 255, b = c & 255;
                if (red) { r = Math.min(255, (int) (r * .35) + 165); gr = (int) (gr * .22); b = (int) (b * .24); }
                else { r = (int) (r * .89); gr = (int) (gr * .89); b = (int) (b * .89); }
                result.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
            }
            cache.put(img, result); return result;
        }
    }
    // Running this file launches the full game, never a standalone test battlefield.
    // Main is in the default package, so a packaged class calls it by reflection.
    public static void main(String[] args) {
        try {
            Class<?> mainMenu = Class.forName("Main");
            mainMenu.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (ReflectiveOperationException e) {
            System.err.println("Could not open CHEMION Main Menu. Compile Main.java and its dependencies first.");
            e.printStackTrace();
        }
    }
}
