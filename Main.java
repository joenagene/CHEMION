
    import battlefield.level1.Level1GamePanel;
    import battlefield.level1.SaveManager;
    import battlefield.level2.Level2GamePanel;
    import battlefield.level3.Level3GamePanel;
    import battlefield.level4.Level4GamePanel;
    import battlefield.level5.Level5GamePanel;
    import game.AudioManager;
    import game.GameManager;
    import game.LevelTransition;
    import game.Scientist;
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
        private LevelTransition activeTransition;
        private Level1GamePanel currentLevel1;
        private Level2GamePanel currentLevel2;
        private Level3GamePanel currentLevel3;
        private Level4GamePanel currentLevel4;
        private Level5GamePanel currentLevel5;
        private OutbreakIntro5 currentIntro5;
        private OutbreakEnding currentEnding;
        private OutbreakIntro5.DialoguePanel currentLevel5Dialogue;
        private boolean level5EndingShown;
        private boolean level4EndingShown;
        private OutbreakIntro4 currentIntro4;
        private OutbreakIntro4.DialoguePanel currentLevel4Dialogue;
        private boolean level2EndingShown;
        private boolean endingShown;
        private OutbreakIntro currentIntro;
        private OutbreakIntro.DialoguePanel currentDialogue;
        // Level 2 has its own cinematic intro and conversation, separate from Level 1.
        private OutbreakIntro2 currentIntro2;
        private OutbreakIntro2.DialoguePanel currentLevel2Dialogue;
        private OutbreakIntro3 currentIntro3;
        private OutbreakIntro3.DialoguePanel currentLevel3Dialogue;
        private boolean level3EndingShown;
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
            AudioManager.installButtonClicks();
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

        boolean isLevel1Gameplay = screenName.equals("Level1");

        AudioManager.setAmbientEnabled(
            activeTransition == null &&
            !isLevel1Gameplay &&
            isStoryOrGameplayScreen(screenName)
        );

        if (activeTransition == null && isLevel1Gameplay) {
            AudioManager.playGameplayMusic();
        } else if (!isLevel1Gameplay) {
            AudioManager.stopGameplayMusic();
        }

        if (screenName.equals("Instructions")) {
            for (Component component : screens.getComponents()) {

                if (component instanceof GameInstructions) {
                    ((GameInstructions) component).startTyping();
                }
            }
        }

        if (screenName.equals("MainMenu") ||
            screenName.equals("LevelSelect")) {

            if (menuMusic == null) {
                playMenuMusic();
            }

        } else {
            stopMenuMusic();
            }
        }

        private static boolean isStoryOrGameplayScreen(String name) {
            return name.equals("StoryNarration") || name.equals("StoryDialogue")
                || name.equals("Level1") || name.equals("Level2") || name.equals("Level3")
                || name.equals("Level2Narration") || name.equals("Level2Conversation")
                || name.equals("Level2EndingDialogue") || name.equals("Level3Narration")
                || name.equals("Level3Conversation") || name.equals("Level3EndingDialogue")
                || name.equals("Level3Unlocked") || name.equals("Level4Unlocked")
                || name.equals("Level5Unlocked");
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
            GameManager.getInstance().resetProgress();
            GameManager.getInstance().restoreUnlocks(SaveManager.highestUnlockedLevel());
            GameManager.getInstance().startGame(Scientist.forKey(
                SCIENTIST_SPRITE_KEYS[characterIndex]));
            introducedEnemies.clear();
            endingShown = false;
            level3EndingShown = false;

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
            clearLevel3Screens();

            // Preserve the original outbreak story and scientist conversation.
            showNarration(OutbreakIntro.opening(this::showScientistConversation));
        }

        // Create the game ONLY AFTER the intro and dialogue are complete.
        // Continue Game bypasses the intro and restores the saved battle.
        private void enterLevel1() {

        AudioManager.setAmbientEnabled(false);

        activeTransition = new LevelTransition(
            1,
            "Abandoned Town",
            () -> {

                AudioManager.stopCinematic();

                activeTransition.setVisible(false);
                activeTransition = null;

                if (currentLevel1 != null) {
                    currentLevel1.setCinematicActive(false);
                }

                AudioManager.playGameplayMusic();
            }
        );

        setGlassPane(activeTransition);
        activeTransition.setVisible(true);

        enterLevel1(false);

        activeTransition.start();
    }

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

            if (activeTransition != null) {
                currentLevel1.setCinematicActive(true);
            }

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
                // Resume an unfinished hospital run without repeating the intro.
                // Otherwise start the mysterious Level 2 narration and conversation.
                int savedIndex = findSavedScientistIndex();
                if (savedIndex >= 0) selectedScientistIndex = savedIndex;
                if (hasSavedLevel2()) enterLevel2(true);
                else startLevel2Intro();
            } else if (level == 3) {
                int savedIndex = findSavedScientistIndex();
                if (savedIndex >= 0) selectedScientistIndex = savedIndex;
                // Resume unfinished factory gameplay; otherwise play the intro.
                if (hasSavedLevel3()) enterLevel3(true);
                else startLevel3Intro();
            } else if (level == 4) {
                int savedIndex = findSavedScientistIndex();
                if (savedIndex >= 0) selectedScientistIndex = savedIndex;
                if (hasSavedLevel4()) enterLevel4(true);
                else startLevel4Intro();
            } else if (level == 5) {
                int savedIndex = findSavedScientistIndex();
                if (savedIndex >= 0) selectedScientistIndex = savedIndex;
                if (hasSavedLevel5()) enterLevel5(true);
                else startLevel5Intro();
            }
        }

        private void showLevelSelection() {
            clearLevel5Screens();
            clearLevel2Screens();
            clearLevel3Screens();
            clearLevel4Screens();
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

            if (activeTransition != null) return;
            if (currentLevel5 != null) {
                currentLevel5.openPauseMenu();
                showScreen("Level5Battlefield");
                return;
            }
            
            if (currentLevel4 != null) {
                currentLevel4.openPauseMenu();
                showScreen("Level4Battlefield");
                return;
            }
            if (currentLevel3 != null) {
                currentLevel3.openPauseMenu();
                showScreen("Level3");
                return;
            }
            if (currentLevel2 != null) {
                // Use the existing purple Game Menu; never stack a white dialog on it.
                currentLevel2.openPauseMenu();
                showScreen("Level2");
                return;
            }
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
            if (currentIntro3 != null) currentIntro3.stop();
            if (currentIntro4 != null) currentIntro4.stop();
            if (currentLevel3 != null) currentLevel3.disposeGame();
            if (currentLevel4 != null) currentLevel4.disposeGame();
            if (currentLevel5 != null) currentLevel5.disposeGame();
            if (currentIntro5 != null) currentIntro5.stop();
            if (currentEnding != null) currentEnding.stop();
            stopMenuMusic();
            AudioManager.shutdown();
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
            clearLevel5Screens();
            GameManager.getInstance().endGame();
            clearLevel4Screens();
            clearLevel2Screens();
            clearLevel3Screens();
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
                () -> enterLevel2(false)
            );
            screens.add(currentLevel2Dialogue, "Level2Conversation");
            screens.revalidate();
            showScreen("Level2Conversation");
            SwingUtilities.invokeLater(() -> {
                if (currentLevel2Dialogue != null)
                    currentLevel2Dialogue.requestFocusInWindow();
            });
        }

        // The Level 2 battlefield uses the same scientist as Level 1 and has
        // its own enemies, map, lab, research journal and persistent checkpoints.
        private static boolean hasSavedLevel2() {
            Properties p = SaveManager.load();
            return "true".equals(p.getProperty("level2.started"))
                && !"true".equals(p.getProperty("level2.won"))
                && p.getProperty("scientist") != null;
        }

        private void enterLevel2(boolean restore) {
            if (SaveManager.highestUnlockedLevel() < 2) {
                showLevelSelection();
                return;
            }
            int savedIndex = findSavedScientistIndex();
            if (savedIndex >= 0) selectedScientistIndex = savedIndex;
            clearLevel2Screens();
            if (currentLevel1 != null) {
                currentLevel1.disposeGame();
                screens.remove(currentLevel1);
                currentLevel1 = null;
            }
            level2EndingShown = false;

            currentLevel2 = new Level2GamePanel(
                resolveScientistSpriteKey(selectedScientistIndex),
                this::showLevel2Ending,
                this::returnToMainMenu
            );
            if (restore) {
                if (!currentLevel2.loadProgress()) {
                    currentLevel2.disposeGame();
                    currentLevel2 = null;
                    JOptionPane.showMessageDialog(this,
                        "Level 2 save could not be restored. Your saved data was not overwritten.",
                        "Load Failed", JOptionPane.WARNING_MESSAGE);
                    showLevelSelection();
                    return;
                }
            } else {
                try {
                    currentLevel2.saveProgress();
                } catch (IOException e) {
                    currentLevel2.disposeGame();
                    currentLevel2 = null;
                    JOptionPane.showMessageDialog(this,
                        "Could not initialize the Level 2 save:\n" + e.getMessage(),
                        "Save Failed", JOptionPane.ERROR_MESSAGE);
                    showLevelSelection();
                    return;
                }
            }
            screens.add(currentLevel2, "Level2");
            screens.revalidate();
            showScreen("Level2");
            SwingUtilities.invokeLater(() -> {
                if (currentLevel2 != null) currentLevel2.requestFocusInWindow();
            });
        }

        // Plague Doctor defeat -> ending conversation -> Level 3 unlock narration.
        private void showLevel2Ending() {
            if (level2EndingShown) return;
            level2EndingShown = true;
            if (currentLevel2 != null) {
                currentLevel2.disposeGame();
                screens.remove(currentLevel2);
                currentLevel2 = null;
            }
            if (currentLevel2Dialogue != null) screens.remove(currentLevel2Dialogue);
            currentLevel2Dialogue = new OutbreakIntro2.DialoguePanel(
                selectedScientistIndex,
                OutbreakIntro2.level2EndingConversation(),
                () -> {
                    if (currentLevel2Dialogue != null) {
                        screens.remove(currentLevel2Dialogue);
                        currentLevel2Dialogue = null;
                    }
                    currentIntro2 = OutbreakIntro2.level3Unlocked(this::startLevel3Intro);
                    screens.add(currentIntro2, "Level3Unlocked");
                    screens.revalidate();
                    showScreen("Level3Unlocked");
                    currentIntro2.start();
                }
            );
            screens.add(currentLevel2Dialogue, "Level2EndingDialogue");
            screens.revalidate();
            showScreen("Level2EndingDialogue");
            currentLevel2Dialogue.requestFocusInWindow();
        }

        private void clearLevel2Screens() {
            if (currentLevel2 != null) {
                currentLevel2.disposeGame();
                screens.remove(currentLevel2);
                currentLevel2 = null;
            }
            if (currentIntro2 != null) {
                currentIntro2.stop();
                screens.remove(currentIntro2);
                currentIntro2 = null;
            }
            if (currentLevel2Dialogue != null) {
                screens.remove(currentLevel2Dialogue);
                currentLevel2Dialogue = null;
            }
        }

        private static boolean hasSavedLevel5() {
            Properties p = SaveManager.load();
            return "true".equals(p.getProperty("level5.started"))
                && !"true".equals(p.getProperty("level5.won"))
                && p.getProperty("scientist") != null;
        }
        private void startLevel5Intro() {
            if (SaveManager.highestUnlockedLevel() < 5) { showLevelSelection(); return; }
            clearLevel2Screens(); clearLevel3Screens(); clearLevel4Screens(); clearLevel5Screens();
            currentIntro5 = OutbreakIntro5.opening(this::showLevel5Conversation);
            screens.add(currentIntro5, "Level5Narration"); screens.revalidate();
            showScreen("Level5Narration"); currentIntro5.start();
        }
        private void showLevel5Conversation() {
            if (currentIntro5 != null) {
                currentIntro5.stop(); screens.remove(currentIntro5); currentIntro5 = null;
            }
            currentLevel5Dialogue = new OutbreakIntro5.DialoguePanel(selectedScientistIndex,
                OutbreakIntro5.scientistConversation(), () -> {
                    clearLevel5Screens(); enterLevel5(false);
                });
            screens.add(currentLevel5Dialogue, "Level5Conversation"); screens.revalidate();
            showScreen("Level5Conversation");
            SwingUtilities.invokeLater(() -> currentLevel5Dialogue.requestFocusInWindow());
        }
        private void enterLevel5(boolean restore) {
            if (SaveManager.highestUnlockedLevel() < 5) { showLevelSelection(); return; }
            clearLevel2Screens(); clearLevel3Screens(); clearLevel4Screens(); clearLevel5Screens();
            level5EndingShown = false;
            currentLevel5 = new Level5GamePanel(resolveScientistSpriteKey(selectedScientistIndex),
                this::showLevel5Ending, this::returnToMainMenu);
            if (restore && !currentLevel5.loadProgress()) {
                currentLevel5.disposeGame(); currentLevel5 = null;
                int choice = JOptionPane.showConfirmDialog(this,
                    "Level 5 checkpoint could not be restored. Restart Level 5?\n"
                    + "Completed Levels 1-4 and your scientist will be preserved.",
                    "Restart Level 5", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        Properties data = SaveManager.load();
                        java.nio.file.Path file = SaveManager.saveFile();
                        if (java.nio.file.Files.exists(file)) {
                            java.nio.file.Files.copy(file, file.resolveSibling("save-before-level5-reset-"
                                + java.util.UUID.randomUUID() + ".properties"));
                        }
                        data.stringPropertyNames().stream()
                            .filter(key -> key.startsWith("level5."))
                            .forEach(data::remove);
                        SaveManager.save(data);
                        startLevel5Intro();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Could not reset Level 5: " + ex.getMessage(),
                            "Save Error", JOptionPane.ERROR_MESSAGE);
                        showLevelSelection();
                    }
                } else showLevelSelection();
                return;
            }
            if (!restore) {
                try { currentLevel5.saveProgress(); }
                catch (IOException e) { JOptionPane.showMessageDialog(this,
                    "Could not save Level 5: " + e.getMessage()); }
            }
            screens.add(currentLevel5, "Level5Battlefield"); screens.revalidate();
            showScreen("Level5Battlefield");
            SwingUtilities.invokeLater(() -> currentLevel5.requestFocusInWindow());
        }
        private void clearLevel5Screens() {
            if (currentLevel5 != null) {
                currentLevel5.disposeGame(); screens.remove(currentLevel5); currentLevel5 = null;
            }
            if (currentIntro5 != null) {
                currentIntro5.stop(); screens.remove(currentIntro5); currentIntro5 = null;
            }
            if (currentLevel5Dialogue != null) {
                screens.remove(currentLevel5Dialogue); currentLevel5Dialogue = null;
            } if (currentEnding != null) {
                  currentEnding.stop();
                  screens.remove(currentEnding);
                  currentEnding = null;
            }
        }
        public void showLevel5Ending() {
            if (level5EndingShown) return;
            try { SaveManager.markCompleted(5); }
            catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Could not save final mission: " + e.getMessage());
                return;
            }
            level5EndingShown = true;
            clearLevel5Screens();
            currentLevel5Dialogue = new OutbreakIntro5.DialoguePanel(selectedScientistIndex,
                OutbreakIntro5.endingConversation(), () -> {
                    if (currentLevel5Dialogue != null) {
                        screens.remove(currentLevel5Dialogue); currentLevel5Dialogue = null;
                    }
                    currentEnding = new OutbreakEnding(
                    selectedScientistIndex,
                    this::showLevelSelection
                    );

                    screens.add(currentEnding, "OutbreakEnding");
                    screens.revalidate();
                    showScreen("OutbreakEnding");
                    currentEnding.start();
                    currentEnding.requestFocusInWindow();
                });
            screens.add(currentLevel5Dialogue, "GenesisEndingDialogue"); screens.revalidate();
            showScreen("GenesisEndingDialogue"); currentLevel5Dialogue.requestFocusInWindow();
        }

        private void startLevel4Intro() {
            if (SaveManager.highestUnlockedLevel() < 4) {
                showLevelSelection();
                return;
            }
            int savedIndex = findSavedScientistIndex();
            if (savedIndex >= 0) selectedScientistIndex = savedIndex;
            clearLevel2Screens();
            clearLevel3Screens();
            clearLevel4Screens();
            if (currentLevel1 != null) {
                currentLevel1.disposeGame();
                screens.remove(currentLevel1);
                currentLevel1 = null;
            }
            currentIntro4 = OutbreakIntro4.opening(this::showLevel4Conversation);
            screens.add(currentIntro4, "Level4Narration");
            screens.revalidate();
            showScreen("Level4Narration");
            currentIntro4.start();
        }

        private void showLevel4Conversation() {
            if (currentIntro4 != null) {
                currentIntro4.stop();
                screens.remove(currentIntro4);
                currentIntro4 = null;
            }
            if (currentLevel4Dialogue != null) screens.remove(currentLevel4Dialogue);
            currentLevel4Dialogue = new OutbreakIntro4.DialoguePanel(
                selectedScientistIndex,
                OutbreakIntro4.scientistConversation(),
                () -> {
                    clearLevel4Screens();
                    enterLevel4(false);
                }
            );
            screens.add(currentLevel4Dialogue, "Level4Conversation");
            screens.revalidate();
            showScreen("Level4Conversation");
            SwingUtilities.invokeLater(() -> {
                if (currentLevel4Dialogue != null) currentLevel4Dialogue.requestFocusInWindow();
            });
        }

        private static boolean hasSavedLevel4() {
            Properties p = SaveManager.load();
            return "true".equals(p.getProperty("level4.started"))
                && !"true".equals(p.getProperty("level4.won"))
                && p.getProperty("scientist") != null;
        }

        private void enterLevel4(boolean restore) {
            if (SaveManager.highestUnlockedLevel() < 4) { showLevelSelection(); return; }
            int savedIndex = findSavedScientistIndex();
            if (savedIndex >= 0) selectedScientistIndex = savedIndex;
            clearLevel2Screens(); clearLevel3Screens(); clearLevel4Screens();
            if (currentLevel1 != null) {
                currentLevel1.disposeGame(); screens.remove(currentLevel1); currentLevel1 = null;
            }
            level4EndingShown = false;
            currentLevel4 = new Level4GamePanel(resolveScientistSpriteKey(selectedScientistIndex),
                this::showLevel4Ending, this::returnToMainMenu);
            if (restore && !currentLevel4.loadProgress()) {
                currentLevel4.disposeGame(); currentLevel4 = null;
                JOptionPane.showMessageDialog(this,
                    "Level 4 save could not be restored. Your saved data was not overwritten.",
                    "Load Failed", JOptionPane.WARNING_MESSAGE);
                showLevelSelection(); return;
            }
            if (!restore) {
                try { currentLevel4.saveProgress(); }
                catch (IOException e) { JOptionPane.showMessageDialog(this,
                    "Could not save Level 4: " + e.getMessage()); }
            }
            screens.add(currentLevel4, "Level4Battlefield"); screens.revalidate();
            showScreen("Level4Battlefield");
            SwingUtilities.invokeLater(() -> currentLevel4.requestFocusInWindow());
        }

        private void clearLevel4Screens() {
            if (currentLevel4 != null) {
                currentLevel4.disposeGame(); screens.remove(currentLevel4); currentLevel4 = null;
            }
            if (currentIntro4 != null) {
                currentIntro4.stop();
                screens.remove(currentIntro4);
                currentIntro4 = null;
            }
            if (currentLevel4Dialogue != null) {
                screens.remove(currentLevel4Dialogue);
                currentLevel4Dialogue = null;
            }
        }

        // Triggered after the Regenerator Zombie boss is stabilized in Level 4.
        public void showLevel4Ending() {
            if (level4EndingShown) return;
            try {
                SaveManager.markCompleted(4);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Could not save Level 4 completion:\n" + e.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            level4EndingShown = true;
            clearLevel4Screens();
            currentLevel4Dialogue = new OutbreakIntro4.DialoguePanel(
                selectedScientistIndex,
                OutbreakIntro4.level4EndingConversation(),
                () -> {
                    if (currentLevel4Dialogue != null) {
                        screens.remove(currentLevel4Dialogue);
                        currentLevel4Dialogue = null;
                    }
                    currentIntro4 = OutbreakIntro4.level5Unlocked(this::showLevelSelection);
                    screens.add(currentIntro4, "Level5Unlocked");
                    screens.revalidate();
                    showScreen("Level5Unlocked");
                    currentIntro4.start();
                }
            );
            screens.add(currentLevel4Dialogue, "Level4EndingDialogue");
            screens.revalidate();
            showScreen("Level4EndingDialogue");
            currentLevel4Dialogue.requestFocusInWindow();
        }

        // LEVEL 3: Toxic Chemical Factory narration -> scientist dialogue -> game.
        private void startLevel3Intro() {
            if (SaveManager.highestUnlockedLevel() < 3) {
                showLevelSelection();
                return;
            }

            int savedIndex = findSavedScientistIndex();
            if (savedIndex >= 0) selectedScientistIndex = savedIndex;
            level3EndingShown = false;

            clearLevel2Screens();
            clearLevel3Screens();
            if (currentLevel1 != null) {
                currentLevel1.disposeGame();
                screens.remove(currentLevel1);
                currentLevel1 = null;
            }

            currentIntro3 = OutbreakIntro3.opening(this::showLevel3Conversation);
            screens.add(currentIntro3, "Level3Narration");
            screens.revalidate();
            showScreen("Level3Narration");
            currentIntro3.start();
        }

        private void showLevel3Conversation() {
            if (currentIntro3 != null) {
                currentIntro3.stop();
                screens.remove(currentIntro3);
                currentIntro3 = null;
            }
            if (currentLevel3Dialogue != null) {
                screens.remove(currentLevel3Dialogue);
            }
            currentLevel3Dialogue = new OutbreakIntro3.DialoguePanel(
                selectedScientistIndex,
                OutbreakIntro3.scientistConversation(),
                () -> enterLevel3(false)
            );
            screens.add(currentLevel3Dialogue, "Level3Conversation");
            screens.revalidate();
            showScreen("Level3Conversation");
            SwingUtilities.invokeLater(() -> {
                if (currentLevel3Dialogue != null)
                    currentLevel3Dialogue.requestFocusInWindow();
            });
        }

        private static boolean hasSavedLevel3() {
            Properties p = SaveManager.load();
            return "true".equals(p.getProperty("level3.started"))
                && !"true".equals(p.getProperty("level3.won"))
                && p.getProperty("scientist") != null;
        }

        private void enterLevel3(boolean restore) {
            if (SaveManager.highestUnlockedLevel() < 3) {
                showLevelSelection();
                return;
            }
            int savedIndex = findSavedScientistIndex();
            if (savedIndex >= 0) selectedScientistIndex = savedIndex;
            clearLevel2Screens();
            clearLevel3Screens();
            if (currentLevel1 != null) {
                currentLevel1.disposeGame();
                screens.remove(currentLevel1);
                currentLevel1 = null;
            }
            level3EndingShown = false;
            currentLevel3 = new Level3GamePanel(
                resolveScientistSpriteKey(selectedScientistIndex),
                this::showLevel3Ending,
                this::returnToMainMenu
            );
            if (restore) {
                if (!currentLevel3.loadProgress()) {
                    currentLevel3.disposeGame();
                    currentLevel3 = null;
                    JOptionPane.showMessageDialog(this,
                        "Level 3 save could not be restored. Your saved data was not overwritten.",
                        "Load Failed", JOptionPane.WARNING_MESSAGE);
                    showLevelSelection();
                    return;
                }
            } else {
                try {
                    currentLevel3.saveProgress();
                } catch (IOException e) {
                    currentLevel3.disposeGame();
                    currentLevel3 = null;
                    JOptionPane.showMessageDialog(this,
                        "Could not create Level 3 checkpoint:\n" + e.getMessage(),
                        "Save Failed", JOptionPane.ERROR_MESSAGE);
                    showLevelSelection();
                    return;
                }
            }
            screens.add(currentLevel3, "Level3");
            screens.revalidate();
            showScreen("Level3");
            SwingUtilities.invokeLater(() -> {
                if (currentLevel3 != null) currentLevel3.requestFocusInWindow();
            });
        }

        // Chemical Abomination defeated -> outro dialogue -> Level 4 unlocked.
        public void showLevel3Ending() {
            if (level3EndingShown) return;
            try {
                SaveManager.markCompleted(3);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Could not save Level 3 completion:\n" + e.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            level3EndingShown = true;
            clearLevel3Screens();
            currentLevel3Dialogue = new OutbreakIntro3.DialoguePanel(
                selectedScientistIndex,
                OutbreakIntro3.level3EndingConversation(),
                () -> {
                    if (currentLevel3Dialogue != null) {
                        screens.remove(currentLevel3Dialogue);
                        currentLevel3Dialogue = null;
                    }
                    currentIntro3 = OutbreakIntro3.level4Unlocked(this::showLevelSelection);
                    screens.add(currentIntro3, "Level4Unlocked");
                    screens.revalidate();
                    showScreen("Level4Unlocked");
                    currentIntro3.start();
                }
            );
            screens.add(currentLevel3Dialogue, "Level3EndingDialogue");
            screens.revalidate();
            showScreen("Level3EndingDialogue");
            currentLevel3Dialogue.requestFocusInWindow();
        }

        private void clearLevel3Screens() {
            if (currentLevel3 != null) {
                currentLevel3.disposeGame();
                screens.remove(currentLevel3);
                currentLevel3 = null;
            }
            if (currentIntro3 != null) {
                currentIntro3.stop();
                screens.remove(currentIntro3);
                currentIntro3 = null;
            }
            if (currentLevel3Dialogue != null) {
                screens.remove(currentLevel3Dialogue);
                currentLevel3Dialogue = null;
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
