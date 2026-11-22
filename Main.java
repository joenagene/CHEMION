
import battlefield.level1.Level1GamePanel;
import battlefield.level1.SaveManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

public class Main extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);

    private Clip menuMusic;
    private Level1GamePanel currentLevel1;
    private boolean endingShown;
    private OutbreakIntro currentIntro;
    private OutbreakIntro.DialoguePanel currentDialogue;
    // Level 2 has its own cinematic intro and conversation, separate from Level 1.
    private OutbreakIntro2 currentIntro2;
    private OutbreakIntro2.DialoguePanel currentLevel2Dialogue;
    private JPanel level2EndScreen;
    private JPanel levelSelection;
    private boolean exploreFromLevelSelection;
    private final Set<String> introducedEnemies = new HashSet<>();
    private int selectedScientistIndex;

    private static final int IMAGE_WIDTH = 1648;
    private static final int IMAGE_HEIGHT = 928;

    public Main() {
        setTitle("CHEMION: The Last Cure");
        setSize(1100, 620);
        setMinimumSize(new Dimension(800, 450));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                confirmExitApplication();
            }
        });

        screens.add(createMainMenu(), "MainMenu");
        screens.add(new CharacterSelection(this), "CharacterSelection");
        screens.add(new GameInstructions(this), "Instructions");

        add(screens);
        showScreen("MainMenu");
    }

    // MAIN MENU MUSIC
    private void playMenuMusic() {
        if (menuMusic != null) return;

        try {
            File file = new File("assets/audio/main_menu.wav");

            try (AudioInputStream audio =
                    AudioSystem.getAudioInputStream(file)) {

                menuMusic = AudioSystem.getClip();
                menuMusic.open(audio);
                menuMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }

        } catch (Exception e) {
            System.out.println("Music error: " + e.getMessage());

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
            () -> { exploreFromLevelSelection = false; showScreen("CharacterSelection"); },

            this::showLevelSelection,

            () -> { exploreFromLevelSelection = false; showScreen("CharacterSelection"); },

            () -> showScreen("Instructions"),

            this::confirmExitApplication
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
                    getWidth(),
                    getHeight(),
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

        if (screenName.equals("MainMenu") || screenName.equals("LevelSelect")) {
            if (menuMusic == null) {
                playMenuMusic();
            }
        } else {
            stopMenuMusic();
        }
    }

    // START GAME -> INTRO -> CONVERSATION -> ACTUAL LEVEL 1
    // CharacterSelection sends the selected character index (0 to 4).
    private static final String[] SCIENTIST_SPRITE_KEYS = {
        "sebastian", "dutch", "nicole", "gene", "elena"
    };

    // A newly initialized Level 1 is auto-saved before the player moves.
    // Do not mistake that empty checkpoint for gameplay progress.
    // Still protect actual activity and any unlocked/completed levels.
    private static boolean hasMeaningfulSavedProgress() {
        Properties p = SaveManager.load();
        if ("true".equals(p.getProperty("level.1.completed"))
                || SaveManager.highestUnlockedLevel() > 1) return true;
        if (!"true".equals(p.getProperty("level1.started"))) return false;

        try {
            if (Integer.parseInt(p.getProperty("level1.wave", "1")) > 1
                    || Integer.parseInt(p.getProperty("level1.spawned", "0")) > 0
                    || Integer.parseInt(p.getProperty("level1.xp", "0")) > 0
                    || Integer.parseInt(p.getProperty("level1.coins", "10")) != 10
                    || Integer.parseInt(p.getProperty("level1.medkits", "2")) != 2
                    || Double.parseDouble(p.getProperty("level1.hp", "100")) < 100
                    || Math.abs(Double.parseDouble(p.getProperty("level1.px", "130")) - 130) > 0.01
                    || Math.abs(Double.parseDouble(p.getProperty("level1.py", "386")) - 386) > 0.01
                    || !p.getProperty("level1.enemies", "").isEmpty()
                    || !p.getProperty("level1.projectiles", "").isEmpty()
                    || p.getProperty("level1.seen", "").contains("true")
                    || p.getProperty("level1.recipes", "").contains("true")
                    || !p.getProperty("level1.vials", "0,0,0,0").equals("0,0,0,0")
                    || !p.getProperty("level1.stock", "7,11,3,2,2,0,2,1")
                        .equals("7,11,3,2,2,0,2,1")) {
                return true;
            }
        } catch (NumberFormatException ex) {
            // An unreadable save should not be silently discarded.
            return true;
        }
        return false;
    }

    public void startNewGame(int characterIndex) {
        if (characterIndex < 0 || characterIndex >= SCIENTIST_SPRITE_KEYS.length) {
            return;
        }

        // "Explore" on the level carousel replays Level 1 without deleting unlocks.
        // "New Game" is a fresh run and asks before replacing existing progress.
        if (!exploreFromLevelSelection && hasMeaningfulSavedProgress()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Start a NEW game? This will replace your saved run and reset all level unlocks.",
                "Replace Saved Game?", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        try {
            if (exploreFromLevelSelection) SaveManager.clearLevel1KeepUnlocks();
            else SaveManager.clear();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Could not prepare your game save:\n" + e.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        exploreFromLevelSelection = false;

        selectedScientistIndex = characterIndex;
        introducedEnemies.clear();
        endingShown = false;

        // Clean up any previous run without modifying other screens.
        if (currentIntro != null) {
            currentIntro.stop();
            screens.remove(currentIntro);
            currentIntro = null;
        }
        if (currentDialogue != null) {
            screens.remove(currentDialogue);
            currentDialogue = null;
        }
        if (currentLevel1 != null) {
            currentLevel1.disposeGame();
            screens.remove(currentLevel1);
            currentLevel1 = null;
        }
        clearLevel2Screens();

        // Preserve the original outbreak story and scientist conversation.
        showNarration(OutbreakIntro.opening(this::showScientistConversation));
    }

    // Create the game ONLY AFTER the intro and dialogue are complete.
    // Continue Game bypasses the intro and restores the saved battle.
    private void enterLevel1() { enterLevel1(false); }

    private void enterLevel1(boolean restore) {
        if (currentLevel1 != null) {
            currentLevel1.disposeGame();
            screens.remove(currentLevel1);
        }

        String scientistKey = resolveScientistSpriteKey(selectedScientistIndex);
        currentLevel1 = new Level1GamePanel(
            scientistKey,
            this::showLevel1Ending,
            this::returnToMainMenu
        );

        // Ctrl+M also opens the pause menu, rather than bypassing saving.
        currentLevel1.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK),
            "level1PauseMenu"
        );
        currentLevel1.getActionMap().put("level1PauseMenu", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (currentLevel1 != null) currentLevel1.openPauseMenu();
            }
        });

        if (restore) {
            if (!currentLevel1.loadProgress()) {
                JOptionPane.showMessageDialog(this,
                    "Saved Level 1 progress could not be restored. "
                    + "Your save has not been overwritten.",
                    "Load Failed", JOptionPane.WARNING_MESSAGE);
                currentLevel1.disposeGame();
                currentLevel1 = null;
                showLevelSelection();
                return;
            }
        } else {
            try {
                currentLevel1.saveProgress();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "The game started, but the initial save failed:\n" + e.getMessage(),
                    "Save Failed", JOptionPane.WARNING_MESSAGE);
            }
        }

        screens.add(currentLevel1, "Level1");
        showScreen("Level1");
        SwingUtilities.invokeLater(() -> {
            if (currentLevel1 != null) currentLevel1.requestFocusInWindow();
        });
    }

    private int findSavedScientistIndex() {
        String saved = SaveManager.savedScientist().toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
        for (int i = 0; i < SCIENTIST_SPRITE_KEYS.length; i++) {
            if (saved.contains(SCIENTIST_SPRITE_KEYS[i])) return i;
        }
        return -1;
    }

    private void continueLevel1() {
        if (!SaveManager.hasSavedLevel1()) {
            exploreFromLevelSelection = true;
            showScreen("CharacterSelection");
            return;
        }
        int index = findSavedScientistIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this,
                "Could not identify the scientist in the saved game.",
                "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selectedScientistIndex = index;
        introducedEnemies.clear();
        endingShown = false;
        enterLevel1(true);
    }

    private void openSelectedLevel(int level) {
        if (level > SaveManager.highestUnlockedLevel() || level < 1) return;
        if (level == 1) {
            if (SaveManager.hasSavedLevel1()) continueLevel1();
            else {
                exploreFromLevelSelection = true;
                showScreen("CharacterSelection");
            }
        } else if (level == 2) {
            // Level 2 can be selected only after Level 1 is completed.
            // Restore the scientist chosen in the saved Level 1 run.
            int savedIndex = findSavedScientistIndex();
            if (savedIndex >= 0) selectedScientistIndex = savedIndex;
            startLevel2Intro();
        } else {
            // Levels 3-5 still have no connected battlefield or intro.
            JOptionPane.showMessageDialog(this,
                "Level " + level + " is unlocked, but its gameplay is not connected yet.",
                "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showLevelSelection() {
        clearLevel2Screens();
        if (levelSelection != null) screens.remove(levelSelection);
        levelSelection = new ContinueGame(
            this::openSelectedLevel,
            () -> showScreen("MainMenu")
        );
        screens.add(levelSelection, "LevelSelect");
        screens.revalidate();
        screens.repaint();
        showScreen("LevelSelect");
        SwingUtilities.invokeLater(() -> levelSelection.requestFocusInWindow());
    }

    private void confirmExitApplication() {
        if (currentLevel1 != null) {
            String[] options = {"SAVE & EXIT", "EXIT WITHOUT SAVING", "CANCEL"};
            int choice = JOptionPane.showOptionDialog(this,
                "Do you want to save your Level 1 progress before closing CHEMION?",
                "Exit CHEMION", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                try { currentLevel1.saveProgress(); }
                catch (IOException e) {
                    JOptionPane.showMessageDialog(this,
                        "The game could not be saved:\n" + e.getMessage(),
                        "Save Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (choice != 1) return;
        } else {
            int choice = JOptionPane.showConfirmDialog(this,
                "Exit CHEMION?", "Exit Game", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        if (currentLevel1 != null) currentLevel1.disposeGame();
        if (currentIntro != null) currentIntro.stop();
        if (currentIntro2 != null) currentIntro2.stop();
        stopMenuMusic();
        dispose();
    }

    // Pass the selected scientist's actual sprite folder to the Level 1 panel.
    // Supports common folder names like 'nicole', 'dr_nicole', 'dr. nicole'.
    private String resolveScientistSpriteKey(int index) {
        String key = SCIENTIST_SPRITE_KEYS[index];
        File characters = new File("assets/characters");
        if (!characters.isDirectory()) characters = new File("../assets/characters");
        if (!characters.isDirectory()) {
            System.err.println("Character sprites folder missing: assets/characters");
            return key;
        }
        if (new File(characters, key).isDirectory()) return key;

        File[] folders = characters.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                String normalized = folder.getName().toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z0-9]", "");
                if (normalized.equals("dr" + key) || normalized.equals(key + "sprites")
                    || normalized.equals(key + "sprite")) {
                    return folder.getName();
                }
            }
        }
        System.err.println("No sprite folder found for " + key
            + ". Expected assets/characters/" + key + "/walk/*.png");
        return key;
    }

    private void returnToMainMenu() {
        clearLevel2Screens();
        if (currentIntro != null) {
            currentIntro.stop();
            screens.remove(currentIntro);
            currentIntro = null;
        }
        if (currentDialogue != null) {
            screens.remove(currentDialogue);
            currentDialogue = null;
        }
        if (currentLevel1 != null) {
            currentLevel1.disposeGame();
            screens.remove(currentLevel1);
            currentLevel1 = null;
        }
        screens.revalidate();
        screens.repaint();
        showScreen("MainMenu");
    }

    // SHOW CENTERED NARRATION
    private void showNarration(OutbreakIntro narration) {

        if (narration == null) return;

        if (currentIntro != null) {
            currentIntro.stop();
            screens.remove(currentIntro);
        }

        currentIntro = narration;

        screens.add(currentIntro, "StoryNarration");
        showScreen("StoryNarration");
        currentIntro.start();
    }

    // SCIENTIST AND LAB CONTROL CONVERSATION
    private void showScientistConversation() {

        showDialogue(
            OutbreakIntro.scientistConversation(),
            this::enterLevel1
        );
    }

    // SHOW DIALOGUE PANEL
    private void showDialogue(
            String[][] lines,
            Runnable afterDialogue) {

        if (currentDialogue != null) {
            screens.remove(currentDialogue);
        }

        currentDialogue = new OutbreakIntro.DialoguePanel(
            selectedScientistIndex,
            lines,
            afterDialogue
        );

        screens.add(currentDialogue, "StoryDialogue");
        showScreen("StoryDialogue");
        currentDialogue.requestFocusInWindow();
    }

    // FIRST ENCOUNTER WITH AN ENEMY
    public void showEnemyIntroduction(String enemyId) {

        if (enemyId == null ||
            introducedEnemies.contains(enemyId)) {
            return;
        }

        OutbreakIntro story = OutbreakIntro.enemy(
            enemyId,
            () -> showScreen("Level1")
        );

        if (story == null) return;

        introducedEnemies.add(enemyId);
        showNarration(story);
    }

    // LEVEL 1 BOSS DEFEAT ENDING
    // Finish the original Level 1 story and unlock scene BEFORE Level 2's intro.
    public void showLevel1Ending() {
        if (endingShown) return;
        endingShown = true;

        showDialogue(
            OutbreakIntro.level1EndingConversation(),
            () -> showNarration(
                OutbreakIntro.level2Unlocked(this::startLevel2Intro)
            )
        );
    }

    // LEVEL 2: black-screen mystery narration -> scientist/Lab Control dialogue.
    // This also runs when the player selects unlocked Level 2 in Continue Game.
    private void startLevel2Intro() {
        if (SaveManager.highestUnlockedLevel() < 2) {
            showLevelSelection();
            return;
        }

        // Level 2 must use the same scientist as the saved Level 1 run.
        int savedIndex = findSavedScientistIndex();
        if (savedIndex >= 0) selectedScientistIndex = savedIndex;

        clearLevel2Screens();
        if (currentIntro != null) {
            currentIntro.stop();
            screens.remove(currentIntro);
            currentIntro = null;
        }
        if (currentDialogue != null) {
            screens.remove(currentDialogue);
            currentDialogue = null;
        }
        if (currentLevel1 != null) {
            currentLevel1.disposeGame();
            screens.remove(currentLevel1);
            currentLevel1 = null;
        }

        currentIntro2 = OutbreakIntro2.opening(this::showLevel2Conversation);
        screens.add(currentIntro2, "Level2Narration");
        screens.revalidate();
        showScreen("Level2Narration");
        currentIntro2.start();
    }

    private void showLevel2Conversation() {
        if (currentIntro2 != null) {
            currentIntro2.stop();
            screens.remove(currentIntro2);
            currentIntro2 = null;
        }
        if (currentLevel2Dialogue != null) {
            screens.remove(currentLevel2Dialogue);
        }

        currentLevel2Dialogue = new OutbreakIntro2.DialoguePanel(
            selectedScientistIndex,
            OutbreakIntro2.scientistConversation(),
            this::showLevel2EndScreen
        );
        screens.add(currentLevel2Dialogue, "Level2Conversation");
        screens.revalidate();
        showScreen("Level2Conversation");
        SwingUtilities.invokeLater(() -> {
            if (currentLevel2Dialogue != null)
                currentLevel2Dialogue.requestFocusInWindow();
        });
    }

    // The Level 2 battlefield is not implemented in the supplied project yet.
    // End at an in-game screen instead of launching a missing class or white dialog.
    private void showLevel2EndScreen() {
        if (currentLevel2Dialogue != null) {
            screens.remove(currentLevel2Dialogue);
            currentLevel2Dialogue = null;
        }
        if (level2EndScreen != null) screens.remove(level2EndScreen);

        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(new Color(15, 11, 29));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("LEVEL 2: CONTAMINATED HOSPITAL");
        heading.setForeground(new Color(217, 167, 255));
        heading.setFont(new Font("Monospaced", Font.BOLD, 22));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel message = new JLabel("The mystery begins here. Level 2 gameplay is not connected yet.");
        message.setForeground(new Color(214, 207, 229));
        message.setFont(new Font("SansSerif", Font.PLAIN, 15));
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        PixelButton selectButton = new PixelButton("LEVEL SELECT");
        selectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectButton.setMaximumSize(new Dimension(290, 55));
        selectButton.setPreferredSize(new Dimension(290, 55));
        selectButton.addActionListener(e -> showLevelSelection());

        PixelButton menuButton = new PixelButton("MAIN MENU");
        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuButton.setMaximumSize(new Dimension(290, 55));
        menuButton.setPreferredSize(new Dimension(290, 55));
        menuButton.addActionListener(e -> returnToMainMenu());

        content.add(heading);
        content.add(Box.createVerticalStrut(24));
        content.add(message);
        content.add(Box.createVerticalStrut(32));
        content.add(selectButton);
        content.add(Box.createVerticalStrut(12));
        content.add(menuButton);
        page.add(content);

        level2EndScreen = page;
        screens.add(level2EndScreen, "Level2End");
        screens.revalidate();
        showScreen("Level2End");
    }

    private void clearLevel2Screens() {
        if (currentIntro2 != null) {
            currentIntro2.stop();
            screens.remove(currentIntro2);
            currentIntro2 = null;
        }
        if (currentLevel2Dialogue != null) {
            screens.remove(currentLevel2Dialogue);
            currentLevel2Dialogue = null;
        }
        if (level2EndScreen != null) {
            screens.remove(level2EndScreen);
            level2EndScreen = null;
        }
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
