import battlefield.level1.SaveManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.IntConsumer;
import javax.swing.*;

/**
 * Five-level horizontal carousel for CHEMION.
 * The selected card grows; click arrows, use arrow keys, wheel, or swipe/drag.
 * No extra art files or libraries are required.
 */
public class ContinueGame extends JPanel {
    private static final int DESIGN_W = 1100;
    private static final int DESIGN_H = 620;
    private static final String[] NAMES = {
        "ABANDONED TOWN", "CONTAMINATED HOSPITAL", "TOXIC CHEMICAL FACTORY",
        "LOST RESEARCH FACILITY", "GENESIS LABORATORY"
    };
    private static final Color[] ACCENTS = {
        new Color(191, 135, 239), new Color(98, 209, 201),
        new Color(239, 170, 88), new Color(146, 141, 247),
        new Color(223, 113, 170)
    };
    private static final Color BG = new Color(13, 12, 28);
    private static final Color WHITE = new Color(240, 232, 255);
    private static final Color MUTED = new Color(168, 157, 190);
    private static final Rectangle LEFT_ARROW = new Rectangle(35, 275, 70, 90);
    private static final Rectangle RIGHT_ARROW = new Rectangle(995, 275, 70, 90);
    private static final Rectangle ACTION = new Rectangle(406, 522, 288, 49);
    private static final Rectangle BACK = new Rectangle(42, 543, 154, 42);

    private final IntConsumer onLevelSelected;
    private final Runnable onBack;
    private final Timer animation;
    private int selectedIndex = 0;
    private double displayedIndex = 0;
    private boolean dragging;
    private boolean suppressClick;
    private int pressX;
    private int pressIndex;

    public ContinueGame(IntConsumer onLevelSelected, Runnable onBack) {
        this.onLevelSelected = onLevelSelected;
        this.onBack = onBack;
        setOpaque(true);
        setBackground(BG);
        setFocusable(true);

        animation = new Timer(16, e -> {
            if (dragging) return;
            displayedIndex += (selectedIndex - displayedIndex) * .22;
            if (Math.abs(displayedIndex - selectedIndex) < .004) {
                displayedIndex = selectedIndex;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                pressX = scaledX(e.getX());
                pressIndex = selectedIndex;
                dragging = false;
                suppressClick = false;
            }
            @Override public void mouseDragged(MouseEvent e) {
                int dx = scaledX(e.getX()) - pressX;
                if (Math.abs(dx) > 12) dragging = true;
                if (dragging) {
                    ((Timer) e.getSource()).stop();
                    displayedIndex = clamp(pressIndex - dx / 275.0, 0, 4);
                    repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (dragging) {
                    int dx = scaledX(e.getX()) - pressX;
                    int steps = Math.max(1, (int) Math.round(Math.abs(dx) / 275.0));
                    select(clamp(pressIndex + (dx < 0 ? steps : -steps), 0, 4));
                    suppressClick = true;
                    dragging = false;
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (suppressClick) { suppressClick = false; return; }
                int x = scaledX(e.getX()), y = scaledY(e.getY());
                if (LEFT_ARROW.contains(x, y)) { select(selectedIndex - 1); return; }
                if (RIGHT_ARROW.contains(x, y)) { select(selectedIndex + 1); return; }
                if (BACK.contains(x, y)) { onBack.run(); return; }
                if (ACTION.contains(x, y)) { activateSelected(); return; }
                // Clicking a neighboring card moves it to the center.
                for (int i = 0; i < 5; i++) {
                    if (cardBounds(i).contains(x, y)) {
                        select(i);
                        return;
                    }
                }
            }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                select(selectedIndex + (e.getWheelRotation() > 0 ? 1 : -1));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        bind(KeyEvent.VK_LEFT, "previous", () -> select(selectedIndex - 1));
        bind(KeyEvent.VK_RIGHT, "next", () -> select(selectedIndex + 1));
        bind(KeyEvent.VK_ENTER, "open", this::activateSelected);
        bind(KeyEvent.VK_ESCAPE, "back", onBack);
    }

    private void bind(int key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private int scaledX(int x) { return (int) Math.round(x * DESIGN_W / (double) Math.max(1, getWidth())); }
    private int scaledY(int y) { return (int) Math.round(y * DESIGN_H / (double) Math.max(1, getHeight())); }
    private static int clamp(int n, int lo, int hi) { return Math.max(lo, Math.min(hi, n)); }
    private static double clamp(double n, double lo, double hi) { return Math.max(lo, Math.min(hi, n)); }

    private void select(int index) {
        selectedIndex = clamp(index, 0, 4);
        if (!animation.isRunning()) animation.start();
        repaint();
    }

    private void activateSelected() {
        int level = selectedIndex + 1;
        if (level <= SaveManager.highestUnlockedLevel()) onLevelSelected.accept(level);
    }

    private Rectangle cardBounds(int i) {
        double difference = Math.abs(i - displayedIndex);
        double scale = .88 + .28 * Math.max(0, 1 - difference);
        int w = (int) Math.round(245 * scale);
        int h = (int) Math.round(288 * scale);
        int cx = (int) Math.round(550 + (i - displayedIndex) * 275);
        return new Rectangle(cx - w / 2, 324 - h / 2, w, h);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.scale(getWidth() / (double) DESIGN_W, getHeight() / (double) DESIGN_H);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(23, 17, 45), 0, 620, BG));
        g.fillRect(0, 0, 1100, 620);
        g.setColor(new Color(116, 83, 163, 28));
        for (int x = 0; x < 1100; x += 33) g.drawLine(x, 0, x, 620);
        for (int y = 0; y < 620; y += 33) g.drawLine(0, y, 1100, y);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(550, 300), 520,
            new float[]{0f, 1f}, new Color[]{new Color(111, 60, 155, 45), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, 1100, 620);

        centered(g, "SELECT LEVEL", 550, 65, 29, new Color(222, 179, 255), true);
        centered(g, "SWIPE OR USE  <   >  TO EXPLORE THE FIVE LOCATIONS", 550, 97, 13, MUTED, false);

        // Draw distant cards first, so the focused card is always on top.
        Integer[] order = {0, 1, 2, 3, 4};
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> Math.abs(i - displayedIndex)).reversed());
        for (int i : order) drawCard(g, i);
        drawArrow(g, true, selectedIndex > 0);
        drawArrow(g, false, selectedIndex < 4);
        drawBottom(g);
        g.dispose();
    }

    private void drawCard(Graphics2D g, int i) {
        Rectangle r = cardBounds(i);
        if (r.x > 1100 || r.x + r.width < 0) return;
        boolean locked = i + 1 > SaveManager.highestUnlockedLevel();
        boolean focused = Math.abs(i - displayedIndex) < .5;
        Color accent = locked ? new Color(100, 95, 119) : ACCENTS[i];
        int x = r.x, y = r.y, w = r.width, h = r.height;

        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(x + 8, y + 10, w, h, 19, 19);
        if (focused) {
            g.setColor(new Color(175, 101, 242, 40));
            g.setStroke(new BasicStroke(9));
            g.drawRoundRect(x - 3, y - 3, w + 6, h + 6, 20, 20);
        }
        g.setPaint(new GradientPaint(x, y, new Color(45, 33, 69), x, y + h, new Color(23, 20, 39)));
        g.fillRoundRect(x, y, w, h, 16, 16);
        g.setStroke(new BasicStroke(focused ? 3f : 2f));
        g.setColor(accent);
        g.drawRoundRect(x + 1, y + 1, w - 2, h - 2, 16, 16);

        int inset = Math.max(11, w / 20);
        int artY = y + 44;
        int artH = h - 111;
        drawLocationArt(g, i, x + inset, artY, w - inset * 2, artH, locked);

        centered(g, String.format("LEVEL %02d", i + 1), x + w / 2, y + 29,
            focused ? 15 : 13, accent, true);
        String[] words = NAMES[i].split(" ");
        if (words.length <= 2) {
            centered(g, NAMES[i], x + w / 2, y + h - 48, focused ? 13 : 11, WHITE, true);
        } else {
            int mid = words.length / 2;
            String first = String.join(" ", Arrays.copyOfRange(words, 0, mid));
            String second = String.join(" ", Arrays.copyOfRange(words, mid, words.length));
            centered(g, first, x + w / 2, y + h - 58, focused ? 12 : 10, WHITE, true);
            centered(g, second, x + w / 2, y + h - 43, focused ? 12 : 10, WHITE, true);
        }
        String status = locked ? "LOCKED" : SaveManager.isCompleted(i + 1) ? "COMPLETED"
            : i == 0 && SaveManager.hasSavedLevel1() ? "IN PROGRESS" : "UNLOCKED";
        centered(g, status, x + w / 2, y + h - 20, 10,
            locked ? MUTED : new Color(167, 226, 186), true);
    }

    private void drawLocationArt(Graphics2D g, int index, int x, int y, int w, int h, boolean locked) {
        Shape oldClip = g.getClip();
        g.setClip(x, y, w, h);
        Color base = ACCENTS[index];
        g.setPaint(new GradientPaint(x, y, new Color(27 + index * 4, 29, 52 + index * 3),
            x, y + h, new Color(16, 15, 32)));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 36));
        g.fillOval(x + w / 3 - 60, y - 35, w, h);
        g.setColor(new Color(26, 23, 40));
        g.fillRect(x, y + h - 30, w, 30);

        // Distinct, lightweight pixel-art silhouettes for each location.
        for (int j = 0; j < 6; j++) {
            int bw = Math.max(19, w / 6 + (j % 2) * 9);
            int bx = x + j * w / 5 - 10;
            int bh = (index == 0 ? 52 : index == 1 ? 77 : index == 2 ? 65 : index == 3 ? 85 : 90)
                + ((j * 29 + index * 17) % 54);
            int by = y + h - 25 - bh;
            g.setColor(new Color(27 + index * 2, 25, 39 + index * 2));
            g.fillRect(bx, by, bw, bh);
            g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 80));
            for (int wx = bx + 7; wx < bx + bw - 3; wx += 14)
                for (int wy = by + 10; wy < by + bh - 7; wy += 18)
                    if ((wx + wy + j) % 3 != 0) g.fillRect(wx, wy, 5, 7);
        }
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 125));
        if (index == 1) { // hospital cross
            int cx = x + w / 2, cy = y + h / 3;
            g.fillRect(cx - 5, cy - 24, 10, 48);
            g.fillRect(cx - 24, cy - 5, 48, 10);
        } else if (index == 2) { // factory chimneys
            for (int k = 0; k < 3; k++) {
                int cx = x + w / 4 + k * w / 4;
                g.fillRect(cx, y + 35 + k * 14, 12, h / 2);
                g.drawOval(cx - 9, y + 8 + k * 12, 32, 19);
            }
        } else if (index == 3) { // research antenna
            int cx = x + w / 2;
            g.drawLine(cx, y + 15, cx, y + h / 2);
            g.drawArc(cx - 25, y + 5, 50, 42, 20, 140);
            g.drawArc(cx - 40, y - 6, 80, 64, 25, 130);
        } else if (index == 4) { // genesis chamber
            int cx = x + w / 2;
            g.drawRoundRect(cx - 24, y + 26, 48, 92, 20, 20);
            g.drawLine(cx - 15, y + 70, cx + 15, y + 70);
        } else { // deserted town lamp
            int cx = x + w / 2;
            g.fillRect(cx, y + 34, 4, 90);
            g.fillRect(cx - 21, y + 34, 25, 5);
            g.fillRect(cx - 24, y + 37, 13, 7);
        }
        g.setColor(new Color(184, 171, 207, 27));
        for (int j = 0; j < 10; j++)
            g.fillRect(x + ((j * 31 + 11) % w), y + h - 27 - j * 9, 32, 2);
        if (locked) {
            g.setColor(new Color(10, 10, 19, 164));
            g.fillRect(x, y, w, h);
            drawLock(g, x + w / 2, y + h / 2);
        }
        g.setClip(oldClip);
        g.setColor(new Color(120, 102, 156));
        g.drawRect(x, y, w - 1, h - 1);
    }

    private void drawLock(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(195, 186, 208));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(cx - 15, cy - 26, 30, 31, 15, 15);
        g.fillRoundRect(cx - 22, cy - 7, 44, 35, 7, 7);
        g.setColor(new Color(48, 43, 61));
        g.fillOval(cx - 4, cy + 3, 8, 8);
        g.fillRect(cx - 2, cy + 9, 4, 10);
    }

    private void drawArrow(Graphics2D g, boolean left, boolean enabled) {
        Rectangle r = left ? LEFT_ARROW : RIGHT_ARROW;
        int cx = r.x + r.width / 2, cy = r.y + r.height / 2;
        g.setColor(enabled ? new Color(76, 49, 114, 210) : new Color(43, 39, 59, 160));
        g.fillRoundRect(r.x + 9, r.y + 14, r.width - 18, r.height - 28, 12, 12);
        g.setColor(enabled ? new Color(217, 164, 255) : new Color(97, 90, 114));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int dir = left ? 1 : -1;
        g.drawLine(cx + dir * 9, cy - 13, cx - dir * 6, cy);
        g.drawLine(cx - dir * 6, cy, cx + dir * 9, cy + 13);
    }

    private void drawBottom(Graphics2D g) {
        int level = selectedIndex + 1;
        boolean locked = level > SaveManager.highestUnlockedLevel();
        boolean saved = level == 1 && SaveManager.hasSavedLevel1();
        String actionText = locked ? "LOCKED" : saved ? "CONTINUE GAME" : "EXPLORE";
        g.setPaint(new GradientPaint(ACTION.x, ACTION.y,
            locked ? new Color(57, 52, 73) : new Color(128, 76, 177),
            ACTION.x, ACTION.y + ACTION.height,
            locked ? new Color(42, 39, 58) : new Color(71, 41, 111)));
        g.fillRoundRect(ACTION.x, ACTION.y, ACTION.width, ACTION.height, 10, 10);
        g.setStroke(new BasicStroke(2));
        g.setColor(locked ? new Color(105, 97, 121) : new Color(221, 171, 255));
        g.drawRoundRect(ACTION.x, ACTION.y, ACTION.width, ACTION.height, 10, 10);
        centered(g, actionText, 550, ACTION.y + 31, 17, locked ? MUTED : WHITE, true);

        g.setColor(new Color(51, 38, 73));
        g.fillRoundRect(BACK.x, BACK.y, BACK.width, BACK.height, 8, 8);
        g.setColor(new Color(154, 115, 194));
        g.drawRoundRect(BACK.x, BACK.y, BACK.width, BACK.height, 8, 8);
        centered(g, "< BACK", BACK.x + BACK.width / 2, BACK.y + 27, 13, WHITE, true);

        for (int i = 0; i < 5; i++) {
            g.setColor(i == selectedIndex ? new Color(209, 151, 255) : new Color(80, 70, 103));
            g.fillOval(500 + i * 25, 594, i == selectedIndex ? 10 : 7, i == selectedIndex ? 10 : 7);
        }
    }

    private static void centered(Graphics2D g, String text, int centerX, int baseline,
                                 int size, Color color, boolean bold) {
        g.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, size));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, centerX - fm.stringWidth(text) / 2, baseline);
    }
}
