import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.text.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;


public class OutbreakIntro4 extends JPanel {

    private static final int FADE_IN = 1000;
    private static final int HOLD = 4500;
    private static final int FADE_OUT = 1000;
    private static final int SCENE_DURATION = FADE_IN + HOLD + FADE_OUT;

    private final Scene[] scenes;
    private final Runnable onFinished;
    private final Timer timer;

    private int sceneIndex;
    private long startedAt;
    private float opacity;

    private static class Scene {
        final String heading;
        final String body;
        final boolean goldHeading;
        final String italicHeading;
        final String italicBody;

        Scene(String heading, String body, boolean goldHeading,
              String italicHeading, String italicBody) {
            this.heading = heading;
            this.body = body;
            this.goldHeading = goldHeading;
            this.italicHeading = italicHeading;
            this.italicBody = italicBody;
        }
    }

    private OutbreakIntro4(Scene[] scenes, Runnable onFinished) {
        this.scenes = scenes;
        this.onFinished = onFinished;
        this.timer = new Timer(40, e -> updateFade());

        setOpaque(true);
        setBackground(Color.BLACK);
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (e.getX() >= getWidth() / 2) {
                    nextScene();
                } else {
                    previousScene();
                }
            }
        });

        bindKey(KeyEvent.VK_RIGHT, "next", this::nextScene);
        bindKey(KeyEvent.VK_ENTER, "enter", this::nextScene);
        bindKey(KeyEvent.VK_SPACE, "space", this::nextScene);
        bindKey(KeyEvent.VK_LEFT, "previous", this::previousScene);
    }

    private void bindKey(int key, String actionName, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(key, 0), actionName
        );
        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // Level 4 follows the factory shipment trail into the sealed facility.
    public static OutbreakIntro4 opening(Runnable onFinished) {
        return new OutbreakIntro4(new Scene[] {
            new Scene(
                "THE FACILITY WAS NEVER ABANDONED.",
                "Beyond the factory district, a research facility stood behind "
                + "locked gates and silent watchtowers. The transfer records "
                + "had named this place, but every official map marked it closed.",
                false, "NEVER ABANDONED.", "marked it closed."
            ),
            new Scene(
                "",
                "Inside, security monitors still glowed. Broken containment "
                + "chambers lined the corridors, and experimental growths had "
                + "spread across laboratory equipment. Someone had continued "
                + "the experiments long after the evacuation.",
                false, null, "long after the evacuation."
            ),
            new Scene(
                "",
                "The factory's missing records had led here. Yet the deepest "
                + "research files remained encrypted, and the facility's last "
                + "distress signal ended with a warning: do not open the lower lab.",
                false, null, "do not open the lower lab."
            )
        }, onFinished);
    }

    public static String[][] scientistConversation() {
        return new String[][] {
            {"Lab Control", "The shipment code matches this facility. Its systems are still drawing power, but no staff have answered our calls."},
            {"Scientist", "The factory was sending experimental materials here. What were they trying to develop?"},
            {"Lab Control", "The remaining logs mention containment failures, crystalline growth, and unauthorized biological research."},
            {"Scientist", "Then the infected here may have changed in ways we haven't seen before."},
            {"Lab Control", "Proceed carefully. Document each mutation, recover the missing research, and analyze samples before preparing treatments."},
            {"Scientist", "Understood. We need to find out who kept this place running—and what they were hiding."}
        };
    }

    // Optional first-encounter cinematics, ready for future Level 4 gameplay.
    public static OutbreakIntro4 enemy(String enemyId, Runnable onFinished) {
        if (enemyId == null) return null;
        String heading, body;
        switch (enemyId.trim().toLowerCase(Locale.ROOT)) {
            case "experiment_01":
                heading = "EXPERIMENT 01 - THE FIRST SUBJECT";
                body = "An early test subject is still roaming the research wing. Its behavior suggests the experiments continued after the containment alarms began.";
                break;
            case "mutant_stalker":
                heading = "MUTANT STALKER - THE HUNTER";
                body = "The Mutant Stalker has escaped its containment cell. Its unpredictable movements make approaching it dangerous.";
                break;
            case "project_chimera":
                heading = "PROJECT CHIMERA - THE HYBRID EXPERIMENT";
                body = "Project Chimera was kept in a restricted test chamber. The surviving research notes describe an unstable, dangerous mutation.";
                break;
            case "regenerator_zombie":
                heading = "REGENERATOR ZOMBIE - THE HIDDEN EXPERIMENT";
                body = "The Regenerator Zombie was engineered to recover from damage. Its rapid recovery makes it the final obstacle to securing the facility.";
                break;
            default: return null;
        }
        return new OutbreakIntro4(new Scene[] {
            new Scene(heading, body, true, null, null)
        }, onFinished);
    }

    // Level 4 boss completion: bridge the facility's hidden research to Genesis Laboratory.
    public static String[][] level4EndingConversation() {
        return new String[][] {
            {"Scientist", "The Regenerator is stabilized. I recovered the final encrypted research report."},
            {"Lab Control", "I'm decoding it now. These experiments were never isolated. The samples were transferred to a site called Genesis Laboratory."},
            {"Scientist", "Genesis? The factory shipments and the hospital trials were all connected to it?"},
            {"Lab Control", "Yes. The facility's records identify Genesis as the central research site. The origin of the outbreak may be there."},
            {"Scientist", "Then that's our final destination. I'll take the recovered data and finish what we started."}
        };
    }

    public static OutbreakIntro4 level5Unlocked(Runnable onFinished) {
        return new OutbreakIntro4(new Scene[] {
            new Scene("LEVEL 5 UNLOCKED", "GENESIS LABORATORY", true, null, null)
        }, onFinished);
    }

    // Reuse Level 1's existing portrait-based dialogue panel.
    public static class DialoguePanel extends OutbreakIntro.DialoguePanel {
        public DialoguePanel(int scientistIndex, String[][] lines,
                             Runnable onFinished) {
            super(scientistIndex, lines, onFinished);
        }
    }

    public void start() {
        timer.stop();
        sceneIndex = 0;
        opacity = 0f;
        startedAt = System.nanoTime();
        setVisible(true);
        timer.start();
        requestFocusInWindow();
    }

    public void stop() {
        timer.stop();
    }

    private void nextScene() {
        if (!timer.isRunning()) return;
        if (sceneIndex + 1 >= scenes.length) {
            finishNarration();
        } else {
            sceneIndex++;
            restartSceneTimer();
        }
    }

    private void previousScene() {
        if (!timer.isRunning() || sceneIndex == 0) return;
        sceneIndex--;
        restartSceneTimer();
    }

    private void restartSceneTimer() {
        startedAt = System.nanoTime();
        opacity = 0f;
        repaint();
    }

    private void finishNarration() {
        timer.stop();
        setVisible(false);
        if (onFinished != null) onFinished.run();
    }

    private void updateFade() {
        long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
        if (elapsed >= SCENE_DURATION) {
            nextScene();
            return;
        }
        if (elapsed < FADE_IN) {
            opacity = elapsed / (float) FADE_IN;
        } else if (elapsed < FADE_IN + HOLD) {
            opacity = 1f;
        } else {
            opacity = (SCENE_DURATION - elapsed) / (float) FADE_OUT;
        }
        repaint();
    }

    private List<TextLayout> wrapText(String text, Font font,
                                      String italicPart, float width,
                                      FontRenderContext context) {
        List<TextLayout> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        AttributedString attributed = new AttributedString(text);
        attributed.addAttribute(TextAttribute.FONT, font);

        if (italicPart != null) {
            int start = text.indexOf(italicPart);
            if (start >= 0) {
                attributed.addAttribute(
                    TextAttribute.FONT,
                    font.deriveFont(font.getStyle() | Font.ITALIC),
                    start,
                    start + italicPart.length()
                );
            }
        }

        AttributedCharacterIterator iterator = attributed.getIterator();
        LineBreakMeasurer measurer = new LineBreakMeasurer(iterator, context);
        while (measurer.getPosition() < iterator.getEndIndex()) {
            lines.add(measurer.nextLayout(width));
        }
        return lines;
    }

    private float blockHeight(List<TextLayout> lines) {
        float height = 0f;
        for (TextLayout line : lines) {
            height += line.getAscent() + line.getDescent()
                    + line.getLeading() + 6f;
        }
        return height;
    }

    private float drawCentered(Graphics2D g2, List<TextLayout> lines,
                               float y, Color color) {
        g2.setColor(color);
        for (TextLayout line : lines) {
            y += line.getAscent();
            line.draw(g2, (getWidth() - line.getAdvance()) / 2f, y);
            y += line.getDescent() + line.getLeading() + 6f;
        }
        return y;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.SrcOver.derive(
            Math.max(0f, Math.min(1f, opacity))
        ));

        Scene scene = scenes[sceneIndex];
        boolean hasHeading = !scene.heading.isEmpty();
        Font headingFont = new Font("SansSerif", Font.BOLD,
                                    scene.goldHeading ? 16 : 19);
        Font bodyFont = new Font("SansSerif", Font.PLAIN, 17);
        float maxWidth = Math.max(100f,
                                  Math.min(760f, getWidth() - 64f));

        List<TextLayout> headingLines = wrapText(
            scene.heading, headingFont, scene.italicHeading,
            maxWidth, g2.getFontRenderContext()
        );
        List<TextLayout> bodyLines = wrapText(
            scene.body, bodyFont, scene.italicBody,
            maxWidth, g2.getFontRenderContext()
        );

        float gap = hasHeading ? 22f : 0f;
        float totalHeight = blockHeight(headingLines)
                            + gap + blockHeight(bodyLines);
        float y = (getHeight() - totalHeight) / 2f;

        y = drawCentered(g2, headingLines, y,
            scene.goldHeading ? new Color(212, 180, 128)
                              : new Color(244, 244, 244));
        drawCentered(g2, bodyLines, y + gap,
                     new Color(239, 239, 242));
        g2.dispose();
    }
}
