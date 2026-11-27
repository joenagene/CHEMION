package battlefield.level5;

import game.AudioManager;
import game.HealthEffects;

import battlefield.level1.SaveManager;
import battlefield.level1.SaveManager;
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

public final class Level5GamePanel extends JPanel {
    // Constants
    private static final int W = 960, H = 540, WORLD = 7200;

    private static final int WALK_MIN_Y = 110, WALK_MAX_Y = 1250;
    private static final int FLOOR_TOP = 229, FLOOR_BOTTOM = 448;

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
        "elite_infected", "biohazard_guardian", "alpha_mutant", "patient_zero_the_origin"
    };

    private static final double[] ENEMY_BASE_SPEED = {1.02, 0.88, 1.12, 0.95};
    private static final int SHATTER_TICKS = 17;
    private static final String[] NAMES = {
        "Elite Infected", "Biohazard Guardian", "Alpha Mutant", "Patient Zero - The Origin"
    };
    private static final String[] SHORT_NAMES = {"ELITE", "GUARDIAN", "ALPHA", "ORIGIN"};

    private static final int HOTBAR_X = 22, HOTBAR_Y = 468, HOTBAR_SLOT_W = 126;
    private static final int HOTBAR_SLOT_H = 51, HOTBAR_GAP = 6;

    private static final String[] CHEMS = {
        "Distilled Water", "Activated Carbon", "Sodium Bicarbonate", "Zeolite",
        "Silica Gel", "Calcium Carbonate", "Sodium Thiosulfate", "Potassium Iodide"
    };
    private static final int[] PRICES = {2, 2, 4, 5, 4, 4, 3, 5};
    private static final int[] START_STOCK = {12, 9, 8, 8, 8, 7, 7, 7};
    private static final int[][] RECIPES = {
        {0, 1, 2, 4},
        {0, 1, 3, 4, 6},
        {0, 2, 5, 6, 7},
        {0, 1, 2, 3, 6, 7}
    };
    private static final String[] FORMULAS = {
        "Water + Activated Carbon + Sodium Bicarbonate + Silica Gel",
        "Water + Activated Carbon + Zeolite + Silica Gel + Sodium Thiosulfate",
        "Water + Sodium Bicarbonate + Calcium Carbonate + Sodium Thiosulfate + Potassium Iodide",
        "Water + Activated Carbon + Sodium Bicarbonate + Zeolite + Sodium Thiosulfate + Potassium Iodide"
    };
    // Ingredient clues
    private static final String[] CLUES = {
        "Find a purified solvent, porous carbon adsorbent, mild bicarbonate base and moisture adsorbent.",
        "Find a purified solvent, porous carbon adsorbent, ion-exchange mineral, moisture adsorbent and chlorine-reducing agent.",
        "Find a purified solvent, mild bicarbonate base, calcium carbonate source, chlorine-reducing agent and iodide salt.",
        "Find a purified solvent, porous carbon adsorbent, mild bicarbonate base, ion-exchange mineral, chlorine-reducing agent and iodide salt."
    };
    private static final String[] CONDITIONS = {
        "Exposure caused acidic buildup, accumulated contaminants and abnormal moisture retention.",
        "Hazardous exposure left chemical contaminants, unwanted ions, excess moisture and reactive oxidants.",
        "Repeated trials caused excessive acidity, calcium imbalance, reactive oxidants and iodide depletion.",
        "The original strain carries severe contamination, acidic residues, unwanted ions, reactive oxidants and iodide depletion."
    };
    private static final String[] OBJECTIVES = {
        "Develop a four-ingredient formula to neutralize acidity, adsorb contaminants and control moisture. Consult the journal for clues.",
        "Develop a five-ingredient formula to capture contaminants and ions, control moisture and reduce oxidants. Consult the journal.",
        "Develop a five-ingredient formula to address acidity, mineral imbalance and oxidation. Consult the journal for clues.",
        "Use five Alpha doses to break the shield, then five Origin doses to stabilize the core. Consult the journal for six ingredient clues."
    };
    // Zombie backgrounds
    private static final String[] DESCRIPTIONS = {
        "Former presidents and political leaders attended the official Genesis Project demonstration. When the experiment failed, they could not escape and were exposed to the unstable compound.",
        "The former containment chief stayed inside the facility after the directors refused to authorize evacuation. Exposure to hazardous experimental compounds during containment caused the mutation.",
        "A seriously ill volunteer joined the Genesis Project hoping for a cure. Repeated experimental treatments instead caused severe mutations and chemical imbalances.",
        "A scientist became the first human test subject of the Genesis Project. When the experiment failed, the directors concealed the incident and abandoned the scientist, allowing the original mutation to spread."
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

    // Game state
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
    private double cameraY;
    private int transition, cinematic;
    private boolean sealReady, sealIntroSeen;
    private int sealIntroTicks;
    private boolean sealIntroActive;
    private int room() { return roomEntered ? wave : wave-1; }
    private int base() { return room()*1440; }
    private int roomWidth() { return room()==4 ? 1440 : room()==2 ? 1840 : 960; }
    private int roomHeight() { return room()==1 || room()==3 ? 1360 : room()==4 ? 1120 : 540; }
    private double doorX() { return base() + (room()==0 ? 925 : room()==1 ? 925 : room()==2 ? roomWidth()-35 : 480); }
    private double doorY() { return room()==1 ? 270 : room()==3 ? 1210 : 300; }
    private double terminalX() { return base() + (room()==2 ? 610 : room()==4 ? 1190 : 480); }
    private double terminalY() { return room()==1 ? 340 : room()==3 ? 920 : room()==4 ? 560 : 300; }
    private void updateCamera() { camera=clamp(px-W*.5,base(),base()+roomWidth()-W); cameraY=clamp(py-H*.55,0,roomHeight()-H); }

    private double px = 120, py = 240, camera, hp = 100;
    private int ticks, wave = 1, spawned, spawnTimer = 90, waveTimer;
    private int xp, coins = 55, medkits = 2, equipped, target, batchQty = 1;
    private int hurtTicks, throwCooldown, labTicks, labTab, journalTab = 2, journalChoice;
    private int noticeTicks, encounterType = -1, hoverControl;
    private String facing = "right";
    private String notice = "Genesis Laboratory: the doors remember what the records conceal.";
    private String labNotice = "Select a zombie and examine its chemistry clue.";
    private boolean paused, lab, journal, encounter, dead, won, winNotified;
    private boolean roomEntered, roomInvestigated;
    private boolean entranceVerified;
    private boolean needsEntranceVerification() { return wave == 1 && !roomEntered && !entranceVerified; }
    private double securityX() { return base()+410; }
    private double securityY() { return 310; }
    private boolean verifyEquation(int stage) {
        String[] reasons = {
            "Emergency lockdown: the Genesis demonstration failed. The security terminal requires a balanced reaction record to verify scientific access before opening the infected sector.",
            "The presentation hall guest archive is encrypted. Restore its incomplete reaction record to recover the identities of the infected guests.",
            "The containment leak corrupted the evacuation archive. Correct the atom balance in its reaction record to recover the containment chief's report.",
            "The directors restricted the patient trial records. Reconstruct the final reaction record to authenticate access to the hidden experiment files."
        };
        String[][] formulas = {{"H₂", "O₂", "H₂O"}, {"Al", "O₂", "Al₂O₃"}, {"NaHCO₃", "Na₂CO₃", "H₂O", "CO₂"}, {"Fe₂O₃", "CO", "Fe", "CO₂"}};
        int[][] answers = {{2,1,2},{4,3,2},{2,1,1,1},{1,3,2,3}};
        int[] split = {2,2,1,2};
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Genesis chemical verification", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(12,16));
        content.setBackground(new Color(32,24,48));
        content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PURPLE,2),BorderFactory.createEmptyBorder(20,20,20,20)));
        JLabel reason = new JLabel("<html><div style='width:500px'><b>GENESIS // CHEMICAL VERIFICATION</b><br><br>"+reasons[stage]+"<br><br>Balance the equation using the smallest positive whole-number coefficients. This is a simulated archive exercise, not a cure formula.</div></html>");
        reason.setForeground(WHITE); reason.setFont(new Font("SansSerif",Font.PLAIN,15)); content.add(reason,BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0,20));
        body.setOpaque(false);
        JPanel equation = new JPanel(new FlowLayout(FlowLayout.CENTER,12,10));
        equation.setOpaque(false);
        Color field = new Color(49,39,68);
        List<JComboBox<Integer>> coefficients = new ArrayList<>();
        for(int i=0;i<formulas[stage].length;i++) {
            if(i>0) { JLabel sign=new JLabel(i==split[stage]?"→":"+"); sign.setForeground(GOLD); sign.setFont(new Font("SansSerif",Font.BOLD,22)); equation.add(sign); }
            JComboBox<Integer> coefficient = new JComboBox<>(new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12});
            coefficient.setPreferredSize(new Dimension(90,40));
            coefficient.setBackground(field); coefficient.setForeground(WHITE);
            coefficient.setFont(new Font("SansSerif",Font.PLAIN,18));
            coefficient.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton arrow = new JButton() {
                        @Override public void paint(Graphics g) {
                            Graphics2D a = (Graphics2D) g.create();
                            a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            a.setColor(Color.WHITE);
                            a.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            int x=getWidth()/2, y=getHeight()/2;
                            a.drawLine(x-5,y-3,x,y+2); a.drawLine(x,y+2,x+5,y-3); a.dispose();
                        }
                    };
                    arrow.setPreferredSize(new Dimension(28,40)); arrow.setOpaque(false);
                    arrow.setBorderPainted(false); arrow.setContentAreaFilled(false); arrow.setFocusable(false);
                    return arrow;
                }
                @Override public void paint(Graphics g, JComponent c) {
                    Graphics2D a=(Graphics2D)g.create();
                    a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    a.setColor(field); a.fillRoundRect(1,1,c.getWidth()-3,c.getHeight()-3,24,24);
                    a.setColor(c.hasFocus()?PURPLE:new Color(111,91,139));
                    a.drawRoundRect(1,1,c.getWidth()-3,c.getHeight()-3,24,24);
                    a.setColor(WHITE); a.setFont(c.getFont());
                    FontMetrics fm=a.getFontMetrics();
                    a.drawString(String.valueOf(coefficient.getSelectedItem()),14,(c.getHeight()-fm.getHeight())/2+fm.getAscent());
                    a.dispose();
                }
            });
            coefficient.setOpaque(false);
            coefficient.setBorder(BorderFactory.createEmptyBorder());
            coefficient.setRenderer(new DefaultListCellRenderer(){
                @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
                    super.getListCellRendererComponent(list,value,index,selected,focus);
                    setBackground(selected?new Color(101,61,153):field); setForeground(WHITE); setOpaque(index>=0); return this;
                }
            });
            coefficients.add(coefficient); equation.add(coefficient);
            JLabel formula=new JLabel(formulas[stage][i]); formula.setForeground(WHITE); formula.setFont(new Font("SansSerif",Font.BOLD,22)); equation.add(formula);
        }
        body.add(equation,BorderLayout.NORTH);
        String[][] atoms={{"Hydrogen","Oxygen"},{"Aluminium","Oxygen"},{"Sodium","Hydrogen","Carbon","Oxygen"},{"Iron","Carbon","Oxygen"}};
        int[][][] counts={{{2,0,2},{0,2,1}},{{1,0,2},{0,2,3}},{{1,2,0,0},{1,0,2,0},{1,1,0,1},{3,3,1,2}},{{2,0,1,0},{0,1,0,1},{3,1,0,2}}};
        JPanel table=new JPanel(new GridLayout(atoms[stage].length+1,3,16,12));
        table.setOpaque(false); table.setBorder(BorderFactory.createEmptyBorder(8,12,12,12));
        for(String heading:new String[]{"Atom","Reactants","Products"}){
            JLabel label=new JLabel(heading); label.setForeground(PURPLE); label.setFont(new Font("SansSerif",Font.BOLD,16)); table.add(label);
        }
        JLabel[] left=new JLabel[atoms[stage].length], right=new JLabel[atoms[stage].length];
        for(int j=0;j<left.length;j++){
            JLabel atom=new JLabel(atoms[stage][j]); atom.setForeground(WHITE); atom.setFont(new Font("SansSerif",Font.PLAIN,16)); table.add(atom);
            left[j]=new JLabel(); right[j]=new JLabel();
            for(JLabel label:new JLabel[]{left[j],right[j]}){label.setFont(new Font("SansSerif",Font.PLAIN,18));table.add(label);}
        }
        Runnable refresh=()->{
            for(int j=0;j<left.length;j++){
                int l=0,r=0;
                for(int i=0;i<coefficients.size();i++){
                    int n=(Integer)coefficients.get(i).getSelectedItem()*counts[stage][j][i];
                    if(i<split[stage])l+=n;else r+=n;
                }
                left[j].setText(Integer.toString(l));right[j].setText(Integer.toString(r));
                left[j].setForeground(l==r?TEAL:RED);right[j].setForeground(l==r?TEAL:RED);
            }
        };
        for(JComboBox<Integer> coefficient:coefficients)coefficient.addActionListener(e->refresh.run());
        refresh.run(); body.add(table,BorderLayout.CENTER); content.add(body,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new GridLayout(0,1,0,8)); bottom.setOpaque(false);
        JLabel feedback=new JLabel("Check that each element has equal atoms on both sides."); feedback.setForeground(GOLD); bottom.add(feedback);
        JButton verify=roundedEquationButton("VERIFY EQUATION"), cancel=roundedEquationButton("BACK");
        for(JButton button:new JButton[]{verify,cancel}) { button.setBackground(new Color(101,61,153)); button.setForeground(WHITE); button.setFocusPainted(false); button.setPreferredSize(new Dimension(100,38)); bottom.add(button); }
        boolean[] passed={false};
        verify.addActionListener(e->{
            for(int i=0;i<coefficients.size();i++) if((Integer)coefficients.get(i).getSelectedItem()!=answers[stage][i]) { feedback.setText("Not correct. Check atom counts and use the smallest ratio."); return; }
            passed[0]=true; dialog.dispose();
        });
        cancel.addActionListener(e->dialog.dispose()); content.add(bottom,BorderLayout.SOUTH);
        dialog.setContentPane(content); dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); dialog.getRootPane().setDefaultButton(verify); dialog.pack(); dialog.setLocationRelativeTo(this);
        boolean running=timer.isRunning(); timer.stop(); keys.clear();
        try { dialog.setVisible(true); return passed[0]; }
        finally { dialog.dispose(); keys.clear(); if(running)timer.start(); requestFocusInWindow(); }
    }
    private JButton roundedEquationButton(String text) {
        JButton button=new JButton(text) {
            @Override public void paint(Graphics g) {
                Graphics2D a=(Graphics2D)g.create();
                a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                a.setColor(getModel().isPressed()?new Color(82,48,128):getModel().isRollover()?new Color(126,79,181):getBackground());
                int arc=getHeight()-4;
                a.fillRoundRect(1,1,getWidth()-3,getHeight()-3,arc,arc);
                a.setColor(new Color(155,119,205)); a.drawRoundRect(1,1,getWidth()-3,getHeight()-3,arc,arc);
                a.setColor(Color.WHITE); a.setFont(getFont());
                FontMetrics fm=a.getFontMetrics();
                a.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()-fm.getHeight())/2+fm.getAscent());
                a.dispose();
            }
        };
        button.setOpaque(false); button.setContentAreaFilled(false); button.setBorderPainted(false);
        button.setRolloverEnabled(true); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
    private int fundedWave;
    private final boolean[] openedDoors = new boolean[4];
    private static final int[] DOOR_X = {632, 1264, 1896, 2528};
    private static final String[] ROOM_NAMES = {"PRESENTATION HALL", "CONTAINMENT VAULT", "EVOLUTION WING", "ORIGIN VAULT"};
    private static final String[] ROOM_CLUES = {
        "Guest records: presidents and senior politicians attended the Genesis demonstration. When the trial failed, they could not escape. Exposure transformed them into Elite Infected.",
        "Evacuation log: the directors refused evacuation to conceal the failed trial. The containment chief stayed to contain the leak and became infected. Filter tests suggest ion exchange and oxidant reduction.",
        "Patient file: an ill volunteer sought treatment but endured repeated failed trials. Tests recorded mineral imbalance and iodide depletion. Seal clue: purified solvent, mild bicarbonate base and ion-exchange mineral.",
        "Origin file: the first human subject was a scientist betrayed by the directors. They concealed the failed experiment instead of stopping it. The original strain spread through the facility. Five Alpha doses break the shield; five Origin doses stabilize the core."
    };
    private Enemy encountered;

    private static final class Popup {
        int x, y, h, objectiveY, objectiveH, buttonY;
        Rectangle button() { return new Rectangle(x + 311, y + buttonY, 96, 25); }
    }
    private static final class Enemy {
        final int type;
        double x, y;
        int hp, cooldown, flash, slowed, charge, attackTicks;
        int bossPhaseOneHits, bossPhaseTwoHits;

        double speedFactor = 1.0;
        String face = "left";
        Enemy(int type, double x, double y) {
            this.type = type; this.x = x; this.y = y;
            this.hp = type == 3 ? 10 : type == 0 ? 4 : type == 1 ? 5 : 6;
        }
    }

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

    private static final class EnemyShot {
        final int type;
        double x, y, vx, vy;
        int ttl;
        EnemyShot(int type, double x, double y, double vx, double vy, int ttl) {
            this.type = type; this.x = x; this.y = y;
            this.vx = vx; this.vy = vy; this.ttl = ttl;
        }
    }

    public Level5GamePanel(String scientist, Runnable onWin, Runnable onMenu) {
        this.scientist = scientist == null ? "nicole" : scientist;
        GameplayBridge.startLevel(5, this.scientist);
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
        bind(KeyEvent.VK_E, "interactDoor", this::interactWithRoom);
        bind(KeyEvent.VK_M, "lab", this::toggleLab);
        bind(KeyEvent.VK_J, "journal", this::toggleJournal);
        bind(KeyEvent.VK_H, "heal", this::heal);
        bind(KeyEvent.VK_Q, "nextPotion", this::switchPotion);
        bind(KeyEvent.VK_P, "pause", this::togglePause);
        bind(KeyEvent.VK_ESCAPE, "escape", this::escape);
        bind(KeyEvent.VK_R, "restart", () -> { if (dead) reset(); });
        bind(KeyEvent.VK_ENTER, "enter", () -> {
            if (sealIntroActive) finishSealIntro();
            else if (encounter) encounter = false;
            else if (won) advanceAfterWin();
        });
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            bind(KeyEvent.VK_1 + i, "slot" + i, () -> equipPotion(slot));
            bind(KeyEvent.VK_NUMPAD1 + i, "numpadSlot" + i, () -> equipPotion(slot));
        }
    }
    private void escape() {
        if (sealIntroActive) return;
        if (encounter) encounter = false;
        else if (lab) lab = false;
        else if (journal) journal = false;
        else togglePause();
    }
    private void togglePause() { openPauseMenu(); }
    public void openPauseMenu() {
        if (dead || won || sealIntroActive) return;
        paused = !paused;
        keys.clear(); hoverControl = 0;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }
    public void disposeGame() {
        timer.stop();
        keys.clear();

        AudioManager.stopHeartbeat();
        healthEffects.reset();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(sprintKeys);
    }
    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

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
        if (sealIntroActive || transition>0 || cinematic>0 || paused || dead || won || encounter) return;
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
        if (sealIntroActive || transition>0 || cinematic>0 || paused || dead || won || encounter) return;
        lab = false; journal = !journal; keys.clear();
    }
    private boolean canPlay() {
        return !sealIntroActive && transition==0 && cinematic==0 && !paused && !lab && !journal && !encounter && !dead && !won;
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
        if (!canPlay()) return;
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

    private static final int ACTOR_RADIUS = 11;
    private java.util.List<Rectangle> furniture() {
        java.util.List<Rectangle> r=new ArrayList<>();
        int rw=roomWidth(),rh=roomHeight();
        if(room()==2){
            r.add(new Rectangle(base()+65,205,180,42));
            r.add(new Rectangle(base()+285,205,180,42));
            r.add(new Rectangle(base()+505,175,490,42));
            r.add(new Rectangle(base()+90,425,430,42));
            r.add(new Rectangle(base()+850,425,300,42));
            r.add(new Rectangle(base()+1050,205,180,42));
            r.add(new Rectangle(base()+1270,205,180,42));
            r.add(new Rectangle(base()+1490,205,180,42));
            r.add(new Rectangle(base()+1300,425,380,42));
            return r;
        }
        if(room()==3){
            for(int y:new int[]{115,560}) {
                r.add(new Rectangle(base()+50,y,112,155));
                r.add(new Rectangle(base()+180,y,112,155));
                r.add(new Rectangle(base()+305,y+45,145,105));
                r.add(new Rectangle(base()+640,y+5,275,195));
                r.add(new Rectangle(base()+55,y+230,335,105));
                r.add(new Rectangle(base()+650,y+260,265,100));
            }
            r.add(new Rectangle(base()+55,1030,280,120));
            r.add(new Rectangle(base()+650,1030,265,130));
            return r;
        }
        if(room()==4){for(int x:new int[]{80,250,1090,1260}) for(int y:new int[]{290,950}) r.add(new Rectangle(base()+x,y,104,40)); for(int x:new int[]{85,1170}) r.add(new Rectangle(base()+x,740,180,42)); return r;}
        for(int y=135;y<rh-70;y+=330){
            for(int x=80;x<rw-160;x+=180){
                if(Math.abs(base()+x+52-doorX())<100 && Math.abs(y-doorY())<110)continue;
                if(Math.abs(base()+x+52-terminalX())<110 && Math.abs(y-terminalY())<110)continue;
                r.add(new Rectangle(base()+x,y+25,104,40));
            }
            int dy=y+175;
            if(dy<rh-100)for(int x:new int[]{85,335,rw-225}){
                if(room()==1 && y==135 && x==rw-225)continue;
                if(Math.abs(base()+x+70-doorX())<125&&Math.abs(dy-doorY())<100)continue;
                if(x!=335 && Math.abs(base()+x+70-terminalX())<80&&Math.abs(dy-terminalY())<65)continue;
                r.add(new Rectangle(base()+x,dy,150,42));
            }
        }
        return r;
    }
    private boolean canStand(double x,double y,int radius) {
        if(x<base()+35||x>base()+roomWidth()-35||y<110||y>roomHeight()-90)return false;
        for(Rectangle r:furniture()) {
            Rectangle solid = room()==3 ? r : room()==2 ? new Rectangle(r.x,r.y-(r.width==180?100:r.width==490?85:(r.width==300||r.width==380)?95:29),r.width,(r.width==180?142:r.width==490?127:(r.width==300||r.width==380)?137:73)) : r.width==104 ? new Rectangle(r.x,r.y-123,104,162) : new Rectangle(r.x,r.y-29,r.width,73);
            if(solid.intersects(x-radius,y-radius,radius*2,radius*2))return false;
        }

        for(int sy=100;room()!=2 && room()!=3 && room()!=4 && sy<roomHeight()-100;sy+=330){
            int my=sy+(sy==100?65:0);
            Rectangle monitor=new Rectangle(base()+roomWidth()-235,my-72,200,88);
            if(monitor.intersects(x-radius,y-radius,radius*2,radius*2))return false;
        }
        if(roomEntered && (!roomInvestigated || room()>=2)){
            Rectangle terminal=new Rectangle((int)terminalX()-65,(int)terminalY()-66,130,65);
            if(terminal.intersects(x-radius,y-radius,radius*2,radius*2))return false;
        }
        return true;
    }
    private double[] moveAroundEquipment(double x,double y,double dx,double dy,int radius) {
        if(!canStand(x,y,radius)){ double[] safe=findOpenFloor(x,y,radius); x=safe[0]; y=safe[1]; }
        if(canStand(x+dx,y,radius))x+=dx;
        if(canStand(x,y+dy,radius))y+=dy;
        return new double[]{x,y};
    }
    private double[] findOpenFloor(double x,double y,int radius) {
        x=clamp(x,base()+35,base()+roomWidth()-35); y=clamp(y,110,roomHeight()-90);
        if(canStand(x,y,radius))return new double[]{x,y};
        for(int d=8;d<500;d+=8)for(int a=0;a<16;a++){
            double nx=x+Math.cos(a*Math.PI/8)*d,ny=y+Math.sin(a*Math.PI/8)*d;
            if(canStand(nx,ny,radius))return new double[]{nx,ny};
        }
        return new double[]{base()+roomWidth()/2,roomHeight()/2};
    }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private int quota() { return wave == 1 ? 6 : wave == 2 ? 8 : wave == 3 ? 10 : 1; }
    private int activeLimit() { return wave == 1 ? 3 : wave == 2 ? 4 : wave == 3 ? 5 : 1; }
    private int nextEnemyType() {
        if (wave == 1) return 0;
        if (wave == 2) return spawned % 3 == 0 ? 0 : 1;
        if (wave == 3) return spawned % 3 == 0 ? 2 : spawned % 3 == 1 ? 1 : 0;
        return 3;
    }
    private void spawnEnemy() {
        int type = nextEnemyType();
        double roomStart = base() + roomWidth()/2 - 200;
        double ex = roomStart + 120 + rng.nextInt(320);
        if (Math.abs(ex - px) < 125) ex = roomStart + (ex < px ? 45 : 420);
        ex = Math.min(WORLD - 85, ex);
        double ey = clamp(py + (rng.nextBoolean()? 1:-1)*(140+rng.nextInt(100)), 120, roomHeight()-100);
        double[] spawn = findOpenFloor(ex, ey, ACTOR_RADIUS);
        Enemy z = new Enemy(type, spawn[0], spawn[1]);
        z.speedFactor = .85 + rng.nextDouble() * .30;
        enemies.add(z);
        spawned++; spawnTimer = type == 3 ? 165 : 105;
    }
    private void updateGame() {

        healthEffects.update(hp, 100);

        if (!canPlay() || sealIntroActive) {
            AudioManager.stopHeartbeat();
        } else {
            AudioManager.updateHeartbeat(hp, 100);
        }

        if (sealIntroActive) {
            sealIntroTicks++;
            repaint();
            return;
        }

        if (paused) { repaint(); return; }
        if(transition>0){ if(--transition==0) enterNextRoom(); repaint(); return; }
        if(cinematic>0){ if(--cinematic==0){ roomInvestigated=true; bossAuraTicks=180; } repaint(); return; }
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
        if (bossAuraTicks > 0 && !encounter) bossAuraTicks--;
        defeated.removeIf(d -> ++d.age >= SHATTER_TICKS);
        if (hurtTicks > 0) hurtTicks--;
        if (throwCooldown > 0) throwCooldown--;
        updatePlayer();
        if (roomInvestigated && spawned < quota() && enemies.size() < activeLimit() && --spawnTimer <= 0) spawnEnemy();
        updateEnemies();
        updateEnemyShots();
        updateProjectiles();
        if (roomInvestigated && !encounter && spawned >= quota() && enemies.isEmpty()) {
            if (++waveTimer >= 80 && wave < 4) {
                wave++; coins += wave == 2 ? 85 : wave == 3 ? 145 : 180; for (int c : RECIPES[wave-1]) stock[c] += 3; if (wave == 4) for (int c : RECIPES[2]) stock[c] += 5; spawned = 0; spawnTimer = 75; waveTimer = 0;
                roomEntered = false; roomInvestigated = false;
                message("Containment released. Find the next sealed door and press E.");
                try { saveProgress(); } catch (IOException e) { message("Checkpoint save failed: " + e.getMessage()); }
            }
        } else waveTimer = 0;
        if (hp <= 0) {
            hp = 0;
            dead = true;
            keys.clear();

            AudioManager.stopHeartbeat();
            healthEffects.reset();
        }
        repaint();
    }

    private void interactWithRoom() {
        if(!canPlay() || transition>0 || cinematic>0)return;
        if(!roomEntered){
            if(needsEntranceVerification()) {
                if(Math.hypot(px-securityX(),py-securityY())>110) { message("Access locked. Investigate the entrance security table [E]."); return; }
                if(verifyEquation(0)) { entranceVerified=true; message("Verification accepted. Wave 1 door unlocked. Press E at the door."); try{saveProgress();}catch(IOException ignored){} }
                return;
            }
            if(Math.hypot(px-doorX(),py-doorY())>110){message("Follow the gold marker to the door.");return;}
            if(wave==4&&!sealReady){
                if (!sealIntroSeen) { sealIntroActive=true; sealIntroTicks=0; keys.clear(); return; }
                lab=true;labTab=0;labTicks=3600;Arrays.fill(selected,false);keys.clear();
                labNotice="SEAL: purified solvent + mild bicarbonate base + ion-exchange mineral. Press MIX.";return;
            }
            transition=65;keys.clear();
        }else if(roomInvestigated && room()>=2 && Math.hypot(px-terminalX(),py-terminalY())<110){keys.clear();journalTab=0;journal=true;}else if(!roomInvestigated && Math.hypot(px-terminalX(),py-terminalY())<110){
            if(wave<=3 && !verifyEquation(wave)) return;
            roomInvestigated=true;spawnTimer=45;keys.clear();journalTab=0;journal=true;
            message(ROOM_CLUES[wave-1]);try{saveProgress();}catch(IOException ignored){}
        }
    }
    private void enterNextRoom(){
        openedDoors[wave-1]=true;roomEntered=true;
        px=base()+(wave==2?120:roomWidth()/2);py=wave==1?roomHeight()-130:wave==3?150:390;
        if(wave==4){cinematic=300;px=base()+720;py=900;roomInvestigated=true;spawnEnemy();Enemy boss=enemies.get(enemies.size()-1);boss.x=base()+720;boss.y=470;}
        double[] safeEntry=findOpenFloor(px,py,ACTOR_RADIUS);px=safeEntry[0];py=safeEntry[1];
        updateCamera();message("Follow the gold marker to the research terminal.");
        try{saveProgress();}catch(IOException ignored){}
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
            double speed = keys.contains(KeyEvent.VK_SHIFT) ? 3.50 : 2.50;
            double[] next = moveAroundEquipment(px, py, dx / length * speed, dy / length * speed, ACTOR_RADIUS);
            px = next[0]; py = next[1];
            GameplayBridge.recordMovement(oldX, oldY, px, py);
            if (dx < 0) facing = "left";
            else if (dx > 0) facing = "right";
            else if (dy < 0) facing = "up";
            else facing = "down";
        }
        updateCamera();
    }
    private void hurtPlayer(int damage) {
        if (hurtTicks > 0 || dead) return;
        hp = GameplayBridge.damagePlayer(hp, damage);
        hurtTicks = 42;
        message("Facility contamination! Dodge attacks and craft the matching cure.");
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
            double[] safe = findOpenFloor(z.x, z.y, ACTOR_RADIUS);
            z.x = safe[0]; z.y = safe[1];
            if (z.flash > 0) z.flash--;
            if (z.cooldown > 0) z.cooldown--;
            if (z.slowed > 0) z.slowed--;
            if (z.attackTicks > 0) z.attackTicks--;
            double dx = px - z.x, dy = py - z.y;
            double distance = Math.hypot(dx, dy);
            if (distance < 450 && !seen[z.type]) {
                seen[z.type] = true;
                GameplayBridge.scan(5, z.type, CHEMS[RECIPES[z.type][0]]);
                encounterType = z.type; encountered = z; encounter = true;
                if (z.type == 3) bossAuraTicks = 180;
                keys.clear();
                message("NEW RESEARCH: " + NAMES[z.type]);
                return;
            }
            if (z.type == 1 && z.charge > 0) {
                z.charge--;
                if (z.charge == 0) {
                    z.attackTicks = 24;
                    z.cooldown = 125;
                    if (distance < 88) hurtPlayer(15);
                    message("Crystal lunge! Stay clear of the warning ring.");
                }
                continue;
            }

            if ((z.type == 2 || z.type == 3) && distance < (z.type == 3 ? 270 : 255)
                    && z.cooldown == 0 && distance > 55) {
                fireEnemyShot(z, z.type == 3 ? 4.2 : 3.5);
                z.cooldown = z.type == 3 ? 105 : 125;
            }
            if (z.type == 1 && distance < 88 && z.cooldown == 0) {
                z.charge = 25;
                message("Biohazard Guardian is lunging - dodge the warning ring!");
                continue;
            }
            if (distance > (z.type == 1 ? 125 : z.type == 3 ? 68 : 43)) {
                double speed = ENEMY_BASE_SPEED[z.type] * z.speedFactor;
                if (z.slowed > 0) speed *= .55;
                double[] next = moveAroundEquipment(z.x, z.y,
                    dx / Math.max(distance, 1) * speed,
                    dy / Math.max(distance, 1) * speed * .65, ACTOR_RADIUS);
                z.x = next[0]; z.y = next[1];
                z.face = dx < 0 ? "left" : "right";
            } else if (distance <= 46 && z.cooldown == 0) {
                hurtPlayer(GameplayBridge.attackDamage(5, z.type));
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
                if (p.enemy.type == 3) {

                    if (p.enemy.bossPhaseOneHits < 5 && p.type == 2) {
                        p.enemy.bossPhaseOneHits++;
                        p.enemy.hp = 10 - p.enemy.bossPhaseOneHits;
                        message("GENESIS SHIELD " + p.enemy.bossPhaseOneHits + "/5 - Alpha formula effective!");
                    } else if (p.enemy.bossPhaseOneHits >= 5 && p.type == 3) {
                        p.enemy.bossPhaseTwoHits++;
                        p.enemy.hp = 5 - p.enemy.bossPhaseTwoHits;
                        message("ORIGIN CORE " + p.enemy.bossPhaseTwoHits + "/5 - Final formula effective!");
                    } else {
                        message(p.enemy.bossPhaseOneHits < 5
                                ? "SHIELD ACTIVE: throw 5 Alpha potions (5 ingredients each)!"
                                : "CORE EXPOSED: throw 5 Origin potions (6 ingredients each)!");
                    }
                } else {
                    p.enemy.hp = GameplayBridge.applyTreatment(5, p.enemy.type, p.enemy.hp, p.type);
                }
                if (p.enemy.hp == previousHealth) {
                    p.enemy.slowed = 80;
                    message("Incorrect treatment! Review the Research Journal.");
                } else {
                    p.enemy.flash = 28;
                    if (p.enemy.hp <= 0) stabilize(p.enemy);
                    else message("Treatment effective. " + NAMES[p.enemy.type] + " is weakening.");
                }
            }
        }
    }
    private void stabilize(Enemy z) {
        enemies.remove(z);
        BufferedImage pose = sprites.get(false, ENEMY_IDS[z.type],
                "walk", z.face, ticks / 9);
        if (pose == null) pose = fallbackShatterPose(z);

        defeated.add(new Defeated(z, sprites.red(pose), rng));
        stabilized[z.type]++;
        Reward earned = GameplayBridge.rewards(5, z.type);
        int[] progress = GameplayBridge.award(xp, coins, earned);
        xp = progress[0]; coins = progress[1] + (wave == 1 ? 18 : wave == 2 ? 28 : wave == 3 ? 40 : 12); for (int c : RECIPES[z.type]) stock[c] += 2;
        int reward = rng.nextInt(CHEMS.length - 2);
        stock[reward]++;
        if (rng.nextInt(6) == 0) medkits++;
        message(NAMES[z.type] + " stabilized! XP, coins and an ingredient earned.");
        if (z.type == 3) completeLevel();
    }
    private void completeLevel() {
        won = true; keys.clear();
        try {
            GameplayBridge.completeLevel(5);
            SaveManager.markCompleted(5);
            saveProgress();
            message("GENESIS CONTAINED - the last cure has been recovered.");
        } catch (IOException e) {
            message("Level completed, but saving failed: " + e.getMessage());
        }
    }
    private void advanceAfterWin() {
        if (winNotified) return;
        try {
            SaveManager.markCompleted(5);
            saveProgress();
        } catch (IOException e) {
            message("Cannot unlock Level 5: " + e.getMessage());
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
        px = 120; py = 240; camera = 0; hp = 100;

        healthEffects.reset();
        AudioManager.stopHeartbeat();

        ticks = 0; wave = 1; spawned = 0; spawnTimer = 90; waveTimer = 0;
        transition=0;cinematic=0;sealReady=false;sealIntroSeen=false;sealIntroActive=false;sealIntroTicks=0;cameraY=0; fundedWave = 0; roomEntered = false; roomInvestigated = false; Arrays.fill(openedDoors, false); entranceVerified=false;
        xp = 0; coins = 55; medkits = 2; equipped = 0; target = 0; batchQty = 1;
        hurtTicks = 0; throwCooldown = 0; labTicks = 0; labTab = 0;
        journalTab = 2; journalChoice = 0; encounterType = -1;
        facing = "right"; encountered = null; paused = false; lab = false; journal = false;
        encounter = false; dead = false; won = false; winNotified = false;
        Arrays.fill(vials, 0); Arrays.fill(stabilized, 0);
        Arrays.fill(seen, false); Arrays.fill(unlockedRecipes, false);
        Arrays.fill(selected, false);
        System.arraycopy(START_STOCK, 0, stock, 0, stock.length);
        enemies.clear(); projectiles.clear(); enemyShots.clear(); defeated.clear(); keys.clear(); bossAuraTicks = 0;
        message("Facility run restarted.");
        try { saveProgress(); } catch (IOException e) { message("Restart save failed: " + e.getMessage()); }
    }

    public void saveProgress() throws IOException {
        Properties p = SaveManager.load();
        p.setProperty("scientist", scientist);
        p.setProperty("level5.started", "true");
        p.setProperty("level5.won", Boolean.toString(won));
        p.setProperty("level5.px", Double.toString(px));
        p.setProperty("level5.py", Double.toString(py));
        p.setProperty("level5.hp", Double.toString(hp));
        p.setProperty("level5.ticks", Integer.toString(ticks));
        p.setProperty("level5.wave", Integer.toString(wave));
        p.setProperty("level5.mapVersion", "2");
        p.setProperty("level5.sealReady", Boolean.toString(sealReady));
        p.setProperty("level5.sealIntroSeen", Boolean.toString(sealIntroSeen));
        p.setProperty("level5.roomEntered", Boolean.toString(roomEntered));
        p.setProperty("level5.roomInvestigated", Boolean.toString(roomInvestigated));
        p.setProperty("level5.entranceVerified", Boolean.toString(entranceVerified));
        p.setProperty("level5.openedDoors", join(openedDoors));
        p.setProperty("level5.spawned", Integer.toString(spawned));
        p.setProperty("level5.spawnTimer", Integer.toString(spawnTimer));
        p.setProperty("level5.waveTimer", Integer.toString(waveTimer));
        p.setProperty("level5.xp", Integer.toString(xp));
        if (GameManager.getInstance().getPlayer() != null)
            p.setProperty("level5.researchPoints", Integer.toString(
                GameManager.getInstance().getPlayer().getResearchPoints()));
        p.setProperty("level5.fundedWave", Integer.toString(fundedWave));
        p.setProperty("level5.coins", Integer.toString(coins));
        p.setProperty("level5.medkits", Integer.toString(medkits));
        p.setProperty("level5.equipped", Integer.toString(equipped));
        p.setProperty("level5.target", Integer.toString(target));
        p.setProperty("level5.facing", facing);
        p.setProperty("level5.stock", join(stock));
        p.setProperty("level5.vials", join(vials));
        p.setProperty("level5.stabilized", join(stabilized));
        p.setProperty("level5.seen", join(seen));
        p.setProperty("level5.recipes", join(unlockedRecipes));
        StringJoiner zombies = new StringJoiner(";");
        for (Enemy z : enemies) {
            zombies.add(z.type + "," + z.x + "," + z.y + "," + z.hp + ","
                    + z.cooldown + "," + z.flash + "," + z.slowed + "," + z.face
                    + "," + z.speedFactor + "," + z.charge + "," + z.attackTicks
                    + "," + z.bossPhaseOneHits + "," + z.bossPhaseTwoHits);
        }
        p.setProperty("level5.enemies", zombies.toString());
        StringJoiner shots = new StringJoiner(";");
        for (Projectile shot : projectiles) {
            int idx = enemies.indexOf(shot.enemy);
            if (idx < 0) continue;
            shots.add(idx + "," + shot.type + "," + shot.sx + "," + shot.sy + ","
                    + shot.x + "," + shot.y + "," + shot.t);
        }
        p.setProperty("level5.projectiles", shots.toString());
        StringJoiner hostile = new StringJoiner(";");
        for (EnemyShot shot : enemyShots) {
            hostile.add(shot.type + "," + shot.x + "," + shot.y + "," + shot.vx + "," + shot.vy + "," + shot.ttl);
        }
        p.setProperty("level5.enemyShots", hostile.toString());
        GameManager.getInstance().saveProgress(() -> SaveManager.save(p));
    }
    public boolean loadProgress() {
        Properties p = SaveManager.load();
        if (!"true".equals(p.getProperty("level5.started"))
                || "true".equals(p.getProperty("level5.won"))
                || !scientist.equals(p.getProperty("scientist"))) return false;
        try {
            double newX = clamp(Double.parseDouble(p.getProperty("level5.px", "132")), 34, WORLD - 35);
            double newY = clamp(Double.parseDouble(p.getProperty("level5.py", "392")), WALK_MIN_Y, WALK_MAX_Y);
            double newHp = clamp(Double.parseDouble(p.getProperty("level5.hp", "100")), 1, 100);
            int newWave = clamp(Integer.parseInt(p.getProperty("level5.wave", "1")), 1, 4);
            int[] newStock = parseInts(p.getProperty("level5.stock"), START_STOCK);
            int[] newVials = parseInts(p.getProperty("level5.vials"), new int[4]);
            int[] newStabilized = parseInts(p.getProperty("level5.stabilized"), new int[4]);
            boolean[] newSeen = parseBools(p.getProperty("level5.seen"), new boolean[4]);
            boolean[] newRecipes = parseBools(p.getProperty("level5.recipes"), new boolean[4]);
            List<Enemy> loadedEnemies = new ArrayList<>();
            String enemyData = p.getProperty("level5.enemies", "");
            if (!enemyData.isEmpty()) for (String line : enemyData.split(";")) {
                String[] a = line.split(",", -1);

                if (a.length != 8 && a.length != 9 && a.length != 11 && a.length != 13)
                    throw new IllegalArgumentException("Bad enemy data");
                int type = Integer.parseInt(a[0]);
                if (type < 0 || type > 3) throw new IllegalArgumentException("Bad enemy type");
                Enemy z = new Enemy(type, Double.parseDouble(a[1]), Double.parseDouble(a[2]));
                z.hp = Math.min(z.hp, Integer.parseInt(a[3])); z.cooldown = Integer.parseInt(a[4]);
                z.flash = Integer.parseInt(a[5]); z.slowed = Integer.parseInt(a[6]);
                z.face = a[7];
                if (a.length >= 9) {
                    double factor = Double.parseDouble(a[8]);
                    if (!Double.isFinite(factor)) throw new IllegalArgumentException("Bad enemy speed");
                    z.speedFactor = clamp(factor, .80, 1.20);
                }
                if (a.length >= 11) {
                    z.charge = clamp(Integer.parseInt(a[9]), 0, 30);
                    z.attackTicks = clamp(Integer.parseInt(a[10]), 0, 24);
                }
                if (a.length == 13) {
                    z.bossPhaseOneHits = clamp(Integer.parseInt(a[11]), 0, 5);
                    z.bossPhaseTwoHits = clamp(Integer.parseInt(a[12]), 0, 5);
                }
                loadedEnemies.add(z);
            }
            List<Projectile> loadedShots = new ArrayList<>();
            String projectileData = p.getProperty("level5.projectiles", "");
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
            String hostileData = p.getProperty("level5.enemyShots", "");
            if (!hostileData.isEmpty()) for (String line : hostileData.split(";")) {
                String[] a = line.split(",", -1);
                if (a.length != 6) throw new IllegalArgumentException("Bad enemy projectile");
                int type = Integer.parseInt(a[0]);
                if (type < 0 || type > 3) throw new IllegalArgumentException("Bad shot type");
                loadedEnemyShots.add(new EnemyShot(type, Double.parseDouble(a[1]),
                    Double.parseDouble(a[2]), Double.parseDouble(a[3]),
                    Double.parseDouble(a[4]), clamp(Integer.parseInt(a[5]), 0, 150)));
            }
            wave=newWave;roomEntered=Boolean.parseBoolean(p.getProperty("level5.roomEntered","false"));
            sealReady=Boolean.parseBoolean(p.getProperty("level5.sealReady","false"));
            sealIntroSeen=Boolean.parseBoolean(p.getProperty("level5.sealIntroSeen","false"));
            sealIntroActive=false; sealIntroTicks=0;
            if(!"2".equals(p.getProperty("level5.mapVersion"))) return false;
            double[] safePlayer = room()==0 ? findOpenFloor(120, 240, ACTOR_RADIUS) : findOpenFloor(newX, newY, ACTOR_RADIUS);
            px = safePlayer[0]; py = safePlayer[1]; hp = newHp;
            wave = newWave; ticks = parseInt(p, "level5.ticks", 0);
            roomEntered = Boolean.parseBoolean(p.getProperty("level5.roomEntered", "false"));
            roomInvestigated = Boolean.parseBoolean(p.getProperty("level5.roomInvestigated", "false"));
            entranceVerified = Boolean.parseBoolean(p.getProperty("level5.entranceVerified", Boolean.toString(wave>1 || roomEntered)));
            boolean[] loadedDoors = parseBools(p.getProperty("level5.openedDoors"), new boolean[4]);
            System.arraycopy(loadedDoors, 0, openedDoors, 0, 4);
            spawned = clamp(parseInt(p, "level5.spawned", 0), 0, quota());
            spawnTimer = parseInt(p, "level5.spawnTimer", 90);
            waveTimer = parseInt(p, "level5.waveTimer", 0);
            xp = parseInt(p, "level5.xp", 0);
            coins = parseInt(p, "level5.coins", 55); fundedWave = parseInt(p, "level5.fundedWave", 0);
            if (!p.containsKey("level5.fundedWave")) coins = Math.max(coins, 55);
            medkits = parseInt(p, "level5.medkits", 2);
            equipped = clamp(parseInt(p, "level5.equipped", 0), 0, 3);
            target = clamp(parseInt(p, "level5.target", 0), 0, 3);
            facing = p.getProperty("level5.facing", "right");
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
            updateCamera();
            paused = false; lab = false; journal = false; encounter = false; encountered = null; dead = false; won = false;
            GameplayBridge.restoreScientistProgress(xp, coins,
                parseInt(p, "level5.researchPoints", 0), hp, px, py);
            keys.clear();
            message("Level 5 progress restored. Continue your research.");
            repaint();
            return true;
        } catch (RuntimeException e) {
            System.err.println("CHEMION Level 5: Invalid save: " + e.getMessage());
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

    private int detectedRecipe() {
        return GameplayBridge.detectRecipe(5, CHEMS, RECIPES, NAMES, selected);
    }
    private int maxBatch(int recipe) {
        return GameplayBridge.maxBatch(5, CHEMS, RECIPES, NAMES, stock, recipe);
    }
    private int maxBatch() {
        int recipe = detectedRecipe();
        return recipe < 0 ? 0 : maxBatch(recipe);
    }
    private void mix() {
        if(wave==4&&!roomEntered&&!sealReady){
            boolean correct=true;for(int i=0;i<selected.length;i++)if(selected[i]!=(i==0||i==2||i==3))correct=false;
            if(!correct){labNotice="Seal clue: purified solvent + mild bicarbonate base + ion-exchange mineral.";return;}
            for(int i:new int[]{0,2,3})if(stock[i]<1){labNotice="Buy missing seal ingredients in Shop.";return;}
            for(int i:new int[]{0,2,3})stock[i]--;sealReady=true;lab=false;Arrays.fill(selected,false);message("Seal dissolved. Press E at the door.");return;
        }
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
        if (!GameplayBridge.craft(5, CHEMS, RECIPES, NAMES, stock, vials,
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
        if (coins < 20 && fundedWave < wave) {
            fundedWave = wave;
            coins += wave >= 3 ? 100 : 45;
        }
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
        return !sealIntroActive && transition==0 && cinematic==0 && !paused && !lab && !journal && !encounter && !dead && !won;
    }
    private void handleClick(int x, int y) {
        if (sealIntroActive) { if (new Rectangle(370, 435, 220, 42).contains(x,y)) finishSealIntro(); return; }
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
            openPauseMenu(); return;
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
                if (Math.hypot(z.x - camera - x, z.y - cameraY - 34 - y) < 46
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
            else if (new Rectangle(640, 406, 40, 24).contains(x, y)) { int qty = askQuantity("Potion quantity", maxBatch(), batchQty, 0); if (qty > 0) batchQty = qty; }
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
        Graphics2D scene=(Graphics2D)g.create();
        if(cinematic>0 && cinematic<=65){double zoom=1.0+cinematic/65.0*.65;scene.translate(W/2.0,H/2.0);scene.scale(zoom,zoom);scene.translate(-W/2.0,-H/2.0);}
        drawNewRoom(scene);
        Graphics2D actors=(Graphics2D)scene.create();actors.translate(0,-cameraY);drawActors(actors);actors.dispose();scene.dispose();
        drawBossEntranceEdges(g);

        if (canPlay()) {
            healthEffects.drawVignette(g, W, H);
        }

        drawHUD(g);
        drawExplorationObjective(g);
        if (paused) drawPauseMenu(g);
        else if (encounter) drawEncounter(g);
        else if (lab) drawLab(g);
        else if (journal) drawJournal(g);
        else if (dead) drawEnd(g, "GAME OVER", "Your scientist was overwhelmed.", false);
        else if (won && defeated.isEmpty())
            drawEnd(g, "GENESIS CONTAINED", "Patient Zero has been stabilized. The truth survives.", true);
        if(transition>0) drawDoorTransition(g);
        if(cinematic>0) drawCinematic(g);
        if(sealIntroActive) drawSealIntro(g);
        g.dispose();
    }

    private static BufferedImage loadFacilityBackground() { return buildFacility(); }

    private static BufferedImage buildFacility() {
        BufferedImage image = new BufferedImage(WORLD,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); Random r = new Random(501);
        g.setPaint(new GradientPaint(0,0,new Color(9,6,13),0,340,new Color(43,28,35))); g.fillRect(0,0,WORLD,H);
        g.setColor(new Color(27,24,30)); g.fillRect(0,330,WORLD,H-330);
        for(int y=330;y<H;y+=29){g.setColor(new Color(51,43,48));g.drawLine(0,y,WORLD,y);for(int x=(y%2)*48;x<WORLD;x+=96)g.drawLine(x,y,x,y+29);}
        for(int i=0;i<700;i++){int x=r.nextInt(WORLD),y=335+r.nextInt(115);g.setColor(new Color(65,36,42,100));g.fillRect(x,y,2+r.nextInt(15),1+r.nextInt(3));}
        String[] signs={"GENESIS / RESTRICTED", "PRIVATE DEMONSTRATION", "QUARANTINE FAILURE", "HUMAN TRIALS", "ORIGIN / DO NOT DISCONNECT"};
        for(int sector=0;sector<5;sector++){
            int x=sector*632;
            g.setColor(new Color(13,10,17));g.fillRect(x+15,75,600,150);
            g.setColor(new Color(75,48,53));g.fillRect(x+15,75,600,5);
            g.setFont(new Font("Monospaced",Font.BOLD,15));g.setColor(new Color(195,114,105));g.drawString(signs[sector],x+55,110);
            for(int j=0;j<4;j++){int wx=x+42+j*145;g.setColor(new Color(38,28,37));g.fillRect(wx,126,120,94);g.setColor(new Color(70,52,61));g.drawRect(wx,126,120,94);g.setColor(new Color(14,12,19));g.fillRect(wx+8,134,104,78);g.setColor(new Color(107,76,82));g.drawLine(wx+18,135,wx+75,207);g.drawLine(wx+75,207,wx+110,169);g.drawLine(wx+75,207,wx+49,170);}
            if(sector==1){g.setColor(new Color(67,39,47));g.fillRect(x+110,128,400,76);g.setFont(new Font("Monospaced",Font.BOLD,21));g.setColor(new Color(184,139,127));g.drawString("GENESIS: A NEW ERA",x+155,173);g.setColor(new Color(109,40,45));g.drawLine(x+112,129,x+505,202);}
            if(sector==4){g.setColor(new Color(83,22,35));g.fillOval(x+210,113,210,105);g.setColor(new Color(190,58,68));g.drawOval(x+210,113,210,105);g.setColor(new Color(18,10,17));g.fillOval(x+291,130,38,38);g.fillRoundRect(x+280,162,60,50,20,20);}
            for(Rectangle b:java.util.Collections.<Rectangle>emptyList()){if(b.x<x||b.x>=x+632)continue; if(sector==2||sector==4){tank(g,b.x+7,b.y+4,b.width-14,b.height-8,new Color(67,48,59),new Color(160,38,64));}else{researchDesk(g,b.x+3,b.y+10,b.width-6,b.height-22,sector%2);}}
            g.setColor(new Color(83,46,52));g.fillRect(x+8,0,8,330);g.fillRect(x+618,0,8,330);
            for(int j=0;j<3;j++){int lx=x+90+j*190;g.setColor(new Color(100,22,34,45));g.fillOval(lx-55,42,120,100);g.setColor(new Color(185,48,55));g.fillRect(lx,48,32,5);}
        }
        g.dispose();return image;
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

    private static void researchDesk(Graphics2D g, int x, int y, int w, int h, int variant) {

        g.setColor(new Color(4, 10, 18, 100));
        g.fillOval(x + 3, y + h - 4, w + 7, 16);
        g.setColor(new Color(48, 67, 80));
        g.fillRect(x + 10, y + 26, 8, h - 16);
        g.fillRect(x + w - 18, y + 26, 8, h - 16);
        g.setColor(new Color(102, 124, 137));
        g.fillRect(x + 10, y + h + 5, 19, 4);
        g.fillRect(x + w - 29, y + h + 5, 19, 4);

        g.setColor(new Color(14, 26, 39));
        g.fillRect(x - 3, y + 23, w + 6, 13);
        g.setColor(new Color(84, 111, 126));
        g.fillRect(x, y + 21, w, 8);
        g.setColor(new Color(148, 169, 166));
        g.fillRect(x + 3, y + 22, w - 6, 2);
        g.setColor(new Color(33, 47, 63));
        g.fillRect(x + 5, y + 29, w - 10, 4);

        int mx = x + (variant == 0 ? 10 : 14);
        g.setColor(new Color(10, 16, 29));
        g.fillRect(mx - 2, y - 10, 53, 35);
        g.setColor(new Color(95, 119, 138));
        g.fillRect(mx, y - 8, 49, 30);
        g.setColor(new Color(9, 39, 50));
        g.fillRect(mx + 4, y - 4, 41, 22);
        g.setColor(variant == 0 ? new Color(85, 214, 188) : new Color(121, 183, 242));
        g.fillRect(mx + 8, y + 1, 22, 2);
        g.fillRect(mx + 8, y + 7, 33, 2);
        g.fillRect(mx + 8, y + 13, 16, 2);
        g.setColor(new Color(99, 119, 134));
        g.fillRect(mx + 22, y + 22, 6, 4);

        g.setColor(new Color(16, 26, 39));
        g.fillRect(mx + 6, y + 27, 42, 5);
        g.setColor(new Color(132, 165, 169));
        for (int k = 0; k < 7; k++) g.fillRect(mx + 9 + k * 5, y + 28, 3, 1);

        int px = x + w - 37;
        g.setColor(new Color(196, 206, 193));
        g.fillRect(px, y + 2, 24, 17);
        g.setColor(new Color(229, 224, 196));
        g.fillRect(px + 3, y, 23, 17);
        g.setColor(new Color(87, 111, 125));
        g.fillRect(px + 6, y + 5, 16, 1);
        g.fillRect(px + 6, y + 9, 13, 1);
        g.fillRect(px + 6, y + 13, 17, 1);
        g.setColor(new Color(207, 142, 92));
        g.fillRect(px - 2, y + 17, 22, 2);

        g.setColor(variant == 0 ? new Color(89, 220, 158) : new Color(240, 184, 95));
        g.fillRect(x + w - 6, y + 24, 3, 2);
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

    private void drawBossEntranceEdges(Graphics2D g) {
        if (bossAuraTicks <= 0) return;
        float fade = Math.min(1f, bossAuraTicks / 45f);
        double pulse = .5 + .5 * Math.sin(ticks * .37);
        int alpha = Math.min(205, (int) ((65 + 130 * pulse) * fade));
        for (int i = 0; i < 125; i += 5) {
            int a = alpha * (125 - i) / 125;
            g.setColor(new Color(244, 23, 48, a));
            g.fillRect(i, 0, 5, H);
            g.fillRect(W - i - 5, 0, 5, H);
            g.fillRect(0, i, W, 5);
            g.fillRect(0, H - i - 5, W, 5);
        }
    }

    private void drawGenesisDoors(Graphics2D g) {
        for (int d = 0; d < DOOR_X.length; d++) {
            int x = DOOR_X[d] - (int) camera;
            if (x < -110 || x > W + 110) continue;
            boolean open = openedDoors[d];
            g.setColor(new Color(4, 9, 21, 210)); g.fillRect(x - 38, 104, 76, 126);
            g.setColor(new Color(107, 137, 155)); g.fillRect(x - 42, 100, 8, 136);
            g.fillRect(x + 34, 100, 8, 136);
            g.setColor(open ? new Color(76, 217, 185) : new Color(227, 88, 114));
            g.fillRect(x - 27, 118, 54, 5);
            g.setColor(new Color(28, 44, 61));
            if (!open) {
                g.fillRect(x - 30, 127, 29, 103); g.fillRect(x + 2, 127, 28, 103);
                g.setColor(new Color(96, 117, 137)); g.fillRect(x - 2, 127, 4, 103);
            } else {
                g.fillRect(x - 30, 127, 7, 103); g.fillRect(x + 23, 127, 7, 103);
            }
            g.setColor(open ? new Color(110, 240, 193) : new Color(247, 169, 112));
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.drawString("DOOR 0" + (d + 1), x - 25, 92);
            if (d == wave - 1 && !open && Math.abs(px - DOOR_X[d]) < 170)
                g.drawString("[E] OPEN", x - 29, 251);
        }

        for (int d = 0; d < DOOR_X.length; d++) {
            int tx = DOOR_X[d] + 245 - (int) camera;
            if (tx < -170 || tx > W + 170) continue;
            g.setColor(new Color(11, 20, 35, 180)); g.fillRoundRect(tx - 84, 185, 168, 63, 7, 7);
            g.setColor(new Color(81, 115, 131)); g.fillRect(tx - 81, 184, 162, 8);
            g.setColor(new Color(41, 62, 78)); g.fillRect(tx - 70, 192, 140, 43);
            g.setColor(new Color(125, 227, 210)); g.fillRect(tx - 44, 196, 88, 28);
            g.setColor(new Color(13, 41, 54)); g.fillRect(tx - 41, 199, 82, 22);
            for (int i = 0; i < 4; i++) {
                int vx = tx - 70 + i * 44;
                Color vial = VIAL_COLORS[i];
                g.setColor(new Color(vial.getRed(), vial.getGreen(), vial.getBlue(), 35 + (ticks / 15 + i) % 3 * 15));
                g.fillOval(vx - 10, 212, 31, 30);
                g.setColor(vial); g.fillRoundRect(vx, 219, 11, 17, 4, 4);
                g.setColor(new Color(238, 241, 224)); g.fillRect(vx + 2, 216, 7, 4);
            }
            g.setColor(new Color(215, 216, 204)); g.fillRect(tx + 40, 225, 27, 13);
            g.setColor(new Color(115, 142, 164)); g.drawLine(tx + 44, 229, tx + 60, 229);
            if (d == wave - 1 && roomEntered && !roomInvestigated && Math.abs(px - (DOOR_X[d] + 245)) < 165) {
                g.setColor(new Color(120, 250, 214));
                g.setFont(new Font("Monospaced", Font.BOLD, 12));
                g.drawString("[E] INVESTIGATE", tx - 66, 174);
            }
        }
    }
    private void drawExplorationObjective(Graphics2D g){
        if(won||dead)return;
        String objective=needsEntranceVerification()?"VERIFY SECURITY TABLE [E]":!roomEntered?(wave==4&&!sealReady?"DISSOLVE CHEMICAL SEAL [E]":"OPEN NEXT DOOR [E]"):!roomInvestigated?"INVESTIGATE TERMINAL [E]":"STABILIZE INFECTED / "+(spawned-enemies.size())+" OF "+quota();
        g.setColor(new Color(9,6,16,26));g.fillRoundRect(230,91,500,52,9,9);g.setColor(new Color(240,190,95,45));g.drawRoundRect(230,91,500,52,9,9);
        centered(g,objective,230,94,500,25,16,GOLD,true);centered(g,ROOM_NAMES[wave-1]+" | J: READ CLUES",230,119,500,20,12,WHITE,true);
        if(!roomEntered||!roomInvestigated){double tx=needsEntranceVerification()?securityX():roomEntered?terminalX():doorX(),ty=needsEntranceVerification()?securityY():roomEntered?terminalY():doorY();int x=clamp((int)(tx-camera),65,W-65),y=clamp((int)(ty-cameraY)-40,180,420);String dir=Math.hypot(px-tx,py-ty)<110?"PRESS E":Math.abs(tx-px)>Math.abs(ty-py)?(tx>px?"GO RIGHT >":"< GO LEFT"):(ty>py?"GO DOWN / S":"GO UP / W");g.setColor(GOLD);g.fillPolygon(new int[]{x-12,x+12,x},new int[]{y,y,y+15},3);centered(g,dir,x-75,y-22,150,20,13,GOLD,true);}
    }
    private void drawNewRoom(Graphics2D g){
        Graphics2D a=(Graphics2D)g.create();a.translate(-camera,-cameraY);int b=base(),rw=roomWidth(),rh=roomHeight();
        a.setPaint(new GradientPaint(b,0,room()==2?new Color(16,34,40):room()==3?new Color(38,28,48):new Color(25,18,32),b+rw,rh,room()==2?new Color(38,53,55):room()==3?new Color(55,42,61):new Color(45,25,39)));a.fillRect(b,0,rw,rh);
        for(int y=0;y<rh;y+=48)for(int x=b;x<b+rw;x+=96){
            a.setColor(new Color(56,43,57));a.drawRect(x,y,96,48);
            a.setColor(new Color(32,25,39));a.drawLine(x+1,y+1,x+95,y+1);
            if((x/96+y/48)%4==0){a.setColor(new Color(65,39,49));a.fillRect(x+12,y+30,15,2);}
        }

        a.setColor(new Color(11,8,17));a.fillRect(b,0,35,rh);a.fillRect(b+rw-35,0,35,rh);
        a.setColor(new Color(65,44,63));a.fillRect(b+23,0,12,rh);a.fillRect(b+rw-35,0,12,rh);
        a.setColor(new Color(17,12,24));a.fillRect(b+35,0,rw-70,95);
        a.setColor(new Color(86,52,69));a.fillRect(b+35,89,rw-70,6);
        for(int x=b+55;x<b+rw-100;x+=145){
            a.setColor(new Color(48,34,48));a.fillRect(x,12,112,65);
            a.setColor(new Color(11,17,26));a.fillRect(x+6,18,100,52);
            a.setColor(new Color(90,66,83));a.drawLine(x+15,20,x+66,65);a.drawLine(x+66,65,x+92,33);
        }
        for(int y=130;y<rh-90;y+=190){for(int x:new int[]{b+25,b+rw-33}){
            a.setColor(new Color(160,36,60,45));a.fillOval(x-35,y-30,80,70);
            a.setColor(new Color(210,57,76));a.fillRect(x,y,8,18);
        }}

        Random debris=new Random(831+room());
        for(int i=0;i<rh*rw/10000;i++){
            int x=b+45+debris.nextInt(rw-90),y=110+debris.nextInt(rh-130);
            a.setColor(new Color(87,16,33,155));a.fillOval(x,y,12+debris.nextInt(38),4+debris.nextInt(13));
            a.setColor(new Color(126,28,42,130));a.fillRect(x+9,y+8,3,3);
            a.setColor(new Color(112,159,169,150));a.drawLine(x+17,y-9,x+23,y-15);a.drawLine(x+23,y-15,x+28,y-8);a.drawLine(x+28,y-8,x+17,y-9);
            a.setColor(new Color(186,202,199,130));a.fillRect(x+30,y-5,4,2);
        }
        for(int sy=100;room()!=2 && room()!=3 && room()!=4 && sy<rh-100;sy+=330){
            int y=sy+(sy==100?65:0);
            int x=b+rw-235;
            a.setColor(new Color(13,12,23));a.fillRect(x,y-72,200,88);
            a.setColor(new Color(80,66,90));a.drawRect(x,y-72,200,88);
            a.setColor(new Color(12,42,49));a.fillRect(x+5,y-67,190,78);
            a.setColor(new Color(75,218,184));a.drawString("GENESIS // SUBJECT MONITOR",x+8,y-50);
            for(int k=0;k<7;k++){int xx=x+12+k*25;int hh=10+(int)(15*(1+Math.sin(ticks*.06+k)));a.fillRect(xx,y+4-hh,13,hh);}
            a.setColor(ticks/30%2==0?RED:new Color(100,35,50));a.fillRect(x+175,y-64,12,5);
        }
        if(room()==2){
            a.setColor(new Color(160,130,55));for(int x=b+40;x<b+rw-45;x+=36){a.fillRect(x,98,18,5);a.fillRect(x,rh-65,18,5);}
            for(int x=b+110;x<b+rw-100;x+=250){a.setColor(new Color(45,75,80));a.fillRect(x,18,150,55);a.setColor(TEAL);a.drawString("CONTAINMENT / LOCKDOWN",x+5,42);a.setColor(RED);a.drawString("EVACUATION DENIED",x+5,60);}
        }else if(room()==3){
            for(int y=160;y<rh-80;y+=180){a.setColor(new Color(83,57,105));a.fillRect(b+290,y,380,3);a.setColor(new Color(150,110,190));a.drawString("SUBJECT TRIAL / "+(y/180+1),b+300,y-8);}
        }else if(room()==4){
            drawOriginArena(a,b,rw,rh,ticks);
        }
        int index=0;
        Color[] fluids={new Color(50,215,150),new Color(72,174,240),new Color(170,75,235),new Color(240,155,50),new Color(210,55,90)};
        for(Rectangle r:furniture()){
            int n=index++;
            if(r.width==104){
                int x=r.x+7,y=r.y-123,w=90,h=145;Color c=fluids[n%fluids.length];boolean broken=n%4==2;
                a.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),28));a.fillOval(x-24,y+50,110,85);
                tank(a,x,y,w,h,new Color(72,65,85),c);
                a.setColor(new Color(9,13,24,205));
                if(n%2==0){a.fillOval(x+24,y+31,15,16);a.fillRoundRect(x+22,y+46,19,28,7,7);a.fillRect(x+20,y+69,7,15);a.fillRect(x+35,y+69,7,15);}
                else{a.fillOval(x+18,y+49,31,22);a.fillOval(x+34,y+39,15,17);a.drawLine(x+20,y+60,x+12,y+48);a.fillRect(x+23,y+68,5,12);a.fillRect(x+39,y+68,5,12);}
                a.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),160));
                for(int k=0;k<4;k++){int yy=y+78-(ticks/3+k*13)%48;a.drawOval(x+17+(k*9)%30,yy,3,4);}
                if(broken){a.setColor(new Color(195,205,219));a.drawPolyline(new int[]{x+44,x+31,x+40,x+25},new int[]{y+28,y+44,y+57,y+78},4);a.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),110));a.fillOval(x+12,r.y+23,70,17);a.fillRect(x+48,y+65,3,45);}
                a.setColor((ticks/25+n)%2==0?c:new Color(60,35,55));a.fillRect(x+45,y+12,6,4);
            }else if(room()==2){
                drawContainmentFurniture(a,r,n);
            }else if(room()==3){
                drawEvolutionFurniture(a,r,n);
            }else{
                researchDesk(a,r.x+4,r.y-13,r.width-8,48,n%2);
                if(n%2==0){a.setColor(new Color(13,22,35));a.fillRect(r.x+61,r.y-29,43,30);a.setColor(TEAL);a.drawRect(r.x+65,r.y-25,35,21);a.drawLine(r.x+69,r.y-18,r.x+95,r.y-18);}
                for(int k=0;k<4;k++){int vx=r.x+66+k*14,vy=r.y-5;Color c=fluids[(n+k)%5];a.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),40));a.fillOval(vx-5,vy-6,19,25);a.setColor(c);a.fillRoundRect(vx,vy,8,13,3,3);a.setColor(WHITE);a.fillRect(vx+2,vy-3,4,3);}
            }
        }
        if(!roomEntered || room()==2){int x=(int)doorX(),y=(int)doorY();int left=x-58,top=y-100,width=116,height=155;
            if(room()<=2){left=b+rw-116;width=116;top=y-85;height=170;
                if(room()==0 || room()==2){top=y-110;height=160;}
            }
            a.setColor(new Color(10,8,16));a.fillRect(left-5,top-5,width+10,height+10);
            a.setColor(new Color(106,73,100));a.fillRect(left,top,width/2-2,height);a.fillRect(left+width/2+2,top,width/2-2,height);
            a.setColor(new Color(52,33,62));a.drawRect(left+5,top+9,width/2-12,height-18);a.drawRect(left+width/2+7,top+9,width/2-12,height-18);
            a.setColor(RED);a.fillRect(left+10,top+5,width-20,5);
            if(wave==4 && !roomEntered && !sealReady) drawChemicalSeal(a,left,top,width,height);
        }
        if(roomEntered && (!roomInvestigated || room()>=2)){int x=(int)terminalX(),y=(int)terminalY();researchDesk(a,x-65,y-55,130,45,0);a.setColor(TEAL);a.drawRect(x-55,y-66,49,30);if(room()>=2){a.setColor(GOLD);a.drawString("CLASSIFIED FILE [E]",x-70,y-75);}}
        a.dispose();

    }
    // Origin vault
    private void drawOriginArena(Graphics2D a,int b,int rw,int rh,int time){
        int cx=b+rw/2, cy=510;
        a.setColor(new Color(18,15,29)); a.fillRect(b+365,105,710,190);
        a.setColor(new Color(91,62,87)); a.fillRect(b+365,105,710,12);
        for(int i=0;i<3;i++){
            int x=b+390+i*225;
            a.setColor(new Color(65,49,73)); a.fillRect(x,128,210,120);
            a.setColor(new Color(9,22,31)); a.fillRect(x+6,134,198,108);
            a.setColor(i==1?RED:TEAL); a.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
            a.drawString(i==0?"GENESIS / DNA":i==1?"CONTAINMENT FAILED":"VITAL SIGNAL",x+12,151);
            for(int k=0;k<170;k+=4){int yy=(int)(Math.sin((k+time)*.09)*22); if(i==0){a.fillRect(x+15+k,191+yy,4,3);a.fillRect(x+15+k,191-yy,4,3);if(k%12==0)a.drawLine(x+15+k,191+yy,x+15+k,191-yy);}else{int next=(int)(Math.sin((k+4+time)*.09)*22);a.drawLine(x+15+k,191+yy,x+19+k,191+next);}}
        }
        a.setColor(new Color(42,35,52));a.fillOval(cx-330,cy-235,660,470);
        a.setColor(new Color(85,65,88));a.fillOval(cx-310,cy-215,620,430);
        a.setColor(new Color(31,23,39));a.fillOval(cx-290,cy-195,580,390);
        for(int i=0;i<24;i++){double angle=i*Math.PI/12;int x=cx+(int)(310*Math.cos(angle)),y=cy+(int)(215*Math.sin(angle));a.setColor(i%2==0?new Color(180,65,83):new Color(90,48,67));a.fillRect(x-8,y-4,16,8);}
        a.setColor(new Color(126,32,66));a.setStroke(new BasicStroke(3));a.drawOval(cx-220,cy-150,440,300);a.setStroke(new BasicStroke(1));
        for(int side:new int[]{-1,1}){int x=cx+side*440;a.setColor(new Color(12,10,20));a.setStroke(new BasicStroke(14));a.drawPolyline(new int[]{x,x,cx+side*300},new int[]{300,cy,cy},3);a.setColor(new Color(147,48,81));a.setStroke(new BasicStroke(4));a.drawPolyline(new int[]{x,x,cx+side*300},new int[]{300,cy,cy},3);a.setStroke(new BasicStroke(1));}
        // Ruptured chamber floor and chemical leaks
        a.setColor(new Color(80,29,56));a.fillOval(cx-100,cy-65,200,130);
        a.setColor(new Color(205,76,115));a.drawOval(cx-100,cy-65,200,130);
        Random r=new Random(505);for(int i=0;i<55;i++){int x=cx-155+r.nextInt(310),y=cy-100+r.nextInt(200);a.setColor(i%3==0?new Color(132,230,183,145):new Color(165,85,115,140));a.fillRect(x,y,3+r.nextInt(8),2);}
        a.setColor(new Color(185,78,106));a.setFont(new Font(Font.MONOSPACED,Font.BOLD,16));a.drawString("ORIGIN // SUBJECT 000",cx-110,cy+180);
        for(int i=0;i<8;i++){int x=b+380+i*95;a.setColor(new Color(130,35,62,40+(int)(20*(1+Math.sin(time*.06+i)))));a.fillOval(x-40,95,100,90);a.setColor(RED);a.fillRect(x,100,22,5);}
        for(int i=0;i<14;i++){int x=b+60+(i*97+time/3)%1320,y=330+(i*73)%650;a.setColor(new Color(125,100,150,15));a.fillOval(x,y,100,18);}
    }

    // Evolution wing furniture
    private void drawEvolutionFurniture(Graphics2D g, Rectangle r,int n){
        Graphics2D a=(Graphics2D)g.create();
        int x=r.x,y=r.y,w=r.width,h=r.height;
        a.setFont(new Font(Font.MONOSPACED,Font.PLAIN,10));
        if(w==112){
            Color c=n%2==0?new Color(104,224,194):new Color(183,110,250);
            int glow=25+(int)(15*(1+Math.sin(ticks*.06+n)));
            a.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),glow));a.fillRoundRect(x-8,y-5,w+16,h+10,30,30);
            a.setColor(new Color(48,45,65));a.fillRoundRect(x,y,w,h,20,20);
            a.setColor(new Color(15,29,43));a.fillRoundRect(x+10,y+12,w-20,h-36,16,16);
            a.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),110));a.fillRoundRect(x+13,y+25,w-26,h-51,12,12);
            a.setColor(new Color(20,23,39));a.fillOval(x+45,y+37,22,24);a.fillRoundRect(x+40,y+62,32,38,12,12);a.fillRect(x+43,y+95,10,25);a.fillRect(x+59,y+95,10,25);a.setStroke(new BasicStroke(7));a.drawLine(x+42,y+68,x+30,y+92);a.drawLine(x+70,y+68,x+82,y+92);
            a.setStroke(new BasicStroke(1));a.setColor(c);for(int k=0;k<8;k++){int by=y+119-(ticks/2+k*17)%89;a.drawOval(x+17+(k*23)%74,by,4,5);}
            a.drawRoundRect(x+10,y+12,w-20,h-36,16,16);a.setColor(new Color(90,99,119));a.fillRect(x+5,y+h-25,w-10,17);a.setColor(c);a.fillRect(x+15,y+h-19,9,5);a.drawString("SUBJECT 0"+(n+1),x+30,y+h-14);
        }else if(w==145){
            a.setColor(new Color(74,88,105));a.setStroke(new BasicStroke(5));a.drawPolyline(new int[]{x-13,x+12,x+12,x+32},new int[]{y+35,y+35,y+9,y+9},4);a.setStroke(new BasicStroke(1));
            researchDesk(a,x,y+32,w,48,0);a.setColor(new Color(20,34,45));a.fillRect(x+12,y+8,57,35);a.setColor(TEAL);a.drawRect(x+15,y+11,51,28);a.drawString("TRIAL",x+21,y+28);
            for(int k=0;k<4;k++){int bx=x+78+k*14;a.setColor(VIAL_COLORS[k]);a.fillRoundRect(bx,y+31,10,19,3,3);a.setColor(WHITE);a.fillRect(bx+2,y+27,6,4);}
            a.setColor(new Color(51,49,68));a.fillRect(x+8,y+78,w-16,23);a.setColor(PURPLE);a.drawString("SAMPLE CONTROL",x+18,y+94);
        }else if(w==335||w==280){
            a.setColor(new Color(67,67,88));a.fillRect(x,y,w,h);a.setColor(new Color(23,28,43));a.fillRect(x+7,y+7,w-14,h-18);
            for(int row=0;row<2;row++){int sy=y+43+row*43;a.setColor(new Color(105,109,132));a.fillRect(x+7,sy,w-14,4);for(int k=0;k<(w-20)/25;k++){int bx=x+13+k*25;a.setColor(VIAL_COLORS[(k+row)%4]);a.fillRoundRect(bx,sy-25,13,24,3,3);a.setColor(WHITE);a.fillRect(bx+3,sy-29,7,4);a.fillRect(bx+2,sy-13,9,5);}}
        }else{
            a.setColor(new Color(10,16,28));a.fillRect(x,y,w,h);a.setColor(new Color(96,90,130));a.drawRect(x,y,w,h);a.setColor(new Color(15,34,48));a.fillRect(x+5,y+5,w-10,h-10);a.setColor(TEAL);
            a.drawString(h==195?"GENESIS / GENETIC SEQUENCING":"LIVE / PATIENT VITALS",x+12,y+17);
            if(h==195){int cx=x+62,top=y+30;for(int i=3;i<145;i+=3){int d=(int)(32*Math.sin(i*.09+ticks*.05)),prev=(int)(32*Math.sin((i-3)*.09+ticks*.05));a.setColor(TEAL);a.drawLine(cx+prev,top+i-3,cx+d,top+i);a.setColor(PURPLE);a.drawLine(cx-prev,top+i-3,cx-d,top+i);if(i%9==0)a.drawLine(cx-d,top+i,cx+d,top+i);}a.setColor(TEAL);a.drawString("ALPHA / STRAIN 03",x+115,y+42);for(int k=0;k<8;k++){a.setColor(k%3==0?PURPLE:TEAL);a.fillRect(x+115,y+54+k*14,45+(k*19)%91,2);}a.drawString("MUTATION ACTIVE",x+115,y+180);}
            else{int last=y+58;for(int i=1;i< w-24;i++){int phase=(i+ticks*2)%65;int v=y+58+(phase==24?-24:phase==25?25:phase==26?-12:(int)(3*Math.sin(phase*.3)));a.drawLine(x+11+i-1,last,x+11+i,v);last=v;}a.drawString("SIGNAL / UNSTABLE",x+12,y+h-9);}
            a.setColor(new Color(100,235,210,45));int scan=y+24+ticks%Math.max(1,h-35);a.drawLine(x+7,scan,x+w-7,scan);
        }
        a.dispose();
    }
    // Containment vault furniture
    private void drawContainmentFurniture(Graphics2D g, Rectangle r, int n){
        int x=r.x,y=r.y,w=r.width;
        if(r.width==380){
            drawMutationMonitor(g,x,y-95,w,137);
        }else if(r.width==180){
            boolean storage = n==0 || n==5 || n==7;
            int t=y-100; g.setColor(new Color(16,24,30));g.fillRect(x+5,t+8,w,142);
            g.setColor(new Color(72,92,99));g.fillRect(x,t,w,142);g.setColor(new Color(112,137,140));g.fillRect(x,t,w,5);
            if(storage){
                g.setColor(new Color(22,35,40));g.fillRect(x+7,t+9,w-14,112);
                for(int row=0;row<3;row++){
                    int sy=t+42+row*38;g.setColor(new Color(120,139,142));g.fillRect(x+7,sy,w-14,4);
                    for(int k=0;k<6;k++){int bx=x+14+k*26;g.setColor(new Color(55+25*k,155-12*k,145+12*row));g.fillRect(bx,sy-23,14,23);g.setColor(new Color(190,200,190));g.fillRect(bx+3,sy-27,8,4);g.fillRect(bx+2,sy-13,10,5);}
                }
            }else{
                for(int k=0;k<3;k++){int bx=x+5+k*58;g.setColor(new Color(49,66,74));g.fillRect(bx,t+7,54,114);g.setColor(new Color(112,135,140));g.drawRect(bx,t+7,54,114);for(int j=0;j<4;j++)g.drawLine(bx+10,t+17+j*5,bx+43,t+17+j*5);g.fillRect(bx+39,t+60,5,17);g.setColor(GOLD);g.fillRect(bx+35,t+80,9,8);}
            }
            g.setColor(GOLD);g.drawString(storage?"CHEMICAL STORAGE":"SECURITY VAULT",x+9,y+36);
        }else if(n==3){
            researchDesk(g,x,y-13,w,48,0);
            for(int k=0;k<12;k++){int px=x+12+k*33,py=y-23+(k%3)*8;g.setColor(new Color(225,218,185));g.fillRect(px,py,25,18);g.setColor(new Color(100,105,105));for(int j=0;j<3;j++)g.drawLine(px+3,py+4+j*4,px+20,py+4+j*4);}
        }else{
            int t=y-(n==2?85:95),h=n==2?127:137;
            g.setColor(new Color(9,16,24));g.fillRect(x,t,w,h);g.setColor(new Color(85,116,125));g.drawRect(x,t,w,h);g.setColor(new Color(10,37,43));g.fillRect(x+6,t+6,w-12,h-12);g.setColor(TEAL);
            g.drawString(n==2?"GENESIS / SPECIMEN ANALYSIS":"CLASSIFIED / CONTAINMENT LOG",x+14,t+20);
            if(n==2){
                int cx=x+70,cy=t+49;g.drawOval(cx-10,cy-12,20,22);g.drawRoundRect(cx-15,cy+10,30,35,8,8);g.drawLine(cx-15,cy+14,cx-30,cy+36);g.drawLine(cx+15,cy+14,cx+30,cy+36);g.drawLine(cx-7,cy+45,cx-12,cy+62);g.drawLine(cx+7,cy+45,cx+12,cy+62);
                for(int k=0;k<6;k++){int xx=x+145+k*24,yy=t+65+(k%2)*18;g.drawOval(xx,yy,13,13);if(k>0)g.drawLine(xx-11,yy+(k%2==0?18:-18)+6,xx,yy+6);}
                g.drawString("ION EXCHANGE / OXIDANT RESPONSE",x+145,t+42);g.drawString("SPECIMEN 02 / GUARDIAN",x+145,t+108);
                g.setColor(new Color(75,218,184,100));int scan=t+30+(ticks%75);g.drawLine(x+15,scan,x+w-15,scan);
            }else{
                g.setColor(RED);g.drawString("EVACUATION REQUEST: DENIED",x+14,t+42);g.setColor(TEAL);g.drawString("DIRECTOR AUTHORIZATION: SEALED",x+14,t+59);g.drawString("STRAIN SEQUENCE / FILE 02",x+14,t+76);
                for(int k=0;k<22;k++){int xx=x+15+k*12,dy=(int)(12*Math.sin(k*.55+ticks*.025));g.drawLine(xx,t+105+dy,xx,t+105-dy);g.fillOval(xx-2,t+103+dy,4,4);g.fillOval(xx-2,t+103-dy,4,4);}
            }
        }
    }
    // Mutation monitor
    private void drawMutationMonitor(Graphics2D g,int x,int y,int w,int h){
        Graphics2D a=(Graphics2D)g.create();
        a.setColor(new Color(9,16,24));a.fillRect(x,y,w,h);
        a.setColor(new Color(85,116,125));a.drawRect(x,y,w,h);
        a.setColor(new Color(10,37,43));a.fillRect(x+6,y+6,w-12,h-12);
        a.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
        a.setColor(TEAL);a.drawString("GENESIS / MUTATION ANALYSIS",x+14,y+20);
        a.drawString("STRAIN ACTIVITY",x+18,y+37);a.drawString("DNA SEQUENCE",x+246,y+37);
        int gx=x+20,gy=y+48,gw=198,gh=65;
        a.setColor(new Color(35,75,80));
        for(int i=0;i<=4;i++)a.drawLine(gx,gy+i*gh/4,gx+gw,gy+i*gh/4);
        for(int i=0;i<=6;i++)a.drawLine(gx+i*gw/6,gy,gx+i*gw/6,gy+gh);
        a.setColor(TEAL);a.setStroke(new BasicStroke(2));
        int last=gy+gh/2;
        for(int i=0;i<=gw;i+=2){double t=(i+ticks*1.4)*.065;int v=gy+gh/2+(int)(18*Math.sin(t)+8*Math.sin(t*2.7));if(i>0)a.drawLine(gx+i-2,last,gx+i,v);last=v;}
        a.fillOval(gx+gw-3,last-3,6,6);
        int cx=x+300,top=y+48;int p1=0,p2=0;
        for(int i=0;i<=66;i+=3){int d=(int)(28*Math.sin(i*.115+ticks*.055));int u=cx+d,v=cx-d;if(i>0){a.setColor(TEAL);a.drawLine(p1,top+i-3,u,top+i);a.setColor(new Color(178,137,235));a.drawLine(p2,top+i-3,v,top+i);}if(i%9==0){a.setColor(new Color(104,164,177));a.drawLine(u,top+i,v,top+i);}p1=u;p2=v;}
        a.setStroke(new BasicStroke(1));a.setColor(new Color(100,235,210,65));int scan=y+43+ticks%74;a.drawLine(x+10,scan,x+w-10,scan);
        a.setColor(TEAL);a.drawString("LIVE / SPECIMEN 02",x+20,y+128);a.dispose();
    }
    // Chemical seal cinematic
    private void finishSealIntro() {
        if (sealIntroTicks < 60) return;
        sealIntroActive=false; sealIntroSeen=true; keys.clear();
        message("Chemical seal identified. Press E at the door to mix the dissolver.");
        try { saveProgress(); } catch (IOException ignored) {}
    }

    private void drawSealIntro(Graphics2D g) {
        int alpha=Math.min(255, sealIntroTicks*5);
        g.setColor(new Color(0,0,0,alpha)); g.fillRect(0,0,W,H);
        if(sealIntroTicks<52) return;
        centered(g,"GENESIS / THE SEALED VAULT",0,65,W,40,22,PURPLE,true);
        String[] lines={"Something behind this door is still alive.",
            "The Genesis seal was never designed to keep people out...",
            "It was designed to keep something inside."};
        int chars=(sealIntroTicks-52)/2;
        for(int i=0;i<lines.length;i++) {
            int n=Math.max(0,Math.min(chars,lines[i].length()));
            centered(g,lines[i].substring(0,n),0,135+i*32,W,30,17,WHITE,false);
            chars-=lines[i].length();
        }
        if(sealIntroTicks>=60) {
            centered(g,"CHEMICAL CLUE",0,260,W,30,19,TEAL,true);
            centered(g,"A purified solvent, a mild bicarbonate base,",0,300,W,28,16,WHITE,false);
            centered(g,"and an ion-exchange mineral can dissolve the seal.",0,330,W,28,16,WHITE,false);
            g.setColor(new Color(45,26,65));g.fillRoundRect(370,435,220,42,8,8);
            g.setColor(PURPLE);g.drawRoundRect(370,435,220,42,8,8);
            centered(g,"CONTINUE [ENTER]",370,435,220,42,14,WHITE,true);
        }
    }

    private void drawChemicalSeal(Graphics2D g,int x,int y,int w,int h) {
        Graphics2D a=(Graphics2D)g.create();
        int pulse=(int)(45+25*Math.sin(ticks*.07));
        a.setColor(new Color(75,255,120,pulse));a.fillRoundRect(x-12,y-12,w+24,h+24,18,18);
        a.setColor(new Color(38,160,76,115));a.fillRect(x+4,y+8,w-8,h-12);
        a.setStroke(new BasicStroke(3));
        for(int i=0;i<5;i++) {
            int xx=x+12+i*(w-24)/4;
            a.setColor(new Color(80,255,130,150+pulse));
            a.drawPolyline(new int[]{xx,xx+7,xx-5,xx+5},new int[]{y+10,y+h/3,y+2*h/3,y+h-8},4);
        }
        for(int i=0;i<20;i++) {
            int xx=x+(i*37)%w, yy=y+h-(ticks/2+i*17)%(h+35);
            a.setColor(new Color(110,255,150,55+(i%4)*30));a.fillOval(xx,yy,3+i%4,3+i%4);
            if(i<8){a.setColor(new Color(70,220,100,30));a.fillOval(xx-12,yy-8,28,18);}
        }
        a.setColor(new Color(130,255,160));a.drawRect(x+3,y+3,w-6,h-6);
        a.setFont(new Font("Monospaced",Font.BOLD,11));a.drawString("CHEMICAL SEAL",x+8,y-16);
        a.dispose();
    }

    private void drawDoorTransition(Graphics2D g){int half=(int)(W/2.0*transition/65);g.setColor(new Color(18,12,26));g.fillRect(0,0,half,H);g.fillRect(W-half,0,half,H);g.setColor(RED);g.drawLine(half,0,half,H);g.drawLine(W-half,0,W-half,H);}
    private void drawCinematic(Graphics2D g){
        if(cinematic>100){g.setColor(new Color(10,5,17));g.fillRect(0,0,W,H);int pan=(300-cinematic)*4;for(int i=0;i<12;i++){int x=i*180-pan;g.setColor(new Color(72,23,61));g.fillRoundRect(x,80,125,330,25,25);g.setColor(new Color(154,35,79));g.drawRoundRect(x,80,125,330,25,25);g.fillOval(x+40,155,45,145);}centered(g,"GENESIS / RESTRICTED SPECIMENS",0,440,W,40,18,RED,true);}
        else if(cinematic>65){g.setColor(Color.BLACK);g.fillRect(0,0,W,H);centered(g,"SIGNAL LOST",0,240,W,50,24,RED,true);}
        else{g.setColor(new Color(60,0,20,100));g.fillRect(0,0,W,H);centered(g,"PATIENT ZERO - THE ORIGIN",0,220,W,65,30,RED,true);}
    }

    private void drawFacilityEffects(Graphics2D g) {
        for(int i=0;i<5;i++){int x=i*632+315-(int)camera; if(x < -350 || x > W+350) continue;
            int a=22+(int)(12*(1+Math.sin(ticks*.035+i))); g.setColor(new Color(165,24,42,a));g.fillOval(x-260,100,520,270);
            g.setColor(new Color(160,145,155,18));g.fillOval(x-180+(ticks%140),365,250,20);
        }

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

        for (EnemyShot shot : enemyShots) {
            int x = (int) (shot.x - camera), y = (int) shot.y;
            Color glow = shot.type == 3 ? new Color(198, 91, 214) : new Color(95, 221, 133);
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 47));
            g.fillOval(x - 13, y - 13, 26, 26);
            g.setColor(glow); g.fillOval(x - 5, y - 5, 10, 10);
            g.setColor(new Color(242, 240, 234)); g.fillRect(x - 1, y - 2, 3, 3);
        }
    }
    // Scientist rendering
    private void drawPlayer(Graphics2D g) {
        int x = (int) (px - camera), y = (int) py;
        g.setColor(new Color(6, 7, 13, 130));
        g.fillOval(x - 19, y - 8, 38, 12);
        boolean moving = keys.contains(KeyEvent.VK_W) || keys.contains(KeyEvent.VK_A)
                || keys.contains(KeyEvent.VK_S) || keys.contains(KeyEvent.VK_D)
                || keys.contains(KeyEvent.VK_UP) || keys.contains(KeyEvent.VK_DOWN)
                || keys.contains(KeyEvent.VK_LEFT) || keys.contains(KeyEvent.VK_RIGHT);
        boolean running = moving && keys.contains(KeyEvent.VK_SHIFT);

        String moveAction = running ? "run" : "walk";

        String action = throwCooldown > 14 && !dead ? "throw potion" : moveAction;
        int frame = moving ? ticks / (running ? 5 : 10) : 0;
        BufferedImage sprite = sprites.get(true, scientist, action, facing, frame);
        boolean red = hurtTicks > 0 && hurtTicks % 8 >= 4;
        if (sprite != null) {
            drawSprite(g, red ? sprites.red(sprite) : sprite, x, y, 65, 1f, 59);
        } else {

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
    // Enemy rendering
    private void drawEnemy(Graphics2D g, Enemy z) {
        int x = (int) (z.x - camera), y = (int) z.y;
        if (x < -120 || x > W + 120) return;
        boolean boss = z.type == 3;
        g.setColor(new Color(5, 8, 15, 150));
        g.fillOval(x - (boss ? 35 : 20), y - 7, boss ? 70 : 40, 13);

        if (boss && bossAuraTicks > 0) {
            float fade = Math.min(1f, bossAuraTicks / 45f);
            int pulse = (int) (4 * Math.sin(ticks * 0.45));
            g.setColor(new Color(226, 53, 80, (int) (31 * fade)));
            g.fillOval(x - 61 - pulse, y - 123 - pulse, 122 + pulse * 2, 132 + pulse * 2);
            g.setColor(new Color(238, 75, 94, (int) (85 * fade)));
            g.drawOval(x - 49 - pulse, y - 112 - pulse, 98 + pulse * 2, 114 + pulse * 2);
        }

        if (z.type == 1 && z.charge > 0) {
            int r = 67 + (25 - z.charge) / 2;
            g.setColor(new Color(255, 152, 64, 37));
            g.fillOval(x - r, y - r / 3, 2 * r, r * 2 / 3);
            g.setColor(new Color(250, 172, 76, 150));
            g.drawOval(x - r, y - r / 3, 2 * r, r * 2 / 3);
        }
        boolean attacking = z.attackTicks > 0 && z.flash <= 0;
        String action = attacking ? "attack" : "walk";
        int frame = attacking ? Math.min(3, (24 - z.attackTicks) / 6)
                : ticks / (z.type == 2 ? 8 : z.type == 3 ? 13 : 11);
        BufferedImage sprite = sprites.get(false, ENEMY_IDS[z.type], action, z.face, frame);
        boolean red = z.flash > 0 && z.flash % 8 >= 4;
        if (sprite != null) {
            drawSprite(g, red ? sprites.red(sprite) : sprite, x, y, boss ? 205 : z.type == 0 ? 75 : 68, 1f);
        } else if (boss) {
            Graphics2D large = (Graphics2D) g.create();
            large.translate(x, y);
            large.scale(1.42, 1.42);
            drawFallbackEnemy(large, z, 0, 0, red);
            large.dispose();
        } else drawFallbackEnemy(g, z, x, y, red);

        if (boss) {
            int barW = 135;
            g.setColor(new Color(24, 16, 34, 220)); g.fillRect(x - barW / 2, y - 172, barW, 7);
            g.setColor(new Color(212, 81, 99));
            g.fillRect(x - barW / 2 + 1, y - 171, (int) ((barW - 2) * clamp(z.hp / 10.0, 0, 1)), 5);
            g.setColor(new Color(197, 155, 202)); g.drawRect(x - barW / 2, y - 172, barW, 7);
        }
    }
    private BufferedImage fallbackShatterPose(Enemy z) {

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

        if (z.type == 0) {
            g.setColor(new Color(24, 33, 49)); g.fillRect(x - 13, y - 22, 11, 22); g.fillRect(x + 3, y - 22, 11, 22);
            g.setColor(new Color(37, 52, 68)); g.fillRect(x - 20, y - 57, 40, 36);
            g.setColor(new Color(68, 82, 103)); g.fillRect(x - 27, y - 53, 9, 27); g.fillRect(x + 18, y - 53, 9, 27);
            g.setColor(new Color(106, 143, 143)); g.fillRect(x - 12, y - 77, 24, 22);
            g.setColor(new Color(28, 39, 60)); g.fillRect(x - 17, y - 83, 34, 9); g.fillRect(x - 19, y - 76, 38, 4);
            g.setColor(new Color(232, 66, 83)); g.fillRect(x - 8, y - 66, 5, 4); g.fillRect(x + 4, y - 66, 5, 4);
            g.setColor(new Color(171, 154, 100)); g.fillRect(x - 5, y - 48, 10, 6);
        } else if (z.type == 1) {
            g.setColor(new Color(55, 44, 88)); g.fillRect(x - 27, y - 33, 53, 19);
            g.setColor(new Color(75, 58, 111)); g.fillRect(x - 38, y - 24, 12, 22); g.fillRect(x - 20, y - 23, 10, 23);
            g.fillRect(x + 13, y - 23, 10, 23); g.fillRect(x + 30, y - 24, 11, 22);
            g.setColor(new Color(100, 71, 156)); g.fillRect(x + 18, y - 41, 23, 19);
            g.setColor(new Color(200, 106, 248));
            g.fillPolygon(new int[]{x-28,x-21,x-12},new int[]{y-33,y-60,y-33},3);
            g.fillPolygon(new int[]{x-7,x+1,x+10},new int[]{y-33,y-66,y-33},3);
            g.fillPolygon(new int[]{x+14,x+22,x+30},new int[]{y-33,y-54,y-33},3);
            g.setColor(new Color(245, 130, 255)); g.fillRect(x + 32, y - 34, 5, 4);
        } else if (z.type == 2) {
            g.setColor(new Color(143, 158, 158)); g.fillRect(x - 12, y - 22, 10, 22); g.fillRect(x + 4, y - 22, 10, 22);
            g.setColor(new Color(178, 193, 185)); g.fillRect(x - 19, y - 58, 38, 38);
            g.setColor(new Color(143, 158, 158)); g.fillRect(x - 28, y - 55, 10, 31); g.fillRect(x + 18, y - 55, 10, 31);
            g.setColor(new Color(189, 209, 202)); g.fillRoundRect(x - 17, y - 82, 34, 31, 8, 8);
            g.setColor(new Color(29, 53, 61)); g.fillRect(x - 12, y - 75, 24, 17);
            g.setColor(new Color(76, 222, 142)); g.fillRect(x - 9, y - 71, 18, 5);
            g.fillRect(x - 5, y - 39, 10, 14); g.fillRect(x + 21, y - 25, 5, 9);
        } else {
            g.setColor(new Color(24, 23, 39)); g.fillRect(x - 20, y - 27, 15, 27); g.fillRect(x + 7, y - 27, 15, 27);
            g.setColor(new Color(34, 29, 52)); g.fillRect(x - 28, y - 77, 56, 53);
            g.setColor(new Color(51, 41, 69)); g.fillRect(x - 35, y - 72, 10, 49); g.fillRect(x + 25, y - 72, 10, 49);
            g.setColor(new Color(145, 169, 149)); g.fillRect(x - 16, y - 106, 32, 30);
            g.setColor(new Color(22, 21, 38)); g.fillRect(x - 19, y - 110, 38, 12);
            g.setColor(new Color(118, 239, 165)); g.fillRect(x - 10, y - 92, 7, 4); g.fillRect(x + 5, y - 92, 7, 4);
            g.setColor(new Color(52, 210, 125)); g.fillRect(x - 32, y - 60, 5, 31); g.fillRect(x + 27, y - 60, 5, 31);
            g.setColor(new Color(166, 250, 178)); g.fillRect(x - 6, y - 62, 12, 17);
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
        label(g, "RESEARCH WING", 413, 33, 10, MUTED, true);
        label(g, "WAVE " + wave + " / 4", 426, 55, 16, PURPLE, true);
        hudPanel(g, 721, 14, 225, 75);
        label(g, "XP  " + xp, 735, 38, 13, new Color(186, 222, 166), true);
        label(g, "COINS  " + coins, 827, 38, 13, new Color(243, 210, 148), true);
        int total = 0; for (int n : stabilized) total += n;
        label(g, "STABILIZED  " + total, 735, 61, 11, WHITE, true);
        label(g, "MEDKITS  " + medkits, 846, 61, 10, WHITE, true);

        if (hudControlsAvailable()) {
            drawHudControl(g, HUD_PAUSE, true, hoverControl == 1);
            drawHudControl(g, HUD_EXIT, false, hoverControl == 2);
        }

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

        Color glass = available ? new Color(225, 218, 244) : new Color(110, 104, 128);
        Color fill = available ? liquid : new Color(80, 75, 94);
        g.setColor(new Color(99, 75, 66)); g.fillRect(x + 7, y - 2, 11, 5);
        g.setColor(glass); g.drawRect(x + 8, y + 3, 9, 5);
        g.drawRoundRect(x + 2, y + 9, 22, 22, 5, 5);
        g.setColor(fill); g.fillRoundRect(x + 5, y + 17, 16, 11, 3, 3);
        g.setColor(available ? new Color(250, 246, 255) : new Color(150, 144, 166));
        g.fillRect(x + 7, y + 12, 3, 7);
        g.setColor(new Color(25, 22, 42)); g.drawLine(x + 7, y + 28, x + 19, y + 28);
    }
    // Encounter popup
    private void drawEncounter(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics(new Font("SansSerif", Font.PLAIN, 11));
        Popup p = popupLayout(fm);
        int x = p.x, y = p.y, i = encounterType;
        if (encountered != null) {
            int ex = (int) (encountered.x - camera), ey = (int) (encountered.y-cameraY) - 38;
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
    // Chemistry lab
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
    // Potion crafting
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

        label(g, "QTY", 566, 422, 11, new Color(245, 207, 155), true);
        button(g, 604, 406, 35, 24, "-", new Color(65, 48, 97));
        button(g, 640, 406, 40, 24, String.valueOf(batchQty), new Color(88, 57, 127));
        button(g, 681, 406, 35, 24, "+", new Color(65, 48, 97));
        button(g, 727, 406, 66, 24, "MAX", new Color(88, 57, 127));
        label(g, "Max: " + maxBatch(), 810, 422, 10, MUTED, false);
        g.setColor(new Color(100, 81, 129)); g.drawLine(210, 420, 543, 420);
        wrap(g, labNotice, 210, 445, 414, 11, 16, new Color(245, 207, 155));
        button(g, 646, 443, 214, 36, "MIX POTION", new Color(114, 71, 167));
        label(g, "TIP: open Recipes for chemistry clues", 210, 486, 10, MUTED, false);
    }
    // Recipes
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
    // Element properties
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
    // Shop
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
    // Research files
    private void drawLabResearch(Graphics2D g) {
        label(g, "OBSERVATION LOG - LEVEL 5", 210, 128, 14, PURPLE, true);
        int y = 163;
        for (int i = 0; i < 4; i++) {
            if (seen[i]) {
                label(g, NAMES[i].toUpperCase(Locale.ROOT), 210, y, 12, PURPLE, true);
                y = wrap(g, CLUE[i], 210, y + 18, 665, 11, 16, WHITE) + 17;
            }
        }
        if (y == 163) label(g, "No research collected yet.", 210, 166, 12, MUTED, false);
    }
    // Research journal
    private void drawJournal(Graphics2D g) {
        g.setColor(new Color(8, 6, 17, 200)); g.fillRect(0, 0, W, H);
        panel(g, 34, 24, 892, 492, 250);
        label(g, "RESEARCH JOURNAL", 54, 62, 19, PURPLE, true);
        label(g, "LEVEL 5 / GENESIS LABORATORY", 611, 59, 10, MUTED, true);
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
            label(g, "RECOVERED GENESIS ARCHIVE", 62, 166, 15, GOLD, true);
            for (int i = 0; i < 4; i++) {
                int y = 193 + i * 75;
                label(g, ROOM_NAMES[i], 62, y, 13, PURPLE, true);
                wrap(g, i < wave-1 || (i == wave-1 && roomInvestigated) ? ROOM_CLUES[i] : "LOCKED - investigate the research terminal.", 62, y+18, 820, 12, 15, WHITE);
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
            wrap(g, "Research unlocks when you first encounter this infected specimen.",
                    345, 225, 523, 12, 19, WHITE);
            return;
        }
        label(g, NAMES[i].toUpperCase(Locale.ROOT), 345, 191, 16, PURPLE, true);
        label(g, "TYPE: " + (i == 3 ? "Experimental Boss" : "Facility Mutation"), 345, 220, 11, WHITE, true);
        label(g, "DIFFICULTY: " + (i == 0 ? "Moderate" : i == 1 ? "Challenging" : i == 2 ? "Hard" : "Boss"), 345, 235, 11, WHITE, true);
        label(g, "STATUS: " + (stabilized[i] > 0 ? "Completed" : "In Progress"), 345, 250, 11, WHITE, true);
        label(g, "BACKGROUND", 345, 268, 11, PURPLE, true);
        int bio = wrap(g, DESC[i], 345, 284, 521, 11, 14, WHITE);
        label(g, "OBSERVED CONDITION", 345, bio + 5, 11, GOLD, true);
        int next = wrap(g, CONDITION[i], 345, bio + 20, 521, 11, 14, WHITE);
        label(g, "RESEARCH NOTES", 345, next + 10, 11, PURPLE, true);
        int end = wrap(g, CLUE[i], 345, next + 28, 521, 11, 16, WHITE);
        if (unlockedRecipes[i])
            wrap(g, "UNLOCKED: " + FORMULA[i], 345, end + 5, 521, 10, 13, new Color(186, 231, 165));
    }
    // Pause menu
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

    // Popup layout
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
        int ey = encountered == null ? 385 : (int) (encountered.y-cameraY);
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

    private static final class SpriteLoader {
        private final Map<String, BufferedImage[]> cache = new HashMap<>();

        private final Map<BufferedImage, BufferedImage> redCache = new IdentityHashMap<>();
        private final Set<String> missing = new HashSet<>();
        private final Set<String> missingRunNotices = new HashSet<>();

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
            String key = (player ? "characters/" : "enemies/level5/") + name;
            BufferedImage[] frames = load(key, name, action, direction);
            if (frames == null && player && action.equals("run")
                    && missingRunNotices.add(name + ":" + direction)) {

                System.err.println("CHEMION Level 5: RUN sprite not found for " + name
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
            if (!folder.isDirectory() && key.startsWith("enemies/level5/")) {
                File enemyRoot = new File(assets, "enemies/level5");
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

            if (files.isEmpty() && key.startsWith("enemies/level5/")) {
                for (String rootName : new String[]{"enemies/level5", "zombies/level5", "level5"}) {
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
                System.err.println("Level 5 sprite: " + best + ": " + e.getMessage());
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

            boolean hasTransparency = false;
            for (int x = 0; x < w && !hasTransparency; x += Math.max(1, w / 60)) {
                for (int y = 0; y < h; y += Math.max(1, h / 30)) {
                    if ((sheet.getRGB(x, y) >>> 24) < 24) {
                        hasTransparency = true; break;
                    }
                }
            }

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

                int[] cuts = new int[poses.size() + 1];
                cuts[0] = 0; cuts[poses.size()] = w;
                for (int i = 1; i < poses.size(); i++)
                    cuts[i] = (poses.get(i - 1)[1] + poses.get(i)[0] + 1) / 2;
                return splitAt(sheet, cuts);
            }

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
