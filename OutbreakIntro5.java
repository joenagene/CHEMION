import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.text.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;


public class OutbreakIntro5 extends JPanel {

    private static final int FADE_IN = 1000;
    private static final int HOLD = 8500;
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

    private OutbreakIntro5(Scene[] scenes, Runnable onFinished) {
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

    public static OutbreakIntro5 opening(Runnable onFinished) {
        return new OutbreakIntro5(new Scene[] {
            new Scene("THE LAST SIGNAL CAME FROM GENESIS.",
                "Beyond the research facility, a final transmission repeated one name: Genesis. "
                + "A private demonstration for ministers and investors had promised a medical revolution. The doors closed before the guests could leave.",
                false, "GENESIS.", "fell silent."),
            new Scene("", "The surviving records did not describe a cure. They described a choice: "
                + "how much of a person could be changed before nothing of them remained. "
                + "The answer was buried behind sealed rooms and erased names.",
                false, null, "erased names."),
            new Scene("", "The official report blamed an accident. Four sealed archives held the testimony that had been erased. "
                + "At the heart of Genesis, the first patient's heartbeat still appeared on a monitor. "
                + "Someone had kept the machine running for a reason.",
                false, null, "for a reason.")
        }, onFinished);
    }
    public static String[][] scientistConversation() {
        return new String[][] {
            {"Lab Control", "Genesis is still active. The doors have independent locks. Some records are missing from the central database."},
            {"Scientist", "Then whoever hid them wanted us to see the laboratory in pieces."},
            {"Lab Control", "The specimens are not simply guarding the facility. Their vital signs are linked to the original host."},
            {"Scientist", "If I disable the source without understanding that link, I could destroy our only chance to reverse the infection."},
            {"Lab Control", "Proceed carefully. The old research terminals may contain what the main system erased."},
            {"Scientist", "Then the truth is part of the treatment. I won't leave anyone behind just because the records call them a failure."}
        };
    }
    public static String[][] endingConversation() {
        return new String[][] {
            {"Lab Control", "The network is silent. Genesis is no longer transmitting the infection signal."},
            {"Scientist", "Patient Zero wasn't the monster they described. They were the first person who trusted the researchers."},
            {"Lab Control", "The recovered records confirm it. Officials authorized the trial after the safety team warned them. The archive names those responsible."},
            {"Scientist", "They had a chance to stop. They chose control instead of care."},
            {"Lab Control", "The stabilized samples and the full research archive are intact. We can begin reversing what happened outside."},
            {"Scientist", "Send the complete archive to the survivors. The cure belongs to them, and so does the truth."}
        };
    }
    public static OutbreakIntro5 finalEnding(Runnable onFinished) {
        return new OutbreakIntro5(new Scene[] {
            new Scene("THE ORIGIN WAS NOT THE END.",
                "Genesis fell quiet. Beyond its sealed doors, the city was still wounded. "
                + "But the research had finally returned to the people it was meant to protect.",
                true, "NOT THE END.", "meant to protect."),
            new Scene("THE LAST CURE", "A cure was never a single formula. It was the decision "
                + "to see a person where others had seen only an experiment. "
                + "And to make sure no one could bury that choice again.",
                true, "THE LAST CURE", "bury that choice again.")
        }, onFinished);
    }

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
