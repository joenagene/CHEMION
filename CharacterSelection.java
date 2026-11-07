
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public class CharacterSelection extends JPanel {

    private BufferedImage background;

    private static final int IMAGE_WIDTH = 1648;
    private static final int IMAGE_HEIGHT = 928;

    private static final Color PANEL_COLOR =
        new Color(27, 17, 52, 235);

    private static final Color NORMAL_COLOR =
        new Color(37, 32, 57);

    private static final Color HOVER_COLOR =
        new Color(76, 42, 106);

    private static final Color BORDER_COLOR =
        new Color(167, 101, 221);

    private static final Color GLOW_COLOR =
        new Color(225, 140, 255);

    private static final Color TEXT_COLOR =
        new Color(220, 185, 255);

    private static final int CHARACTER_COUNT = 5;

    private int currentCharacter = 0;
    private int selectedCharacter = -1;

    // CHARACTER NAMES
    private final String[] characterNames = {
        "DR. SEBASTIAN",
        "DR. NICOLE",
        "DR. GENE",
        "",
        ""
    };

    // CHARACTER ROLES
    private final String[] characterRoles = {
        "HEAD SCIENTIST",
        "CHEMIST",
        "RESEARCH SCIENTIST",
        "BIOLOGIST",
        "MEDICAL SCIENTIST"
    };

    // CHARACTER DESCRIPTIONS
    private final String[] characterDescriptions = {
        "A brilliant scientist skilled in research, experimentation, and solving complex scientific problems.",

        "An expert in chemical reactions, specializing in creating powerful potions and experimental compounds.",

        "A sharp-minded researcher skilled in analyzing samples, investigating mutations, and uncovering hidden discoveries.",

        "A knowledgeable biologist specializing in living organisms, mutations, and infected behavior.",

        "A skilled medical scientist specializing in disease analysis, experimental treatments, and developing cures."
    };

    // CHARACTER STATS - PLACEHOLDERS FOR NOW
    private final int[][] characterStats = {
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0}
    };

    private final JButton[] characterButtons =
        new JButton[CHARACTER_COUNT];

    private final PixelButton backButton =
        new PixelButton("BACK");

    private final PixelButton nextButton =
        new PixelButton("NEXT");

    private final PixelButton selectButton =
        new PixelButton("SELECT");

    private final JPanel descriptionPanel;

    private final JLabel nameLabel = new JLabel("");
    private final JLabel roleLabel = new JLabel("");

    private final JTextArea descriptionArea =
        new JTextArea();

    private final JLabel[] statLabels = {
        new JLabel("HEALTH"),
        new JLabel("ALCHEMY"),
        new JLabel("SPEED")
    };

    private final JLabel characterPlaceholder =
        new JLabel("", SwingConstants.CENTER);

    private final Main main;

    public CharacterSelection() {
        this(null);
    }

    public CharacterSelection(Main main) {
        this.main = main;

        setLayout(null);
        setBackground(new Color(16, 10, 35));

        // LOAD BACKGROUND
        try {
            background = ImageIO.read(
                new File(
                    "assets/backgrounds/character_selection_bg.png"
                )
            );
        } catch (Exception e) {
            System.out.println(
                "Character selection background error: "
                + e.getMessage()
            );
        }

        // CHARACTER PORTRAIT BUTTONS
        for (int i = 0; i < CHARACTER_COUNT; i++) {

            final int index = i;

            PixelButton portrait =
                new PixelButton(String.valueOf(i + 1));

            portrait.setSelectedGlow(i == currentCharacter);

            portrait.addActionListener(e -> {
                currentCharacter = index;
                updateCharacterDisplay();
            });

            characterButtons[i] = portrait;
            add(portrait);
        }

        // MAIN CHARACTER PLACEHOLDER
        characterPlaceholder.setFont(
            new Font("Press Start 2P", Font.BOLD, 18)
        );

        characterPlaceholder.setForeground(TEXT_COLOR);
        add(characterPlaceholder);

        // DESCRIPTION PANEL
        descriptionPanel = new JPanel(null) {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                int w = getWidth();
                int h = getHeight();

                g2.setColor(PANEL_COLOR);

                g2.fillRoundRect(
                    4, 4, w - 8, h - 8, 18, 18
                );

                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(3f));

                g2.drawRoundRect(
                    4, 4, w - 8, h - 8, 18, 18
                );

                g2.dispose();
            }
        };

        descriptionPanel.setOpaque(false);

        // NAME
        nameLabel.setFont(
            new Font("Press Start 2P", Font.BOLD, 15)
        );

        nameLabel.setForeground(TEXT_COLOR);
        descriptionPanel.add(nameLabel);

        // ROLE
        roleLabel.setFont(
            new Font("Press Start 2P", Font.PLAIN, 10)
        );

        roleLabel.setForeground(GLOW_COLOR);
        descriptionPanel.add(roleLabel);

        // DESCRIPTION
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFocusable(false);
        descriptionArea.setForeground(TEXT_COLOR);

        descriptionArea.setFont(
            new Font("Press Start 2P", Font.PLAIN, 9)
        );

        descriptionPanel.add(descriptionArea);

        // STATS
        for (JLabel stat : statLabels) {

            stat.setFont(
                new Font("Press Start 2P", Font.PLAIN, 10)
            );

            stat.setForeground(TEXT_COLOR);
            descriptionPanel.add(stat);
        }

        add(descriptionPanel);

        // BACK BUTTON
        backButton.addActionListener(e -> {

            if (main != null) {
                main.showScreen("MainMenu");
            } else {

                Container parent = getParent();

                while (parent != null &&
                       !(parent instanceof Main)) {
                    parent = parent.getParent();
                }

                if (parent instanceof Main) {
                    ((Main) parent).showScreen("MainMenu");
                }
            }
        });

        add(backButton);

        // NEXT BUTTON
        nextButton.addActionListener(e -> {

            currentCharacter =
                (currentCharacter + 1) % CHARACTER_COUNT;

            updateCharacterDisplay();
        });

        add(nextButton);

        // SELECT BUTTON
        selectButton.addActionListener(e -> {

            selectedCharacter = currentCharacter;
            updateCharacterDisplay();
        });

        add(selectButton);

        // RESPONSIVE LAYOUT
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                updateLayout();
            }
        });

        updateCharacterDisplay();
    }

    // UPDATE CHARACTER SELECTION
    private void updateCharacterDisplay() {

        nameLabel.setText(
            characterNames[currentCharacter]
        );

        roleLabel.setText(
            characterRoles[currentCharacter]
        );

        descriptionArea.setText(
            characterDescriptions[currentCharacter]
        );

        characterPlaceholder.setText(
            "CHARACTER " + (currentCharacter + 1)
        );

        for (int i = 0; i < CHARACTER_COUNT; i++) {

            if (characterButtons[i] instanceof PixelButton) {

                ((PixelButton) characterButtons[i])
                    .setSelectedGlow(i == currentCharacter);
            }
        }

        boolean isSelected =
            selectedCharacter == currentCharacter;

        // THE BUTTON ITSELF CHANGES TO SELECTED
        selectButton.setText(
            isSelected ? "SELECTED" : "SELECT"
        );

        selectButton.setSelectedGlow(isSelected);

        repaint();
    }

    // GET SELECTED CHARACTER FOR GAME SYSTEM
    public int getSelectedCharacter() {
        return selectedCharacter;
    }

    // RESPONSIVE LAYOUT
    private void updateLayout() {

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        double scaleX = width / (double) IMAGE_WIDTH;
        double scaleY = height / (double) IMAGE_HEIGHT;
        double scale = Math.min(scaleX, scaleY);

        // FIVE CHARACTER BUTTONS
        int portraitX = (int) (115 * scaleX);
        int portraitY = (int) (185 * scaleY);

        int portraitWidth = (int) (130 * scaleX);
        int portraitHeight = (int) (105 * scaleY);
        int portraitSpacing = (int) (12 * scaleY);

        for (int i = 0; i < CHARACTER_COUNT; i++) {

            characterButtons[i].setBounds(
                portraitX,
                portraitY
                    + i * (portraitHeight + portraitSpacing),
                portraitWidth,
                portraitHeight
            );

            characterButtons[i].setFont(
                new Font("Press Start 2P", Font.BOLD, 1)
                    .deriveFont(
                        (float) Math.max(7, 15 * scale)
                    )
            );
        }

        // CHARACTER DISPLAY
        characterPlaceholder.setBounds(
            (int) (300 * scaleX),
            (int) (250 * scaleY),
            (int) (460 * scaleX),
            (int) (370 * scaleY)
        );

        characterPlaceholder.setFont(
            new Font("Press Start 2P", Font.BOLD, 1)
                .deriveFont(
                    (float) Math.max(8, 18 * scale)
                )
        );

        // DESCRIPTION PANEL
        int panelX = (int) (850 * scaleX);
        int panelY = (int) (215 * scaleY);
        int panelW = (int) (650 * scaleX);
        int panelH = (int) (525 * scaleY);

        descriptionPanel.setBounds(
            panelX, panelY, panelW, panelH
        );

        nameLabel.setBounds(
            (int) (40 * scaleX),
            (int) (40 * scaleY),
            panelW - (int) (80 * scaleX),
            (int) (55 * scaleY)
        );

        roleLabel.setBounds(
            (int) (40 * scaleX),
            (int) (105 * scaleY),
            panelW - (int) (80 * scaleX),
            (int) (40 * scaleY)
        );

        descriptionArea.setBounds(
            (int) (40 * scaleX),
            (int) (175 * scaleY),
            panelW - (int) (80 * scaleX),
            (int) (185 * scaleY)
        );

        for (int i = 0; i < statLabels.length; i++) {

            statLabels[i].setBounds(
                (int) (45 * scaleX),
                (int) ((385 + i * 42) * scaleY),
                panelW - (int) (90 * scaleX),
                (int) (35 * scaleY)
            );
        }

        // BOTTOM BUTTONS
        int buttonWidth = (int) (270 * scaleX);
        int buttonHeight = (int) (75 * scaleY);
        int buttonY = height - (int) (105 * scaleY);

        backButton.setBounds(
            (int) (45 * scaleX),
            buttonY,
            buttonWidth,
            buttonHeight
        );

        int selectWidth = (int) (350 * scaleX);
        int selectHeight = (int) (100 * scaleY);

        selectButton.setBounds(
            (width - selectWidth) / 2,
            buttonY - (selectHeight - buttonHeight),
            selectWidth,
            selectHeight
        );

        nextButton.setBounds(
            width - buttonWidth - (int) (45 * scaleX),
            buttonY,
            buttonWidth,
            buttonHeight
        );

        float buttonFontSize =
            (float) Math.max(7, 16 * scale);

        backButton.setFont(
            new Font("Press Start 2P", Font.BOLD, 1)
                .deriveFont(buttonFontSize)
        );

        selectButton.setFont(
            new Font("Press Start 2P", Font.BOLD, 1)
                .deriveFont((float) Math.max(7, 22 * scale))
        );

        nextButton.setFont(
            new Font("Press Start 2P", Font.BOLD, 1)
                .deriveFont(buttonFontSize)
        );

        revalidate();
        repaint();
    }

    // DRAW BACKGROUND
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

    // PIXEL BUTTON WITH GLOW
    private static class PixelButton extends JButton {

        private boolean selectedGlow = false;

        private static final int GLOW_MARGIN = 5;

        public PixelButton(String text) {
            super(text);

            setForeground(TEXT_COLOR);

            setFont(
                new Font("Press Start 2P", Font.PLAIN, 11)
            );

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

        public void setSelectedGlow(boolean glow) {
            selectedGlow = glow;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int margin = GLOW_MARGIN;

            int x = margin;
            int y = margin;
            int w = getWidth() - margin * 2 - 1;
            int h = getHeight() - margin * 2 - 1;

            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            int cut = Math.min(14, Math.max(3, h / 5));

            Polygon shape = createShape(
                x, y, w, h, cut
            );

            boolean glow =
                getModel().isRollover()
                || getModel().isPressed()
                || selectedGlow;

            if (glow) {
                for (int i = 9; i >= 1; i--) {

                    g2.setStroke(
                        new BasicStroke(i * 2f)
                    );

                    g2.setColor(
                        new Color(190, 80, 255, 12)
                    );

                    g2.drawPolygon(shape);
                }
            }

            g2.setColor(
                glow ? HOVER_COLOR : NORMAL_COLOR
            );

            g2.fillPolygon(shape);

            g2.setStroke(
                new BasicStroke(glow ? 3f : 2f)
            );

            g2.setColor(
                glow ? GLOW_COLOR : BORDER_COLOR
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
                    glow
                        ? new Color(190, 125, 245)
                        : new Color(100, 76, 135)
                );

                g2.drawPolygon(inner);
            }

            g2.dispose();
            super.paintComponent(g);
        }

        private Polygon createShape(
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

        @Override
        public boolean contains(int x, int y) {

            int margin = GLOW_MARGIN;
            int w = getWidth() - margin * 2 - 1;
            int h = getHeight() - margin * 2 - 1;

            if (w <= 0 || h <= 0) return false;

            int cut = Math.min(14, Math.max(3, h / 5));

            return createShape(
                margin, margin, w, h, cut
            ).contains(x, y);
        }
    }
}
