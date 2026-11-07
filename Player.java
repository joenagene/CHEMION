
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

public class Player extends JPanel {

    private SpriteAnimation walkDown;
    private SpriteAnimation walkUp;
    private SpriteAnimation walkLeft;
    private SpriteAnimation walkRight;
    private SpriteAnimation currentAnimation;

    private int x = 350;
    private int y = 200;
    private final int speed = 4;
    private final int spriteWidth = 96;
    private final int spriteHeight = 112;

    private boolean up, down, left, right;

    public Player() {
        setPreferredSize(new Dimension(800, 500));
        setBackground(new Color(30, 20, 50));

        try {
            String folder = "assets/characters/";

            walkDown = new SpriteAnimation(
                folder + "nicole_walk_down.png", 144, 167, 5
            );
            walkUp = new SpriteAnimation(
                folder + "nicole_walk_up.png", 144, 167, 5
            );
            walkLeft = new SpriteAnimation(
                folder + "nicole_walk_left.png", 144, 167, 5
            );
            walkRight = new SpriteAnimation(
                folder + "nicole_walk_right.png", 144, 167, 5
            );

            currentAnimation = walkDown;

        } catch (IOException e) {
            System.err.println(
                "Sprite loading error: " + e.getMessage()
            );
        }

        bindKey("W", "up",
            () -> up = true, () -> up = false);
        bindKey("S", "down",
            () -> down = true, () -> down = false);
        bindKey("A", "left",
            () -> left = true, () -> left = false);
        bindKey("D", "right",
            () -> right = true, () -> right = false);

        new Timer(16, e -> {
            updateGame();
            repaint();
        }).start();
    }

    private void bindKey(String key, String name,
                         Runnable press, Runnable release) {

        InputMap input = getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        ActionMap actions = getActionMap();

        input.put(KeyStroke.getKeyStroke("pressed " + key),
                  name + "Pressed");
        input.put(KeyStroke.getKeyStroke("released " + key),
                  name + "Released");

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

        boolean moving = up || down || left || right;

        if (up) {
            y -= speed;
            currentAnimation = walkUp;
        }
        if (down) {
            y += speed;
            currentAnimation = walkDown;
        }
        if (left) {
            x -= speed;
            currentAnimation = walkLeft;
        }
        if (right) {
            x += speed;
            currentAnimation = walkRight;
        }

        x = Math.max(0, Math.min(x, getWidth() - spriteWidth));
        y = Math.max(0, Math.min(y, getHeight() - spriteHeight));

        if (currentAnimation != null) {
            if (moving) {
                currentAnimation.update();
            } else {
                currentAnimation.reset();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (currentAnimation != null) {
            currentAnimation.draw(
                g, x, y, spriteWidth, spriteHeight
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(
                "CHEMION - Nicole Movement Test"
            );
            frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
            );
            frame.add(new Player());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
