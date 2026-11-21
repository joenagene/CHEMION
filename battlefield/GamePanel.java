
package battlefield;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GamePanel extends JPanel implements ActionListener {

    private final TileManager tileManager;
    private final EnvironmentManager environmentManager;
    private final Timer gameTimer;

    private int cameraX = 0;
    private int cameraY = 0;

    private static final int CAMERA_SPEED = 8;

    private boolean up, down, left, right;

    public GamePanel() {
        tileManager = new TileManager();
        environmentManager = new EnvironmentManager(tileManager);

        setPreferredSize(new Dimension(960, 640));
        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);

        setupControls();

        gameTimer = new Timer(16, this);
        gameTimer.start();
    }

    private void setupControls() {
        bindKey("pressed W", "wPressed", () -> up = true);
        bindKey("released W", "wReleased", () -> up = false);

        bindKey("pressed S", "sPressed", () -> down = true);
        bindKey("released S", "sReleased", () -> down = false);

        bindKey("pressed A", "aPressed", () -> left = true);
        bindKey("released A", "aReleased", () -> left = false);

        bindKey("pressed D", "dPressed", () -> right = true);
        bindKey("released D", "dReleased", () -> right = false);

        bindKey("pressed UP", "upPressed", () -> up = true);
        bindKey("released UP", "upReleased", () -> up = false);

        bindKey("pressed DOWN", "downPressed", () -> down = true);
        bindKey("released DOWN", "downReleased", () -> down = false);

        bindKey("pressed LEFT", "leftPressed", () -> left = true);
        bindKey("released LEFT", "leftReleased", () -> left = false);

        bindKey("pressed RIGHT", "rightPressed", () -> right = true);
        bindKey("released RIGHT", "rightReleased", () -> right = false);
    }

    private void bindKey(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(key), name
        );

        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (up) cameraY -= CAMERA_SPEED;
        if (down) cameraY += CAMERA_SPEED;
        if (left) cameraX -= CAMERA_SPEED;
        if (right) cameraX += CAMERA_SPEED;

        cameraX = Math.max(0, Math.min(
            cameraX, Math.max(0, tileManager.getMapWidth() - getWidth())
        ));

        cameraY = Math.max(0, Math.min(
            cameraY, Math.max(0, tileManager.getMapHeight() - getHeight())
        ));

        environmentManager.update();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        tileManager.draw(g2, cameraX, cameraY);
        environmentManager.draw(g2, cameraX, cameraY);

        g2.setColor(new Color(15, 15, 25, 190));
        g2.fillRoundRect(12, 12, 400, 85, 12, 12);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2.drawString("CHEMION - LEVEL 1: THE OUTBREAK", 24, 37);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.drawString("WASD / ARROWS - Explore Town", 24, 61);
        g2.drawString("Camera: " + cameraX + ", " + cameraY, 24, 82);

        g2.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(
                "CHEMION - Level 1: The Outbreak"
            );

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new GamePanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
