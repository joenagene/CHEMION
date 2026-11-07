
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.text.*;

public class CharacterSelection extends JPanel {

    private static final int REF_WIDTH = 1648;
    private static final int REF_HEIGHT = 928;

    private static final Color TEXT = new Color(235, 212, 255);
    private static final Color BORDER = new Color(176, 109, 230);
    private static final Color GLOW = new Color(235, 150, 255);
    private static final Color DARK = new Color(35, 29, 55);

    private final Main main;
    private BufferedImage background;
    private Clip characterMusic;

    private final String[] names = {
        "DR. SEBASTIAN",
        "DR. DUTCH",
        "DR. NICOLE",
        "DR. GENE",
        "DR. ELENA"
    };

    private final String[] roles = {
        "HEAD SCIENTIST",
        "CHEMIST",
        "RESEARCH SCIENTIST",
        "BIOLOGIST",
        "MEDICAL SCIENTIST"
    };

    private final String[][] profiles = {

        // DR. SEBASTIAN
        {
            "Leadership and Strategic Planning",
            "Intelligent, decisive, and calm under pressure",
            "Overthinks decisions and struggles to trust others",
            "Serious, disciplined, and protective",
            "We don't have the luxury of failure. Not when humanity is counting on us."
        },

        // DR. DUTCH
        {
            "Chemical Formulation and Experimentation",
            "Creative, resourceful, and quick-thinking",
            "Impulsive and overly confident in his experiments",
            "Playful, sarcastic, and adventurous",
            "Relax. The last explosion was completely intentional. Mostly."
        },

        // DR. NICOLE
        {
            "Scientific Investigation and Data Analysis",
            "Observant, analytical, and detail-oriented",
            "Gets distracted by mysteries and unanswered questions",
            "Curious, clever, and quietly competitive",
            "Something doesn't add up... and I won't stop until I find out what."
        },

        // DR. GENE
        {
            "Mutation Analysis and Biological Research",
            "Patient, adaptable, and highly knowledgeable",
            "Hesitates when forced to make dangerous decisions",
            "Reserved, thoughtful, and mysterious",
            "Nature doesn't make mistakes. It makes things we don't understand."
        },

        // DR. ELENA
        {
            "Medical Treatment and Cure Development",
            "Determined, resilient, and highly skilled",
            "Short-tempered, stubborn, and struggles to accept help",
            "Grumpy, blunt, sarcastic, and secretly caring",
            "If you're not dying, stop wasting my time."
        }
    };

    private final String[] gifFiles = {
        "dr. sebastian.gif",
        "dr. dutch.gif",
        "dr. nicole.gif",
        "dr. gene.gif",
        "dr. elena.gif"
    };

    private final ImageIcon[] gifs = new ImageIcon[5];

    private final CharacterPreview mainPortrait =
        new CharacterPreview();

    private final PortraitButton[] portraitButtons =
        new PortraitButton[5];

    private final JPanel descriptionPanel;

    private final JLabel nameLabel = new JLabel();
    private final JLabel roleLabel = new JLabel();

    // JTextPane allows different font sizes in one profile.
    private final JTextPane profileArea = new JTextPane();

    private final PixelButton backButton =
        new PixelButton("BACK");

    private final PixelButton selectButton =
        new PixelButton("SELECT");

    private final PixelButton nextButton =
        new PixelButton("NEXT");

    private int currentCharacter = 0;
    private int selectedCharacter = -1;

    private final Timer animationTimer;

    public CharacterSelection() {
        this(null);
    }

    public CharacterSelection(Main main) {
        this.main = main;

        setLayout(null);
        setBackground(new Color(20, 10, 38));
        setFocusable(true);

        loadBackground();
        loadGifs();

        mainPortrait.setOpaque(false);
        add(mainPortrait);

        // CHARACTER THUMBNAILS
        for (int i = 0; i < 5; i++) {
            final int index = i;

            PortraitButton button = new PortraitButton();
            button.setCharacterIcon(gifs[i]);

            button.addActionListener(e -> {
                currentCharacter = index;
                updateCharacter();
            });

            portraitButtons[i] = button;
            add(button);
        }

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

                g2.setColor(new Color(27, 17, 52, 235));

                g2.fillRoundRect(
                    3, 3,
                    Math.max(0, w - 6),
                    Math.max(0, h - 6),
                    18, 18
                );

                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(3f));

                g2.drawRoundRect(
                    3, 3,
                    Math.max(0, w - 6),
                    Math.max(0, h - 6),
                    18, 18
                );

                g2.dispose();
            }
        };

        descriptionPanel.setOpaque(false);

        nameLabel.setForeground(TEXT);
        descriptionPanel.add(nameLabel);

        roleLabel.setForeground(GLOW);
        descriptionPanel.add(roleLabel);

        // PROFILE TEXT PANE
        profileArea.setEditable(false);
        profileArea.setOpaque(false);
        profileArea.setFocusable(false);
        profileArea.setForeground(TEXT);
        profileArea.setBorder(null);
        profileArea.setMargin(new Insets(0, 0, 0, 0));

        descriptionPanel.add(profileArea);
        add(descriptionPanel);

        // BACK BUTTON
        backButton.addActionListener(e -> goBack());

        // NEXT BUTTON
        nextButton.addActionListener(e -> {
            currentCharacter = (currentCharacter + 1) % 5;
            updateCharacter();
        });

        // SELECT BUTTON
        selectButton.addActionListener(e -> {
            selectedCharacter = currentCharacter;
            updateCharacter();
        });

        add(backButton);
        add(selectButton);
        add(nextButton);

        // ESCAPE KEY
        getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW
        ).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            "back"
        );

        getActionMap().put(
            "back",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    goBack();
                }
            }
        );

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateLayout();
            }
        });

        animationTimer = new Timer(80, e -> {
            if (!isShowing()) return;

            mainPortrait.repaint();

            for (PortraitButton button : portraitButtons) {
                if (button != null) {
                    button.repaint();
                }
            }
        });

        updateCharacter();
    }

    // BACK TO MAIN MENU
    private void goBack() {
        stopCharacterMusic();

        if (main != null) {
            main.showScreen("MainMenu");
        }
    }

    // CHARACTER SELECTION MUSIC
    private void playCharacterMusic() {

        if (characterMusic != null) {
            if (!characterMusic.isRunning()) {
                characterMusic.setFramePosition(0);
                characterMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }
            return;
        }

        try {
            File musicFile = new File(
                "assets/audio/character_selection.wav"
            );

            if (!musicFile.isFile()) {
                System.out.println(
                    "Character music not found: "
                    + musicFile.getAbsolutePath()
                );
                return;
            }

            try (AudioInputStream audio =
                    AudioSystem.getAudioInputStream(musicFile)) {

                characterMusic = AudioSystem.getClip();
                characterMusic.open(audio);
                characterMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }

        } catch (Exception e) {
            System.out.println(
                "Character music error: " + e.getMessage()
            );

            stopCharacterMusic();
        }
    }

    private void stopCharacterMusic() {
        if (characterMusic != null) {
            characterMusic.stop();
            characterMusic.close();
            characterMusic = null;
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();

        if (animationTimer != null) {
            animationTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        stopCharacterMusic();

        if (animationTimer != null) {
            animationTimer.stop();
        }

        super.removeNotify();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            SwingUtilities.invokeLater(() -> {
                if (isShowing()) {
                    playCharacterMusic();
                }
            });
        } else {
            stopCharacterMusic();
        }
    }

    // PIXEL FONT
    private Font gameFont(float size) {
        return new Font(
            "Press Start 2P",
            Font.PLAIN,
            Math.max(11, Math.round(size))
        );
    }

    // BACKGROUND IMAGE
    private void loadBackground() {
        try {
            File file = new File(
                "assets/backgrounds/character_selection_bg.png"
            );

            if (file.isFile()) {
                background = ImageIO.read(file);
            } else {
                System.out.println(
                    "Background not found: "
                    + file.getAbsolutePath()
                );
            }

        } catch (Exception e) {
            System.out.println(
                "Background error: " + e.getMessage()
            );
        }
    }

    // CHARACTER GIFS
    private void loadGifs() {
        for (int i = 0; i < gifFiles.length; i++) {

            File file = new File(
                "assets/characters/portraits/" + gifFiles[i]
            );

            if (!file.isFile()) {
                System.out.println(
                    "MISSING GIF: " + gifFiles[i]
                );
                continue;
            }

            ImageIcon icon = new ImageIcon(
                file.getAbsolutePath()
            );

            if (icon.getIconWidth() <= 0 ||
                icon.getIconHeight() <= 0) {

                System.out.println(
                    "INVALID GIF: " + gifFiles[i]
                );
                continue;
            }

            gifs[i] = icon;
        }
    }

    // ADD STYLED TEXT TO PROFILE
    private void appendProfileText(
            StyledDocument document,
            String text,
            AttributeSet style) {

        try {
            document.insertString(
                document.getLength(),
                text,
                style
            );
        } catch (BadLocationException e) {
            System.out.println(
                "Profile text error: " + e.getMessage()
            );
        }
    }

    // UPDATE CHARACTER PROFILE
    private void updateCharacter() {

        nameLabel.setText(names[currentCharacter]);
        roleLabel.setText(roles[currentCharacter]);

        String[] profile = profiles[currentCharacter];

        StyledDocument document =
            profileArea.getStyledDocument();

        try {
            document.remove(0, document.getLength());
        } catch (BadLocationException e) {
            System.out.println(
                "Profile clear error: " + e.getMessage()
            );
        }

        // NORMAL PROFILE FONT
        SimpleAttributeSet normalStyle =
            new SimpleAttributeSet();

        StyleConstants.setFontFamily(
            normalStyle, "Press Start 2P"
        );

        StyleConstants.setFontSize(
            normalStyle,
            Math.max(11, profileArea.getFont().getSize())
        );

        StyleConstants.setForeground(
            normalStyle, TEXT
        );

        // SLIGHTLY BIGGER QUOTE FONT
        SimpleAttributeSet quoteStyle =
            new SimpleAttributeSet();

        StyleConstants.setFontFamily(
            quoteStyle, "Press Start 2P"
        );

        StyleConstants.setFontSize(
            quoteStyle,
            Math.max(13, profileArea.getFont().getSize() + 2)
        );

        StyleConstants.setForeground(
            quoteStyle, GLOW
        );

        StyleConstants.setItalic(
            quoteStyle, true
        );

        // TALENT
        appendProfileText(
            document,
            "TALENT: " + profile[0] + "\n\n",
            normalStyle
        );

        // STRENGTH
        appendProfileText(
            document,
            "STRENGTH: " + profile[1] + "\n\n",
            normalStyle
        );

        // WEAKNESS
        appendProfileText(
            document,
            "WEAKNESS: " + profile[2] + "\n\n",
            normalStyle
        );

        // PERSONALITY
        appendProfileText(
            document,
            "PERSONALITY: " + profile[3] + "\n\n",
            normalStyle
        );

        // SIGNATURE QUOTE
        appendProfileText(
            document,
            "\"" + profile[4] + "\"",
            quoteStyle
        );

        profileArea.setCaretPosition(0);

        mainPortrait.setCharacterIcon(
            gifs[currentCharacter]
        );

        if (currentCharacter == 0) {
            mainPortrait.setCharacterAdjustment(1.06);
        } else {
            mainPortrait.setCharacterAdjustment(1.0);
        }

        for (int i = 0; i < 5; i++) {
            portraitButtons[i].setSelectedGlow(
                i == currentCharacter
            );
        }

        boolean selected =
            selectedCharacter == currentCharacter;

        selectButton.setText(
            selected ? "SELECTED" : "SELECT"
        );

        selectButton.setSelectedGlow(selected);

        repaint();
    }

    // ORIGINAL SCREEN LAYOUT
    private void updateLayout() {

        int w = getWidth();
        int h = getHeight();

        if (w <= 0 || h <= 0) return;

        double sx = w / (double) REF_WIDTH;
        double sy = h / (double) REF_HEIGHT;
        double scale = Math.min(sx, sy);

        // LEFT CHARACTER THUMBNAILS
        int buttonX = (int) (115 * sx);
        int buttonY = (int) (185 * sy);
        int buttonW = (int) (130 * sx);
        int buttonH = (int) (105 * sy);
        int gap = (int) (12 * sy);

        for (int i = 0; i < 5; i++) {
            portraitButtons[i].setBounds(
                buttonX,
                buttonY + i * (buttonH + gap),
                buttonW,
                buttonH
            );
        }

        // MAIN CHARACTER - UNCHANGED
        mainPortrait.setBounds(
            (int) (340 * sx),
            (int) (95 * sy),
            (int) (545 * sx),
            (int) (675 * sy)
        );

        // DESCRIPTION PANEL - UNCHANGED
        int panelW = (int) (650 * sx);
        int panelH = (int) (525 * sy);

        descriptionPanel.setBounds(
            (int) (850 * sx),
            (int) (215 * sy),
            panelW,
            panelH
        );

        nameLabel.setBounds(
            (int) (40 * sx),
            (int) (35 * sy),
            panelW - (int) (80 * sx),
            (int) (55 * sy)
        );

        roleLabel.setBounds(
            (int) (40 * sx),
            (int) (105 * sy),
            panelW - (int) (80 * sx),
            (int) (40 * sy)
        );

        // PROFILE AREA - SAME POSITION
        int textX = (int) (40 * sx);
        int textY = (int) (160 * sy);

        int textW = panelW - (int) (80 * sx);
        int textH = panelH - textY - (int) (20 * sy);

        profileArea.setBounds(
            textX,
            textY,
            textW,
            Math.max(1, textH)
        );

        // NAVIGATION BUTTONS - UNCHANGED
        int navW = (int) (270 * sx);
        int navH = (int) (75 * sy);
        int navY = h - (int) (105 * sy);

        backButton.setBounds(
            (int) (45 * sx),
            navY,
            navW,
            navH
        );

        nextButton.setBounds(
            w - navW - (int) (45 * sx),
            navY,
            navW,
            navH
        );

        int selectW = (int) (350 * sx);
        int selectH = (int) (100 * sy);

        selectButton.setBounds(
            (w - selectW) / 2,
            navY - (selectH - navH),
            selectW,
            selectH
        );

        // ORIGINAL BUTTON AND HEADING FONTS
        backButton.setFont(
            gameFont((float) (17 * scale))
        );

        nextButton.setFont(
            gameFont((float) (17 * scale))
        );

        selectButton.setFont(
            gameFont((float) (20 * scale))
        );

        nameLabel.setFont(
            gameFont((float) (24 * scale))
        );

        roleLabel.setFont(
            gameFont((float) (18 * scale))
        );

        // NORMAL PROFILE FONT SIZE
        profileArea.setFont(
            gameFont((float) (15 * scale))
        );

        // REFRESH STYLED TEXT AT CORRECT FONT SIZE
        updateProfileFontStyles();

        revalidate();
        repaint();
    }

    // UPDATE FONT SIZES WITHOUT CHANGING TEXT
    private void updateProfileFontStyles() {

        StyledDocument document =
            profileArea.getStyledDocument();

        int length = document.getLength();

        if (length == 0) return;

        int normalSize = profileArea.getFont().getSize();
        int quoteSize = normalSize + 2;

        SimpleAttributeSet normalStyle =
            new SimpleAttributeSet();

        StyleConstants.setFontFamily(
            normalStyle, "Press Start 2P"
        );

        StyleConstants.setFontSize(
            normalStyle, normalSize
        );

        StyleConstants.setForeground(
            normalStyle, TEXT
        );

        SimpleAttributeSet quoteStyle =
            new SimpleAttributeSet();

        StyleConstants.setFontFamily(
            quoteStyle, "Press Start 2P"
        );

        StyleConstants.setFontSize(
            quoteStyle, quoteSize
        );

        StyleConstants.setForeground(
            quoteStyle, GLOW
        );

        StyleConstants.setItalic(
            quoteStyle, true
        );

        String[] profile = profiles[currentCharacter];

        String quoteText = "\"" + profile[4] + "\"";

        String fullText = "";

        try {
            fullText = document.getText(0, length);
        } catch (BadLocationException e) {
            return;
        }

        int quoteStart = fullText.lastIndexOf(quoteText);

        document.setCharacterAttributes(
            0, length, normalStyle, true
        );

        if (quoteStart >= 0) {
            document.setCharacterAttributes(
                quoteStart,
                quoteText.length(),
                quoteStyle,
                true
            );
        }
    }

    public int getSelectedCharacter() {
        return selectedCharacter;
    }

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
                this
            );
        }
    }

    // ANIMATED GIF RENDERING
    private static void drawAnimatedGif(
            Graphics2D g,
            Component observer,
            ImageIcon icon,
            int x,
            int y,
            int width,
            int height) {

        if (icon == null) return;

        int sourceW = icon.getIconWidth();
        int sourceH = icon.getIconHeight();

        if (sourceW <= 0 ||
            sourceH <= 0 ||
            width <= 0 ||
            height <= 0) {
            return;
        }

        double scale = Math.min(
            width / (double) sourceW,
            height / (double) sourceH
        );

        double drawW = sourceW * scale;
        double drawH = sourceH * scale;

        double drawX =
            x + (width - drawW) / 2.0;

        double drawY =
            y + (height - drawH) / 2.0;

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        g2.translate(drawX, drawY);
        g2.scale(scale, scale);

        icon.paintIcon(observer, g2, 0, 0);

        g2.dispose();
    }

    // MAIN CHARACTER PREVIEW
    private static class CharacterPreview extends JComponent {

        private ImageIcon characterIcon;
        private double characterScale = 1.0;

        CharacterPreview() {
            setOpaque(false);
        }

        void setCharacterIcon(ImageIcon icon) {
            characterIcon = icon;
            repaint();
        }

        void setCharacterAdjustment(double scale) {
            characterScale = scale;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (characterIcon == null) return;

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );

            int sourceW = characterIcon.getIconWidth();
            int sourceH = characterIcon.getIconHeight();

            if (sourceW <= 0 || sourceH <= 0) {
                g2.dispose();
                return;
            }

            double maxW = getWidth() - 8.0;
            double maxH = getHeight() - 8.0;

            double baseScale = Math.min(
                maxW / sourceW,
                maxH / sourceH
            );

            double finalScale =
                baseScale * characterScale;

            finalScale = Math.min(
                finalScale,
                Math.min(
                    maxW / sourceW,
                    maxH / sourceH
                )
            );

            double drawW = sourceW * finalScale;
            double drawH = sourceH * finalScale;

            double drawX =
                (getWidth() - drawW) / 2.0;

            double drawY =
                getHeight() - drawH - 4.0;

            drawY = Math.max(4.0, drawY);

            g2.translate(drawX, drawY);
            g2.scale(finalScale, finalScale);

            characterIcon.paintIcon(this, g2, 0, 0);

            g2.dispose();
        }
    }

    // LEFT CHARACTER BUTTONS
    private static class PortraitButton extends JButton {

        private ImageIcon characterIcon;
        private boolean selectedGlow = false;

        PortraitButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);

            setCursor(
                Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
                )
            );
        }

        void setCharacterIcon(ImageIcon icon) {
            characterIcon = icon;
            repaint();
        }

        void setSelectedGlow(boolean value) {
            selectedGlow = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int x = 5;
            int y = 5;
            int w = getWidth() - 11;
            int h = getHeight() - 11;

            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            int cut = Math.min(
                14,
                Math.max(3, h / 5)
            );

            Polygon shape =
                createPolygon(x, y, w, h, cut);

            boolean active =
                selectedGlow ||
                getModel().isRollover() ||
                getModel().isPressed();

            if (active) {
                for (int i = 8; i >= 1; i--) {
                    g2.setStroke(
                        new BasicStroke(i * 2f)
                    );

                    g2.setColor(
                        new Color(195, 90, 255, 12)
                    );

                    g2.drawPolygon(shape);
                }
            }

            g2.setColor(
                active
                    ? new Color(70, 40, 100)
                    : DARK
            );

            g2.fillPolygon(shape);

            g2.setStroke(
                new BasicStroke(active ? 3f : 2f)
            );

            g2.setColor(
                active ? GLOW : BORDER
            );

            g2.drawPolygon(shape);

            if (characterIcon != null) {
                Shape oldClip = g2.getClip();
                g2.clip(shape);

                int padding = Math.max(6, h / 12);

                drawAnimatedGif(
                    g2,
                    this,
                    characterIcon,
                    x + padding,
                    y + padding,
                    Math.max(1, w - padding * 2),
                    Math.max(1, h - padding * 2)
                );

                g2.setClip(oldClip);
            }

            if (w > 12 && h > 12) {
                Polygon inner = createPolygon(
                    x + 4,
                    y + 4,
                    w - 8,
                    h - 8,
                    Math.max(2, cut - 3)
                );

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(113, 80, 148));
                g2.drawPolygon(inner);
            }

            g2.dispose();
        }
    }

    // BOTTOM PIXEL BUTTONS
    private static class PixelButton extends JButton {

        private boolean selectedGlow = false;

        PixelButton(String text) {
            super(text);

            setForeground(TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);

            setCursor(
                Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
                )
            );
        }

        void setSelectedGlow(boolean value) {
            selectedGlow = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int x = 5;
            int y = 5;
            int w = getWidth() - 11;
            int h = getHeight() - 11;

            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            int cut = Math.min(
                14,
                Math.max(3, h / 5)
            );

            Polygon shape =
                createPolygon(x, y, w, h, cut);

            boolean active =
                selectedGlow ||
                getModel().isRollover() ||
                getModel().isPressed();

            if (active) {
                for (int i = 8; i >= 1; i--) {
                    g2.setStroke(
                        new BasicStroke(i * 2f)
                    );

                    g2.setColor(
                        new Color(195, 90, 255, 12)
                    );

                    g2.drawPolygon(shape);
                }
            }

            g2.setColor(
                active
                    ? new Color(70, 40, 100)
                    : DARK
            );

            g2.fillPolygon(shape);

            g2.setStroke(
                new BasicStroke(active ? 3f : 2f)
            );

            g2.setColor(
                active ? GLOW : BORDER
            );

            g2.drawPolygon(shape);

            if (w > 12 && h > 12) {
                Polygon inner = createPolygon(
                    x + 4,
                    y + 4,
                    w - 8,
                    h - 8,
                    Math.max(2, cut - 3)
                );

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(113, 80, 148));
                g2.drawPolygon(inner);
            }

            String label = getText();

            if (label != null && !label.isEmpty()) {
                g2.setFont(getFont());

                FontMetrics fm = g2.getFontMetrics();

                int textX =
                    (getWidth() - fm.stringWidth(label)) / 2;

                int textY =
                    (getHeight() - fm.getHeight()) / 2
                    + fm.getAscent();

                g2.setColor(
                    active ? Color.WHITE : TEXT
                );

                g2.drawString(label, textX, textY);
            }

            g2.dispose();
        }
    }

    private static Polygon createPolygon(
            int x,
            int y,
            int w,
            int h,
            int cut) {

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
}
