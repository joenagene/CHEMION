import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.text.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;


public class OutbreakIntro2 extends JPanel {

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

    private OutbreakIntro2(Scene[] scenes, Runnable onFinished) {
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

    // Level 2's mysterious opening. Narration first, then scientist dialogue.
    // This story deliberately hints at the truth without revealing it yet.
    public static OutbreakIntro2 opening(Runnable onFinished) {
        return new OutbreakIntro2(new Scene[] {
            new Scene(
                "THE HOSPITAL WAS SUPPOSED TO SAVE THEM.",
                "Beyond the fallen town, the emergency hospital stood in silence. "
                + "Its doors were sealed, yet the lights still burned inside. "
                + "The medical record found on the Mutated Survivor pointed here.",
                false,
                "SUPPOSED TO SAVE THEM.",
                "stood in silence."
            ),
            new Scene(
                "",
                "Patient files had been altered. Unlabeled vials lay shattered "
                + "beside unfinished treatment notes. Something had happened "
                + "behind those locked doors... something the records refused to explain.",
                false,
                null,
                "something the records refused to explain."
            ),
            new Scene(
                "",
                "Some infected bore strange burns. Others had changed in ways "
                + "the original outbreak could not explain. Were they victims "
                + "of the infection... or of the cure meant to stop it?",
                false,
                null,
                "or of the cure meant to stop it?"
            )
        }, onFinished);
    }

    // The conversation begins ONLY after the last narration scene fades out.
    public static String[][] scientistConversation() {
        return new String[][] {
            {
                "Lab Control",
                "The medical record from the Mutated Survivor leads directly to this hospital. But its emergency channel is silent."
            },
            {
                "Scientist",
                "The doors were locked from the inside. Why would they seal themselves in?"
            },
            {
                "Lab Control",
                "The patient files mention unusual chemical reactions. Some treatments were never approved."
            },
            {
                "Scientist",
                "So the infected here might be different from the ones we encountered in town."
            },
            {
                "Lab Control",
                "Exactly. We don't know what changed them. Examine each case, recover the research notes, and identify their weaknesses."
            },
            {
                "Scientist",
                "Understood. If someone tried to stop the outbreak here, I want to know what went wrong."
            }
        };
    }

    // One-time narration for each Level 2 zombie's first encounter.
    public static OutbreakIntro2 enemy(String enemyId, Runnable onFinished) {
        if (enemyId == null) return null;

        String heading;
        String body;

        switch (enemyId.trim().toLowerCase(Locale.ROOT)) {
            case "acid_zombie":
                heading = "ACID ZOMBIE - THE CORRODED PATIENT";
                body = "A chemical spill reached the hospital's treatment wing. "
                     + "One infected patient was exposed to corrosive substances, "
                     + "leaving its body dangerously unstable.";
                break;

            case "patient_zero":
                heading = "PATIENT ZERO - THE FIRST RECORDED CASE";
                body = "The hospital labeled its earliest documented infected "
                     + "patient as Case Zero. The file marks the beginning of "
                     + "the hospital's outbreak records, but not necessarily "
                     + "the origin of the infection itself.";
                break;

            case "toxic_zombie":
                heading = "TOXIC ZOMBIE - THE CONTAMINATED HOST";
                body = "When experimental chemicals leaked into the ventilation "
                     + "system, some infected patients absorbed toxic fumes. "
                     + "Their bodies now carry a dangerous contamination.";
                break;

            case "plague_doctor":
                heading = "PLAGUE DOCTOR - THE FALLEN PHYSICIAN";
                body = "A physician remained behind to search for a treatment. "
                     + "Repeated exposure to unfinished compounds changed him. "
                     + "Now he guards the hospital's sealed research wing.";
                break;

            default:
                return null;
        }

        return new OutbreakIntro2(new Scene[] {
            new Scene(heading, body, true, null, null)
        }, onFinished);
    }

    // Conversation after the Level 2 boss is defeated.
    public static String[][] level2EndingConversation() {
        return new String[][] {
            {
                "Scientist",
                "I found the physician's research log. These chemicals weren't all made here."
            },
            {
                "Lab Control",
                "The records mention repeated deliveries from a factory outside town."
            },
            {
                "Scientist",
                "Even after the hospital was placed under quarantine?"
            },
            {
                "Lab Control",
                "Yes. That factory may explain where the experimental substances came from."
            },
            {
                "Scientist",
                "Then that's our next destination."
            }
        };
    }

    public static OutbreakIntro2 level3Unlocked(Runnable onFinished) {
        return new OutbreakIntro2(new Scene[] {
            new Scene(
                "LEVEL 3 UNLOCKED",
                "TOXIC CHEMICAL FACTORY",
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
        if (scene.heading.equals("LEVEL 3 UNLOCKED")) {
            int centerY = getHeight() / 2;
            g2.setFont(new Font("Monospaced", Font.BOLD, 24));
            g2.setColor(new Color(150, 150, 150));
            g2.drawString(scene.heading,
                (getWidth() - g2.getFontMetrics().stringWidth(scene.heading)) / 2,
                centerY - 65);
            int size = 48;
            g2.setFont(new Font("Serif", Font.BOLD, size));
            while (size > 12 && g2.getFontMetrics().stringWidth(scene.body) > getWidth() - 40) {
                g2.setFont(new Font("Serif", Font.BOLD, --size));
            }
            g2.setColor(new Color(220, 220, 220));
            g2.drawString(scene.body,
                (getWidth() - g2.getFontMetrics().stringWidth(scene.body)) / 2,
                centerY);
            g2.dispose();
            return;
        }
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
    // Standalone Level 2 INTRO PREVIEW. No Main.java connection.
    // The full game can later call opening(...) and scientistConversation().
    public static void main(String[] args) {
        final int scientistIndex;
        if (args.length > 0) {
            int parsed;
            try {
                parsed = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                parsed = 2;
            }
            scientistIndex = Math.max(0, Math.min(4, parsed));
        } else {
            scientistIndex = 2; // Nicole for preview only; no saved selection changed.
        }

        SwingUtilities.invokeLater(() -> {
            JFrame preview = new JFrame("CHEMION - Level 2 Intro Preview");
            preview.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            preview.setSize(1100, 620);
            preview.setMinimumSize(new Dimension(800, 450));
            preview.setLocationRelativeTo(null);

            CardLayout cards = new CardLayout();
            JPanel pages = new JPanel(cards);
            pages.setBackground(Color.BLACK);

            // End the standalone preview here; do NOT open the Main Menu.
            JPanel finished = new JPanel(new GridBagLayout());
            finished.setBackground(Color.BLACK);
            JPanel message = new JPanel();
            message.setOpaque(false);
            message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("END OF LEVEL 2 INTRO PREVIEW");
            title.setForeground(new Color(218, 184, 255));
            title.setFont(new Font("Monospaced", Font.BOLD, 22));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel subtitle = new JLabel("Level 2 gameplay is not connected yet.");
            subtitle.setForeground(new Color(200, 197, 210));
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            JButton close = new JButton("CLOSE PREVIEW");
            close.setAlignmentX(Component.CENTER_ALIGNMENT);
            close.addActionListener(e -> preview.dispose());
            message.add(title);
            message.add(Box.createVerticalStrut(20));
            message.add(subtitle);
            message.add(Box.createVerticalStrut(28));
            message.add(close);
            finished.add(message);

            DialoguePanel dialogue = new DialoguePanel(
                scientistIndex,
                scientistConversation(),
                () -> cards.show(pages, "finished")
            );
            OutbreakIntro2 narration = opening(() -> {
                cards.show(pages, "dialogue");
                dialogue.requestFocusInWindow();
            });

            pages.add(narration, "narration");
            pages.add(dialogue, "dialogue");
            pages.add(finished, "finished");
            preview.setContentPane(pages);
            cards.show(pages, "narration");
            preview.setVisible(true);
            narration.start();
        });
    }

}
