
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.*;

public class GameInstructions extends JPanel {

    private final Main main;
    private final JTextPane instructions;
    private final Timer typingTimer;
    private final JButton back;
    private final JPanel textBox;
    private final JScrollPane scrollPane;

    private BufferedImage background;

    // TYPING SOUND
    private Clip typingSound;

    private static final int IMAGE_WIDTH = 1648;
    private static final int IMAGE_HEIGHT = 928;

    private static final Color NORMAL_COLOR =
        new Color(37, 32, 57);

    private static final Color HOVER_COLOR =
        new Color(76, 42, 106);

    private static final Color BORDER_COLOR =
        new Color(167, 101, 221);

    private static final String MISSION =
        "YOUR MISSION: FIND THE CURE AND SAVE HUMANITY!";

    private final String fullText =
        "HOW TO PLAY\n\n" +

        "EXPLORE AND SURVIVE\n\n" +
        "Explore different infected locations, complete missions, " +
        "and discover the secrets behind the zombie outbreak.\n\n" +

        "COLLECT AND ANALYZE\n\n" +
        "Gather chemical elements, ingredients, and research samples " +
        "throughout your journey. Analyze the infected to identify " +
        "their weaknesses and find possible ways to fight the infection.\n\n" +

        "CRAFT AND FIGHT\n\n" +
        "Combine chemical elements and ingredients in the laboratory " +
        "to create special potions. Use these potions strategically " +
        "to fight zombies, survive dangerous encounters, " +
        "and defeat powerful bosses.\n\n" +

        "COMPLETE AND PROGRESS\n\n" +
        "Complete objectives, earn experience points and rewards, " +
        "and unlock new locations. Discover new potion recipes " +
        "and prepare for stronger enemies as you advance through the game.\n\n" +

        "FIND THE FINAL CURE\n\n" +
        "Travel through all five levels, uncover the origin of the outbreak, " +
        "and defeat the final boss. Develop the ultimate cure to stop " +
        "the infection and save humanity.\n\n" +

        MISSION;

    private int charIndex = 0;

    public GameInstructions(Main main) {
        this.main = main;

        setLayout(null);
        setBackground(new Color(16, 20, 35));

        // LOAD TYPING SOUND
        try {
            File soundFile = new File("assets/audio/typing.wav");

            try (AudioInputStream audio =
                    AudioSystem.getAudioInputStream(soundFile)) {

                typingSound = AudioSystem.getClip();
                typingSound.open(audio);
            }

        } catch (Exception e) {
            System.out.println(
                "Typing sound error: " + e.getMessage()
            );
        }

        // BACKGROUND
        try {
            background = ImageIO.read(
                new File("assets/backgrounds/instructions_bg.png")
            );
        } catch (Exception e) {
            System.out.println(
                "Instructions background error: " + e.getMessage()
            );
        }

        // INSTRUCTIONS TEXT
        instructions = new JTextPane();
        instructions.setEditable(false);
        instructions.setForeground(new Color(210, 170, 255));
        instructions.setOpaque(false);
        instructions.setFont(
            new Font("Press Start 2P", Font.PLAIN, 12)
        );
        instructions.setMargin(new Insets(0, 0, 0, 0));
        instructions.setFocusable(false);

        StyledDocument doc = instructions.getStyledDocument();

        // CENTER ALIGNMENT AND LINE SPACING
        SimpleAttributeSet center = new SimpleAttributeSet();

        StyleConstants.setAlignment(
            center,
            StyleConstants.ALIGN_CENTER
        );

        StyleConstants.setLineSpacing(center, 0.2f);

        doc.setParagraphAttributes(
            0,
            doc.getLength(),
            center,
            false
        );

        // SCROLL PANE
        scrollPane = new JScrollPane(instructions);

        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        JScrollBar scrollbar = scrollPane.getVerticalScrollBar();

        scrollbar.setPreferredSize(new Dimension(7, 0));
        scrollbar.setUnitIncrement(12);
        scrollbar.setOpaque(false);

        // SCROLLBAR
        scrollbar.setUI(new BasicScrollBarUI() {

            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(115, 110, 125);
                trackColor = NORMAL_COLOR;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return invisibleButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return invisibleButton();
            }

            private JButton invisibleButton() {
                JButton button = new JButton();

                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));

                return button;
            }

            @Override
            protected void paintTrack(
                    Graphics g, JComponent c, Rectangle r) {
                // INVISIBLE TRACK
            }

            @Override
            protected void paintThumb(
                    Graphics g, JComponent c, Rectangle r) {

                if (r.isEmpty()) return;

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(new Color(115, 110, 125, 170));

                g2.fillRoundRect(
                    r.x + 1,
                    r.y,
                    Math.max(2, r.width - 2),
                    r.height,
                    6,
                    6
                );

                g2.dispose();
            }
        });

        //  TEXT BOX
        textBox = new JPanel(new BorderLayout()) {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                int margin = 3;
                int w = getWidth() - margin * 2 - 1;
                int h = getHeight() - margin * 2 - 1;

                if (w <= 0 || h <= 0) {
                    g2.dispose();
                    return;
                }

                int cut = Math.min(18, Math.max(3, h / 8));

                Polygon shape = createShape(
                    margin, margin, w, h, cut
                );

                g2.setColor(NORMAL_COLOR);
                g2.fillPolygon(shape);

                g2.setStroke(new BasicStroke(2f));
                g2.setColor(BORDER_COLOR);
                g2.drawPolygon(shape);

                if (w > 16 && h > 16) {
                    Polygon inner = createShape(
                        margin + 4,
                        margin + 4,
                        w - 8,
                        h - 8,
                        Math.max(2, cut - 3)
                    );

                    g2.setStroke(new BasicStroke(1f));
                    g2.setColor(new Color(100, 76, 135));
                    g2.drawPolygon(inner);
                }

                g2.dispose();
            }
        };

        textBox.setOpaque(false);

        textBox.setBorder(
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        );

        textBox.add(scrollPane, BorderLayout.CENTER);
        add(textBox);

        // BACK BUTTON
        back = new PixelButton("BACK TO MAIN MENU");
        add(back);

        // TYPEWRITER ANIMATION
        typingTimer = new Timer(35, e -> {

            if (charIndex < fullText.length()) {
                try {
                    String howToPlay = "HOW TO PLAY";

                    boolean isSpecial =
                        charIndex < howToPlay.length()
                        ||
                        charIndex >= fullText.length() - MISSION.length();

                    SimpleAttributeSet attributes =
                        new SimpleAttributeSet();

                    if (isSpecial) {
                        float specialSize = getSpecialFontSize();

                        StyleConstants.setFontFamily(
                            attributes, "Press Start 2P"
                        );

                        StyleConstants.setFontSize(
                            attributes, Math.round(specialSize)
                        );

                        StyleConstants.setBold(attributes, true);

                        StyleConstants.setForeground(
                            attributes, new Color(225, 140, 255)
                        );
                    } else {
                        StyleConstants.setFontFamily(
                            attributes, "Press Start 2P"
                        );

                        StyleConstants.setFontSize(
                            attributes, Math.round(getNormalFontSize())
                        );

                        StyleConstants.setForeground(
                            attributes, new Color(210, 170, 255)
                        );
                    }

                    doc.insertString(
                        doc.getLength(),
                        String.valueOf(fullText.charAt(charIndex)),
                        attributes
                    );

                    charIndex++;

                    // AUTO SCROLL WHILE TYPING
                    SwingUtilities.invokeLater(() -> {
                        JScrollBar bar =
                            scrollPane.getVerticalScrollBar();

                        bar.setValue(bar.getMaximum());
                    });

                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }

            } else {
                ((Timer) e.getSource()).stop();

                // STOP SOUND WHEN TYPING FINISHES
                stopTypingSound();
            }
        });

        // BACK BUTTON ACTION
        back.addActionListener(e -> {
            typingTimer.stop();

            // STOP TYPING SOUND
            stopTypingSound();

            main.showScreen("MainMenu");
        });

        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateLayout();
            }
        });
    }

    // PLAY TYPING SOUND CONTINUOUSLY
    private void playTypingSound() {
        if (typingSound != null && typingSound.isOpen()) {
            typingSound.stop();
            typingSound.setFramePosition(0);
            typingSound.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    // STOP TYPING SOUND
    private void stopTypingSound() {
        if (typingSound != null && typingSound.isOpen()) {
            typingSound.stop();
        }
    }

    // FONT SIZE
    private float getNormalFontSize() {
        double scaleX = getWidth() / (double) IMAGE_WIDTH;
        double scaleY = getHeight() / (double) IMAGE_HEIGHT;
        double scale = Math.min(scaleX, scaleY);

        return (float) Math.max(6, 19 * scale);
    }

    // FONT SIZE
    private float getSpecialFontSize() {
        return getNormalFontSize() + 2f;
    }

    // CUT-CORNER SHAPE
    private static Polygon createShape(
            int x, int y, int w, int h, int cut) {

        int[] xs = {
            x + cut,
            x + w - cut,
            x + w,
            x + w,
            x + w - cut,
            x + cut,
            x,
            x
        };

        int[] ys = {
            y,
            y,
            y + cut,
            y + h - cut,
            y + h,
            y + h,
            y + h - cut,
            y + cut
        };

        return new Polygon(xs, ys, 8);
    }

    // PIXEL BUTTON WITH PURPLE HOVER GLOW
    private static class PixelButton extends JButton {

        private static final int GLOW_MARGIN = 3;

        public PixelButton(String text) {
            super(text);

            setFont(
                new Font("Press Start 2P", Font.PLAIN, 11)
            );

            setForeground(BORDER_COLOR);

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);

            setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            setMargin(new Insets(0, 0, 0, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int x = GLOW_MARGIN;
            int y = GLOW_MARGIN;
            int w = getWidth() - GLOW_MARGIN * 2 - 1;
            int h = getHeight() - GLOW_MARGIN * 2 - 1;

            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            int cut = Math.min(12, Math.max(3, h / 5));

            Polygon shape = createShape(x, y, w, h, cut);
            boolean hover = getModel().isRollover();

            if (hover) {
                for (int i = 10; i >= 1; i--) {
                    g2.setStroke(new BasicStroke(
                        i * 2f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                    ));

                    g2.setColor(
                        new Color(190, 80, 255, 9)
                    );

                    g2.drawPolygon(shape);
                }
            }

            g2.setColor(
                hover ? HOVER_COLOR : NORMAL_COLOR
            );

            g2.fillPolygon(shape);

            g2.setStroke(
                new BasicStroke(hover ? 3f : 2f)
            );

            g2.setColor(
                hover
                    ? new Color(225, 140, 255)
                    : BORDER_COLOR
            );

            g2.drawPolygon(shape);

            if (w > 16 && h > 16) {
                Polygon inner = createShape(
                    x + 4,
                    y + 4,
                    w - 8,
                    h - 8,
                    Math.max(2, cut - 3)
                );

                g2.setStroke(new BasicStroke(1f));

                g2.setColor(
                    hover
                        ? new Color(190, 125, 245)
                        : new Color(100, 76, 135)
                );

                g2.drawPolygon(inner);
            }

            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public boolean contains(int x, int y) {

            int margin = GLOW_MARGIN;
            int w = getWidth() - margin * 2 - 1;
            int h = getHeight() - margin * 2 - 1;

            if (w <= 0 || h <= 0) return false;

            int cut = Math.min(12, Math.max(3, h / 5));

            return createShape(
                margin, margin, w, h, cut
            ).contains(x, y);
        }
    }

    
    private void updateLayout() {

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        double scaleX = width / (double) IMAGE_WIDTH;
        double scaleY = height / (double) IMAGE_HEIGHT;
        double scale = Math.min(scaleX, scaleY);

        // TEXT BOX POSITION AND SIZE
        int boxWidth = (int) (1050 * scaleX);
        int boxHeight = (int) (600 * scaleY);

        int boxX = (width - boxWidth) / 2;
        int boxY = (int) (150 * scaleY);

        textBox.setBounds(
            boxX,
            boxY,
            boxWidth,
            boxHeight
        );

        // NORMAL FONT
        float fontSize = getNormalFontSize();

        instructions.setFont(
            new Font("Press Start 2P", Font.PLAIN, 1)
                .deriveFont(fontSize)
        );

        
        StyledDocument doc = instructions.getStyledDocument();

        String currentText = instructions.getText();

        if (!currentText.isEmpty()) {
            SimpleAttributeSet normal = new SimpleAttributeSet();

            StyleConstants.setFontFamily(
                normal, "Press Start 2P"
            );

            StyleConstants.setFontSize(
                normal, Math.round(getNormalFontSize())
            );

            doc.setCharacterAttributes(
                0, doc.getLength(), normal, false
            );

            SimpleAttributeSet special = new SimpleAttributeSet();

            StyleConstants.setFontFamily(
                special, "Press Start 2P"
            );

            StyleConstants.setFontSize(
                special, Math.round(getSpecialFontSize())
            );

            StyleConstants.setBold(special, true);

            StyleConstants.setForeground(
                special, new Color(225, 140, 255)
            );

            int titleLength = Math.min(
                "HOW TO PLAY".length(),
                currentText.length()
            );

            doc.setCharacterAttributes(
                0, titleLength, special, false
            );

            int missionStart = currentText.indexOf(MISSION);

            if (missionStart >= 0) {
                doc.setCharacterAttributes(
                    missionStart,
                    MISSION.length(),
                    special,
                    false
                );
            }
        }

        // TEXT BOX PADDING
        int padding = Math.max(12, (int) (45 * scale));

        textBox.setBorder(
            BorderFactory.createEmptyBorder(
                padding,
                padding,
                padding,
                padding
            )
        );

        // BACK BUTTON
        int buttonWidth = (int) (550 * scaleX);
        int buttonHeight = (int) (65 * scaleY);

        int buttonX = (width - buttonWidth) / 2;

        int buttonY = height - buttonHeight
            - (int) (25 * scaleY);

        back.setBounds(
            buttonX,
            buttonY,
            buttonWidth,
            buttonHeight
        );

        float buttonFontSize =
            (float) Math.max(6, 15 * scale);

        back.setFont(
            new Font("Press Start 2P", Font.PLAIN, 1)
                .deriveFont(buttonFontSize)
        );

        revalidate();
        repaint();
    }

    // BACKGROUND
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (background != null) {
            g.drawImage(
                background,
                0,
                0,
                getWidth(),
                getHeight(),
                null
            );
        }
    }

    // TYPEWRITER EFFECT
    public void startTyping() {
        typingTimer.stop();

        // STOP PREVIOUS SOUND
        stopTypingSound();

        instructions.setText("");
        charIndex = 0;

        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
        });

        StyledDocument doc = instructions.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();

        StyleConstants.setAlignment(
            center,
            StyleConstants.ALIGN_CENTER
        );

        StyleConstants.setLineSpacing(center, 0.2f);

        doc.setParagraphAttributes(
            0,
            doc.getLength(),
            center,
            false
        );

        // START TYPING SOUND
        playTypingSound();

        typingTimer.start();
    }

    // STOP TYPEWRITER EFFECT
    public void stopTyping() {
        typingTimer.stop();
        stopTypingSound();
    }
}
