import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.text.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;


public class OutbreakIntro3 extends JPanel {

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

    private OutbreakIntro3(Scene[] scenes, Runnable onFinished) {
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

    // Level 3 begins where the hospital's final shipment records lead.
    // Preserve the cinematic pacing and mystery of Levels 1 and 2.
    public static OutbreakIntro3 opening(Runnable onFinished) {
        return new OutbreakIntro3(new Scene[] {
            new Scene(
                "THE FACTORY SHOULD HAVE BEEN EMPTY.",
                "The physician's records led beyond the hospital's quarantine line. "
                + "At the end of an abandoned industrial road stood a chemical factory. "
                + "Its gates were chained, but its machinery had not gone silent.",
                false,
                "SHOULD HAVE BEEN EMPTY.",
                "its machinery had not gone silent."
            ),
            new Scene(
                "",
                "Warning lights flickered above ruptured storage tanks. "
                + "Unmarked containers lay scattered across the production floor. "
                + "Every corridor carried traces of substances that should never have escaped.",
                false,
                null,
                "substances that should never have escaped."
            ),
            new Scene(
                "",
                "The hospital had received its last shipments from this place. "
                + "But the factory's production records had been erased. "
                + "Someone had hidden what was made here... and why.",
                false,
                null,
                "and why."
            )
        }, onFinished);
    }

    // Begin this dialogue only after the opening narration finishes.
    // "Scientist" is resolved by Level 1's existing portrait-based dialogue UI.
    public static String[][] scientistConversation() {
        return new String[][] {
            {
                "Lab Control",
                "The Plague Doctor's research log confirms it. This factory supplied the hospital with experimental compounds."
            },
            {
                "Scientist",
                "Then this is where those chemicals came from. But why is the production line still running?"
            },
            {
                "Lab Control",
                "Safety systems are offline. The remaining samples show unstable mixtures, extreme pH levels, and unusual concentrations."
            },
            {
                "Scientist",
                "That could explain why the infected here are different. Their bodies were exposed to far more concentrated substances."
            },
            {
                "Lab Control",
                "Be careful. Study each mutation, collect research materials, and identify the correct treatment before engaging."
            },
            {
                "Scientist",
                "Understood. I'll search the production records too. Someone wanted these experiments to continue."
            }
        };
    }

    // One-time narration for each Level 3 infected type's first encounter.
    public static OutbreakIntro3 enemy(String enemyId, Runnable onFinished) {
        if (enemyId == null) return null;

        String heading;
        String body;

        switch (enemyId.trim().toLowerCase(Locale.ROOT)) {
            case "toxic_brute":
                heading = "TOXIC BRUTE - THE CONTAMINATED WORKER";
                body = "A factory loader was trapped near a concentrated waste tank. "
                     + "Exposure and infection transformed the worker into a massive, "
                     + "heavily altered creature with terrifying strength.";
                break;

            case "venom_spitter":
                heading = "VENOM SPITTER - THE MIXING-LINE MUTANT";
                body = "A mixing technician was caught in a chemical spray when "
                     + "the pressure valves failed. The infection altered the host, "
                     + "which now expels contaminated fluid from a distance.";
                break;

            case "explosive_zombie":
                heading = "EXPLOSIVE ZOMBIE - THE UNSTABLE HOST";
                body = "A maintenance worker was exposed beside a ruptured "
                     + "pressure vessel. Volatile residue remains trapped around "
                     + "the infected body, making sudden contact dangerous.";
                break;

            case "chemical_abomination":
                heading = "CHEMICAL ABOMINATION - THE FAILED EXPERIMENT";
                body = "In the restricted processing chamber, an infected host "
                     + "was transformed by multiple experimental substances. "
                     + "The final security report names it the Chemical Abomination, "
                     + "but the experiment's purpose is missing.";
                break;

            default:
                return null;
        }

        return new OutbreakIntro3(new Scene[] {
            new Scene(heading, body, true, null, null)
        }, onFinished);
    }

    // Conversation after the Level 3 boss is stabilized.
    public static String[][] level3EndingConversation() {
        return new String[][] {
            {
                "Scientist",
                "The processing chamber is secure. I found a shipment label with another facility code."
            },
            {
                "Lab Control",
                "That code belongs to a sealed research site beyond the factory district."
            },
            {
                "Scientist",
                "So the hospital wasn't the only place receiving these chemicals."
            },
            {
                "Lab Control",
                "Correct. The transfer records were erased, but one destination remains: the Lost Research Facility."
            },
            {
                "Scientist",
                "Then we follow the trail. Whatever happened here, the answers are still ahead."
            }
        };
    }

    public static OutbreakIntro3 level4Unlocked(Runnable onFinished) {
        return new OutbreakIntro3(new Scene[] {
            new Scene(
                "LEVEL 4 UNLOCKED",
                "LOST RESEARCH FACILITY",
                true,
                null,
                null
            )
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
