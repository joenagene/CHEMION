
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;

public class OutbreakIntro extends JPanel {

    private static final int FADE_IN = 1000;
    private static final int HOLD = 4500;
    private static final int FADE_OUT = 1000;
    private static final int SCENE_DURATION = FADE_IN + HOLD + FADE_OUT;

    private final Scene[] scenes;
    private final Runnable onFinished;
    private final Timer timer;

    private long startedAt;
    private float opacity = 0f;
    private int sceneIndex = 0;

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

    private OutbreakIntro(Scene[] scenes, Runnable onFinished) {
        this.scenes = scenes;
        this.onFinished = onFinished;

        setOpaque(true);
        setBackground(new Color(16, 18, 22));

        timer = new Timer(40, e -> updateFade());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                if (e.getX() >= getWidth() / 2) {
                    nextScene();
                } else {
                    previousScene();
                }
            }
        });
    }

    public static OutbreakIntro opening(Runnable onFinished) {
        return new OutbreakIntro(new Scene[] {
            new Scene(
                "It began with a fever. Then came the silence.",
                "The elderly and the sick were among the first to fall. "
                + "Within days, the infection spread throughout the town. "
                + "But not everyone who turned became the same kind of monster.",
                false,
                "Then came the silence.",
                "the same kind of monster."
            ),
            new Scene(
                "",
                "Some became faster. Others lost the ability to walk. "
                + "And a few transformed into something far more dangerous.",
                false,
                null,
                "far more dangerous."
            ),
            new Scene(
                "",
                "The town had fallen, but the fight was far from over. "
                + "Somewhere within the outbreak lay the answer—and "
                + "with it, humanity's last hope.",
                false,
                null,
                "humanity's last hope."
            )
        }, onFinished);
    }

    public static OutbreakIntro enemy(String enemyId, Runnable onFinished) {
        if (enemyId == null) {
            return null;
        }

        String heading;
        String body;

        switch (enemyId) {
            case "common_zombie":
                heading = "COMMON ZOMBIE - THE FIRST VICTIMS";
                body = "When the outbreak reached the evacuation shelters, "
                    + "the elderly and those already weakened by illness "
                    + "were among the first to succumb. The infection left "
                    + "their bodies slow, but their hunger never faded.";
                break;

            case "runner_zombie":
                heading = "RUNNER ZOMBIE - THE RESTLESS INFECTED";
                body = "The younger survivors tried to outrun the outbreak. "
                    + "But for some, the pathogen overstimulated their nervous "
                    + "systems, forcing their bodies into violent, "
                    + "uncontrollable movement.";
                break;

            case "crawler_zombie":
                heading = "CRAWLER ZOMBIE - THE FALLEN SURVIVORS";
                body = "During the evacuation, many were injured and left behind. "
                    + "Unable to stand, some continued dragging themselves "
                    + "through the streets, even after the infection had taken over.";
                break;

            case "mutated_survivor":
                heading = "MUTATED SURVIVOR - THE FAILED RESCUE";
                body = "He was part of a rescue team searching for survivors. "
                    + "But after exposure to an unidentified chemical substance, "
                    + "the infection transformed his body beyond recognition.";
                break;

            default:
                return null;
        }

        return new OutbreakIntro(
            new Scene[] {
                new Scene(heading, body, true, null, null)
            },
            onFinished
        );
    }

    public static OutbreakIntro level2Unlocked(Runnable onFinished) {
        return new OutbreakIntro(
            new Scene[] {
                new Scene(
                    "LEVEL 2 UNLOCKED",
                    "CONTAMINATED HOSPITAL",
                    true,
                    null,
                    null
                )
            },
            onFinished
        );
    }

    public static String[][] scientistConversation() {
        return new String[][] {
            {
                "Lab Control",
                "We've detected several forms of infected in this town."
            },
            {
                "Scientist",
                "Several forms? I thought they were all infected by the same pathogen."
            },
            {
                "Lab Control",
                "They are. But the infection reacts differently depending on the host's condition and chemical exposure."
            },
            {
                "Scientist",
                "Then each type could have a different weakness."
            },
            {
                "Lab Control",
                "Exactly. Study the infected, identify their weaknesses, and create the compounds needed to stop them."
            },
            {
                "Scientist",
                "Understood. Let's find out what happened here."
            }
        };
    }

    public static String[][] level1EndingConversation() {
        return new String[][] {
            {
                "Scientist",
                "Wait... there's a medical record attached to his identification."
            },
            {
                "Lab Control",
                "The treatment came from the town's emergency hospital."
            },
            {
                "Scientist",
                "So someone was already experimenting with the infection?"
            },
            {
                "Lab Control",
                "We need to investigate. The hospital may have answers."
            }
        };
    }

    public void start() {
        sceneIndex = 0;
        opacity = 0f;
        startedAt = System.nanoTime();

        setVisible(true);
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void nextScene() {
        if (!timer.isRunning()) {
            return;
        }

        if (sceneIndex + 1 >= scenes.length) {
            finishNarration();
        } else {
            sceneIndex++;
            restartSceneTimer();
        }
    }

    private void previousScene() {
        if (!timer.isRunning() || sceneIndex == 0) {
            return;
        }

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

        if (onFinished != null) {
            onFinished.run();
        }
    }

    private void updateFade() {
        long elapsed =
            (System.nanoTime() - startedAt) / 1_000_000L;

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

    private List<TextLayout> wrapText(
            String text,
            Font font,
            String italicPart,
            float width,
            FontRenderContext context) {

        List<TextLayout> lines = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return lines;
        }

        AttributedString attributed = new AttributedString(text);

        attributed.addAttribute(
            TextAttribute.FONT,
            font
        );

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

        AttributedCharacterIterator iterator =
            attributed.getIterator();

        LineBreakMeasurer measurer =
            new LineBreakMeasurer(iterator, context);

        while (measurer.getPosition() < iterator.getEndIndex()) {
            lines.add(measurer.nextLayout(width));
        }

        return lines;
    }

    private float blockHeight(List<TextLayout> lines) {
        float height = 0;

        for (TextLayout line : lines) {
            height += line.getAscent()
                    + line.getDescent()
                    + line.getLeading()
                    + 6;
        }

        return height;
    }

    private float drawCentered(
            Graphics2D g2,
            List<TextLayout> lines,
            float y,
            Color color) {

        g2.setColor(color);

        for (TextLayout line : lines) {
            y += line.getAscent();

            float x =
                (getWidth() - line.getAdvance()) / 2f;

            line.draw(g2, x, y);

            y += line.getDescent()
                    + line.getLeading()
                    + 6;
        }

        return y;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        g2.setComposite(
            AlphaComposite.SrcOver.derive(
                Math.max(0f, Math.min(1f, opacity))
            )
        );

        Scene scene = scenes[sceneIndex];
        if (scene.heading.equals("LEVEL 2 UNLOCKED")) {
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

        Font headingFont = new Font(
            "SansSerif",
            Font.BOLD,
            scene.goldHeading ? 16 : 19
        );

        Font bodyFont = new Font(
            "SansSerif",
            Font.PLAIN,
            17
        );

        float maxWidth = Math.max(
            100f,
            Math.min(760f, getWidth() - 64f)
        );

        List<TextLayout> headingLines = wrapText(
            scene.heading,
            headingFont,
            scene.italicHeading,
            maxWidth,
            g2.getFontRenderContext()
        );

        List<TextLayout> bodyLines = wrapText(
            scene.body,
            bodyFont,
            scene.italicBody,
            maxWidth,
            g2.getFontRenderContext()
        );

        float gap = hasHeading ? 22f : 0f;

        float totalHeight =
            blockHeight(headingLines)
            + gap
            + blockHeight(bodyLines);

        float y =
            (getHeight() - totalHeight) / 2f;

        y = drawCentered(
            g2,
            headingLines,
            y,
            scene.goldHeading
                ? new Color(212, 180, 128)
                : new Color(244, 244, 244)
        );

        y += gap;

        drawCentered(
            g2,
            bodyLines,
            y,
            new Color(239, 239, 242)
        );

        g2.dispose();
    }

    public static class DialoguePanel extends JPanel {

        private static final String[] SCIENTISTS = {
            "DR. SEBASTIAN",
            "DR. DUTCH",
            "DR. NICOLE",
            "DR. GENE",
            "DR. ELENA"
        };

        private static final String[] PORTRAITS = {
            "dr. sebastian.png",
            "dr. dutch.png",
            "dr. nicole.png",
            "dr. gene.png",
            "dr. elena.png"
        };

        private final int scientistIndex;
        private final String[][] lines;
        private final Runnable onFinished;

        private final Image scientistPortrait;
        private final Image labControlPortrait;

        private int lineIndex = 0;
        private boolean scientistSpeaking;

        private final JLabel speakerLabel = new JLabel();
        private final JTextPane dialogueText = new JTextPane();
        private final JButton nextButton = new JButton("NEXT >");

        public DialoguePanel(
                int scientistIndex,
                String[][] lines,
                Runnable onFinished) {

            this.scientistIndex = Math.max(
                0,
                Math.min(scientistIndex, SCIENTISTS.length - 1)
            );

            this.lines = lines;
            this.onFinished = onFinished;

            setBackground(new Color(16, 12, 32));
            setLayout(null);
            setFocusable(true);

            scientistPortrait = loadPortrait(
                "assets/portraits/" + PORTRAITS[this.scientistIndex],
                "assets/characters/portraits/" + PORTRAITS[this.scientistIndex]
            );

            labControlPortrait = loadPortrait(
                "assets/portraits/lab_control.png",
                "assets/characters/portraits/lab_control.png",
                "assets/portraits/lab_control.gif",
                "assets/characters/portraits/lab_control.gif"
            );

            speakerLabel.setForeground(
                new Color(244, 183, 255)
            );

            speakerLabel.setFont(
                new Font("SansSerif", Font.BOLD, 19)
            );

            add(speakerLabel);

            dialogueText.setEditable(false);
            dialogueText.setFocusable(false);
            dialogueText.setOpaque(false);

            dialogueText.setForeground(
                new Color(249, 245, 255)
            );

            dialogueText.setFont(
                new Font("SansSerif", Font.PLAIN, 18)
            );

            dialogueText.setBorder(null);
            dialogueText.setMargin(new Insets(0, 0, 0, 0));

            add(dialogueText);

            nextButton.setFocusPainted(false);
            nextButton.setContentAreaFilled(false);
            nextButton.setBorderPainted(false);
            nextButton.setOpaque(false);

            nextButton.setForeground(
                new Color(244, 183, 255)
            );

            nextButton.setFont(
                new Font("SansSerif", Font.BOLD, 15)
            );

            nextButton.setHorizontalAlignment(
                SwingConstants.RIGHT
            );

            nextButton.setMargin(new Insets(0, 0, 0, 0));

            nextButton.setCursor(
                Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
                )
            );

            nextButton.addActionListener(e -> nextLine());
            add(nextButton);

            getInputMap(
                WHEN_IN_FOCUSED_WINDOW
            ).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "nextLine"
            );

            getInputMap(
                WHEN_IN_FOCUSED_WINDOW
            ).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                "nextLine"
            );

            getActionMap().put(
                "nextLine",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        nextLine();
                    }
                }
            );

            updateLine();
        }

        private static Image loadPortrait(String... paths) {
            for (String path : paths) {
                File file = new File(path);

                if (!file.isFile()) {
                    continue;
                }

                try {
                    if (path.toLowerCase().endsWith(".png")) {
                        BufferedImage image = ImageIO.read(file);

                        if (image == null) {
                            continue;
                        }

                        if (image.getColorModel().hasAlpha()) {
                            int minX = image.getWidth();
                            int minY = image.getHeight();
                            int maxX = -1;
                            int maxY = -1;

                            for (int y = 0; y < image.getHeight(); y++) {
                                for (int x = 0; x < image.getWidth(); x++) {
                                    int alpha =
                                        (image.getRGB(x, y) >>> 24) & 0xff;

                                    if (alpha > 12) {
                                        minX = Math.min(minX, x);
                                        minY = Math.min(minY, y);
                                        maxX = Math.max(maxX, x);
                                        maxY = Math.max(maxY, y);
                                    }
                                }
                            }

                            if (maxX >= minX && maxY >= minY) {
                                return image.getSubimage(
                                    minX,
                                    minY,
                                    maxX - minX + 1,
                                    maxY - minY + 1
                                );
                            }
                        }

                        return image;
                    }

                    Image image =
                        new ImageIcon(file.getAbsolutePath()).getImage();

                    if (image.getWidth(null) > 0
                            && image.getHeight(null) > 0) {
                        return image;
                    }

                } catch (Exception ignored) {
                }
            }

            System.err.println(
                "Portrait not found. Checked: "
                + String.join(", ", paths)
            );

            return null;
        }

        private void updateLine() {
            scientistSpeaking =
                lines[lineIndex][0].equalsIgnoreCase("Scientist");

            speakerLabel.setText(
                scientistSpeaking
                    ? SCIENTISTS[scientistIndex]
                    : "LAB CONTROL"
            );

            speakerLabel.setHorizontalAlignment(
                scientistSpeaking
                    ? SwingConstants.RIGHT
                    : SwingConstants.LEFT
            );

            dialogueText.setText(lines[lineIndex][1]);

            StyledDocument doc =
                dialogueText.getStyledDocument();

            SimpleAttributeSet paragraph =
                new SimpleAttributeSet();

            StyleConstants.setAlignment(
                paragraph,
                scientistSpeaking
                    ? StyleConstants.ALIGN_RIGHT
                    : StyleConstants.ALIGN_LEFT
            );

            doc.setParagraphAttributes(
                0,
                doc.getLength(),
                paragraph,
                false
            );

            dialogueText.setCaretPosition(0);

            nextButton.setText(
                lineIndex == lines.length - 1
                    ? "DONE"
                    : "NEXT >"
            );

            revalidate();
            repaint();
        }

        private void nextLine() {
            if (lineIndex + 1 >= lines.length) {
                if (onFinished != null) {
                    onFinished.run();
                }
                return;
            }

            lineIndex++;
            updateLine();
        }

        private int dialogueBoxHeight() {
            return Math.min(
                225,
                Math.max(175, getHeight() / 3)
            );
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int h = getHeight();

            int boxHeight = dialogueBoxHeight();
            int boxY = h - boxHeight - 18;

            int left = 48;
            int right = Math.max(left + 100, w - 48);
            int contentWidth = Math.max(100, right - left);

            speakerLabel.setBounds(
                left,
                boxY + 20,
                contentWidth,
                28
            );

            dialogueText.setBounds(
                left,
                boxY + 62,
                contentWidth,
                Math.max(48, boxHeight - 117)
            );

            nextButton.setBounds(
                Math.max(left, w - 160),
                boxY + boxHeight - 43,
                112,
                29
            );
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            int w = getWidth();
            int h = getHeight();

            int boxHeight = dialogueBoxHeight();
            int boxY = h - boxHeight - 18;

            Image activePortrait =
                scientistSpeaking
                    ? scientistPortrait
                    : labControlPortrait;

            if (activePortrait != null) {
                drawHalfBody(
                    g2,
                    activePortrait,
                    scientistSpeaking,
                    boxY
                );
            }

            g2.setColor(
                new Color(56, 24, 91, 185)
            );

            g2.fillRoundRect(
                22,
                boxY,
                w - 44,
                boxHeight,
                16,
                16
            );

            g2.setStroke(new BasicStroke(2f));

            g2.setColor(
                new Color(187, 103, 238, 225)
            );

            g2.drawRoundRect(
                22,
                boxY,
                w - 44,
                boxHeight,
                16,
                16
            );

            g2.dispose();
        }

        private void drawHalfBody(
                Graphics2D g2,
                Image portrait,
                boolean onRight,
                int boxY) {

            int iw = portrait.getWidth(this);
            int ih = portrait.getHeight(this);

            if (iw <= 0 || ih <= 0) {
                return;
            }

            int targetHeight = (int) (
                getHeight() * (onRight ? 0.62 : 0.67)
            );

            int targetWidth = Math.max(
                1,
                Math.round(
                    iw * targetHeight / (float) ih
                )
            );

            int maxWidth = Math.max(
                120,
                (int) (getWidth() * 0.42)
            );

            if (targetWidth > maxWidth) {
                float scale =
                    maxWidth / (float) targetWidth;

                targetWidth = maxWidth;
                targetHeight =
                    Math.round(targetHeight * scale);
            }

            int edgeMargin = 18;
            int overlap = 55;

            int x = onRight
                ? getWidth() - targetWidth - edgeMargin
                : edgeMargin;

            int y =
                boxY + overlap - targetHeight;

            g2.drawImage(
                portrait,
                x,
                y,
                x + targetWidth,
                y + targetHeight,
                0,
                0,
                iw,
                ih,
                this
            );
        }
    }
}
