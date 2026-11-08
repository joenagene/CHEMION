
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

public class Main extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);

    private Clip menuMusic;

    private static final int IMAGE_WIDTH = 1648;
    private static final int IMAGE_HEIGHT = 928;

    public Main() {
        setTitle("CHEMION: The Last Cure");
        setSize(1100, 620);
        setMinimumSize(new Dimension(800, 450));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        screens.add(createMainMenu(), "MainMenu");
        screens.add(
            new CharacterSelection(this),
            "CharacterSelection"
        );
        screens.add(
            new GameInstructions(this),
            "Instructions"
        );

        add(screens);
        showScreen("MainMenu");
    }

    // MAIN MENU MUSIC
    private void playMenuMusic() {
        if (menuMusic != null) return;

        try {
            File file = new File(
                "assets/audio/main_menu.wav"
            );

            try (AudioInputStream audio =
                    AudioSystem.getAudioInputStream(file)) {

                menuMusic = AudioSystem.getClip();
                menuMusic.open(audio);
                menuMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }

        } catch (Exception e) {
            System.out.println(
                "Music error: " + e.getMessage()
            );

            if (menuMusic != null) {
                menuMusic.close();
                menuMusic = null;
            }
        }
    }

    private void stopMenuMusic() {
        if (menuMusic != null) {
            menuMusic.stop();
            menuMusic.close();
            menuMusic = null;
        }
    }

    // PIXEL BUTTON
    private static class PixelButton extends JButton {

        private static final int GLOW_MARGIN = 3;

        private final Color normalColor =
            new Color(37, 32, 57);

        private final Color hoverColor =
            new Color(76, 42, 106);

        private final Color borderColor =
            new Color(167, 101, 221);

        PixelButton(String text) {
            super(text);

            setFont(new Font(
                "Press Start 2P", Font.PLAIN, 11
            ));

            setForeground(borderColor);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);

            setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            ));

            setMargin(new Insets(0, 0, 0, 0));
        }

        private Polygon createShape(
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
                hover ? hoverColor : normalColor
            );
            g2.fillPolygon(shape);

            g2.setStroke(new BasicStroke(
                hover ? 3f : 2f
            ));

            g2.setColor(
                hover
                    ? new Color(225, 140, 255)
                    : borderColor
            );

            g2.drawPolygon(shape);

            if (w > 16 && h > 16) {
                Polygon inner = createShape(
                    x + 4, y + 4,
                    w - 8, h - 8,
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

    // MAIN MENU
    private JPanel createMainMenu() {

        BackgroundPanel panel = new BackgroundPanel();
        panel.setLayout(null);

        String[] labels = {
            "NEW GAME",
            "CONTINUE GAME",
            "CHARACTER SELECTION",
            "GAME INTRUCTIONS",
            "EXIT GAME"
        };

        Runnable[] actions = {
            () -> showScreen("CharacterSelection"),

            () -> JOptionPane.showMessageDialog(
                this,
                "No saved game available yet."
            ),

            () -> showScreen("CharacterSelection"),

            () -> showScreen("Instructions"),

            () -> {
                int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to exit?",
                    "Exit Game",
                    JOptionPane.YES_NO_OPTION
                );

                if (answer == JOptionPane.YES_OPTION) {
                    stopMenuMusic();
                    dispose();
                }
            }
        };

        JButton[] buttons = new JButton[labels.length];

        for (int i = 0; i < labels.length; i++) {
            JButton button = new PixelButton(labels[i]);
            final int index = i;

            button.addActionListener(
                e -> actions[index].run()
            );

            buttons[i] = button;
            panel.add(button);
        }

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = panel.getWidth();
                int height = panel.getHeight();

                double scaleX =
                    width / (double) IMAGE_WIDTH;
                double scaleY =
                    height / (double) IMAGE_HEIGHT;

                int buttonWidth = (int) (410 * scaleX);
                int buttonHeight = (int) (78 * scaleY);
                int spacing = (int) (12 * scaleY);

                int centerX = (int) (835 * scaleX);
                int startY = (int) (445 * scaleY);

                for (int i = 0; i < buttons.length; i++) {
                    int x = centerX - buttonWidth / 2;
                    int y = startY +
                        i * (buttonHeight + spacing);

                    buttons[i].setBounds(
                        x, y, buttonWidth, buttonHeight
                    );
                }
            }
        });

        return panel;
    }

    // MAIN MENU BACKGROUND
    private static class BackgroundPanel extends JPanel {

        private BufferedImage background;

        BackgroundPanel() {
            try {
                background = ImageIO.read(
                    new File(
                        "assets/backgrounds/main_menu.png"
                    )
                );
            } catch (Exception e) {
                System.out.println(
                    "Background error: " + e.getMessage()
                );
            }

            setBackground(new Color(16, 20, 35));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (background != null) {
                g.drawImage(
                    background,
                    0, 0,
                    getWidth(), getHeight(),
                    null
                );
            }
        }
    }

    // SCREEN NAVIGATION
    public void showScreen(String screenName) {

        cardLayout.show(screens, screenName);

        if (screenName.equals("Instructions")) {
            for (Component component :
                    screens.getComponents()) {

                if (component instanceof GameInstructions) {
                    ((GameInstructions) component).startTyping();
                }
            }
        }

        if (screenName.equals("MainMenu")) {
            if (menuMusic == null) {
                playMenuMusic();
            }
        } else {
            stopMenuMusic();
        }
    }

    // START GAME -> TEMPORARY LEVEL 1
    public void startNewGame(int characterIndex) {

        String[] scientists = {
            "DR. SEBASTIAN",
            "DR. DUTCH",
            "DR. NICOLE",
            "DR. GENE",
            "DR. ELENA"
        };

        if (characterIndex < 0 ||
            characterIndex >= scientists.length) {
            return;
        }

        JPanel level1 = new JPanel(new GridBagLayout());
        level1.setBackground(new Color(16, 12, 32));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(
            new BoxLayout(content, BoxLayout.Y_AXIS)
        );

        JLabel title = new JLabel(
            "LEVEL 1 - ABANDONED TOWN"
        );

        title.setForeground(new Color(235, 150, 255));
        title.setFont(new Font(
            "Press Start 2P", Font.BOLD, 17
        ));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scientist = new JLabel(
            "Selected: " + scientists[characterIndex]
        );

        scientist.setForeground(new Color(235, 212, 255));
        scientist.setFont(new Font(
            "Press Start 2P", Font.PLAIN, 12
        ));
        scientist.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel note = new JLabel(
            "Battlefield is under development"
        );
        note.setForeground(Color.LIGHT_GRAY);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton back = new JButton("BACK TO MAIN MENU");
        back.setAlignmentX(Component.CENTER_ALIGNMENT);

        back.addActionListener(
            e -> showScreen("MainMenu")
        );

        content.add(title);
        content.add(Box.createVerticalStrut(24));
        content.add(scientist);
        content.add(Box.createVerticalStrut(20));
        content.add(note);
        content.add(Box.createVerticalStrut(28));
        content.add(back);

        level1.add(content);

        screens.add(level1, "Level1");
        showScreen("Level1");
    }

    public void addScreen(String name, JPanel panel) {
        screens.add(panel, name);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main game = new Main();
            game.setVisible(true);
        });
    }
}
