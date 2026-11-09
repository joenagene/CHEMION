import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

public class Player extends JPanel {

    // Walking animations only
    private SpriteAnimation walkDown;
    private SpriteAnimation walkUp;
    private SpriteAnimation walkLeft;
    private SpriteAnimation walkRight;

    private SpriteAnimation currentAnimation;

    // Player position
    private int x = 350;
    private int y = 200;

    private final int speed = 4;

    // Display size
    private final int spriteWidth = 96;
    private final int spriteHeight = 112;

    // Movement controls
    private boolean up, down, left, right;

    // Last direction
    private String facing = "down";

    // Damage flash timer (replaces the hurt sprite sheets)
    private int hurtTimer = 0; 

    public Player() {

        setPreferredSize(new Dimension(800, 500));
        setBackground(new Color(30, 20, 50));

        loadAnimations();
        setupControls();

        // Game loop: approximately 60 FPS
        Timer gameTimer = new Timer(16, e -> {
            updateGame();
            repaint();
        });

        gameTimer.start();
    }

    private void loadAnimations() {

        String walkFolder = "assets/characters/nicole/walk/";

        try {

            // WALKING
            walkDown = new SpriteAnimation(
                walkFolder + "nicole_walk_down.png",
                144, 167, 5
            );

            walkUp = new SpriteAnimation(
                walkFolder + "nicole_walk_up.png",
                144, 167, 5
            );

            walkLeft = new SpriteAnimation(
                walkFolder + "nicole_walk_left.png",
                144, 167, 5
            );

            walkRight = new SpriteAnimation(
                walkFolder + "nicole_walk_right.png",
                144, 167, 5
            );

            currentAnimation = walkDown;

            System.out.println(
                "Nicole's walking animations loaded!"
            );

        } catch (IOException e) {

            System.err.println(
                "Failed to load Nicole's animations!"
            );

            e.printStackTrace();
        }
    }

    private void setupControls() {

        bindKey("W", "up",
            () -> up = true,
            () -> up = false
        );

        bindKey("S", "down",
            () -> down = true,
            () -> down = false
        );

        bindKey("A", "left",
            () -> left = true,
            () -> left = false
        );

        bindKey("D", "right",
            () -> right = true,
            () -> right = false
        );

        // Press H to test taking damage (red flash effect)
        bindKey("H", "hurt",
            () -> triggerDamage(),
            () -> {}
        );
    }

    // Call this method whenever a zombie hits the player
    public void triggerDamage() {
        hurtTimer = 20; // Flashes red for 20 frames (~1/3 of a second at 60 FPS)
    }

    private void bindKey(
            String key,
            String name,
            Runnable press,
            Runnable release
    ) {

        InputMap input = getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        ActionMap actions = getActionMap();

        input.put(
            KeyStroke.getKeyStroke("pressed " + key),
            name + "Pressed"
        );

        input.put(
            KeyStroke.getKeyStroke("released " + key),
            name + "Released"
        );

        actions.put(name + "Pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                press.run();
            }
        });

        actions.put(name + "Released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                release.run();
            }
        });
    }

    private void updateGame() {

        // Count down the hurt flash timer
        if (hurtTimer > 0) {
            hurtTimer--;
        }

        boolean moving = up || down || left || right;

        if (up) {
            y -= speed;
            facing = "up";
        }
        else if (down) {
            y += speed;
            facing = "down";
        }
        else if (left) {
            x -= speed;
            facing = "left";
        }
        else if (right) {
            x += speed;
            facing = "right";
        }

        // Keep Nicole inside the game window
        x = Math.max(
            0,
            Math.min(x, getWidth() - spriteWidth)
        );

        y = Math.max(
            0,
            Math.min(y, getHeight() - spriteHeight)
        );

        selectWalkingAnimation();

        if (currentAnimation != null) {

            if (moving) {
                currentAnimation.update();
            }
            else {
                currentAnimation.reset();
            }
        }
    }

    private void selectWalkingAnimation() {

        switch (facing) {

            case "up":
                currentAnimation = walkUp;
                break;

            case "down":
                currentAnimation = walkDown;
                break;

            case "left":
                currentAnimation = walkLeft;
                break;

            case "right":
                currentAnimation = walkRight;
                break;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (currentAnimation != null) {

            // Pass true if hurtTimer is active to trigger the red tint overlay
            boolean isFlashing = hurtTimer > 0;

            currentAnimation.draw(
                g,
                x,
                y,
                spriteWidth,
                spriteHeight,
                isFlashing
            );
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame(
                "CHEMION - Nicole Animation Test"
            );

            frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
            );

            frame.add(new Player());

            frame.pack();

            frame.setLocationRelativeTo(null);

            frame.setResizable(false);

            frame.setVisible(true);
        });
    }
}