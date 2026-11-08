
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
        "DR. SEBASTIAN", "DR. DUTCH", "DR. NICOLE",
        "DR. GENE", "DR. ELENA"
    };

    private final String[] roles = {
        "HEAD SCIENTIST", "CHEMIST", "RESEARCH SCIENTIST",
        "BIOLOGIST", "MEDICAL SCIENTIST"
    };

    private final String[][] profiles = {
        {
            "Leadership and Strategic Planning",
            "Intelligent, decisive, and calm under pressure",
            "Overthinks decisions and struggles to trust others",
            "Serious, disciplined, and protective",
            "We don't have the luxury of failure. Not when humanity is counting on us."
        },
        {
            "Chemical Formulation and Experimentation",
            "Creative, resourceful, and quick-thinking",
            "Impulsive and overly confident in his experiments",
            "Playful, sarcastic, and adventurous",
            "Relax. The last explosion was completely intentional. Mostly."
        },
        {
            "Scientific Investigation and Data Analysis",
            "Observant, analytical, and detail-oriented",
            "Gets distracted by mysteries and unanswered questions",
            "Curious, clever, and quietly competitive",
            "Something doesn't add up... and I won't stop until I find out what."
        },
        {
            "Mutation Analysis and Biological Research",
            "Patient, adaptable, and highly knowledgeable",
            "Hesitates when forced to make dangerous decisions",
            "Reserved, thoughtful, and mysterious",
            "Nature doesn't make mistakes. It makes things we don't understand."
        },
        {
            "Medical Treatment and Cure Development",
            "Determined, resilient, and highly skilled",
            "Short-tempered, stubborn, and struggles to accept help",
            "Grumpy, blunt, sarcastic, and secretly caring",
            "If you're not dying, stop wasting my time."
        }
    };

    private final String[] gifFiles = {
        "dr. sebastian.gif", "dr. dutch.gif",
        "dr. nicole.gif", "dr. gene.gif", "dr. elena.gif"
    };

    private final ImageIcon[] gifs = new ImageIcon[5];
    private final CharacterPreview mainPortrait = new CharacterPreview();
    private final PortraitButton[] portraitButtons = new PortraitButton[5];

    private final JPanel descriptionPanel;
    private final JLabel nameLabel = new JLabel();
    private final JLabel roleLabel = new JLabel();
    private final JTextPane profileArea = new JTextPane();

    private final PixelButton backButton = new PixelButton("BACK");
    private final PixelButton selectButton = new PixelButton("SELECT");
    private final PixelButton nextButton = new PixelButton("NEXT");
    private final PixelButton startGameButton = new PixelButton("START GAME");

    private int currentCharacter = 0;
    private int selectedCharacter = -1;

    private final Timer animationTimer;

    // DARK OVERLAY
    private final JPanel startOverlay = new JPanel(null) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(5, 3, 16, 215));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    };

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
                g2.fillRoundRect(3, 3, Math.max(0, w - 6),
                    Math.max(0, h - 6), 18, 18);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(3, 3, Math.max(0, w - 6),
                    Math.max(0, h - 6), 18, 18);
                g2.dispose();
            }
        };

        descriptionPanel.setOpaque(false);

        nameLabel.setForeground(TEXT);
        roleLabel.setForeground(GLOW);

        descriptionPanel.add(nameLabel);
        descriptionPanel.add(roleLabel);

        profileArea.setEditable(false);
        profileArea.setOpaque(false);
        profileArea.setFocusable(false);
        profileArea.setForeground(TEXT);
        profileArea.setBorder(null);
        profileArea.setMargin(new Insets(0, 0, 0, 0));
        profileArea.setFont(gameFont(15));

        descriptionPanel.add(profileArea);
        add(descriptionPanel);

        backButton.addActionListener(e -> goBack());

        nextButton.addActionListener(e -> {
            currentCharacter = (currentCharacter + 1) % 5;
            updateCharacter();
        });

        selectButton.addActionListener(e -> {
            selectedCharacter = currentCharacter;
            updateCharacter();
            showStartOverlay();
        });

        add(backButton);
        add(selectButton);
        add(nextButton);

        // OVERLAY AND START BUTTON
        startOverlay.setOpaque(false);
        startOverlay.setVisible(false);

        startOverlay.add(startGameButton);

        startOverlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                hideStartOverlay();
            }
        });

        startGameButton.addActionListener(e -> {
            if (selectedCharacter < 0) return;

            hideStartOverlay();
            stopCharacterMusic();

            if (main != null) {
                main.startNewGame(selectedCharacter);
            }
        });

        add(startOverlay);
        setComponentZOrder(startOverlay, 0);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back"
        );

        getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (startOverlay.isVisible()) {
                    hideStartOverlay();
                } else {
                    goBack();
                }
            }
        });

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
                if (button != null) button.repaint();
            }
        });

        updateCharacter();
    }

    private void showStartOverlay() {
        startOverlay.setBounds(0, 0, getWidth(), getHeight());
        updateStartButtonLayout();
        startOverlay.setVisible(true);
        startOverlay.repaint();
    }

    private void hideStartOverlay() {
        startOverlay.setVisible(false);
    }

    private void updateStartButtonLayout() {
        int w = getWidth();
        int h = getHeight();
        double sx = w / (double) REF_WIDTH;
        double sy = h / (double) REF_HEIGHT;
        double scale = Math.min(sx, sy);

        int buttonW = Math.max(180, (int) (520 * sx));
        int buttonH = Math.max(55, (int) (110 * sy));

        startGameButton.setBounds(
            (w - buttonW) / 2,
            (h - buttonH) / 2,
            buttonW,
            buttonH
        );

        startGameButton.setFont(gameFont((float) (23 * scale)));
    }

    private void goBack() {
        hideStartOverlay();
        stopCharacterMusic();

        if (main != null) {
            main.showScreen("MainMenu");
        }
    }

    private void playCharacterMusic() {
        if (characterMusic != null) {
            if (!characterMusic.isRunning()) {
                characterMusic.setFramePosition(0);
                characterMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }
            return;
        }

        try {
            File file = new File(
                "assets/audio/character_selection.wav"
            );

            if (!file.isFile()) return;

            try (AudioInputStream audio =
                    AudioSystem.getAudioInputStream(file)) {
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
        if (animationTimer != null) animationTimer.start();
    }

    @Override
    public void removeNotify() {
        stopCharacterMusic();
        if (animationTimer != null) animationTimer.stop();
        super.removeNotify();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            SwingUtilities.invokeLater(() -> {
                if (isShowing()) playCharacterMusic();
            });
        } else {
            stopCharacterMusic();
        }
    }

    private Font gameFont(float size) {
        return new Font(
            "Press Start 2P",
            Font.PLAIN,
            Math.max(11, Math.round(size))
        );
    }

    private void loadBackground() {
        try {
            File file = new File(
                "assets/backgrounds/character_selection_bg.png"
            );
            if (file.isFile()) background = ImageIO.read(file);
        } catch (Exception e) {
            System.out.println(
                "Background error: " + e.getMessage()
            );
        }
    }

    private void loadGifs() {
        for (int i = 0; i < gifFiles.length; i++) {
            File file = new File(
                "assets/characters/portraits/" + gifFiles[i]
            );

            if (!file.isFile()) {
                System.out.println("MISSING GIF: " + gifFiles[i]);
                continue;
            }

            ImageIcon icon = new ImageIcon(file.getAbsolutePath());

            if (icon.getIconWidth() > 0 &&
                icon.getIconHeight() > 0) {
                gifs[i] = icon;
            }
        }
    }

    private void appendProfileText(
            StyledDocument doc, String text, AttributeSet style) {
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            System.out.println(e.getMessage());
        }
    }

    private void updateCharacter() {
        nameLabel.setText(names[currentCharacter]);
        roleLabel.setText(roles[currentCharacter]);

        StyledDocument doc = profileArea.getStyledDocument();

        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }

        int normalSize = Math.max(
            11, profileArea.getFont().getSize()
        );

        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setFontFamily(normal, "Press Start 2P");
        StyleConstants.setFontSize(normal, normalSize);
        StyleConstants.setForeground(normal, TEXT);

        SimpleAttributeSet quote = new SimpleAttributeSet();
        StyleConstants.setFontFamily(quote, "Press Start 2P");
        StyleConstants.setFontSize(quote, normalSize + 2);
        StyleConstants.setForeground(quote, GLOW);
        StyleConstants.setItalic(quote, true);

        String[] p = profiles[currentCharacter];

        appendProfileText(doc, "TALENT: " + p[0] + "\n\n", normal);
        appendProfileText(doc, "STRENGTH: " + p[1] + "\n\n", normal);
        appendProfileText(doc, "WEAKNESS: " + p[2] + "\n\n", normal);
        appendProfileText(doc, "PERSONALITY: " + p[3] + "\n\n", normal);
        appendProfileText(doc, "\"" + p[4] + "\"", quote);

        profileArea.setCaretPosition(0);
        mainPortrait.setCharacterIcon(gifs[currentCharacter]);

        mainPortrait.setCharacterAdjustment(
            currentCharacter == 0 ? 1.06 : 1.0
        );

        for (int i = 0; i < portraitButtons.length; i++) {
            portraitButtons[i].setSelectedGlow(i == currentCharacter);
        }

        boolean selected = selectedCharacter == currentCharacter;
        selectButton.setText(selected ? "SELECTED" : "SELECT");
        selectButton.setSelectedGlow(selected);

        repaint();
    }

    private void updateLayout() {
        int w = getWidth();
        int h = getHeight();

        if (w <= 0 || h <= 0) return;

        double sx = w / (double) REF_WIDTH;
        double sy = h / (double) REF_HEIGHT;
        double scale = Math.min(sx, sy);

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

        mainPortrait.setBounds(
            (int) (340 * sx),
            (int) (95 * sy),
            (int) (545 * sx),
            (int) (675 * sy)
        );

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

        int textX = (int) (40 * sx);
        int textY = (int) (160 * sy);
        int textW = panelW - (int) (80 * sx);
        int textH = panelH - textY - (int) (20 * sy);

        profileArea.setBounds(
            textX, textY, textW, Math.max(1, textH)
        );

        int navW = (int) (270 * sx);
        int navH = (int) (75 * sy);
        int navY = h - (int) (105 * sy);

        backButton.setBounds(
            (int) (45 * sx), navY, navW, navH
        );

        nextButton.setBounds(
            w - navW - (int) (45 * sx),
            navY, navW, navH
        );

        int selectW = (int) (350 * sx);
        int selectH = (int) (100 * sy);

        selectButton.setBounds(
            (w - selectW) / 2,
            navY - (selectH - navH),
            selectW,
            selectH
        );

        backButton.setFont(gameFont((float) (17 * scale)));
        nextButton.setFont(gameFont((float) (17 * scale)));
        selectButton.setFont(gameFont((float) (20 * scale)));
        nameLabel.setFont(gameFont((float) (24 * scale)));
        roleLabel.setFont(gameFont((float) (18 * scale)));
        profileArea.setFont(gameFont((float) (15 * scale)));

        updateCharacter();

        startOverlay.setBounds(0, 0, w, h);
        updateStartButtonLayout();

        revalidate();
        repaint();
    }

    public int getSelectedCharacter() {
        return selectedCharacter;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (background != null) {
            g.drawImage(
                background, 0, 0,
                getWidth(), getHeight(), this
            );
        }
    }

    private static void drawAnimatedGif(
            Graphics2D g, Component observer,
            ImageIcon icon, int x, int y,
            int width, int height) {

        if (icon == null || width <= 0 || height <= 0) return;

        int sw = icon.getIconWidth();
        int sh = icon.getIconHeight();

        if (sw <= 0 || sh <= 0) return;

        double scale = Math.min(
            width / (double) sw,
            height / (double) sh
        );

        double dw = sw * scale;
        double dh = sh * scale;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        g2.translate(
            x + (width - dw) / 2.0,
            y + (height - dh) / 2.0
        );
        g2.scale(scale, scale);
        icon.paintIcon(observer, g2, 0, 0);
        g2.dispose();
    }

    private static class CharacterPreview extends JComponent {
        private ImageIcon characterIcon;
        private double characterScale = 1.0;

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

            int sw = characterIcon.getIconWidth();
            int sh = characterIcon.getIconHeight();
            if (sw <= 0 || sh <= 0) return;

            double maxW = getWidth() - 8.0;
            double maxH = getHeight() - 8.0;

            double scale = Math.min(maxW / sw, maxH / sh);
            scale = Math.min(scale * characterScale, scale);

            double dw = sw * scale;
            double dh = sh * scale;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );

            g2.translate(
                (getWidth() - dw) / 2.0,
                Math.max(4, getHeight() - dh - 4)
            );
            g2.scale(scale, scale);
            characterIcon.paintIcon(this, g2, 0, 0);
            g2.dispose();
        }
    }

    private static class PortraitButton extends JButton {
        private ImageIcon characterIcon;
        private boolean selectedGlow;

        PortraitButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            ));
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

            int x = 5, y = 5;
            int w = getWidth() - 11;
            int h = getHeight() - 11;

            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            int cut = Math.min(14, Math.max(3, h / 5));
            Polygon shape = createPolygon(x, y, w, h, cut);

            boolean active = selectedGlow ||
                getModel().isRollover() ||
                getModel().isPressed();

            if (active) {
                for (int i = 8; i >= 1; i--) {
                    g2.setStroke(new BasicStroke(i * 2f));
                    g2.setColor(new Color(195, 90, 255, 12));
                    g2.drawPolygon(shape);
                }
            }

            g2.setColor(active ?
                new Color(70, 40, 100) : DARK);
            g2.fillPolygon(shape);

            g2.setStroke(new BasicStroke(active ? 3f : 2f));
            g2.setColor(active ? GLOW : BORDER);
            g2.drawPolygon(shape);

            if (characterIcon != null) {
                Shape oldClip = g2.getClip();
                g2.clip(shape);

                int padding = Math.max(6, h / 12);
                drawAnimatedGif(
                    g2, this, characterIcon,
                    x + padding, y + padding,
                    Math.max(1, w - padding * 2),
                    Math.max(1, h - padding * 2)
                );

                g2.setClip(oldClip);
            }

            if (w > 12 && h > 12) {
                Polygon inner = createPolygon(
                    x + 4, y + 4, w - 8, h - 8,
                    Math.max(2, cut - 3)
                );
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(113, 80, 148));
                g2.drawPolygon(inner);
            }

            g2.dispose();
        }
    }

    private static class PixelButton extends JButton {
        private boolean selectedGlow;

        PixelButton(String text) {
            super(text);
            setForeground(TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            ));
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

            int x = 5, y = 5;
            int w = getWidth() - 11;
            int h = getHeight() - 11;

            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            int cut = Math.min(14, Math.max(3, h / 5));
            Polygon shape = createPolygon(x, y, w, h, cut);

            boolean active = selectedGlow ||
                getModel().isRollover() ||
                getModel().isPressed();

            if (active) {
                for (int i = 8; i >= 1; i--) {
                    g2.setStroke(new BasicStroke(i * 2f));
                    g2.setColor(new Color(195, 90, 255, 12));
                    g2.drawPolygon(shape);
                }
            }

            g2.setColor(active ?
                new Color(70, 40, 100) : DARK);
            g2.fillPolygon(shape);

            g2.setStroke(new BasicStroke(active ? 3f : 2f));
            g2.setColor(active ? GLOW : BORDER);
            g2.drawPolygon(shape);

            if (w > 12 && h > 12) {
                Polygon inner = createPolygon(
                    x + 4, y + 4, w - 8, h - 8,
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

                int textX = (getWidth() -
                    fm.stringWidth(label)) / 2;
                int textY = (getHeight() -
                    fm.getHeight()) / 2 + fm.getAscent();

                g2.setColor(active ? Color.WHITE : TEXT);
                g2.drawString(label, textX, textY);
            }

            g2.dispose();
        }
    }

    private static Polygon createPolygon(
            int x, int y, int w, int h, int cut) {

        int[] xs = {
            x + cut, x + w - cut, x + w, x + w,
            x + w - cut, x + cut, x, x
        };

        int[] ys = {
            y, y, y + cut, y + h - cut,
            y + h, y + h, y + h - cut, y + cut
        };

        return new Polygon(xs, ys, 8);
    }
}
